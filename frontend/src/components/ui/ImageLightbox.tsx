'use client';

import { useCallback, useEffect, useRef, useState, type MouseEvent, type PointerEvent } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { cn } from '@/lib/cn';
import { IconButton } from './Button';

export interface LightboxImage {
  /** URL the browser can load directly: an object URL, a blob URL from `useProtectedFileUrl`, or a public path. */
  src: string;
  alt: string;
}

interface ImageLightboxProps {
  images: LightboxImage[];
  /** Index of the image on show; null keeps the lightbox closed. */
  index: number | null;
  onIndexChange: (index: number) => void;
  onClose: () => void;
}

const MIN_SCALE = 1;
const MAX_SCALE = 8;
const STEP = 1.5;

/**
 * Full-screen viewer for one or more images. The image is the original file, drawn with object-contain:
 * never cropped or re-encoded. Wheel, the +/- buttons or keys and a double click zoom; drag pans while zoomed.
 * Arrow keys move between images. Escape, the close button or a click on the backdrop closes it.
 *
 * It sits on the `--z-confirm` layer, so it can open from inside a Modal without closing it.
 */
export function ImageLightbox({ images, index, onIndexChange, onClose }: ImageLightboxProps) {
  const open = index !== null && images.length > 0;
  const current = open ? Math.min(index, images.length - 1) : 0;
  const image = images[current];
  const count = images.length;

  const go = useCallback(
    (delta: number) => {
      if (count > 1) onIndexChange((current + delta + count) % count);
    },
    [count, current, onIndexChange],
  );

  return (
    <Dialog.Root open={open} onOpenChange={(next) => !next && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay
          className={cn(
            'fixed inset-0 z-[var(--z-confirm)] bg-[var(--color-image-overlay)] backdrop-blur-[2px]',
            'data-[state=open]:animate-in data-[state=open]:fade-in data-[state=open]:duration-200',
            'data-[state=closed]:animate-out data-[state=closed]:fade-out data-[state=closed]:duration-150',
          )}
        />
        <Dialog.Content
          aria-describedby={undefined}
          onKeyDown={(event) => {
            if (event.key === 'ArrowLeft') go(-1);
            else if (event.key === 'ArrowRight') go(1);
            else return;
            event.preventDefault();
          }}
          className={cn(
            'fixed inset-0 z-[var(--z-confirm)] flex flex-col outline-none',
            'data-[state=open]:animate-in data-[state=open]:fade-in data-[state=open]:duration-200',
            'data-[state=closed]:animate-out data-[state=closed]:fade-out data-[state=closed]:duration-150',
          )}
        >
          <Dialog.Title className="sr-only">{image?.alt || 'Image'}</Dialog.Title>
          {/* Keyed by image so zoom and pan start fresh on every image. */}
          {image && (
            <ZoomStage
              key={`${current}:${image.src}`}
              image={image}
              position={count > 1 ? `${current + 1}/${count}` : null}
              onPrevious={count > 1 ? () => go(-1) : undefined}
              onNext={count > 1 ? () => go(1) : undefined}
              onClose={onClose}
            />
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

interface ZoomStageProps {
  image: LightboxImage;
  position: string | null;
  onPrevious?: () => void;
  onNext?: () => void;
  onClose: () => void;
}

interface View {
  scale: number;
  x: number;
  y: number;
}

const FIT: View = { scale: 1, x: 0, y: 0 };

/** Pointer position relative to the centre of `element`. */
function fromCentre(element: HTMLElement, clientX: number, clientY: number) {
  const rect = element.getBoundingClientRect();
  return [clientX - rect.left - rect.width / 2, clientY - rect.top - rect.height / 2] as const;
}

function ZoomStage({ image, position, onPrevious, onNext, onClose }: ZoomStageProps) {
  const stageRef = useRef<HTMLDivElement>(null);
  const imageRef = useRef<HTMLImageElement>(null);
  const [view, setView] = useState<View>(FIT);
  const [failed, setFailed] = useState(false);
  const [dragging, setDragging] = useState(false);
  const drag = useRef<{ pointerId: number; startX: number; startY: number; x: number; y: number; moved: boolean } | null>(null);

  /** Keeps the zoomed image covering the stage instead of drifting off screen. */
  const clamp = useCallback((next: View): View => {
    const stage = stageRef.current;
    const img = imageRef.current;
    const scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, next.scale));
    if (!stage || !img || scale === MIN_SCALE) return { scale, x: 0, y: 0 };
    const maxX = Math.max(0, (img.offsetWidth * scale - stage.clientWidth) / 2);
    const maxY = Math.max(0, (img.offsetHeight * scale - stage.clientHeight) / 2);
    return { scale, x: Math.min(maxX, Math.max(-maxX, next.x)), y: Math.min(maxY, Math.max(-maxY, next.y)) };
  }, []);

  /** Zooms to `scale` keeping the point under (cx, cy), relative to the stage centre, in place. */
  const zoomAt = useCallback(
    (scale: number, cx = 0, cy = 0) =>
      setView((v) => {
        const target = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale));
        const ratio = target / v.scale;
        return clamp({ scale: target, x: cx - (cx - v.x) * ratio, y: cy - (cy - v.y) * ratio });
      }),
    [clamp],
  );

  // React wheel listeners are passive; a native one is needed to stop ctrl+wheel from zooming the page.
  useEffect(() => {
    const stage = stageRef.current;
    if (!stage) return;
    const onWheel = (event: WheelEvent) => {
      event.preventDefault();
      const [cx, cy] = fromCentre(stage, event.clientX, event.clientY);
      setView((v) => {
        const target = Math.min(MAX_SCALE, Math.max(MIN_SCALE, v.scale * Math.exp(-event.deltaY * 0.002)));
        const ratio = target / v.scale;
        return clamp({ scale: target, x: cx - (cx - v.x) * ratio, y: cy - (cy - v.y) * ratio });
      });
    };
    stage.addEventListener('wheel', onWheel, { passive: false });
    return () => stage.removeEventListener('wheel', onWheel);
  }, [clamp]);

  // Re-fit the pan limits when the window size changes.
  useEffect(() => {
    const onResize = () => setView((v) => clamp(v));
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [clamp]);

  function handlePointerDown(event: PointerEvent<HTMLDivElement>) {
    if (event.button !== 0) return;
    drag.current = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, x: view.x, y: view.y, moved: false };
  }

  function handlePointerMove(event: PointerEvent<HTMLDivElement>) {
    const state = drag.current;
    if (!state || state.pointerId !== event.pointerId) return;
    const dx = event.clientX - state.startX;
    const dy = event.clientY - state.startY;
    if (!state.moved && Math.hypot(dx, dy) < 4) return;
    if (!state.moved) {
      state.moved = true;
      setDragging(true);
      event.currentTarget.setPointerCapture(event.pointerId);
    }
    if (view.scale > MIN_SCALE) setView((v) => clamp({ ...v, x: state.x + dx, y: state.y + dy }));
  }

  function handlePointerUp(event: PointerEvent<HTMLDivElement>) {
    const state = drag.current;
    drag.current = null;
    setDragging(false);
    if (!state || state.pointerId !== event.pointerId) return;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
    // A plain click on the backdrop closes; a click on the image or the end of a drag does not.
    if (!state.moved && event.target === event.currentTarget) onClose();
  }

  function handleDoubleClick(event: MouseEvent<HTMLDivElement>) {
    if (event.target !== imageRef.current) return;
    if (view.scale > MIN_SCALE) setView(FIT);
    else zoomAt(2.5, ...fromCentre(event.currentTarget, event.clientX, event.clientY));
  }

  const zoomed = view.scale > MIN_SCALE;

  return (
    <div
      className="flex h-full flex-col"
      onKeyDown={(event) => {
        if (event.key === '+' || event.key === '=') zoomAt(view.scale * STEP);
        else if (event.key === '-') zoomAt(view.scale / STEP);
        else if (event.key === '0') setView(FIT);
        else return;
        event.preventDefault();
      }}
    >
      <header className="flex shrink-0 items-center justify-between gap-3 px-4 py-3 text-[var(--color-text-inverse)]">
        <p className="min-w-0 truncate text-sm font-medium">
          {position && <span className="mr-3 font-metric tabular-nums" aria-label={`Image ${position}`}>{position}</span>}
          <span className="opacity-80">{image.alt}</span>
        </p>
        <div className="flex shrink-0 items-center gap-1.5">
          <IconButton icon="minus" label="Zoom out" variant="secondary" size="sm" onClick={() => zoomAt(view.scale / STEP)} disabled={!zoomed} />
          <span className="w-12 text-center text-xs font-metric tabular-nums" aria-live="polite">{Math.round(view.scale * 100)}%</span>
          <IconButton icon="plus" label="Zoom in" variant="secondary" size="sm" onClick={() => zoomAt(view.scale * STEP)} disabled={view.scale >= MAX_SCALE} />
          <IconButton icon="refresh" label="Fit to screen" variant="secondary" size="sm" onClick={() => setView(FIT)} disabled={!zoomed} />
          <Dialog.Close asChild>
            <IconButton icon="x" label="Close" variant="secondary" size="sm" className="ml-2" />
          </Dialog.Close>
        </div>
      </header>

      <div className="relative min-h-0 flex-1">
        <div
          ref={stageRef}
          className={cn(
            'absolute inset-0 flex touch-none select-none items-center justify-center overflow-hidden p-4',
            zoomed ? 'cursor-grab active:cursor-grabbing' : 'cursor-zoom-out',
          )}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={() => { drag.current = null; setDragging(false); }}
          onDoubleClick={handleDoubleClick}
        >
          {failed ? (
            <p className="text-sm text-[var(--color-text-inverse)]">The image could not be loaded.</p>
          ) : (
            // eslint-disable-next-line @next/next/no-img-element -- the original file is shown as is; the optimizer would re-encode it
            <img
              ref={imageRef}
              src={image.src}
              alt={image.alt}
              draggable={false}
              onError={() => setFailed(true)}
              style={{ transform: `translate(${view.x}px, ${view.y}px) scale(${view.scale})` }}
              className={cn(
                'max-h-full max-w-full object-contain',
                !zoomed && 'cursor-zoom-in',
                // Smooth for button and double-click zoom; a drag follows the pointer directly.
                !dragging && 'transition-transform duration-[var(--duration-fast)] ease-[var(--ease-out)] motion-reduce:transition-none',
              )}
            />
          )}
        </div>

        {onPrevious && (
          <IconButton
            icon="chevron-left"
            label="Previous image"
            variant="secondary"
            onClick={onPrevious}
            className="absolute left-4 top-1/2 -translate-y-1/2 rounded-full shadow-[var(--shadow-popover)]"
          />
        )}
        {onNext && (
          <IconButton
            icon="chevron-right"
            label="Next image"
            variant="secondary"
            onClick={onNext}
            className="absolute right-4 top-1/2 -translate-y-1/2 rounded-full shadow-[var(--shadow-popover)]"
          />
        )}
      </div>
    </div>
  );
}

interface ImageThumbnailProps {
  /** URL the browser can load directly (an object URL for a file that is not uploaded yet). */
  src: string;
  alt: string;
  /** Usually opens an `ImageLightbox` on this image. */
  onClick: () => void;
  disabled?: boolean;
  className?: string;
}

/** A clickable, cropped preview of an image. Pair it with `ImageLightbox` to show the whole image. */
export function ImageThumbnail({ src, alt, onClick, disabled, className }: ImageThumbnailProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={`View ${alt}`}
      className={cn(
        'block h-20 w-full cursor-zoom-in overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] outline-none',
        'transition-opacity duration-[var(--duration-fast)] hover:opacity-90 focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]',
        'disabled:cursor-default disabled:opacity-60',
        className,
      )}
    >
      {/* eslint-disable-next-line @next/next/no-img-element -- local or blob preview; not served by the optimizer */}
      <img src={src} alt="" className="h-full w-full object-cover" />
    </button>
  );
}
