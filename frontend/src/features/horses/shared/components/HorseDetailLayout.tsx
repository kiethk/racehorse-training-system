import type { ReactNode } from 'react';

import { Button, LinkButton } from '@/components/ui';
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
 * Back link sits outside the card; header is inside the rounded card top.
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
        <Button
          variant="tertiary"
          size="sm"
          icon="arrow-left"
          onClick={onBack}
          className="-ml-2.5"
        >
          {backLabel}
        </Button>
      ) : (
        <LinkButton
          variant="tertiary"
          size="sm"
          icon="arrow-left"
          href={returnTo}
          className="-ml-2.5"
        >
          {backLabel}
        </LinkButton>
      )}

      {/* No overflow-hidden: would make this the scroll container and break sticky aside. */}
      <div className="min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] shadow-[var(--shadow-panel)]">
        <div className="overflow-hidden rounded-t-[var(--radius-lg)]">{header}</div>

        <div className="space-y-5 rounded-b-[var(--radius-lg)] bg-[var(--color-surface-subtle)] p-4 sm:p-6">
          <div className={aside ? horseDetailGridClassName : 'grid min-w-0 grid-cols-1 gap-5'}>
            <main className="min-w-0 space-y-5">
              {content ?? (
                <div className="grid min-w-0 grid-cols-1 gap-4 xl:grid-cols-2">
                  {sections?.map((section, idx) => (
                    <div key={idx} className="min-w-0">
                      {section}
                    </div>
                  ))}
                </div>
              )}
            </main>
            {aside && <aside className={cn(horseAsideClassName)}>{aside}</aside>}
          </div>
        </div>
      </div>
    </div>
  );
}
