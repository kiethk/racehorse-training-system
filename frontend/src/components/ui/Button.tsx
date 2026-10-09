import type { ButtonHTMLAttributes, ComponentProps, ReactNode } from 'react';
import Link from 'next/link';
import { cn } from '@/lib/cn';
import { Icon, type IconName } from './Icon';

type Variant = 'primary' | 'secondary' | 'tertiary' | 'destructive' | 'warning' | 'link';
type Size = 'sm' | 'md';

interface ButtonStyleProps {
  variant?: Variant;
  size?: Size;
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>, ButtonStyleProps {
  icon?: IconName;
  iconRight?: IconName;
  loading?: boolean;
  children?: ReactNode;
}

const base =
  'inline-flex items-center justify-center gap-1.5 rounded-[var(--radius-sm)] font-medium ' +
  'transition-[color,background-color,border-color,opacity,transform] duration-[var(--duration-fast)] ' +
  'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] ' +
  'focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--color-surface)] ' +
  'active:scale-[0.98] disabled:pointer-events-none disabled:opacity-50 ' +
  'aria-disabled:pointer-events-none aria-disabled:opacity-50 select-none whitespace-nowrap';

const sizes: Record<Size, string> = {
  sm: 'h-8 px-2.5 text-xs',
  md: 'h-9 px-3 text-sm',
};

const variants: Record<Variant, string> = {
  primary:
    'bg-[var(--color-primary)] text-[var(--color-text-inverse)] hover:bg-[var(--color-primary-hover)]',
  secondary:
    'bg-[var(--color-surface)] text-[var(--color-text-primary)] border border-[var(--color-border-strong)] hover:bg-[var(--color-surface-muted)]',
  tertiary:
    'bg-transparent text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]',
  destructive: 'bg-[var(--color-danger)] text-[var(--color-text-inverse)] hover:opacity-90',
  warning: 'bg-[var(--color-warning)] text-[var(--color-text-inverse)] hover:opacity-90',
  link: 'h-auto px-0 text-[var(--color-primary)] hover:underline active:scale-100',
};

/** Class string for anything that must look like a Button (e.g. a link). */
export function buttonClassName({ variant = 'secondary', size = 'md' }: ButtonStyleProps = {}, className?: string) {
  return cn(base, sizes[size], variants[variant], className);
}

export function Button({
  variant = 'secondary',
  size = 'md',
  icon,
  iconRight,
  loading = false,
  children,
  className,
  disabled,
  ...props
}: ButtonProps) {
  const iconSize = size === 'sm' ? 14 : 16;
  return (
    <button
      className={buttonClassName({ variant, size }, className)}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...props}
    >
      {loading ? (
        <span
          className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent"
          aria-hidden="true"
        />
      ) : (
        icon && <Icon name={icon} size={iconSize} />
      )}
      {children}
      {iconRight && !loading && <Icon name={iconRight} size={iconSize} />}
    </button>
  );
}

/** A navigation link styled as a Button. Use this instead of re-typing button classes on <Link>. */
export function LinkButton({
  variant = 'secondary',
  size = 'md',
  icon,
  iconRight,
  children,
  className,
  ...props
}: ComponentProps<typeof Link> & ButtonStyleProps & { icon?: IconName; iconRight?: IconName }) {
  const iconSize = size === 'sm' ? 14 : 16;
  return (
    <Link className={buttonClassName({ variant, size }, className)} {...props}>
      {icon && <Icon name={icon} size={iconSize} />}
      {children}
      {iconRight && <Icon name={iconRight} size={iconSize} />}
    </Link>
  );
}

/** Square icon-only button. `label` is required: it is the accessible name and the tooltip. */
export function IconButton({
  icon,
  label,
  size = 'md',
  variant = 'tertiary',
  className,
  type = 'button',
  ...props
}: Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'aria-label'> &
  ButtonStyleProps & { icon: IconName; label: string }) {
  return (
    <button
      type={type}
      aria-label={label}
      title={label}
      className={cn(base, variants[variant], size === 'sm' ? 'h-7 w-7' : 'h-9 w-9', 'px-0', className)}
      {...props}
    >
      <Icon name={icon} size={size === 'sm' ? 14 : 18} />
    </button>
  );
}
