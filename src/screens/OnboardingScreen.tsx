import React, { useState } from 'react';
import { CloudOff, Repeat, Trophy, ShieldCheck, ArrowRight, Check } from 'lucide-react';
import { AppSettings } from '../types';

interface OnboardingScreenProps {
  onComplete: () => void;
  accentColor: string;
}

export const OnboardingScreen: React.FC<OnboardingScreenProps> = ({
  onComplete,
  accentColor,
}) => {
  const [step, setStep] = useState(0);

  const steps = [
    {
      title: 'Build Better Days',
      subtitle: 'Deterministic Daily Planner & Productivity Intelligence',
      description:
        'Daynexa brings effortless clarity to your daily schedule with zero-bloat deterministic recurrence, real-time schedule conflict prevention, and smart habit streaks.',
      icon: Trophy,
    },
    {
      title: '100% Offline & Private',
      subtitle: 'Zero telemetry. Zero tracking.',
      description:
        'Your routines, tasks, and usage metrics never leave your physical device. No account creation or internet connection required.',
      icon: CloudOff,
    },
    {
      title: 'Productivity Score 2.0',
      subtitle: 'Holistic accountability engine',
      description:
        'Calculates real-time productivity score balancing task completion rates, streak momentum, and deep focus time from Focentra sessions.',
      icon: ShieldCheck,
    },
  ];

  const current = steps[step];
  const Icon = current.icon;

  const handleNext = () => {
    if (step < steps.length - 1) {
      setStep(step + 1);
    } else {
      onComplete();
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 dark:bg-[#090D16] flex flex-col justify-between p-6 max-w-md mx-auto">
      <div className="flex justify-end pt-2">
        <button
          onClick={onComplete}
          className="text-xs font-bold text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
        >
          Skip
        </button>
      </div>

      <div className="my-auto text-center px-4">
        <div
          className="w-20 h-20 rounded-3xl mx-auto flex items-center justify-center shadow-md mb-6"
          style={{ backgroundColor: `${accentColor}20`, color: accentColor }}
        >
          <Icon className="w-10 h-10 stroke-[2]" />
        </div>

        <h1 className="text-2xl font-black text-slate-900 dark:text-slate-100 tracking-tight">
          {current.title}
        </h1>
        <h2
          className="text-sm font-bold mt-1"
          style={{ color: accentColor }}
        >
          {current.subtitle}
        </h2>
        <p className="text-xs text-slate-600 dark:text-slate-400 mt-4 leading-relaxed max-w-xs mx-auto">
          {current.description}
        </p>

        {/* Step dots */}
        <div className="flex justify-center gap-2 mt-8">
          {steps.map((_, i) => (
            <span
              key={i}
              className={`h-2 rounded-full transition-all ${
                i === step
                  ? 'w-6 bg-blue-600'
                  : 'w-2 bg-slate-300 dark:bg-slate-700'
              }`}
            />
          ))}
        </div>
      </div>

      <div className="pb-6">
        <button
          onClick={handleNext}
          style={{ backgroundColor: accentColor }}
          className="w-full py-4 rounded-2xl text-white font-bold text-sm shadow-md hover:opacity-95 active:scale-98 transition-all flex items-center justify-center gap-2"
        >
          {step === steps.length - 1 ? (
            <>
              Get Started <Check className="w-4 h-4 stroke-[3]" />
            </>
          ) : (
            <>
              Continue <ArrowRight className="w-4 h-4" />
            </>
          )}
        </button>
      </div>
    </div>
  );
};
