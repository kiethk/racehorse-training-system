'use client';

import React from 'react';
import type { CorrelationStrength } from '../types/eda';
import { formatCorrelationStrength } from '../lib/edaTransform';

interface CorrelationBadgeProps {
  strength: CorrelationStrength;
  pearsonR: number;
}

export function CorrelationBadge({ strength, pearsonR }: CorrelationBadgeProps) {
  const info = formatCorrelationStrength(strength, pearsonR);

  const toneClasses: Record<string, string> = {
    info: 'bg-[var(--color-info-subtle)] text-[var(--color-info)] border-[var(--color-info)]/20',
    success: 'bg-[var(--color-success-subtle)] text-[var(--color-success)] border-[var(--color-success)]/20',
    warning: 'bg-[var(--color-warning-subtle)] text-[var(--color-warning)] border-[var(--color-warning)]/20',
    danger: 'bg-[var(--color-danger-subtle)] text-[var(--color-danger)] border-[var(--color-danger)]/20',
    neutral: 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] border-[var(--color-border)]',
  };

  const badgeClass = toneClasses[info.tone] || toneClasses.neutral;

  return (
    <div className="inline-flex flex-col gap-0.5">
      <div
        title={info.tooltip}
        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border cursor-help ${badgeClass}`}
      >
        <span className="w-1.5 h-1.5 rounded-full bg-current" />
        <span>{info.qualitativeLabel}</span>
        {info.tooltip.startsWith('Pearson r =') && !info.tooltip.includes('N/A') && (
          <span className="text-[11px] font-mono opacity-85">({pearsonR > 0 ? `+${pearsonR.toFixed(2)}` : pearsonR.toFixed(2)})</span>
        )}
      </div>
      <span className="text-[11px] text-[var(--color-text-secondary)] italic">
        {info.description}
      </span>
    </div>
  );
}
