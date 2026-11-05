'use client';

import clsx from 'clsx';
import { Icon, LinkButton, Pill, type IconName } from '@/components/ui';

export interface UrgentActionCardProps {
  title: string;
  subtitle?: string;
  badgeText: string;
  badgeTone?: 'danger' | 'warning' | 'info' | 'success';
  actionLabel: string;
  actionHref: string;
  timeText?: string;
  icon?: IconName;
  className?: string;
}

export function UrgentActionCard({
  title,
  subtitle,
  badgeText,
  badgeTone = 'warning',
  actionLabel,
  actionHref,
  timeText,
  icon,
  className,
}: UrgentActionCardProps) {
  const borderToneClass =
    badgeTone === 'danger'
      ? 'border-l-rose-500'
      : badgeTone === 'warning'
      ? 'border-l-amber-500'
      : badgeTone === 'info'
      ? 'border-l-sky-500'
      : 'border-l-emerald-500';

  return (
    <div
      className={clsx(
        'flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] border-l-4 transition-all hover:shadow-xs',
        borderToneClass,
        className
      )}
    >
      <div className="flex items-start gap-3 min-w-0">
        {icon && (
          <div
            className={clsx(
              'mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md',
              badgeTone === 'danger' && 'bg-rose-500/10 text-rose-600 dark:text-rose-400',
              badgeTone === 'warning' && 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
              badgeTone === 'info' && 'bg-sky-500/10 text-sky-600 dark:text-sky-400',
              badgeTone === 'success' && 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
            )}
          >
            <Icon name={icon} size={16} />
          </div>
        )}

        <div className="min-w-0 space-y-0.5">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-semibold text-sm text-[var(--color-text-primary)] truncate">
              {title}
            </span>
            <Pill tone={badgeTone} size="sm">
              {badgeText}
            </Pill>
          </div>

          {subtitle && (
            <p className="text-xs text-[var(--color-text-secondary)] line-clamp-1">
              {subtitle}
            </p>
          )}

          {timeText && (
            <span className="text-[11px] text-[var(--color-text-muted)] block">
              {timeText}
            </span>
          )}
        </div>
      </div>

      <div className="shrink-0 self-end sm:self-center">
        <LinkButton href={actionHref} size="sm" variant="secondary" className="font-medium whitespace-nowrap">
          {actionLabel}
        </LinkButton>
      </div>
    </div>
  );
}
