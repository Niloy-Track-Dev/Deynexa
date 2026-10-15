import React from 'react';
import { Flame, Clock, CheckCircle2, Clock4, AlertCircle } from 'lucide-react';

interface TodayProgressCardProps {
  totalCount: number;
  completedCount: number;
  pendingCount: number;
  skippedCount: number;
  completionPercentage: number;
  currentStreak: number;
  totalFocusMinutes: number;
  accentColor: string;
}

export const TodayProgressCard: React.FC<TodayProgressCardProps> = ({
  totalCount,
  completedCount,
  pendingCount,
  skippedCount,
  completionPercentage,
  currentStreak,
  totalFocusMinutes,
  accentColor,
}) => {
  const percentInt = Math.round(completionPercentage * 100);

  return (
    <div className="bg-white dark:bg-[#111827] rounded-2xl p-5 shadow-xs border border-slate-200/80 dark:border-slate-800">
      <div className="flex items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Daily Progress
            </span>
            {currentStreak > 0 && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-bold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                <Flame className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                {currentStreak} {currentStreak === 1 ? 'day' : 'days'}
              </span>
            )}
            {totalFocusMinutes > 0 && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-bold bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                <Clock className="w-3.5 h-3.5 text-blue-500" />
                {totalFocusMinutes}m focus
              </span>
            )}
          </div>
          <h2 className="text-2xl font-extrabold text-slate-900 dark:text-slate-100 mt-1">
            {completedCount} of {totalCount} completed
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            {totalCount === 0
              ? 'No routines scheduled for this day'
              : percentInt === 100
              ? '🎉 All routines crushed today! Outstanding!'
              : `${pendingCount} remaining • ${skippedCount} skipped`}
          </p>
        </div>

        {/* Circular Progress Gauge */}
        <div className="relative w-16 h-16 shrink-0 flex items-center justify-center">
          <svg className="w-full h-full -rotate-90" viewBox="0 0 36 36">
            <path
              className="text-slate-100 dark:text-slate-800"
              strokeWidth="3.5"
              stroke="currentColor"
              fill="none"
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
            />
            <path
              strokeDasharray={`${percentInt}, 100`}
              strokeWidth="3.5"
              strokeLinecap="round"
              stroke={percentInt === 100 ? '#10B981' : accentColor}
              fill="none"
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              className="transition-all duration-500"
            />
          </svg>
          <div className="absolute text-center">
            <span className="text-sm font-extrabold text-slate-800 dark:text-slate-100">
              {percentInt}%
            </span>
          </div>
        </div>
      </div>

      {/* Progress Pills Breakdown */}
      <div className="grid grid-cols-3 gap-2 mt-4 pt-3 border-t border-slate-100 dark:border-slate-800/80">
        <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200/50 dark:border-emerald-800/40">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
          <div>
            <div className="text-[10px] uppercase font-bold text-emerald-700 dark:text-emerald-300">
              Done
            </div>
            <div className="text-sm font-bold text-emerald-900 dark:text-emerald-100">
              {completedCount}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/50 dark:border-slate-700/40">
          <Clock4 className="w-4 h-4 text-slate-500 dark:text-slate-400 shrink-0" />
          <div>
            <div className="text-[10px] uppercase font-bold text-slate-600 dark:text-slate-400">
              Pending
            </div>
            <div className="text-sm font-bold text-slate-800 dark:text-slate-200">
              {pendingCount}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-rose-50 dark:bg-rose-950/30 border border-rose-200/50 dark:border-rose-800/40">
          <AlertCircle className="w-4 h-4 text-rose-500 dark:text-rose-400 shrink-0" />
          <div>
            <div className="text-[10px] uppercase font-bold text-rose-700 dark:text-rose-300">
              Skipped
            </div>
            <div className="text-sm font-bold text-rose-900 dark:text-rose-100">
              {skippedCount}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
