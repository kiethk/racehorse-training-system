'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainingApi } from '../services/trainingService';
import type { Subject, SubjectCategory, CreateSubjectRequest, SurfaceType, IntensityLevel } from '../types';

export function SubjectList() {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [categories, setCategories] = useState<SubjectCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [showForm, setShowForm] = useState(false);
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [surfaceType, setSurfaceType] = useState<SurfaceType>('TURF');
  const [targetDistanceMeters, setTargetDistanceMeters] = useState<number>(1000);
  const [intensityLevel, setIntensityLevel] = useState<IntensityLevel>('MEDIUM');
  const [durationMinutes, setDurationMinutes] = useState<number>(60);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [subs, cats] = await Promise.all([
        trainingApi.getSubjects(),
        trainingApi.getCategories(),
      ]);
      setSubjects(subs);
      setCategories(cats);
      if (cats.length > 0 && categoryId === '') {
        setCategoryId(cats[0].id);
      }
    } catch (err) {
      console.error('Failed to load exercises:', err);
      setError('Unable to load the exercise library.');
    } finally {
      setLoading(false);
    }
  }, [categoryId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim() || !categoryId) {
      setFormError('Enter an exercise name and select a category.');
      return;
    }
    // Block durations over 240 minutes (golden-hour window 06:00 - 10:00)
    if (durationMinutes <= 0 || durationMinutes > 240) {
      setFormError('Duration must be between 1 and 240 minutes.');
      return;
    }

    setFormError(null);
    setSubmitting(true);
    try {
      const payload: CreateSubjectRequest = {
        categoryId: Number(categoryId),
        name: name.trim(),
        description: description.trim() || undefined,
        surfaceType,
        targetDistanceMeters: Number(targetDistanceMeters),
        intensityLevel,
        durationMinutes: Number(durationMinutes),
      };
      await trainingApi.createSubject(payload);
      setName('');
      setDescription('');
      setShowForm(false);
      await loadData();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Unable to create the exercise.');
    } finally {
      setSubmitting(false);
    }
  }

  const categoryMap = new Map(categories.map((c) => [c.id, c.name]));

  if (loading) return <ListSkeleton rows={4} />;

  if (error) {
    return (
      <Panel padded>
      <EmptyState icon="alert-triangle" title="Unable to load data" description={error} />
      </Panel>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-[16px] font-semibold text-[var(--color-text-primary)]">
            Exercise library ({subjects.length})
          </h2>
          <p className="text-[12px] text-[var(--color-text-secondary)]">
            Standard exercises for building training courses.
          </p>
        </div>
        <Button variant="primary" size="sm" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Close form' : 'Create exercise'}
        </Button>
      </div>

      {showForm && (
        <Panel padded>
          <form onSubmit={handleCreate} className="space-y-4">
            <h3 className="text-[14px] font-semibold text-[var(--color-text-primary)]">
              Create exercise
            </h3>

            {formError && (
              <div className="rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
                {formError}
              </div>
            )}

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Exercise name *
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Gallop 1,200 m"
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                />
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Category *
                </label>
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                >
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Track surface
                </label>
                <select
                  value={surfaceType}
                  onChange={(e) => setSurfaceType(e.target.value as SurfaceType)}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                >
                  <option value="TURF">Turf (TURF)</option>
                  <option value="DIRT">Dirt (DIRT)</option>
                  <option value="SYNTHETIC">Synthetic (SYNTHETIC)</option>
                </select>
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Intensity
                </label>
                <select
                  value={intensityLevel}
                  onChange={(e) => setIntensityLevel(e.target.value as IntensityLevel)}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                >
                  <option value="LOW">Low (LOW)</option>
                  <option value="MEDIUM">Medium (MEDIUM)</option>
                  <option value="HIGH">High (HIGH)</option>
                </select>
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Target distance (metres)
                </label>
                <input
                  type="number"
                  min="100"
                  step="50"
                  value={targetDistanceMeters}
                  onChange={(e) => setTargetDistanceMeters(Number(e.target.value))}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                />
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Estimated duration (minutes, max 240) *
                </label>
                <input
                  type="number"
                  min="15"
                  max="240"
                  step="5"
                  required
                  value={durationMinutes}
                  onChange={(e) => setDurationMinutes(Number(e.target.value))}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                />
                <span className="text-[11px] text-[var(--color-text-muted)]">
                Determines lot duration. The training window is limited to 240 minutes.
                </span>
              </div>
            </div>


            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Description
              </label>
              <textarea
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Technical notes or instructions for the groom…"
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button variant="secondary" size="sm" type="button" onClick={() => setShowForm(false)}>
                Cancel
              </Button>
              <Button variant="primary" size="sm" type="submit" disabled={submitting}>
                {submitting ? 'Creating…' : 'Save exercise'}
              </Button>
            </div>
          </form>
        </Panel>
      )}

      {subjects.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="clipboard"
            title="No exercises yet"
            description="Create your first exercise to start building a training course."
          />
        </Panel>
      ) : (
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
          {subjects.map((sub) => (
            <Panel key={sub.id} padded>
              <div className="flex items-start justify-between gap-2">
                <div>
                  <h4 className="text-[14px] font-semibold text-[var(--color-text-primary)]">
                    {sub.name}
                  </h4>
                  <div className="text-[11px] text-[var(--color-text-muted)]">
                    {categoryMap.get(sub.categoryId) || `Category #${sub.categoryId}`}
                  </div>
                </div>
                <Pill
                  tone={
                    sub.intensityLevel === 'HIGH'
                      ? 'danger'
                      : sub.intensityLevel === 'MEDIUM'
                      ? 'warning'
                      : 'info'
                  }
                  size="sm"
                >
                  {sub.intensityLevel}
                </Pill>
              </div>

              {sub.description && (
                <p className="mt-2 text-[12px] text-[var(--color-text-secondary)] line-clamp-2">
                  {sub.description}
                </p>
              )}

              <div className="mt-3 flex flex-wrap gap-2 text-[11px] text-[var(--color-text-muted)] border-t border-[var(--color-border)] pt-2">
                <span>{sub.durationMinutes} min</span>
                <span>📏 {sub.targetDistanceMeters}m</span>
                <span>🏟️ {sub.surfaceType}</span>
              </div>
            </Panel>
          ))}
        </div>
      )}
    </div>
  );
}
