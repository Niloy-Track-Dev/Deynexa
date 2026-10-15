import React, { useState } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  Calendar as CalendarIcon,
  Plus,
  Clock,
  Sparkles,
} from 'lucide-react';
import {
  Task,
  TaskWithOccurrence,
  Category,
  TaskState,
  RecurrenceEditMode,
  FocentraStudySession,
  ProductivityInsights,
} from '../types';
import { TimeUtils } from '../utils/timeUtils';
import { TodayProgressCard } from '../components/TodayProgressCard';
import { TaskCard } from '../components/TaskCard';
import { TaskModal } from '../components/TaskModal';
import { RescheduleModal } from '../components/RescheduleModal';
import { SkipReasonModal } from '../components/SkipReasonModal';
import { FocentraTimerModal } from '../components/FocentraTimerModal';
import { numberToHexColor } from '../utils/iconHelper';
import { storageService } from '../services/storage';

interface TodayScreenProps {
  selectedDate: string;
  tasksWithOccurrences: TaskWithOccurrence[];
  allTasks: Task[];
  categories: Category[];
  is24Hour: boolean;
  accentColor: string;
  insights: ProductivityInsights;
  onSelectDate: (date: string) => void;
  onSaveTask: (task: Task, mode: RecurrenceEditMode) => void;
  onDeleteTask: (id: number) => void;
  onToggleComplete: (taskId: number, date: string, currentState: TaskState) => void;
  onToggleSkip: (taskId: number, date: string, currentState: TaskState, reason?: string) => void;
  onRescheduleOccurrence: (
    taskId: number,
    originalDate: string,
    targetDate: string,
    startTime: number,
    endTime: number,
    notes?: string
  ) => void;
  onSaveTemplate: (name: string, task: Task) => void;
  onLogFocentraSession: (session: FocentraStudySession) => void;
}

export const TodayScreen: React.FC<TodayScreenProps> = ({
  selectedDate,
  tasksWithOccurrences,
  allTasks,
  categories,
  is24Hour,
  accentColor,
  insights,
  onSelectDate,
  onSaveTask,
  onDeleteTask,
  onToggleComplete,
  onToggleSkip,
  onRescheduleOccurrence,
  onSaveTemplate,
  onLogFocentraSession,
}) => {
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [isCreatingTask, setIsCreatingTask] = useState(false);
  const [reschedulingItem, setReschedulingItem] = useState<TaskWithOccurrence | null>(null);
  const [skippingItem, setSkippingItem] = useState<TaskWithOccurrence | null>(null);
  const [showFocentraTimer, setShowFocentraTimer] = useState(false);

  const todayStr = TimeUtils.formatLocalDate(new Date());
  const isToday = selectedDate === todayStr;

  // Filtering by category
  const filteredTasks = selectedCategoryId
    ? tasksWithOccurrences.filter((t) => t.task.categoryId === selectedCategoryId)
    : tasksWithOccurrences;

  const completedTasks = filteredTasks.filter((t) => t.occurrence.state === 'COMPLETED');
  const activeTasks = filteredTasks.filter((t) => t.occurrence.state !== 'COMPLETED');

  const totalCount = filteredTasks.length;
  const completedCount = completedTasks.length;
  const pendingCount = filteredTasks.filter((t) => t.occurrence.state === 'PENDING').length;
  const skippedCount = filteredTasks.filter((t) => t.occurrence.state === 'SKIPPED').length;
  const completionPercentage = totalCount > 0 ? completedCount / totalCount : 0;

  const handlePrevDay = () => onSelectDate(TimeUtils.addDays(selectedDate, -1));
  const handleNextDay = () => onSelectDate(TimeUtils.addDays(selectedDate, 1));
  const handleJumpToday = () => onSelectDate(todayStr);

  return (
    <div className="pb-28">
      {/* Top Header */}
      <header className="sticky top-0 z-30 bg-slate-100/90 dark:bg-[#090D16]/90 backdrop-blur-md px-4 pt-4 pb-3 border-b border-slate-200/60 dark:border-slate-800/60">
        <div className="max-w-md mx-auto flex items-center justify-between">
          <div>
            <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              {isToday
                ? TimeUtils.getDayGreeting()
                : selectedDate < todayStr
                ? 'Viewing History'
                : 'Viewing Future'}
            </div>
            <h1 className="text-xl font-extrabold text-slate-900 dark:text-slate-100">
              {TimeUtils.formatDateHeader(selectedDate)}
            </h1>
          </div>

          <div className="flex items-center gap-1">
            {!isToday && (
              <button
                onClick={handleJumpToday}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold transition-all"
                style={{
                  backgroundColor: `${accentColor}18`,
                  color: accentColor,
                  border: `1px solid ${accentColor}35`,
                }}
              >
                <CalendarIcon className="w-3.5 h-3.5" />
                Today
              </button>
            )}

            <button
              onClick={() => setShowFocentraTimer(true)}
              title="Launch Focentra Focus Timer"
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors"
            >
              <Clock className="w-4 h-4 text-blue-500" />
            </button>

            <button
              onClick={handlePrevDay}
              aria-label="Previous Day"
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>

            <button
              onClick={handleNextDay}
              aria-label="Next Day"
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-md mx-auto px-4 pt-4 space-y-4">
        {/* Daily Progress Gauge */}
        <TodayProgressCard
          totalCount={totalCount}
          completedCount={completedCount}
          pendingCount={pendingCount}
          skippedCount={skippedCount}
          completionPercentage={completionPercentage}
          currentStreak={insights.currentStreak}
          totalFocusMinutes={insights.totalFocusMinutes}
          accentColor={accentColor}
        />

        {/* Category Filter Chips */}
        {categories.length > 0 && (
          <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
            <button
              onClick={() => setSelectedCategoryId(null)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold shrink-0 transition-all ${
                selectedCategoryId === null
                  ? 'bg-blue-600 text-white shadow-2xs'
                  : 'bg-white dark:bg-[#111827] text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-slate-800 hover:border-slate-300'
              }`}
            >
              All
            </button>
            {categories.map((c) => {
              const active = selectedCategoryId === c.id;
              const color = numberToHexColor(c.color);
              return (
                <button
                  key={c.id}
                  onClick={() => setSelectedCategoryId(active ? null : c.id)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold shrink-0 flex items-center gap-1.5 transition-all ${
                    active
                      ? 'shadow-2xs'
                      : 'bg-white dark:bg-[#111827] border border-slate-200 dark:border-slate-800'
                  }`}
                  style={
                    active
                      ? { backgroundColor: `${color}20`, color: color, border: `1px solid ${color}40` }
                      : { color: active ? color : undefined }
                  }
                >
                  <span
                    className="w-2 h-2 rounded-full"
                    style={{ backgroundColor: color }}
                  />
                  {c.name}
                </button>
              );
            })}
          </div>
        )}

        {/* Task lists */}
        {filteredTasks.length === 0 ? (
          <div className="text-center py-12 px-4 bg-white dark:bg-[#111827] rounded-3xl border border-dashed border-slate-200 dark:border-slate-800">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 dark:bg-blue-950/40 text-blue-500 mx-auto flex items-center justify-center mb-3">
              <CalendarIcon className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-800 dark:text-slate-200">
              {selectedCategoryId ? 'No routines in this category' : 'No routines scheduled'}
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-xs mx-auto">
              {isToday
                ? 'Plan your day by adding recurring routines or one-time tasks.'
                : `No routines scheduled for ${TimeUtils.formatDateHeader(selectedDate)}.`}
            </p>
            {isToday && (
              <button
                onClick={() => setIsCreatingTask(true)}
                className="mt-4 px-4 py-2 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 shadow-xs transition-all inline-flex items-center gap-1.5"
              >
                <Plus className="w-4 h-4" />
                Add New Task
              </button>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            {/* Active Schedule Section */}
            {activeTasks.length > 0 && (
              <div>
                <h4 className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1 mb-2">
                  Schedule ({activeTasks.length})
                </h4>
                <div className="space-y-2.5">
                  {activeTasks.map((item) => (
                    <TaskCard
                      key={`${item.task.id}_${item.occurrence.date}`}
                      item={item}
                      category={categories.find((c) => c.id === item.task.categoryId)}
                      is24Hour={is24Hour}
                      onToggleComplete={() =>
                        onToggleComplete(item.task.id, item.occurrence.date, item.occurrence.state)
                      }
                      onToggleSkip={(reason) => {
                        if (item.occurrence.state === 'SKIPPED') {
                          onToggleSkip(item.task.id, item.occurrence.date, item.occurrence.state);
                        } else {
                          setSkippingItem(item);
                        }
                      }}
                      onEdit={() => setEditingTask(item.task)}
                      onReschedule={() => setReschedulingItem(item)}
                      onDelete={() => onDeleteTask(item.task.id)}
                    />
                  ))}
                </div>
              </div>
            )}

            {/* Completed Section */}
            {completedTasks.length > 0 && (
              <div className="pt-2">
                <h4 className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1 mb-2">
                  Completed ({completedTasks.length})
                </h4>
                <div className="space-y-2.5">
                  {completedTasks.map((item) => (
                    <TaskCard
                      key={`comp_${item.task.id}_${item.occurrence.date}`}
                      item={item}
                      category={categories.find((c) => c.id === item.task.categoryId)}
                      is24Hour={is24Hour}
                      onToggleComplete={() =>
                        onToggleComplete(item.task.id, item.occurrence.date, item.occurrence.state)
                      }
                      onToggleSkip={() =>
                        onToggleSkip(item.task.id, item.occurrence.date, item.occurrence.state)
                      }
                      onEdit={() => setEditingTask(item.task)}
                      onReschedule={() => setReschedulingItem(item)}
                      onDelete={() => onDeleteTask(item.task.id)}
                    />
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Floating Action Button */}
      <button
        onClick={() => setIsCreatingTask(true)}
        style={{ backgroundColor: accentColor }}
        aria-label="Add Routine"
        className="fixed bottom-20 right-5 z-40 p-4 rounded-2xl text-white shadow-lg shadow-blue-500/25 hover:scale-105 active:scale-95 transition-all flex items-center justify-center"
      >
        <Plus className="w-6 h-6 stroke-[2.5]" />
      </button>

      {/* Task Creation / Edit Modal */}
      {(isCreatingTask || editingTask) && (
        <TaskModal
          initialTask={editingTask}
          categories={categories}
          templates={storageService.getTemplates()}
          allTasks={allTasks}
          is24Hour={is24Hour}
          onSave={(task, mode) => {
            onSaveTask(task, mode);
            setIsCreatingTask(false);
            setEditingTask(null);
          }}
          onSaveTemplate={onSaveTemplate}
          onClose={() => {
            setIsCreatingTask(false);
            setEditingTask(null);
          }}
        />
      )}

      {/* Reschedule Modal */}
      {reschedulingItem && (
        <RescheduleModal
          item={reschedulingItem}
          is24Hour={is24Hour}
          onSave={(targetDate, startTime, endTime, notes) => {
            onRescheduleOccurrence(
              reschedulingItem.task.id,
              reschedulingItem.occurrence.date,
              targetDate,
              startTime,
              endTime,
              notes
            );
            setReschedulingItem(null);
          }}
          onClose={() => setReschedulingItem(null)}
        />
      )}

      {/* Skip Reason Modal */}
      {skippingItem && (
        <SkipReasonModal
          taskName={skippingItem.task.name}
          onConfirm={(reason) => {
            onToggleSkip(
              skippingItem.task.id,
              skippingItem.occurrence.date,
              skippingItem.occurrence.state,
              reason
            );
            setSkippingItem(null);
          }}
          onClose={() => setSkippingItem(null)}
        />
      )}

      {/* Focentra Study Timer Modal */}
      {showFocentraTimer && (
        <FocentraTimerModal
          onSessionComplete={onLogFocentraSession}
          onClose={() => setShowFocentraTimer(false)}
        />
      )}
    </div>
  );
};
