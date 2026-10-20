'use client';

import { useFrame } from '@react-three/fiber';
import type { RefObject } from 'react';
import * as THREE from 'three';
import { cn } from '@/lib/cn';

interface LabelBase {
  key: string;
  position: [number, number, number];
  /** Higher wins when two labels would overlap on screen; the loser is hidden until there is room. */
  priority: number;
}

export type SceneLabel =
  | (LabelBase & {
      kind: 'stall';
      code: string;
      horseName: string | null;
      /** Resolved colour of the stall state, shown as the leading dot. */
      toneColor: string;
      groom: { color: string; initials: string; name: string } | null;
      selected: boolean;
      dimmed: boolean;
    })
  | (LabelBase & {
      kind: 'staff';
      name: string;
      /** Short mark in the coloured badge (initials), matching the badge on each of their stalls. */
      badge: string;
      color: string | null;
      caption?: string;
      isSelf: boolean;
      highlighted: boolean;
      faded: boolean;
    });

export type LabelElements = RefObject<Map<string, HTMLElement>>;

const projected = new THREE.Vector3();
const GAP = 3;

interface Placed { left: number; top: number; right: number; bottom: number }

const overlaps = (a: Placed, b: Placed) =>
  a.left < b.right + GAP && b.left < a.right + GAP && a.top < b.bottom + GAP && b.top < a.bottom + GAP;

/**
 * Moves the HTML labels to their 3D anchors after every rendered frame and hides the ones that would
 * collide with a more important label. One DOM layer for every label (instead of one React root per label)
 * keeps camera moves cheap and avoids root mount/unmount races.
 */
export function LabelProjector({ labels, elements }: { labels: SceneLabel[]; elements: LabelElements }) {
  useFrame(({ camera, size }) => {
    // Read every size first, then write: transforms do not trigger layout, so this stays one layout pass.
    const items = labels.flatMap((label) => {
      const element = elements.current.get(label.key);
      if (!element) return [];
      projected.set(...label.position).project(camera);
      return [{
        label,
        element,
        x: ((projected.x + 1) / 2) * size.width,
        y: ((1 - projected.y) / 2) * size.height,
        inFront: projected.z < 1,
        width: element.offsetWidth,
        height: element.offsetHeight,
      }];
    }).sort((a, b) => b.label.priority - a.label.priority);

    // Overlays drawn over the canvas (area title, hint, groom key) count as taken space.
    const layer = items[0]?.element.parentElement;
    const origin = layer?.getBoundingClientRect();
    const placed: Placed[] = origin
      ? [...(layer?.parentElement?.querySelectorAll('[data-scene-overlay]') ?? [])].map((overlay) => {
          const rect = overlay.getBoundingClientRect();
          return { left: rect.left - origin.left, top: rect.top - origin.top, right: rect.right - origin.left, bottom: rect.bottom - origin.top };
        })
      : [];
    for (const item of items) {
      // Stall tags may slide one row up or down to make room. People's tags never move: a name drifting
      // next to someone else's horses would say the wrong thing, so they hide instead (the groom key lists them).
      const step = item.height + GAP;
      const offsets = item.label.kind === 'stall' ? [0, -step, step] : [0];
      const box = offsets.map((dy) => ({
        left: item.x - item.width / 2,
        top: item.y - item.height + dy,
        right: item.x + item.width / 2,
        bottom: item.y + dy,
      })).find((candidate) => !placed.some((other) => overlaps(candidate, other)));
      const show = item.inFront && box != null;
      const at = box ?? { left: item.x - item.width / 2, top: item.y - item.height };
      if (show) placed.push(box);
      item.element.style.transform = `translate3d(${at.left.toFixed(1)}px, ${at.top.toFixed(1)}px, 0)`;
      item.element.style.opacity = show ? '1' : '0';
      item.element.style.visibility = item.inFront ? 'visible' : 'hidden';
    }
  });
  return null;
}

function StallLabel({ label }: { label: Extract<SceneLabel, { kind: 'stall' }> }) {
  return (
    <div
      className={cn(
        'flex items-center gap-1.5 whitespace-nowrap rounded-[var(--radius-sm)] border py-0.5 pl-1.5 text-xs shadow-[var(--shadow-panel)]',
        label.groom ? 'pr-0.5' : 'pr-1.5',
        label.selected
          ? 'border-[var(--color-primary)] bg-[var(--color-primary)] text-[var(--color-text-inverse)]'
          : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-primary)]',
        label.dimmed && !label.selected && 'opacity-60',
      )}
    >
      <span className="h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: label.toneColor }} />
      <span className="font-metric font-bold">{label.code}</span>
      {label.horseName && <span className="max-w-28 truncate font-medium">{label.horseName}</span>}
      {label.groom && (
        <span
          className="rounded-[var(--radius-xs)] px-1 font-semibold text-[var(--color-text-inverse)]"
          style={{ backgroundColor: label.groom.color }}
          title={`Groom: ${label.groom.name}`}
        >
          {label.groom.initials}
        </span>
      )}
    </div>
  );
}

function StaffLabel({ label }: { label: Extract<SceneLabel, { kind: 'staff' }> }) {
  return (
    <div
      className={cn(
        'flex items-center gap-1.5 whitespace-nowrap rounded-full border bg-[var(--color-surface)] py-0.5 pl-0.5 pr-2 text-xs shadow-[var(--shadow-panel)]',
        label.highlighted ? 'border-[var(--color-primary)] shadow-[var(--shadow-popover)]' : 'border-[var(--color-border)]',
        label.faded && !label.highlighted && 'opacity-60',
      )}
    >
      <span
        className={cn(
          'flex h-5 min-w-5 items-center justify-center rounded-full px-1 font-semibold text-[var(--color-text-inverse)]',
          label.color == null && 'border border-dashed border-[var(--color-border-strong)] text-[var(--color-text-muted)]',
        )}
        style={label.color ? { backgroundColor: label.color } : undefined}
      >
        {label.badge}
      </span>
      <span className={cn('font-medium', label.color == null ? 'italic text-[var(--color-text-muted)]' : 'text-[var(--color-text-primary)]')}>
        {label.name}
      </span>
      {label.isSelf && <span className="font-semibold text-[var(--color-primary)]">You</span>}
      {label.caption && <span className="text-[var(--color-text-muted)]">{label.caption}</span>}
    </div>
  );
}

/** The DOM side of the labels. Rendered next to the canvas, outside the WebGL tree. */
export function LabelLayer({ labels, elements }: { labels: SceneLabel[]; elements: LabelElements }) {
  return (
    // z-0 keeps the labels' own z-order inside this layer, under the overlays drawn after it.
    <div className="pointer-events-none absolute inset-0 z-0 overflow-hidden" aria-hidden="true">
      {labels.map((label) => (
        <div
          key={label.key}
          className="absolute left-0 top-0 transition-opacity duration-[var(--duration-fast)]"
          // Hidden until the projector has placed it, so nothing flashes at the corner.
          style={{ visibility: 'hidden', zIndex: label.priority }}
          ref={(element) => {
            if (element) elements.current.set(label.key, element);
            else elements.current.delete(label.key);
          }}
        >
          {label.kind === 'stall' ? <StallLabel label={label} /> : <StaffLabel label={label} />}
        </div>
      ))}
    </div>
  );
}
