# Shared drag and drop

Import `createDragDrop` from `@/components/ui`. Native HTML drag and drop, no additional dependency. The primitive owns the active item, drag lifecycle, accepted destinations, hover state, cancellation and screen reader announcements. Features own layout, business rules, confirmation and persistence.

## Exercise into Course

Create one typed instance **at module scope**, shared by all source/destination components in that feature:

```tsx
'use client';

import { createDragDrop } from '@/components/ui';
import type { Exercise } from '../types';

export const exerciseDragDrop = createDragDrop<Exercise>();
```

Wrap the screen in `<exerciseDragDrop.Provider>`. Hooks must run in children of that provider. Separate provider instances are isolated, even when nested. Do not call the factory inside a component.

Source card:

```tsx
const { dragProps, pickUp, isDragging } = exerciseDragDrop.useDraggable({
  item: exercise,
  label: exercise.name,
  disabled: saving,
});

return (
  <SelectableCard {...dragProps} selected={isDragging} disabled={saving} onClick={pickUp}>
    {exercise.name}
  </SelectableCard>
);
```

Destination:

```tsx
const { dropProps, canDrop, isOver, drop } = exerciseDragDrop.useDropZone({
  disabled: saving,
  accept: exercise => !selectedExercises.some(item => item.id === exercise.id),
  onDrop: exercise => setSelectedExercises(items => [...items, exercise]),
});

return (
  <Panel {...dropProps} className={cn(isOver && 'ring-2 ring-[var(--color-primary)]')}>
    {/* Render selected exercises here. */}
    <Button disabled={!canDrop} onClick={drop}>Add selected exercise</Button>
  </Panel>
);
```

The Add button provides the same action by click, Enter or Space after selecting a source card. Keep this alternative, or provide an existing picker/menu with equivalent behavior. Dragging alone is insufficient for keyboard, touch and accessibility. Escape cancels; `useDragDrop().cancel()` supports an explicit Cancel action. Do not pass `cancel` directly to `onClick` because it optionally accepts a source ID; use `onClick={() => cancel()}`.

## API

| API | Input / output |
|---|---|
| `Provider` | Wraps related controls. Adds only a visually hidden live status; no layout wrapper. |
| `useDraggable({ item, label, disabled? })` | Spread `dragProps` on the source; `pickUp()` selects through a button; `isDragging` includes selection. |
| `useDropZone({ accept, onDrop, disabled? })` | Spread `dropProps` on the destination; `canDrop`, `isOver`, `drop()` for visuals and accessible alternatives. |
| `useDragDrop()` | `{ activeItem, cancel }`; `activeItem` is `T \| null`. |

- `accept` must be a pure predicate. It runs during hover and again before delivery. Reject duplicates, incompatible types or unavailable slots here; backend remains authoritative.
- Only a source within the matching provider can deliver an item. Files, text and payloads from other browser windows are not parsed as application objects.
- Nested compatible destinations deliver once to the innermost zone. Rejected inner zones may bubble to a compatible ancestor.
- The session ends before `onDrop` runs. Use `onDrop` to update a draft or open confirmation; call the feature API separately. Report save errors in the feature.
- Native drag ends/cancelled/outside drops clear the session. Removing a source also clears its session. A cancelled/rejected move does not call `onDrop`.
- Spread bindings onto the actual source/destination. Avoid overriding their event handlers; keep click actions on `Button` or `SelectableCard`. For hooks inside a list, extract a row component rather than calling hooks inside `map`.
- This is item transfer/assignment, not sortable-list insertion, multi-select, cross-window data transfer or file upload.

## Existing consumers

- Stable: `src/features/stable/components/staffDragDrop.ts`, `StaffAssignment`, `AreaCard`, `StableStaffPanel`. Staff limits, occupied-block protection and swap confirmation stay in Stable.
- Runnable Exercise → Course example: `/design-test`, section **Drag and drop** (`src/app/design-test/DragDropExample.tsx`). Example state is local and makes no API writes.
