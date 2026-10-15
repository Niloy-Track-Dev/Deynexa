# Daynexa

<div align="center">

<h3>Build Better Days.</h3>

<p>A privacy-first, 100% offline daily routine, habit management, and productivity intelligence web application built with React, TypeScript, Vite, and Tailwind CSS.</p>

<p>
  <span style="background-color: #3B82F6; color: white; padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 12px;">React 19 + TypeScript</span>
  <span style="background-color: #10B981; color: white; padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 12px;">100% Offline-First</span>
  <span style="background-color: #0F172A; color: white; padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 12px;">Zero Telemetry</span>
  <span style="background-color: #8B5CF6; color: white; padding: 4px 8px; border-radius: 4px; font-weight: bold; font-size: 12px;">Productivity Score 2.0</span>
</p>

</div>

---

## 📑 Overview

**Daynexa** is a lightweight, distraction-free productivity application engineered to help you design, track, and sustain meaningful daily routines. Unlike conventional productivity tools that require cloud accounts, display advertisements, or monetize your activity logs, Daynexa operates **100% locally in your browser** with zero remote telemetry.

Featuring deterministic schedule computation, an interactive monthly calendar matrix, visual streak analytics, a 14-week consistency heatmap, **app & digital usage diagnostics**, **smart routine reminders**, and complete JSON backup export/import (SAF compatible), Daynexa gives you total control over your habits without sacrificing privacy.

---

## 🚀 Key Features

### 📅 Smart Daily Planner (`Today`)
- **Deterministic Scheduling Engine**: Evaluates recurring rules dynamically without generating redundant future database records. Supports Daily, Weekdays, Weekends, Specific Days of Week, Every X Days, Every X Weeks, Monthly, and Yearly.
- **Flexible Time Windows**: Scheduled start/end time ranges or flexible All-Day routines.
- **Interactive Progress Gauge**: Real-time circular completion gauge with Done, Pending, and Skipped counts.
- **Occurrence Rescheduling & Exceptions**: Adjust timing or target date for an individual occurrence without altering master recurring rules.
- **Accountability Skip Logging**: Record reasons when routines are skipped to maintain productivity insight accuracy.

### 🗓️ Interactive Calendar Matrix
- **Month-at-a-Glance**: Monthly calendar grid with color intensity dots representing completion rates (<50%, 50-79%, 80-100%).
- **Day Inspection**: Tap any date to inspect historical schedules, completion stats, and routines.
- **Quick Navigation**: Single-tap "Jump to Today" and previous/next month controls.

### 📊 Productivity Score 2.0 & Analytics
- **Composite Productivity Score**: Evaluates routine accomplishment, streak consistency, and active days on a 0–100 scale (High, Medium, Growing rating).
- **14-Week Consistency Heatmap**: 98-day activity matrix visualizing daily consistency over time with 4-level color scaling.
- **7-Day Trend Bar Chart**: Daily routine completion percentages across the past week.
- **Goals & Habit Streaks**: Target tracking for tasks completed, focus minutes, and high-priority tasks.
- **Weekly Review Debrief**: Automated weekly review synthesizing completion rates, focus minutes, and best days.

### 🏷️ Custom Categories & Color Palettes
- Color-coded categories with customizable icons (Heart, Briefcase, Person, Dumbbell, Book, Code, Home, Star, Brush, Clock, Coffee, Runner, Leaf, Brain, Graduation Cap).
- Real-time category filtering on the daily timeline.

### 📱 App Usage & Productivity Diagnostics
- **App Screen Time Breakdown**: Track foreground duration across configured applications.
- **App Quality Ratings**: Categorize applications into *Very Good*, *Good*, *Neutral*, *Not Good*, *Bad*, *Very Bad*.
- **Risk Level / Severity**: Tag applications with *Normal*, *High / Severe*, or *Critical / Fatal*.
- **Tagging & Productivity Ratios**: Multi-category tagging (Coding, Education, AI, Social Media, Entertainment, Tools, etc.) to compute productive vs non-productive ratios.

### ⏱️ Focentra Study Focus Integration
- Built-in focus session timer with subject, topic, and duration controls.
- Automatic logging into Focentra session history, syncing directly with productivity focus scores.

### ⚙️ Preferences & Data Portability
- **Appearance Themes**: Light, Dark, or System mode with brand color presets (Cobalt, Emerald, Amethyst, Amber, Rose).
- **Time Format**: Native support for 12-Hour (AM/PM) and 24-Hour clock formats.
- **Week Start & Weekend Customization**: Customize routine start day (Monday or Sunday) and weekend days.
- **Unified Backup & Restore**: Export full JSON backup bundle or Focentra session bundle; restore via Merge (safe integration) or Replace (full database restore).

---

## 🛠️ Development & Running Locally

### Prerequisites
- Node.js 20+ (Node.js 22 recommended)
- npm

### Installation & Run

```bash
# Install dependencies
npm install

# Start Vite dev server on port 3000
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

---

## 🔒 Privacy & Architecture

Daynexa is designed as a **zero-telemetry, offline-first** web application:
- Local storage only (`localStorage`) with clean JSON import/export.
- No third-party ad networks, telemetry SDKs, or cloud databases.
- All deterministic calculations and schedule evaluations execute locally in the browser runtime.

---

## 📄 License

Open-source software licensed under the [MIT License](LICENSE).
