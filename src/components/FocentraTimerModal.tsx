import React, { useState, useEffect } from 'react';
import { X, Play, Pause, RotateCcw, CheckCircle, Clock } from 'lucide-react';
import { FocentraStudySession } from '../types';
import confetti from 'canvas-confetti';

interface FocentraTimerModalProps {
  onSessionComplete: (session: FocentraStudySession) => void;
  onClose: () => void;
}

export const FocentraTimerModal: React.FC<FocentraTimerModalProps> = ({
  onSessionComplete,
  onClose,
}) => {
  const [subject, setSubject] = useState('Deep Work');
  const [topic, setTopic] = useState('');
  const [targetMinutes, setTargetMinutes] = useState(25);
  const [timeLeft, setTimeLeft] = useState(25 * 60);
  const [isRunning, setIsRunning] = useState(false);
  const [sessionStartTime, setSessionStartTime] = useState<number | null>(null);

  useEffect(() => {
    let interval: any = null;
    if (isRunning && timeLeft > 0) {
      interval = setInterval(() => {
        setTimeLeft((prev) => prev - 1);
      }, 1000);
    } else if (timeLeft === 0 && isRunning) {
      setIsRunning(false);
      handleFinish();
    }
    return () => clearInterval(interval);
  }, [isRunning, timeLeft]);

  const handleStart = () => {
    if (!sessionStartTime) {
      setSessionStartTime(Date.now());
    }
    setIsRunning(true);
  };

  const handlePause = () => {
    setIsRunning(false);
  };

  const handleReset = () => {
    setIsRunning(false);
    setTimeLeft(targetMinutes * 60);
    setSessionStartTime(null);
  };

  const handleDurationSelect = (mins: number) => {
    if (!isRunning) {
      setTargetMinutes(mins);
      setTimeLeft(mins * 60);
    }
  };

  const handleFinish = () => {
    confetti({ particleCount: 50, spread: 60 });
    const elapsedSeconds = targetMinutes * 60 - timeLeft;
    const session: FocentraStudySession = {
      sessionId: `focentra_${Date.now()}`,
      subject,
      topic: topic.trim() || 'Focus Session',
      startTime: sessionStartTime || Date.now() - elapsedSeconds * 1000,
      endTime: Date.now(),
      duration: Math.max(elapsedSeconds, 60),
      completionStatus: 'COMPLETED',
      focusScore: Math.min(100, Math.round(90 + Math.random() * 10)),
      schemaVersion: 1,
    };
    onSessionComplete(session);
    onClose();
  };

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  const progressPercent = ((targetMinutes * 60 - timeLeft) / (targetMinutes * 60)) * 100;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
      <div className="bg-white dark:bg-[#111827] w-full max-w-sm rounded-3xl p-6 shadow-2xl border border-slate-200 dark:border-slate-800 text-center animate-in zoom-in-95">
        <div className="flex items-center justify-between pb-2">
          <div className="flex items-center gap-2">
            <div className="p-1.5 rounded-xl bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400">
              <Clock className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
              Focentra Study Focus
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Duration presets */}
        {!isRunning && (
          <div className="flex justify-center gap-2 my-3">
            {[15, 25, 45, 60].map((m) => (
              <button
                key={m}
                onClick={() => handleDurationSelect(m)}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                  targetMinutes === m
                    ? 'bg-blue-600 text-white'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'
                }`}
              >
                {m}m
              </button>
            ))}
          </div>
        )}

        {/* Circular Timer Display */}
        <div className="relative w-48 h-48 mx-auto my-4 flex items-center justify-center">
          <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
            <circle
              cx="50"
              cy="50"
              r="44"
              className="text-slate-100 dark:text-slate-800"
              strokeWidth="6"
              stroke="currentColor"
              fill="none"
            />
            <circle
              cx="50"
              cy="50"
              r="44"
              stroke="#3B82F6"
              strokeWidth="6"
              strokeLinecap="round"
              strokeDasharray="276.46"
              strokeDashoffset={276.46 - (276.46 * progressPercent) / 100}
              fill="none"
              className="transition-all duration-500"
            />
          </svg>
          <div className="absolute text-center">
            <div className="text-4xl font-black tracking-tight text-slate-900 dark:text-slate-100 font-mono">
              {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
            </div>
            <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider mt-1">
              {isRunning ? 'Focusing' : 'Paused'}
            </div>
          </div>
        </div>

        {/* Subject & Topic inputs */}
        <div className="space-y-2 mb-4 text-left">
          <div>
            <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Subject
            </label>
            <input
              type="text"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              disabled={isRunning}
              className="w-full px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs font-bold"
            />
          </div>
          <div>
            <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Topic / Note
            </label>
            <input
              type="text"
              placeholder="e.g. Chapter 4 revision or Project Architecture"
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              className="w-full px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-xs"
            />
          </div>
        </div>

        {/* Control Buttons */}
        <div className="flex items-center justify-center gap-3 pt-2">
          <button
            onClick={handleReset}
            className="p-3 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors"
            title="Reset"
          >
            <RotateCcw className="w-5 h-5" />
          </button>

          {isRunning ? (
            <button
              onClick={handlePause}
              className="px-6 py-3 rounded-2xl bg-amber-500 hover:bg-amber-600 text-white font-bold flex items-center gap-2 shadow-xs transition-all"
            >
              <Pause className="w-5 h-5 fill-current" />
              Pause
            </button>
          ) : (
            <button
              onClick={handleStart}
              className="px-6 py-3 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-bold flex items-center gap-2 shadow-xs transition-all"
            >
              <Play className="w-5 h-5 fill-current" />
              Start Focus
            </button>
          )}

          {timeLeft < targetMinutes * 60 && (
            <button
              onClick={handleFinish}
              className="p-3 rounded-2xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-100 transition-colors"
              title="Finish and log session"
            >
              <CheckCircle className="w-5 h-5" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
