import type { HTMLAttributes, ReactNode } from 'react';

export type ScreenVariant = 'list' | 'dashboard' | 'detail' | 'form';

const variantClasses: Record<ScreenVariant, string> = {
  list: 'w-full min-w-0 space-y-5',
  dashboard: 'w-full min-w-0 space-y-6',
  detail: 'w-full min-w-0 space-y-5',
  form: 'mx-auto w-full min-w-0 max-w-4xl space-y-5',
};

export function ScreenLayout({
  variant = 'list',
  className = '',
  children,
  ...props
}: HTMLAttributes<HTMLDivElement> & { variant?: ScreenVariant; children: ReactNode }) {
  return (
    <div className={`${variantClasses[variant]} ${className}`} {...props}>
      {children}
    </div>
  );
}

export function PageSection({
  title,
  description,
  actions,
  className = '',
  children,
}: {
  title?: string;
  description?: ReactNode;
  actions?: ReactNode;
  className?: string;
  children: ReactNode;
}) {
  return (
    <section className={`space-y-3 ${className}`}>
      {(title || description || actions) && (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            {title && <h2 className="text-base font-semibold text-[var(--color-text-primary)]">{title}</h2>}
            {description && <div className="mt-1 text-sm text-[var(--color-text-secondary)]">{description}</div>}
          </div>
          {actions && <div className="flex items-center gap-2">{actions}</div>}
        </div>
      )}
      {children}
    </section>
  );
}
