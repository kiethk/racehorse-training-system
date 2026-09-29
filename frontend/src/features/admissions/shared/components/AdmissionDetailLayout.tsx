import Link from 'next/link';
import { Icon } from '@/components/ui/Icon';

interface AdmissionDetailLayoutProps {
  returnTo: string;
  header: React.ReactNode;
  pipeline: React.ReactNode;
  sections: React.ReactNode[];
  actions?: React.ReactNode;
}

export function AdmissionDetailLayout({
  returnTo,
  header,
  pipeline,
  sections,
  actions,
}: AdmissionDetailLayoutProps) {
  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link href={returnTo} className="inline-flex items-center gap-2 text-sm font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]">
          <Icon name="arrow-left" size={15} /> Back to applications
        </Link>
      </div>

      <div className="rounded-[var(--radius-lg)] overflow-hidden border border-[var(--color-border)] shadow-sm">
        {header}

        <div className="space-y-6 bg-[var(--color-surface-subtle)] p-6">
          {pipeline}

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {sections[0]}
            {sections[1]}
          </div>

          {sections.length > 2 && (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {sections[2]}
              {sections[3]}
            </div>
          )}
          
          {sections.length > 4 && (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {sections.slice(4).map((section, idx) => (
                <div key={idx}>{section}</div>
              ))}
            </div>
          )}

          {actions}
        </div>
      </div>
    </div>
  );
}
