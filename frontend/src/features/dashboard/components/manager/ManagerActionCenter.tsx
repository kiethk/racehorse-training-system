'use client';

import Link from 'next/link';
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
            All Clear — No pending managerial sign-offs or urgent incidents.
          </span>
        </div>
      </Panel>
    );
  }

  const isSplitLayout = hasPendingApprovals && hasIncidentAlerts;

  return (
    <div className={isSplitLayout ? 'grid grid-cols-1 lg:grid-cols-2 gap-4' : 'space-y-4'}>
      {/* 1. Pending Manager Sign-offs Panel */}
      {hasPendingApprovals && (
        <Panel padded className="p-3.5 space-y-2.5 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-1">
              <div className="flex items-center gap-2">
                <SectionTitle>Action Center</SectionTitle>
                <span className="flex h-4 min-w-4 items-center justify-center rounded-full bg-amber-500/20 px-1 text-[11px] font-bold text-amber-700 dark:text-amber-400">
                  {pendingAdmissions.length}
                </span>
              </div>
            </div>

            <div className="space-y-1.5 pt-1">
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
                    <div className="min-w-0 flex items-center gap-1.5">
                      <span className="font-semibold text-xs text-[var(--color-text-primary)] truncate">
                        {admission.candidateHorseName}
                      </span>
                      {admission.ownerName && (
                        <span className="text-[11px] text-[var(--color-text-muted)] truncate hidden sm:inline">
                          ({admission.ownerName})
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <Pill tone="warning" size="sm">
                      Review
                    </Pill>
                    <span className="text-[11px] text-[var(--color-text-muted)] hidden md:inline">
                      {formatDate(admission.submittedAt)}
                    </span>
                    <LinkButton
                      href={`/manager/admissions/${admission.admissionId}`}
                      variant="secondary"
                      size="sm"
                      className="h-7 text-xs px-2.5"
                    >
                      Review
                    </LinkButton>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </Panel>
      )}

      {/* 2. Urgent Equine Incidents Panel (Executive Attention Card, zero pale yellow bg) */}
      {hasIncidentAlerts && (
        <Panel padded className="p-3.5 space-y-2.5 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-1">
              <div className="flex items-center gap-2">
                <SectionTitle>Urgent Health Incidents</SectionTitle>
                <span className="flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500/20 px-1 text-[11px] font-bold text-rose-700 dark:text-rose-400">
                  {urgentIncidents.length}
                </span>
              </div>
              <Link
                href="/manager/horses"
                className="text-xs font-semibold text-[var(--color-primary)] hover:underline inline-flex items-center gap-1"
              >
                All horses <Icon name="chevron-right" size={12} />
              </Link>
            </div>

            <div className="space-y-1.5 pt-1">
              {urgentIncidents.map((incident) => {
                const isCritical = incident.incidentType === 'TRAUMA';
                return (
                  <div
                    key={`inc-${incident.incidentId}`}
                    className="flex items-center justify-between gap-3 px-3 py-2 rounded-md bg-[var(--color-surface)] border border-[var(--color-border)] hover:bg-[var(--color-surface-hover)]/40 transition-colors"
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span
                        className={
                          isCritical
                            ? 'h-2 w-2 rounded-full bg-rose-500 shrink-0'
                            : 'h-2 w-2 rounded-full bg-amber-500 shrink-0'
                        }
                      />
                      <div className="min-w-0 flex items-center gap-2">
                        <Link
                          href={`/manager/horses/${incident.horseId}`}
                          className="font-semibold text-xs text-[var(--color-text-primary)] hover:text-[var(--color-primary)] hover:underline truncate"
                        >
                          {incident.horseName}
                        </Link>
                        <span className="text-[11px] text-[var(--color-text-secondary)] truncate">
                          {incident.title}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <Pill tone={isCritical ? 'danger' : 'warning'} size="sm">
                        {isCritical ? 'Critical' : 'High'}
                      </Pill>
                      <span className="text-[11px] text-[var(--color-text-muted)] hidden md:inline">
                        {formatDate(incident.reportedAt)}
                      </span>
                      <LinkButton
                        href={`/manager/horses/${incident.horseId}`}
                        variant="secondary"
                        size="sm"
                        className="h-7 text-xs px-2.5"
                      >
                        Details
                      </LinkButton>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </Panel>
      )}
    </div>
  );
}
