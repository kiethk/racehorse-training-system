/* eslint-disable no-restricted-syntax */
'use client';

import Link from 'next/link';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { formatDate } from '@/lib/display';
import { ModuleBadge } from './ModuleBadge';
import { parseAuditDetails, resolveLogModule, resolveValueLink } from '../lib/auditDiff';
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
          <span className="text-base font-bold text-[var(--color-text-primary)]">
            Audit Log #{item.id}
          </span>
          <ModuleBadge moduleName={moduleName} />
          {item.success ? (
            <span className="text-xs text-[var(--color-text-muted)]">
              (Success)
            </span>
          ) : (
            <span className="text-xs text-[var(--color-text-muted)]">
              (Failed - {item.statusCode})
            </span>
          )}
        </div>
      }
      description={
        <span className="text-xs text-[var(--color-text-muted)] font-mono">
          {formatDate(item.createdAt, {
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
      <div className="space-y-4 pt-1">
        {/* Unified Overview Card */}
        <div className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] p-3.5 space-y-3">
          <div className="border-b border-[var(--color-border)] pb-2.5">
            <div className="text-[11px] font-medium text-[var(--color-text-muted)]">Action</div>
            <div className="text-sm font-semibold text-[var(--color-text-primary)]">
              {actionTitle}
            </div>
            {parsed.summary && (
              <div className="mt-0.5 text-xs text-[var(--color-text-secondary)]">
                {parsed.summary}
              </div>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
            <div>
              <span className="text-[var(--color-text-muted)]">Actor: </span>
              {item.actorUserId ? (
                <Link
                  href={`/manager/accounts?userId=${item.actorUserId}`}
                  className="font-medium text-[var(--color-text-primary)] hover:underline inline-flex items-center gap-1"
                  title="View user account"
                >
                  <span>{item.actorName || `User #${item.actorUserId}`}</span>
                  {item.actorRole && (
                    <span className="text-[var(--color-text-muted)]">
                      ({formatRole(item.actorRole)})
                    </span>
                  )}
                  <Icon name="external-link" className="size-3 opacity-60 ml-0.5" />
                </Link>
              ) : (
                <span className="text-[var(--color-text-secondary)]">System</span>
              )}
              {item.actorEmail && (
                <div className="text-[var(--color-text-muted)] font-mono text-[11px]">
                  {item.actorEmail}
                </div>
              )}
            </div>

            <div>
              <span className="text-[var(--color-text-muted)]">Target: </span>
              {item.entityName ? (
                parsed.targetUrl ? (
                  <Link
                    href={parsed.targetUrl}
                    className="font-medium text-[var(--color-text-primary)] hover:underline inline-flex items-center gap-1"
                    title={`View ${item.entityName} ${item.entityId != null ? '#' + item.entityId : ''}`}
                  >
                    <span>{item.entityName}</span>
                    {item.entityId != null && (
                      <span className="font-mono text-[var(--color-text-muted)]">#{item.entityId}</span>
                    )}
                    <Icon name="external-link" className="size-3 opacity-60 ml-0.5" />
                  </Link>
                ) : (
                  <span className="font-medium text-[var(--color-text-primary)]">
                    {item.entityName}
                    {item.entityId != null && (
                      <span className="ml-1 font-mono text-[var(--color-text-muted)]">#{item.entityId}</span>
                    )}
                  </span>
                )
              ) : (
                <span className="text-[var(--color-text-secondary)]">System</span>
              )}
            </div>
          </div>
        </div>

        {/* Reason Callout (Clean, normal text) */}
        {parsed.reason && (
          <div className="rounded border border-[var(--color-border)] px-3.5 py-2 text-xs text-[var(--color-text-primary)]">
            <span className="font-semibold">Reason: </span>
            {parsed.reason}
          </div>
        )}

        {/* Changes Table (Standard, clean enterprise table with normal black text and entity deep links) */}
        <div>
          <div className="text-[11px] font-semibold text-[var(--color-text-muted)] uppercase tracking-wider mb-1.5">
            Changes
          </div>

          {parsed.diffs.length > 0 ? (
            <div className="overflow-hidden rounded border border-[var(--color-border)]">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[var(--color-surface-muted)] text-[var(--color-text-primary)] font-semibold border-b border-[var(--color-border)]">
                  <tr>
                    <th className="py-2 px-3 w-1/3">Field</th>
                    <th className="py-2 px-3 w-1/3">Previous</th>
                    <th className="py-2 px-3 w-1/3">New</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-border)] font-mono text-xs">
                  {parsed.diffs.map((diff) => {
                    const beforeLink = resolveValueLink(diff.field, diff.beforeValue);
                    const afterLink = resolveValueLink(diff.field, diff.afterValue);

                    return (
                      <tr key={diff.field}>
                        <td className="py-2 px-3 font-medium text-[var(--color-text-primary)]">
                          {diff.field}
                        </td>
                        <td className="py-2 px-3 text-[var(--color-text-primary)]">
                          {beforeLink ? (
                            <Link
                              href={beforeLink}
                              className="inline-flex items-center gap-1 hover:underline font-mono"
                              title={`View ${diff.field}`}
                            >
                              <span>{diff.beforeValue}</span>
                              <Icon name="external-link" className="size-3 opacity-60" />
                            </Link>
                          ) : (
                            diff.beforeValue
                          )}
                        </td>
                        <td className="py-2 px-3">
                          {afterLink ? (
                            <Link
                              href={afterLink}
                              className={`inline-flex items-center gap-1 hover:underline text-[var(--color-text-primary)] font-mono ${
                                diff.isChanged ? 'font-bold' : ''
                              }`}
                              title={`View ${diff.field}`}
                            >
                              <span>{diff.afterValue}</span>
                              <Icon name="external-link" className="size-3 opacity-60" />
                            </Link>
                          ) : (
                            <span className={`text-[var(--color-text-primary)] ${diff.isChanged ? 'font-bold' : ''}`}>
                              {diff.afterValue}
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="rounded border border-[var(--color-border)] bg-[var(--color-surface-muted)] px-3 py-2 text-xs font-mono text-[var(--color-text-primary)] whitespace-pre-wrap break-all">
              {parsed.raw || 'No change payload recorded for this operation.'}
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
}
