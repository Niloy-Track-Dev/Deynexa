import React, { useState, useEffect, useMemo } from 'react';
import {
  X,
  Clock,
  Repeat,
  AlertTriangle,
  Calendar,
  Sparkles,
  BookmarkCheck,
  Bell,
  CheckCircle2,
  CalendarDays,
} from 'lucide-react';
import {
  Task,
  Category,
  TaskTemplate,
  TaskPriority,
  RecurrenceType,
  RecurrenceEndType,
  RecurrenceEditMode,
  DayOfWeek,
  DAYS_OF_WEEK,
} from '../types';
import { TimeUtils } from '../utils/timeUtils';
import { schedulingService } from '../services/schedulingService';
import { storageService } from '../services/storage';

interface TaskModalProps {
  initialTask?: Task | null;
  categories: Category[];
  templates: TaskTemplate[];
  allTasks: Task[];
  is24Hour: boolean;
  onSave: (task: Task, mode: RecurrenceEditMode) => void;
  onSaveTemplate: (templateName: string, task: Task) => void;
  onClose: () => void;
}

export const TaskModal: React.FC<TaskModalProps> = ({
  initialTask,
  categories,
  templates,
  allTasks,
  is24Hour,
  onSave,
  onSaveTemplate,
  onClose,
}) => {
  const isEditing = !!initialTask && initialTask.id > 0;
  const todayStr = TimeUtils.formatLocalDate(new Date());

  const [name, setName] = useState(initialTask?.name || '');
  const [description, setDescription] = useState(initialTask?.description || '');
  const [categoryId, setCategoryId] = useState<number>(
    initialTask?.categoryId || categories[0]?.id || 1
  );
  const [priority, setPriority] = useState<TaskPriority>(initialTask?.priority || 'MEDIUM');
  const [isAllDay, setIsAllDay] = useState(initialTask?.isAllDay || false);
  const [startTime, setStartTime] = useState(
    TimeUtils.minutesToInputTime(initialTask?.startTime ?? 480)
  );
  const [endTime, setEndTime] = useState(
    TimeUtils.minutesToInputTime(initialTask?.endTime ?? 540)
  );
  const [isRecurring, setIsRecurring] = useState(initialTask?.isRecurring ?? true);
  const [recurrenceType, setRecurrenceType] = useState<RecurrenceType>(
    initialTask?.recurrenceType || 'DAILY'
  );
  const [recurringDays, setRecurringDays] = useState<DayOfWeek[]>(
    initialTask?.recurringDays || ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
  );
  const [recurrenceInterval, setRecurrenceInterval] = useState(
    initialTask?.recurrenceInterval || 1
  );
  const [recurrenceEndType, setRecurrenceEndType] = useState<RecurrenceEndType>(
    initialTask?.recurrenceEndType || 'NEVER'
  );
  const [recurrenceEndDate, setRecurrenceEndDate] = useState(
    initialTask?.recurrenceEndDate || ''
  );
  const [recurrenceCount, setRecurrenceCount] = useState(
    initialTask?.recurrenceCount || 10
  );
  const [startDate, setStartDate] = useState(initialTask?.startDate || todayStr);
  const [reminderEnabled, setReminderEnabled] = useState(
    initialTask?.reminderEnabled || false
  );
  const [reminderOffsetMinutes, setReminderOffsetMinutes] = useState(
    initialTask?.reminderOffsetMinutes || 0
  );

  // Template saving & Edit Mode dialog state
  const [templateNameInput, setTemplateNameInput] = useState('');
  const [showTemplateModal, setShowTemplateModal] = useState(false);
  const [showEditModeDialog, setShowEditModeDialog] = useState(false);

  // Build draft task for live evaluation
  const draftTask: Task = useMemo(() => {
    const sMins = isAllDay ? null : TimeUtils.parseTimeToMinutes(startTime);
    const eMins = isAllDay ? null : TimeUtils.parseTimeToMinutes(endTime);
    return {
      id: initialTask?.id || 0,
      name: name.trim() || 'New Routine',
      description: description.trim(),
      categoryId,
      priority,
      startTime: sMins,
      endTime: eMins,
      isAllDay,
      isEnabled: true,
      isRecurring,
      recurringDays,
      recurrenceType: isRecurring ? recurrenceType : 'NONE',
      recurrenceInterval,
      recurrenceEndType,
      recurrenceEndDate: recurrenceEndDate || null,
      recurrenceCount: recurrenceEndType === 'AFTER_OCCURRENCES' ? recurrenceCount : null,
      startDate,
      reminderEnabled,
      reminderOffsetMinutes: reminderEnabled ? reminderOffsetMinutes : null,
      createdAt: initialTask?.createdAt || Date.now(),
      updatedAt: Date.now(),
    };
  }, [
    initialTask,
    name,
    description,
    categoryId,
    priority,
    isAllDay,
    startTime,
    endTime,
    isRecurring,
    recurringDays,
    recurrenceType,
    recurrenceInterval,
    recurrenceEndType,
    recurrenceEndDate,
    recurrenceCount,
    startDate,
    reminderEnabled,
    reminderOffsetMinutes,
  ]);

  // Next Occurrence calculation
  const nextOccurrence = useMemo(() => {
    return schedulingService.calculateNextOccurrence(draftTask);
  }, [draftTask]);

  // Real-time conflict detection
  const conflicts = useMemo(() => {
    if (isAllDay || !nextOccurrence) return [];
    const occurrences = storageService.getOccurrences();
    const tasksOnDate = schedulingService.getTasksForDate(
      allTasks,
      occurrences,
      nextOccurrence.date
    );
    const startMins = TimeUtils.parseTimeToMinutes(startTime);
    const endMins = TimeUtils.parseTimeToMinutes(endTime);
    return schedulingService.findConflicts(
      tasksOnDate,
      initialTask?.id || 0,
      startMins,
      endMins,
      isAllDay
    );
  }, [allTasks, nextOccurrence, startTime, endTime, isAllDay, initialTask]);

  const toggleDay = (day: DayOfWeek) => {
    if (recurringDays.includes(day)) {
      if (recurringDays.length > 1) {
        setRecurringDays(recurringDays.filter((d) => d !== day));
      }
    } else {
      setRecurringDays([...recurringDays, day]);
    }
  };

  const handleApplyTemplate = (tmpl: TaskTemplate) => {
    setName(tmpl.name);
    setDescription(tmpl.description);
    setCategoryId(tmpl.categoryId);
    setPriority(tmpl.priority);
    setIsAllDay(tmpl.isAllDay);
    if (tmpl.startTime !== null && tmpl.startTime !== undefined) {
      setStartTime(TimeUtils.minutesToInputTime(tmpl.startTime));
    }
    if (tmpl.endTime !== null && tmpl.endTime !== undefined) {
      setEndTime(TimeUtils.minutesToInputTime(tmpl.endTime));
    }
    setIsRecurring(tmpl.recurrenceType !== 'NONE');
    setRecurrenceType(tmpl.recurrenceType);
    if (tmpl.recurringDays?.length > 0) {
      setRecurringDays(tmpl.recurringDays);
    }
    setReminderEnabled(tmpl.reminderEnabled);
    setReminderOffsetMinutes(tmpl.reminderOffsetMinutes);
  };

  const handleSaveClick = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    if (isEditing && isRecurring) {
      setShowEditModeDialog(true);
    } else {
      onSave(draftTask, 'ENTIRE_SERIES');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs overflow-y-auto animate-in fade-in">
      <div className="bg-white dark:bg-[#111827] w-full max-w-lg rounded-3xl p-6 shadow-2xl border border-slate-200 dark:border-slate-800 my-8 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">
              {isEditing ? 'Edit Routine' : 'Create Routine'}
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {isRecurring ? 'Recurring schedule' : 'One-time task'}
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Templates Bar */}
        {templates.length > 0 && !isEditing && (
          <div className="mt-4 p-3 rounded-2xl bg-blue-50/70 dark:bg-blue-950/20 border border-blue-200/50 dark:border-blue-900/40">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-bold text-blue-700 dark:text-blue-300 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5" />
                Quick Routine Templates
              </span>
            </div>
            <div className="flex gap-2 overflow-x-auto pb-1">
              {templates.map((tmpl) => (
                <button
                  key={tmpl.id}
                  type="button"
                  onClick={() => handleApplyTemplate(tmpl)}
                  className="px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 border border-slate-200 dark:border-slate-700 hover:border-blue-500 shadow-2xs transition-all"
                >
                  {tmpl.name}
                </button>
              ))}
            </div>
          </div>
        )}

        <form onSubmit={handleSaveClick} className="space-y-4 mt-4">
          {/* Routine Name */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1">
              Routine Name *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Morning Workout or Deep Focus Block"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm font-semibold text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1">
              Description / Action Steps
            </label>
            <textarea
              rows={2}
              placeholder="Optional notes, checklist, or instructions"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3.5 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
            />
          </div>

          {/* Category & Priority Row */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1">
                Category
              </label>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(Number(e.target.value))}
                className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs font-semibold text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1">
                Priority
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as TaskPriority)}
                className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs font-semibold text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>
          </div>

          {/* Time & Duration */}
          <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-800 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-slate-400" />
                Schedule Time
              </span>
              <label className="flex items-center gap-2 cursor-pointer">
                <span className="text-xs text-slate-500 dark:text-slate-400">All Day</span>
                <input
                  type="checkbox"
                  checked={isAllDay}
                  onChange={(e) => setIsAllDay(e.target.checked)}
                  className="rounded text-blue-600 focus:ring-blue-500 w-4 h-4"
                />
              </label>
            </div>

            {!isAllDay && (
              <div className="grid grid-cols-2 gap-3 pt-1">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1">
                    Start Time
                  </label>
                  <input
                    type="time"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-semibold text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1">
                    End Time
                  </label>
                  <input
                    type="time"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-semibold text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>
            )}
          </div>

          {/* Recurrence Engine */}
          <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-800 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <Repeat className="w-3.5 h-3.5 text-slate-400" />
                Recurrence Rule
              </span>
              <label className="flex items-center gap-2 cursor-pointer">
                <span className="text-xs text-slate-500 dark:text-slate-400">Recurring Routine</span>
                <input
                  type="checkbox"
                  checked={isRecurring}
                  onChange={(e) => setIsRecurring(e.target.checked)}
                  className="rounded text-blue-600 focus:ring-blue-500 w-4 h-4"
                />
              </label>
            </div>

            {isRecurring && (
              <>
                <div className="grid grid-cols-2 gap-3 pt-1">
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1">
                      Pattern
                    </label>
                    <select
                      value={recurrenceType}
                      onChange={(e) => setRecurrenceType(e.target.value as RecurrenceType)}
                      className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-semibold text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="DAILY">Daily (Every Day)</option>
                      <option value="WEEKDAYS">Weekdays (Mon - Fri)</option>
                      <option value="WEEKENDS">Weekends Only</option>
                      <option value="SPECIFIC_DAYS">Specific Days of Week</option>
                      <option value="EVERY_X_DAYS">Every X Days</option>
                      <option value="EVERY_X_WEEKS">Every X Weeks</option>
                      <option value="MONTHLY">Monthly</option>
                      <option value="YEARLY">Yearly</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1">
                      Interval ({recurrenceType === 'EVERY_X_WEEKS' ? 'Weeks' : 'Days'})
                    </label>
                    <input
                      type="number"
                      min={1}
                      max={90}
                      value={recurrenceInterval}
                      onChange={(e) => setRecurrenceInterval(Math.max(1, Number(e.target.value)))}
                      className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-semibold text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                </div>

                {/* Specific Days Selector */}
                {(recurrenceType === 'SPECIFIC_DAYS' || recurrenceType === 'EVERY_X_WEEKS') && (
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1.5">
                      Active Days
                    </label>
                    <div className="flex gap-1.5">
                      {DAYS_OF_WEEK.map((day) => {
                        const active = recurringDays.includes(day);
                        return (
                          <button
                            key={day}
                            type="button"
                            onClick={() => toggleDay(day)}
                            className={`flex-1 py-1.5 rounded-lg text-[11px] font-bold transition-all ${
                              active
                                ? 'bg-blue-600 text-white shadow-2xs'
                                : 'bg-slate-200/70 dark:bg-slate-700/70 text-slate-600 dark:text-slate-300 hover:bg-slate-300 dark:hover:bg-slate-600'
                            }`}
                          >
                            {day.slice(0, 1)}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* Recurrence End Conditions */}
                <div className="pt-2 border-t border-slate-200/60 dark:border-slate-700/60">
                  <label className="block text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1">
                    Recurrence End
                  </label>
                  <div className="grid grid-cols-3 gap-2">
                    <button
                      type="button"
                      onClick={() => setRecurrenceEndType('NEVER')}
                      className={`py-1.5 rounded-xl text-xs font-semibold transition-all ${
                        recurrenceEndType === 'NEVER'
                          ? 'bg-blue-600 text-white font-bold'
                          : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
                      }`}
                    >
                      Never
                    </button>
                    <button
                      type="button"
                      onClick={() => setRecurrenceEndType('ON_DATE')}
                      className={`py-1.5 rounded-xl text-xs font-semibold transition-all ${
                        recurrenceEndType === 'ON_DATE'
                          ? 'bg-blue-600 text-white font-bold'
                          : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
                      }`}
                    >
                      On Date
                    </button>
                    <button
                      type="button"
                      onClick={() => setRecurrenceEndType('AFTER_OCCURRENCES')}
                      className={`py-1.5 rounded-xl text-xs font-semibold transition-all ${
                        recurrenceEndType === 'AFTER_OCCURRENCES'
                          ? 'bg-blue-600 text-white font-bold'
                          : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
                      }`}
                    >
                      After Count
                    </button>
                  </div>

                  {recurrenceEndType === 'ON_DATE' && (
                    <div className="mt-2">
                      <input
                        type="date"
                        value={recurrenceEndDate}
                        onChange={(e) => setRecurrenceEndDate(e.target.value)}
                        className="w-full px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-semibold"
                      />
                    </div>
                  )}

                  {recurrenceEndType === 'AFTER_OCCURRENCES' && (
                    <div className="mt-2">
                      <input
                        type="number"
                        min={1}
                        max={365}
                        value={recurrenceCount}
                        onChange={(e) => setRecurrenceCount(Number(e.target.value))}
                        placeholder="Number of occurrences"
                        className="w-full px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-semibold"
                      />
                    </div>
                  )}
                </div>
              </>
            )}
          </div>

          {/* Reminders */}
          <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-800 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <Bell className="w-3.5 h-3.5 text-slate-400" />
                On-Device Reminder Alert
              </span>
              <input
                type="checkbox"
                checked={reminderEnabled}
                onChange={(e) => setReminderEnabled(e.target.checked)}
                className="rounded text-blue-600 focus:ring-blue-500 w-4 h-4 cursor-pointer"
              />
            </div>

            {reminderEnabled && (
              <div className="flex gap-2 pt-1 overflow-x-auto">
                {[0, 5, 10, 15, 30].map((offset) => (
                  <button
                    key={offset}
                    type="button"
                    onClick={() => setReminderOffsetMinutes(offset)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                      reminderOffsetMinutes === offset
                        ? 'bg-blue-600 text-white'
                        : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
                    }`}
                  >
                    {offset === 0 ? 'At start' : `${offset}m before`}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Next Occurrence & Conflict Warnings */}
          <div className="space-y-2">
            {nextOccurrence && (
              <div className="flex items-center gap-2 px-3 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-xs text-slate-600 dark:text-slate-300 font-medium">
                <CalendarDays className="w-4 h-4 text-blue-500 shrink-0" />
                <span>Next planned occurrence: <strong>{nextOccurrence.formattedLabel}</strong></span>
              </div>
            )}

            {conflicts.length > 0 && (
              <div className="px-3.5 py-2.5 rounded-xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-900 text-xs text-amber-800 dark:text-amber-200 space-y-1">
                <div className="font-bold flex items-center gap-1.5">
                  <AlertTriangle className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                  Schedule Conflict Detected
                </div>
                {conflicts.map((c, i) => (
                  <p key={i} className="text-[11px]">
                    • {c.message}
                  </p>
                ))}
              </div>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-between pt-4 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              onClick={() => setShowTemplateModal(true)}
              className="text-xs font-bold text-slate-500 dark:text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 flex items-center gap-1.5"
            >
              <BookmarkCheck className="w-4 h-4" />
              Save as Template
            </button>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 shadow-xs transition-all"
              >
                {isEditing ? 'Save Changes' : 'Create Routine'}
              </button>
            </div>
          </div>
        </form>

        {/* Save As Template Modal */}
        {showTemplateModal && (
          <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
            <div className="bg-white dark:bg-[#1F2937] w-full max-w-sm rounded-2xl p-5 shadow-2xl border border-slate-200 dark:border-slate-700 animate-in zoom-in-95">
              <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
                Save as Routine Template
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Store this routine structure to quickly reuse it in the future.
              </p>
              <input
                type="text"
                placeholder={name || 'Template Name'}
                value={templateNameInput}
                onChange={(e) => setTemplateNameInput(e.target.value)}
                className="w-full mt-3 px-3.5 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs font-semibold text-slate-900 dark:text-slate-100"
              />
              <div className="flex justify-end gap-2 mt-4">
                <button
                  type="button"
                  onClick={() => setShowTemplateModal(false)}
                  className="px-3 py-1.5 rounded-xl text-xs font-bold text-slate-600 dark:text-slate-300"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => {
                    onSaveTemplate(templateNameInput || name, draftTask);
                    setShowTemplateModal(false);
                  }}
                  className="px-4 py-1.5 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700"
                >
                  Save Template
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Recurrence Edit Mode Confirmation Dialog */}
        {showEditModeDialog && (
          <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
            <div className="bg-white dark:bg-[#1F2937] w-full max-w-sm rounded-2xl p-5 shadow-2xl border border-slate-200 dark:border-slate-700 animate-in zoom-in-95 space-y-3">
              <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
                Update Recurring Routine
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Choose which occurrences you want to apply your changes to:
              </p>

              <div className="space-y-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowEditModeDialog(false);
                    onSave(draftTask, 'ENTIRE_SERIES');
                  }}
                  className="w-full p-3 rounded-xl text-left bg-slate-50 dark:bg-slate-800 hover:bg-blue-50 dark:hover:bg-blue-950/40 border border-slate-200 dark:border-slate-700 transition-all"
                >
                  <div className="text-xs font-bold text-slate-900 dark:text-slate-100">
                    All Occurrences in Series
                  </div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">
                    Update all past and future routines in this series
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setShowEditModeDialog(false);
                    onSave(draftTask, 'THIS_AND_FUTURE');
                  }}
                  className="w-full p-3 rounded-xl text-left bg-slate-50 dark:bg-slate-800 hover:bg-blue-50 dark:hover:bg-blue-950/40 border border-slate-200 dark:border-slate-700 transition-all"
                >
                  <div className="text-xs font-bold text-slate-900 dark:text-slate-100">
                    This and Future Occurrences
                  </div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">
                    Split series starting today without altering past records
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setShowEditModeDialog(false);
                    onSave(draftTask, 'THIS_OCCURRENCE');
                  }}
                  className="w-full p-3 rounded-xl text-left bg-slate-50 dark:bg-slate-800 hover:bg-blue-50 dark:hover:bg-blue-950/40 border border-slate-200 dark:border-slate-700 transition-all"
                >
                  <div className="text-xs font-bold text-slate-900 dark:text-slate-100">
                    This Occurrence Only
                  </div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">
                    Save as a one-time adjustment exception for today
                  </div>
                </button>
              </div>

              <div className="flex justify-end pt-2">
                <button
                  type="button"
                  onClick={() => setShowEditModeDialog(false)}
                  className="px-4 py-1.5 rounded-xl text-xs font-bold text-slate-600 dark:text-slate-300"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
