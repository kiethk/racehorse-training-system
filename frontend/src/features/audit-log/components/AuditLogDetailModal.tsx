'use client';

import Link from 'next/link';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { parseAuditDetails, resolveLogModule } from '../lib/auditDiff';
import type { AuditLogItem } from '../types';

interface AuditLogDetailModalProps {
  item: AuditLogItem | null;
  open: boolean;
  onClose: () => void;
}

function formatRole(role: string | null): string {
  if (!role) return 'System';
  switch (role) {
    case 'CLUB_MANAGER': return 'Club Manager';
    case 'HEAD_TRAINER': return 'Head Trainer';
    case 'VETERINARIAN': return 'Veterinarian';
    case 'GROOM': return 'Groom';
    case 'HORSE_OWNER': return 'Horse Owner';
    default: return role;
  }
}

function formatAction(action: string | null, method?: string, path?: string): string {
  if (action) {
    if (action.includes('_') && action === action.toUpperCase()) {
      return action
        .toLowerCase()
        .split('_')
        .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
        .join(' ');
    }
    return action;
  }
  if (method && path) {
    return `${method} ${path}`;
  }
  return 'System Activity';
}

export function AuditLogDetailModal({ item, open, onClose }: AuditLogDetailModalProps) {
  if (!item) return null;

  const parsed = parseAuditDetails(item.details, item.entityName, item.entityId);
  const actionTitle = formatAction(item.action, item.httpMethod, item.requestPath);
  const moduleName = resolveLogModule(item.module || parsed.module, item.entityName, item.requestPath, item.action);

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-lg font-bold text-[var(--color-text-primary)]">
            Audit Log #{item.id}
          </span>
          <span className="rounded-full bg-[var(--color-primary-soft)] px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wider text-[var(--color-primary)]">
            {moduleName}
          </span>
          {item.success ? (
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400">
              <span className="size-1.5 rounded-full bg-emerald-500" />
              Success
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-semibold text-rose-700 dark:bg-rose-950/40 dark:text-rose-400">
              <span className="size-1.5 rounded-full bg-rose-500" />
              Failed ({item.statusCode})
            </span>
          )}
        </div>
      }
      description={
        <span className="text-xs text-[var(--color-text-muted)] font-mono">
          Recorded on {new Date(item.createdAt).toLocaleString('en-GB', {
            dateStyle: 'full',
            timeStyle: 'medium',
          })}
        </span>
      }
      footer={
        <div className="flex items-center justify-end w-full">
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-6 pt-2">
        {/* Action Title & High-level Summary */}
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4">
          <div className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
            Action Performed
          </div>
          <div className="mt-1 text-base font-semibold text-[var(--color-text-primary)]">
            {actionTitle}
          </div>
          {parsed.summary && (
            <p className="mt-2 text-sm text-[var(--color-text-secondary)] leading-relaxed">
              {parsed.summary}
            </p>
          )}
        </div>

        {/* Actor & Target Entity Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Actor Info */}
          <div className="rounded-lg border border-[var(--color-border)] p-4 flex flex-col justify-between">
            <div>
              <div className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)] mb-2">
                Initiated By (Actor)
              </div>
              {item.actorUserId ? (
                <div className="space-y-1">
                  <div className="font-semibold text-[var(--color-text-primary)] text-sm">
                    {item.actorName || `User #${item.actorUserId}`}
                  </div>
                  {item.actorEmail && (
                    <div className="text-xs text-[var(--color-text-secondary)] font-mono">
                      {item.actorEmail}
                    </div>
                  )}
                  <div className="pt-1 flex items-center gap-2">
                    <span className="text-xs px-2 py-0.5 rounded bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] font-medium">
                      {formatRole(item.actorRole)}
                    </span>
                    <span className="text-xs text-[var(--color-text-muted)] font-mono">
                      ID: {item.actorUserId}
                    </span>
                  </div>
                </div>
              ) : (
                <div className="text-sm italic text-[var(--color-text-secondary)]">
                  System Process / Unauthenticated
                </div>
              )}
            </div>
          </div>

          {/* Target Entity Info */}
          <div className="rounded-lg border border-[var(--color-border)] p-4 flex flex-col justify-between">
            <div>
              <div className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)] mb-2">
                Target Entity
              </div>
              {item.entityName ? (
                <div className="space-y-1">
                  <div className="font-semibold text-[var(--color-text-primary)] text-sm">
                    {item.entityName}
                  </div>
                  {item.entityId != null && (
                    <div className="text-xs text-[var(--color-text-muted)] font-mono">
                      Entity ID: #{item.entityId}
                    </div>
                  )}
                </div>
              ) : (
                <div className="text-sm italic text-[var(--color-text-secondary)]">
                  General System / Endpoint
                </div>
              )}
            </div>

            {/* Universal Entity Redirection Link */}
            {parsed.targetUrl && (
              <div className="mt-3 pt-3 border-t border-[var(--color-border)]">
                <Link
                  href={parsed.targetUrl}
                  className="inline-flex items-center gap-1.5 text-xs font-medium text-[var(--color-primary)] hover:underline"
                >
                  <Icon name="external-link" className="size-3.5" />
                  View Target Entity Details
                </Link>
              </div>
            )}
          </div>
        </div>

        {/* Reason / Justification Callout */}
        {parsed.reason && (
          <div className="rounded-lg border border-amber-200 bg-amber-50/70 p-4 dark:border-amber-900/50 dark:bg-amber-950/20">
            <div className="text-xs font-semibold uppercase tracking-wider text-amber-800 dark:text-amber-400">
              Operational Reason / Justification
            </div>
            <p className="mt-1 text-sm text-amber-900 dark:text-amber-200 leading-relaxed">
              {parsed.reason}
            </p>
          </div>
        )}

        {/* Change Diff Table */}
        <div>
          <div className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)] mb-2">
            Detailed Changes & State Transition
          </div>

          {parsed.diffs.length > 0 ? (
            <div className="overflow-hidden rounded-lg border border-[var(--color-border)]">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] font-medium">
                  <tr>
                    <th className="py-2.5 px-4 border-b border-[var(--color-border)]">Field</th>
                    <th className="py-2.5 px-4 border-b border-[var(--color-border)]">Before State</th>
                    <th className="py-2.5 px-4 border-b border-[var(--color-border)]">After State</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-border)]">
                  {parsed.diffs.map((diff) => (
                    <tr
                      key={diff.field}
                      className={
                        diff.isChanged
                          ? 'bg-amber-50/40 dark:bg-amber-950/10'
                          : undefined
                      }
                    >
                      <td className="py-2.5 px-4 font-mono font-medium text-[var(--color-text-primary)]">
                        {diff.field}
                      </td>
                      <td className="py-2.5 px-4 font-mono text-[var(--color-text-secondary)]">
                        {diff.beforeValue}
                      </td>
                      <td className="py-2.5 px-4 font-mono">
                        <span
                          className={
                            diff.isChanged
                              ? 'font-semibold text-emerald-700 dark:text-emerald-400'
                              : 'text-[var(--color-text-primary)]'
                          }
                        >
                          {diff.afterValue}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3">
              <pre className="text-xs font-mono whitespace-pre-wrap break-all text-[var(--color-text-secondary)]">
                {parsed.raw || 'No detailed payload recorded for this operation.'}
              </pre>
            </div>
          )}
        </div>

        {/* Technical HTTP Metadata */}
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-muted)]/50 p-3">
          <div className="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)] mb-1">
            HTTP Trace Metadata
          </div>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs font-mono text-[var(--color-text-secondary)]">
            <div>
              <span className="text-[var(--color-text-muted)]">Method: </span>
              <span className="font-semibold text-[var(--color-text-primary)]">{item.httpMethod}</span>
            </div>
            <div>
              <span className="text-[var(--color-text-muted)]">Status: </span>
              <span>{item.statusCode}</span>
            </div>
            <div>
              <span className="text-[var(--color-text-muted)]">Path: </span>
              <span>{item.requestPath}</span>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
}
