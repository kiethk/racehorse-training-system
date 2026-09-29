import { Panel, SectionTitle } from '@/components/ui/Panel';

export function InfoRow({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <span className="shrink-0 text-[var(--color-text-muted)]">{label}</span>
      <span className="text-right font-medium text-[var(--color-text-primary)]">
        {value || <span className="font-normal italic text-[var(--color-text-muted)]">N/A</span>}
      </span>
    </div>
  );
}

export function AdmissionInfoSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Panel padded className="bg-[var(--color-surface)]">
      <SectionTitle>{title}</SectionTitle>
      <div className="mt-4 space-y-3 text-[12px]">
        {children}
      </div>
    </Panel>
  );
}
