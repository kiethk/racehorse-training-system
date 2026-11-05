'use client';

import { Panel, SectionTitle, Icon, Pill, LinkButton } from '@/components/ui';
import { formatDate } from '@/lib/display';
import type { PendingAdmissionActionDto, UrgentIncidentActionDto } from '../../types';

export interface ManagerActionCenterProps {
  pendingAdmissions: PendingAdmissionActionDto[];
  urgentIncidents: UrgentIncidentActionDto[];
}

export function ManagerActionCenter({
  pendingAdmissions,
  urgentIncidents,
}: ManagerActionCenterProps) {
  const hasPendingApprovals = pendingAdmissions.length > 0;
  const hasIncidentAlerts = urgentIncidents.length > 0;

  if (!hasPendingApprovals && !hasIncidentAlerts) {
    return (
      <Panel padded className="py-3 px-4 border-emerald-500/20 bg-emerald-50/40 dark:bg-emerald-950/10">
        <div className="flex items-center gap-2.5 text-xs text-emerald-800 dark:text-emerald-300">
          <Icon name="check" size={16} className="text-emerald-600 dark:text-emerald-400 shrink-0" />
          <span className="font-medium">
            All Clear — No pending managerial sign-offs or critical incidents.
          </span>
        </div>
      </Panel>
    );
  }

  return (
    <div className="space-y-3">
      {/* 1. Situational Incident Notice Strip (Informational only, no action confusion) */}
      {hasIncidentAlerts && (
        <div className="flex items-center justify-between gap-3 px-3.5 py-2.5 rounded-lg border border-amber-500/25 bg-amber-500/10 text-xs">
          <div className="flex items-center gap-2.5 min-w-0">
            <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-amber-500/20 text-amber-600 dark:text-amber-400">
              <Icon name="alert-triangle" size={13} />
            </span>
            <div className="min-w-0 text-[var(--color-text-primary)] truncate">
              <strong className="font-semibold text-amber-700 dark:text-amber-400 uppercase tracking-wide mr-1.5">
                Incident Notice:
              </strong>
              {urgentIncidents.map((inc) => `${inc.horseName} (${inc.title})`).join(' · ')}
            </div>
          </div>
          <span className="text-[11px] text-[var(--color-text-muted)] shrink-0 hidden sm:inline">
            Under veterinary care
          </span>
        </div>
      )}

      {/* 2. Actionable Items (Pending Manager Sign-offs) - Ultra Compact Single Row */}
      {hasPendingApprovals && (
        <Panel padded className="p-3.5 space-y-2.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <SectionTitle>Action Center</SectionTitle>
              <span className="flex h-4 min-w-4 items-center justify-center rounded-full bg-amber-500/20 px-1 text-[11px] font-bold text-amber-700 dark:text-amber-400">
                {pendingAdmissions.length}
              </span>
            </div>
            <span className="text-[11px] text-[var(--color-text-muted)]">
              Pending managerial sign-off
            </span>
          </div>

          <div className="space-y-1.5">
            {pendingAdmissions.map((admission) => (
              <div
                key={`adm-${admission.admissionId}`}
                className="flex items-center justify-between gap-3 px-3 py-2 rounded-md bg-[var(--color-surface)] border border-[var(--color-border)] hover:bg-[var(--color-surface-hover)]/40 transition-colors"
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <span
                    className={
                      admission.isArrivalOverdueOrExpiring
                        ? 'h-2 w-2 rounded-full bg-rose-500 shrink-0'
                        : 'h-2 w-2 rounded-full bg-amber-500 shrink-0'
                    }
                  />
                  <span className="font-semibold text-xs text-[var(--color-text-primary)] truncate">
                    {admission.candidateHorseName}
                  </span>
                  <span className="text-xs text-[var(--color-text-muted)] truncate hidden sm:inline">
                    · {admission.ownerName}
                  </span>
                  <Pill
                    tone={admission.isArrivalOverdueOrExpiring ? 'danger' : 'warning'}
                    size="sm"
                  >
                    {admission.isArrivalOverdueOrExpiring ? 'Arrival Expiring' : 'Manager Review'}
                  </Pill>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  <span className="text-[11px] text-[var(--color-text-muted)] hidden md:inline">
                    {formatDate(admission.submittedAt)}
                  </span>
                  <LinkButton
                    href={`/manager/admissions/${admission.admissionId}`}
                    size="sm"
                    variant="secondary"
                    className="h-7 text-xs px-2.5"
                  >
                    Review
                  </LinkButton>
                </div>
              </div>
            ))}
          </div>
        </Panel>
      )}
    </div>
  );
}
