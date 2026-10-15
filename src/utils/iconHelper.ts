import React from 'react';
import {
  Heart,
  Briefcase,
  User,
  Dumbbell,
  BookOpen,
  Code2,
  Home,
  Star,
  Paintbrush,
  Clock,
  Coffee,
  Footprints,
  Leaf,
  Brain,
  GraduationCap,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  LucideIcon,
} from 'lucide-react';

export const ICON_MAP: Record<string, LucideIcon> = {
  heart: Heart,
  briefcase: Briefcase,
  person: User,
  dumbbell: Dumbbell,
  book: BookOpen,
  code: Code2,
  home: Home,
  star: Star,
  brush: Paintbrush,
  clock: Clock,
  coffee: Coffee,
  runner: Footprints,
  leaf: Leaf,
  brain: Brain,
  'graduation-cap': GraduationCap,
};

export const AVAILABLE_ICONS = [
  'heart',
  'briefcase',
  'person',
  'dumbbell',
  'book',
  'code',
  'home',
  'star',
  'brush',
  'clock',
  'coffee',
  'runner',
  'leaf',
  'brain',
  'graduation-cap',
];

export const PRESET_COLORS = [
  { label: 'Blue', value: 0xff3b82f6, hex: '#3B82F6' },
  { label: 'Emerald', value: 0xff10b981, hex: '#10B981' },
  { label: 'Amber', value: 0xfff59e0b, hex: '#F59E0B' },
  { label: 'Rose', value: 0xfff43f5e, hex: '#F43F5E' },
  { label: 'Purple', value: 0xff8b5cf6, hex: '#8B5CF6' },
  { label: 'Cyan', value: 0xff06b6d4, hex: '#06B6D4' },
  { label: 'Indigo', value: 0xff6366f1, hex: '#6366F1' },
  { label: 'Teal', value: 0xff14b8a6, hex: '#14B8A6' },
];

export function getCategoryIcon(name: string): LucideIcon {
  return ICON_MAP[name] || Sparkles;
}

export function numberToHexColor(colorNum: number | undefined): string {
  if (!colorNum) return '#3B82F6';
  const hex = (colorNum & 0x00ffffff).toString(16).padStart(6, '0');
  return `#${hex}`;
}
