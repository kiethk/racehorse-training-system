import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';

/**
 * Placeholder shown when Audit Log API is not yet implemented.
 * Layout follows the ui-reference AuditLogSection visual structure.
 */
export function AuditLogPlaceholder() {
  return (
    <Panel padded>
      <div className="flex items-center justify-between gap-2">
        <SectionTitle>Audit log</SectionTitle>
      </div>
      <div className="mt-6 flex flex-col items-center justify-center py-8 text-center">
        <span className="mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-[var(--color-surface-muted)] text-[var(--color-text-muted)]">
          <Icon name="clipboard" size={18} />
        </span>
        <p className="text-[13px] font-semibold text-[var(--color-text-primary)]">
          Audit Log — Coming Soon
        </p>
        <p className="mt-1 max-w-xs text-[12px] text-[var(--color-text-secondary)]">
          System-wide audit history will be available once the audit log API is implemented.
        </p>
      </div>
    </Panel>
  );
}
