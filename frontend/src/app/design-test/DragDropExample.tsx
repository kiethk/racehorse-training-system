'use client';

import { useState } from 'react';
import { Button, createDragDrop, IconButton, Panel, SelectableCard } from '@/components/ui';
import { cn } from '@/lib/cn';

interface ExampleExercise { id: number; name: string }
const exerciseDragDrop = createDragDrop<ExampleExercise>();
const exercises: ExampleExercise[] = [
  { id: 1, name: 'Groundwork' },
  { id: 2, name: 'Endurance' },
  { id: 3, name: 'Recovery' },
];

/** Local showcase only: no course or exercise records are saved. */
export function DragDropExample() {
  return <exerciseDragDrop.Provider><CourseExample /></exerciseDragDrop.Provider>;
}

function CourseExample() {
  const [assigned, setAssigned] = useState<ExampleExercise[]>([]);
  const { activeItem, cancel } = exerciseDragDrop.useDragDrop();
  const { dropProps, canDrop, isOver, drop } = exerciseDragDrop.useDropZone({
    accept: exercise => !assigned.some(item => item.id === exercise.id),
    onDrop: exercise => setAssigned(items => [...items, exercise]),
  });
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <Panel padded className="space-y-3">
        <h3 className="text-sm font-semibold">Exercises</h3>
        <p className="text-xs text-[var(--color-text-secondary)]">Drag an exercise, or select it and choose Add selected exercise. Escape cancels selection.</p>
        {exercises.map(exercise => <ExerciseCard key={exercise.id} exercise={exercise} disabled={assigned.some(item => item.id === exercise.id)} />)}
        {activeItem && <Button variant="tertiary" size="sm" onClick={() => cancel()}>Cancel selection</Button>}
      </Panel>
      <Panel {...dropProps} padded className={cn('space-y-3', isOver && 'ring-2 ring-[var(--color-primary)]')}>
        <h3 className="text-sm font-semibold">Course exercises</h3>
        {assigned.length === 0 && <p className="text-sm text-[var(--color-text-secondary)]">No exercises added</p>}
        {assigned.map(exercise => <div key={exercise.id} className="flex items-center justify-between gap-3 text-sm">
          <span>{exercise.name}</span>
          <IconButton icon="x" label={`Remove ${exercise.name}`} size="sm" onClick={() => setAssigned(items => items.filter(item => item.id !== exercise.id))} />
        </div>)}
        <Button disabled={!canDrop} onClick={drop}>Add selected exercise</Button>
      </Panel>
    </div>
  );
}

function ExerciseCard({ exercise, disabled }: { exercise: ExampleExercise; disabled: boolean }) {
  const { dragProps, pickUp, isDragging } = exerciseDragDrop.useDraggable({ item: exercise, label: exercise.name, disabled });
  return <SelectableCard {...dragProps} disabled={disabled} selected={isDragging} onClick={pickUp}
    className={cn(!disabled && 'cursor-grab active:cursor-grabbing')}>{exercise.name}</SelectableCard>;
}
