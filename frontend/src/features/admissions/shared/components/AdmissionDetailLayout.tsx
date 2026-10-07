import Link from 'next/link';
import { Icon } from '@/components/ui/Icon';

interface AdmissionDetailLayoutProps {
  returnTo?: string;
  onBack?: () => void;
  header: React.ReactNode;
  pipeline: React.ReactNode;
  sections?: React.ReactNode[];
  content?: React.ReactNode;
  sidebar?: React.ReactNode;
  actions?: React.ReactNode;
}

export function AdmissionDetailLayout({
  returnTo,
  onBack,
  header,
  pipeline,
  sections,
  content,
  sidebar,
  actions,
}: AdmissionDetailLayoutProps) {
  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        {onBack ? (
          <button type="button" onClick={onBack} className="inline-flex min-h-11 items-center gap-2 text-sm font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
            <Icon name="arrow-left" size={15} /> Back to admissions
          </button>
        ) : (
          <Link href={returnTo ?? '/'} className="inline-flex min-h-11 items-center gap-2 text-sm font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
            <Icon name="arrow-left" size={15} /> Back to admissions
          </Link>
        )}
      </div>

      <div className="min-w-0 overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] shadow-sm">
        {header}

        <div className="space-y-6 bg-[var(--color-surface-subtle)] p-4 sm:p-6">
          {pipeline}

          <div className={`grid min-w-0 gap-5 ${sidebar || actions ? 'lg:grid-cols-[minmax(0,1fr)_minmax(17rem,22rem)]' : 'grid-cols-1'}`}>
            <main className="min-w-0 space-y-5">
              {content ?? (
                <div className="grid min-w-0 grid-cols-1 gap-4 xl:grid-cols-2">
                  {sections?.map((section, idx) => (
                    <div key={idx} className="min-w-0">{section}</div>
                  ))}
                </div>
              )}
            </main>
            {(sidebar || actions) && (
              <aside className="min-w-0 space-y-5 lg:sticky lg:top-6">
                {sidebar ?? actions}
              </aside>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
