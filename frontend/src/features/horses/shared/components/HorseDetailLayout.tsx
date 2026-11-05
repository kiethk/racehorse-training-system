import type { ReactNode } from 'react';

import { BackLink } from '@/components/ui';
import { cn } from '@/lib/cn';

export interface HorseDetailLayoutProps {
  /** Href for the back link. Defaults to '/'. */
  returnTo?: string;
  /** Optional onClick handler for back button instead of link. */
  onBack?: () => void;
  /** Label for the back button/link. Defaults to 'Back to horses'. */
  backLabel?: string;
  /** Rendered inside the top banner of the card (replaces Panel header). */
  header: ReactNode;
  /** Main content column. */
  content?: ReactNode;
  /** Optional list of section components rendered in a responsive grid if content is not provided. */
  sections?: ReactNode[];
  /** Optional sticky side column. */
  sidebar?: ReactNode;
  /** Alternative alias for sidebar content (e.g. action cards). */
  actions?: ReactNode;
  /** Give the main column the full width, hiding the aside. */
  asideHidden?: boolean;
}

/** Two-column grid used by horse detail screens. */
export const horseDetailGridClassName =
  'grid min-w-0 gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(17rem,22rem)]';

/** Side column that stays in view while the main column scrolls. */
export const horseAsideClassName =
  'min-w-0 space-y-5 lg:sticky lg:top-[var(--sticky-top)] lg:self-start';

/**
 * Shell layout shared by every horse detail screen.
 * Identity banner and tab content have independent surfaces, without nested card frames.
 * Sidebar is optional — when omitted the main column takes full width.
 */
export function HorseDetailLayout({
  returnTo = '/',
  onBack,
  backLabel = 'Back to horses',
  header,
  content,
  sections,
  sidebar,
  actions,
  asideHidden = false,
}: HorseDetailLayoutProps) {
  const aside = asideHidden ? null : (sidebar ?? actions);

  return (
    <div className="space-y-4">
      {onBack ? (
        <BackLink onClick={onBack}>{backLabel}</BackLink>
      ) : (
        <BackLink href={returnTo}>{backLabel}</BackLink>
      )}

      {header && (
        <div className="min-w-0 overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-panel)]">
          {header}
        </div>
      )}
      <div className={aside ? horseDetailGridClassName : 'grid min-w-0 grid-cols-1 gap-5'}>
        <div className="min-w-0 space-y-5">
          {content ?? (
            <div className="grid min-w-0 grid-cols-1 gap-4 xl:grid-cols-2">
              {sections?.map((section, idx) => (
                <div key={idx} className="min-w-0">{section}</div>
              ))}
            </div>
          )}
        </div>
        {aside && <aside className={cn(horseAsideClassName)}>{aside}</aside>}
      </div>
    </div>
  );
}
