export type TaskState = 'PENDING' | 'COMPLETED' | 'SKIPPED';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type RecurrenceType =
  | 'NONE'
  | 'DAILY'
  | 'SPECIFIC_DAYS'
  | 'WEEKDAYS'
  | 'WEEKENDS'
  | 'EVERY_X_DAYS'
  | 'EVERY_X_WEEKS'
  | 'MONTHLY'
  | 'SPECIFIC_DAY_OF_MONTH'
  | 'YEARLY'
  | 'CUSTOM';

export type RecurrenceEndType = 'NEVER' | 'ON_DATE' | 'AFTER_OCCURRENCES';

export type RecurrenceEditMode = 'THIS_OCCURRENCE' | 'THIS_AND_FUTURE' | 'ENTIRE_SERIES';

export type GoalType = 'TASKS_COMPLETED' | 'FOCUS_MINUTES' | 'HIGH_PRIORITY_TASKS';

export type GoalPeriod = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export const DAYS_OF_WEEK: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

export interface Category {
  id: number;
  name: string;
  icon: string;
  color: number; // Hex number, e.g. 0xFF3B82F6
}

export interface Task {
  id: number;
  name: string;
  description: string;
  categoryId: number;
  icon?: string;
  priority: TaskPriority;
  deadlineDate?: string | null; // YYYY-MM-DD
  deadlineTime?: number | null; // Minutes since midnight
  startTime?: number | null; // Minutes since midnight
  endTime?: number | null; // Minutes since midnight
  isAllDay: boolean;
  isEnabled: boolean;
  isRecurring: boolean;
  recurringDays: DayOfWeek[];
  recurrenceType: RecurrenceType;
  recurrenceInterval: number;
  recurrenceDayOfMonth?: number | null;
  recurrenceMonthOfYear?: number | null;
  recurrenceEndType: RecurrenceEndType;
  recurrenceEndDate?: string | null;
  recurrenceCount?: number | null;
  seriesId?: number | null;
  startDate: string; // YYYY-MM-DD
  color?: number | null;
  notes?: string;
  reminderEnabled: boolean;
  reminderOffsetMinutes?: number | null; // 0, 5, 10, 15, 30
  createdAt: number;
  updatedAt: number;
}

export interface TaskOccurrence {
  taskId: number;
  date: string; // YYYY-MM-DD
  state: TaskState;
  rescheduledStartTime?: number | null;
  rescheduledEndTime?: number | null;
  rescheduledDate?: string | null;
  isException?: boolean;
  notes?: string | null;
  updatedAt: number;
}

export interface TaskWithOccurrence {
  task: Task;
  occurrence: TaskOccurrence;
  effectiveStartTime?: number | null;
  effectiveEndTime?: number | null;
}

export interface TaskTemplate {
  id: number;
  name: string;
  description: string;
  categoryId: number;
  priority: TaskPriority;
  defaultDurationMinutes: number;
  startTime?: number | null;
  endTime?: number | null;
  isAllDay: boolean;
  recurrenceType: RecurrenceType;
  recurringDays: DayOfWeek[];
  recurrenceInterval: number;
  reminderEnabled: boolean;
  reminderOffsetMinutes: number;
  createdAt: number;
}

export interface Goal {
  id: number;
  title: string;
  targetType: GoalType;
  targetPeriod: GoalPeriod;
  targetValue: number;
  unit: string;
  categoryId?: number | null;
  currentStreak: number;
  longestStreak: number;
  lastCompletedPeriod?: string | null;
  isActive: boolean;
  createdAt: number;
}

export interface GoalProgress {
  goal: Goal;
  currentValue: number;
  targetValue: number;
  percentage: number;
  isCompleted: boolean;
  periodLabel: string;
}

export interface ScheduleConflict {
  conflictingTask: Task;
  overlapMinutes: number;
  message: string;
}

export interface NextOccurrenceInfo {
  date: string; // YYYY-MM-DD
  timeMinutes: number | null;
  formattedLabel: string;
}

export interface ProductivityInsights {
  mostCompletedTaskName: string;
  mostCompletedTaskCount: number;
  mostSkippedTaskName: string;
  mostSkippedTaskCount: number;
  bestCompletionWeekday: string;
  bestCompletionWeekdayRate: number;
  lowestCompletionWeekday: string;
  lowestCompletionWeekdayRate: number;
  currentStreak: number;
  longestRecurringStreak: number;
  averageFocusSessionMinutes: number;
  longestFocusSessionMinutes: number;
  totalFocusMinutes: number;
  bestFocusDay: string;
}

export interface WeeklyReviewData {
  weekStartDate: string;
  weekEndDate: string;
  totalScheduled: number;
  totalCompleted: number;
  totalSkipped: number;
  completionRate: number;
  focentraFocusMinutes: number;
  longestFocusSessionMinutes: number;
  bestFocusDay: string;
  averageProductivityScore: number;
  bestDayName: string;
  activeGoalsCount: number;
  completedGoalsCount: number;
  highlights: string[];
}

export type AppQualityRating =
  | 'VERY_GOOD'
  | 'GOOD'
  | 'NEUTRAL'
  | 'NOT_GOOD'
  | 'BAD'
  | 'VERY_BAD'
  | 'UNRATED';

export type ProductivityType = 'PRODUCTIVE' | 'NON_PRODUCTIVE' | 'NEUTRAL';

export type AppSeverity = 'NORMAL' | 'HIGH' | 'FATAL';

export interface AppUsageInfo {
  packageName: string;
  appName: string;
  totalTimeInForegroundMillis: number;
  categories: string[];
  qualityRating: AppQualityRating;
  productivityType: ProductivityType;
  isUsageTracked: boolean;
  severity: AppSeverity;
  percentageOfTotal: number;
  lastTimeUsed: number;
}

export interface InstalledAppInfo {
  packageName: string;
  appName: string;
  categories: string[];
  qualityRating: AppQualityRating;
  productivityType: ProductivityType;
  isUsageTracked: boolean;
  severity: AppSeverity;
}

export interface DailyUsagePoint {
  dateLabel: string;
  fullDate: string;
  productiveMillis: number;
  nonProductiveMillis: number;
  neutralMillis: number;
  focentraFocusMillis: number;
  totalMillis: number;
}

export interface CategoryUsageBreakdown {
  category: string;
  durationMillis: number;
  percentage: number;
  count: number;
}

export interface AppCategoryItem {
  id: number;
  name: string;
  isProductive: boolean;
}

export interface DiagnosticSummary {
  startDateLabel: string;
  endDateLabel: string;
  totalUsageTimeMillis: number;
  productiveTimeMillis: number;
  nonProductiveTimeMillis: number;
  neutralTimeMillis: number;
  focentraFocusMillis: number;
  mostUsedApp: AppUsageInfo | null;
  appsCount: number;
  productiveAppsCount: number;
  nonProductiveAppsCount: number;
  productivityRate: number;
  mostProductiveDayName: string;
  topApps: AppUsageInfo[];
  dailyTrend: DailyUsagePoint[];
  categoryBreakdown: CategoryUsageBreakdown[];
}

export interface FocentraStudySession {
  sessionId: string;
  subject: string;
  topic: string;
  startTime: number;
  endTime: number;
  duration: number; // in seconds or minutes
  completionStatus: 'COMPLETED' | 'ABORTED' | 'PAUSED';
  focusScore: number;
  schemaVersion: number;
}

export interface FocentraIntegrationStatus {
  isInstalled: boolean;
  isConnected: boolean;
  hasConsent: boolean;
  schemaVersion: number;
  totalImportedSessions: number;
  lastSyncTimestamp: number;
}

export interface AppSettings {
  onboarding_completed: boolean;
  theme: 'SYSTEM' | 'LIGHT' | 'DARK';
  color_preset: 'COBALT' | 'EMERALD' | 'AMETHYST' | 'VIOLET' | 'ROSE' | 'AMBER';
  time_format: '24H' | '12H';
  week_start: DayOfWeek;
  weekend_days: DayOfWeek[];
  notifications_enabled: boolean;
  default_reminder_offset: number; // 0, 5, 10, 15, 30
}

export interface FullBackupBundle {
  version: number;
  exportedAt: string;
  app: 'Daynexa';
  data: {
    categories: Category[];
    tasks: Task[];
    occurrences: TaskOccurrence[];
    taskTemplates: TaskTemplate[];
    goals: Goal[];
    installedApps: InstalledAppInfo[];
    focentraSessions: FocentraStudySession[];
    settings: Record<string, string>;
  };
}

export interface FocentraExportBundle {
  version: number;
  app: 'Focentra';
  exportedAt: string;
  sessions: FocentraStudySession[];
}

export type BackupValidationResult =
  | { type: 'VALID_FULL'; bundle: FullBackupBundle; summary: string }
  | { type: 'VALID_FOCENTRA'; bundle: FocentraExportBundle; summary: string }
  | { type: 'INVALID'; error: string };

export type ImportMode = 'MERGE' | 'REPLACE';
