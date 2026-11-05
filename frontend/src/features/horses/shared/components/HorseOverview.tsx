'use client';

import { useEffect, useState, type ReactNode } from 'react';
import Link from 'next/link';

import { useAuth } from '@/context/AuthContext';
import { Icon } from '@/components/ui';
import type { HorseSpecialty, HorseAdmissionHistory } from '@/features/horses/types';
import type { Horse } from '@/features/stable/types';
import { formatDate } from '@/lib/display';
import { cn } from '@/lib/cn';
import { formatHorseSex } from '../../lib/sex';
import { horseService } from '../../services/horseService';
import { HorseInfoRow, HorseInfoSection } from './HorseInfoSection';
import { HorseSpecialtySection } from './HorseSpecialtySection';

/** Common first tab. Actors can append context without duplicating the profile. */
export function HorseOverview({
  horse,
  specialty = null,
  showSpecialty = true,
  children,
}: {
  horse: Horse;
  specialty?: HorseSpecialty | null;
  showSpecialty?: boolean;
  children?: ReactNode;
}) {
  const { user } = useAuth();
  const [admissionId, setAdmissionId] = useState<number | null>(null);
  const [loadingAdmission, setLoadingAdmission] = useState(true);

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

  useEffect(() => {
    let active = true;
    horseService
      .getAdmissionHistory(horse.id)
      .then((history: HorseAdmissionHistory[]) => {
        if (active && history.length > 0) {
          setAdmissionId(history[0].admissionId);
        }
      })
      .catch(() => undefined)
      .finally(() => {
        if (active) setLoadingAdmission(false);
      });
    return () => {
      active = false;
    };
  }, [horse.id]);

  return (
    <div className="@container min-w-0 space-y-5">
      <div className={cn('grid min-w-0 items-start gap-5', showSpecialty ? '@3xl:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]' : 'grid-cols-1')}>
        <HorseInfoSection
          title="Profile details"
          icon="clipboard"
          aside={
            admissionId ? (
              <Link
                href={`/${rolePath}/admissions/${admissionId}`}
                className="group inline-flex items-center gap-1 text-xs font-semibold text-[var(--color-primary)] hover:underline"
                title="View official documents in admission record"
              >
                <Icon name="file-text" size={13} />
                <span>Documents</span>
                <Icon name="external-link" size={11} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
              </Link>
            ) : null
          }
        >
          <div className="space-y-3.5">
            <HorseInfoRow label="Registration number" value={horse.registrationNumber?.trim()} />
            <HorseInfoRow label="Registry" value={horse.registryName?.trim()} />
            <HorseInfoRow label="Date of birth" value={horse.dateOfBirth ? formatDate(horse.dateOfBirth) : null} />
            <HorseInfoRow label="Sex" value={formatHorseSex(horse.sex)} />
            <HorseInfoRow
              label="Official documents"
              value={
                loadingAdmission ? (
                  <span className="text-[var(--color-text-muted)] italic">Checking records...</span>
                ) : admissionId ? (
                  <Link
                    href={`/${rolePath}/admissions/${admissionId}`}
                    className="group inline-flex items-center gap-1.5 font-semibold text-[var(--color-primary)] hover:underline"
                    title="View official horse documents in admission dossier"
                  >
                    <span>View admission documents</span>
                    <Icon name="external-link" size={13} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                  </Link>
                ) : (
                  <span className="text-[var(--color-text-muted)] italic">No admission documents attached</span>
                )
              }
            />
          </div>
        </HorseInfoSection>
        {showSpecialty && <HorseSpecialtySection specialty={specialty} />}
      </div>
      {children}
    </div>
  );
}
