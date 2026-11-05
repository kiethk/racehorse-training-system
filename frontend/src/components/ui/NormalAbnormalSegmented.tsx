import React from 'react';

interface NormalAbnormalSegmentedProps {
  value: 'NORMAL' | 'ABNORMAL';
  onChange: (value: 'NORMAL' | 'ABNORMAL') => void;
  label?: string;
  disabled?: boolean;
}

export function NormalAbnormalSegmented({
  value,
  onChange,
  label,
  disabled = false,
}: NormalAbnormalSegmentedProps) {
  return (
    <div className="space-y-1">
      {label && <label className="text-[12px] font-medium text-[var(--color-text-primary)]">{label}</label>}
      <div className="inline-flex rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-0.5">
        <button
          type="button"
          disabled={disabled}
          onClick={() => onChange('NORMAL')}
          className={`px-3 py-1 text-[12px] font-medium rounded transition-all ${
            value === 'NORMAL'
              ? 'bg-[var(--color-surface)] text-[var(--color-success)] shadow-sm'
              : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
          }`}
        >
          Bình thường
        </button>
        <button
          type="button"
          disabled={disabled}
          onClick={() => onChange('ABNORMAL')}
          className={`px-3 py-1 text-[12px] font-medium rounded transition-all ${
            value === 'ABNORMAL'
              ? 'bg-red-50 dark:bg-red-950/40 text-red-600 dark:text-red-400 font-semibold shadow-sm'
              : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
          }`}
        >
          Bất thường
        </button>
      </div>
    </div>
  );
}
