'use client';

import { useState } from 'react';
import { Button, createDragDrop, IconButton, Panel, SelectableCard } from '@/components/ui';
import { cn } from '@/lib/cn';

interface ExampleSubject { id: number; name: string }
const subjectDragDrop = createDragDrop<ExampleSubject>();
const subjects: ExampleSubject[] = [
  { id: 1, name: 'Groundwork' },
  { id: 2, name: 'Endurance' },
  { id: 3, name: 'Recovery' },
];

/** Local showcase only: no course or subject records are saved. */
export function DragDropExample() {
  return <subjectDragDrop.Provider><CourseExample /></subjectDragDrop.Provider>;
}

function CourseExample() {
  const [assigned, setAssigned] = useState<ExampleSubject[]>([]);
  const { activeItem, cancel } = subjectDragDrop.useDragDrop();
  const { dropProps, canDrop, isOver, drop } = subjectDragDrop.useDropZone({
    accept: subject => !assigned.some(item => item.id === subject.id),
    onDrop: subject => setAssigned(items => [...items, subject]),
  });
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <Panel padded className="space-y-3">
        <h3 className="text-sm font-semibold">Subjects</h3>
        <p className="text-xs text-[var(--color-text-secondary)]">Drag a subject, or select it and choose Add selected subject. Escape cancels selection.</p>
        {subjects.map(subject => <SubjectCard key={subject.id} subject={subject} disabled={assigned.some(item => item.id === subject.id)} />)}
        {activeItem && <Button variant="tertiary" size="sm" onClick={() => cancel()}>Cancel selection</Button>}
      </Panel>
      <Panel {...dropProps} padded className={cn('space-y-3', isOver && 'ring-2 ring-[var(--color-primary)]')}>
        <h3 className="text-sm font-semibold">Course subjects</h3>
        {assigned.length === 0 && <p className="text-sm text-[var(--color-text-secondary)]">No subjects added</p>}
        {assigned.map(subject => <div key={subject.id} className="flex items-center justify-between gap-3 text-sm">
          <span>{subject.name}</span>
          <IconButton icon="x" label={`Remove ${subject.name}`} size="sm" onClick={() => setAssigned(items => items.filter(item => item.id !== subject.id))} />
        </div>)}
        <Button disabled={!canDrop} onClick={drop}>Add selected subject</Button>
      </Panel>
    </div>
  );
}

function SubjectCard({ subject, disabled }: { subject: ExampleSubject; disabled: boolean }) {
  const { dragProps, pickUp, isDragging } = subjectDragDrop.useDraggable({ item: subject, label: subject.name, disabled });
  return <SelectableCard {...dragProps} disabled={disabled} selected={isDragging} onClick={pickUp}
    className={cn(!disabled && 'cursor-grab active:cursor-grabbing')}>{subject.name}</SelectableCard>;
}
