'use client';

import {
  Icon,
  Pill,
  StatusBadge,
  Table,
  TBody,
  Td,
  Th,
  Tr,
} from '@/components/ui';
import type { HorseStatus } from '@/components/ui/StatusBadge';
import type { Horse } from '@/features/stable/types';
import type { HorsePedigree } from '../types';
import { formatDate } from '@/lib/display';
import { formatHorseSex } from '../lib/sex';

interface HorseOverviewPedigreeTableProps {
  horse: Horse;
  pedigree: HorsePedigree | null;
}

export function HorseOverviewPedigreeTable({ horse, pedigree }: HorseOverviewPedigreeTableProps) {
  const hasSire = Boolean(pedigree?.sire || pedigree?.sireName || pedigree?.sireRegistrationNumber);
  const hasDam = Boolean(pedigree?.dam || pedigree?.damName || pedigree?.damRegistrationNumber);

  // Sire details
  const sireName = pedigree?.sire?.name || pedigree?.sireName || (hasSire ? 'External Sire' : 'Not recorded');
  const sireBreed = pedigree?.sire?.breed || (hasSire && !pedigree?.sire ? 'External ancestor' : null);
  const sireDob = pedigree?.sire?.dateOfBirth ? formatDate(pedigree.sire.dateOfBirth) : null;
  const sireUeln = pedigree?.sireRegistrationNumber || (hasSire ? 'Not recorded' : '—');
  const sireStatusBadge = pedigree?.sire?.currentStatus ? (
    <StatusBadge status={pedigree.sire.currentStatus as HorseStatus} size="sm" />
  ) : hasSire ? (
    <Pill tone="neutral" size="sm">Outside system</Pill>
  ) : null;

  // Dam details
  const damName = pedigree?.dam?.name || pedigree?.damName || (hasDam ? 'External Dam' : 'Not recorded');
  const damBreed = pedigree?.dam?.breed || (hasDam && !pedigree?.dam ? 'External ancestor' : null);
  const damDob = pedigree?.dam?.dateOfBirth ? formatDate(pedigree.dam.dateOfBirth) : null;
  const damUeln = pedigree?.damRegistrationNumber || (hasDam ? 'Not recorded' : '—');
  const damStatusBadge = pedigree?.dam?.currentStatus ? (
    <StatusBadge status={pedigree.dam.currentStatus as HorseStatus} size="sm" />
  ) : hasDam ? (
    <Pill tone="neutral" size="sm">Outside system</Pill>
  ) : null;

  const registrationNo = horse.registrationNumber || pedigree?.registrationNumber || 'Not registered';
  const registryName = horse.registryName || pedigree?.registryName || 'Not recorded';

  return (
    <Table>
      <TBody>
        {/* ── Section 1: Horse Overview & Identification ── */}
        <Tr className="bg-[var(--color-surface-muted)]">
          <Th colSpan={4} className="py-2.5 px-4 text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
            <div className="flex items-center gap-1.5">
              <Icon name="info" size={14} className="text-[var(--color-primary)]" />
              <span>Horse Overview & Identification</span>
            </div>
          </Th>
        </Tr>

        <Tr>
          <Td className="w-1/5 font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
            Horse ID
          </Td>
          <Td className="w-[30%] font-semibold text-[var(--color-text-primary)]">
            #{horse.id}
          </Td>
          <Td className="w-1/5 font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
            Current Status
          </Td>
          <Td className="w-[30%]">
            <StatusBadge status={horse.currentStatus} size="sm" />
          </Td>
        </Tr>

        <Tr>
          <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
            Breed
          </Td>
          <Td className="font-medium text-[var(--color-text-primary)]">
            {horse.breed || 'Unknown'}
          </Td>
          <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
            Sex
          </Td>
          <Td className="font-medium text-[var(--color-text-primary)]">
            {formatHorseSex(horse.sex)}
          </Td>
        </Tr>

        <Tr>
          <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
            Date of Birth
          </Td>
          <Td className="font-medium text-[var(--color-text-primary)]">
            {horse.dateOfBirth ? formatDate(horse.dateOfBirth) : 'Unknown'}
          </Td>
          <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
            Training Clearance
          </Td>
          <Td>
            <Pill tone={horse.trainingDecision === 'ALLOWED' ? 'success' : 'danger'} size="sm">
              {horse.trainingDecision || 'ALLOWED'}
            </Pill>
          </Td>
        </Tr>

        <Tr>
          <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
            Registration (UELN)
          </Td>
          <Td className="font-mono text-xs font-semibold text-[var(--color-text-primary)]">
            {registrationNo}
          </Td>
          <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
            Registry
          </Td>
          <Td className="font-medium text-[var(--color-text-primary)]">
            {registryName}
          </Td>
        </Tr>

        <Tr>
          <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
            Stable Location
          </Td>
          <Td className="font-medium text-[var(--color-text-primary)]">
            {horse.currentStallId ? `Stall #${horse.currentStallId}` : 'Unassigned'}
          </Td>
          <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
            Status Detail
          </Td>
          <Td className="text-xs text-[var(--color-text-secondary)]">
            {horse.statusReason || (horse.trainingDecision === 'BLOCKED' ? horse.trainingDecisionReason : 'Active / Cleared')}
          </Td>
        </Tr>

        {/* ── Section 2: Pedigree & Lineage ── */}
        <Tr className="bg-[var(--color-surface-muted)]">
          <Th colSpan={4} className="py-2.5 px-4 text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
            <div className="flex items-center gap-1.5">
              <Icon name="git-branch" size={14} className="text-[var(--color-primary)]" />
              <span>Pedigree & Lineage</span>
            </div>
          </Th>
        </Tr>

        {!pedigree ? (
          <Tr>
            <Td colSpan={4} className="py-4 text-center text-xs text-[var(--color-text-muted)] italic">
              Official ancestral lineage and pedigree details have not been submitted for this horse.
            </Td>
          </Tr>
        ) : (
          <>
            {/* Sire (Father) */}
            <Tr>
              <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
                <span className="font-semibold text-[var(--color-primary)]">
                  Sire (Father)
                </span>
              </Td>
              <Td>
                <div className="space-y-0.5">
                  <div className="font-semibold text-[var(--color-text-primary)]">
                    {sireName}
                  </div>
                  {(sireBreed || sireDob) && (
                    <div className="text-xs text-[var(--color-text-muted)]">
                      {sireBreed}
                      {sireBreed && sireDob ? ' · ' : ''}
                      {sireDob ? `Born: ${sireDob}` : ''}
                    </div>
                  )}
                </div>
              </Td>
              <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
                Sire UELN
              </Td>
              <Td>
                <div className="flex items-center justify-between gap-2">
                  <span className="font-mono text-xs text-[var(--color-text-primary)]">{sireUeln}</span>
                  {sireStatusBadge}
                </div>
              </Td>
            </Tr>

            {/* Dam (Mother) */}
            <Tr>
              <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
                <span className="font-semibold text-[var(--color-info)]">
                  Dam (Mother)
                </span>
              </Td>
              <Td>
                <div className="space-y-0.5">
                  <div className="font-semibold text-[var(--color-text-primary)]">
                    {damName}
                  </div>
                  {(damBreed || damDob) && (
                    <div className="text-xs text-[var(--color-text-muted)]">
                      {damBreed}
                      {damBreed && damDob ? ' · ' : ''}
                      {damDob ? `Born: ${damDob}` : ''}
                    </div>
                  )}
                </div>
              </Td>
              <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
                Dam UELN
              </Td>
              <Td>
                <div className="flex items-center justify-between gap-2">
                  <span className="font-mono text-xs text-[var(--color-text-primary)]">{damUeln}</span>
                  {damStatusBadge}
                </div>
              </Td>
            </Tr>

            {/* Pedigree Notes */}
            {pedigree.pedigreeNotes && (
              <Tr>
                <Td className="font-medium text-[var(--color-text-muted)] bg-[var(--color-surface-subtle)]/40 text-xs uppercase tracking-wide">
                  Pedigree Notes
                </Td>
                <Td colSpan={3} className="text-xs text-[var(--color-text-secondary)] italic">
                  {pedigree.pedigreeNotes}
                </Td>
              </Tr>
            )}
          </>
        )}
      </TBody>
    </Table>
  );
}
