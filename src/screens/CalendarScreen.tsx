import React, { useState, useMemo } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  Calendar as CalendarIcon,
  CheckCircle2,
  Clock4,
  AlertCircle,
  Plus,
} from 'lucide-react';
import {
  Task,
  TaskOccurrence,
  Category,
  DayOfWeek,
  TaskState,
  TaskWithOccurrence,
} from '../types';
import { TimeUtils } from '../utils/timeUtils';
import { schedulingService } from '../services/schedulingService';
import { TaskCard } from '../components/TaskCard';
import { RescheduleModal } from '../components/RescheduleModal';
import { SkipReasonModal } from '../components/SkipReasonModal';
import { TaskModal } from '../components/TaskModal';
import { storageService } from '../services/storage';

interface CalendarScreenProps {
  tasks: Task[];
  occurrences: TaskOccurrence[];
  categories: Category[];
  is24Hour: boolean;
  weekStart: DayOfWeek;
  accentColor: string;
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
  onSaveTask: (task: Task) => void;
  onDeleteTask: (id: number) => void;
}

export const CalendarScreen: React.FC<CalendarScreenProps> = ({
  tasks,
  occurrences,
  categories,
  is24Hour,
  weekStart,
  accentColor,
  onToggleComplete,
  onToggleSkip,
  onRescheduleOccurrence,
  onSaveTask,
  onDeleteTask,
}) => {
  const [currentYearMonth, setCurrentYearMonth] = useState(() => {
    const now = new Date();
    return { year: now.getFullYear(), month: now.getMonth() };
  });

  const todayStr = TimeUtils.formatLocalDate(new Date());
  const [selectedDate, setSelectedDate] = useState(todayStr);

  const [reschedulingItem, setReschedulingItem] = useState<TaskWithOccurrence | null>(null);
  const [skippingItem, setSkippingItem] = useState<TaskWithOccurrence | null>(null);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [isCreatingTask, setIsCreatingTask] = useState(false);

  const handlePrevMonth = () => {
    setCurrentYearMonth((prev) => {
      if (prev.month === 0) return { year: prev.year - 1, month: 11 };
      return { year: prev.year, month: prev.month - 1 };
    });
  };

  const handleNextMonth = () => {
    setCurrentYearMonth((prev) => {
      if (prev.month === 11) return { year: prev.year + 1, month: 0 };
      return { year: prev.year, month: prev.month + 1 };
    });
  };

  const handleJumpToday = () => {
    const now = new Date();
    setCurrentYearMonth({ year: now.getFullYear(), month: now.getMonth() });
    setSelectedDate(todayStr);
  };

  // Build weekday headers
  const weekdayLabels = useMemo(() => {
    return weekStart === 'SUNDAY'
      ? ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
      : ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  }, [weekStart]);

  // Build grid calendar days
  const calendarDays = useMemo(() => {
    const { year, month } = currentYearMonth;
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);

    const daysInMonth = lastDay.getDate();
    let startDayOfWeek = firstDay.getDay(); // 0 is Sunday
    if (weekStart === 'MONDAY') {
      startDayOfWeek = startDayOfWeek === 0 ? 6 : startDayOfWeek - 1;
    }

    const days: { dateStr: string; dayNumber: number; isCurrentMonth: boolean }[] = [];

    // Preceding padding days
    const prevMonthLastDay = new Date(year, month, 0).getDate();
    for (let i = startDayOfWeek - 1; i >= 0; i--) {
      const dNum = prevMonthLastDay - i;
      const prevDate = new Date(year, month - 1, dNum);
      days.push({
        dateStr: TimeUtils.formatLocalDate(prevDate),
        dayNumber: dNum,
        isCurrentMonth: false,
      });
    }

    // Days of current month
    for (let i = 1; i <= daysInMonth; i++) {
      const curDate = new Date(year, month, i);
      days.push({
        dateStr: TimeUtils.formatLocalDate(curDate),
        dayNumber: i,
        isCurrentMonth: true,
      });
    }

    // Trailing padding days to fill 35 or 42 grid cells
    const remaining = (7 - (days.length % 7)) % 7;
    for (let i = 1; i <= remaining; i++) {
      const nextDate = new Date(year, month + 1, i);
      days.push({
        dateStr: TimeUtils.formatLocalDate(nextDate),
        dayNumber: i,
        isCurrentMonth: false,
      });
    }

    return days;
  }, [currentYearMonth, weekStart]);

  // Tasks for selected date
  const selectedDayTasks = useMemo(() => {
    return schedulingService.getTasksForDate(tasks, occurrences, selectedDate);
  }, [tasks, occurrences, selectedDate]);

  const completedCount = selectedDayTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
  const totalCount = selectedDayTasks.length;

  const monthName = new Date(currentYearMonth.year, currentYearMonth.month).toLocaleDateString(
    'en-US',
    { month: 'long', year: 'numeric' }
  );

  return (
    <div className="pb-28">
      {/* Month Navigation Header */}
      <header className="sticky top-0 z-30 bg-slate-100/90 dark:bg-[#090D16]/90 backdrop-blur-md px-4 pt-4 pb-3 border-b border-slate-200/60 dark:border-slate-800/60">
        <div className="max-w-md mx-auto flex items-center justify-between">
          <div>
            <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Interactive Calendar
            </div>
            <h1 className="text-xl font-extrabold text-slate-900 dark:text-slate-100">
              {monthName}
            </h1>
          </div>

          <div className="flex items-center gap-1">
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

            <button
              onClick={handlePrevMonth}
              aria-label="Previous Month"
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>

            <button
              onClick={handleNextMonth}
              aria-label="Next Month"
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-md mx-auto px-4 pt-4 space-y-4">
        {/* Calendar Grid Card */}
        <div className="bg-white dark:bg-[#111827] rounded-3xl p-4 shadow-xs border border-slate-200/80 dark:border-slate-800">
          {/* Weekday headers */}
          <div className="grid grid-cols-7 gap-1 text-center mb-2">
            {weekdayLabels.map((label) => (
              <div
                key={label}
                className="text-[11px] font-bold uppercase text-slate-400 py-1"
              >
                {label}
              </div>
            ))}
          </div>

          {/* Grid cells */}
          <div className="grid grid-cols-7 gap-1">
            {calendarDays.map((cell) => {
              const isSelected = cell.dateStr === selectedDate;
              const isTodayCell = cell.dateStr === todayStr;

              // Check day routines
              const dayTasks = schedulingService.getTasksForDate(tasks, occurrences, cell.dateStr);
              const dayTotal = dayTasks.length;
              const dayComp = dayTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
              const rate = dayTotal > 0 ? dayComp / dayTotal : 0;

              return (
                <button
                  key={cell.dateStr}
                  onClick={() => setSelectedDate(cell.dateStr)}
                  className={`h-12 rounded-2xl flex flex-col items-center justify-center p-1 transition-all relative ${
                    isSelected
                      ? 'bg-blue-600 text-white font-bold shadow-xs scale-105 z-10'
                      : isTodayCell
                      ? 'bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 font-extrabold border border-blue-200 dark:border-blue-800'
                      : cell.isCurrentMonth
                      ? 'text-slate-800 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800'
                      : 'text-slate-300 dark:text-slate-600'
                  }`}
                >
                  <span className="text-xs">{cell.dayNumber}</span>

                  {/* Completion dot or count */}
                  {dayTotal > 0 && (
                    <div className="flex items-center gap-0.5 mt-0.5">
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          isSelected
                            ? 'bg-white'
                            : rate === 1
                            ? 'bg-emerald-500'
                            : rate > 0
                            ? 'bg-amber-500'
                            : 'bg-slate-400'
                        }`}
                      />
                    </div>
                  )}
                </button>
              );
            })}
          </div>

          {/* Intensity Legend */}
          <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400 pt-3 mt-3 border-t border-slate-100 dark:border-slate-800">
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-emerald-500" />
              100% Done
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-amber-500" />
              Partial
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-slate-400" />
              Pending
            </span>
          </div>
        </div>

        {/* Selected Date Header & Stats */}
        <div className="flex items-center justify-between px-1">
          <div>
            <h2 className="text-base font-extrabold text-slate-900 dark:text-slate-100">
              {TimeUtils.formatDateHeader(selectedDate)}
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {totalCount === 0
                ? 'No routines scheduled'
                : `${completedCount} of ${totalCount} completed (${Math.round(
                    (completedCount / totalCount) * 100
                  )}%)`}
            </p>
          </div>

          <button
            onClick={() => setIsCreatingTask(true)}
            className="px-3 py-1.5 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 shadow-xs flex items-center gap-1 transition-all"
          >
            <Plus className="w-3.5 h-3.5" />
            Add Task
          </button>
        </div>

        {/* Selected Day Routines List */}
        {selectedDayTasks.length === 0 ? (
          <div className="p-8 text-center bg-white dark:bg-[#111827] rounded-3xl border border-dashed border-slate-200 dark:border-slate-800">
            <p className="text-xs text-slate-500 dark:text-slate-400">
              No tasks scheduled for this date.
            </p>
          </div>
        ) : (
          <div className="space-y-2.5">
            {selectedDayTasks.map((item) => (
              <TaskCard
                key={`cal_${item.task.id}_${item.occurrence.date}`}
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
        )}
      </main>

      {/* Modals */}
      {(isCreatingTask || editingTask) && (
        <TaskModal
          initialTask={editingTask}
          categories={categories}
          templates={storageService.getTemplates()}
          allTasks={tasks}
          is24Hour={is24Hour}
          onSave={(task) => {
            onSaveTask(task);
            setIsCreatingTask(false);
            setEditingTask(null);
          }}
          onSaveTemplate={(name, task) => {
            storageService.saveTemplate({
              id: 0,
              name,
              description: task.description,
              categoryId: task.categoryId,
              priority: task.priority,
              defaultDurationMinutes: 45,
              startTime: task.startTime,
              endTime: task.endTime,
              isAllDay: task.isAllDay,
              recurrenceType: task.recurrenceType,
              recurringDays: task.recurringDays,
              recurrenceInterval: task.recurrenceInterval,
              reminderEnabled: task.reminderEnabled,
              reminderOffsetMinutes: task.reminderOffsetMinutes || 0,
              createdAt: Date.now(),
            });
          }}
          onClose={() => {
            setIsCreatingTask(false);
            setEditingTask(null);
          }}
        />
      )}

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
    </div>
  );
};
