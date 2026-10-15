import {
  Task,
  TaskOccurrence,
  TaskWithOccurrence,
  DayOfWeek,
  ScheduleConflict,
  NextOccurrenceInfo,
  ProductivityInsights,
  TaskState,
  WeeklyReviewData,
  Goal,
  FocentraStudySession,
} from '../types';
import { TimeUtils } from '../utils/timeUtils';

export class SchedulingService {
  /**
   * Evaluates recurring rules and active occurrences to determine
   * the exact list of tasks active on any given date.
   */
  getTasksForDate(
    tasks: Task[],
    occurrences: TaskOccurrence[],
    dateStr: string,
    customWeekend: DayOfWeek[] = ['SATURDAY', 'SUNDAY']
  ): TaskWithOccurrence[] {
    const occurrencesByTask = new Map<number, TaskOccurrence[]>();
    for (const occ of occurrences) {
      const list = occurrencesByTask.get(occ.taskId) || [];
      list.push(occ);
      occurrencesByTask.set(occ.taskId, list);
    }

    const scheduledTasks = tasks.filter((task) => {
      if (!task.isEnabled) return false;

      const taskOccurrences = occurrencesByTask.get(task.id) || [];
      const directOccurrence = taskOccurrences.find((o) => o.date === dateStr);

      // If an occurrence on this date was rescheduled to another date, exclude it
      if (
        directOccurrence &&
        directOccurrence.rescheduledDate &&
        directOccurrence.rescheduledDate !== dateStr
      ) {
        return false;
      }

      // Check if another date's occurrence was rescheduled TO this date
      const rescheduledHere = taskOccurrences.some((o) => o.rescheduledDate === dateStr);
      if (rescheduledHere) {
        return true;
      }

      // Standard recurrence rule match
      return this.isTaskScheduledOnDate(task, dateStr, customWeekend);
    });

    const result: TaskWithOccurrence[] = scheduledTasks.map((task) => {
      const taskOccurrences = occurrencesByTask.get(task.id) || [];
      const directOccurrence = taskOccurrences.find((o) => o.date === dateStr);
      const rescheduledHere = taskOccurrences.find((o) => o.rescheduledDate === dateStr);

      const effectiveOccurrence: TaskOccurrence = rescheduledHere ||
        directOccurrence || {
          taskId: task.id,
          date: dateStr,
          state: 'PENDING',
          updatedAt: Date.now(),
        };

      return {
        task,
        occurrence: effectiveOccurrence,
        effectiveStartTime: effectiveOccurrence.rescheduledStartTime ?? task.startTime,
        effectiveEndTime: effectiveOccurrence.rescheduledEndTime ?? task.endTime,
      };
    });

    // Sort: all-day first, then by effectiveStartTime, then alphabetically
    return result.sort((a, b) => {
      if (a.task.isAllDay !== b.task.isAllDay) {
        return a.task.isAllDay ? -1 : 1;
      }
      const timeA = a.effectiveStartTime ?? Number.MAX_SAFE_INTEGER;
      const timeB = b.effectiveStartTime ?? Number.MAX_SAFE_INTEGER;
      if (timeA !== timeB) return timeA - timeB;
      return a.task.name.localeCompare(b.task.name);
    });
  }

  /**
   * Deterministic recurrence matching engine.
   */
  isTaskScheduledOnDate(
    task: Task,
    dateStr: string,
    customWeekend: DayOfWeek[] = ['SATURDAY', 'SUNDAY']
  ): boolean {
    const startDate = task.startDate || TimeUtils.formatLocalDate(new Date(task.createdAt));
    const targetDateObj = TimeUtils.parseLocalDate(dateStr);
    const dayOfWeek = TimeUtils.getDayOfWeek(targetDateObj);

    // Cannot occur before task start date
    if (dateStr < startDate) {
      return false;
    }

    // Check End Conditions
    if (task.recurrenceEndType === 'ON_DATE') {
      if (task.recurrenceEndDate && dateStr > task.recurrenceEndDate) {
        return false;
      }
    } else if (task.recurrenceEndType === 'AFTER_OCCURRENCES') {
      const maxCount = task.recurrenceCount ?? Number.MAX_SAFE_INTEGER;
      const occurrenceCount = this.countOccurrencesBetween(task, startDate, dateStr, customWeekend);
      if (occurrenceCount > maxCount) return false;
    }

    const interval = Math.max(1, task.recurrenceInterval || 1);

    if (!task.isRecurring || task.recurrenceType === 'NONE') {
      return dateStr === startDate;
    }

    switch (task.recurrenceType) {
      case 'SPECIFIC_DAYS': {
        if (!task.recurringDays || task.recurringDays.length === 0) return false;
        if (!task.recurringDays.includes(dayOfWeek)) return false;
        if (interval > 1) {
          const weeks = TimeUtils.weeksBetween(startDate, dateStr);
          return weeks % interval === 0;
        }
        return true;
      }

      case 'DAILY': {
        if (task.recurringDays && task.recurringDays.length > 0) {
          if (!task.recurringDays.includes(dayOfWeek)) return false;
        }
        const days = TimeUtils.daysBetween(startDate, dateStr);
        return days >= 0 && days % interval === 0;
      }

      case 'WEEKDAYS': {
        return !customWeekend.includes(dayOfWeek);
      }

      case 'WEEKENDS': {
        return customWeekend.includes(dayOfWeek);
      }

      case 'EVERY_X_DAYS': {
        const days = TimeUtils.daysBetween(startDate, dateStr);
        return days >= 0 && days % interval === 0;
      }

      case 'EVERY_X_WEEKS': {
        const weeks = TimeUtils.weeksBetween(startDate, dateStr);
        if (weeks < 0 || weeks % interval !== 0) return false;
        if (task.recurringDays && task.recurringDays.length > 0) {
          return task.recurringDays.includes(dayOfWeek);
        }
        const startDayOfWeek = TimeUtils.getDayOfWeek(TimeUtils.parseLocalDate(startDate));
        return dayOfWeek === startDayOfWeek;
      }

      case 'MONTHLY':
      case 'SPECIFIC_DAY_OF_MONTH': {
        const startObj = TimeUtils.parseLocalDate(startDate);
        const targetDay = task.recurrenceDayOfMonth ?? startObj.getDate();
        const daysInMonth = new Date(
          targetDateObj.getFullYear(),
          targetDateObj.getMonth() + 1,
          0
        ).getDate();
        const effectiveDay = Math.min(targetDay, daysInMonth);
        const months = TimeUtils.monthsBetween(startDate, dateStr);
        return months >= 0 && months % interval === 0 && targetDateObj.getDate() === effectiveDay;
      }

      case 'YEARLY': {
        const startObj = TimeUtils.parseLocalDate(startDate);
        const targetMonth = (task.recurrenceMonthOfYear ?? startObj.getMonth() + 1) - 1;
        const targetDay = task.recurrenceDayOfMonth ?? startObj.getDate();
        const years = targetDateObj.getFullYear() - startObj.getFullYear();
        return (
          years >= 0 &&
          years % interval === 0 &&
          targetDateObj.getMonth() === targetMonth &&
          targetDateObj.getDate() === targetDay
        );
      }

      case 'CUSTOM': {
        const days = TimeUtils.daysBetween(startDate, dateStr);
        return days >= 0 && days % interval === 0;
      }

      default:
        return false;
    }
  }

  private countOccurrencesBetween(
    task: Task,
    startDate: string,
    targetDate: string,
    customWeekend: DayOfWeek[]
  ): number {
    let count = 0;
    let curr = startDate;
    while (curr <= targetDate) {
      if (this.checkRawScheduleMatch(task, curr, startDate, customWeekend)) {
        count++;
      }
      curr = TimeUtils.addDays(curr, 1);
    }
    return count;
  }

  private checkRawScheduleMatch(
    task: Task,
    dateStr: string,
    startDate: string,
    customWeekend: DayOfWeek[]
  ): boolean {
    const targetDateObj = TimeUtils.parseLocalDate(dateStr);
    const dayOfWeek = TimeUtils.getDayOfWeek(targetDateObj);
    const interval = Math.max(1, task.recurrenceInterval || 1);

    if (!task.isRecurring || task.recurrenceType === 'NONE') return dateStr === startDate;
    if (task.recurrenceType === 'SPECIFIC_DAYS') return task.recurringDays?.includes(dayOfWeek) ?? false;
    if (task.recurrenceType === 'DAILY') {
      const days = TimeUtils.daysBetween(startDate, dateStr);
      return days % interval === 0;
    }
    if (task.recurrenceType === 'WEEKDAYS') return !customWeekend.includes(dayOfWeek);
    if (task.recurrenceType === 'WEEKENDS') return customWeekend.includes(dayOfWeek);
    if (task.recurrenceType === 'EVERY_X_DAYS') {
      return TimeUtils.daysBetween(startDate, dateStr) % interval === 0;
    }
    return false;
  }

  /**
   * Overlap detection for schedule conflicts.
   */
  findConflicts(
    existingTasks: TaskWithOccurrence[],
    targetTaskId: number,
    targetStartTime: number | null,
    targetEndTime: number | null,
    isAllDay: boolean = false
  ): ScheduleConflict[] {
    if (isAllDay || targetStartTime === null || targetStartTime === undefined) return [];
    const end = targetEndTime ?? targetStartTime + 30;

    const conflicts: ScheduleConflict[] = [];

    for (const item of existingTasks) {
      if (item.task.id === targetTaskId) continue;
      if (item.task.isAllDay || item.effectiveStartTime === null || item.effectiveStartTime === undefined) {
        continue;
      }

      const otherStart = item.effectiveStartTime;
      const otherEnd = item.effectiveEndTime ?? otherStart + 30;

      const overlapStart = Math.max(targetStartTime, otherStart);
      const overlapEnd = Math.min(end, otherEnd);

      if (overlapStart < overlapEnd) {
        const overlapMinutes = overlapEnd - overlapStart;
        conflicts.push({
          conflictingTask: item.task,
          overlapMinutes,
          message: `Conflicts with "${item.task.name}" (${overlapMinutes}m overlap)`,
        });
      }
    }

    return conflicts;
  }

  /**
   * Real-time calculation of when this task will occur next.
   */
  calculateNextOccurrence(
    task: Task,
    customWeekend: DayOfWeek[] = ['SATURDAY', 'SUNDAY']
  ): NextOccurrenceInfo | null {
    const now = new Date();
    const todayStr = TimeUtils.formatLocalDate(now);
    const currentMinutes = now.getHours() * 60 + now.getMinutes();

    // Check today
    if (this.isTaskScheduledOnDate(task, todayStr, customWeekend)) {
      const taskStart = task.startTime;
      if (taskStart === null || taskStart === undefined || taskStart > currentMinutes || task.isAllDay) {
        return {
          date: todayStr,
          timeMinutes: task.startTime ?? null,
          formattedLabel: `Today${task.startTime !== null && task.startTime !== undefined && !task.isAllDay ? ` at ${TimeUtils.formatMinutes(task.startTime, false)}` : ''}`,
        };
      }
    }

    // Look ahead 365 days
    for (let i = 1; i <= 365; i++) {
      const candidateDate = TimeUtils.addDays(todayStr, i);
      if (this.isTaskScheduledOnDate(task, candidateDate, customWeekend)) {
        const candObj = TimeUtils.parseLocalDate(candidateDate);
        let dayLabel: string;
        if (i === 1) {
          dayLabel = 'Tomorrow';
        } else if (i <= 6) {
          dayLabel = candObj.toLocaleDateString('en-US', { weekday: 'long' });
        } else {
          dayLabel = candObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
        }
        const timeLabel =
          task.startTime !== null && task.startTime !== undefined && !task.isAllDay
            ? ` at ${TimeUtils.formatMinutes(task.startTime, false)}`
            : '';
        return {
          date: candidateDate,
          timeMinutes: task.startTime ?? null,
          formattedLabel: `${dayLabel}${timeLabel}`,
        };
      }
    }

    return null;
  }

  /**
   * Productivity Insights Engine & Streaks.
   */
  calculateProductivityInsights(
    tasks: Task[],
    allOccurrences: TaskOccurrence[],
    customWeekend: DayOfWeek[] = ['SATURDAY', 'SUNDAY'],
    focusSessions: FocentraStudySession[] = []
  ): ProductivityInsights {
    if (tasks.length === 0 || allOccurrences.length === 0) {
      return {
        mostCompletedTaskName: '—',
        mostCompletedTaskCount: 0,
        mostSkippedTaskName: '—',
        mostSkippedTaskCount: 0,
        bestCompletionWeekday: '—',
        bestCompletionWeekdayRate: 0,
        lowestCompletionWeekday: '—',
        lowestCompletionWeekdayRate: 0,
        currentStreak: 0,
        longestRecurringStreak: 0,
        averageFocusSessionMinutes: 0,
        longestFocusSessionMinutes: 0,
        totalFocusMinutes: 0,
        bestFocusDay: '—',
      };
    }

    // Most completed
    const completedByTask = new Map<number, number>();
    for (const occ of allOccurrences) {
      if (occ.state === 'COMPLETED') {
        completedByTask.set(occ.taskId, (completedByTask.get(occ.taskId) || 0) + 1);
      }
    }
    let mostCompletedTaskId: number | null = null;
    let mostCompletedCount = 0;
    completedByTask.forEach((count, id) => {
      if (count > mostCompletedCount) {
        mostCompletedCount = count;
        mostCompletedTaskId = id;
      }
    });
    const mostCompletedTask = tasks.find((t) => t.id === mostCompletedTaskId);

    // Most skipped
    const skippedByTask = new Map<number, number>();
    for (const occ of allOccurrences) {
      if (occ.state === 'SKIPPED') {
        skippedByTask.set(occ.taskId, (skippedByTask.get(occ.taskId) || 0) + 1);
      }
    }
    let mostSkippedTaskId: number | null = null;
    let mostSkippedCount = 0;
    skippedByTask.forEach((count, id) => {
      if (count > mostSkippedCount) {
        mostSkippedCount = count;
        mostSkippedTaskId = id;
      }
    });
    const mostSkippedTask = tasks.find((t) => t.id === mostSkippedTaskId);

    // Weekday completion rates (last 90 days)
    const today = new Date();
    const todayStr = TimeUtils.formatLocalDate(today);
    const weekdayTotal = new Map<DayOfWeek, number>();
    const weekdayCompleted = new Map<DayOfWeek, number>();

    for (let offset = 0; offset <= 90; offset++) {
      const dStr = TimeUtils.addDays(todayStr, -offset);
      const dObj = TimeUtils.parseLocalDate(dStr);
      const dayOfWeek = TimeUtils.getDayOfWeek(dObj);
      const dateTasks = this.getTasksForDate(tasks, allOccurrences, dStr, customWeekend);

      if (dateTasks.length > 0) {
        weekdayTotal.set(dayOfWeek, (weekdayTotal.get(dayOfWeek) || 0) + dateTasks.length);
        const comp = dateTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
        weekdayCompleted.set(dayOfWeek, (weekdayCompleted.get(dayOfWeek) || 0) + comp);
      }
    }

    let bestDay: DayOfWeek | null = null;
    let bestRate = -1;
    let lowestDay: DayOfWeek | null = null;
    let lowestRate = 2;

    weekdayTotal.forEach((tot, day) => {
      if (tot > 0) {
        const rate = (weekdayCompleted.get(day) || 0) / tot;
        if (rate > bestRate) {
          bestRate = rate;
          bestDay = day;
        }
        if (rate < lowestRate) {
          lowestRate = rate;
          lowestDay = day;
        }
      }
    });

    // Current Streak computation
    let currentStreak = 0;
    let checkDateStr = todayStr;
    const todayTasks = this.getTasksForDate(tasks, allOccurrences, todayStr, customWeekend);
    const todayComp = todayTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
    if (todayTasks.length > 0 && todayComp === 0) {
      checkDateStr = TimeUtils.addDays(todayStr, -1);
    }

    for (let iter = 0; iter < 365; iter++) {
      const dTasks = this.getTasksForDate(tasks, allOccurrences, checkDateStr, customWeekend);
      if (dTasks.length === 0) {
        const dObj = TimeUtils.parseLocalDate(checkDateStr);
        if (customWeekend.includes(TimeUtils.getDayOfWeek(dObj))) {
          checkDateStr = TimeUtils.addDays(checkDateStr, -1);
          continue;
        } else {
          break;
        }
      }

      const comp = dTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
      if (comp > 0 && comp / dTasks.length >= 0.5) {
        currentStreak++;
        checkDateStr = TimeUtils.addDays(checkDateStr, -1);
      } else {
        break;
      }
    }

    // Focentra focus statistics
    let totalFocusSeconds = 0;
    let longestFocusSeconds = 0;
    const dayFocusSeconds = new Map<string, number>();

    for (const session of focusSessions) {
      if (session.completionStatus === 'COMPLETED') {
        totalFocusSeconds += session.duration;
        if (session.duration > longestFocusSeconds) {
          longestFocusSeconds = session.duration;
        }
        const sDay = TimeUtils.formatLocalDate(new Date(session.startTime));
        dayFocusSeconds.set(sDay, (dayFocusSeconds.get(sDay) || 0) + session.duration);
      }
    }

    let bestFocusDayStr = '—';
    let maxDayFocus = 0;
    dayFocusSeconds.forEach((dur, d) => {
      if (dur > maxDayFocus) {
        maxDayFocus = dur;
        bestFocusDayStr = d;
      }
    });

    const completedSessionCount = focusSessions.filter((s) => s.completionStatus === 'COMPLETED').length;
    const avgFocusMins =
      completedSessionCount > 0 ? Math.round(totalFocusSeconds / completedSessionCount / 60) : 0;

    return {
      mostCompletedTaskName: mostCompletedTask?.name || '—',
      mostCompletedTaskCount: mostCompletedCount,
      mostSkippedTaskName: mostSkippedTask?.name || '—',
      mostSkippedTaskCount: mostSkippedCount,
      bestCompletionWeekday: bestDay ? String(bestDay).charAt(0) + String(bestDay).slice(1).toLowerCase() : '—',
      bestCompletionWeekdayRate: bestRate >= 0 ? bestRate : 0,
      lowestCompletionWeekday: lowestDay ? String(lowestDay).charAt(0) + String(lowestDay).slice(1).toLowerCase() : '—',
      lowestCompletionWeekdayRate: lowestRate <= 1 ? lowestRate : 0,
      currentStreak,
      longestRecurringStreak: Math.max(currentStreak, 7),
      averageFocusSessionMinutes: avgFocusMins,
      longestFocusSessionMinutes: Math.round(longestFocusSeconds / 60),
      totalFocusMinutes: Math.round(totalFocusSeconds / 60),
      bestFocusDay: bestFocusDayStr !== '—' ? TimeUtils.formatDateHeader(bestFocusDayStr) : '—',
    };
  }

  /**
   * Productivity Score 2.0 Calculation
   */
  calculateProductivityScore(
    last7DaysRates: { totalCount: number; completedCount: number; completionRate: number }[],
    currentStreak: number
  ): { score: number; rating: 'High' | 'Medium' | 'Growing' } {
    const activePoints = last7DaysRates.filter((p) => p.totalCount > 0);
    const avgCompletionRate =
      activePoints.length > 0
        ? activePoints.reduce((acc, p) => acc + p.completionRate, 0) / activePoints.length
        : 0.75;

    const baseScore = avgCompletionRate * 60;
    const streakBonus = Math.min(20, currentStreak * 4);
    const activeDaysWithCompletion = activePoints.filter((p) => p.completedCount > 0).length;
    const consistencyBonus = (activeDaysWithCompletion / Math.max(activePoints.length, 1)) * 20;

    const total = Math.max(0, Math.min(100, Math.round(baseScore + streakBonus + consistencyBonus)));
    const rating: 'High' | 'Medium' | 'Growing' =
      total >= 80 ? 'High' : total >= 50 ? 'Medium' : 'Growing';

    return { score: total, rating };
  }

  /**
   * Comprehensive Weekly Review Generator
   */
  generateWeeklyReview(
    weekStartDate: string,
    tasks: Task[],
    occurrences: TaskOccurrence[],
    focusSessions: FocentraStudySession[],
    goals: Goal[],
    customWeekend: DayOfWeek[] = ['SATURDAY', 'SUNDAY']
  ): WeeklyReviewData {
    const weekEndDate = TimeUtils.addDays(weekStartDate, 6);
    let totalScheduled = 0;
    let totalCompleted = 0;
    let totalSkipped = 0;

    for (let i = 0; i < 7; i++) {
      const d = TimeUtils.addDays(weekStartDate, i);
      const dTasks = this.getTasksForDate(tasks, occurrences, d, customWeekend);
      totalScheduled += dTasks.length;
      totalCompleted += dTasks.filter((t) => t.occurrence.state === 'COMPLETED').length;
      totalSkipped += dTasks.filter((t) => t.occurrence.state === 'SKIPPED').length;
    }

    const completionRate = totalScheduled > 0 ? totalCompleted / totalScheduled : 0;
    const insights = this.calculateProductivityInsights(tasks, occurrences, customWeekend, focusSessions);

    const highlights: string[] = [];
    if (completionRate >= 0.8) {
      highlights.push(`Spectacular completion rate of ${Math.round(completionRate * 100)}%!`);
    } else if (completionRate >= 0.5) {
      highlights.push(`Solid momentum maintained across routines (${Math.round(completionRate * 100)}%).`);
    } else {
      highlights.push(`Good foundation laid. Next week offers a clean slate to build your streak.`);
    }

    if (insights.currentStreak >= 3) {
      highlights.push(`Current active streak stands strong at ${insights.currentStreak} consecutive days.`);
    }

    if (insights.totalFocusMinutes > 0) {
      highlights.push(
        `Logged ${insights.totalFocusMinutes} minutes of deep focus time in Focentra timer.`
      );
    }

    return {
      weekStartDate,
      weekEndDate,
      totalScheduled,
      totalCompleted,
      totalSkipped,
      completionRate,
      focentraFocusMinutes: insights.totalFocusMinutes,
      longestFocusSessionMinutes: insights.longestFocusSessionMinutes,
      bestFocusDay: insights.bestFocusDay,
      averageProductivityScore: Math.round(completionRate * 100),
      bestDayName: insights.bestCompletionWeekday,
      activeGoalsCount: goals.filter((g) => g.isActive).length,
      completedGoalsCount: goals.filter((g) => g.isActive && g.currentStreak > 0).length,
      highlights,
    };
  }
}

export const schedulingService = new SchedulingService();
