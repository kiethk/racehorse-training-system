'use client';

import { useEffect, useMemo } from 'react';
import * as THREE from 'three';
import { Button, IconButton } from '@/components/ui';
import type { Horse } from '../../../types';
import { assignColorSlots, type AreaLayout } from '../../lib/stallLayout';
import { initials, type SceneColors } from './sceneResources';

export interface GroomBadge { color: string; name: string; initials: string }

/**
 * One identity per groom: colour, initials and name. The same badge appears on the groom's figure, on every
 * stall they look after and in the groom key, so "who cares for this horse" reads at a glance. Without access to
 * the staff directory the badge falls back to G1, G2…, which still pairs figures with stalls.
 */
export function groomBadgesFor(
  layout: AreaLayout,
  colors: SceneColors,
  personName: (kind: 'groom', id: number) => string | null,
) {
  const slots = assignColorSlots(layout.blocks.map((block) => block.groomId), colors.staff.length);
  return new Map([...slots].map(([id, slot], index): [number, GroomBadge] => {
    const name = personName('groom', id);
    return [id, { color: colors.staff[slot], name: name ?? `Groom ${index + 1}`, initials: name ? initials(name) : `G${index + 1}` }];
  }));
}

/**
 * Key in the corner of the barn: who the grooms are, their block and how many horses they have. Click to focus.
 * It folds into a small chip so it never hides stalls (folded by default in the top view), and offers a way back
 * from a focused groom.
 */
export function GroomKey({ layout, badges, horseByStallId, currentUserId, focusedId, onFocus, onHover, onClearFocus, collapsed, onToggleCollapsed }: {
  layout: AreaLayout;
  badges: ReadonlyMap<number, GroomBadge>;
  horseByStallId: ReadonlyMap<number, Horse>;
  currentUserId?: number | null;
  focusedId: number | null;
  onFocus: (groomId: number) => void;
  onHover: (groomId: number | null) => void;
  onClearFocus: () => void;
  collapsed: boolean;
  onToggleCollapsed: () => void;
}) {
  if (badges.size === 0) return null;
  const focusedName = focusedId != null ? badges.get(focusedId)?.name : null;
  const showEveryone = focusedId != null && (
    <Button variant="secondary" size="sm" icon="x" onClick={onClearFocus}>Show everyone</Button>
  );

  if (collapsed) {
    return (
      <div data-scene-overlay className="absolute bottom-3 left-3 z-10 flex items-center gap-2">
        <Button variant="secondary" size="sm" icon="users" onClick={onToggleCollapsed} aria-expanded={false}
          className="shadow-[var(--shadow-popover)]">
          Grooms ({badges.size}){focusedName ? ` · ${focusedName}` : ''}
        </Button>
        {showEveryone}
      </div>
    );
  }

  return (
    <div data-scene-overlay className="absolute bottom-3 left-3 z-10 w-72 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2 shadow-[var(--shadow-popover)]">
      <div className="flex items-center justify-between gap-2 px-1 pb-1">
        <p className="text-xs font-semibold text-[var(--color-text-secondary)]">Grooms · click to highlight their horses</p>
        <IconButton icon="minimize" label="Fold the groom key" size="sm" variant="tertiary" onClick={onToggleCollapsed} />
      </div>
      {focusedId != null && <div className="px-1 pb-1">{showEveryone}</div>}
      <ul className="space-y-0.5">
        {[...badges].map(([id, badge]) => {
          const blocks = layout.blocks.filter((block) => block.groomId === id).map((block) => block.blockNumber);
          const horses = layout.slots.filter((slot) => slot.stall.groomId === id && horseByStallId.has(slot.stall.id)).length;
          return (
            <li key={id}>
              <Button
                variant={focusedId === id ? 'secondary' : 'tertiary'}
                size="sm"
                aria-pressed={focusedId === id}
                className="w-full justify-start gap-2"
                onClick={() => onFocus(id)}
                onMouseEnter={() => onHover(id)}
                onMouseLeave={() => onHover(null)}
              >
                <span
                  className="flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full px-1 text-xs font-semibold text-[var(--color-text-inverse)]"
                  style={{ backgroundColor: badge.color }}
                >
                  {badge.initials}
                </span>
                <span className="min-w-0 flex-1 truncate text-left">
                  {badge.name}{id === currentUserId ? ' (you)' : ''}
                </span>
                <span className="shrink-0 text-xs text-[var(--color-text-muted)]">
                  B{blocks.join(', ')} · {horses} {horses === 1 ? 'horse' : 'horses'}
                </span>
              </Button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

/** A thin tube from a groom to one of their horses, drawn over the walls so it is visible from any angle. */
export function GroomLink({ points, color }: { points: [number, number, number][]; color: string }) {
  const geometry = useMemo(() => {
    const curve = new THREE.CatmullRomCurve3(points.map(([x, y, z]) => new THREE.Vector3(x, y, z)));
    return new THREE.TubeGeometry(curve, 24, 0.09, 8, false);
  }, [points]);
  useEffect(() => () => geometry.dispose(), [geometry]);
  return (
    <mesh geometry={geometry} renderOrder={10} raycast={() => null}>
      <meshBasicMaterial color={color} depthTest={false} transparent opacity={0.9} />
    </mesh>
  );
}
