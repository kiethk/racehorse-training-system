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
  Textarea,
} from '@/components/ui';
import { cn } from '@/lib/cn';
import { CATEGORY_OPTIONS, intensityOption, surfaceLabel } from '../lib/subjectOptions';
import type { CreateCourseRequest, Subject, SubjectCategory } from '../types';
import { subjectDragDrop } from './subjectDragDrop';

const DEFAULT_SESSIONS = 12;

interface CourseFormProps {
  subjects: Subject[];
  submitting: boolean;
  error: string | null;
  onSubmit: (request: CreateCourseRequest) => void;
  onCancel: () => void;
}

/**
 * Form that builds a course out of an ordered rotation of subjects.
 * The subject library is presented as a separate side panel (mirroring StableStaffPanel)
 * where subjects are grouped by category and can be dragged directly into the rotation.
 */
export function CourseForm(props: CourseFormProps) {
  return (
    <subjectDragDrop.Provider>
      <CourseFormContent {...props} />
    </subjectDragDrop.Provider>
  );
}

function CourseFormContent({
  subjects,
  submitting,
  error,
  onSubmit,
  onCancel,
}: CourseFormProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [targetGoal, setTargetGoal] = useState('');
  const [totalSessions, setTotalSessions] = useState(DEFAULT_SESSIONS);
  const [rotation, setRotation] = useState<number[]>([]);
  const [search, setSearch] = useState('');
  const [validation, setValidation] = useState<string | null>(null);

  const subjectById = useMemo(
    () => new Map(subjects.map((subject) => [subject.id, subject])),
    [subjects],
  );

  // Group subjects by category
  const groupedSubjects = useMemo(() => {
    const groups = new Map<SubjectCategory, Subject[]>();
    CATEGORY_OPTIONS.forEach((opt) => groups.set(opt.value, []));
    subjects.forEach((sub) => {
      const list = groups.get(sub.category);
      if (list) list.push(sub);
      else groups.set(sub.category, [sub]);
    });
    return groups;
  }, [subjects]);

  const { dropProps, isOver } = subjectDragDrop.useDropZone({
    disabled: submitting,
    accept: (subject) => !rotation.includes(subject.id),
    onDrop: (subject) => {
      setRotation((current) => [...current, subject.id]);
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!name.trim() || totalSessions <= 0) {
      setValidation('Enter a course name and a valid number of sessions.');
      return;
    }
    if (rotation.length === 0) {
      setValidation('Select at least one subject for the course rotation.');
      return;
    }
    setValidation(null);
    onSubmit({
      name: name.trim(),
      description: description.trim() || undefined,
      targetGoal: targetGoal.trim() || undefined,
      totalSessions: Number(totalSessions),
      subjects: rotation.map((subjectId, index) => ({ subjectId, orderIndex: index + 1 })),
    });
  }

  const message = validation ?? error;

  const filteredCount = useMemo(() => {
    return subjects.filter((s) => s.name.toLowerCase().includes(search.trim().toLowerCase()))
      .length;
  }, [subjects, search]);

  return (
    <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_18rem]">
      {/* Left Column: Course Form Panel */}
      <Panel padded className="min-w-0">
        <form onSubmit={handleSubmit} className="space-y-5">
          <SectionTitle>Create course with subject rotation</SectionTitle>
          {message && <Notice tone="error">{message}</Notice>}

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <FormField label="Course name" required>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Sprint 2YO Foundation"
                disabled={submitting}
              />
            </FormField>
            <FormField
              label="Total sessions"
              required
              hint="Subjects repeat in order until the total number of sessions is reached."
            >
              <Input
                type="number"
                min={1}
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
              disabled={submitting}
            />
          </FormField>

          <FormField label="Course description">
            <Textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe the training path and suitable fitness level…"
              disabled={submitting}
            />
          </FormField>

          {/* Course Subject Rotation (Drop Zone) */}
          <div className="space-y-3 border-t border-[var(--color-border)] pt-4">
            <div className="flex items-center justify-between gap-2">
              <div>
                <FieldLabel>Subject rotation ({rotation.length} selected)</FieldLabel>
                <p className="text-xs text-[var(--color-text-secondary)]">
                  Drag subjects from the library on the right into this rotation, or select a subject and click &ldquo;Add selected subject&rdquo;.
                </p>
              </div>
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
                    Drop subjects here
                  </p>
                  <p className="mt-1 max-w-sm text-xs text-[var(--color-text-muted)]">
                    Drag a subject from the library on the right, or click any subject card to add it to the rotation.
                  </p>
                </div>
              ) : (
                <ol className="space-y-2">
                  {rotation.map((subjectId, index) => {
                    const subject = subjectById.get(subjectId);
                    const intensity = subject ? intensityOption(subject.intensityLevel) : null;
                    return (
                      <li
                        key={`${subjectId}-${index}`}
                        className="flex items-center justify-between gap-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 shadow-sm transition-shadow hover:shadow"
                      >
                        <div className="flex min-w-0 items-center gap-2.5">
                          <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-xs font-bold text-[var(--color-primary)]">
                            {index + 1}
                          </span>
                          <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-[var(--color-text-primary)]">
                              {subject?.name ?? `Subject #${subjectId}`}
                            </p>
                            <div className="mt-0.5 flex flex-wrap items-center gap-1.5 text-xs text-[var(--color-text-secondary)]">
                              {subject && <span>{subject.durationMinutes} min</span>}
                              {subject && <span>• {subject.targetDistanceMeters}m</span>}
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
                            label={`Remove subject ${index + 1} from rotation`}
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

      {/* Right Column: Separated Subject Library Side Panel (styled like StableStaffPanel) */}
      <aside
        className="min-w-0 lg:sticky lg:top-4 lg:self-start"
        aria-label="Subject library"
      >
        <Panel className="overflow-hidden">
          <div className="space-y-3 border-b border-[var(--color-border)] p-4">
            <div className="flex items-center justify-between gap-2">
              <div>
                <h2 className="text-base font-semibold text-[var(--color-text-primary)]">
                  Subject library
                </h2>
                <p className="text-xs text-[var(--color-text-secondary)]">
                  {subjects.length} {subjects.length === 1 ? 'subject' : 'subjects'} available
                </p>
              </div>
            </div>
            <SearchInput
              value={search}
              onChange={setSearch}
              label="Search subjects"
              placeholder="Search subjects..."
            />
          </div>

          <div className="scroll-slim max-h-[calc(100vh-16rem)] space-y-4 overflow-y-auto p-3">
            {CATEGORY_OPTIONS.map((category) => {
              const categorySubjects = (groupedSubjects.get(category.value) ?? []).filter((s) =>
                s.name.toLowerCase().includes(search.trim().toLowerCase()),
              );
              if (categorySubjects.length === 0) return null;

              return (
                <div key={category.value} className="space-y-2">
                  <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-1">
                    <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
                      {category.label}
                    </span>
                    <span className="text-xs text-[var(--color-text-muted)]">
                      {categorySubjects.length}
                    </span>
                  </div>

                  <div className="space-y-2">
                    {categorySubjects.map((subject) => {
                      const isAdded = rotation.includes(subject.id);
                      return (
                        <DraggableSubjectCard
                          key={subject.id}
                          subject={subject}
                          isAdded={isAdded}
                          disabled={submitting}
                          onAdd={() => setRotation((curr) => [...curr, subject.id])}
                        />
                      );
                    })}
                  </div>
                </div>
              );
            })}

            {filteredCount === 0 && (
              <p className="py-6 text-center text-xs text-[var(--color-text-muted)]">
                No subjects match &ldquo;{search}&rdquo;
              </p>
            )}
          </div>
        </Panel>
      </aside>
    </div>
  );
}

function DraggableSubjectCard({
  subject,
  isAdded,
  disabled,
  onAdd,
}: {
  subject: Subject;
  isAdded: boolean;
  disabled: boolean;
  onAdd: () => void;
}) {
  const { dragProps, pickUp, isDragging } = subjectDragDrop.useDraggable({
    item: subject,
    label: subject.name,
    disabled: disabled || isAdded,
  });

  const intensity = intensityOption(subject.intensityLevel);

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
          {subject.name}
        </span>
        {isAdded ? (
          <Pill tone="neutral" size="sm">
            In course
          </Pill>
        ) : (
          <IconButton
            icon="plus"
            size="sm"
            label={`Add ${subject.name} to course`}
            disabled={disabled}
            onClick={(e) => {
              e.stopPropagation();
              onAdd();
            }}
          />
        )}
      </div>

      <div className="mt-1.5 flex flex-wrap items-center gap-1 text-[11px] text-[var(--color-text-secondary)]">
        <span>{subject.durationMinutes} min</span>
        <span>• {subject.targetDistanceMeters}m</span>
        <span>• {surfaceLabel(subject.surfaceType)}</span>
        <Pill tone={intensity.tone} size="sm">
          {intensity.label}
        </Pill>
      </div>
    </div>
  );
}
