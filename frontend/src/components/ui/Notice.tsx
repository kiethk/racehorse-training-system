import type { ReactNode } from 'react';
import { Icon, type IconName } from './Icon';

const variants = {
  success: { icon: 'check', className: 'bg-[var(--color-success-soft)] text-[var(--color-success)]' },
  error: { icon: 'alert-triangle', className: 'bg-[var(--color-danger-soft)] text-[var(--color-danger)]' },
  info: { icon: 'info', className: 'bg-[var(--color-info-soft)] text-[var(--color-info)]' },
  warning: { icon: 'alert-triangle', className: 'bg-[var(--color-warning-soft)] text-[var(--color-warning)]' },
} satisfies Record<string, { icon: IconName; className: string }>;

export function Notice({
  tone = 'info',
  children,
  className = '',
}: {
  tone?: keyof typeof variants;
  children: ReactNode;
  className?: string;
}) {
  const variant = variants[tone];
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={`flex items-start gap-2 rounded-[var(--radius-md)] px-3 py-2.5 text-sm ${variant.className} ${className}`}>
      <Icon name={variant.icon} size={16} className="mt-0.5 shrink-0" aria-hidden="true" />
      <div className="min-w-0">{children}</div>
    </div>
  );
}
