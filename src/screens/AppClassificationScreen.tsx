import React, { useState } from 'react';
import {
  ArrowLeft,
  Search,
  Plus,
  BookOpen,
  Check,
  Smartphone,
  Shield,
  Clock,
  Sparkles,
} from 'lucide-react';
import {
  InstalledAppInfo,
  AppQualityRating,
  ProductivityType,
  AppSeverity,
} from '../types';

interface AppClassificationScreenProps {
  apps: InstalledAppInfo[];
  onBack: () => void;
  onSaveClassification: (app: InstalledAppInfo) => void;
}

const DEFAULT_CATEGORY_OPTIONS = [
  'Ai',
  'Browser',
  'Coding',
  'Communication',
  'Education',
  'Entertainment',
  'Games',
  'Name Jap',
  'Other',
  'Productivity',
  'Social Media',
  'Study Timer',
  'System App',
  'Tools',
  'Utilities',
];

const RED_DOT_CATEGORIES = ['Entertainment', 'Games', 'Social Media'];

export const AppClassificationScreen: React.FC<AppClassificationScreenProps> = ({
  apps,
  onBack,
  onSaveClassification,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedApp, setSelectedApp] = useState<InstalledAppInfo | null>(null);

  // Modal edit state
  const [rating, setRating] = useState<AppQualityRating>('NEUTRAL');
  const [isUsageTracked, setIsUsageTracked] = useState(true);
  const [severity, setSeverity] = useState<AppSeverity>('NORMAL');
  const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
  const [customCategories, setCustomCategories] = useState<string[]>([]);
  const [isAddingCategory, setIsAddingCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');

  const filteredApps = apps.filter(
    (a) =>
      a.appName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      a.packageName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleOpenEdit = (app: InstalledAppInfo) => {
    setSelectedApp(app);
    setRating(app.qualityRating);
    setIsUsageTracked(app.isUsageTracked);
    setSeverity(app.severity);
    setSelectedCategories(app.categories || []);
  };

  const handleToggleCategory = (cat: string) => {
    if (selectedCategories.includes(cat)) {
      setSelectedCategories(selectedCategories.filter((c) => c !== cat));
    } else {
      setSelectedCategories([...selectedCategories, cat]);
    }
  };

  const handleAddNewCategory = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCategoryName.trim()) return;
    const name = newCategoryName.trim();
    if (!customCategories.includes(name) && !DEFAULT_CATEGORY_OPTIONS.includes(name)) {
      setCustomCategories([...customCategories, name]);
      setSelectedCategories([...selectedCategories, name]);
    }
    setNewCategoryName('');
    setIsAddingCategory(false);
  };

  const handleSaveModal = () => {
    if (!selectedApp) return;

    // Infer productivity type
    let prodType: ProductivityType = 'NEUTRAL';
    if (rating === 'VERY_GOOD' || rating === 'GOOD') prodType = 'PRODUCTIVE';
    else if (rating === 'BAD' || rating === 'VERY_BAD' || rating === 'NOT_GOOD') prodType = 'NON_PRODUCTIVE';
    else if (selectedCategories.some((c) => ['Education', 'Coding', 'Productivity', 'Study Timer'].includes(c))) {
      prodType = 'PRODUCTIVE';
    } else if (selectedCategories.some((c) => ['Entertainment', 'Games', 'Social Media'].includes(c))) {
      prodType = 'NON_PRODUCTIVE';
    }

    const updated: InstalledAppInfo = {
      ...selectedApp,
      qualityRating: rating,
      isUsageTracked,
      severity,
      categories: selectedCategories,
      productivityType: prodType,
    };

    onSaveClassification(updated);
    setSelectedApp(null);
  };

  const allAvailableCategories = [...DEFAULT_CATEGORY_OPTIONS, ...customCategories];

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
            Manage App Classifications
          </h1>
        </div>
      </header>

      <main className="max-w-md mx-auto px-4 pt-4 space-y-3">
        {/* Search bar */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
          <input
            type="text"
            placeholder="Search installed apps..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#111827] text-xs font-semibold text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-500"
          />
        </div>

        {/* Apps List */}
        <div className="space-y-2">
          {filteredApps.map((app) => (
            <div
              key={app.packageName}
              onClick={() => handleOpenEdit(app)}
              className="bg-white dark:bg-[#111827] rounded-2xl p-4 border border-slate-200/80 dark:border-slate-800 hover:border-emerald-500/50 cursor-pointer transition-all shadow-2xs flex items-center justify-between"
            >
              <div className="flex items-center gap-3">
                <div className="w-11 h-11 rounded-2xl bg-slate-900 text-white flex items-center justify-center shrink-0">
                  <Smartphone className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">
                    {app.appName}
                  </h3>
                  <p className="text-[11px] text-slate-400 font-mono truncate max-w-[180px]">
                    {app.packageName}
                  </p>
                  <div className="flex items-center gap-1.5 mt-1 flex-wrap">
                    <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                      {app.qualityRating.replace('_', ' ')}
                    </span>
                    {app.categories.slice(0, 2).map((cat) => (
                      <span
                        key={cat}
                        className="px-2 py-0.5 rounded-md text-[10px] font-medium bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400"
                      >
                        {cat}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400 shrink-0">
                Edit
              </span>
            </div>
          ))}
        </div>
      </main>

      {/* App Classification Modal - Matching screenshots photo_2026-10-15_08-58-16.jpg & 14.jpg */}
      {selectedApp && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs overflow-y-auto animate-in fade-in">
          <div className="bg-[#F8FAFC] dark:bg-[#111827] w-full max-w-sm rounded-[32px] p-6 shadow-2xl border border-slate-200 dark:border-slate-800 my-8 max-h-[90vh] overflow-y-auto animate-in zoom-in-95">
            {/* Modal App Header */}
            <div className="flex items-center gap-3 pb-3 border-b border-slate-200/60 dark:border-slate-800">
              <div className="w-12 h-12 rounded-2xl bg-[#0F172A] text-white flex items-center justify-center shrink-0 shadow-xs">
                <BookOpen className="w-6 h-6" />
              </div>
              <div className="min-w-0 flex-1">
                <h2 className="text-base font-extrabold text-slate-900 dark:text-slate-100 truncate">
                  {selectedApp.appName}
                </h2>
                <p className="text-xs text-slate-500 dark:text-slate-400 font-mono truncate">
                  {selectedApp.packageName}
                </p>
              </div>
            </div>

            <div className="space-y-5 mt-4">
              {/* App Quality Rating */}
              <div>
                <label className="block text-xs font-bold text-slate-800 dark:text-slate-200 mb-2">
                  App Quality Rating
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {(
                    [
                      { id: 'VERY_GOOD', label: 'Very Good' },
                      { id: 'GOOD', label: 'Good' },
                      { id: 'NEUTRAL', label: 'Neutral' },
                      { id: 'NOT_GOOD', label: 'Not Good' },
                      { id: 'BAD', label: 'Bad' },
                      { id: 'VERY_BAD', label: 'Very Bad' },
                    ] as const
                  ).map((r) => {
                    const active = rating === r.id;
                    return (
                      <button
                        key={r.id}
                        type="button"
                        onClick={() => setRating(r.id)}
                        className={`py-2 rounded-2xl text-xs font-semibold transition-all ${
                          active
                            ? 'bg-slate-900 text-white dark:bg-white dark:text-slate-900 font-bold shadow-xs'
                            : 'bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:bg-slate-100'
                        }`}
                      >
                        {r.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Count Usage History Switch */}
              <div className="flex items-center justify-between p-3 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200/80 dark:border-slate-700">
                <div>
                  <div className="text-xs font-bold text-slate-900 dark:text-slate-100">
                    Count Usage History
                  </div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400">
                    App usage is included in total screen time.
                  </div>
                </div>

                <label className="relative inline-flex items-center cursor-pointer shrink-0">
                  <input
                    type="checkbox"
                    checked={isUsageTracked}
                    onChange={(e) => setIsUsageTracked(e.target.checked)}
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-500"></div>
                </label>
              </div>

              {/* App Severity / Risk Level */}
              <div>
                <label className="block text-xs font-bold text-slate-800 dark:text-slate-200 mb-2">
                  App Severity / Risk Level
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {(
                    [
                      { id: 'NORMAL', label: 'Normal' },
                      { id: 'HIGH', label: 'High / Severe' },
                      { id: 'FATAL', label: 'Critical / Fatal' },
                    ] as const
                  ).map((s) => {
                    const active = severity === s.id;
                    return (
                      <button
                        key={s.id}
                        type="button"
                        onClick={() => setSeverity(s.id)}
                        className={`py-2 rounded-2xl text-[11px] font-semibold transition-all ${
                          active
                            ? 'bg-slate-900 text-white dark:bg-white dark:text-slate-900 font-bold shadow-xs'
                            : 'bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700'
                        }`}
                      >
                        {s.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Categories (Multiple Allowed) with + New button */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-bold text-slate-800 dark:text-slate-200">
                    Categories (Multiple Allowed)
                  </label>
                  <button
                    type="button"
                    onClick={() => setIsAddingCategory(true)}
                    className="text-xs font-bold text-emerald-600 dark:text-emerald-400 hover:text-emerald-700 flex items-center gap-1"
                  >
                    + New
                  </button>
                </div>

                {isAddingCategory && (
                  <form onSubmit={handleAddNewCategory} className="flex gap-2 mb-2.5">
                    <input
                      type="text"
                      placeholder="Category name"
                      value={newCategoryName}
                      onChange={(e) => setNewCategoryName(e.target.value)}
                      className="flex-1 px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-medium"
                    />
                    <button
                      type="submit"
                      className="px-3 py-1.5 rounded-xl text-xs font-bold bg-emerald-600 text-white"
                    >
                      Add
                    </button>
                  </form>
                )}

                <div className="flex flex-wrap gap-2">
                  {allAvailableCategories.map((cat) => {
                    const isSelected = selectedCategories.includes(cat);
                    const hasRedDot = RED_DOT_CATEGORIES.includes(cat);

                    return (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => handleToggleCategory(cat)}
                        className={`px-3 py-1.5 rounded-2xl text-xs font-medium flex items-center gap-1.5 transition-all ${
                          isSelected
                            ? 'bg-emerald-500 text-white font-bold shadow-2xs'
                            : 'bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:bg-slate-100'
                        }`}
                      >
                        {cat}
                        {hasRedDot && (
                          <span className="w-1.5 h-1.5 rounded-full bg-rose-500" />
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Cancel / Save matching screenshot */}
            <div className="flex items-center justify-end gap-3 mt-6 pt-4 border-t border-slate-200/60 dark:border-slate-800">
              <button
                type="button"
                onClick={() => setSelectedApp(null)}
                className="px-5 py-2.5 rounded-2xl text-xs font-bold text-emerald-600 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-950/30 transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveModal}
                className="px-6 py-2.5 rounded-2xl text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 shadow-xs transition-all"
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
