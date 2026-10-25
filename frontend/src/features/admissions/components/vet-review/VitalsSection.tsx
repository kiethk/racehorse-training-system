'use client';

import { FormField } from '@/components/ui/FormField';
import type { Dispatch, SetStateAction } from 'react';
import { Input, Select } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import type { MetricField } from './constants';
import { metricConfigs } from './constants';

export interface VitalsSectionProps {
  metrics: Record<MetricField, string>;
  setMetrics: Dispatch<SetStateAction<Record<MetricField, string>>>;
  fieldErrors: Record<string, string>;
  isFormDisabled: boolean;
  onRandomize: () => void;
}

export function VitalsSection({ metrics, setMetrics, fieldErrors, isFormDisabled, onRandomize }: VitalsSectionProps) {
  return (
    <fieldset disabled={isFormDisabled} className="min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)] space-y-4">
      <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2">
        <h3 className="text-sm font-semibold tracking-tight text-[var(--color-text-primary)]">
          1. Patient Vitals &amp; Telemetry
        </h3>
        <Button
          type="button"
          size="sm"
          variant="secondary"
          icon="sparkles"
          onClick={onRandomize}
          disabled={isFormDisabled}
          className="transition-all active:scale-[0.97]"
          title="Simulate automated telemetry scan and randomize realistic examination findings"
        >
          Simulate Auto-Exam
        </Button>
      </div>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-3 2xl:grid-cols-5">
        {metricConfigs.map((cfg) => {
          const valStr = metrics[cfg.key];
          // metric tone computed in parent? recompute here minimal
          let borderClass = 'border-[var(--color-border)] focus:border-[var(--color-primary)]';
          let statusLabel: string | null = null;
          let tone: 'warning' | 'danger' | 'success' | null = null;
          if (valStr.trim()) {
            const num = Number(valStr);
            if (Number.isNaN(num)) {
              borderClass = 'border-[var(--color-danger)] focus:border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]';
              statusLabel = 'Invalid';
              tone = 'danger';
            } else if (num < cfg.criticalMin) {
              borderClass = 'border-[var(--color-danger)] focus:border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]';
              statusLabel = 'Critical Low';
              tone = 'danger';
            } else if (num > cfg.criticalMax) {
              borderClass = 'border-[var(--color-danger)] focus:border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]';
              statusLabel = 'Critical High';
              tone = 'danger';
            } else if (num < cfg.normalMin) {
              borderClass = 'border-[var(--color-warning)] focus:border-[var(--color-warning)] ring-1 ring-[var(--color-warning)]';
              statusLabel = 'Low';
              tone = 'warning';
            } else if (num > cfg.normalMax) {
              borderClass = 'border-[var(--color-warning)] focus:border-[var(--color-warning)] ring-1 ring-[var(--color-warning)]';
              statusLabel = 'Elevated';
              tone = 'warning';
            } else {
              borderClass = 'border-[var(--color-success)] focus:border-[var(--color-success)]';
              statusLabel = 'Normal';
              tone = 'success';
            }
          }
          return (
            <div key={cfg.key} className="space-y-1">
              <div className="flex items-center justify-between text-xs">
                <label htmlFor={`metric-${cfg.key}`} className="font-medium text-[var(--color-text-primary)]">
                  {cfg.label}
                </label>
                {statusLabel && (
                  <span
                    className={`text-xs font-bold ${
                      tone === 'danger'
                        ? 'text-[var(--color-danger)]'
                        : tone === 'warning'
                          ? 'text-[var(--color-warning)]'
                          : 'text-[var(--color-success)]'
                    }`}
                  >
                    {statusLabel}
                  </span>
                )}
              </div>
              <div className="relative">
                <Input
                  id={`metric-${cfg.key}`}
                  type="number"
                  min="0"
                  step={cfg.step}
                  placeholder={cfg.placeholder}
                  value={metrics[cfg.key]}
                  onChange={(e) => setMetrics((curr) => ({ ...curr, [cfg.key]: e.target.value }))}
                  className={`w-full rounded-[var(--radius-sm)] border bg-[var(--color-surface)] px-3 pr-10 font-metric text-sm font-semibold text-[var(--color-text-primary)] outline-none transition-all ${borderClass}`}
                />
                <span className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-[var(--color-text-muted)]">
                  {cfg.unit}
                </span>
              </div>
              {fieldErrors[cfg.key] && (
                <span className="block text-xs font-medium text-[var(--color-danger)]">
                  {fieldErrors[cfg.key]}
                </span>
              )}
            </div>
          );
        })}
      </div>

      <div className="grid gap-3 sm:grid-cols-2 pt-1">
        <div>
          <FormField label="Hydration &amp; Mucous Membranes" htmlFor="metric-hydration">
            <Select
              id="metric-hydration"
              value={metrics.hydrationStatus}
              onChange={(e) => setMetrics((curr) => ({ ...curr, hydrationStatus: e.target.value }))}
            >
              <option value="">Not recorded</option>
              <option value="Normal (CRT < 2s, pink)">Normal (CRT &lt; 2s, pink)</option>
              <option value="Mild dehydration">Mild dehydration</option>
              <option value="Moderate dehydration">Moderate dehydration</option>
              <option value="Severe dehydration">Severe dehydration</option>
              <option value="Abnormal CRT / mucous membranes">Abnormal mucous membranes / CRT</option>
            </Select>
          </FormField>
        </div>
        <div>
          <FormField label="Telemetry &amp; Observation Context" htmlFor="metric-notes">
            <Input
              id="metric-notes"
              type="text"
              maxLength={2000}
              value={metrics.notes}
              onChange={(e) => setMetrics((curr) => ({ ...curr, notes: e.target.value }))}
              placeholder="e.g. Calm disposition, post-transport check"
            />
          </FormField>
        </div>
      </div>
    </fieldset>
  );
}