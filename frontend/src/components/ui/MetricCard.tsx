import type { IconName } from './Icon';
import { Icon } from './Icon';

type Tone = 'default' | 'neutral' | 'success' | 'warning' | 'danger' | 'info';

const toneText: Record<Tone, string> = {
  default: 'text-[var(--color-text-primary)]',
  neutral: 'text-[var(--color-text-primary)]',
  success: 'text-[var(--color-success)]',
  warning: 'text-[var(--color-warning)]',
  danger: 'text-[var(--color-danger)]',
  info: 'text-[var(--color-info)]',
};

interface MetricCardProps {
  label: string;
  value: string | number;
  unit?: string;
  icon?: IconName;
  tone?: Tone;
  hint?: string;
  onClick?: () => void;
}

/** Compact Prism-style operational metric. Not a marketing KPI card. */
export function MetricCard({ label, value, unit, icon, tone = 'default', hint, onClick }: MetricCardProps) {
  const Wrapper = onClick ? 'button' : 'div';
  return (
    <Wrapper
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      onClick={onClick as any}
      className={
        'flex flex-col rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 text-left ' +
        (onClick
          ? 'outline-none transition-colors hover:border-[var(--color-border-strong)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]'
          : '')
      }
    >
      <div className="flex items-center gap-1.5 text-[var(--color-text-muted)]">
        {icon && <Icon name={icon} size={13} />}
        <span className="text-[11px] font-medium uppercase tracking-wide">{label}</span>
      </div>
      <div className="mt-1.5 flex items-baseline gap-1">
        <span className={`font-metric text-[22px] font-semibold leading-none ${toneText[tone]}`}>
          {value}
        </span>
        {unit && <span className="text-[12px] text-[var(--color-text-secondary)]">{unit}</span>}
      </div>
      {hint && <p className="mt-1 text-[11px] text-[var(--color-text-muted)]">{hint}</p>}
    </Wrapper>
  );
}
