import React, { useState, useRef } from 'react';
import {
  ArrowLeft,
  CloudOff,
  Palette,
  Clock,
  Calendar,
  Share2,
  Bell,
  Upload,
  Download,
  Activity,
  Sliders,
  Trash2,
  Check,
  ChevronRight,
  Sparkles,
  AlertTriangle,
} from 'lucide-react';
import {
  AppSettings,
  DayOfWeek,
  BackupValidationResult,
  ImportMode,
  FocentraIntegrationStatus,
} from '../types';
import { storageService } from '../services/storage';
import { notificationService } from '../services/notificationService';

interface SettingsScreenProps {
  settings: AppSettings;
  focentraStatus: FocentraIntegrationStatus;
  onUpdateSettings: (settings: AppSettings) => void;
  onNavigateToDiagnostic: () => void;
  onNavigateToAppClassification: () => void;
  onConnectFocentra: () => void;
  onDisconnectFocentra: () => void;
  onSyncFocentra: () => void;
  onResetAllData: () => void;
  onBack?: () => void;
}

const COLOR_PRESETS = [
  { id: 'COBALT' as const, label: 'Cobalt', hex: '#3B82F6' },
  { id: 'EMERALD' as const, label: 'Emerald', hex: '#10B981' },
  { id: 'AMETHYST' as const, label: 'Amethyst', hex: '#8B5CF6' },
  { id: 'AMBER' as const, label: 'Amber', hex: '#F59E0B' },
  { id: 'ROSE' as const, label: 'Rose', hex: '#F43F5E' },
];

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  settings,
  focentraStatus,
  onUpdateSettings,
  onNavigateToDiagnostic,
  onNavigateToAppClassification,
  onConnectFocentra,
  onDisconnectFocentra,
  onSyncFocentra,
  onResetAllData,
  onBack,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Backup state
  const [validationResult, setValidationResult] = useState<BackupValidationResult | null>(null);
  const [importMode, setImportMode] = useState<ImportMode>('MERGE');
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);
  const [showResetConfirm, setShowResetConfirm] = useState(false);

  const handleThemeChange = (theme: AppSettings['theme']) => {
    onUpdateSettings({ ...settings, theme });
  };

  const handleColorPresetChange = (preset: AppSettings['color_preset']) => {
    onUpdateSettings({ ...settings, color_preset: preset });
  };

  const handleTimeFormatChange = (time_format: '12H' | '24H') => {
    onUpdateSettings({ ...settings, time_format });
  };

  const handleWeekStartChange = (week_start: DayOfWeek) => {
    onUpdateSettings({ ...settings, week_start });
  };

  const handleToggleNotifications = (enabled: boolean) => {
    if (enabled) {
      notificationService.requestPermission();
    }
    onUpdateSettings({ ...settings, notifications_enabled: enabled });
  };

  const handleReminderOffsetChange = (default_reminder_offset: number) => {
    onUpdateSettings({ ...settings, default_reminder_offset });
  };

  const handleExportBackup = () => {
    try {
      const bundle = storageService.generateFullBackup();
      const jsonStr = JSON.stringify(bundle, null, 2);
      const blob = new Blob([jsonStr], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `daynexa_backup_${new Date().toISOString().slice(0, 10)}.daynexa.json`;
      a.click();
      URL.revokeObjectURL(url);
      setFeedbackMessage('Backup bundle exported successfully to download folder!');
    } catch (e) {
      setFeedbackMessage(`Export failed: ${(e as Error).message}`);
    }
  };

  const handleFileSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result as string;
      if (content) {
        const result = storageService.validateBackup(content);
        setValidationResult(result);
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  const handleExecuteRestore = () => {
    if (!validationResult) return;
    if (validationResult.type === 'VALID_FULL') {
      const ok = storageService.restoreFullBackup(validationResult.bundle, importMode);
      setFeedbackMessage(
        ok
          ? `Backup successfully ${importMode === 'REPLACE' ? 'restored and replaced' : 'merged'}!`
          : 'Failed to restore backup.'
      );
    } else if (validationResult.type === 'VALID_FOCENTRA') {
      const ok = storageService.restoreFocentraBackup(validationResult.bundle, importMode);
      setFeedbackMessage(
        ok
          ? `Focentra sessions successfully ${importMode === 'REPLACE' ? 'restored' : 'merged'}!`
          : 'Failed to restore Focentra backup.'
      );
    }
    setValidationResult(null);
  };

  return (
    <div className="pb-32">
      {/* Top Bar matching screenshot photo_2026-10-15_08-58-28.jpg */}
      <header className="sticky top-0 z-30 bg-white/95 dark:bg-[#090D16]/95 backdrop-blur-md px-4 py-3 border-b border-slate-200 dark:border-slate-800 flex items-center gap-3">
        {onBack && (
          <button
            onClick={onBack}
            className="p-1.5 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
        )}
        <h1 className="text-xl font-bold text-slate-900 dark:text-slate-100">
          Settings
        </h1>
      </header>

      <main className="max-w-md mx-auto px-4 pt-4 space-y-6">
        {/* Feedback message banner */}
        {feedbackMessage && (
          <div className="p-3.5 rounded-2xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 text-xs font-semibold text-emerald-800 dark:text-emerald-200 flex items-center justify-between">
            <span>{feedbackMessage}</span>
            <button
              onClick={() => setFeedbackMessage(null)}
              className="text-emerald-600 hover:text-emerald-800 text-xs font-bold"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* 100% Offline & Private Card - Matching screenshot */}
        <div className="p-4 rounded-3xl bg-blue-50/70 dark:bg-blue-950/30 border border-blue-200/60 dark:border-blue-900/50 flex items-center gap-3.5 shadow-2xs">
          <div className="w-12 h-12 rounded-2xl bg-blue-100 dark:bg-blue-900/50 text-blue-600 dark:text-blue-400 flex items-center justify-center shrink-0">
            <CloudOff className="w-6 h-6 stroke-[2.2]" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-blue-900 dark:text-blue-200">
              100% Offline & Private
            </h2>
            <p className="text-xs text-blue-700/80 dark:text-blue-300/80 mt-0.5 leading-relaxed">
              Your data stays on your device. No cloud sync, no tracking, no telemetry. Ever.
            </p>
          </div>
        </div>

        {/* APPEARANCE SECTION */}
        <section className="space-y-3">
          <h3 className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1">
            Appearance
          </h3>

          <div className="bg-white dark:bg-[#111827] rounded-3xl p-4 border border-slate-200/80 dark:border-slate-800 shadow-2xs space-y-4">
            {/* App Theme */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Palette className="w-5 h-5 text-slate-500" />
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    App Theme
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    Select between Light, Dark, or System mode
                  </div>
                </div>
              </div>

              <div className="flex gap-1 bg-slate-100 dark:bg-slate-800 p-1 rounded-2xl">
                {(
                  [
                    { id: 'LIGHT', label: 'Light' },
                    { id: 'DARK', label: 'Dark' },
                    { id: 'SYSTEM', label: 'Auto' },
                  ] as const
                ).map((t) => (
                  <button
                    key={t.id}
                    onClick={() => handleThemeChange(t.id)}
                    className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                      settings.theme === t.id
                        ? 'bg-blue-600 text-white shadow-2xs'
                        : 'text-slate-600 dark:text-slate-300 hover:text-slate-900'
                    }`}
                  >
                    {t.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Color Presets */}
            <div className="pt-3 border-t border-slate-100 dark:border-slate-800">
              <div className="text-xs font-semibold text-slate-500 dark:text-slate-400 mb-2">
                Brand Accent Palette
              </div>
              <div className="flex gap-2 overflow-x-auto pb-1">
                {COLOR_PRESETS.map((preset) => {
                  const isSelected = settings.color_preset === preset.id;
                  return (
                    <button
                      key={preset.id}
                      onClick={() => handleColorPresetChange(preset.id)}
                      className={`px-3 py-1.5 rounded-full text-xs font-bold flex items-center gap-1.5 transition-all shrink-0 ${
                        isSelected
                          ? 'shadow-xs'
                          : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                      }`}
                      style={
                        isSelected
                          ? { backgroundColor: preset.hex, color: '#ffffff' }
                          : undefined
                      }
                    >
                      <span
                        className="w-2 h-2 rounded-full"
                        style={{ backgroundColor: preset.hex }}
                      />
                      {preset.label}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        </section>

        {/* GENERAL PREFERENCES */}
        <section className="space-y-3">
          <h3 className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1">
            General Preferences
          </h3>

          <div className="bg-white dark:bg-[#111827] rounded-3xl p-4 border border-slate-200/80 dark:border-slate-800 shadow-2xs space-y-4">
            {/* Time Format */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Clock className="w-5 h-5 text-slate-500" />
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    Time Format
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    Choose your preferred clock system
                  </div>
                </div>
              </div>

              <div className="flex gap-1 bg-slate-100 dark:bg-slate-800 p-1 rounded-2xl">
                {(['12H', '24H'] as const).map((fmt) => (
                  <button
                    key={fmt}
                    onClick={() => handleTimeFormatChange(fmt)}
                    className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                      settings.time_format === fmt
                        ? 'bg-emerald-600 text-white shadow-2xs'
                        : 'text-slate-600 dark:text-slate-300'
                    }`}
                  >
                    {fmt}
                  </button>
                ))}
              </div>
            </div>

            {/* Week Start Day */}
            <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-3">
                <Calendar className="w-5 h-5 text-slate-500" />
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    Week Start Day
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    Select which day starts your routine week
                  </div>
                </div>
              </div>

              <div className="flex gap-1 bg-slate-100 dark:bg-slate-800 p-1 rounded-2xl">
                {(
                  [
                    { id: 'MONDAY' as const, label: 'Mon' },
                    { id: 'SUNDAY' as const, label: 'Sun' },
                  ] as const
                ).map((day) => (
                  <button
                    key={day.id}
                    onClick={() => handleWeekStartChange(day.id)}
                    className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                      settings.week_start === day.id
                        ? 'bg-emerald-600 text-white shadow-2xs'
                        : 'text-slate-600 dark:text-slate-300'
                    }`}
                  >
                    {day.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* INTEGRATIONS • FOCENTRA */}
        <section className="space-y-3">
          <h3 className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1">
            Integrations • Focentra
          </h3>

          <div className="bg-white dark:bg-[#111827] rounded-3xl p-4 border border-slate-200/80 dark:border-slate-800 shadow-2xs flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 flex items-center justify-center shrink-0">
                <Share2 className="w-5 h-5" />
              </div>
              <div>
                <div className="text-sm font-bold text-slate-900 dark:text-slate-100">
                  Focentra Study Focus
                </div>
                <div className="text-xs text-slate-500 dark:text-slate-400">
                  {focentraStatus.isConnected
                    ? `Connected • ${focentraStatus.totalImportedSessions} sessions imported`
                    : 'Not Connected (Optional)'}
                </div>
              </div>
            </div>

            {focentraStatus.isConnected ? (
              <div className="flex items-center gap-2">
                <button
                  onClick={onSyncFocentra}
                  className="text-xs font-bold text-emerald-600 hover:text-emerald-700 px-2 py-1"
                >
                  Sync
                </button>
                <button
                  onClick={onDisconnectFocentra}
                  className="px-3 py-1.5 rounded-full border border-rose-300 text-xs font-bold text-rose-600 hover:bg-rose-50"
                >
                  Disconnect
                </button>
              </div>
            ) : (
              <button
                onClick={onConnectFocentra}
                className="px-4 py-1.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-xs transition-all"
              >
                Connect
              </button>
            )}
          </div>
        </section>

        {/* NOTIFICATIONS & REMINDERS */}
        <section className="space-y-3">
          <h3 className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1">
            Notifications & Reminders
          </h3>

          <div className="bg-white dark:bg-[#111827] rounded-3xl p-4 border border-slate-200/80 dark:border-slate-800 shadow-2xs space-y-4">
            {/* Toggle */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Bell className="w-5 h-5 text-slate-500" />
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    Routine Notifications
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    Smart on-device reminder alerts enabled
                  </div>
                </div>
              </div>

              <label className="relative inline-flex items-center cursor-pointer shrink-0">
                <input
                  type="checkbox"
                  checked={settings.notifications_enabled}
                  onChange={(e) => handleToggleNotifications(e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-500"></div>
              </label>
            </div>

            {/* Default Reminder Timing Pills matching screenshot photo_2026-10-15_08-58-08.jpg */}
            <div className="pt-3 border-t border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-2 mb-2">
                <Clock className="w-4 h-4 text-emerald-500" />
                <div>
                  <div className="text-xs font-bold text-slate-900 dark:text-slate-100">
                    Default Reminder Timing
                  </div>
                  <div className="text-[11px] text-slate-400">
                    {settings.default_reminder_offset === 0
                      ? 'At start time'
                      : `${settings.default_reminder_offset}m before`}
                  </div>
                </div>
              </div>

              <div className="flex gap-2 overflow-x-auto pb-1">
                {[
                  { offset: 0, label: 'At start' },
                  { offset: 5, label: '5m before' },
                  { offset: 10, label: '10m before' },
                  { offset: 15, label: '15m before' },
                  { offset: 30, label: '30m before' },
                ].map((item) => (
                  <button
                    key={item.offset}
                    onClick={() => handleReminderOffsetChange(item.offset)}
                    className={`px-3.5 py-1.5 rounded-2xl text-xs font-bold shrink-0 transition-all ${
                      settings.default_reminder_offset === item.offset
                        ? 'bg-slate-900 text-white dark:bg-white dark:text-slate-900 shadow-2xs'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200'
                    }`}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Test alert button */}
            <button
              onClick={() => notificationService.sendTestAlert()}
              className="w-full flex items-center justify-between pt-3 border-t border-slate-100 dark:border-slate-800 text-left hover:opacity-80 transition-opacity"
            >
              <div>
                <div className="text-xs font-bold text-slate-900 dark:text-slate-100">
                  Send Test Routine Alert
                </div>
                <div className="text-[11px] text-slate-400">
                  Verify on-device notification sound and banner
                </div>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-400" />
            </button>
          </div>
        </section>

        {/* DATA & PORTABILITY */}
        <section className="space-y-3">
          <h3 className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1">
            Data & Portability
          </h3>

          <div className="bg-white dark:bg-[#111827] rounded-3xl p-4 border border-slate-200/80 dark:border-slate-800 shadow-2xs space-y-4">
            <button
              onClick={handleExportBackup}
              className="w-full flex items-center justify-between text-left hover:opacity-80 transition-opacity"
            >
              <div className="flex items-center gap-3">
                <Download className="w-5 h-5 text-slate-600 dark:text-slate-300" />
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    Export Backup
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    Unified backup including routines, logs, goals & imported Focentra sessions
                  </div>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-400" />
            </button>

            <button
              onClick={() => fileInputRef.current?.click()}
              className="w-full flex items-center justify-between text-left pt-3 border-t border-slate-100 dark:border-slate-800 hover:opacity-80 transition-opacity"
            >
              <div className="flex items-center gap-3">
                <Upload className="w-5 h-5 text-slate-600 dark:text-slate-300" />
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    Import Backup / Restore Data
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    Select .daynexa or .json backup file with Merge or Replace mode
                  </div>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-400" />
            </button>
            <input
              type="file"
              ref={fileInputRef}
              accept=".json,.daynexa"
              onChange={handleFileSelected}
              className="hidden"
            />
          </div>
        </section>

        {/* APP USAGE DIAGNOSTICS */}
        <section className="space-y-3">
          <h3 className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 px-1">
            App Usage Diagnostics
          </h3>

          <div className="bg-white dark:bg-[#111827] rounded-3xl p-4 border border-slate-200/80 dark:border-slate-800 shadow-2xs space-y-4">
            <button
              onClick={onNavigateToDiagnostic}
              className="w-full flex items-center justify-between text-left hover:opacity-80 transition-opacity"
            >
              <div className="flex items-center gap-3">
                <Activity className="w-5 h-5 text-slate-600 dark:text-slate-300" />
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    Diagnostic Dashboard
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    View focus time, app quality ratings, and usage stats
                  </div>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-400" />
            </button>

            <button
              onClick={onNavigateToAppClassification}
              className="w-full flex items-center justify-between text-left pt-3 border-t border-slate-100 dark:border-slate-800 hover:opacity-80 transition-opacity"
            >
              <div className="flex items-center gap-3">
                <Sliders className="w-5 h-5 text-slate-600 dark:text-slate-300" />
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    Manage App Classifications
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    Categorize installed apps and set quality ratings
                  </div>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 text-slate-400" />
            </button>
          </div>
        </section>

        {/* DANGER ZONE matching screenshot photo_2026-10-15_08-58-20.jpg */}
        <section className="space-y-3">
          <h3 className="text-[11px] font-bold uppercase tracking-wider text-rose-500 px-1">
            Danger Zone
          </h3>

          <div className="bg-white dark:bg-[#111827] rounded-3xl p-4 border border-rose-200/80 dark:border-rose-950/50 shadow-2xs">
            <button
              onClick={() => setShowResetConfirm(true)}
              className="w-full flex items-center justify-between text-left hover:opacity-80 transition-opacity"
            >
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400">
                  <Trash2 className="w-5 h-5" />
                </div>
                <div>
                  <div className="text-sm font-bold text-rose-600 dark:text-rose-400">
                    Reset All Application Data
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    Erase all routines, history, and restored backups
                  </div>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 text-rose-400" />
            </button>
          </div>
        </section>

        {/* Footer info matching screenshot */}
        <div className="text-center pt-4 pb-2">
          <p className="text-xs font-bold text-slate-700 dark:text-slate-300">Daynexa</p>
          <p className="text-[11px] text-slate-400 mt-0.5">Build Better Days • v1.0.0</p>
        </div>
      </main>

      {/* Backup Validation Dialog */}
      {validationResult && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white dark:bg-[#1F2937] w-full max-w-sm rounded-3xl p-6 shadow-2xl border border-slate-200 dark:border-slate-700 animate-in zoom-in-95 space-y-4">
            <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
              Restore Backup Bundle
            </h3>

            {validationResult.type === 'INVALID' ? (
              <div className="p-3 rounded-2xl bg-rose-50 text-rose-700 text-xs font-medium">
                {validationResult.error}
              </div>
            ) : (
              <div className="space-y-3">
                <div className="p-3.5 rounded-2xl bg-blue-50 dark:bg-blue-950/40 text-xs text-blue-900 dark:text-blue-200 font-medium">
                  {validationResult.summary}
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
                    Select Restore Mode
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      type="button"
                      onClick={() => setImportMode('MERGE')}
                      className={`p-2.5 rounded-2xl text-xs font-bold transition-all ${
                        importMode === 'MERGE'
                          ? 'bg-blue-600 text-white shadow-2xs'
                          : 'bg-slate-100 dark:bg-slate-800 text-slate-600'
                      }`}
                    >
                      Merge (Safe)
                    </button>
                    <button
                      type="button"
                      onClick={() => setImportMode('REPLACE')}
                      className={`p-2.5 rounded-2xl text-xs font-bold transition-all ${
                        importMode === 'REPLACE'
                          ? 'bg-rose-600 text-white shadow-2xs'
                          : 'bg-slate-100 dark:bg-slate-800 text-slate-600'
                      }`}
                    >
                      Replace All
                    </button>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-1">
                    {importMode === 'MERGE'
                      ? 'Integrates missing routines and logs seamlessly without losing existing items.'
                      : 'Wipes current local database and replaces entirely with this file.'}
                  </p>
                </div>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100 dark:border-slate-800">
              <button
                type="button"
                onClick={() => setValidationResult(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-500"
              >
                Cancel
              </button>
              {validationResult.type !== 'INVALID' && (
                <button
                  type="button"
                  onClick={handleExecuteRestore}
                  className="px-5 py-2 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700"
                >
                  Execute Restore
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Reset Confirmation Dialog */}
      {showResetConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white dark:bg-[#1F2937] w-full max-w-sm rounded-3xl p-6 shadow-2xl border border-slate-200 dark:border-slate-700 animate-in zoom-in-95 space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
                Erase All Application Data?
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                This will reset all routines, logs, and categories back to clean defaults. This action cannot be undone.
              </p>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowResetConfirm(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-500"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onResetAllData();
                  setShowResetConfirm(false);
                  setFeedbackMessage('All data reset to clean factory state.');
                }}
                className="px-5 py-2 rounded-xl text-xs font-bold text-white bg-rose-600 hover:bg-rose-700"
              >
                Yes, Reset All Data
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
