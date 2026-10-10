import type { ReactNode } from 'react';
import { BrandLogo } from '@/components/ui/BrandLogo';

/** Centered card used by the public sign-in and registration pages. */
export function AuthCard({
  title,
  description,
  footer,
  note,
  children,
}: {
  title: string;
  description: string;
  /** Line under the form, e.g. the link to the other auth page. */
  footer?: ReactNode;
  /** Small text below the card. */
  note?: ReactNode;
  children: ReactNode;
}) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[var(--color-background)] px-4 py-10">
      <div className="w-full max-w-md animate-in fade-in slide-in-from-bottom-2 duration-300">
        <div className="mb-6 flex items-center justify-center gap-2">
          <BrandLogo className="h-9 w-9" />
          <span className="text-lg font-semibold tracking-tight text-[var(--color-text-primary)]">RTMS</span>
        </div>

        <section className="rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 shadow-[var(--shadow-popover)]">
          <div className="mb-6">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--color-primary)]">
              Riverside Training Club
            </p>
            <h1 className="mt-2 text-xl font-semibold tracking-tight text-[var(--color-text-primary)]">{title}</h1>
            <p className="mt-1.5 text-sm leading-relaxed text-[var(--color-text-secondary)]">{description}</p>
          </div>

          {children}

          {footer && <p className="mt-5 text-center text-xs text-[var(--color-text-muted)]">{footer}</p>}
        </section>

        {note && <p className="mt-4 text-center text-xs text-[var(--color-text-muted)]">{note}</p>}
      </div>
    </main>
  );
}
