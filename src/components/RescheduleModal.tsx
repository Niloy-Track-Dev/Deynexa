import React, { useState } from 'react';
import { CalendarClock, X } from 'lucide-react';
import { TaskWithOccurrence } from '../types';
import { TimeUtils } from '../utils/timeUtils';

interface RescheduleModalProps {
  item: TaskWithOccurrence;
  is24Hour: boolean;
  onSave: (date: string, startTime: number, endTime: number, notes?: string) => void;
  onClose: () => void;
}

export const RescheduleModal: React.FC<RescheduleModalProps> = ({
  item,
  is24Hour,
  onSave,
  onClose,
}) => {
  const [targetDate, setTargetDate] = useState(
    item.occurrence.rescheduledDate || item.occurrence.date
  );
  const [startTime, setStartTime] = useState(
    TimeUtils.minutesToInputTime(item.effectiveStartTime ?? 480)
  );
  const [endTime, setEndTime] = useState(
    TimeUtils.minutesToInputTime(item.effectiveEndTime ?? 540)
  );
  const [notes, setNotes] = useState(item.occurrence.notes || '');

  const handleApply = () => {
    const startMins = TimeUtils.parseTimeToMinutes(startTime);
    const endMins = TimeUtils.parseTimeToMinutes(endTime);
    onSave(targetDate, startMins, endMins, notes.trim() || undefined);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in">
      <div className="bg-white dark:bg-[#1F2937] w-full max-w-sm rounded-2xl p-5 shadow-xl border border-slate-200 dark:border-slate-700 animate-in zoom-in-95">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400">
              <CalendarClock className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
                Reschedule Occurrence
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 truncate max-w-[200px]">
                {item.task.name}
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

        <p className="text-xs text-slate-500 dark:text-slate-400 mt-3">
          Adjust timing or date for this occurrence without altering your master recurring rule:
        </p>

        <div className="space-y-3 mt-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Target Date
            </label>
            <input
              type="date"
              value={targetDate}
              onChange={(e) => setTargetDate(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm font-medium text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Start Time
              </label>
              <input
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm font-medium text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                End Time
              </label>
              <input
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm font-medium text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Reason / Notes (Optional)
            </label>
            <input
              type="text"
              placeholder="e.g. Doctor appointment delay"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        <div className="flex items-center justify-end gap-2 mt-5 pt-3 border-t border-slate-100 dark:border-slate-800">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleApply}
            className="px-4 py-2 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 shadow-xs transition-all"
          >
            Apply Adjustment
          </button>
        </div>
      </div>
    </div>
  );
};
