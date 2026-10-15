import React from 'react';

export interface MetricThreshold {
  warningMin?: number;
  warningMax?: number;
  dangerMin?: number;
  dangerMax?: number;
  unit: string;
}

/**
 * Dải ngưỡng dạng khoảng kín [min, max] — khớp mô hình của backend
 * (`normalMin/Max`, `warningMin/Max`, `dangerMin/Max` là các dải tách rời).
 */
export interface MetricBands {
  warning: [number, number];
  danger: [number, number];
}

interface VitalsInputProps {
  label: string;
  value: string | number;
  onChange: (value: string) => void;
  threshold?: MetricThreshold;
  /**
   * Ưu tiên hơn `threshold` khi có: phân loại theo đúng dải khoảng của backend.
   * `threshold` khi đó chỉ còn dùng để hiển thị vùng chuẩn và đơn vị.
   */
  bands?: MetricBands;
  placeholder?: string;
  previousValue?: number | null;
  disabled?: boolean;
}

export function VitalsInput({
  label,
  value,
  onChange,
  threshold,
  bands,
  placeholder,
  previousValue,
  disabled = false,
}: VitalsInputProps) {
  const numValue = value === '' ? NaN : Number(value);

  // Xác định tình trạng: normal, warning, danger
  let status: 'normal' | 'warning' | 'danger' = 'normal';
  let message = '';

  if (!Number.isNaN(numValue) && bands) {
    if (numValue >= bands.danger[0] && numValue <= bands.danger[1]) {
      status = 'danger';
      message = 'Vượt ngưỡng nguy hiểm';
    } else if (numValue >= bands.warning[0] && numValue <= bands.warning[1]) {
      status = 'warning';
      message = 'Cảnh báo bất thường';
    }
  } else if (!Number.isNaN(numValue) && threshold) {
    if (
      (threshold.dangerMin !== undefined && numValue < threshold.dangerMin) ||
      (threshold.dangerMax !== undefined && numValue > threshold.dangerMax)
    ) {
      status = 'danger';
      message = 'Vượt ngưỡng nguy hiểm';
    } else if (
      (threshold.warningMin !== undefined && numValue < threshold.warningMin) ||
      (threshold.warningMax !== undefined && numValue > threshold.warningMax)
    ) {
      status = 'warning';
      message = 'Cảnh báo bất thường';
    }
  }

  // Delta so với lần trước
  let delta: number | null = null;
  if (!Number.isNaN(numValue) && previousValue !== undefined && previousValue !== null) {
    delta = Math.round((numValue - previousValue) * 10) / 10;
  }

  const borderClass =
    status === 'danger'
      ? 'border-red-500 focus:border-red-600 focus:ring-red-100 dark:border-red-500'
      : status === 'warning'
      ? 'border-amber-500 focus:border-amber-600 focus:ring-amber-100 dark:border-amber-500'
      : 'border-[var(--color-border)] focus:border-[var(--color-primary)] focus:ring-blue-100';

  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between">
        <label className="text-[12px] font-medium text-[var(--color-text-primary)]">
          {label} {threshold?.unit && <span className="text-[var(--color-text-muted)] font-normal">({threshold.unit})</span>}
        </label>
        {delta !== null && (
          <span
            className={`text-[11px] font-medium ${
              delta > 0 ? 'text-amber-600 dark:text-amber-400' : delta < 0 ? 'text-blue-600 dark:text-blue-400' : 'text-neutral-500'
            }`}
          >
            {delta > 0 ? `▲ +${delta}` : delta < 0 ? `▼ ${delta}` : '— 0.0'}
            <span className="text-[10px] text-[var(--color-text-muted)] ml-1 font-normal">(so với trước)</span>
          </span>
        )}
      </div>

      <div className="relative">
        <input
          type="number"
          step="any"
          value={value}
          disabled={disabled}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className={`w-full rounded-[var(--radius-sm)] border px-3 py-1.5 text-[13px] bg-[var(--color-surface)] text-[var(--color-text-primary)] transition-colors focus:outline-none focus:ring-2 ${borderClass} disabled:opacity-50 disabled:bg-[var(--color-surface-subtle)]`}
        />
      </div>

      <div className="flex items-center justify-between text-[11px]">
        {threshold && (
          <span className="text-[var(--color-text-muted)]">
            Chuẩn: {threshold.warningMin ?? '...'} - {threshold.warningMax ?? '...'} {threshold.unit}
          </span>
        )}
        {status !== 'normal' && (
          <span
            className={`font-medium ${
              status === 'danger' ? 'text-red-600 dark:text-red-400' : 'text-amber-600 dark:text-amber-400'
            }`}
          >
            {message}
          </span>
        )}
      </div>
    </div>
  );
}
