import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';

/**
 * Placeholder shown when Access Control API is not yet implemented.
 * Layout follows the ui-reference AccessControlSection visual structure.
 */
export function AccessControlPlaceholder() {
  return (
    <div className="space-y-4">
      <Panel padded>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <SectionTitle>Role-based access control</SectionTitle>
            <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
              Manage role permissions and access levels for each user type.
            </p>
          </div>
        </div>
      </Panel>

      <Panel padded>
        <div className="flex flex-col items-center justify-center py-10 text-center">
          <span className="mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-[var(--color-surface-muted)] text-[var(--color-text-muted)]">
            <Icon name="shield" size={18} />
          </span>
          <p className="text-[13px] font-semibold text-[var(--color-text-primary)]">
            Access Control — Coming Soon
          </p>
          <p className="mt-1 max-w-xs text-[12px] text-[var(--color-text-secondary)]">
            Role-based access control will be implemented in a future release.
          </p>
        </div>
      </Panel>
    </div>
  );
}
