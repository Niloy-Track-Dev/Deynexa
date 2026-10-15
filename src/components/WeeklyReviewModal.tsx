import React from 'react';
import { X, Trophy, CheckCircle2, AlertCircle, Clock, Sparkles, TrendingUp } from 'lucide-react';
import { WeeklyReviewData } from '../types';

interface WeeklyReviewModalProps {
  data: WeeklyReviewData;
  onClose: () => void;
}

export const WeeklyReviewModal: React.FC<WeeklyReviewModalProps> = ({ data, onClose }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs overflow-y-auto animate-in fade-in">
      <div className="bg-white dark:bg-[#111827] w-full max-w-md rounded-3xl p-6 shadow-2xl border border-slate-200 dark:border-slate-800 my-8 animate-in zoom-in-95">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-2xl bg-amber-50 dark:bg-amber-950/40 text-amber-500">
              <Trophy className="w-5 h-5 fill-current" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100">
                Weekly Review
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {data.weekStartDate} — {data.weekEndDate}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Highlights banner */}
        <div className="mt-4 p-4 rounded-2xl bg-blue-50/80 dark:bg-blue-950/30 border border-blue-200/50 dark:border-blue-900/40 space-y-2">
          <div className="flex items-center gap-2 text-xs font-bold text-blue-700 dark:text-blue-300">
            <Sparkles className="w-4 h-4" />
            Key Week Takeaways
          </div>
          {data.highlights.map((h, i) => (
            <p key={i} className="text-xs text-slate-700 dark:text-slate-300 leading-relaxed">
              • {h}
            </p>
          ))}
        </div>

        {/* Key Metrics Grid */}
        <div className="grid grid-cols-2 gap-3 mt-4">
          <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/70 dark:border-slate-800">
            <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Completion Rate
            </div>
            <div className="text-2xl font-black text-slate-900 dark:text-slate-100 mt-0.5">
              {Math.round(data.completionRate * 100)}%
            </div>
            <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
              {data.totalCompleted} of {data.totalScheduled} routines
            </div>
          </div>

          <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/70 dark:border-slate-800">
            <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Focentra Focus Time
            </div>
            <div className="text-2xl font-black text-slate-900 dark:text-slate-100 mt-0.5">
              {data.focentraFocusMinutes}m
            </div>
            <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
              Best Day: {data.bestFocusDay}
            </div>
          </div>

          <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/70 dark:border-slate-800">
            <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Most Consistent Day
            </div>
            <div className="text-base font-bold text-slate-900 dark:text-slate-100 mt-1">
              {data.bestDayName}
            </div>
            <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
              Peak completion weekday
            </div>
          </div>

          <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/70 dark:border-slate-800">
            <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Active Goals
            </div>
            <div className="text-base font-bold text-slate-900 dark:text-slate-100 mt-1">
              {data.completedGoalsCount} of {data.activeGoalsCount} on track
            </div>
            <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
              Goals maintaining streak
            </div>
          </div>
        </div>

        <div className="flex justify-end mt-5 pt-3 border-t border-slate-100 dark:border-slate-800">
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 shadow-xs transition-all"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
