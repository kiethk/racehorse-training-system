'use client';

import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { EmptyState, Icon, StatusBadge } from '@/components/ui';
import { Pill, type HorseStatus } from '@/components/ui/StatusBadge';
import { HorseInfoSection } from '../shared/components/HorseInfoSection';
import type { HorsePedigree } from '../types';
import type { Horse } from '@/features/stable/types';

interface HorsePedigreeCardProps {
  pedigree: HorsePedigree | null;
  horse?: Horse | null;
}

export function HorsePedigreeCard({ pedigree, horse }: HorsePedigreeCardProps) {
  const { user } = useAuth();
  const rolePath =
    user?.role === 'HEAD_TRAINER'
      ? 'trainer'
      : user?.role === 'VETERINARIAN'
        ? 'veterinarian'
        : user?.role === 'HORSE_OWNER'
          ? 'owner'
          : user?.role === 'GROOM'
            ? 'groom'
            : 'manager';

  if (!pedigree) {
    return (
      <HorseInfoSection
        title="Ancestral Pedigree & Bloodline"
        icon="git-branch"
      >
        <EmptyState
          icon="file-text"
          title="No Pedigree Record Yet"
          description="Official ancestral lineage and registration details have not been submitted for this horse."
        />
      </HorseInfoSection>
    );
  }

  const subjectName = horse?.name || pedigree.horse?.name || 'Subject Horse';
  const subjectUeln = horse?.registrationNumber || pedigree.registrationNumber || 'Not recorded';
  const subjectStatus = horse?.currentStatus || pedigree.horse?.currentStatus;

  const hasSire = Boolean(pedigree.sire || pedigree.sireName || pedigree.sireRegistrationNumber);
  const hasDam = Boolean(pedigree.dam || pedigree.damName || pedigree.damRegistrationNumber);

  const sireName = pedigree.sire?.name || pedigree.sireName || (hasSire ? 'External Sire' : 'Not recorded');
  const sireUeln = pedigree.sireRegistrationNumber || (hasSire ? 'Not recorded' : '—');
  const sireIsInternal = Boolean(pedigree.sire?.id);

  const damName = pedigree.dam?.name || pedigree.damName || (hasDam ? 'External Dam' : 'Not recorded');
  const damUeln = pedigree.damRegistrationNumber || (hasDam ? 'Not recorded' : '—');
  const damIsInternal = Boolean(pedigree.dam?.id);

  return (
    <HorseInfoSection
      title="Ancestral Pedigree & Bloodline"
      icon="git-branch"
    >
      <div className="space-y-4">
        {/* Pedigree Tree Diagram (Bracket Layout) */}
        <div className="overflow-x-auto py-2">
          <div className="flex items-center min-w-[560px] max-w-3xl py-2 px-1">
            {/* 1. Subject Horse Node */}
            <div className="w-56 shrink-0 rounded-xl border-2 border-[var(--color-primary)]/40 bg-[var(--color-surface)] p-3.5 shadow-sm transition-all hover:border-[var(--color-primary)]">
              <div className="flex items-center justify-between gap-1 mb-1.5">
                <span className="text-[11px] font-bold uppercase tracking-wider text-[var(--color-primary)]">
                  Subject Horse
                </span>
                {subjectStatus && (
                  <StatusBadge status={subjectStatus as HorseStatus} size="sm" />
                )}
              </div>
              <div className="font-bold text-sm text-[var(--color-text-primary)] truncate" title={subjectName}>
                {subjectName}
              </div>
              <div className="font-mono text-xs text-[var(--color-text-muted)] truncate mt-1">
                {subjectUeln}
              </div>
            </div>

            {/* 2. Bracket Line Connector (Fork) */}
            <div className="w-14 shrink-0 self-stretch flex items-center justify-center relative">
              <svg
                className="w-full h-full text-[var(--color-border-strong)]"
                viewBox="0 0 56 160"
                preserveAspectRatio="none"
                fill="none"
              >
                {/* Horizontal stem from horse to junction */}
                <path d="M 0 80 H 28" stroke="currentColor" strokeWidth="2" />
                {/* Vertical stem between branches */}
                <path d="M 28 40 V 120" stroke="currentColor" strokeWidth="2" />
                {/* Top horizontal branch into Sire */}
                <path d="M 28 40 H 56" stroke="currentColor" strokeWidth="2" />
                {/* Bottom horizontal branch into Dam */}
                <path d="M 28 120 H 56" stroke="currentColor" strokeWidth="2" />
              </svg>
            </div>

            {/* 3. Parents Column (Sire & Dam) */}
            <div className="grid grid-rows-2 gap-4 flex-1 min-w-[240px]">
              {/* Sire (Father) Node */}
              <div className="rounded-xl border-l-4 border-l-[var(--color-primary)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3.5 shadow-sm transition-all hover:shadow-md">
                <div className="flex items-center justify-between gap-1 mb-1.5">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-[var(--color-primary)]">
                    Sire (Father)
                  </span>
                  {pedigree.sire?.currentStatus ? (
                    <StatusBadge status={pedigree.sire.currentStatus as HorseStatus} size="sm" />
                  ) : hasSire ? (
                    <Pill tone="neutral" size="sm">Outside system</Pill>
                  ) : null}
                </div>
                <div>
                  {sireIsInternal ? (
                    <Link
                      href={`/${rolePath}/horses/${pedigree.sire!.id}`}
                      className="group inline-flex items-center gap-1.5 font-bold text-sm text-[var(--color-primary)] hover:underline truncate max-w-full"
                      title="View sire profile"
                    >
                      <span className="truncate">{sireName}</span>
                      <Icon name="external-link" size={13} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                    </Link>
                  ) : (
                    <span className="font-bold text-sm text-[var(--color-text-primary)] truncate block" title={sireName}>
                      {sireName}
                    </span>
                  )}
                </div>
                <div className="font-mono text-xs text-[var(--color-text-muted)] truncate mt-1">
                  {sireUeln}
                </div>
              </div>

              {/* Dam (Mother) Node */}
              <div className="rounded-xl border-l-4 border-l-[var(--color-info)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3.5 shadow-sm transition-all hover:shadow-md">
                <div className="flex items-center justify-between gap-1 mb-1.5">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-[var(--color-info)]">
                    Dam (Mother)
                  </span>
                  {pedigree.dam?.currentStatus ? (
                    <StatusBadge status={pedigree.dam.currentStatus as HorseStatus} size="sm" />
                  ) : hasDam ? (
                    <Pill tone="neutral" size="sm">Outside system</Pill>
                  ) : null}
                </div>
                <div>
                  {damIsInternal ? (
                    <Link
                      href={`/${rolePath}/horses/${pedigree.dam!.id}`}
                      className="group inline-flex items-center gap-1.5 font-bold text-sm text-[var(--color-info)] hover:underline truncate max-w-full"
                      title="View dam profile"
                    >
                      <span className="truncate">{damName}</span>
                      <Icon name="external-link" size={13} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                    </Link>
                  ) : (
                    <span className="font-bold text-sm text-[var(--color-text-primary)] truncate block" title={damName}>
                      {damName}
                    </span>
                  )}
                </div>
                <div className="font-mono text-xs text-[var(--color-text-muted)] truncate mt-1">
                  {damUeln}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </HorseInfoSection>
  );
}
