'use client';

import { createContext, useCallback, useContext, useEffect, useId, useMemo, useRef, useState } from 'react';
import type { DragEvent, HTMLAttributes, ReactNode } from 'react';

type DragProps = Pick<HTMLAttributes<HTMLElement>, 'draggable' | 'onDragStart' | 'onDragEnd'>;
type DropProps = Pick<HTMLAttributes<HTMLElement>, 'onDragOver' | 'onDragLeave' | 'onDrop'>;

interface DragSession<T> {
  sourceId: string;
  item: T;
  label: string;
  mode: 'pointer' | 'selection';
}

/** Create once at module scope. Each instance has its own typed, isolated drag context. */
export function createDragDrop<T>() {
  interface ContextValue {
    active: DragSession<T> | null;
    begin: (session: DragSession<T>) => void;
    cancel: (sourceId?: string) => void;
    complete: (accept: (item: T) => boolean, onDrop: (item: T) => void) => void;
  }

  const Context = createContext<ContextValue | null>(null);

  function useContextValue() {
    const context = useContext(Context);
    if (!context) throw new Error('Drag and drop controls must be inside their matching Provider.');
    return context;
  }

  function Provider({ children }: { children: ReactNode }) {
    const [active, setActive] = useState<DragSession<T> | null>(null);
    const current = useRef<DragSession<T> | null>(null);
    const [announcement, setAnnouncement] = useState('');

    const begin = useCallback((session: DragSession<T>) => {
      current.current = session;
      setActive(session);
      setAnnouncement(`${session.label} selected. Choose a destination. Press Escape to cancel.`);
    }, []);

    const cancel = useCallback((sourceId?: string) => {
      if (!current.current || (sourceId && current.current.sourceId !== sourceId)) return;
      current.current = null;
      setActive(null);
      setAnnouncement('Move cancelled.');
    }, []);

    const complete = useCallback((accept: (item: T) => boolean, onDrop: (item: T) => void) => {
      const session = current.current;
      if (!session || !accept(session.item)) return;
      // Clear synchronously so nested destinations or repeated clicks cannot deliver twice.
      current.current = null;
      setActive(null);
      setAnnouncement(`${session.label} dropped.`);
      onDrop(session.item);
    }, []);

    useEffect(() => {
      const onKeyDown = (event: KeyboardEvent) => { if (event.key === 'Escape') cancel(); };
      const endPointerDrag = () => {
        if (current.current?.mode === 'pointer') cancel();
      };
      document.addEventListener('keydown', onKeyDown);
      window.addEventListener('dragend', endPointerDrag);
      window.addEventListener('drop', endPointerDrag);
      return () => {
        document.removeEventListener('keydown', onKeyDown);
        window.removeEventListener('dragend', endPointerDrag);
        window.removeEventListener('drop', endPointerDrag);
      };
    }, [cancel]);

    const value = useMemo(() => ({ active, begin, cancel, complete }), [active, begin, cancel, complete]);
    return (
      <Context.Provider value={value}>
        {children}
        <span role="status" aria-live="polite" aria-atomic="true" className="sr-only">{announcement}</span>
      </Context.Provider>
    );
  }

  function useDragDrop() {
    const { active, cancel } = useContextValue();
    return { activeItem: active?.item ?? null, cancel };
  }

  function useDraggable({ item, label, disabled = false }: { item: T; label: string; disabled?: boolean }) {
    const { active, begin, cancel } = useContextValue();
    const sourceId = useId();
    useEffect(() => () => cancel(sourceId), [cancel, sourceId]);

    const pickUp = () => {
      if (!disabled) begin({ sourceId, item, label, mode: 'selection' });
    };
    const dragProps: DragProps = {
      draggable: !disabled,
      onDragStart: event => {
        // A nested source owns its drag; do not replace it with its ancestor's item.
        event.stopPropagation();
        if (disabled) { event.preventDefault(); return; }
        event.dataTransfer.setData('application/x-rtms-item', sourceId);
        event.dataTransfer.effectAllowed = 'move';
        begin({ sourceId, item, label, mode: 'pointer' });
      },
      onDragEnd: event => { event.stopPropagation(); cancel(sourceId); },
    };
    return { dragProps, pickUp, isDragging: active?.sourceId === sourceId };
  }

  function useDropZone({ accept, onDrop, disabled = false }: {
    accept: (item: T) => boolean;
    onDrop: (item: T) => void;
    disabled?: boolean;
  }) {
    const { active, complete } = useContextValue();
    const [hovered, setHovered] = useState<DragSession<T> | null>(null);
    const canDrop = !disabled && active != null && accept(active.item);
    const canPointerDrop = canDrop && active?.mode === 'pointer';
    const drop = () => { if (!disabled) complete(accept, onDrop); };
    const dropProps: DropProps = {
      onDragOver: event => {
        if (!canPointerDrop) return;
        event.preventDefault();
        event.stopPropagation();
        event.dataTransfer.dropEffect = 'move';
        setHovered(active);
      },
      onDragLeave: event => {
        if (!(event.relatedTarget instanceof Node) || !event.currentTarget.contains(event.relatedTarget)) setHovered(null);
      },
      onDrop: (event: DragEvent<HTMLElement>) => {
        // Incompatible nested zones may bubble to a compatible ancestor.
        if (!canPointerDrop) return;
        event.preventDefault();
        event.stopPropagation();
        setHovered(null);
        drop();
      },
    };
    return { dropProps, canDrop, isOver: canPointerDrop && hovered === active, drop };
  }

  return { Provider, useDragDrop, useDraggable, useDropZone };
}
