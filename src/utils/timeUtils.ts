import { DayOfWeek } from '../types';

export const TimeUtils = {
  // Format minutes since midnight (e.g. 480 -> 08:00 or 8:00 AM)
  formatMinutes(minutes: number | null | undefined, is24Hour: boolean = true): string {
    if (minutes === null || minutes === undefined) return 'All Day';
    const normalized = Math.floor(minutes % 1440);
    const hours = Math.floor(normalized / 60);
    const mins = normalized % 60;
    const minsStr = mins < 10 ? `0${mins}` : `${mins}`;

    if (is24Hour) {
      const hoursStr = hours < 10 ? `0${hours}` : `${hours}`;
      return `${hoursStr}:${minsStr}`;
    }

    const period = hours < 12 ? 'AM' : 'PM';
    const displayHour = hours === 0 ? 12 : hours > 12 ? hours - 12 : hours;
    return `${displayHour}:${minsStr} ${period}`;
  },

  // Parse "HH:MM" string to minutes since midnight
  parseTimeToMinutes(timeStr: string): number {
    if (!timeStr) return 0;
    const [h, m] = timeStr.split(':').map(Number);
    return (h || 0) * 60 + (m || 0);
  },

  // Convert minutes to "HH:MM" for HTML input[type="time"]
  minutesToInputTime(minutes: number | null | undefined): string {
    if (minutes === null || minutes === undefined) return '08:00';
    const normalized = Math.floor(minutes % 1440);
    const hours = Math.floor(normalized / 60);
    const mins = normalized % 60;
    return `${hours < 10 ? '0' : ''}${hours}:${mins < 10 ? '0' : ''}${mins}`;
  },

  // Format YYYY-MM-DD
  formatLocalDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  parseLocalDate(dateStr: string): Date {
    const [y, m, d] = dateStr.split('-').map(Number);
    return new Date(y, m - 1, d);
  },

  getDayGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  },

  formatDateHeader(dateStr: string): string {
    const d = this.parseLocalDate(dateStr);
    const today = this.formatLocalDate(new Date());
    const tomorrow = this.formatLocalDate(new Date(Date.now() + 86400000));
    const yesterday = this.formatLocalDate(new Date(Date.now() - 86400000));

    if (dateStr === today) {
      return `Today, ${d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}`;
    }
    if (dateStr === tomorrow) {
      return `Tomorrow, ${d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}`;
    }
    if (dateStr === yesterday) {
      return `Yesterday, ${d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}`;
    }

    return d.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: d.getFullYear() !== new Date().getFullYear() ? 'numeric' : undefined,
    });
  },

  getDayOfWeek(date: Date): DayOfWeek {
    const dayMap: DayOfWeek[] = [
      'SUNDAY',
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
    ];
    return dayMap[date.getDay()];
  },

  addDays(dateStr: string, days: number): string {
    const d = this.parseLocalDate(dateStr);
    d.setDate(d.getDate() + days);
    return this.formatLocalDate(d);
  },

  daysBetween(startStr: string, endStr: string): number {
    const start = this.parseLocalDate(startStr).getTime();
    const end = this.parseLocalDate(endStr).getTime();
    return Math.round((end - start) / (1000 * 60 * 60 * 24));
  },

  weeksBetween(startStr: string, endStr: string): number {
    return Math.floor(this.daysBetween(startStr, endStr) / 7);
  },

  monthsBetween(startStr: string, endStr: string): number {
    const start = this.parseLocalDate(startStr);
    const end = this.parseLocalDate(endStr);
    return (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth());
  },

  formatDuration(minutes: number): string {
    if (minutes < 60) return `${minutes}m`;
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return m > 0 ? `${h}h ${m}m` : `${h}h`;
  },
};
