'use client';

import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { Input, Textarea } from '@/components/ui/Input';
import { FormField } from '@/components/ui/FormField';
import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { Button, IconButton, LinkButton } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainingApi } from '../services/trainingService';
import type { Course, Subject, CreateCourseRequest, CourseDetailResponse } from '../types';

export function CourseList() {
  const [courses, setCourses] = useState<Course[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Detail modal
  const [viewDetail, setViewDetail] = useState<CourseDetailResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // Form create course
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [targetGoal, setTargetGoal] = useState('');
  const [totalSessions, setTotalSessions] = useState<number>(12);
  const [selectedSubjectIds, setSelectedSubjectIds] = useState<number[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [crs, subs] = await Promise.all([
        trainingApi.getCourses(),
        trainingApi.getSubjects(),
      ]);
      setCourses(crs);
      setSubjects(subs);
    } catch (err) {
      console.error('Failed to load training courses:', err);
      setError('Unable to load training courses.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  async function handleOpenDetail(courseId: number) {
    try {
      setDetailLoading(true);
      const detail = await trainingApi.getCourse(courseId);
      setViewDetail(detail);
    } catch (err) {
      console.error('Failed to load course details:', err);
    } finally {
      setDetailLoading(false);
    }
  }

  function addSubjectToCourseSequence(subId: number) {
    setSelectedSubjectIds((prev) => [...prev, subId]);
  }

  function removeSubjectFromSequence(index: number) {
    setSelectedSubjectIds((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleCreateCourse(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim() || totalSessions <= 0) {
      setFormError('Enter a course name and a valid number of sessions.');
      return;
    }
    if (selectedSubjectIds.length === 0) {
      setFormError('Select at least one exercise for the course rotation.');
      return;
    }

    setFormError(null);
    setSubmitting(true);
    try {
      const payload: CreateCourseRequest = {
        name: name.trim(),
        description: description.trim() || undefined,
        targetGoal: targetGoal.trim() || undefined,
        totalSessions: Number(totalSessions),
        subjects: selectedSubjectIds.map((subId, idx) => ({
          subjectId: subId,
          orderIndex: idx + 1,
        })),
      };
      await trainingApi.createCourse(payload);
      setName('');
      setDescription('');
      setTargetGoal('');
      setTotalSessions(12);
      setSelectedSubjectIds([]);
      setShowForm(false);
      await loadData();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Unable to create the course.');
    } finally {
      setSubmitting(false);
    }
  }

  const subjectMap = new Map(subjects.map((s) => [s.id, s]));

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
          <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">
            Training courses ({courses.length})
          </h2>
          <p className="text-xs text-[var(--color-text-secondary)]">
            Create complete training courses with rotating exercises for horse enrollment.
          </p>
        </div>
        <div className="flex gap-2">
          <Link href="/trainer/plans/new">
            <Button variant="secondary" size="sm">
            Enroll horses
            </Button>
          </Link>
          <Button variant="primary" size="sm" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Close form' : 'Create course'}
          </Button>
        </div>
      </div>

      {showForm && (
        <Panel padded>
          <form onSubmit={handleCreateCourse} className="space-y-4">
            <h3 className="text-base font-semibold text-[var(--color-text-primary)]">
              Create course with exercise rotation
            </h3>

            {formError && (
              <Notice tone="error">{formError}</Notice>
            )}

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <FormField label="Course name" required>
                  <Input
                    type="text"
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="e.g. Sprint 2YO Foundation"
                  />
                </FormField>
              </div>

              <div>
                <FormField label="Total sessions" required>
                  <Input
                    type="number"
                    min="1"
                    required
                    value={totalSessions}
                    onChange={(e) => setTotalSessions(Number(e.target.value))}
                  />
                </FormField>
                <span className="text-xs text-[var(--color-text-muted)]">
                  Exercises repeat in order until the total number of sessions is reached.
                </span>
              </div>
            </div>

            <div>
              <FormField label="Course goal">
                <Input
                  type="text"
                  value={targetGoal}
                  onChange={(e) => setTargetGoal(e.target.value)}
                    placeholder="e.g. Prepare for short-distance races up to 1,200 m"
                />
              </FormField>
            </div>

            <div>
              <FormField label="Course description">
                <Textarea
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                    placeholder="Describe the training path and suitable fitness level…"
                />
              </FormField>
            </div>

            {/* Pick the subject rotation */}
            <div className="border-t border-[var(--color-border)] pt-3">
              <label className="block text-xs font-medium text-[var(--color-text-primary)]">
                  Exercise rotation ({selectedSubjectIds.length} selected)
              </label>
              <p className="text-xs text-[var(--color-text-muted)]">
                  Select exercises below to add them to the rotation in order:
              </p>

              {/* Selected subjects */}
              <div className="mt-2 min-h-12 rounded-[var(--radius-md)] border border-dashed border-[var(--color-border-strong)] bg-[var(--color-surface-muted)] p-2">
                {selectedSubjectIds.length === 0 ? (
                  <span className="text-xs text-[var(--color-text-muted)] italic">
                  No exercises have been added to the rotation.
                  </span>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {selectedSubjectIds.map((subId, idx) => {
                      const s = subjectMap.get(subId);
                      return (
                        <div
                          key={`${subId}-${idx}`}
                          className="flex items-center gap-1.5 rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1 text-xs"
                        >
                          <span className="font-bold text-[var(--color-primary)]">#{idx + 1}</span>
                          <span>{s?.name || `Subject #${subId}`}</span>
                          <IconButton icon="x" size="sm" label="Remove subject" className="-mr-1.5 h-5 w-5" onClick={() => removeSubjectFromSequence(idx)} />
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Available subjects to pick from */}
              <div className="mt-3 flex flex-wrap gap-1.5">
                {subjects.map((sub) => (
                  <Button
                    key={sub.id}
                    size="sm"
                    variant="secondary"
                    icon="plus"
                    onClick={() => addSubjectToCourseSequence(sub.id)}
                  >
                    {sub.name} ({sub.durationMinutes}&apos;)
                  </Button>
                ))}
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-[var(--color-border)]">
              <Button variant="secondary" size="sm" type="button" onClick={() => setShowForm(false)}>
                  Cancel
              </Button>
              <Button variant="primary" size="sm" type="submit" disabled={submitting}>
                  {submitting ? 'Creating…' : 'Save course'}
              </Button>
            </div>
          </form>
        </Panel>
      )}

      {courses.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="activity"
            title="No training courses yet"
            description="Create your first course to prepare horses for training."
          />
        </Panel>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {courses.map((course) => (
            <Panel key={course.id} padded>
              <div className="flex flex-col justify-between h-full">
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="text-lg font-semibold text-[var(--color-text-primary)]">
                      {course.name}
                    </h3>
                    <Pill tone="primary" size="sm">
                  {course.totalSessions} sessions
                    </Pill>
                  </div>

                  {course.targetGoal && (
                    <div className="mt-1 text-xs font-medium text-[var(--color-primary)]">
                      🎯 {course.targetGoal}
                    </div>
                  )}

                  {course.description && (
                    <p className="mt-2 text-xs text-[var(--color-text-secondary)] line-clamp-3">
                      {course.description}
                    </p>
                  )}
                </div>

                <div className="mt-4 pt-3 border-t border-[var(--color-border)] flex items-center justify-between gap-2">
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => handleOpenDetail(course.id)}
                    disabled={detailLoading}
                  >
                    View course details
                  </Button>
                  <Link href={`/trainer/plans/new?courseId=${course.id}`}>
                    <Button variant="primary" size="sm">
                      Ghi danh
                    </Button>
                  </Link>
                </div>
              </div>
            </Panel>
          ))}
        </div>
      )}

      {/* Course detail modal */}
      <Modal
        open={Boolean(viewDetail)}
        onClose={() => setViewDetail(null)}
        title={viewDetail?.course.name ?? ''}
        description={viewDetail ? `Total sessions: ${viewDetail.course.totalSessions}` : undefined}
        footer={viewDetail && (
          <>
            <Button variant="secondary" onClick={() => setViewDetail(null)}>
              Close
            </Button>
            <LinkButton variant="primary" href={`/trainer/plans/new?courseId=${viewDetail.course.id}`}>
              Enroll a group in this course
            </LinkButton>
          </>
        )}
      >
        {viewDetail && (
          <div className="space-y-4">
            {viewDetail.course.targetGoal && (
              <p className="text-sm font-medium text-[var(--color-primary)]">Goal: {viewDetail.course.targetGoal}</p>
            )}
            <div>
              <h4 className="mb-2 text-sm font-medium text-[var(--color-text-primary)]">
                Exercise rotation ({viewDetail.subjects.length} exercises)
              </h4>
              <div className="space-y-1.5">
                {viewDetail.subjects.map((s) => (
                  <div
                    key={s.id}
                    className="flex items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] p-2 text-xs"
                  >
                    <span className="font-semibold text-[var(--color-primary)]">Session {s.orderIndex}</span>
                    <span className="min-w-0 flex-1 truncate font-medium text-[var(--color-text-primary)]">{s.subjectName}</span>
                    <span className="text-[var(--color-text-muted)]">{s.durationMinutes} min</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
