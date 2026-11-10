'use client';

import { useMemo, useState } from 'react';
import {
  Button,
  FieldLabel,
  FormField,
  Icon,
  IconButton,
  Input,
  Notice,
  Panel,
  Pill,
  SearchInput,
  SectionTitle,
} from '@/components/ui';
import { cn } from '@/lib/cn';
import { CATEGORY_OPTIONS, intensityOption, surfaceLabel } from '../lib/exerciseOptions';
import type { CreateCourseRequest, Exercise, ExerciseCategory } from '../types';
import { useTrainingRules } from '../hooks/useTrainingRules';
import { exerciseDragDrop } from './exerciseDragDrop';

const DEFAULT_SESSIONS = 12;

interface CourseFormProps {
  exercises: Exercise[];
  submitting: boolean;
  error: string | null;
  onSubmit: (request: CreateCourseRequest) => void;
  onCancel: () => void;
}

/**
 * Form that builds a course out of an ordered rotation of exercises.
 * The exercise library is presented as a separate side panel (mirroring StableStaffPanel)
 * where exercises are grouped by category and can be dragged directly into the rotation.
 */
export function CourseForm(props: CourseFormProps) {
  return (
    <exerciseDragDrop.Provider>
      <CourseFormContent {...props} />
    </exerciseDragDrop.Provider>
  );
}

function CourseFormContent({
  exercises,
  submitting,
  error,
  onSubmit,
  onCancel,
}: CourseFormProps) {
  const rules = useTrainingRules();
  const [name, setName] = useState('');
  const [targetGoal, setTargetGoal] = useState('');
  const [totalSessions, setTotalSessions] = useState(DEFAULT_SESSIONS);
  const [rotation, setRotation] = useState<number[]>([]);
  const [search, setSearch] = useState('');
  const [validation, setValidation] = useState<string | null>(null);

  const exerciseById = useMemo(
    () => new Map(exercises.map((exercise) => [exercise.id, exercise])),
    [exercises],
  );

  // Group exercises by category
  const groupedExercises = useMemo(() => {
    const groups = new Map<ExerciseCategory, Exercise[]>();
    CATEGORY_OPTIONS.forEach((opt) => groups.set(opt.value, []));
    exercises.forEach((sub) => {
      const list = groups.get(sub.category);
      if (list) list.push(sub);
      else groups.set(sub.category, [sub]);
    });
    return groups;
  }, [exercises]);

  const { dropProps, isOver } = exerciseDragDrop.useDropZone({
    disabled: submitting,
    accept: (exercise) => !rotation.includes(exercise.id),
    onDrop: (exercise) => {
      setRotation((current) => [...current, exercise.id]);
    },
  });

  const sessionsError =
    !Number.isInteger(totalSessions) || totalSessions < 1 || (rules && totalSessions > rules.maxCourseSessions)
      ? `Enter a whole number from 1 to ${rules?.maxCourseSessions ?? 60}.`
      : null;

  // One exercise per session, so the rotation cannot be longer than the course.
  const tooManyExercises = !sessionsError && rotation.length > totalSessions
    ? `A course cannot have more exercises (${rotation.length}) than sessions (${totalSessions}). Remove exercises or add sessions.`
    : null;

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!name.trim() || sessionsError) {
      setValidation('Enter a course name and a valid number of sessions.');
      return;
    }
    if (rules && rotation.length > rules.maxCourseExercises) {
      setValidation(`A course can have at most ${rules.maxCourseExercises} exercises.`);
      return;
    }
    if (rotation.length === 0) {
      setValidation('Select at least one exercise for the course rotation.');
      return;
    }
    if (tooManyExercises) {
      setValidation(tooManyExercises);
      return;
    }
    setValidation(null);
    onSubmit({
      name: name.trim(),
      targetGoal: targetGoal.trim() || undefined,
      totalSessions: Number(totalSessions),
      exercises: rotation.map((exerciseId, index) => ({ exerciseId, orderIndex: index + 1 })),
    });
  }

  const message = validation ?? error;

  const filteredCount = useMemo(() => {
    return exercises.filter((s) => s.name.toLowerCase().includes(search.trim().toLowerCase()))
      .length;
  }, [exercises, search]);

  return (
    <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_18rem]">
      {/* Left Column: Course Form Panel */}
      <Panel padded className="min-w-0">
        <form onSubmit={handleSubmit} className="space-y-5">
          <SectionTitle>Create course with exercise rotation</SectionTitle>
          {message && <Notice tone="error">{message}</Notice>}

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <FormField label="Course name" required>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Sprint 2YO Foundation"
                maxLength={255}
                disabled={submitting}
              />
            </FormField>
            <FormField label="Total sessions" required error={sessionsError ?? undefined}>
              <Input
                type="number"
                min={1}
                max={rules?.maxCourseSessions}
                value={totalSessions}
                onChange={(e) => setTotalSessions(Number(e.target.value))}
                disabled={submitting}
              />
            </FormField>
          </div>

          <FormField label="Course goal">
            <Input
              value={targetGoal}
              onChange={(e) => setTargetGoal(e.target.value)}
              placeholder="e.g. Prepare for short-distance races"
              maxLength={1000}
              disabled={submitting}
            />
          </FormField>

          {/* Course Exercise Rotation (Drop Zone) */}
          <div className="space-y-3 border-t border-[var(--color-border)] pt-4">
            <div className="flex items-center justify-between gap-2">
              <FieldLabel>Exercise rotation ({rotation.length} selected)</FieldLabel>
              {rotation.length > 0 && (
                <Button
                  type="button"
                  size="sm"
                  variant="tertiary"
                  onClick={() => setRotation([])}
                  disabled={submitting}
                  className="h-auto p-0 text-xs text-[var(--color-text-muted)] hover:text-[var(--color-danger)]"
                >
                  Clear all
                </Button>
              )}
            </div>

            {tooManyExercises && <p className="text-xs text-[var(--color-danger)]">{tooManyExercises}</p>}

            <div
              {...dropProps}
              className={cn(
                'min-h-52 rounded-[var(--radius-lg)] border-2 border-dashed p-3 transition-colors',
                isOver
                  ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]/30 ring-2 ring-[var(--color-primary)]'
                  : 'border-[var(--color-border-strong)] bg-[var(--color-surface-muted)]',
              )}
            >
              {rotation.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-10 text-center">
                  <Icon name="clipboard" size={28} className="text-[var(--color-text-muted)]" />
                  <p className="mt-2 text-sm font-medium text-[var(--color-text-secondary)]">
                    Drop exercises here
                  </p>
                  <p className="mt-1 max-w-sm text-xs text-[var(--color-text-muted)]">
                    Drag an exercise from the library on the right, or click any exercise card to add it to the rotation.
                  </p>
                </div>
              ) : (
                <ol className="space-y-2">
                  {rotation.map((exerciseId, index) => {
                    const exercise = exerciseById.get(exerciseId);
                    const intensity = exercise ? intensityOption(exercise.intensityLevel) : null;
                    return (
                      <li
                        key={`${exerciseId}-${index}`}
                        className="flex items-center justify-between gap-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 shadow-sm transition-shadow hover:shadow"
                      >
                        <div className="flex min-w-0 items-center gap-2.5">
                          <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-xs font-bold text-[var(--color-primary)]">
                            {index + 1}
                          </span>
                          <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-[var(--color-text-primary)]">
                              {exercise?.name ?? `Exercise #${exerciseId}`}
                            </p>
                            <div className="mt-0.5 flex flex-wrap items-center gap-1.5 text-xs text-[var(--color-text-secondary)]">
                              {exercise && <span>{exercise.durationMinutes} min</span>}
                              {exercise && <span>• {exercise.targetDistanceMeters}m</span>}
                              {intensity && (
                                <Pill tone={intensity.tone} size="sm">
                                  {intensity.label}
                                </Pill>
                              )}
                            </div>
                          </div>
                        </div>
                        <div className="flex shrink-0 items-center">
                          <IconButton
                            icon="x"
                            size="sm"
                            label={`Remove exercise ${index + 1} from rotation`}
                            disabled={submitting}
                            onClick={() =>
                              setRotation((curr) => curr.filter((_, i) => i !== index))
                            }
                          />
                        </div>
                      </li>
                    );
                  })}
                </ol>
              )}
            </div>
          </div>

          <div className="flex justify-end gap-2 border-t border-[var(--color-border)] pt-4">
            <Button type="button" onClick={onCancel} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={submitting}>
              Save course
            </Button>
          </div>
        </form>
      </Panel>

      {/* Right Column: Separated Exercise Library Side Panel (styled like StableStaffPanel) */}
      <aside
        className="min-w-0 lg:sticky lg:top-4 lg:self-start"
        aria-label="Exercise library"
      >
        <Panel className="overflow-hidden">
          <div className="space-y-3 border-b border-[var(--color-border)] p-4">
            <div className="flex items-center justify-between gap-2">
              <div>
                <h2 className="text-base font-semibold text-[var(--color-text-primary)]">
                  Exercise library
                </h2>
                <p className="text-xs text-[var(--color-text-secondary)]">
                  {exercises.length} {exercises.length === 1 ? 'exercise' : 'exercises'} available
                </p>
              </div>
            </div>
            <SearchInput
              value={search}
              onChange={setSearch}
              label="Search exercises"
              placeholder="Search exercises..."
            />
          </div>

          <div className="scroll-slim max-h-[calc(100vh-16rem)] space-y-4 overflow-y-auto p-3">
            {CATEGORY_OPTIONS.map((category) => {
              const categoryExercises = (groupedExercises.get(category.value) ?? []).filter((s) =>
                s.name.toLowerCase().includes(search.trim().toLowerCase()),
              );
              if (categoryExercises.length === 0) return null;

              return (
                <div key={category.value} className="space-y-2">
                  <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-1">
                    <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
                      {category.label}
                    </span>
                    <span className="text-xs text-[var(--color-text-muted)]">
                      {categoryExercises.length}
                    </span>
                  </div>

                  <div className="space-y-2">
                    {categoryExercises.map((exercise) => {
                      const isAdded = rotation.includes(exercise.id);
                      return (
                        <DraggableExerciseCard
                          key={exercise.id}
                          exercise={exercise}
                          isAdded={isAdded}
                          disabled={submitting}
                          onAdd={() => setRotation((curr) => [...curr, exercise.id])}
                        />
                      );
                    })}
                  </div>
                </div>
              );
            })}

            {filteredCount === 0 && (
              <p className="py-6 text-center text-xs text-[var(--color-text-muted)]">
                No exercises match &ldquo;{search}&rdquo;
              </p>
            )}
          </div>
        </Panel>
      </aside>
    </div>
  );
}

function DraggableExerciseCard({
  exercise,
  isAdded,
  disabled,
  onAdd,
}: {
  exercise: Exercise;
  isAdded: boolean;
  disabled: boolean;
  onAdd: () => void;
}) {
  const { dragProps, pickUp, isDragging } = exerciseDragDrop.useDraggable({
    item: exercise,
    label: exercise.name,
    disabled: disabled || isAdded,
  });

  const intensity = intensityOption(exercise.intensityLevel);

  return (
    <div
      {...dragProps}
      onClick={isAdded || disabled ? undefined : onAdd}
      className={cn(
        'group relative flex flex-col justify-between rounded-[var(--radius-md)] border p-2.5 transition-all select-none',
        isDragging && 'opacity-50 ring-2 ring-[var(--color-primary)]',
        isAdded
          ? 'border-[var(--color-border)] bg-[var(--color-surface-subtle)] opacity-60'
          : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:border-[var(--color-border-strong)] hover:shadow-sm cursor-grab active:cursor-grabbing',
      )}
    >
      <div className="flex items-start justify-between gap-1.5">
        <span className="text-xs font-semibold text-[var(--color-text-primary)] line-clamp-1">
          {exercise.name}
        </span>
        {isAdded ? (
          <Pill tone="neutral" size="sm">
            In course
          </Pill>
        ) : (
          <IconButton
            icon="plus"
            size="sm"
            label={`Add ${exercise.name} to course`}
            disabled={disabled}
            onClick={(e) => {
              e.stopPropagation();
              onAdd();
            }}
          />
        )}
      </div>

      <div className="mt-1.5 flex flex-wrap items-center gap-1 text-[11px] text-[var(--color-text-secondary)]">
        <span>{exercise.durationMinutes} min</span>
        <span>• {exercise.targetDistanceMeters}m</span>
        <span>• {surfaceLabel(exercise.surfaceType)}</span>
        <Pill tone={intensity.tone} size="sm">
          {intensity.label}
        </Pill>
      </div>
    </div>
  );
}
