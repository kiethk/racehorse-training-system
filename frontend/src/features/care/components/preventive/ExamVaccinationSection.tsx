'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Button,
  Checkbox,
  FormField,
  IconButton,
  Input,
  Notice,
  Pill,
  Select,
  Skeleton,
} from '@/components/ui';
import { displayError, formatDate } from '@/lib/display';
import { preventiveCareService } from '../../services/preventiveCareService';
import type { HorsePreventiveStatus, PreventiveProtocol } from '../../types/preventive';
import { DueStatusPill, FEVER_THRESHOLD_C, isDueForCombinedVisit, todayIso } from './preventiveDisplay';
import { lastDoseDraft, lastDoseKey, type VaccinationDraft } from './vaccinationDrafts';

// The draft logic lives in a plain .ts file (testable without JSX); re-exported for existing imports.
export { toVaccinationRequests, validateVaccinationDrafts, type VaccinationDraft } from './vaccinationDrafts';

export const DEFER_DAY_OPTIONS = [7, 8, 9, 10] as const;

interface ExamVaccinationSectionProps {
  horseId: number;
  careType: 'INITIAL' | 'ROUTINE' | 'URGENT';
  purpose?: 'FOLLOW_UP' | 'PREVENTIVE';
  /** Raw value from the vitals input, in °C. */
  temperature: string;
  trainingBlocked: boolean;
  disabled: boolean;
  value: VaccinationDraft[];
  onChange: (next: VaccinationDraft[]) => void;
  deferDays: number | null;
  onDeferChange: (days: number | null) => void;
  errors?: Record<string, string>;
}

/**
 * Vaccinations in an exam (docs/preventive-care-flow.md 5.1, 5.2, 5.6).
 * Admission exam: the vet only records the last dose date of each protocol from the passport.
 * Other exams: suggests doses that are due, warns when the horse looks unwell, and lets a
 * preventive visit defer vaccination by 7–10 days.
 */
export function ExamVaccinationSection({
  horseId,
  careType,
  purpose,
  temperature,
  trainingBlocked,
  disabled,
  value,
  onChange,
  deferDays,
  onDeferChange,
  errors = {},
}: ExamVaccinationSectionProps) {
  const [status, setStatus] = useState<HorsePreventiveStatus | null>(null);
  const [protocols, setProtocols] = useState<PreventiveProtocol[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const draftSequence = useRef(0);
  const isAdmission = careType === 'INITIAL';
  const isPreventiveVisit = purpose === 'PREVENTIVE';

  useEffect(() => {
    let active = true;
    Promise.all([preventiveCareService.horseStatus(horseId), preventiveCareService.listProtocols()])
      .then(([horseStatus, list]) => {
        if (!active) return;
        setStatus(horseStatus);
        setProtocols(list);
      })
      .catch((cause) => active && setLoadError(displayError(cause, 'Could not load the vaccination status.')))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [horseId]);

  const today = todayIso();
  const due = useMemo(
    () => (status?.protocols ?? []).filter((p) => isDueForCombinedVisit(p.status, p.nextDueOn, today)),
    [status, today],
  );
  const givenToday = new Set(value.filter((d) => d.source === 'ADMINISTERED').map((d) => d.protocolId));
  const suggestions = due.filter((p) => !givenToday.has(p.protocolId));
  const addable = protocols.filter((p) => !givenToday.has(p.id));
  const fever = Number(temperature) > FEVER_THRESHOLD_C;
  const unwell = fever || trainingBlocked;

  const add = (protocolId: number) => {
    const protocol = protocols.find((p) => p.id === protocolId);
    draftSequence.current += 1;
    onChange([
      ...value,
      {
        key: `dose-${draftSequence.current}`,
        protocolId,
        dosage: protocol?.defaultDosage ?? '',
        route: protocol?.defaultRoute ?? '',
        source: 'ADMINISTERED',
        administeredOn: today,
      },
    ]);
    onDeferChange(null);
  };
  const update = (key: string, patch: Partial<VaccinationDraft>) =>
    onChange(value.map((draft) => (draft.key === key ? { ...draft, ...patch } : draft)));
  const remove = (key: string) => onChange(value.filter((draft) => draft.key !== key));
  const protocolName = (id: number) => protocols.find((p) => p.id === id)?.name ?? `Protocol #${id}`;

  // Admission: at most one "last dose" draft per protocol; clearing the date removes it. A dose dated today may
  // optionally be marked as given here at the club, which records it with dosage and route (ADMINISTERED).
  const lastDoseOf = (protocolId: number) => value.find((d) => d.key === lastDoseKey(protocolId));
  const writeLastDose = (protocol: PreventiveProtocol, date: string, atClub: boolean) => {
    const existing = lastDoseOf(protocol.id);
    const next = lastDoseDraft(protocol, date, today, atClub, existing);
    const others = value.filter((d) => d.key !== lastDoseKey(protocol.id));
    onChange(next ? [...others, next] : others);
  };

  return (
    <fieldset
      disabled={disabled}
      className="min-w-0 space-y-4 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-panel)]"
    >
      <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2">
        <h3 className="text-sm font-semibold tracking-tight text-[var(--color-text-primary)]">Vaccination &amp; deworming</h3>
        {isPreventiveVisit && <Pill tone="neutral" size="sm">Preventive visit</Pill>}
      </div>

      {loading ? (
        <Skeleton className="h-16" />
      ) : loadError ? (
        <Notice tone="error">{loadError}</Notice>
      ) : isAdmission ? (
        <div className="space-y-2">
          <p className="text-xs text-[var(--color-text-muted)]">
            Enter the date of the last vaccination or deworming from the horse&apos;s passport. Leave it empty when there is no record.
          </p>
          <ul className="divide-y divide-[var(--color-border)] rounded-[var(--radius-md)] border border-[var(--color-border)]">
            {protocols.map((protocol) => {
              const draft = lastDoseOf(protocol.id);
              const atClub = draft?.source === 'ADMINISTERED';
              const error = errors[lastDoseKey(protocol.id)];
              return (
                <li key={protocol.id} className="space-y-3 p-3">
                  <div className="flex flex-wrap items-end justify-between gap-3">
                    <div className="min-w-0">
                      <span className="flex flex-wrap items-center gap-2 text-sm font-medium text-[var(--color-text-primary)]">
                        {protocol.name}
                        {atClub && <Pill tone="primary" size="sm">Given at the club</Pill>}
                      </span>
                      <span className="block text-xs text-[var(--color-text-muted)]">
                        {protocol.mandatory ? 'Mandatory' : 'Recommended'} · repeat every {protocol.intervalDays} days
                      </span>
                    </div>
                    <FormField label="Last dose" className="w-44" error={atClub ? undefined : error}>
                      <Input type="date" value={draft?.administeredOn ?? ''} max={today}
                        onChange={(e) => writeLastDose(protocol, e.target.value, atClub)} />
                    </FormField>
                  </div>
                  {draft?.administeredOn === today && (
                    <Checkbox
                      label="Given at the club today"
                      checked={atClub}
                      onChange={(e) => writeLastDose(protocol, today, e.target.checked)}
                    />
                  )}
                  {atClub && draft && (
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                      <FormField label="Dosage" required error={error}>
                        <Input value={draft.dosage} maxLength={255}
                          onChange={(e) => update(draft.key, { dosage: e.target.value })} />
                      </FormField>
                      <FormField label="Route">
                        <Input value={draft.route} maxLength={100}
                          onChange={(e) => update(draft.key, { route: e.target.value })} />
                      </FormField>
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        </div>
      ) : (
        <>
          {unwell && (
            <Notice tone="warning" title="Consider deferring vaccination">
              {fever
                ? `Temperature is above ${FEVER_THRESHOLD_C} °C.`
                : 'The horse is currently blocked from training.'}{' '}
              Vaccinating an unwell horse can worsen the reaction and weaken the immune response.
            </Notice>
          )}

          {suggestions.length > 0 && deferDays == null && (
            <div className="space-y-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3">
              <p className="text-sm font-semibold text-[var(--color-text-primary)]">Due now or within 7 days — give it in this visit?</p>
              <ul className="space-y-1.5">
                {suggestions.map((p) => (
                  <li key={p.protocolId} className="flex flex-wrap items-center justify-between gap-2">
                    <span className="flex items-center gap-2 text-sm text-[var(--color-text-primary)]">
                      {p.name}
                      <DueStatusPill status={p.status} />
                      <span className="text-xs text-[var(--color-text-muted)]">
                        {p.nextDueOn ? `due ${formatDate(p.nextDueOn)}` : 'never recorded'}
                      </span>
                    </span>
                    <Button type="button" size="sm" variant="secondary" icon="plus" onClick={() => add(p.protocolId)}>
                      Add to this visit
                    </Button>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {value.length > 0 && (
            <ul className="space-y-3">
              {value.map((draft) => (
                <li key={draft.key} className="grid grid-cols-1 gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] p-3 md:grid-cols-[1fr_auto]">
                  <div className="space-y-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-semibold text-[var(--color-text-primary)]">{protocolName(draft.protocolId)}</span>
                      <Pill tone="neutral" size="sm">Given today</Pill>
                    </div>
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                      <FormField label="Dosage" required error={errors[draft.key]}>
                        <Input value={draft.dosage} maxLength={255} onChange={(e) => update(draft.key, { dosage: e.target.value })} />
                      </FormField>
                      <FormField label="Route">
                        <Input value={draft.route} maxLength={100} onChange={(e) => update(draft.key, { route: e.target.value })} />
                      </FormField>
                    </div>
                  </div>
                  <IconButton icon="x" label={`Remove ${protocolName(draft.protocolId)}`} onClick={() => remove(draft.key)} />
                </li>
              ))}
            </ul>
          )}

          {deferDays == null && (
            <div className="flex flex-wrap items-end gap-3">
              <FormField label="Add a dose given today" className="w-64">
                <Select value="" onChange={(e) => e.target.value && add(Number(e.target.value))}>
                  <option value="">Choose a protocol…</option>
                  {addable.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                </Select>
              </FormField>
            </div>
          )}

          {isPreventiveVisit && value.length === 0 && (
            <div className="flex flex-wrap items-end gap-3 border-t border-[var(--color-border)] pt-3">
              <Checkbox
                label="Defer vaccination"
                description="Book this visit again later instead of vaccinating today."
                checked={deferDays != null}
                onChange={(e) => onDeferChange(e.target.checked ? 7 : null)}
              />
              {deferDays != null && (
                <FormField label="Book again in" className="w-40">
                  <Select value={deferDays} onChange={(e) => onDeferChange(Number(e.target.value))}>
                    {DEFER_DAY_OPTIONS.map((days) => <option key={days} value={days}>{days} days</option>)}
                  </Select>
                </FormField>
              )}
            </div>
          )}

          {value.length === 0 && deferDays == null && suggestions.length === 0 && (
            <p className="text-xs text-[var(--color-text-muted)]">No vaccination is due for this horse in the next 7 days.</p>
          )}
        </>
      )}
    </fieldset>
  );
}
