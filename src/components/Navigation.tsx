import React from 'react';
import { CalendarCheck, CalendarDays, BarChart3, Shapes, Settings } from 'lucide-react';

export type TabType = 'today' | 'calendar' | 'stats' | 'categories' | 'settings' | 'diagnostic' | 'app_classification';

interface NavigationProps {
  currentTab: TabType;
  onTabChange: (tab: TabType) => void;
  accentColor: string;
}

export const Navigation: React.FC<NavigationProps> = ({ currentTab, onTabChange, accentColor }) => {
  // If in sub-screens, highlight the parent tab
  const activeTab =
    currentTab === 'diagnostic' || currentTab === 'app_classification'
      ? 'settings'
      : currentTab;

  const navItems = [
    { id: 'today' as TabType, label: 'Today', icon: CalendarCheck },
    { id: 'calendar' as TabType, label: 'Calendar', icon: CalendarDays },
    { id: 'stats' as TabType, label: 'Stats', icon: BarChart3 },
    { id: 'categories' as TabType, label: 'Categories', icon: Shapes },
    { id: 'settings' as TabType, label: 'Settings', icon: Settings },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-white/95 dark:bg-[#111827]/95 backdrop-blur-md border-t border-slate-200 dark:border-slate-800 pb-safe">
      <div className="max-w-md mx-auto px-4 py-2 flex items-center justify-between">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onTabChange(item.id)}
              className="flex-1 flex flex-col items-center justify-center py-1 transition-all group focus:outline-none"
            >
              <div
                className={`flex items-center justify-center w-12 h-8 rounded-full transition-all ${
                  isActive
                    ? 'scale-105 shadow-sm'
                    : 'text-slate-500 dark:text-slate-400 group-hover:text-slate-800 dark:group-hover:text-slate-200'
                }`}
                style={
                  isActive
                    ? { backgroundColor: `${accentColor}20`, color: accentColor }
                    : undefined
                }
              >
                <Icon className={`w-5 h-5 ${isActive ? 'stroke-[2.5]' : 'stroke-2'}`} />
              </div>
              <span
                className={`text-[11px] font-medium tracking-tight mt-0.5 transition-colors ${
                  isActive
                    ? 'font-bold'
                    : 'text-slate-500 dark:text-slate-400'
                }`}
                style={isActive ? { color: accentColor } : undefined}
              >
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
