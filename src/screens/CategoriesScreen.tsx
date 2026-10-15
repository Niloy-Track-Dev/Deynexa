import React, { useState } from 'react';
import { Plus, MoreVertical, Edit2, Trash2, Check, X } from 'lucide-react';
import { Category, Task } from '../types';
import {
  PRESET_COLORS,
  AVAILABLE_ICONS,
  getCategoryIcon,
  numberToHexColor,
} from '../utils/iconHelper';

interface CategoriesScreenProps {
  categories: Category[];
  tasks: Task[];
  accentColor: string;
  onSaveCategory: (category: Category) => void;
  onDeleteCategory: (id: number) => void;
}

export const CategoriesScreen: React.FC<CategoriesScreenProps> = ({
  categories,
  tasks,
  accentColor,
  onSaveCategory,
  onDeleteCategory,
}) => {
  const [modalOpen, setModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [name, setName] = useState('');
  const [selectedColor, setSelectedColor] = useState(PRESET_COLORS[0].value);
  const [selectedIcon, setSelectedIcon] = useState(AVAILABLE_ICONS[0]);
  const [menuOpenId, setMenuOpenId] = useState<number | null>(null);

  const handleOpenCreate = () => {
    setEditingCategory(null);
    setName('');
    setSelectedColor(PRESET_COLORS[0].value);
    setSelectedIcon(AVAILABLE_ICONS[0]);
    setModalOpen(true);
  };

  const handleOpenEdit = (cat: Category) => {
    setEditingCategory(cat);
    setName(cat.name);
    setSelectedColor(cat.color);
    setSelectedIcon(cat.icon || AVAILABLE_ICONS[0]);
    setModalOpen(true);
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSaveCategory({
      id: editingCategory?.id || 0,
      name: name.trim(),
      color: selectedColor,
      icon: selectedIcon,
    });
    setModalOpen(false);
  };

  return (
    <div className="pb-28">
      {/* Top Header */}
      <header className="sticky top-0 z-30 bg-slate-100/90 dark:bg-[#090D16]/90 backdrop-blur-md px-4 pt-4 pb-3 border-b border-slate-200/60 dark:border-slate-800/60">
        <div className="max-w-md mx-auto">
          <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            {categories.length} {categories.length === 1 ? 'Category' : 'Categories'}
          </div>
          <h1 className="text-xl font-extrabold text-slate-900 dark:text-slate-100">
            Categories
          </h1>
        </div>
      </header>

      <main className="max-w-md mx-auto px-4 pt-4 space-y-3">
        {categories.map((cat) => {
          const catColor = numberToHexColor(cat.color);
          const CatIcon = getCategoryIcon(cat.icon);
          const taskCount = tasks.filter((t) => t.categoryId === cat.id).length;

          return (
            <div
              key={cat.id}
              className="bg-white dark:bg-[#111827] rounded-2xl p-4 border border-slate-200/80 dark:border-slate-800 shadow-2xs flex items-center justify-between"
            >
              <div className="flex items-center gap-3.5">
                <div
                  className="w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 transition-transform"
                  style={{
                    backgroundColor: `${catColor}18`,
                    color: catColor,
                  }}
                >
                  <CatIcon className="w-5 h-5 stroke-[2.2]" />
                </div>

                <div>
                  <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
                    {cat.name}
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                    {taskCount} {taskCount === 1 ? 'routine' : 'routines'}
                  </p>
                </div>
              </div>

              {/* Menu */}
              <div className="relative">
                <button
                  onClick={() => setMenuOpenId(menuOpenId === cat.id ? null : cat.id)}
                  className="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors"
                >
                  <MoreVertical className="w-5 h-5" />
                </button>

                {menuOpenId === cat.id && (
                  <>
                    <div
                      className="fixed inset-0 z-40"
                      onClick={() => setMenuOpenId(null)}
                    />
                    <div className="absolute right-0 top-8 z-50 w-36 bg-white dark:bg-[#1F2937] rounded-xl shadow-lg border border-slate-200 dark:border-slate-700 py-1">
                      <button
                        onClick={() => {
                          setMenuOpenId(null);
                          handleOpenEdit(cat);
                        }}
                        className="w-full px-3 py-2 text-left text-xs font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 flex items-center gap-2"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                        Edit
                      </button>
                      <button
                        onClick={() => {
                          setMenuOpenId(null);
                          onDeleteCategory(cat.id);
                        }}
                        className="w-full px-3 py-2 text-left text-xs font-semibold text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 flex items-center gap-2"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        Delete
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>
          );
        })}
      </main>

      {/* Floating Action Button */}
      <button
        onClick={handleOpenCreate}
        aria-label="Add Category"
        className="fixed bottom-20 right-5 z-40 p-4 rounded-2xl text-white shadow-lg shadow-emerald-500/25 bg-emerald-600 hover:bg-emerald-700 active:scale-95 transition-all flex items-center justify-center"
      >
        <Plus className="w-6 h-6 stroke-[2.5]" />
      </button>

      {/* Modal - Matching screenshot photo_2026-10-15_08-58-12.jpg */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white dark:bg-[#1F2937] w-full max-w-sm rounded-3xl p-6 shadow-2xl border border-slate-200 dark:border-slate-700 animate-in zoom-in-95">
            <h2 className="text-lg font-extrabold text-slate-900 dark:text-slate-100">
              {editingCategory ? 'Edit Category' : 'New Category'}
            </h2>

            <form onSubmit={handleSave} className="space-y-4 mt-4">
              {/* Category Name input */}
              <div>
                <input
                  type="text"
                  required
                  placeholder="Category Name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-4 py-3 rounded-2xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm font-semibold text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              {/* Color dots row */}
              <div>
                <label className="block text-xs font-bold text-slate-600 dark:text-slate-400 mb-2">
                  Color
                </label>
                <div className="flex items-center justify-between gap-1">
                  {PRESET_COLORS.map((c) => {
                    const isSelected = selectedColor === c.value;
                    return (
                      <button
                        key={c.hex}
                        type="button"
                        onClick={() => setSelectedColor(c.value)}
                        className="w-8 h-8 rounded-full flex items-center justify-center transition-transform hover:scale-110 relative"
                        style={{ backgroundColor: c.hex }}
                      >
                        {isSelected && <Check className="w-4 h-4 text-white stroke-[3]" />}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Icon grid 3x5 */}
              <div>
                <label className="block text-xs font-bold text-slate-600 dark:text-slate-400 mb-2">
                  Icon
                </label>
                <div className="grid grid-cols-5 gap-2.5">
                  {AVAILABLE_ICONS.map((iconName) => {
                    const IconComp = getCategoryIcon(iconName);
                    const isSelected = selectedIcon === iconName;
                    return (
                      <button
                        key={iconName}
                        type="button"
                        onClick={() => setSelectedIcon(iconName)}
                        className={`h-11 rounded-2xl flex items-center justify-center transition-all ${
                          isSelected
                            ? 'bg-emerald-100 dark:bg-emerald-950/60 border-2 border-emerald-500 text-emerald-600 dark:text-emerald-400 scale-105'
                            : 'bg-slate-50 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 hover:bg-slate-100'
                        }`}
                      >
                        <IconComp className="w-5 h-5 stroke-[2]" />
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Cancel / Save buttons matching screenshot */}
              <div className="flex items-center justify-end gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-5 py-2.5 rounded-xl text-xs font-bold text-emerald-600 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-950/30 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-6 py-2.5 rounded-xl text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 shadow-xs transition-all"
                >
                  Save
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
