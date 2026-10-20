import { Button, LinkButton } from '@/components/ui';
import { cn } from '@/lib/cn';

interface AdmissionDetailLayoutProps {
  returnTo?: string;
  onBack?: () => void;
  header: React.ReactNode;
  pipeline: React.ReactNode;
  sections?: React.ReactNode[];
  content?: React.ReactNode;
  sidebar?: React.ReactNode;
  actions?: React.ReactNode;
  /** Give the main column the full width, for content that brings its own side column. */
  asideHidden?: boolean;
}

/** Two-column grid shared by every admission detail screen and by forms that embed their own aside. */
export const admissionDetailGridClassName = 'grid min-w-0 gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(17rem,22rem)]';

/** Side column that stays in view below the top bar while the main column scrolls. */
export const admissionAsideClassName = 'min-w-0 space-y-5 lg:sticky lg:top-[var(--sticky-top)] lg:self-start';

export function AdmissionDetailLayout({
  returnTo,
  onBack,
  header,
  pipeline,
  sections,
  content,
  sidebar,
  actions,
  asideHidden = false,
}: AdmissionDetailLayoutProps) {
  const aside = asideHidden ? null : (sidebar ?? actions);

  return (
    <div className="space-y-4">
      {onBack ? (
        <Button variant="tertiary" size="sm" icon="arrow-left" onClick={onBack} className="-ml-2.5">
          Back to admissions
        </Button>
      ) : (
        <LinkButton variant="tertiary" size="sm" icon="arrow-left" href={returnTo ?? '/'} className="-ml-2.5">
          Back to admissions
        </LinkButton>
      )}

      {/* No overflow-hidden here: it would turn this card into the scroll container and stop the aside from sticking. */}
      <div className="min-w-0 rounded-[var(--radius-lg)] border border-[var(--color-border)] shadow-[var(--shadow-panel)]">
        <div className="overflow-hidden rounded-t-[var(--radius-lg)]">{header}</div>

        <div className="space-y-5 rounded-b-[var(--radius-lg)] bg-[var(--color-surface-subtle)] p-4 sm:p-6">
          {pipeline}

          <div className={aside ? admissionDetailGridClassName : 'grid min-w-0 grid-cols-1 gap-5'}>
            <main className="min-w-0 space-y-5">
              {content ?? (
                <div className="grid min-w-0 grid-cols-1 gap-4 xl:grid-cols-2">
                  {sections?.map((section, idx) => (
                    <div key={idx} className="min-w-0">{section}</div>
                  ))}
                </div>
              )}
            </main>
            {aside && <aside className={cn(admissionAsideClassName)}>{aside}</aside>}
          </div>
        </div>
      </div>
    </div>
  );
}
