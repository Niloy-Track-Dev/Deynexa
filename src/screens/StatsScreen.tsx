import React, { useState, useMemo } from 'react';
import {
  Trophy,
  Flame,
  TrendingUp,
  Calendar,
  Sparkles,
  Target,
  Plus,
  BarChart2,
  CheckCircle2,
  Clock,
  ArrowUpRight,
  ArrowDownRight,
} from 'lucide-react';
import {
  Task,
  TaskOccurrence,
  Category,
  Goal,
  ProductivityInsights,
  DayOfWeek,
  FocentraStudySession,
} from '../types';
import { TimeUtils } from '../utils/timeUtils';
import { schedulingService } from '../services/schedulingService';
import { numberToHexColor } from '../utils/iconHelper';
import { WeeklyReviewModal } from '../components/WeeklyReviewModal';

interface StatsScreenProps {
  tasks: Task[];
  occurrences: TaskOccurrence[];
  categories: Category[];
  goals: Goal[];
  focusSessions: FocentraStudySession[];
  customWeekend: DayOfWeek[];
  accentColor: string;
  onSaveGoal: (goal: Goal) => void;
  onDeleteGoal: (id: number) => void;
}

export const StatsScreen: React.FC<StatsScreenProps> = ({
  tasks,
  occurrences,
  categories,
  goals,
  focusSessions,
  customWeekend,
  accentColor,
  onSaveGoal,
  onDeleteGoal,
}) => {
  const [periodTab, setPeriodTab] = useState<'today' | 'week' | 'month' | 'all'>('week');
  const [showWeeklyReview, setShowWeeklyReview] = useState(false);
  const [showAddGoalModal, setShowAddGoalModal] = useState(false);

  // New goal state
  const [goalTitle, setGoalTitle] = useState('');
  const [goalType, setGoalType] = useState<Goal['targetType']>('TASKS_COMPLETED');
  const [goalPeriod, setGoalPeriod] = useState<Goal['targetPeriod']>('DAILY');
  const [goalValue, setGoalValue] = useState(5);

  const todayStr = TimeUtils.formatLocalDate(new Date());

  // Productivity Insights
  const insights = useMemo(() => {
    return schedulingService.calculateProductivityInsights(
      tasks,
      occurrences,
      customWeekend,
      focusSessions
    );
  }, [tasks, occurrences, customWeekend, focusSessions]);

  // Last 7 days points for chart
  const last7DaysPoints = useMemo(() => {
    const list: { dayLabel: string; date: string; total: number; completed: number; rate: number }[] = [];
    for (let i = 6; i >= 0; i--) {
      const d = TimeUtils.addDays(todayStr, -i);
      const dTasks = schedulingService.getTasksForDate(tasks, occurrences, d, customWeekend);
      const comp = dTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
      const rate = dTasks.length > 0 ? comp / dTasks.length : 0;
      const dObj = TimeUtils.parseLocalDate(d);
      list.push({
        dayLabel: dObj.toLocaleDateString('en-US', { weekday: 'narrow' }),
        date: d,
        total: dTasks.length,
        completed: comp,
        rate,
      });
    }
    return list;
  }, [tasks, occurrences, todayStr, customWeekend]);

  // Productivity Score 2.0
  const scoreResult = useMemo(() => {
    return schedulingService.calculateProductivityScore(
      last7DaysPoints.map((p) => ({
        totalCount: p.total,
        completedCount: p.completed,
        completionRate: p.rate,
      })),
      insights.currentStreak
    );
  }, [last7DaysPoints, insights]);

  // Activity Heatmap: 14 weeks (98 days)
  const heatmapCells = useMemo(() => {
    const cells: { dateStr: string; intensity: number; completed: number; total: number }[] = [];
    const totalDays = 14 * 7;
    for (let i = totalDays - 1; i >= 0; i--) {
      const d = TimeUtils.addDays(todayStr, -i);
      const dTasks = schedulingService.getTasksForDate(tasks, occurrences, d, customWeekend);
      const comp = dTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
      const rate = dTasks.length > 0 ? comp / dTasks.length : 0;
      let intensity = 0;
      if (dTasks.length > 0 && comp > 0) {
        if (rate >= 0.8) intensity = 3;
        else if (rate >= 0.5) intensity = 2;
        else intensity = 1;
      }
      cells.push({
        dateStr: d,
        intensity,
        completed: comp,
        total: dTasks.length,
      });
    }
    return cells;
  }, [tasks, occurrences, todayStr, customWeekend]);

  // Period stats
  const periodStats = useMemo(() => {
    if (periodTab === 'today') {
      const dTasks = schedulingService.getTasksForDate(tasks, occurrences, todayStr, customWeekend);
      const comp = dTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
      const rate = dTasks.length > 0 ? comp / dTasks.length : 0;
      return { total: dTasks.length, completed: comp, rate };
    }
    if (periodTab === 'week') {
      let tot = 0;
      let comp = 0;
      for (let i = 0; i < 7; i++) {
        const d = TimeUtils.addDays(todayStr, -i);
        const dTasks = schedulingService.getTasksForDate(tasks, occurrences, d, customWeekend);
        tot += dTasks.length;
        comp += dTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
      }
      return { total: tot, completed: comp, rate: tot > 0 ? comp / tot : 0 };
    }
    if (periodTab === 'month') {
      let tot = 0;
      let comp = 0;
      for (let i = 0; i < 30; i++) {
        const d = TimeUtils.addDays(todayStr, -i);
        const dTasks = schedulingService.getTasksForDate(tasks, occurrences, d, customWeekend);
        tot += dTasks.length;
        comp += dTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
      }
      return { total: tot, completed: comp, rate: tot > 0 ? comp / tot : 0 };
    }
    // all time
    const allOccs = occurrences;
    const comp = allOccs.filter((o) => o.state === 'COMPLETED').length;
    return { total: allOccs.length, completed: comp, rate: allOccs.length > 0 ? comp / allOccs.length : 0 };
  }, [periodTab, tasks, occurrences, todayStr, customWeekend]);

  // Weekly review data for modal
  const weeklyReviewData = useMemo(() => {
    const mondayOffset = 6;
    const weekStart = TimeUtils.addDays(todayStr, -mondayOffset);
    return schedulingService.generateWeeklyReview(
      weekStart,
      tasks,
      occurrences,
      focusSessions,
      goals,
      customWeekend
    );
  }, [todayStr, tasks, occurrences, focusSessions, goals, customWeekend]);

  const handleCreateGoal = (e: React.FormEvent) => {
    e.preventDefault();
    if (!goalTitle.trim()) return;
    onSaveGoal({
      id: 0,
      title: goalTitle.trim(),
      targetType: goalType,
      targetPeriod: goalPeriod,
      targetValue: goalValue,
      unit: goalType === 'FOCUS_MINUTES' ? 'mins' : 'tasks',
      currentStreak: 0,
      longestStreak: 0,
      isActive: true,
      createdAt: Date.now(),
    });
    setGoalTitle('');
    setShowAddGoalModal(false);
  };

  return (
    <div className="pb-28">
      {/* Top Header */}
      <header className="sticky top-0 z-30 bg-slate-100/90 dark:bg-[#090D16]/90 backdrop-blur-md px-4 pt-4 pb-3 border-b border-slate-200/60 dark:border-slate-800/60">
        <div className="max-w-md mx-auto flex items-center justify-between">
          <div>
            <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Analytics & Intelligence
            </div>
            <h1 className="text-xl font-extrabold text-slate-900 dark:text-slate-100">
              Productivity Score 2.0
            </h1>
          </div>

          <button
            onClick={() => setShowWeeklyReview(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold transition-all shadow-xs"
            style={{
              backgroundColor: `${accentColor}18`,
              color: accentColor,
              border: `1px solid ${accentColor}35`,
            }}
          >
            <Trophy className="w-3.5 h-3.5 fill-current" />
            Weekly Review
          </button>
        </div>
      </header>

      <main className="max-w-md mx-auto px-4 pt-4 space-y-4">
        {/* Productivity Score 2.0 Card */}
        <div className="bg-white dark:bg-[#111827] rounded-3xl p-5 shadow-xs border border-slate-200/80 dark:border-slate-800 relative overflow-hidden">
          <div className="flex items-start justify-between">
            <div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Composite Score
              </span>
              <div className="flex items-baseline gap-2 mt-1">
                <span className="text-4xl font-black text-slate-900 dark:text-slate-100 tracking-tight">
                  {scoreResult.score}
                </span>
                <span className="text-sm font-bold text-slate-400">/ 100</span>
                <span
                  className={`px-2.5 py-0.5 rounded-full text-xs font-bold uppercase ${
                    scoreResult.rating === 'High'
                      ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20'
                      : scoreResult.rating === 'Medium'
                      ? 'bg-blue-500/15 text-blue-600 dark:text-blue-400 border border-blue-500/20'
                      : 'bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/20'
                  }`}
                >
                  {scoreResult.rating}
                </span>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Evaluates routine completion, streak consistency & active days.
              </p>
            </div>

            <div className="p-3 rounded-2xl bg-amber-50 dark:bg-amber-950/30 text-amber-500">
              <Trophy className="w-6 h-6 fill-current" />
            </div>
          </div>

          {/* Quick Metrics Chips */}
          <div className="grid grid-cols-3 gap-2 mt-4 pt-3 border-t border-slate-100 dark:border-slate-800">
            <div className="p-2 rounded-xl bg-slate-50 dark:bg-slate-800/40 text-center">
              <div className="text-[10px] font-bold text-slate-400 uppercase">Current Streak</div>
              <div className="text-sm font-black text-slate-800 dark:text-slate-200 flex items-center justify-center gap-1 mt-0.5">
                <Flame className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                {insights.currentStreak}d
              </div>
            </div>

            <div className="p-2 rounded-xl bg-slate-50 dark:bg-slate-800/40 text-center">
              <div className="text-[10px] font-bold text-slate-400 uppercase">Best Weekday</div>
              <div className="text-sm font-black text-slate-800 dark:text-slate-200 mt-0.5">
                {insights.bestCompletionWeekday}
              </div>
            </div>

            <div className="p-2 rounded-xl bg-slate-50 dark:bg-slate-800/40 text-center">
              <div className="text-[10px] font-bold text-slate-400 uppercase">Focus Minutes</div>
              <div className="text-sm font-black text-slate-800 dark:text-slate-200 mt-0.5">
                {insights.totalFocusMinutes}m
              </div>
            </div>
          </div>
        </div>

        {/* 7-Day Completion Bar Chart */}
        <div className="bg-white dark:bg-[#111827] rounded-3xl p-5 shadow-xs border border-slate-200/80 dark:border-slate-800">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">
                Last 7 Days Completion
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Daily routine accomplishment rate
              </p>
            </div>
            <div className="text-xs font-bold text-slate-500 dark:text-slate-400">
              Avg {Math.round(last7DaysPoints.reduce((acc, p) => acc + p.rate, 0) / 7 * 100)}%
            </div>
          </div>

          <div className="flex items-end justify-between h-28 gap-2 pt-2">
            {last7DaysPoints.map((p) => {
              const heightPercent = Math.max(8, Math.round(p.rate * 100));
              const isTodayPoint = p.date === todayStr;
              return (
                <div key={p.date} className="flex-1 flex flex-col items-center gap-1.5 h-full justify-end">
                  <div className="text-[10px] font-bold text-slate-400">
                    {Math.round(p.rate * 100)}%
                  </div>
                  <div
                    className={`w-full rounded-xl transition-all ${
                      p.rate === 1
                        ? 'bg-emerald-500'
                        : isTodayPoint
                        ? 'bg-blue-600'
                        : p.rate > 0
                        ? 'bg-blue-400 dark:bg-blue-500'
                        : 'bg-slate-200 dark:bg-slate-800'
                    }`}
                    style={{ height: `${heightPercent}%` }}
                  />
                  <div
                    className={`text-[11px] font-bold ${
                      isTodayPoint ? 'text-blue-600 dark:text-blue-400' : 'text-slate-400'
                    }`}
                  >
                    {p.dayLabel}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 14-Week Activity Heatmap (GitHub Style) */}
        <div className="bg-white dark:bg-[#111827] rounded-3xl p-5 shadow-xs border border-slate-200/80 dark:border-slate-800">
          <div className="flex items-center justify-between mb-3">
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">
                Consistency Heatmap
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Last 14 weeks (98 days activity matrix)
              </p>
            </div>
            <div className="flex items-center gap-1 text-[10px] text-slate-400">
              <span>Less</span>
              <span className="w-2.5 h-2.5 rounded-xs bg-slate-100 dark:bg-slate-800" />
              <span className="w-2.5 h-2.5 rounded-xs bg-emerald-200 dark:bg-emerald-950" />
              <span className="w-2.5 h-2.5 rounded-xs bg-emerald-400 dark:bg-emerald-700" />
              <span className="w-2.5 h-2.5 rounded-xs bg-emerald-600 dark:bg-emerald-500" />
              <span>More</span>
            </div>
          </div>

          {/* Grid: 14 columns x 7 rows */}
          <div className="grid grid-flow-col grid-rows-7 gap-1 overflow-x-auto pb-1">
            {heatmapCells.map((cell) => {
              const bg =
                cell.intensity === 3
                  ? 'bg-emerald-600 dark:bg-emerald-500'
                  : cell.intensity === 2
                  ? 'bg-emerald-400 dark:bg-emerald-700'
                  : cell.intensity === 1
                  ? 'bg-emerald-200 dark:bg-emerald-950'
                  : 'bg-slate-100 dark:bg-slate-800/80';
              return (
                <div
                  key={cell.dateStr}
                  title={`${cell.dateStr}: ${cell.completed} / ${cell.total} completed`}
                  className={`w-3.5 h-3.5 rounded-xs transition-colors hover:scale-125 ${bg}`}
                />
              );
            })}
          </div>
        </div>

        {/* Period Breakdown Tabs */}
        <div className="bg-white dark:bg-[#111827] rounded-3xl p-5 shadow-xs border border-slate-200/80 dark:border-slate-800">
          <div className="flex bg-slate-100 dark:bg-slate-800/60 p-1 rounded-2xl mb-4">
            {(['today', 'week', 'month', 'all'] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setPeriodTab(tab)}
                className={`flex-1 py-1.5 rounded-xl text-xs font-bold capitalize transition-all ${
                  periodTab === tab
                    ? 'bg-white dark:bg-[#111827] text-slate-900 dark:text-slate-100 shadow-2xs'
                    : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
                }`}
              >
                {tab === 'all' ? 'All Time' : tab}
              </button>
            ))}
          </div>

          <div className="grid grid-cols-3 gap-3 text-center">
            <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-800/40">
              <div className="text-[10px] font-bold text-slate-400 uppercase">Scheduled</div>
              <div className="text-xl font-black text-slate-900 dark:text-slate-100 mt-1">
                {periodStats.total}
              </div>
            </div>

            <div className="p-3 rounded-2xl bg-emerald-50 dark:bg-emerald-950/30">
              <div className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400 uppercase">
                Completed
              </div>
              <div className="text-xl font-black text-emerald-900 dark:text-emerald-100 mt-1">
                {periodStats.completed}
              </div>
            </div>

            <div className="p-3 rounded-2xl bg-blue-50 dark:bg-blue-950/30">
              <div className="text-[10px] font-bold text-blue-600 dark:text-blue-400 uppercase">
                Success Rate
              </div>
              <div className="text-xl font-black text-blue-900 dark:text-blue-100 mt-1">
                {Math.round(periodStats.rate * 100)}%
              </div>
            </div>
          </div>
        </div>

        {/* Goals & Streaks Section */}
        <div className="bg-white dark:bg-[#111827] rounded-3xl p-5 shadow-xs border border-slate-200/80 dark:border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-1.5">
                <Target className="w-4 h-4 text-emerald-500" />
                Goals & Habit Streaks
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Track long-term productivity targets
              </p>
            </div>

            <button
              onClick={() => setShowAddGoalModal(true)}
              className="p-1.5 rounded-xl text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 transition-colors"
              title="Add New Goal"
            >
              <Plus className="w-5 h-5" />
            </button>
          </div>

          <div className="space-y-2.5 pt-1">
            {goals.map((g) => {
              const currentVal =
                g.targetType === 'TASKS_COMPLETED'
                  ? periodStats.completed
                  : g.targetType === 'FOCUS_MINUTES'
                  ? insights.totalFocusMinutes
                  : 3;
              const progressRate = Math.min(1, currentVal / Math.max(1, g.targetValue));

              return (
                <div
                  key={g.id}
                  className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/70 dark:border-slate-800"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200">
                        {g.title}
                      </h4>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400">
                        {currentVal} of {g.targetValue} {g.unit} ({g.targetPeriod.toLowerCase()})
                      </p>
                    </div>

                    <div className="flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-bold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                      <Flame className="w-3 h-3 fill-current" />
                      {g.currentStreak} streak
                    </div>
                  </div>

                  {/* Progress bar */}
                  <div className="w-full bg-slate-200 dark:bg-slate-700 h-2 rounded-full overflow-hidden mt-2.5">
                    <div
                      className="bg-emerald-500 h-full rounded-full transition-all duration-500"
                      style={{ width: `${Math.round(progressRate * 100)}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </main>

      {/* Weekly Review Modal */}
      {showWeeklyReview && (
        <WeeklyReviewModal
          data={weeklyReviewData}
          onClose={() => setShowWeeklyReview(false)}
        />
      )}

      {/* Add Goal Modal */}
      {showAddGoalModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white dark:bg-[#1F2937] w-full max-w-sm rounded-3xl p-5 shadow-2xl border border-slate-200 dark:border-slate-700 animate-in zoom-in-95">
            <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
              Create New Goal
            </h3>
            <form onSubmit={handleCreateGoal} className="space-y-3 mt-3">
              <div>
                <label className="text-xs font-semibold text-slate-500">Goal Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Daily Study Sprint"
                  value={goalTitle}
                  onChange={(e) => setGoalTitle(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs font-semibold"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-xs font-semibold text-slate-500">Metric</label>
                  <select
                    value={goalType}
                    onChange={(e) => setGoalType(e.target.value as Goal['targetType'])}
                    className="w-full px-2 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs font-semibold"
                  >
                    <option value="TASKS_COMPLETED">Tasks Done</option>
                    <option value="FOCUS_MINUTES">Focus Minutes</option>
                    <option value="HIGH_PRIORITY_TASKS">High Priority</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-500">Period</label>
                  <select
                    value={goalPeriod}
                    onChange={(e) => setGoalPeriod(e.target.value as Goal['targetPeriod'])}
                    className="w-full px-2 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs font-semibold"
                  >
                    <option value="DAILY">Daily</option>
                    <option value="WEEKLY">Weekly</option>
                    <option value="MONTHLY">Monthly</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-500">Target Target Value</label>
                <input
                  type="number"
                  min={1}
                  value={goalValue}
                  onChange={(e) => setGoalValue(Number(e.target.value))}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs font-semibold"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddGoalModal(false)}
                  className="px-3 py-1.5 rounded-xl text-xs font-bold text-slate-500"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700"
                >
                  Save Goal
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
