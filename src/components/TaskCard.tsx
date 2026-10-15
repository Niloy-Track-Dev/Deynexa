import React, { useState } from 'react';
import {
  Check,
  Clock,
  Repeat,
  AlertTriangle,
  MoreVertical,
  CalendarClock,
  SkipForward,
  Edit2,
  Trash2,
  Bell,
  RotateCcw,
} from 'lucide-react';
import { TaskWithOccurrence, Category, TaskState, TaskPriority } from '../types';
import { TimeUtils } from '../utils/timeUtils';
import { getCategoryIcon, numberToHexColor } from '../utils/iconHelper';
import confetti from 'canvas-confetti';

interface TaskCardProps {
  item: TaskWithOccurrence;
  category?: Category;
  is24Hour: boolean;
  onToggleComplete: () => void;
  onToggleSkip: (reason?: string) => void;
  onEdit: () => void;
  onReschedule: () => void;
  onDelete: () => void;
}

export const TaskCard: React.FC<TaskCardProps> = ({
  item,
  category,
  is24Hour,
  onToggleComplete,
  onToggleSkip,
  onEdit,
  onReschedule,
  onDelete,
}) => {
  const [showMenu, setShowMenu] = useState(false);
  const { task, occurrence, effectiveStartTime, effectiveEndTime } = item;
  const isCompleted = occurrence.state === 'COMPLETED';
  const isSkipped = occurrence.state === 'SKIPPED';
  const isRescheduled = occurrence.isException || !!occurrence.rescheduledDate;

  const categoryColor = numberToHexColor(category?.color);
  const CategoryIcon = getCategoryIcon(category?.icon || '');

  const handleCheckboxClick = () => {
    if (!isCompleted) {
      confetti({
        particleCount: 25,
        spread: 40,
        origin: { y: 0.8 },
      });
    }
    onToggleComplete();
  };

  // Priority color config
  const priorityConfig: Record<TaskPriority, { label: string; bg: string; text: string; border: string }> = {
    CRITICAL: {
      label: 'Critical',
      bg: 'bg-rose-50 dark:bg-rose-950/40',
      text: 'text-rose-600 dark:text-rose-400',
      border: 'border-rose-200 dark:border-rose-900',
    },
    HIGH: {
      label: 'High',
      bg: 'bg-amber-50 dark:bg-amber-950/40',
      text: 'text-amber-600 dark:text-amber-400',
      border: 'border-amber-200 dark:border-amber-900',
    },
    MEDIUM: {
      label: 'Medium',
      bg: 'bg-blue-50 dark:bg-blue-950/40',
      text: 'text-blue-600 dark:text-blue-400',
      border: 'border-blue-200 dark:border-blue-900',
    },
    LOW: {
      label: 'Low',
      bg: 'bg-slate-100 dark:bg-slate-800',
      text: 'text-slate-600 dark:text-slate-400',
      border: 'border-slate-200 dark:border-slate-700',
    },
  };

  const priorityStyle = priorityConfig[task.priority || 'MEDIUM'];

  return (
    <div
      className={`relative bg-white dark:bg-[#111827] rounded-2xl p-4 transition-all border ${
        isCompleted
          ? 'border-emerald-200/80 dark:border-emerald-950/50 opacity-80'
          : isSkipped
          ? 'border-rose-200/80 dark:border-rose-950/50 opacity-75'
          : 'border-slate-200/80 dark:border-slate-800 shadow-xs hover:border-slate-300 dark:hover:border-slate-700'
      }`}
    >
      <div className="flex items-start gap-3">
        {/* Checkbox */}
        <button
          onClick={handleCheckboxClick}
          aria-label={isCompleted ? 'Mark pending' : 'Mark completed'}
          className={`w-6 h-6 rounded-full shrink-0 mt-0.5 flex items-center justify-center transition-all ${
            isCompleted
              ? 'bg-emerald-500 text-white shadow-xs'
              : 'border-2 border-slate-300 dark:border-slate-600 hover:border-emerald-500 text-transparent'
          }`}
        >
          <Check className="w-4 h-4 stroke-[3]" />
        </button>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            {/* Category tag */}
            {category && (
              <span
                className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-semibold"
                style={{
                  backgroundColor: `${categoryColor}15`,
                  color: categoryColor,
                }}
              >
                <CategoryIcon className="w-3 h-3" />
                {category.name}
              </span>
            )}

            {/* Priority tag */}
            <span
              className={`inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider border ${priorityStyle.bg} ${priorityStyle.text} ${priorityStyle.border}`}
            >
              {priorityStyle.label}
            </span>

            {/* Recurrence tag */}
            {task.isRecurring && (
              <span className="inline-flex items-center gap-1 text-[11px] text-slate-500 dark:text-slate-400">
                <Repeat className="w-3 h-3 text-slate-400" />
                {task.recurrenceType === 'DAILY'
                  ? 'Daily'
                  : task.recurrenceType === 'WEEKDAYS'
                  ? 'Weekdays'
                  : task.recurrenceType === 'WEEKENDS'
                  ? 'Weekends'
                  : task.recurrenceType === 'SPECIFIC_DAYS'
                  ? `${task.recurringDays.length} days/wk`
                  : task.recurrenceType}
              </span>
            )}

            {/* Reminder indicator */}
            {task.reminderEnabled && (
              <span className="text-slate-400" title="Reminder configured">
                <Bell className="w-3 h-3" />
              </span>
            )}
          </div>

          <h3
            className={`text-base font-bold text-slate-900 dark:text-slate-100 mt-1 truncate ${
              isCompleted ? 'line-through text-slate-400 dark:text-slate-500' : ''
            }`}
          >
            {task.name}
          </h3>

          {task.description && (
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 line-clamp-1">
              {task.description}
            </p>
          )}

          {/* Time & Rescheduled status */}
          <div className="flex items-center gap-3 mt-2 text-xs text-slate-500 dark:text-slate-400 flex-wrap">
            <span className="inline-flex items-center gap-1 font-medium">
              <Clock className="w-3.5 h-3.5 text-slate-400" />
              {task.isAllDay
                ? 'All Day'
                : `${TimeUtils.formatMinutes(effectiveStartTime, is24Hour)} - ${TimeUtils.formatMinutes(
                    effectiveEndTime,
                    is24Hour
                  )}`}
            </span>

            {isRescheduled && (
              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 text-[11px] font-semibold">
                <CalendarClock className="w-3 h-3" />
                Rescheduled
              </span>
            )}

            {isSkipped && (
              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 text-[11px] font-semibold">
                Skipped{occurrence.notes ? `: ${occurrence.notes}` : ''}
              </span>
            )}
          </div>
        </div>

        {/* More actions menu */}
        <div className="relative shrink-0">
          <button
            onClick={() => setShowMenu(!showMenu)}
            aria-label="More options"
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <MoreVertical className="w-4 h-4" />
          </button>

          {showMenu && (
            <>
              <div
                className="fixed inset-0 z-40"
                onClick={() => setShowMenu(false)}
              />
              <div className="absolute right-0 top-8 z-50 w-48 bg-white dark:bg-[#1F2937] rounded-xl shadow-lg border border-slate-200 dark:border-slate-700 py-1.5 animate-in fade-in zoom-in-95">
                <button
                  onClick={() => {
                    setShowMenu(false);
                    onToggleComplete();
                  }}
                  className="w-full px-3.5 py-2 text-left text-xs font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 flex items-center gap-2"
                >
                  <Check className="w-3.5 h-3.5 text-emerald-500" />
                  {isCompleted ? 'Mark Pending' : 'Mark Completed'}
                </button>

                <button
                  onClick={() => {
                    setShowMenu(false);
                    onReschedule();
                  }}
                  className="w-full px-3.5 py-2 text-left text-xs font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 flex items-center gap-2"
                >
                  <CalendarClock className="w-3.5 h-3.5 text-blue-500" />
                  Reschedule Occurrence
                </button>

                <button
                  onClick={() => {
                    setShowMenu(false);
                    onToggleSkip();
                  }}
                  className="w-full px-3.5 py-2 text-left text-xs font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 flex items-center gap-2"
                >
                  {isSkipped ? (
                    <>
                      <RotateCcw className="w-3.5 h-3.5 text-amber-500" />
                      Restore from Skip
                    </>
                  ) : (
                    <>
                      <SkipForward className="w-3.5 h-3.5 text-rose-500" />
                      Skip for Today
                    </>
                  )}
                </button>

                <div className="my-1 border-t border-slate-100 dark:border-slate-750" />

                <button
                  onClick={() => {
                    setShowMenu(false);
                    onEdit();
                  }}
                  className="w-full px-3.5 py-2 text-left text-xs font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 flex items-center gap-2"
                >
                  <Edit2 className="w-3.5 h-3.5 text-slate-400" />
                  Edit Routine
                </button>

                <button
                  onClick={() => {
                    setShowMenu(false);
                    onDelete();
                  }}
                  className="w-full px-3.5 py-2 text-left text-xs font-medium text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 flex items-center gap-2"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  Delete Routine
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
