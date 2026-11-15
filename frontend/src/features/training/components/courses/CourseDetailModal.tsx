import { Button, LinkButton, Modal } from '@/components/ui';
import type { CourseDetailResponse } from '../../types';

/** The ordered exercise rotation of one course, with a shortcut to enroll a group in it. */
export function CourseDetailModal({ detail, onClose }: { detail: CourseDetailResponse | null; onClose: () => void }) {
  return (
    <Modal
      open={detail !== null}
      onClose={onClose}
      title={detail?.course.name ?? ''}
      description={detail ? `Total sessions: ${detail.course.totalSessions}` : undefined}
      footer={
        detail && (
          <>
            <Button onClick={onClose}>Close</Button>
            <LinkButton variant="primary" href={`/trainer/course-runs/new?courseId=${detail.course.id}`}>
              Enroll a group in this course
            </LinkButton>
          </>
        )
      }
    >
      {detail && (
        <div className="space-y-4">
          {detail.course.targetGoal && (
            <p className="text-sm text-[var(--color-text-secondary)]">Goal: {detail.course.targetGoal}</p>
          )}
          <div>
            <h3 className="mb-2 text-sm font-medium text-[var(--color-text-primary)]">
              Exercises ({detail.exercises.length}), repeated in order up to {detail.course.totalSessions} sessions
            </h3>
            <ol className="space-y-1.5">
              {detail.exercises.map((exercise) => (
                <li
                  key={exercise.id}
                  className="flex items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] p-2 text-xs"
                >
                  <span className="font-semibold text-[var(--color-primary)]">Exercise {exercise.orderIndex}</span>
                  <span className="min-w-0 flex-1 truncate font-medium text-[var(--color-text-primary)]">{exercise.exerciseName}</span>
                  <span className="text-[var(--color-text-muted)]">{exercise.durationMinutes} min</span>
                </li>
              ))}
            </ol>
          </div>
        </div>
      )}
    </Modal>
  );
}
