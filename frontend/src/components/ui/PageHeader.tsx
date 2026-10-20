import Link from 'next/link';
import type { ReactNode } from 'react';

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

export function PageHeader({
  title,
  description,
  eyebrow,
  breadcrumbs,
  actions,
  className = '',
}: {
  title: string;
  description?: ReactNode;
  eyebrow?: string;
  breadcrumbs?: BreadcrumbItem[];
  actions?: ReactNode;
  className?: string;
}) {
  return (
    <header className={`flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between ${className}`}>
      <div className="min-w-0">
        {breadcrumbs && breadcrumbs.length > 0 && (
          <nav aria-label="Breadcrumb" className="mb-2 text-xs text-[var(--color-text-muted)]">
            <ol className="flex flex-wrap items-center gap-2">
              {breadcrumbs.map((item, index) => (
                <li key={`${item.label}-${index}`} className="flex items-center gap-2">
                  {index > 0 && <span aria-hidden="true">/</span>}
                  {item.href ? (
                    <Link href={item.href} className="hover:text-[var(--color-text-primary)] focus-visible:rounded-sm focus-visible:outline-2">
                      {item.label}
                    </Link>
                  ) : (
                    <span aria-current={index === breadcrumbs.length - 1 ? 'page' : undefined}>
                      {item.label}
                    </span>
                  )}
                </li>
              ))}
            </ol>
          </nav>
        )}
        {eyebrow && (
          <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-[var(--color-primary)]">
            {eyebrow}
          </p>
        )}
        <h1 className="text-xl font-semibold leading-[var(--leading-heading)] tracking-tight text-[var(--color-text-primary)] sm:text-2xl">
          {title}
        </h1>
        {description && (
          <div className="mt-1.5 max-w-3xl text-sm leading-relaxed text-[var(--color-text-secondary)]">
            {description}
          </div>
        )}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
    </header>
  );
}
