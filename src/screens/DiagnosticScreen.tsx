import React, { useState, useMemo } from 'react';
import {
  ArrowLeft,
  Activity,
  Sparkles,
  PieChart,
  BarChart2,
  Clock,
  Shield,
  Sliders,
  CheckCircle,
  AlertCircle,
  Smartphone,
} from 'lucide-react';
import {
  InstalledAppInfo,
  AppUsageInfo,
  DiagnosticSummary,
  ProductivityType,
} from '../types';
import { TimeUtils } from '../utils/timeUtils';

interface DiagnosticScreenProps {
  installedApps: InstalledAppInfo[];
  accentColor: string;
  onBack: () => void;
  onOpenAppClassification: () => void;
}

export const DiagnosticScreen: React.FC<DiagnosticScreenProps> = ({
  installedApps,
  accentColor,
  onBack,
  onOpenAppClassification,
}) => {
  const [periodTab, setPeriodTab] = useState<number>(1); // 0: Today, 1: Weekly, 2: Monthly, 3: Custom
  const [customStart, setCustomStart] = useState(
    TimeUtils.addDays(TimeUtils.formatLocalDate(new Date()), -7)
  );
  const [customEnd, setCustomEnd] = useState(TimeUtils.formatLocalDate(new Date()));

  // Simulated diagnostic engine for installed apps
  const appUsageSummary: DiagnosticSummary = useMemo(() => {
    // Generate realistic relative foreground times based on app quality rating & category
    const factor = periodTab === 0 ? 1 : periodTab === 1 ? 7 : 30;

    const simulatedUsage: AppUsageInfo[] = installedApps.map((app, index) => {
      // Give base minutes
      let baseMinutes = 20 + ((index * 37) % 65);
      if (app.categories.includes('Social Media')) baseMinutes = 110;
      else if (app.categories.includes('Entertainment')) baseMinutes = 85;
      else if (app.categories.includes('Education') || app.categories.includes('Coding'))
        baseMinutes = 95;
      else if (app.categories.includes('Communication')) baseMinutes = 55;

      const totalMillis = baseMinutes * factor * 60 * 1000;
      return {
        packageName: app.packageName,
        appName: app.appName,
        totalTimeInForegroundMillis: totalMillis,
        categories: app.categories,
        qualityRating: app.qualityRating,
        productivityType: app.productivityType,
        isUsageTracked: app.isUsageTracked,
        severity: app.severity,
        percentageOfTotal: 0,
        lastTimeUsed: Date.now() - index * 3600000,
      };
    });

    const trackedApps = simulatedUsage.filter((a) => a.isUsageTracked);
    const totalTimeMillis = trackedApps.reduce(
      (acc, a) => acc + a.totalTimeInForegroundMillis,
      0
    );

    const productiveMillis = trackedApps
      .filter((a) => a.productivityType === 'PRODUCTIVE')
      .reduce((acc, a) => acc + a.totalTimeInForegroundMillis, 0);

    const nonProductiveMillis = trackedApps
      .filter((a) => a.productivityType === 'NON_PRODUCTIVE')
      .reduce((acc, a) => acc + a.totalTimeInForegroundMillis, 0);

    const neutralMillis = totalTimeMillis - productiveMillis - nonProductiveMillis;

    const rate = totalTimeMillis > 0 ? (productiveMillis / totalTimeMillis) * 100 : 0;

    // Top apps sorted by usage
    const sortedApps = [...trackedApps]
      .sort((a, b) => b.totalTimeInForegroundMillis - a.totalTimeInForegroundMillis)
      .map((a) => ({
        ...a,
        percentageOfTotal: totalTimeMillis > 0 ? (a.totalTimeInForegroundMillis / totalTimeMillis) * 100 : 0,
      }));

    // Category breakdown
    const catMap = new Map<string, number>();
    for (const a of trackedApps) {
      for (const c of a.categories) {
        catMap.set(c, (catMap.get(c) || 0) + a.totalTimeInForegroundMillis);
      }
    }
    const catBreakdown = Array.from(catMap.entries()).map(([cat, millis]) => ({
      category: cat,
      durationMillis: millis,
      percentage: totalTimeMillis > 0 ? (millis / totalTimeMillis) * 100 : 0,
      count: trackedApps.filter((a) => a.categories.includes(cat)).length,
    }));

    return {
      startDateLabel: 'Recent',
      endDateLabel: 'Today',
      totalUsageTimeMillis: totalTimeMillis,
      productiveTimeMillis: productiveMillis,
      nonProductiveTimeMillis: nonProductiveMillis,
      neutralTimeMillis: neutralMillis,
      focentraFocusMillis: 120 * factor * 60 * 1000,
      mostUsedApp: sortedApps[0] || null,
      appsCount: trackedApps.length,
      productiveAppsCount: trackedApps.filter((a) => a.productivityType === 'PRODUCTIVE').length,
      nonProductiveAppsCount: trackedApps.filter((a) => a.productivityType === 'NON_PRODUCTIVE').length,
      productivityRate: Math.round(rate),
      mostProductiveDayName: 'Wednesday',
      topApps: sortedApps,
      dailyTrend: [],
      categoryBreakdown: catBreakdown.sort((a, b) => b.durationMillis - a.durationMillis),
    };
  }, [installedApps, periodTab]);

  const formatHoursMins = (millis: number) => {
    const totalMinutes = Math.floor(millis / 60000);
    return TimeUtils.formatDuration(totalMinutes);
  };

  return (
    <div className="pb-28">
      {/* Top Bar */}
      <header className="sticky top-0 z-30 bg-white/95 dark:bg-[#090D16]/95 backdrop-blur-md px-4 py-3 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            onClick={onBack}
            className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h1 className="text-lg font-bold text-slate-900 dark:text-slate-100">
            Diagnostic Dashboard
          </h1>
        </div>

        <button
          onClick={onOpenAppClassification}
          className="text-xs font-bold text-emerald-600 dark:text-emerald-400 hover:text-emerald-700 flex items-center gap-1"
        >
          <Sliders className="w-4 h-4" />
          Classify
        </button>
      </header>

      <main className="max-w-md mx-auto px-4 pt-4 space-y-4">
        {/* Period Selector Tabs */}
        <div className="flex bg-white dark:bg-[#111827] p-1 rounded-2xl border border-slate-200/80 dark:border-slate-800">
          {['Today', 'Weekly', 'Monthly', 'Custom'].map((tab, idx) => (
            <button
              key={tab}
              onClick={() => setPeriodTab(idx)}
              className={`flex-1 py-1.5 rounded-xl text-xs font-bold transition-all ${
                periodTab === idx
                  ? 'bg-slate-900 text-white dark:bg-white dark:text-slate-900 shadow-2xs'
                  : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* Productivity Ratio Card */}
        <div className="bg-white dark:bg-[#111827] rounded-3xl p-5 shadow-xs border border-slate-200/80 dark:border-slate-800">
          <div className="flex items-center justify-between">
            <div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Productivity Ratio
              </span>
              <div className="text-3xl font-black text-slate-900 dark:text-slate-100 mt-0.5">
                {appUsageSummary.productivityRate}%
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Total Screen Time: {formatHoursMins(appUsageSummary.totalUsageTimeMillis)}
              </p>
            </div>

            <div className="w-14 h-14 rounded-2xl bg-emerald-50 dark:bg-emerald-950/30 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
              <Activity className="w-7 h-7" />
            </div>
          </div>

          {/* Ratio bar */}
          <div className="w-full bg-slate-100 dark:bg-slate-800 h-3 rounded-full overflow-hidden flex mt-4">
            <div
              style={{
                width: `${(appUsageSummary.productiveTimeMillis / appUsageSummary.totalUsageTimeMillis) * 100}%`,
              }}
              className="bg-emerald-500 h-full"
              title="Productive Time"
            />
            <div
              style={{
                width: `${(appUsageSummary.neutralTimeMillis / appUsageSummary.totalUsageTimeMillis) * 100}%`,
              }}
              className="bg-slate-400 h-full"
              title="Neutral Time"
            />
            <div
              style={{
                width: `${(appUsageSummary.nonProductiveTimeMillis / appUsageSummary.totalUsageTimeMillis) * 100}%`,
              }}
              className="bg-rose-500 h-full"
              title="Non-Productive / Distraction"
            />
          </div>

          <div className="flex items-center justify-between text-[11px] pt-3 text-slate-500 dark:text-slate-400">
            <span className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-500" />
              Productive ({formatHoursMins(appUsageSummary.productiveTimeMillis)})
            </span>
            <span className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-rose-500" />
              Distraction ({formatHoursMins(appUsageSummary.nonProductiveTimeMillis)})
            </span>
          </div>
        </div>

        {/* Top Apps Section */}
        <div className="bg-white dark:bg-[#111827] rounded-3xl p-5 shadow-xs border border-slate-200/80 dark:border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">
              App Screen Time Breakdown
            </h3>
            <span className="text-xs text-slate-400">{appUsageSummary.appsCount} apps tracked</span>
          </div>

          <div className="space-y-3 pt-1">
            {appUsageSummary.topApps.slice(0, 6).map((app) => (
              <div key={app.packageName} className="space-y-1">
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-800 dark:text-slate-200">
                      {app.appName}
                    </span>
                    <span
                      className={`text-[10px] px-1.5 py-0.2 rounded font-semibold ${
                        app.productivityType === 'PRODUCTIVE'
                          ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600'
                          : app.productivityType === 'NON_PRODUCTIVE'
                          ? 'bg-rose-50 dark:bg-rose-950/40 text-rose-600'
                          : 'bg-slate-100 dark:bg-slate-800 text-slate-500'
                      }`}
                    >
                      {app.qualityRating.replace('_', ' ')}
                    </span>
                  </div>
                  <span className="font-mono text-slate-500">
                    {formatHoursMins(app.totalTimeInForegroundMillis)}
                  </span>
                </div>

                <div className="w-full bg-slate-100 dark:bg-slate-800 h-2 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full ${
                      app.productivityType === 'PRODUCTIVE'
                        ? 'bg-emerald-500'
                        : app.productivityType === 'NON_PRODUCTIVE'
                        ? 'bg-rose-500'
                        : 'bg-slate-400'
                    }`}
                    style={{ width: `${Math.round(app.percentageOfTotal)}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Category Breakdown */}
        <div className="bg-white dark:bg-[#111827] rounded-3xl p-5 shadow-xs border border-slate-200/80 dark:border-slate-800 space-y-3">
          <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">
            Usage By Category
          </h3>
          <div className="grid grid-cols-2 gap-2">
            {appUsageSummary.categoryBreakdown.slice(0, 6).map((cat) => (
              <div
                key={cat.category}
                className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/60 dark:border-slate-800"
              >
                <div className="text-xs font-bold text-slate-800 dark:text-slate-200">
                  {cat.category}
                </div>
                <div className="text-sm font-black text-slate-900 dark:text-slate-100 mt-0.5">
                  {formatHoursMins(cat.durationMillis)}
                </div>
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
};
