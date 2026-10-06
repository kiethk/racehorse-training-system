'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';
import { incidentApi } from '../services/incidentService';
import { stableApi } from '@/features/stable/services/stableService';
import type { Horse, StableStall } from '@/features/stable/types';
import type { IncidentReport, IncidentSeverity } from '../types';

const SEVERITY_OPTIONS: { value: IncidentSeverity; label: string; hint: string; tone: string }[] = [
  { value: 'LOW', label: 'Minor', hint: 'Monitor the horse; no urgent intervention is required.', tone: 'text-gray-600' },
  { value: 'MEDIUM', label: 'Moderate', hint: 'Veterinary review is needed today.', tone: 'text-blue-600' },
  { value: 'HIGH', label: 'Major', hint: 'Request veterinary attention as soon as possible.', tone: 'text-amber-600' },
  { value: 'CRITICAL', label: 'Critical', hint: 'A health emergency requiring an immediate call.', tone: 'text-red-600 font-semibold' },
];

export function IncidentForm() {
  const router = useRouter();
  const { user } = useAuth();

  const [loadingInitial, setLoadingInitial] = useState(true);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);

  const [horseId, setHorseId] = useState<number | ''>('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [severity, setSeverity] = useState<IncidentSeverity>('MEDIUM');
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadData() {
      try {
        setLoadingInitial(true);
        const [stallList, horseList] = await Promise.all([
          stableApi.getStalls(),
          stableApi.getHorses(),
        ]);

        setStalls(stallList);

        // Keep only horses assigned to stalls managed by the current groom.
        const myStallIds = stallList
          .filter((s) => s.groomId === user?.userId)
          .map((s) => s.id);

        const myHorses = horseList.filter(
          (h) => h.currentStallId && myStallIds.includes(h.currentStallId)
        );

        setHorses(myHorses);
        if (myHorses.length > 0) {
          setHorseId(myHorses[0].id);
        }
      } catch (err) {
        console.error('Unable to load horse and stall data:', err);
        setError('Unable to load the horse list. Please reload the page.');
      } finally {
        setLoadingInitial(false);
      }
    }

    if (user?.userId) {
      loadData();
    }
  }, [user?.userId]);

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0] ?? null;
    if (f) {
      if (f.size > 10 * 1024 * 1024) {
        setError('The image must not exceed 10 MB.');
        return;
      }
      setFile(f);
      const url = URL.createObjectURL(f);
      setPreviewUrl(url);
    } else {
      setFile(null);
      setPreviewUrl(null);
    }
  }

  function handleRemoveFile() {
    setFile(null);
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      setPreviewUrl(null);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!horseId) {
      setError('Please select the horse involved in the incident.');
      return;
    }
    if (!title.trim()) {
      setError('Please enter an incident title.');
      return;
    }
    if (!description.trim()) {
      setError('Please enter a detailed incident description.');
      return;
    }

    setError(null);
    setSubmitting(true);
    let created: IncidentReport | null = null;

    try {
      // Step 1: create the report and receive its id.
      created = await incidentApi.create({
        horseId: Number(horseId),
        title: title.trim(),
        description: description.trim(),
        severity,
      });

      // Step 2: attach the image when one was selected.
      if (file) {
        await incidentApi.uploadImage(created.id, file);
      }

      router.push('/groom/incidents');
    } catch (err) {
      console.error('Unable to submit incident report:', err);
      setError(
        created
          ? `Report #${created.id} was submitted, but the image upload failed. Open the report to try again.`
          : (err instanceof Error ? err.message : 'Failed to submit the incident report.')
      );
    } finally {
      setSubmitting(false);
    }
  }

  const stallMap = new Map<number, string>();
  for (const s of stalls) {
    stallMap.set(s.id, s.stallCode || `Stall #${s.stallNumber}`);
  }

  if (loadingInitial) {
    return (
      <Panel padded>
        <div className="flex items-center justify-center py-12 text-sm text-[var(--color-text-secondary)]">
          Loading stall and horse information...
        </div>
      </Panel>
    );
  }

  return (
    <div className="mx-auto w-full max-w-7xl space-y-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-xl font-semibold text-[var(--color-text-primary)] md:text-2xl">
            Incident Report
          </h1>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            Report unusual symptoms or injuries to the veterinary team.
          </p>
        </div>
        <Link
          href="/groom/incidents"
          className="inline-flex items-center gap-1.5 text-xs text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
        >
          <Icon name="arrow-left" size={14} /> Back to reports
        </Link>
      </div>

      {error && (
        <div className="rounded-[var(--radius-md)] border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {horses.length === 0 ? (
        <Panel padded>
          <div className="py-8 text-center text-sm text-[var(--color-text-secondary)]">
            You are not currently assigned to any stalls or horses.
            <div className="mt-4">
              <Link href="/groom/incidents">
                <Button variant="secondary">Back</Button>
              </Link>
            </div>
          </div>
        </Panel>
      ) : (
        <Panel padded>
          <form onSubmit={handleSubmit} className="grid gap-6 lg:grid-cols-2">
            <div className="space-y-5">
              {/* Horse selection */}
              <div>
                <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
                  Horse involved in the incident <span className="text-red-500">*</span>
                </label>
                <select
                  value={horseId}
                  onChange={(e) => setHorseId(Number(e.target.value))}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-3 py-2 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-primary)] focus:outline-none"
                  required
                >
                  {horses.map((h) => {
                    const stallLabel = h.currentStallId
                      ? stallMap.get(h.currentStallId) || `Stall #${h.currentStallId}`
                      : 'No stall assigned';
                    return (
                      <option key={h.id} value={h.id}>
                        {h.name} (#{h.id}) — {stallLabel}
                      </option>
                    );
                  })}
                </select>
                <p className="mt-1 text-[11px] text-[var(--color-text-muted)]">
                  Only horses in your assigned stalls are shown.
                </p>
              </div>

              {/* Severity */}
              <div>
                <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
                  Severity <span className="text-red-500">*</span>
                </label>
                <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
                  {SEVERITY_OPTIONS.map((opt) => (
                    <label
                      key={opt.value}
                      className={`flex cursor-pointer items-start gap-2.5 rounded-[var(--radius-md)] border p-2.5 transition-colors ${
                        severity === opt.value
                          ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]'
                          : 'border-[var(--color-border)] bg-[var(--color-bg-primary)] hover:border-gray-300'
                      }`}
                    >
                      <input
                        type="radio"
                        name="severity"
                        value={opt.value}
                        checked={severity === opt.value}
                        onChange={() => setSeverity(opt.value)}
                        className="mt-0.5 text-[var(--color-primary)]"
                      />
                      <div>
                        <div className={`text-xs ${opt.tone}`}>{opt.label}</div>
                        <div className="text-[11px] text-[var(--color-text-muted)]">{opt.hint}</div>
                      </div>
                    </label>
                  ))}
                </div>

                {severity === 'CRITICAL' && (
                  <div className="mt-2.5 rounded-[var(--radius-md)] border border-red-300 bg-red-50 p-2.5 text-xs text-red-800">
                    <div className="font-semibold">Emergency warning:</div>
                    The system does not send instant notifications. For a <strong>Critical</strong> incident,{' '}
                    <strong>call the veterinary team directly</strong> or notify a manager immediately after submitting this report.
                  </div>
                )}
              </div>
            </div>

            <div className="space-y-5">
              {/* Title */}
              <div>
                <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
                  Incident title <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="Example: Swollen right front knee, missed breakfast..."
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-3 py-2 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-primary)] focus:outline-none"
                  required
                />
              </div>

              {/* Detailed description */}
              <div>
                <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
                  Detailed description <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={8}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Describe the symptoms, time discovered, horse behavior, and pain or injury location..."
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-3 py-2 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-primary)] focus:outline-none"
                  required
                />
              </div>

              {/* Optional image attachment */}
              <div>
                <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
                  Scene or injury image (optional)
                </label>
                <div className="mt-1.5 flex flex-col gap-2">
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    onChange={handleFileChange}
                    className="text-xs text-[var(--color-text-secondary)] file:mr-3 file:rounded-[var(--radius-sm)] file:border-0 file:bg-[var(--color-bg-secondary)] file:px-3 file:py-1.5 file:text-xs file:font-medium file:text-[var(--color-text-primary)] hover:file:bg-[var(--color-border)]"
                  />
                  <p className="text-[11px] text-[var(--color-text-muted)]">
                    JPEG, PNG, and WebP only. Maximum size: 10 MB.
                  </p>

                  {previewUrl && (
                    <div className="relative mt-2 inline-block max-w-xs">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={previewUrl}
                        alt="Image preview"
                        className="max-h-48 rounded-[var(--radius-md)] border border-[var(--color-border)] object-cover"
                      />
                      <button
                        type="button"
                        onClick={handleRemoveFile}
                        className="absolute right-2 top-2 rounded-full bg-black/60 p-1 text-white hover:bg-black/80"
                        title="Remove image"
                      >
                        <Icon name="x" size={14} />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Form actions */}
            <div className="flex items-center justify-end gap-3 border-t border-[var(--color-border-subtle)] pt-5 lg:col-span-2">
              <Link href="/groom/incidents">
                <Button type="button" variant="secondary" disabled={submitting}>
                  Cancel
                </Button>
              </Link>
              <Button type="submit" variant="primary" disabled={submitting}>
                {submitting ? 'Submitting report...' : 'Submit incident report'}
              </Button>
            </div>
          </form>
        </Panel>
      )}
    </div>
  );
}
