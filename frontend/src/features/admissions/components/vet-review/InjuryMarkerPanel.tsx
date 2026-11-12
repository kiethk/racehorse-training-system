'use client';

import dynamic from 'next/dynamic';
import { useState, type Dispatch, type SetStateAction } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Input } from '@/components/ui/Input';
import {
  markerZoneLabel,
  matchBodyRegion,
  regionKeyLabel,
  worldToNormalized,
} from '@/features/vet/lib/injuryZones';
import type { HorseBodyRegion } from '@/features/health/types';
import type { CreateInjuryMarkerRequest } from '../../types';

const UrgentHorseInjuryViewer = dynamic(
  () => import('@/features/vet/components/UrgentHorseInjuryViewer'),
  { ssr: false },
);

export interface InjuryMarker {
  id: string;
  position: [number, number, number];
  normal: [number, number, number];
  zoneId?: string;
  /** Maps to a real horse body region from the catalog (resolved from 3D hit). */
  bodyRegionId?: number | null;
  description?: string;
}

export function buildInjuryMarkerRequests(markers: InjuryMarker[]): CreateInjuryMarkerRequest[] {
  return markers.map((m) => ({
    bodyRegionId: m.bodyRegionId ?? 1,
    description: m.description?.trim() || '',
    diagnosedAt: new Date().toISOString(),
    positionX: m.position[0],
    positionY: m.position[1],
    positionZ: m.position[2],
    zoneId: m.zoneId ?? null,
    localX: Number(worldToNormalized(m.position).x.toFixed(4)),
    localY: Number(worldToNormalized(m.position).y.toFixed(4)),
    localZ: Number(worldToNormalized(m.position).z.toFixed(4)),
    normalX: m.normal[0],
    normalY: m.normal[1],
    normalZ: m.normal[2],
  }));
}

export interface InjuryMarkerPanelProps {
  markers: InjuryMarker[];
  setMarkers: Dispatch<SetStateAction<InjuryMarker[]>>;
  bodyRegions: HorseBodyRegion[];
  readOnly?: boolean;
  onAppendToFindings?: (line: string) => void;
  fieldErrors?: Record<string, string>;
  setFieldErrors?: Dispatch<SetStateAction<Record<string, string>>>;
}

export function InjuryMarkerPanel(props: InjuryMarkerPanelProps) {
  const { markers, setMarkers, bodyRegions, readOnly, onAppendToFindings, fieldErrors, setFieldErrors } = props;
  const [isAddMode, setIsAddMode] = useState(false);

  const clearMarkerErrors = () => {
    if (setFieldErrors && fieldErrors?.injuryMarkers) {
      setFieldErrors((prev) => ({ ...prev, injuryMarkers: '' }));
    }
  };

  const handleInjuryHit = (hit: {
    position: [number, number, number];
    normal: [number, number, number];
    zoneId: string;
  }) => {
    const id =
      typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : `marker-${Date.now()}`;
    const region = matchBodyRegion(bodyRegions, hit.zoneId);
    const zoneName = region?.displayName ?? regionKeyLabel(hit.zoneId) ?? 'Unspecified';
    const newMarker: InjuryMarker = {
      id,
      position: hit.position,
      normal: hit.normal,
      zoneId: hit.zoneId,
      bodyRegionId: region?.id ?? (bodyRegions.length ? bodyRegions[0].id : null),
      description: '',
    };
    setMarkers((prev) => [...prev, newMarker]);
    if (onAppendToFindings) {
      const line = `- 3D injury marker [${zoneName}] at [${hit.position.map((n) => n.toFixed(2)).join(', ')}]`;
      onAppendToFindings(line);
    }
    clearMarkerErrors();
  };

  const handleDeleteMarker = (id: string) => {
    setMarkers((prev) => prev.filter((m) => m.id !== id));
  };

  const handleUpdateMarker = (id: string, updates: Partial<InjuryMarker>) => {
    setMarkers((prev) =>
      prev.map((m) => {
        if (m.id !== id) return m;
        const next = { ...m, ...updates };
        if ('description' in updates && setFieldErrors && fieldErrors?.[`description:${id}`]) {
          setFieldErrors((errs) => ({ ...errs, [`description:${id}`]: '' }));
        }
        return next;
      }),
    );
  };

  /** Zone label per agreed priority: bodyRegionId → zoneId (canonical, then legacy). */
  const zoneLabelOf = (marker: InjuryMarker) => markerZoneLabel(bodyRegions, marker);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        {!readOnly && (
          <Button
            type="button"
            variant={isAddMode ? 'primary' : 'secondary'}
            onClick={() => setIsAddMode((prev) => !prev)}
            icon={isAddMode ? 'target' : 'plus'}
          >
            {isAddMode ? 'Adding marker: click horse' : 'Add injury marker'}
          </Button>
        )}
        <span className="text-xs text-[var(--color-text-secondary)]">
          {markers.length} marker{markers.length === 1 ? '' : 's'} placed
        </span>
      </div>

      <UrgentHorseInjuryViewer
        injuries={markers.map((m) => ({ ...m, label: zoneLabelOf(m) }))}
        isAddMode={isAddMode}
        readOnly={readOnly}
        onPointerHit={handleInjuryHit}
        onDeleteInjury={handleDeleteMarker}
        onUpdateInjury={handleUpdateMarker}
      />

      <div className="space-y-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3">
        <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2">
          <h4 className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
            <Icon name="alert-triangle" size={14} className="text-[var(--color-danger)]" />
            <span>Marked Injuries ({markers.length})</span>
          </h4>
          {markers.length > 0 && !readOnly && (
            <Button
              type="button"
              variant="tertiary"
              size="sm"
              onClick={() => setMarkers([])}
              className="text-xs text-[var(--color-danger)] hover:bg-[var(--color-danger-subtle)]"
            >
              Clear all markers
            </Button>
          )}
        </div>

        {markers.length === 0 ? (
          <p className="py-4 text-center text-xs text-[var(--color-text-muted)]">
            No injury markers placed yet. Click &quot;Add injury marker&quot; above and click on the 3D
            horse model to mark injuries.
          </p>
        ) : (
          <div className="space-y-3">
            {markers.map((marker, index) => {
              const zoneName = zoneLabelOf(marker) ?? 'Unspecified';
              return (
                <div
                  key={marker.id}
                  className="flex flex-col gap-2 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2.5"
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="flex h-5 w-5 items-center justify-center rounded-full bg-[var(--color-danger)] text-[10px] font-bold text-[var(--color-text-inverse)]">
                        {index + 1}
                      </span>
                      <span className="rounded bg-[var(--color-surface-muted)] px-1.5 py-0.5 text-[10px] font-medium text-[var(--color-text-secondary)]">
                        Zone: {zoneName}
                      </span>
                    </div>
                    {!readOnly && (
                      <Button
                        type="button"
                        variant="tertiary"
                        size="sm"
                        icon="x"
                        onClick={() => handleDeleteMarker(marker.id)}
                        className="h-7 text-xs text-[var(--color-danger)] hover:bg-[var(--color-danger-subtle)]"
                        title="Delete this marker"
                      >
                        Delete
                      </Button>
                    )}
                  </div>

                  <div>
                    <label
                      htmlFor={`marker-note-${marker.id}`}
                      className="mb-1 block text-[11px] font-medium text-[var(--color-text-secondary)]"
                    >
                      Clinical note (Optional)
                    </label>
                    <Input
                      id={`marker-note-${marker.id}`}
                      type="text"
                      placeholder="Optional: enter specific details or leave blank to use region name"
                      value={marker.description || ''}
                      disabled={readOnly}
                      onChange={(e) => handleUpdateMarker(marker.id, { description: e.target.value })}
                      className="text-xs"
                    />
                    {fieldErrors?.[`description:${marker.id}`] && (
                      <p className="mt-1 text-xs text-[var(--color-danger)]">
                        {fieldErrors[`description:${marker.id}`]}
                      </p>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
        {fieldErrors?.injuryMarkers && (
          <p className="mt-1 text-xs font-medium text-[var(--color-danger)]">
            {fieldErrors.injuryMarkers}
          </p>
        )}
      </div>
    </div>
  );
}
