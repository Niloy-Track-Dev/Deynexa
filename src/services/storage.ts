import {
  Category,
  Task,
  TaskOccurrence,
  TaskTemplate,
  Goal,
  InstalledAppInfo,
  FocentraStudySession,
  AppSettings,
  FullBackupBundle,
  FocentraExportBundle,
  BackupValidationResult,
  ImportMode,
} from '../types';
import { TimeUtils } from '../utils/timeUtils';

const STORAGE_KEYS = {
  CATEGORIES: 'daynexa_categories',
  TASKS: 'daynexa_tasks',
  OCCURRENCES: 'daynexa_occurrences',
  TEMPLATES: 'daynexa_templates',
  GOALS: 'daynexa_goals',
  APPS: 'daynexa_installed_apps',
  FOCENTRA_SESSIONS: 'daynexa_focentra_sessions',
  SETTINGS: 'daynexa_settings',
};

// Seed Categories matching Daynexa default
const DEFAULT_CATEGORIES: Category[] = [
  { id: 1, name: 'Personal', icon: 'person', color: 0xff8b5cf6 },
  { id: 2, name: 'Deep Work', icon: 'code', color: 0xff3b82f6 },
  { id: 3, name: 'Health & Fitness', icon: 'heart', color: 0xff10b981 },
  { id: 4, name: 'Learning', icon: 'book', color: 0xfff59e0b },
];

const todayStr = TimeUtils.formatLocalDate(new Date());

// Seed Tasks matching Daynexa routines
const DEFAULT_TASKS: Task[] = [
  {
    id: 1,
    name: 'Morning Hydration & Planning',
    description: 'Drink 500ml water and organize the day priority matrix',
    categoryId: 3,
    priority: 'HIGH',
    startTime: 420, // 07:00
    endTime: 450, // 07:30
    isAllDay: false,
    isEnabled: true,
    isRecurring: true,
    recurringDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
    recurrenceType: 'DAILY',
    recurrenceInterval: 1,
    recurrenceEndType: 'NEVER',
    startDate: todayStr,
    reminderEnabled: true,
    reminderOffsetMinutes: 0,
    createdAt: Date.now() - 86400000 * 7,
    updatedAt: Date.now(),
  },
  {
    id: 2,
    name: 'Deep Focus Work Block',
    description: 'Continuous uninterrupted sprint on primary objective',
    categoryId: 2,
    priority: 'CRITICAL',
    startTime: 540, // 09:00
    endTime: 660, // 11:00
    isAllDay: false,
    isEnabled: true,
    isRecurring: true,
    recurringDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
    recurrenceType: 'WEEKDAYS',
    recurrenceInterval: 1,
    recurrenceEndType: 'NEVER',
    startDate: todayStr,
    reminderEnabled: true,
    reminderOffsetMinutes: 10,
    createdAt: Date.now() - 86400000 * 7,
    updatedAt: Date.now(),
  },
  {
    id: 3,
    name: 'Evening Reflection & Review',
    description: 'Check off habits and prepare workspace for tomorrow',
    categoryId: 1,
    priority: 'MEDIUM',
    startTime: 1260, // 21:00
    endTime: 1290, // 21:30
    isAllDay: false,
    isEnabled: true,
    isRecurring: true,
    recurringDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
    recurrenceType: 'DAILY',
    recurrenceInterval: 1,
    recurrenceEndType: 'NEVER',
    startDate: todayStr,
    reminderEnabled: false,
    reminderOffsetMinutes: 0,
    createdAt: Date.now() - 86400000 * 7,
    updatedAt: Date.now(),
  },
];

const DEFAULT_TEMPLATES: TaskTemplate[] = [
  {
    id: 1,
    name: 'Deep Focus Block (90m)',
    description: 'Deep work sprint without distractions',
    categoryId: 2,
    priority: 'CRITICAL',
    defaultDurationMinutes: 90,
    startTime: 540,
    endTime: 630,
    isAllDay: false,
    recurrenceType: 'WEEKDAYS',
    recurringDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
    recurrenceInterval: 1,
    reminderEnabled: true,
    reminderOffsetMinutes: 5,
    createdAt: Date.now(),
  },
  {
    id: 2,
    name: 'Daily Workout & Cardio',
    description: 'Physical training and stretching',
    categoryId: 3,
    priority: 'HIGH',
    defaultDurationMinutes: 45,
    startTime: 1080, // 18:00
    endTime: 1125,
    isAllDay: false,
    recurrenceType: 'DAILY',
    recurringDays: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
    recurrenceInterval: 1,
    reminderEnabled: true,
    reminderOffsetMinutes: 15,
    createdAt: Date.now(),
  },
];

const DEFAULT_GOALS: Goal[] = [
  {
    id: 1,
    title: 'Daily Task Completion',
    targetType: 'TASKS_COMPLETED',
    targetPeriod: 'DAILY',
    targetValue: 3,
    unit: 'tasks',
    currentStreak: 4,
    longestStreak: 7,
    isActive: true,
    createdAt: Date.now(),
  },
  {
    id: 2,
    title: 'Focus Deep Work',
    targetType: 'FOCUS_MINUTES',
    targetPeriod: 'DAILY',
    targetValue: 90,
    unit: 'mins',
    currentStreak: 3,
    longestStreak: 5,
    isActive: true,
    createdAt: Date.now(),
  },
  {
    id: 3,
    title: 'High Priority Tasks',
    targetType: 'HIGH_PRIORITY_TASKS',
    targetPeriod: 'WEEKLY',
    targetValue: 8,
    unit: 'tasks',
    currentStreak: 2,
    longestStreak: 4,
    isActive: true,
    createdAt: Date.now(),
  },
];

// Installed apps matching screenshot from user (Bangla Dictionary, etc.)
const DEFAULT_APPS: InstalledAppInfo[] = [
  {
    packageName: 'com.dictionary.bn',
    appName: 'Bangla Dictionary',
    categories: ['Education', 'Tools'],
    qualityRating: 'GOOD',
    productivityType: 'PRODUCTIVE',
    isUsageTracked: true,
    severity: 'NORMAL',
  },
  {
    packageName: 'com.google.android.youtube',
    appName: 'YouTube',
    categories: ['Entertainment', 'Social Media'],
    qualityRating: 'NEUTRAL',
    productivityType: 'NON_PRODUCTIVE',
    isUsageTracked: true,
    severity: 'NORMAL',
  },
  {
    packageName: 'com.whatsapp',
    appName: 'WhatsApp',
    categories: ['Communication'],
    qualityRating: 'GOOD',
    productivityType: 'NEUTRAL',
    isUsageTracked: true,
    severity: 'NORMAL',
  },
  {
    packageName: 'com.github.android',
    appName: 'GitHub',
    categories: ['Coding', 'Productivity'],
    qualityRating: 'VERY_GOOD',
    productivityType: 'PRODUCTIVE',
    isUsageTracked: true,
    severity: 'NORMAL',
  },
  {
    packageName: 'com.instagram.android',
    appName: 'Instagram',
    categories: ['Social Media', 'Entertainment'],
    qualityRating: 'BAD',
    productivityType: 'NON_PRODUCTIVE',
    isUsageTracked: true,
    severity: 'HIGH',
  },
  {
    packageName: 'com.brave.browser',
    appName: 'Brave Browser',
    categories: ['Browser'],
    qualityRating: 'VERY_GOOD',
    productivityType: 'PRODUCTIVE',
    isUsageTracked: true,
    severity: 'NORMAL',
  },
  {
    packageName: 'com.focentra.app',
    appName: 'Focentra Study Timer',
    categories: ['Study Timer', 'Productivity'],
    qualityRating: 'VERY_GOOD',
    productivityType: 'PRODUCTIVE',
    isUsageTracked: true,
    severity: 'NORMAL',
  },
];

const DEFAULT_SETTINGS: AppSettings = {
  onboarding_completed: true,
  theme: 'LIGHT',
  color_preset: 'COBALT',
  time_format: '24H',
  week_start: 'MONDAY',
  weekend_days: ['SATURDAY', 'SUNDAY'],
  notifications_enabled: true,
  default_reminder_offset: 0,
};

export class StorageService {
  // --- Category ---
  getCategories(): Category[] {
    const raw = localStorage.getItem(STORAGE_KEYS.CATEGORIES);
    if (!raw) {
      this.saveCategories(DEFAULT_CATEGORIES);
      return DEFAULT_CATEGORIES;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return DEFAULT_CATEGORIES;
    }
  }

  saveCategories(categories: Category[]): void {
    localStorage.setItem(STORAGE_KEYS.CATEGORIES, JSON.stringify(categories));
  }

  saveCategory(category: Category): Category {
    const list = this.getCategories();
    let updated: Category[];
    let saved = category;
    if (category.id > 0) {
      updated = list.map((c) => (c.id === category.id ? category : c));
    } else {
      const nextId = list.length > 0 ? Math.max(...list.map((c) => c.id)) + 1 : 1;
      saved = { ...category, id: nextId };
      updated = [...list, saved];
    }
    this.saveCategories(updated);
    return saved;
  }

  deleteCategory(id: number): void {
    const list = this.getCategories().filter((c) => c.id !== id);
    this.saveCategories(list);
  }

  // --- Task ---
  getTasks(): Task[] {
    const raw = localStorage.getItem(STORAGE_KEYS.TASKS);
    if (!raw) {
      this.saveTasks(DEFAULT_TASKS);
      return DEFAULT_TASKS;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return DEFAULT_TASKS;
    }
  }

  saveTasks(tasks: Task[]): void {
    localStorage.setItem(STORAGE_KEYS.TASKS, JSON.stringify(tasks));
  }

  getTaskById(id: number): Task | undefined {
    return this.getTasks().find((t) => t.id === id);
  }

  saveTask(task: Task): Task {
    const list = this.getTasks();
    let updated: Task[];
    let saved = task;
    const now = Date.now();
    if (task.id > 0) {
      saved = { ...task, updatedAt: now };
      updated = list.map((t) => (t.id === task.id ? saved : t));
    } else {
      const nextId = list.length > 0 ? Math.max(...list.map((t) => t.id)) + 1 : 1;
      saved = { ...task, id: nextId, createdAt: now, updatedAt: now };
      updated = [...list, saved];
    }
    this.saveTasks(updated);
    return saved;
  }

  deleteTask(id: number): void {
    const list = this.getTasks().filter((t) => t.id !== id);
    this.saveTasks(list);
  }

  // --- Occurrences ---
  getOccurrences(): TaskOccurrence[] {
    const raw = localStorage.getItem(STORAGE_KEYS.OCCURRENCES);
    if (!raw) {
      // Create seed completed occurrences for recent days to generate realistic productivity stats
      const seedOccurrences: TaskOccurrence[] = [];
      for (let i = 1; i <= 6; i++) {
        const d = TimeUtils.addDays(todayStr, -i);
        seedOccurrences.push({
          taskId: 1,
          date: d,
          state: 'COMPLETED',
          updatedAt: Date.now() - i * 86400000,
        });
        if (i % 2 === 0) {
          seedOccurrences.push({
            taskId: 2,
            date: d,
            state: 'COMPLETED',
            updatedAt: Date.now() - i * 86400000,
          });
        }
      }
      this.saveOccurrences(seedOccurrences);
      return seedOccurrences;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return [];
    }
  }

  saveOccurrences(occurrences: TaskOccurrence[]): void {
    localStorage.setItem(STORAGE_KEYS.OCCURRENCES, JSON.stringify(occurrences));
  }

  saveOccurrence(occurrence: TaskOccurrence): void {
    const list = this.getOccurrences();
    const index = list.findIndex(
      (o) => o.taskId === occurrence.taskId && o.date === occurrence.date
    );
    if (index >= 0) {
      list[index] = { ...occurrence, updatedAt: Date.now() };
    } else {
      list.push({ ...occurrence, updatedAt: Date.now() });
    }
    this.saveOccurrences(list);
  }

  // --- Templates ---
  getTemplates(): TaskTemplate[] {
    const raw = localStorage.getItem(STORAGE_KEYS.TEMPLATES);
    if (!raw) {
      this.saveTemplates(DEFAULT_TEMPLATES);
      return DEFAULT_TEMPLATES;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return DEFAULT_TEMPLATES;
    }
  }

  saveTemplates(templates: TaskTemplate[]): void {
    localStorage.setItem(STORAGE_KEYS.TEMPLATES, JSON.stringify(templates));
  }

  saveTemplate(template: TaskTemplate): TaskTemplate {
    const list = this.getTemplates();
    const nextId = list.length > 0 ? Math.max(...list.map((t) => t.id)) + 1 : 1;
    const saved = { ...template, id: template.id > 0 ? template.id : nextId, createdAt: Date.now() };
    const updated = template.id > 0 ? list.map((t) => (t.id === template.id ? saved : t)) : [...list, saved];
    this.saveTemplates(updated);
    return saved;
  }

  deleteTemplate(id: number): void {
    const list = this.getTemplates().filter((t) => t.id !== id);
    this.saveTemplates(list);
  }

  // --- Goals ---
  getGoals(): Goal[] {
    const raw = localStorage.getItem(STORAGE_KEYS.GOALS);
    if (!raw) {
      this.saveGoals(DEFAULT_GOALS);
      return DEFAULT_GOALS;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return DEFAULT_GOALS;
    }
  }

  saveGoals(goals: Goal[]): void {
    localStorage.setItem(STORAGE_KEYS.GOALS, JSON.stringify(goals));
  }

  saveGoal(goal: Goal): Goal {
    const list = this.getGoals();
    const nextId = list.length > 0 ? Math.max(...list.map((g) => g.id)) + 1 : 1;
    const saved = { ...goal, id: goal.id > 0 ? goal.id : nextId };
    const updated = goal.id > 0 ? list.map((g) => (g.id === goal.id ? saved : g)) : [...list, saved];
    this.saveGoals(updated);
    return saved;
  }

  deleteGoal(id: number): void {
    const list = this.getGoals().filter((g) => g.id !== id);
    this.saveGoals(list);
  }

  // --- Installed Apps ---
  getInstalledApps(): InstalledAppInfo[] {
    const raw = localStorage.getItem(STORAGE_KEYS.APPS);
    if (!raw) {
      this.saveInstalledApps(DEFAULT_APPS);
      return DEFAULT_APPS;
    }
    try {
      return JSON.parse(raw);
    } catch {
      return DEFAULT_APPS;
    }
  }

  saveInstalledApps(apps: InstalledAppInfo[]): void {
    localStorage.setItem(STORAGE_KEYS.APPS, JSON.stringify(apps));
  }

  saveAppClassification(app: InstalledAppInfo): void {
    const list = this.getInstalledApps();
    const index = list.findIndex((a) => a.packageName === app.packageName);
    if (index >= 0) {
      list[index] = app;
    } else {
      list.push(app);
    }
    this.saveInstalledApps(list);
  }

  // --- Focentra Sessions ---
  getFocentraSessions(): FocentraStudySession[] {
    const raw = localStorage.getItem(STORAGE_KEYS.FOCENTRA_SESSIONS);
    if (!raw) return [];
    try {
      return JSON.parse(raw);
    } catch {
      return [];
    }
  }

  saveFocentraSessions(sessions: FocentraStudySession[]): void {
    localStorage.setItem(STORAGE_KEYS.FOCENTRA_SESSIONS, JSON.stringify(sessions));
  }

  addFocentraSession(session: FocentraStudySession): void {
    const list = this.getFocentraSessions();
    list.unshift(session);
    this.saveFocentraSessions(list);
  }

  clearFocentraSessions(): void {
    localStorage.removeItem(STORAGE_KEYS.FOCENTRA_SESSIONS);
  }

  // --- Settings ---
  getSettings(): AppSettings {
    const raw = localStorage.getItem(STORAGE_KEYS.SETTINGS);
    if (!raw) {
      this.saveSettings(DEFAULT_SETTINGS);
      return DEFAULT_SETTINGS;
    }
    try {
      return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
    } catch {
      return DEFAULT_SETTINGS;
    }
  }

  saveSettings(settings: AppSettings): void {
    localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(settings));
  }

  updateSetting<K extends keyof AppSettings>(key: K, value: AppSettings[K]): void {
    const current = this.getSettings();
    current[key] = value;
    this.saveSettings(current);
  }

  // --- Backup & Data Portability ---
  generateFullBackup(): FullBackupBundle {
    const settings = this.getSettings();
    return {
      version: 4,
      exportedAt: new Date().toISOString(),
      app: 'Daynexa',
      data: {
        categories: this.getCategories(),
        tasks: this.getTasks(),
        occurrences: this.getOccurrences(),
        taskTemplates: this.getTemplates(),
        goals: this.getGoals(),
        installedApps: this.getInstalledApps(),
        focentraSessions: this.getFocentraSessions(),
        settings: Object.fromEntries(
          Object.entries(settings).map(([k, v]) => [k, String(v)])
        ),
      },
    };
  }

  generateFocentraExport(): FocentraExportBundle {
    return {
      version: 1,
      app: 'Focentra',
      exportedAt: new Date().toISOString(),
      sessions: this.getFocentraSessions(),
    };
  }

  validateBackup(jsonString: string): BackupValidationResult {
    try {
      const obj = JSON.parse(jsonString);
      if (obj.app === 'Daynexa' && obj.data) {
        const cCount = obj.data.categories?.length || 0;
        const tCount = obj.data.tasks?.length || 0;
        const oCount = obj.data.occurrences?.length || 0;
        const gCount = obj.data.goals?.length || 0;
        return {
          type: 'VALID_FULL',
          bundle: obj as FullBackupBundle,
          summary: `Daynexa Backup: ${tCount} routines, ${cCount} categories, ${oCount} occurrence logs, ${gCount} goals`,
        };
      }
      if (obj.app === 'Focentra' && Array.isArray(obj.sessions)) {
        return {
          type: 'VALID_FOCENTRA',
          bundle: obj as FocentraExportBundle,
          summary: `Focentra Bundle: ${obj.sessions.length} focus study sessions`,
        };
      }
      return { type: 'INVALID', error: 'Unrecognized file format or missing application bundle signature.' };
    } catch (e) {
      return { type: 'INVALID', error: `Invalid JSON syntax: ${(e as Error).message}` };
    }
  }

  restoreFullBackup(bundle: FullBackupBundle, mode: ImportMode): boolean {
    try {
      if (mode === 'REPLACE') {
        if (bundle.data.categories) this.saveCategories(bundle.data.categories);
        if (bundle.data.tasks) this.saveTasks(bundle.data.tasks);
        if (bundle.data.occurrences) this.saveOccurrences(bundle.data.occurrences);
        if (bundle.data.taskTemplates) this.saveTemplates(bundle.data.taskTemplates);
        if (bundle.data.goals) this.saveGoals(bundle.data.goals);
        if (bundle.data.installedApps) this.saveInstalledApps(bundle.data.installedApps);
        if (bundle.data.focentraSessions) this.saveFocentraSessions(bundle.data.focentraSessions);
      } else {
        // MERGE mode
        const existingCats = this.getCategories();
        const newCats = [...existingCats];
        for (const c of bundle.data.categories || []) {
          if (!newCats.some((x) => x.name.toLowerCase() === c.name.toLowerCase())) {
            newCats.push({ ...c, id: Math.max(...newCats.map((x) => x.id), 0) + 1 });
          }
        }
        this.saveCategories(newCats);

        const existingTasks = this.getTasks();
        const newTasks = [...existingTasks];
        for (const t of bundle.data.tasks || []) {
          if (!newTasks.some((x) => x.name.toLowerCase() === t.name.toLowerCase())) {
            newTasks.push({ ...t, id: Math.max(...newTasks.map((x) => x.id), 0) + 1 });
          }
        }
        this.saveTasks(newTasks);

        const existingOccs = this.getOccurrences();
        const mergedOccs = [...existingOccs];
        for (const o of bundle.data.occurrences || []) {
          if (!mergedOccs.some((x) => x.taskId === o.taskId && x.date === o.date)) {
            mergedOccs.push(o);
          }
        }
        this.saveOccurrences(mergedOccs);

        const existingGoals = this.getGoals();
        const mergedGoals = [...existingGoals];
        for (const g of bundle.data.goals || []) {
          if (!mergedGoals.some((x) => x.title.toLowerCase() === g.title.toLowerCase())) {
            mergedGoals.push({ ...g, id: Math.max(...mergedGoals.map((x) => x.id), 0) + 1 });
          }
        }
        this.saveGoals(mergedGoals);
      }
      return true;
    } catch {
      return false;
    }
  }

  restoreFocentraBackup(bundle: FocentraExportBundle, mode: ImportMode): boolean {
    try {
      if (mode === 'REPLACE') {
        this.saveFocentraSessions(bundle.sessions);
      } else {
        const existing = this.getFocentraSessions();
        const merged = [...existing];
        for (const s of bundle.sessions) {
          if (!merged.some((x) => x.sessionId === s.sessionId)) {
            merged.push(s);
          }
        }
        this.saveFocentraSessions(merged);
      }
      return true;
    } catch {
      return false;
    }
  }

  resetAllData(): void {
    localStorage.clear();
    this.saveCategories(DEFAULT_CATEGORIES);
    this.saveTasks([]);
    this.saveOccurrences([]);
    this.saveTemplates(DEFAULT_TEMPLATES);
    this.saveGoals(DEFAULT_GOALS);
    this.saveInstalledApps(DEFAULT_APPS);
    this.saveSettings(DEFAULT_SETTINGS);
  }
}

export const storageService = new StorageService();
