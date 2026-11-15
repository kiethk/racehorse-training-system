'use client';

import { type ReactNode, useEffect, useState } from 'react';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { Button, EmptyState, HorseAvatar, Icon, IconButton, Panel, Pill } from '@/components/ui';
import { HorseSpecialtyBadges } from '@/features/horses/shared/components/HorseSpecialtyBadges';
import type { HorseSpecialty } from '@/features/horses/types';
import { formatDate, formatEnumLabel } from '@/lib/display';
import { isPlanActiveToday } from '@/features/care/lib/isPlanActiveToday';
import { treatmentPlanService } from '@/services/treatmentPlanService';
import type { TreatmentPlan } from '@/features/care/types';
import { MedicationInstructionModal } from '@/features/care/components/MedicationInstructionModal';
import type { Area, Horse, StableStall } from '../../types';
import { stallState } from './StallCell';

interface StallDetailPanelProps {
  stall: StableStall | null;
  area: Area | undefined;
  horse: Horse | undefined;
  /** Null while loading or when the viewer may not read it; the badges then render nothing. */
  specialty?: HorseSpecialty | null;
  /** Assigned groom's display name */
  groomName?: string | null;
  /** Assigned groom's user ID for direct profile redirection */
  groomId?: number | null;
  /** Assigned trainer's display name */
  trainerName?: string | null;
  /** Assigned trainer's user ID for direct profile redirection */
  trainerId?: number | null;
  /** Custom destination for the horse link if different from the default role route; null shows the name without a link. */
  horseHref?: string | null;
  /** What the role wants to see about this stall (tasks, incidents, etc.). */
  children?: ReactNode;
  /** What the role can do with this stall. */
  actions?: ReactNode;
  /** Shown before any stall is picked. */
  hint: string;
  /** Adds a close button to the header (used when the panel floats over the map). */
  onClose?: () => void;
}

function calculateAge(dateOfBirth: string | null | undefined): string | null {
  if (!dateOfBirth) return null;
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return null;
  const now = new Date();
  let years = now.getFullYear() - dob.getFullYear();
  const m = now.getMonth() - dob.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < dob.getDate())) {
    years--;
  }
  return years >= 0 ? `${years} years old` : null;
}

/** The side panel next to the floor plan. Sticks under the top bar while the map scrolls. */
export function StallDetailPanel({
  stall,
  area,
  horse,
  specialty,
  groomName,
  groomId,
  trainerName,
  trainerId,
  horseHref,
  children,
  actions,
  hint,
  onClose,
}: StallDetailPanelProps) {
  const { user } = useAuth();
  const [activePlan, setActivePlan] = useState<TreatmentPlan | null>(null);
  const [isMedModalOpen, setIsMedModalOpen] = useState(false);

  const horseId = horse?.id;
  useEffect(() => {
    let active = true;
    if (!horseId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setActivePlan(null);
      return;
    }
    treatmentPlanService
      .getActiveByHorseId(horseId)
      .then((plan) => {
        if (active) setActivePlan(plan);
      })
      .catch(() => {
        if (active) setActivePlan(null);
      });
    return () => {
      active = false;
    };
  }, [horseId]);

  const hasActiveTreatment = Boolean(activePlan && isPlanActiveToday(activePlan));

  if (!stall) {
    return (
      <Panel className="xl:sticky xl:top-[var(--sticky-top)]">
        <EmptyState icon="building" title="Select a stall" description={hint} />
      </Panel>
    );
  }

  const state = stallState(stall, horse);
  const occupancy = state.tone === 'danger' ? 'Occupied' : state.label;

  const rolePath =
    user?.role === 'HEAD_TRAINER'
      ? 'trainer'
      : user?.role === 'VETERINARIAN'
        ? 'veterinarian'
        : user?.role === 'GROOM'
          ? 'groom'
          : 'manager';
  // Grooms and vets have no horse profile page, so their horse name is plain text.
  const hasHorsePage = rolePath === 'trainer' || rolePath === 'manager';
  const targetHorseHref = horseHref !== undefined
    ? horseHref
    : horse && hasHorsePage ? `/${rolePath}/horses/${horse.id}` : null;

  const effectiveGroomId = groomId ?? stall?.groomId ?? null;
  const effectiveTrainerId = trainerId ?? area?.trainerId ?? null;
  const effectiveOwnerId = horse?.ownerId ?? null;
  const isManager = user?.role === 'CLUB_MANAGER';

  const ownerAccountHref =
    isManager && effectiveOwnerId
      ? `/manager/accounts?userId=${effectiveOwnerId}`
      : null;
  const groomStaffHref =
    isManager && effectiveGroomId
      ? `/manager/accounts?userId=${effectiveGroomId}`
      : null;
  const trainerStaffHref =
    isManager && effectiveTrainerId
      ? `/manager/accounts?userId=${effectiveTrainerId}`
      : null;

  return (
    <Panel
      key={stall.id}
      padded
      className="space-y-5 animate-in fade-in slide-in-from-right-2 duration-200 xl:sticky xl:top-[var(--sticky-top)]"
    >
      {/* Header: Stall code & occupancy */}
      <div className="flex items-start justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <div className="min-w-0">
          <h2 className="text-xl font-bold tracking-tight text-[var(--color-text-primary)]">Stall {stall.stallCode}</h2>
          <p className="mt-0.5 truncate text-xs font-medium text-[var(--color-text-muted)]">{area?.name ?? 'Unknown area'}</p>
        </div>
        <div className="flex shrink-0 items-center gap-1.5">
          <Pill size="sm" tone={occupancy === 'Occupied' ? 'success' : occupancy === 'Reserved' ? 'info' : 'neutral'}>
            {occupancy}
          </Pill>
          {onClose && <IconButton icon="x" label="Close stall details" size="sm" variant="tertiary" onClick={onClose} />}
        </div>
      </div>

      {horse ? (
        <div className="space-y-4">
          {/* Horse Identity Banner */}
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-3.5 min-w-0">
              <HorseAvatar name={horse.name} image={horse.imageUrl} size={48} rounded="md" />
              <div className="min-w-0">
                {targetHorseHref ? (
                  <Link
                    href={targetHorseHref}
                    className="group inline-flex items-center gap-1.5 font-bold text-base text-[var(--color-text-primary)] hover:text-[var(--color-primary)] transition-colors truncate"
                    title="View full horse profile"
                  >
                    <span className="truncate">{horse.name}</span>
                    <Icon name="external-link" size={13} className="shrink-0 opacity-60 group-hover:opacity-100 transition-opacity" />
                  </Link>
                ) : (
                  <p className="truncate text-base font-bold text-[var(--color-text-primary)]">{horse.name}</p>
                )}
                <p className="truncate text-xs font-medium text-[var(--color-text-secondary)]">{horse.breed ?? 'Breed not recorded'}</p>
              </div>
            </div>
            {horse.trainingDecision && (
              <Pill size="sm" tone={horse.trainingDecision === 'BLOCKED' ? 'danger' : 'success'}>
                {horse.trainingDecision === 'BLOCKED' ? 'Training blocked' : 'Training allowed'}
              </Pill>
            )}
          </div>

          {/* Blocked alert banner */}
          {horse.trainingDecision === 'BLOCKED' && horse.trainingDecisionReason && (
            <div className="rounded-lg bg-[var(--color-danger-soft)] border border-[var(--color-danger)]/20 p-2.5 text-xs text-[var(--color-danger)] leading-relaxed">
              ⚠️ {horse.trainingDecisionReason}
            </div>
          )}

          {/* Active Medication Treatment Banner */}
          {hasActiveTreatment && activePlan && (
            <div className="rounded-lg border border-[var(--color-warning)]/60 bg-[var(--color-warning-soft)]/30 p-3 flex items-center justify-between gap-3 animate-in fade-in duration-200">
              <div className="flex items-center gap-2.5 min-w-0">
                <Icon name="pill" size={18} className="text-[var(--color-warning)] shrink-0" />
                <div className="min-w-0 text-xs">
                  <p className="font-semibold text-[var(--color-text-primary)]">
                    Active Medication Treatment
                  </p>
                  <p className="text-[var(--color-text-muted)] truncate">
                    {activePlan.endDate
                      ? `Prescribed through ${formatDate(activePlan.endDate)}`
                      : 'Ongoing treatment plan'}
                  </p>
                </div>
              </div>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setIsMedModalOpen(true)}
                className="shrink-0"
              >
                View Instructions
              </Button>
            </div>
          )}

          {/* Full Key-Value Specifications */}
          <div className="space-y-2.5 pt-2 border-t border-[var(--color-border)] text-xs">
            <div className="flex items-center justify-between gap-3">
              <span className="text-[var(--color-text-muted)] font-medium">UELN</span>
              <span className="font-mono font-semibold text-[var(--color-text-primary)] text-right">
                {horse.registrationNumber || '—'}
              </span>
            </div>

            <div className="flex items-center justify-between gap-3">
              <span className="text-[var(--color-text-muted)] font-medium">Sex</span>
              <span className="font-semibold text-[var(--color-text-primary)] text-right">
                {formatEnumLabel(horse.sex)}
              </span>
            </div>

            <div className="flex items-center justify-between gap-3">
              <span className="text-[var(--color-text-muted)] font-medium">Age</span>
              <span className="font-semibold text-[var(--color-text-primary)] text-right">
                {calculateAge(horse.dateOfBirth) || '—'}
              </span>
            </div>

            {horse.dateOfBirth && (
              <div className="flex items-center justify-between gap-3">
                <span className="text-[var(--color-text-muted)] font-medium">Date of Birth</span>
                <span className="text-[var(--color-text-primary)] font-medium text-right">
                  {formatDate(horse.dateOfBirth)}
                </span>
              </div>
            )}

            <div className="flex items-center justify-between gap-3">
              <span className="text-[var(--color-text-muted)] font-medium">Owner</span>
              {ownerAccountHref ? (
                <Link
                  href={ownerAccountHref}
                  className="group inline-flex items-center gap-1 font-semibold text-[var(--color-primary)] hover:underline truncate max-w-[200px] text-right"
                  title="View owner account details"
                >
                  <span className="truncate">{horse.ownerName || `Owner #${effectiveOwnerId}`}</span>
                  <Icon name="external-link" size={12} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                </Link>
              ) : (
                <span className="font-semibold text-[var(--color-text-primary)] truncate max-w-[200px] text-right">
                  {horse.ownerName || (effectiveOwnerId ? `Owner #${effectiveOwnerId}` : 'Riverside Club')}
                </span>
              )}
            </div>

            {groomName && (
              <div className="flex items-center justify-between gap-3">
                <span className="text-[var(--color-text-muted)] font-medium">Assigned Groom</span>
                {groomStaffHref ? (
                  <Link
                    href={groomStaffHref}
                    className="group inline-flex items-center gap-1 font-semibold text-[var(--color-primary)] hover:underline truncate max-w-[200px] text-right"
                    title="View groom account details"
                  >
                    <span className="truncate">{groomName}</span>
                    <Icon name="external-link" size={12} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                  </Link>
                ) : (
                  <span className="font-semibold text-[var(--color-text-primary)] text-right">{groomName}</span>
                )}
              </div>
            )}

            {trainerName && (
              <div className="flex items-center justify-between gap-3">
                <span className="text-[var(--color-text-muted)] font-medium">Assigned Trainer</span>
                {trainerStaffHref ? (
                  <Link
                    href={trainerStaffHref}
                    className="group inline-flex items-center gap-1 font-semibold text-[var(--color-primary)] hover:underline truncate max-w-[200px] text-right"
                    title="View trainer account details"
                  >
                    <span className="truncate">{trainerName}</span>
                    <Icon name="external-link" size={12} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                  </Link>
                ) : (
                  <span className="font-semibold text-[var(--color-text-primary)] text-right">{trainerName}</span>
                )}
              </div>
            )}
          </div>

          <HorseSpecialtyBadges specialty={specialty} />
        </div>
      ) : (
        <div className="space-y-3">
          <p className="text-sm italic text-[var(--color-text-muted)]">No horse is stabled here.</p>
          {(groomName || trainerName) && (
            <div className="space-y-2 pt-2 border-t border-[var(--color-border)] text-xs">
              {groomName && (
                <div className="flex items-center justify-between gap-3">
                  <span className="text-[var(--color-text-muted)] font-medium">Assigned Groom</span>
                  {groomStaffHref ? (
                    <Link
                      href={groomStaffHref}
                      className="group inline-flex items-center gap-1 font-semibold text-[var(--color-primary)] hover:underline truncate max-w-[200px] text-right"
                      title="View groom account details"
                    >
                      <span className="truncate">{groomName}</span>
                      <Icon name="external-link" size={12} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                    </Link>
                  ) : (
                    <span className="font-semibold text-[var(--color-text-primary)] text-right">{groomName}</span>
                  )}
                </div>
              )}
              {trainerName && (
                <div className="flex items-center justify-between gap-3">
                  <span className="text-[var(--color-text-muted)] font-medium">Assigned Trainer</span>
                  {trainerStaffHref ? (
                    <Link
                      href={trainerStaffHref}
                      className="group inline-flex items-center gap-1 font-semibold text-[var(--color-primary)] hover:underline truncate max-w-[200px] text-right"
                      title="View trainer account details"
                    >
                      <span className="truncate">{trainerName}</span>
                      <Icon name="external-link" size={12} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                    </Link>
                  ) : (
                    <span className="font-semibold text-[var(--color-text-primary)] text-right">{trainerName}</span>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {children}

      {horse && activePlan && (
        <MedicationInstructionModal
          open={isMedModalOpen}
          onClose={() => setIsMedModalOpen(false)}
          horseName={horse.name}
          stallCode={stall.stallCode}
          diagnosis={activePlan.diagnosis}
          veterinarianName={activePlan.veterinarianName}
          prescriptions={activePlan.prescriptions ?? []}
          notes={activePlan.instructions}
          endDate={activePlan.endDate ? formatDate(activePlan.endDate) : null}
        />
      )}

      {actions && <div className="flex flex-col gap-2 border-t border-[var(--color-border)] pt-3">{actions}</div>}
    </Panel>
  );
}
