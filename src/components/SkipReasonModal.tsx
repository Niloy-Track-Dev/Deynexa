import React, { useState } from 'react';
import { SkipForward, X } from 'lucide-react';

interface SkipReasonModalProps {
  taskName: string;
  onConfirm: (reason: string) => void;
  onClose: () => void;
}

const COMMON_REASONS = [
  'Feeling unwell / Rest day',
  'Urgent meeting or work call',
  'Doctor or medical appointment',
  'Family or personal commitment',
  'Travel or out of town',
  'Rescheduled to tomorrow',
];

export const SkipReasonModal: React.FC<SkipReasonModalProps> = ({
  taskName,
  onConfirm,
  onClose,
}) => {
  const [reason, setReason] = useState('');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in">
      <div className="bg-white dark:bg-[#1F2937] w-full max-w-sm rounded-2xl p-5 shadow-xl border border-slate-200 dark:border-slate-700 animate-in zoom-in-95">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400">
              <SkipForward className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
                Skip Routine
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 truncate max-w-[200px]">
                {taskName}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-xs text-slate-500 dark:text-slate-400 mt-3">
          Logging a reason helps calibrate your weekly productivity insights and keeps you accountable.
        </p>

        {/* Quick Reason Chips */}
        <div className="flex flex-wrap gap-1.5 mt-3">
          {COMMON_REASONS.map((r) => (
            <button
              key={r}
              onClick={() => setReason(r)}
              className={`px-2.5 py-1 rounded-lg text-xs font-medium text-left transition-all ${
                reason === r
                  ? 'bg-rose-500 text-white font-bold'
                  : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
              }`}
            >
              {r}
            </button>
          ))}
        </div>

        <div className="mt-3">
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
            Custom Reason (Optional)
          </label>
          <input
            type="text"
            placeholder="Type reason here..."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-rose-500"
          />
        </div>

        <div className="flex items-center justify-end gap-2 mt-5 pt-3 border-t border-slate-100 dark:border-slate-800">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => onConfirm(reason)}
            className="px-4 py-2 rounded-xl text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 shadow-xs transition-all"
          >
            Skip for Today
          </button>
        </div>
      </div>
    </div>
  );
};
