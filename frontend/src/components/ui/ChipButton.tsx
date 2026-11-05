'use client';

import type { ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';
import { Icon, type IconName } from './Icon';

interface ChipButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  icon?: IconName;
}

/**
 * A small outlined label that opens something when clicked, such as a horse name that opens the horse.
 * It reads as a tag you can follow (outline, underline on hover) rather than as an action button.
 */
export function ChipButton({ icon, className, children, type = 'button', ...props }: ChipButtonProps) {
  return (
    <button
      type={type}
      className={cn(
        'inline-flex h-7 max-w-full items-center gap-1.5 rounded-full border border-[var(--color-text-primary)] bg-[var(--color-surface)] px-2.5',
        'text-xs font-medium text-[var(--color-text-primary)] outline-none underline-offset-2',
        'transition-colors duration-[var(--duration-fast)] hover:bg-[var(--color-surface-muted)] hover:underline',
        'focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] disabled:cursor-not-allowed disabled:opacity-60',
        className,
      )}
      {...props}
    >
      {icon && <Icon name={icon} size={12} />}
      <span className="truncate">{children}</span>
    </button>
  );
}
