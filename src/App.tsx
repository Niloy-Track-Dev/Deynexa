import React, { useState, useEffect, useMemo } from 'react';
import {
  Task,
  TaskOccurrence,
  Category,
  Goal,
  InstalledAppInfo,
  FocentraStudySession,
  AppSettings,
  TaskState,
  RecurrenceEditMode,
  FocentraIntegrationStatus,
} from './types';
import { storageService } from './services/storage';
import { schedulingService } from './services/schedulingService';
import { notificationService } from './services/notificationService';
import { Navigation, TabType } from './components/Navigation';
import { TodayScreen } from './screens/TodayScreen';
import { CalendarScreen } from './screens/CalendarScreen';
import { StatsScreen } from './screens/StatsScreen';
import { CategoriesScreen } from './screens/CategoriesScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { DiagnosticScreen } from './screens/DiagnosticScreen';
import { AppClassificationScreen } from './screens/AppClassificationScreen';
import { OnboardingScreen } from './screens/OnboardingScreen';
import { TimeUtils } from './utils/timeUtils';

const COLOR_PRESET_HEX: Record<AppSettings['color_preset'], string> = {
  COBALT: '#3B82F6',
  EMERALD: '#10B981',
  AMETHYST: '#8B5CF6',
  VIOLET: '#8B5CF6',
  AMBER: '#F59E0B',
  ROSE: '#F43F5E',
};

export const App: React.FC = () => {
  // State from local storage
  const [settings, setSettings] = useState<AppSettings>(() => storageService.getSettings());
  const [categories, setCategories] = useState<Category[]>(() => storageService.getCategories());
  const [tasks, setTasks] = useState<Task[]>(() => storageService.getTasks());
  const [occurrences, setOccurrences] = useState<TaskOccurrence[]>(() =>
    storageService.getOccurrences()
  );
  const [goals, setGoals] = useState<Goal[]>(() => storageService.getGoals());
  const [installedApps, setInstalledApps] = useState<InstalledAppInfo[]>(() =>
    storageService.getInstalledApps()
  );
  const [focentraSessions, setFocentraSessions] = useState<FocentraStudySession[]>(() =>
    storageService.getFocentraSessions()
  );

  const [currentTab, setCurrentTab] = useState<TabType>('today');
  const [selectedDate, setSelectedDate] = useState<string>(() =>
    TimeUtils.formatLocalDate(new Date())
  );
  const [focentraConnected, setFocentraConnected] = useState(false);

  // Sync theme
  useEffect(() => {
    const root = document.documentElement;
    if (
      settings.theme === 'DARK' ||
      (settings.theme === 'SYSTEM' && window.matchMedia('(prefers-color-scheme: dark)').matches)
    ) {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [settings.theme]);

  const accentColor = COLOR_PRESET_HEX[settings.color_preset] || '#3B82F6';

  // Focentra status
  const focentraStatus: FocentraIntegrationStatus = useMemo(
    () => ({
      isInstalled: true,
      isConnected: focentraConnected,
      hasConsent: focentraConnected,
      schemaVersion: 1,
      totalImportedSessions: focentraSessions.length,
      lastSyncTimestamp: Date.now(),
    }),
    [focentraConnected, focentraSessions]
  );

  // Tasks with occurrences for selected date
  const tasksForSelectedDate = useMemo(() => {
    return schedulingService.getTasksForDate(
      tasks,
      occurrences,
      selectedDate,
      settings.weekend_days
    );
  }, [tasks, occurrences, selectedDate, settings.weekend_days]);

  // Overall productivity insights
  const insights = useMemo(() => {
    return schedulingService.calculateProductivityInsights(
      tasks,
      occurrences,
      settings.weekend_days,
      focentraSessions
    );
  }, [tasks, occurrences, settings.weekend_days, focentraSessions]);

  // --- Handlers ---
  const handleUpdateSettings = (newSettings: AppSettings) => {
    storageService.saveSettings(newSettings);
    setSettings(newSettings);
  };

  const handleSaveTask = (task: Task, mode: RecurrenceEditMode = 'ENTIRE_SERIES') => {
    const today = TimeUtils.formatLocalDate(new Date());

    if (mode === 'THIS_OCCURRENCE' && task.id > 0) {
      // Save exception occurrence for today
      const exceptionOcc: TaskOccurrence = {
        taskId: task.id,
        date: today,
        state: 'PENDING',
        rescheduledStartTime: task.isAllDay ? null : task.startTime,
        rescheduledEndTime: task.isAllDay ? null : task.endTime,
        isException: true,
        notes: task.description,
        updatedAt: Date.now(),
      };
      storageService.saveOccurrence(exceptionOcc);
      setOccurrences(storageService.getOccurrences());
      return;
    }

    if (mode === 'THIS_AND_FUTURE' && task.id > 0) {
      // Terminate old task as of yesterday
      const old = storageService.getTaskById(task.id);
      if (old) {
        const yesterday = TimeUtils.addDays(today, -1);
        storageService.saveTask({
          ...old,
          recurrenceEndType: 'ON_DATE',
          recurrenceEndDate: yesterday,
        });
      }
      // Create new task starting today
      const newTask = storageService.saveTask({
        ...task,
        id: 0,
        startDate: today,
      });
      setTasks(storageService.getTasks());
      notificationService.scheduleTaskReminder(
        newTask,
        categories.find((c) => c.id === newTask.categoryId)?.name || ''
      );
      return;
    }

    // Entire series
    const saved = storageService.saveTask(task);
    setTasks(storageService.getTasks());
    notificationService.scheduleTaskReminder(
      saved,
      categories.find((c) => c.id === saved.categoryId)?.name || ''
    );
  };

  const handleDeleteTask = (id: number) => {
    storageService.deleteTask(id);
    setTasks(storageService.getTasks());
  };

  const handleToggleComplete = (taskId: number, date: string, currentState: TaskState) => {
    const newState: TaskState = currentState === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
    storageService.saveOccurrence({
      taskId,
      date,
      state: newState,
      updatedAt: Date.now(),
    });
    setOccurrences(storageService.getOccurrences());
  };

  const handleToggleSkip = (
    taskId: number,
    date: string,
    currentState: TaskState,
    reason?: string
  ) => {
    const newState: TaskState = currentState === 'SKIPPED' ? 'PENDING' : 'SKIPPED';
    storageService.saveOccurrence({
      taskId,
      date,
      state: newState,
      notes: newState === 'SKIPPED' ? reason || 'Skipped for day' : null,
      updatedAt: Date.now(),
    });
    setOccurrences(storageService.getOccurrences());
  };

  const handleRescheduleOccurrence = (
    taskId: number,
    originalDate: string,
    targetDate: string,
    startTime: number,
    endTime: number,
    notes?: string
  ) => {
    storageService.saveOccurrence({
      taskId,
      date: originalDate,
      state: 'PENDING',
      rescheduledDate: targetDate !== originalDate ? targetDate : null,
      rescheduledStartTime: startTime,
      rescheduledEndTime: endTime,
      isException: true,
      notes: notes || null,
      updatedAt: Date.now(),
    });
    setOccurrences(storageService.getOccurrences());
  };

  const handleSaveCategory = (category: Category) => {
    storageService.saveCategory(category);
    setCategories(storageService.getCategories());
  };

  const handleDeleteCategory = (id: number) => {
    storageService.deleteCategory(id);
    setCategories(storageService.getCategories());
  };

  const handleSaveGoal = (goal: Goal) => {
    storageService.saveGoal(goal);
    setGoals(storageService.getGoals());
  };

  const handleDeleteGoal = (id: number) => {
    storageService.deleteGoal(id);
    setGoals(storageService.getGoals());
  };

  const handleSaveClassification = (app: InstalledAppInfo) => {
    storageService.saveAppClassification(app);
    setInstalledApps(storageService.getInstalledApps());
  };

  const handleLogFocentraSession = (session: FocentraStudySession) => {
    storageService.addFocentraSession(session);
    setFocentraSessions(storageService.getFocentraSessions());
  };

  const handleResetAllData = () => {
    storageService.resetAllData();
    setSettings(storageService.getSettings());
    setCategories(storageService.getCategories());
    setTasks(storageService.getTasks());
    setOccurrences(storageService.getOccurrences());
    setGoals(storageService.getGoals());
    setInstalledApps(storageService.getInstalledApps());
    setFocentraSessions([]);
  };

  // If user hasn't completed onboarding
  if (!settings.onboarding_completed) {
    return (
      <OnboardingScreen
        accentColor={accentColor}
        onComplete={() => {
          handleUpdateSettings({ ...settings, onboarding_completed: true });
        }}
      />
    );
  }

  return (
    <div className="min-h-screen bg-slate-100 dark:bg-[#090D16] text-slate-900 dark:text-slate-100 selection:bg-blue-500/20 selection:text-blue-600">
      {/* Active Tab Screen */}
      {currentTab === 'today' && (
        <TodayScreen
          selectedDate={selectedDate}
          tasksWithOccurrences={tasksForSelectedDate}
          allTasks={tasks}
          categories={categories}
          is24Hour={settings.time_format === '24H'}
          accentColor={accentColor}
          insights={insights}
          onSelectDate={setSelectedDate}
          onSaveTask={handleSaveTask}
          onDeleteTask={handleDeleteTask}
          onToggleComplete={handleToggleComplete}
          onToggleSkip={handleToggleSkip}
          onRescheduleOccurrence={handleRescheduleOccurrence}
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
          onLogFocentraSession={handleLogFocentraSession}
        />
      )}

      {currentTab === 'calendar' && (
        <CalendarScreen
          tasks={tasks}
          occurrences={occurrences}
          categories={categories}
          is24Hour={settings.time_format === '24H'}
          weekStart={settings.week_start}
          accentColor={accentColor}
          onToggleComplete={handleToggleComplete}
          onToggleSkip={handleToggleSkip}
          onRescheduleOccurrence={handleRescheduleOccurrence}
          onSaveTask={handleSaveTask}
          onDeleteTask={handleDeleteTask}
        />
      )}

      {currentTab === 'stats' && (
        <StatsScreen
          tasks={tasks}
          occurrences={occurrences}
          categories={categories}
          goals={goals}
          focusSessions={focentraSessions}
          customWeekend={settings.weekend_days}
          accentColor={accentColor}
          onSaveGoal={handleSaveGoal}
          onDeleteGoal={handleDeleteGoal}
        />
      )}

      {currentTab === 'categories' && (
        <CategoriesScreen
          categories={categories}
          tasks={tasks}
          accentColor={accentColor}
          onSaveCategory={handleSaveCategory}
          onDeleteCategory={handleDeleteCategory}
        />
      )}

      {currentTab === 'settings' && (
        <SettingsScreen
          settings={settings}
          focentraStatus={focentraStatus}
          onUpdateSettings={handleUpdateSettings}
          onNavigateToDiagnostic={() => setCurrentTab('diagnostic')}
          onNavigateToAppClassification={() => setCurrentTab('app_classification')}
          onConnectFocentra={() => setFocentraConnected(true)}
          onDisconnectFocentra={() => setFocentraConnected(false)}
          onSyncFocentra={() => {
            // Simulated sync
            setFocentraSessions(storageService.getFocentraSessions());
          }}
          onResetAllData={handleResetAllData}
        />
      )}

      {currentTab === 'diagnostic' && (
        <DiagnosticScreen
          installedApps={installedApps}
          accentColor={accentColor}
          onBack={() => setCurrentTab('settings')}
          onOpenAppClassification={() => setCurrentTab('app_classification')}
        />
      )}

      {currentTab === 'app_classification' && (
        <AppClassificationScreen
          apps={installedApps}
          onBack={() => setCurrentTab('settings')}
          onSaveClassification={handleSaveClassification}
        />
      )}

      {/* Floating Bottom Navigation */}
      <Navigation
        currentTab={currentTab}
        onTabChange={setCurrentTab}
        accentColor={accentColor}
      />
    </div>
  );
};
