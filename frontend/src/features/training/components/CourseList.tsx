'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
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
      console.error('Failed to load courses:', err);
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
      console.error('Unable to load course details:', err);
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
      setFormError('Enter a course name and a valid session count.');
      return;
    }
    if (selectedSubjectIds.length === 0) {
      setFormError('Select at least one workout in the session rotation.');
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
          <h2 className="text-[16px] font-semibold text-[var(--color-text-primary)]">
            Training courses ({courses.length})
          </h2>
          <p className="text-[12px] text-[var(--color-text-secondary)]">
            Complete curricula with workout rotations for horse enrollment.
          </p>
        </div>
        <div className="flex gap-2">
          <Link href="/trainer/plans/new">
            <Button variant="secondary" size="sm">
              Enroll horses
            </Button>
          </Link>
          <Button variant="primary" size="sm" onClick={() => setShowForm(!showForm)}>
            {showForm ? 'Close form' : '+ Create course'}
          </Button>
        </div>
      </div>

      {showForm && (
        <Panel padded>
          <form onSubmit={handleCreateCourse} className="space-y-4">
            <h3 className="text-[14px] font-semibold text-[var(--color-text-primary)]">
              Create course (with workout rotation)
            </h3>

            {formError && (
              <div className="rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
                {formError}
              </div>
            )}

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Course name *
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="For example: Sprint 2YO Foundation"
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                />
              </div>

              <div>
                <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                  Total sessions *
                </label>
                <input
                  type="number"
                  min="1"
                  required
                  value={totalSessions}
                  onChange={(e) => setTotalSessions(Number(e.target.value))}
                  className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
                />
                <span className="text-[11px] text-[var(--color-text-muted)]">
                Workouts rotate sequentially until this number of sessions is reached.
                </span>
              </div>
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Target goal
              </label>
              <input
                type="text"
                value={targetGoal}
                onChange={(e) => setTargetGoal(e.target.value)}
                placeholder="For example: Eligible for a 1,200 m short-distance race"
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Course description
              </label>
              <textarea
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Describe the route and suitable fitness level..."
                className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]"
              />
            </div>

            {/* Chọn vòng xoay bài học */}
            <div className="border-t border-[var(--color-border)] pt-3">
              <label className="block text-[12px] font-medium text-[var(--color-text-primary)]">
                Workout rotation ({selectedSubjectIds.length} selected)
              </label>
              <p className="text-[11px] text-[var(--color-text-muted)]">
                Select workouts below to add them to the rotation order:
              </p>

              {/* Danh sách các bài đã chọn */}
              <div className="mt-2 min-h-12 rounded-[var(--radius-md)] border border-dashed border-[var(--color-border-strong)] bg-[var(--color-surface-muted)] p-2">
                {selectedSubjectIds.length === 0 ? (
                  <span className="text-[12px] text-[var(--color-text-muted)] italic">
                    No workouts selected for the rotation.
                  </span>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {selectedSubjectIds.map((subId, idx) => {
                      const s = subjectMap.get(subId);
                      return (
                        <div
                          key={`${subId}-${idx}`}
                          className="flex items-center gap-1.5 rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1 text-[12px]"
                        >
                          <span className="font-bold text-[var(--color-primary)]">#{idx + 1}</span>
                          <span>{s?.name || `Workout #${subId}`}</span>
                          <button
                            type="button"
                            onClick={() => removeSubjectFromSequence(idx)}
                            className="ml-1 text-[var(--color-text-muted)] hover:text-[var(--color-danger)]"
                          >
                            ×
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Danh sách bài có sẵn để bấm chọn */}
              <div className="mt-3 flex flex-wrap gap-1.5">
                {subjects.map((sub) => (
                  <button
                    key={sub.id}
                    type="button"
                    onClick={() => addSubjectToCourseSequence(sub.id)}
                    className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1 text-[11px] hover:border-[var(--color-primary)] hover:bg-[var(--color-primary-soft)] transition"
                  >
                    + {sub.name} ({sub.durationMinutes}&apos;)
                  </button>
                ))}
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-[var(--color-border)]">
              <Button variant="secondary" size="sm" type="button" onClick={() => setShowForm(false)}>
                Cancel
              </Button>
              <Button variant="primary" size="sm" type="submit" disabled={submitting}>
                {submitting ? 'Creating...' : 'Save course'}
              </Button>
            </div>
          </form>
        </Panel>
      )}

      {courses.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="activity"
            title="No training courses"
            description="Create the first course to prepare horses for training."
          />
        </Panel>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {courses.map((course) => (
            <Panel key={course.id} padded>
              <div className="flex flex-col justify-between h-full">
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="text-[15px] font-semibold text-[var(--color-text-primary)]">
                      {course.name}
                    </h3>
                    <Pill tone="primary" size="sm">
                      {course.totalSessions} sessions
                    </Pill>
                  </div>

                  {course.targetGoal && (
                    <div className="mt-1 text-[12px] font-medium text-[var(--color-primary)]">
                      🎯 {course.targetGoal}
                    </div>
                  )}

                  {course.description && (
                    <p className="mt-2 text-[12px] text-[var(--color-text-secondary)] line-clamp-3">
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

      {/* Modal chi tiết khóa học */}
      {viewDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/30 backdrop-blur-sm"
            onClick={() => setViewDetail(null)}
            aria-hidden="true"
          />
          <div
            role="dialog"
            aria-modal="true"
            className="relative w-full max-w-lg rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 shadow-xl"
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 className="text-[16px] font-semibold text-[var(--color-text-primary)]">
                  {viewDetail.course.name}
                </h3>
                <div className="text-[12px] text-[var(--color-text-secondary)]">
                  Total sessions: {viewDetail.course.totalSessions}
                </div>
              </div>
              <button
                type="button"
                onClick={() => setViewDetail(null)}
                className="text-[14px] text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)]"
              >
                ✕
              </button>
            </div>

            {viewDetail.course.targetGoal && (
              <p className="mt-2 text-[13px] text-[var(--color-primary)] font-medium">
                Goal: {viewDetail.course.targetGoal}
              </p>
            )}

            <div className="mt-4">
              <h4 className="text-[13px] font-medium text-[var(--color-text-primary)] mb-2">
                Sequential workout rotation ({viewDetail.subjects.length} workouts):
              </h4>
              <div className="max-h-60 overflow-y-auto space-y-1.5 pr-1">
                {viewDetail.subjects.map((s) => (
                  <div
                    key={s.id}
                    className="flex items-center justify-between rounded-[var(--radius-md)] border border-[var(--color-border)] p-2 text-[12px]"
                  >
                    <span className="font-semibold text-[var(--color-primary)]">
                      Session {s.orderIndex}:
                    </span>
                    <span className="font-medium text-[var(--color-text-primary)]">
                      {s.subjectName}
                    </span>
                    <span className="text-[var(--color-text-muted)]">
                      {s.durationMinutes} minutes
                    </span>
                  </div>
                ))}
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setViewDetail(null)}>
                Close
              </Button>
              <Link href={`/trainer/plans/new?courseId=${viewDetail.course.id}`}>
                <Button variant="primary" size="sm">
                  Enroll group in this course
                </Button>
              </Link>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
