'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { ListSkeleton } from '@/components/ui/states';
import { stableApi } from '@/features/stable/services/stableService';
import type { Horse } from '@/features/stable/types';
import { racingService } from '../services/racingService';
import type { CreateRaceRegistrationRequest } from '../types';

export function TrainerRaceForm() {
  const router = useRouter();

  // Ngựa
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
        // Lấy ngựa trong khu trainer (mine=true), đủ điều kiện (ELIGIBLE)
        const list = await stableApi.getHorses({ mine: true, status: 'ELIGIBLE' });
        // Khớp Horse.canTrain() ở backend: ELIGIBLE và Thú y không chặn tập.
        const available = list.filter((h) => h.trainingDecision !== 'BLOCKED');
        setHorses(available);
      } catch (err) {
        console.error('Failed to load horses:', err);
        setHorseError(err instanceof Error ? err.message : 'Unable to load horses.');
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
      errs.horseId = 'Select an assigned horse.';
    }
    if (!selectionReason.trim()) {
      errs.selectionReason = 'Enter the reason for nominating this horse.';
    }
    if (!raceName.trim()) {
      errs.raceName = 'Enter the race or event name.';
    }
    if (!raceCategory.trim()) {
      errs.raceCategory = 'Enter a specific category or leg (for example, 3-year-old - 1,200 m).';
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
        errs.nominationDeadline = 'The submission deadline cannot be after the race date.';
      }
    }

    if (sourceUrl.trim()) {
      const url = sourceUrl.trim();
      if (!url.startsWith('http://') && !url.startsWith('https://')) {
        errs.sourceUrl = 'The source link must be a valid HTTP or HTTPS URL.';
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
      console.error('Failed to submit race nomination:', err);
      setSubmitError(err instanceof Error ? err.message : 'Unable to submit the race nomination.');
    } finally {
      setSubmitting(false);
    }
  }

  const selectedHorse = horses.find((h) => h.id === horseId);

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-4">
        <div>
          <div className="flex items-center gap-2">
            <Link
              href="/trainer/racing"
              className="text-[12px] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition"
            >
              ← Nominations
            </Link>
          </div>
          <h1 className="mt-1 text-[20px] font-bold text-[var(--color-text-primary)]">
            Create race nomination
          </h1>
          <p className="text-[13px] text-[var(--color-text-secondary)]">
            Submit an internal nomination for management review.
          </p>
        </div>
      </div>

      {submitError && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-4 text-[13px] text-[var(--color-danger)]">
          <strong>Submission error:</strong> {submitError}
        </div>
      )}

      {/* Phần 1: Ngựa đề cử */}
      <Panel padded>
        <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-1 flex items-center gap-2">
          <span aria-hidden="true">Horse</span> 1. Nominated horse
        </h2>
        <p className="text-[12px] text-[var(--color-text-secondary)] mb-4">
          Only assigned horses with ELIGIBLE status and no training lock are available.
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
              Horses must be assigned to your area, have ELIGIBLE status and not be on a training hold.
            </p>
            <div className="mt-3 flex gap-2">
              <Link href="/trainer/stable">
                <Button variant="secondary" size="sm" type="button">
                  View stable management
                </Button>
              </Link>
              <Link href="/trainer/horses">
                <Button variant="secondary" size="sm" type="button">
                  View horse list
                </Button>
              </Link>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
                Select horse <span className="text-[var(--color-danger)]">*</span>
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
                placeholder="Describe the horse's fitness, preferred distance, current form and suitability for this race..."
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

      {/* Phần 2: Thông tin cuộc đua */}
      <Panel padded>
        <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-1 flex items-center gap-2">
          <span aria-hidden="true">Race</span> 2. Race / event details
        </h2>
        <p className="text-[12px] text-[var(--color-text-secondary)] mb-4">
          Research the event using external sources.
        </p>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Race / event name <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="text"
              placeholder="For example: Autumn Horse Racing 2026"
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
              Nominated category / leg <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="text"
              placeholder="For example: 3-year-old - 1,200 m or Open Cup"
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
              Racecourse & location <span className="text-[var(--color-danger)]">*</span>
            </label>
            <input
              type="text"
              placeholder="For example: Soc Son Racecourse, Hanoi"
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
              placeholder="For example: Horse Racing Federation..."
              value={organizer}
              onChange={(e) => setOrganizer(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Official submission deadline <span className="text-[var(--color-text-muted)]">(optional)</span>
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
              Rules / official link <span className="text-[var(--color-text-muted)]">(optional, for management verification)</span>
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

      {/* Phần 3: Thông tin tham khảo */}
      <Panel padded>
        <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-1 flex items-center gap-2">
          <span aria-hidden="true">Details</span> 3. Reference information & conditions
        </h2>
        <p className="text-[12px] text-[var(--color-text-secondary)] mb-4">
          Add race, prize, logistics or documentation details.
        </p>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Distance (meters) <span className="text-[var(--color-text-muted)]">(optional)</span>
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
              Surface / track <span className="text-[var(--color-text-muted)]">(optional)</span>
            </label>
            <input
              type="text"
              placeholder="For example: Turf, Dirt, Synthetic..."
              value={trackType}
              onChange={(e) => setTrackType(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Prize structure / purse <span className="text-[var(--color-text-muted)]">(optional)</span>
            </label>
            <textarea
              rows={2}
              placeholder="For example: First 50m VND; second 25m VND; third 10m VND..."
              value={prizeDetails}
              onChange={(e) => setPrizeDetails(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-[13px] font-medium text-[var(--color-text-primary)] mb-1">
              Additional trainer notes <span className="text-[var(--color-text-muted)]">(optional)</span>
            </label>
            <textarea
              rows={2}
              placeholder="Age, sex or handicap conditions, veterinary procedures or transport plan..."
              value={trainerNotes}
              onChange={(e) => setTrainerNotes(e.target.value)}
              className="w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
            />
          </div>
        </div>
      </Panel>

      {/* Hành động */}
      <div className="flex flex-col items-end gap-2 border-t border-[var(--color-border)] pt-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-[12px] font-medium text-[var(--color-warning-strong)] bg-[var(--color-warning-soft)] px-3 py-1.5 rounded-[var(--radius-md)] border border-[var(--color-warning)]">
          This is an internal nomination and is not registered with the organizer.
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
            {submitting ? 'Submitting...' : 'Submit for review'}
          </Button>
        </div>
      </div>
    </form>
  );
}
