'use client';

import { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { clampDialogPosition, getPopupPositionFromFab } from '../lib/aiUtils';

export function useChatWindowDrag(isExpanded: boolean, toggleOpen: () => void) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const fabRef = useRef<HTMLDivElement>(null);

  const [anchor, setAnchor] = useState<{ x: number; y: number } | null>(null);
  const [isDraggingDialog, setIsDraggingDialog] = useState(false);
  const [isDraggingFab, setIsDraggingFab] = useState(false);

  const isDraggingAny = isDraggingDialog || isDraggingFab;

  const dialogDragStartRef = useRef<{
    pointerX: number;
    pointerY: number;
    startAnchorX: number;
    startAnchorY: number;
  } | null>(null);

  const fabDragStartRef = useRef<{
    pointerX: number;
    pointerY: number;
    startAnchorX: number;
    startAnchorY: number;
    moved: boolean;
  } | null>(null);

  const getInitialAnchor = useCallback(() => {
    if (anchor) return anchor;
    if (typeof window === 'undefined') return { x: 0, y: 0 };
    const el = fabRef.current;
    if (el) {
      const r = el.getBoundingClientRect();
      return { x: r.left, y: r.top };
    }
    return {
      x: window.innerWidth - 24 - 56,
      y: window.innerHeight - 24 - 56,
    };
  }, [anchor]);

  // Handle dragging the popup from its header
  const handleHeaderPointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (e.button !== 0) return;
    if ((e.target as HTMLElement).closest('button, input, textarea, a')) return;

    const start = getInitialAnchor();
    dialogDragStartRef.current = {
      pointerX: e.clientX,
      pointerY: e.clientY,
      startAnchorX: start.x,
      startAnchorY: start.y,
    };
    setIsDraggingDialog(true);
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {}
  };

  const handleHeaderPointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDraggingDialog || !dialogDragStartRef.current) return;
    const { pointerX, pointerY, startAnchorX, startAnchorY } = dialogDragStartRef.current;
    const deltaX = e.clientX - pointerX;
    const deltaY = e.clientY - pointerY;

    if (Math.abs(deltaX) > 2 || Math.abs(deltaY) > 2 || anchor !== null) {
      const clamped = clampDialogPosition(
        { x: startAnchorX + deltaX, y: startAnchorY + deltaY },
        56,
        56,
        window.innerWidth,
        window.innerHeight,
        12
      );
      setAnchor(clamped);
    }
  };

  const handleHeaderPointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (isDraggingDialog) {
      setIsDraggingDialog(false);
      dialogDragStartRef.current = null;
      try {
        e.currentTarget.releasePointerCapture(e.pointerId);
      } catch {}
    }
  };

  // Handle dragging the circular FAB
  const handleFabPointerDown = (e: React.PointerEvent<HTMLButtonElement>) => {
    if (e.button !== 0) return;
    const start = getInitialAnchor();
    fabDragStartRef.current = {
      pointerX: e.clientX,
      pointerY: e.clientY,
      startAnchorX: start.x,
      startAnchorY: start.y,
      moved: false,
    };
    setIsDraggingFab(true);
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {}
  };

  const handleFabPointerMove = (e: React.PointerEvent<HTMLButtonElement>) => {
    if (!isDraggingFab || !fabDragStartRef.current) return;
    const { pointerX, pointerY, startAnchorX, startAnchorY } = fabDragStartRef.current;
    const deltaX = e.clientX - pointerX;
    const deltaY = e.clientY - pointerY;

    if (!fabDragStartRef.current.moved && Math.hypot(deltaX, deltaY) > 4) {
      fabDragStartRef.current.moved = true;
    }

    if (fabDragStartRef.current.moved) {
      const clamped = clampDialogPosition(
        { x: startAnchorX + deltaX, y: startAnchorY + deltaY },
        56,
        56,
        window.innerWidth,
        window.innerHeight,
        12
      );
      setAnchor(clamped);
    }
  };

  const handleFabPointerUp = (e: React.PointerEvent<HTMLButtonElement>) => {
    if (isDraggingFab) {
      setIsDraggingFab(false);
      try {
        e.currentTarget.releasePointerCapture(e.pointerId);
      } catch {}
    }
  };

  const handleFabPointerCancel = () => {
    setIsDraggingFab(false);
    if (fabDragStartRef.current) {
      fabDragStartRef.current.moved = false;
    }
  };

  const handleFabClick = () => {
    if (fabDragStartRef.current?.moved) {
      fabDragStartRef.current = null;
      return;
    }
    fabDragStartRef.current = null;
    toggleOpen();
  };

  const handleResetPosition = (e?: React.MouseEvent) => {
    if (e && (e.target as HTMLElement).closest('button, input, textarea, a')) return;
    setAnchor(null);
  };

  useEffect(() => {
    if (!anchor) return;
    const handleResize = () => {
      setAnchor((prev) => {
        if (!prev) return null;
        return clampDialogPosition(prev, 56, 56, window.innerWidth, window.innerHeight, 12);
      });
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [anchor]);

  const dialogPosition = useMemo(() => {
    if (!anchor || typeof window === 'undefined') return null;
    return getPopupPositionFromFab(
      anchor,
      isExpanded,
      window.innerWidth,
      window.innerHeight
    );
  }, [anchor, isExpanded]);

  return {
    dialogRef,
    fabRef,
    anchor,
    dialogPosition,
    isDraggingAny,
    handleHeaderPointerDown,
    handleHeaderPointerMove,
    handleHeaderPointerUp,
    handleFabPointerDown,
    handleFabPointerMove,
    handleFabPointerUp,
    handleFabPointerCancel,
    handleFabClick,
    handleResetPosition,
  };
}
