'use client';

import dynamic from 'next/dynamic';
import { useState, type Dispatch, type SetStateAction } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Input, Select } from '@/components/ui/Input';
import { zoneById, worldToNormalized } from '@/features/vet/lib/injuryZones';
import type { HorseBodyRegion } from '@/features/health/types';
import type { CreateInjuryMarkerRequest } from '../../types';
import { INJURY_SEVERITY_OPTIONS } from './constants';

const UrgentHorseInjuryViewer = dynamic(
  () => import('@/features/vet/components/UrgentHorseInjuryViewer'),
  { ssr: false },
);

export interface InjuryMarker {
  id: string;
  position: [number, number, number];
  normal: [number, number, number];
  zoneId?: string;
  /** Free-text injury type required by the backend. */
  injuryType: string;
  /** Maps to a real horse body region from the catalog. */
  bodyRegionId?: number | null;
  description?: string;
  severity: string;
}

export function buildInjuryMarkerRequests(markers: InjuryMarker[]): CreateInjuryMarkerRequest[] {
  return markers.map((m) => ({
    injuryType: m.injuryType.trim() || 'UNSPECIFIED',
    bodyRegionId: m.bodyRegionId ?? 1,
    severity: m.severity || 'MODERATE',
    description: m.description?.trim() || undefined,
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
    meshName: 'Horse.001_M_Body_0',
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

  const handleInjuryHit = (hit: {
    position: [number, number, number];
    normal: [number, number, number];
    zoneId: string;
  }) => {
    const id =
      typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : `marker-${Date.now()}`;
    const zone = hit.zoneId ? zoneById(hit.zoneId) : undefined;
    const zoneName = zone?.label ?? 'Unspecified';
    const newMarker: InjuryMarker = {
      id,
      position: hit.position,
      normal: hit.normal,
      zoneId: hit.zoneId,
      injuryType: '',
      bodyRegionId: bodyRegions.length ? bodyRegions[0].id : null,
      description: '',
      severity: 'MODERATE',
    };
    setMarkers((prev) => [...prev, newMarker]);
    if (onAppendToFindings) {
      const line = `- 3D injury marker [${zoneName}] at [${hit.position.map((n) => n.toFixed(2)).join(', ')}]`;
      onAppendToFindings(line);
    }
    if (setFieldErrors && fieldErrors?.injuryMarkers) {
      setFieldErrors((prev) => ({ ...prev, injuryMarkers: '' }));
    }
  };

  const handleDeleteMarker = (id: string) => {
    setMarkers((prev) => prev.filter((m) => m.id !== id));
  };

  const handleUpdateMarker = (id: string, updates: Partial<InjuryMarker>) => {
    setMarkers((prev) =>
      prev.map((m) => {
        if (m.id !== id) return m;
        const next = { ...m, ...updates };
        if ('injuryType' in updates && setFieldErrors && fieldErrors?.[`injuryType:${id}`]) {
          setFieldErrors((errs) => ({ ...errs, [`injuryType:${id}`]: '' }));
        }
        if ('bodyRegionId' in updates && setFieldErrors && fieldErrors?.[`bodyRegionId:${id}`]) {
          setFieldErrors((errs) => ({ ...errs, [`bodyRegionId:${id}`]: '' }));
        }
        return next;
      }),
    );
  };

  const regionOptions = bodyRegions.length ? bodyRegions : [];

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
        injuries={markers}
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
              const zoneName = (marker.zoneId && zoneById(marker.zoneId)?.label) || 'Unspecified';
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
                      <span className="text-xs font-semibold text-[var(--color-text-primary)]">
                        {marker.injuryType || `Marker ${index + 1}`}
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

                  <div className="grid gap-2 sm:grid-cols-2">
                    <div>
                      <label
                        htmlFor={`marker-type-${marker.id}`}
                        className="mb-1 block text-[11px] font-medium text-[var(--color-text-secondary)]"
                      >
                        Injury type <span className="text-[var(--color-danger)]">*</span>
                      </label>
                      <Input
                        id={`marker-type-${marker.id}`}
                        type="text"
                        placeholder="e.g. SDFT strain, laceration, cellulitis"
                        value={marker.injuryType}
                        disabled={readOnly}
                        onChange={(e) => handleUpdateMarker(marker.id, { injuryType: e.target.value })}
                        className="text-xs"
                      />
                      {fieldErrors?.[`injuryType:${marker.id}`] && (
                        <p className="mt-1 text-xs text-[var(--color-danger)]">
                          {fieldErrors[`injuryType:${marker.id}`]}
                        </p>
                      )}
                    </div>

                    <div>
                      <label
                        htmlFor={`marker-region-${marker.id}`}
                        className="mb-1 block text-[11px] font-medium text-[var(--color-text-secondary)]"
                      >
                        Body region <span className="text-[var(--color-danger)]">*</span>
                      </label>
                      <Select
                        id={`marker-region-${marker.id}`}
                        value={marker.bodyRegionId ?? ''}
                        disabled={readOnly || regionOptions.length === 0}
                        onChange={(e) =>
                          handleUpdateMarker(marker.id, { bodyRegionId: Number(e.target.value) })
                        }
                        className="text-xs"
                      >
                        {regionOptions.length === 0 && (
                          <option value="">Loading regions…</option>
                        )}
                        {regionOptions.map((region) => (
                          <option key={region.id} value={region.id}>
                            {region.displayName}
                          </option>
                        ))}
                      </Select>
                      {fieldErrors?.[`bodyRegionId:${marker.id}`] && (
                        <p className="mt-1 text-xs text-[var(--color-danger)]">
                          {fieldErrors[`bodyRegionId:${marker.id}`]}
                        </p>
                      )}
                    </div>

                    <div>
                      <label
                        htmlFor={`marker-severity-${marker.id}`}
                        className="mb-1 block text-[11px] font-medium text-[var(--color-text-secondary)]"
                      >
                        Severity
                      </label>
                      <Select
                        id={`marker-severity-${marker.id}`}
                        value={marker.severity}
                        disabled={readOnly}
                        onChange={(e) => handleUpdateMarker(marker.id, { severity: e.target.value })}
                        className="text-xs"
                      >
                        {INJURY_SEVERITY_OPTIONS.map((severity) => (
                          <option key={severity} value={severity}>
                            {severity}
                          </option>
                        ))}
                      </Select>
                    </div>

                    <div>
                      <label
                        htmlFor={`marker-note-${marker.id}`}
                        className="mb-1 block text-[11px] font-medium text-[var(--color-text-secondary)]"
                      >
                        Clinical note
                      </label>
                      <Input
                        id={`marker-note-${marker.id}`}
                        type="text"
                        placeholder="Enter clinical observation, swelling, pain response, wound details…"
                        value={marker.description || ''}
                        disabled={readOnly}
                        onChange={(e) => handleUpdateMarker(marker.id, { description: e.target.value })}
                        className="text-xs"
                      />
                    </div>
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
