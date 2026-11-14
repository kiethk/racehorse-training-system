'use client';

import { IconButton, Pill } from '@/components/ui';
import { cn } from '@/lib/cn';
import { intensityOption, surfaceLabel } from '../../lib/exerciseOptions';
import type { Exercise } from '../../types';
import { exerciseDragDrop } from './exerciseDragDrop';

/** A card for one exercise in the library: click or drag it to add it to the course rotation. */
export function DraggableExerciseCard({
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
  const { dragProps, isDragging } = exerciseDragDrop.useDraggable({
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

      <div className="mt-1.5 flex flex-wrap items-center gap-1 text-xs text-[var(--color-text-secondary)]">
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
