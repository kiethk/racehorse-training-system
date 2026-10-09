'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { ListSkeleton } from '@/components/ui/states';
import { PageHeader } from '@/components/ui/PageHeader';
import { ScreenLayout } from '@/components/ui/ScreenLayout';
import { stableApi } from '@/features/stable/services/stableService';
import type { Horse } from '@/features/stable/types';
import { racingService } from '../services/racingService';
import type { CreateRaceRegistrationRequest } from '../types';
import { displayError } from '@/lib/display';

export function TrainerRaceForm() {
  const router = useRouter();

  // Horse
  const [horses, setHorses] = useState<Horse[]>([]);
  const [loadingHorses, setLoadingHorses] = useState(true);
  const [horseError, setHorseError] = useState<string | null>(null);

  // Form state
  const [horseId, setHorseId] = useState<number | ''>('');
  const [selectionReason, setSelectionReason] = useState('');
  const [raceName, setRaceName] = useState('');
  const [raceCategory, setRaceCategory] = useState('');
  const [location, setLocation] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [eventTime, setEventTime] = useState('');
  const [organizer, setOrganizer] = useState('');
  const [sourceUrl, setSourceUrl] = useState('');
  const [nominationDeadline, setNominationDeadline] = useState('');
  const [distanceMeters, setDistanceMeters] = useState<number | ''>('');
  const [trackType, setTrackType] = useState('');
  const [prizeDetails, setPrizeDetails] = useState('');
  const [trainerNotes, setTrainerNotes] = useState('');

  // UI state
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchHorses() {
      try {
        setLoadingHorses(true);
        setHorseError(null);
        // Load horses in the trainer's block (mine=true) that are eligible (ELIGIBLE)
        const list = await stableApi.getHorses({ mine: true, status: 'ELIGIBLE' });
        // Matches Horse.canTrain() on the backend: ELIGIBLE and not blocked from training by the vet.
        const available = list.filter((h) => h.trainingDecision !== 'BLOCKED');
        setHorses(available);
      } catch (err) {
        console.error('Unable to load horses:', err);
        setHorseError(displayError(err, 'Unable to load horses.'));
      } finally {
        setLoadingHorses(false);
      }
    }
    fetchHorses();
  }, []);

  const todayStr = new Date().toISOString().split('T')[0];

  function validate(): boolean {
    const errs: Record<string, string> = {};

    if (!horseId) {
      errs.horseId = 'Select a horse assigned to you.';
    }
    if (!selectionReason.trim()) {
      errs.selectionReason = 'Enter a reason for nominating this horse.';
    }
    if (!raceName.trim()) {
      errs.raceName = 'Enter the race or event name.';
    }
    if (!raceCategory.trim()) {
      errs.raceCategory = 'Enter a specific race category (for example, 3-year-old, 1,200 m).';
    }
    if (!location.trim()) {
      errs.location = 'Enter the racecourse and location.';
    }
    if (!eventDate) {
      errs.eventDate = 'Select the race date.';
    } else if (eventDate < todayStr) {
      errs.eventDate = 'The event date cannot be in the past.';
    }

    if (nominationDeadline) {
      if (eventDate && nominationDeadline > eventDate) {
        errs.nominationDeadline = 'The nomination deadline must be on or before the event date.';
      }
    }

    if (sourceUrl.trim()) {
      const url = sourceUrl.trim();
      if (!url.startsWith('http://') && !url.startsWith('https://')) {
        errs.sourceUrl = 'Enter a valid HTTP or HTTPS URL.';
      }
    }

    if (distanceMeters !== '' && distanceMeters <= 0) {
      errs.distanceMeters = 'Distance must be a positive number.';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    setSubmitError(null);

    const payload: CreateRaceRegistrationRequest = {
      horseId: Number(horseId),
      raceName: raceName.trim(),
      raceCategory: raceCategory.trim(),
      location: location.trim(),
      eventDate,
      eventTime: eventTime ? eventTime : null,
      organizer: organizer.trim() ? organizer.trim() : null,
      sourceUrl: sourceUrl.trim() ? sourceUrl.trim() : null,
      nominationDeadline: nominationDeadline ? nominationDeadline : null,
      distanceMeters: distanceMeters !== '' ? Number(distanceMeters) : null,
      trackType: trackType.trim() ? trackType.trim() : null,
      prizeDetails: prizeDetails.trim() ? prizeDetails.trim() : null,
      selectionReason: selectionReason.trim(),
      trainerNotes: trainerNotes.trim() ? trainerNotes.trim() : null,
    };

    try {
      const created = await racingService.create(payload);
      router.push(`/trainer/racing/${created.id}`);
    } catch (err) {
      console.error('Unable to submit the nomination:', err);
      setSubmitError(displayError(err, 'Unable to submit the nomination.'));
    } finally {
      setSubmitting(false);
    }
  }

  const selectedHorse = horses.find((h) => h.id === horseId);

  return (
    <ScreenLayout variant="form">
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-4">
        <div>
          <div className="flex items-center gap-2">
            <Link
              href="/trainer/racing"
              className="text-[12px] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition"
            >
              ← Back to nominations
            </Link>
          </div>
          <PageHeader title="Create a race nomination" description="Submit an internal nomination for management review." />
        </div>
      </div>

      {submitError && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-4 text-[13px] text-[var(--color-danger)]">
          <strong>Submission error:</strong> {submitError}
        </div>
      )}

      {/* Part 1: nominated horse */}
      <Panel padded>
        <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-1 flex items-center gap-2">
          1. Nominated horse
        </h2>
        <p className="text-[12px] text-[var(--color-text-secondary)] mb-4">
          Only eligible horses in your area that are not blocked from training can be nominated.
        </p>

        {loadingHorses ? (
          <ListSkeleton rows={2} />
        ) : horseError ? (
          <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
            {horseError}
          </div>
        ) : horses.length === 0 ? (
          <div className="rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-4 text-[13px] text-[var(--color-warning)]">
            <p className="font-semibold">No horses are currently available for nomination.</p>
            <p className="mt-1 text-[12px]">
              Horses must be assigned to a stall in your area, have ELIGIBLE status, and not be blocked from training.
            </p>
            <div className="mt-3 flex gap-2">
              <Link href="/trainer/stable">
                <Button variant="secondary" size="sm" type="button">
                  View stable
                </Button>
              </Link>
              <Link href="/trainer/horses">
                <Button variant="secondary" size="sm" type="button">
                  View horses
                </Button>
              </Link>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
                Select a horse <span className="text-[var(--color-danger)]">*</span>
              </label>
              <select
                value={horseId}
                onChange={(e) => setHorseId(e.target.value ? Number(e.target.value) : '')}
                className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              >
                <option value="">-- Select an assigned horse --</option>
                {horses.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.name} {h.registrationNumber ? `(${h.registrationNumber})` : ''} {h.breed ? `• ${h.breed}` : ''}
                  </option>
                ))}
              </select>
              {errors.horseId && (
                <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.horseId}</p>
              )}
            </div>

            {selectedHorse && (
              <div className="rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-3 text-[12px] text-[var(--color-text-secondary)] border border-[var(--color-border)]">
                <div>
                  Selected horse: <strong className="text-[var(--color-text-primary)]">{selectedHorse.name}</strong>
                  {selectedHorse.registrationNumber && (
                    <span className="ml-2 font-metric">({selectedHorse.registrationNumber})</span>
                  )}
                </div>
                {selectedHorse.breed && <div>Breed: {selectedHorse.breed}</div>}
              </div>
            )}

            <div>
              <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
                Nomination reason <span className="text-[var(--color-danger)]">*</span>
              </label>
              <textarea
                rows={3}
                placeholder="Describe the horse’s fitness, preferred distance, current form, and suitability for this event…"
                value={selectionReason}
                onChange={(e) => setSelectionReason(e.target.value)}
                className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
              {errors.selectionReason && (
                <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.selectionReason}</p>
              )}
            </div>
          </div>
        )}
      </Panel>

      {/* Part 2: race information */}
      <Panel padded>
        <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-1 flex items-center gap-2">
          2. Race and event details
        </h2>
        <p className="text-[12px] text-[var(--color-text-secondary)] mb-4">
          Enter event information based on your external research.
        </p>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Race or event name <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Autumn Horse Racing Festival 2026"
              value={raceName}
              onChange={(e) => setRaceName(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.raceName && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.raceName}</p>
            )}
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Nomination category / Stage <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="text"
              placeholder="e.g. 3-year-old, 1,200 m or Open Cup"
              value={raceCategory}
              onChange={(e) => setRaceCategory(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.raceCategory && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.raceCategory}</p>
            )}
          </div>

          <div className="md:col-span-2">
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Racecourse & Location <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Soc Son Racecourse, Hanoi"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.location && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.location}</p>
            )}
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Event date <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="date"
              min={todayStr}
              value={eventDate}
              onChange={(e) => setEventDate(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.eventDate && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.eventDate}</p>
            )}
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Expected start time <span className="text-[var(--color-text-muted)]">(optional)</span>
            </label>
            <input
              type="time"
              value={eventTime}
              onChange={(e) => setEventTime(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Organizer <span className="text-[var(--color-text-muted)]">(optional)</span>
            </label>
            <input
              type="text"
              placeholder="e.g. National Horse Racing Federation…"
              value={organizer}
              onChange={(e) => setOrganizer(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Official nomination deadline <span className="text-[var(--color-text-muted)]">(optional)</span>
            </label>
            <input
              type="date"
              value={nominationDeadline}
              onChange={(e) => setNominationDeadline(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.nominationDeadline && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.nominationDeadline}</p>
            )}
          </div>

          <div className="md:col-span-2">
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Rules or official event URL <span className="text-[var(--color-text-muted)]">(optional, for management verification)</span>
            </label>
            <input
              type="url"
              placeholder="https://example.org/race-conditions"
              value={sourceUrl}
              onChange={(e) => setSourceUrl(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.sourceUrl && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.sourceUrl}</p>
            )}
          </div>
        </div>
      </Panel>

      {/* Part 3: reference information */}
      <Panel padded>
        <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-1 flex items-center gap-2">
          3. Additional event information
        </h2>
        <p className="text-[12px] text-[var(--color-text-secondary)] mb-4">
          Add race details, prize information, or logistics and documentation notes.
        </p>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Distance (metres) <span className="text-[var(--color-text-muted)]">(optional)</span>
            </label>
            <input
              type="number"
              placeholder="VD: 1200"
              min={1}
              value={distanceMeters}
              onChange={(e) => setDistanceMeters(e.target.value ? Number(e.target.value) : '')}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
            {errors.distanceMeters && (
              <p className="mt-1 text-[11px] text-[var(--color-danger)]">{errors.distanceMeters}</p>
            )}
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Track surface <span className="text-[var(--color-text-muted)]">(optional)</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Turf, dirt, synthetic…"
              value={trackType}
              onChange={(e) => setTrackType(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Prize structure <span className="text-[var(--color-text-muted)]">(optional)</span>
            </label>
            <textarea
              rows={2}
              placeholder="e.g. 1st: VND 50 million; 2nd: VND 25 million; 3rd: VND 10 million…"
              value={prizeDetails}
              onChange={(e) => setPrizeDetails(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Trainer notes <span className="text-[var(--color-text-muted)]">(optional)</span>
            </label>
            <textarea
              rows={2}
              placeholder="Age or gender requirements, handicap, veterinary checks, or transport arrangements…"
              value={trainerNotes}
              onChange={(e) => setTrainerNotes(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>
        </div>
      </Panel>

      {/* Actions */}
      <div className="flex flex-col items-end gap-2 border-t border-[var(--color-border)] pt-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-[12px] font-medium text-[var(--color-warning)] bg-[var(--color-warning-soft)] px-3 py-1.5 rounded-[var(--radius-md)] border border-[var(--color-warning)]">
          This is an internal nomination and is not a registration with the event organiser.
        </p>

        <div className="flex items-center gap-3">
          <Link href="/trainer/racing">
            <Button variant="secondary" type="button" disabled={submitting}>
              Cancel
            </Button>
          </Link>
          <Button
            variant="primary"
            type="submit"
            disabled={submitting || horses.length === 0}
          >
            {submitting ? 'Submitting…' : 'Submit for review'}
          </Button>
        </div>
      </div>
    </form>
    </ScreenLayout>
  );
}
