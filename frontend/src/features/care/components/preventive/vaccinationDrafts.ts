import type { PreventiveProtocol, VaccinationRequest, VaccinationSource } from '../../types/preventive.ts';
import { todayIso } from './preventiveDates.ts';

/** One dose being entered in the exam form. `key` only keeps React rows stable. */
export interface VaccinationDraft {
  key: string;
  protocolId: number;
  dosage: string;
  route: string;
  source: VaccinationSource;
  administeredOn: string;
}

/** Errors keyed by the draft key; empty when the doses can be submitted. */
export function validateVaccinationDrafts(drafts: VaccinationDraft[], today = todayIso()): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const draft of drafts) {
    if (draft.source === 'HISTORICAL') {
      if (!draft.administeredOn || draft.administeredOn > today) errors[draft.key] = 'Pick a date on or before today.';
    } else if (!draft.dosage.trim()) {
      errors[draft.key] = 'Dosage is required.';
    }
  }
  return errors;
}

export function toVaccinationRequests(drafts: VaccinationDraft[]): VaccinationRequest[] {
  return drafts.map((draft) => ({
    protocolId: draft.protocolId,
    dosage: draft.dosage.trim() || undefined,
    route: draft.route.trim() || undefined,
    source: draft.source,
    administeredOn: draft.source === 'HISTORICAL' ? draft.administeredOn : undefined,
  }));
}

/** Admission exam: one "last dose" row per protocol, under this key. */
export const lastDoseKey = (protocolId: number) => `last-dose-${protocolId}`;

/**
 * The admission row for a protocol's last dose. A passport date is HISTORICAL. When the date is today the vet
 * may say the dose was given here at the club: it then becomes a real dose (ADMINISTERED) with dosage and
 * route, prefilled from the protocol or kept from what was typed. A dose at the club can only be today, so
 * any other date falls back to HISTORICAL. An empty date removes the row (null).
 */
export function lastDoseDraft(
  protocol: Pick<PreventiveProtocol, 'id' | 'defaultDosage' | 'defaultRoute'>,
  date: string,
  today: string,
  atClub: boolean,
  existing?: VaccinationDraft,
): VaccinationDraft | null {
  if (!date) return null;
  const key = lastDoseKey(protocol.id);
  if (date === today && atClub) {
    const kept = existing?.source === 'ADMINISTERED' ? existing : null;
    return {
      key,
      protocolId: protocol.id,
      dosage: kept ? kept.dosage : protocol.defaultDosage ?? '',
      route: kept ? kept.route : protocol.defaultRoute ?? '',
      source: 'ADMINISTERED',
      administeredOn: today,
    };
  }
  return { key, protocolId: protocol.id, dosage: '', route: '', source: 'HISTORICAL', administeredOn: date };
}
