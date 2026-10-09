import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { Icon, type IconName } from './Icon';

const variants = {
  success: { icon: 'check', className: 'bg-[var(--color-success-soft)] text-[var(--color-success)]' },
  error: { icon: 'alert-triangle', className: 'bg-[var(--color-danger-soft)] text-[var(--color-danger)]' },
  info: { icon: 'info', className: 'bg-[var(--color-info-soft)] text-[var(--color-info)]' },
  warning: { icon: 'alert-triangle', className: 'bg-[var(--color-warning-soft)] text-[var(--color-warning)]' },
} satisfies Record<string, { icon: IconName; className: string }>;

/** Inline message box. Use this for every form/section error, warning or success message. */
export function Notice({
  tone = 'info',
  title,
  children,
  onDismiss,
  className,
}: {
  tone?: keyof typeof variants;
  title?: string;
  children?: ReactNode;
  onDismiss?: () => void;
  className?: string;
}) {
  const variant = variants[tone];
  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={cn(
        'flex items-start gap-2 rounded-[var(--radius-md)] px-3 py-2.5 text-sm animate-in fade-in slide-in-from-top-1 duration-200',
        variant.className,
        className,
      )}
    >
      <Icon name={variant.icon} size={16} className="mt-0.5 shrink-0" />
      <div className="min-w-0 flex-1">
        {title && <p className="font-semibold">{title}</p>}
        {children}
      </div>
      {onDismiss && (
        <button
          type="button"
          aria-label="Dismiss"
          onClick={onDismiss}
          className="-mr-1 shrink-0 rounded-[var(--radius-xs)] p-0.5 opacity-70 outline-none transition-opacity hover:opacity-100 focus-visible:ring-2 focus-visible:ring-current"
        >
          <Icon name="x" size={14} />
        </button>
      )}
    </div>
  );
}
