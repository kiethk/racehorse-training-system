'use client';

import dynamic from 'next/dynamic';
import { useMemo, useState, type Dispatch, type SetStateAction } from 'react';
import { Button } from '@/components/ui/Button';
import { Spinner } from '@/components/ui/Spinner';
import {
  isNormalizedPoint,
  markerZoneLabel,
  matchBodyRegion,
  normalizedToWorld,
  regionKeyLabel,
  resolveInjuryNormal,
  resolveInjuryWorldPosition,
  worldToNormalized,
  zoneById,
  zoneForRegionKey,
} from '@/features/vet/lib/injuryZones';
import type { InjuryMarkerData } from '@/features/vet/components/UrgentHorseInjuryViewer';
import type { HorseBodyRegion, InjuryRecord } from '@/features/health/types';
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
  activeInjuries?: InjuryRecord[];
  resolvedConditionIds?: number[];
  onToggleResolveCondition?: (id: number) => void;
  readOnly?: boolean;
  onAppendToFindings?: (line: string) => void;
  fieldErrors?: Record<string, string>;
  setFieldErrors?: Dispatch<SetStateAction<Record<string, string>>>;
}

export function InjuryMarkerPanel(props: InjuryMarkerPanelProps) {
  const {
    markers,
    setMarkers,
    bodyRegions,
    activeInjuries = [],
    resolvedConditionIds = [],
    onToggleResolveCondition,
    readOnly,
    onAppendToFindings,
    fieldErrors,
    setFieldErrors,
  } = props;
  const [isAddMode, setIsAddMode] = useState(!readOnly);
  const [modelReady, setModelReady] = useState(false);

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

  const handleToggleResolve = (markerId: string) => {
    if (!markerId.startsWith('active-') || !onToggleResolveCondition) return;
    const rawId = Number(markerId.slice('active-'.length));
    if (!Number.isNaN(rawId)) {
      onToggleResolveCondition(rawId);
    }
  };

  const activeMarkerData = useMemo<InjuryMarkerData[]>(() => {
    return activeInjuries.map((inj) => {
      const region = bodyRegions.find((r) => r.id === inj.bodyRegionId);
      const regionKey = inj.zoneId ?? region?.modelNodeKey ?? region?.code;
      const hasCoords = inj.positionX != null || inj.localX != null;
      let worldPos: [number, number, number];
      let needsSurfaceSnap: boolean;
      if (hasCoords) {
        worldPos = resolveInjuryWorldPosition(inj);
        needsSurfaceSnap =
          inj.normalX == null ||
          isNormalizedPoint({
            x: Number(inj.positionX ?? 0),
            y: Number(inj.positionY ?? 0),
            z: Number(inj.positionZ ?? 0),
          });
      } else {
        const geoZone = zoneForRegionKey(regionKey);
        const anchor = (geoZone && zoneById(geoZone)?.anchor) || { x: 0.5, y: 0.45 };
        const sideX = regionKey?.includes('right') ? 0.75 : 0.25;
        worldPos = normalizedToWorld({ x: sideX, y: 1 - anchor.y, z: 1 - anchor.x });
        needsSurfaceSnap = true;
      }
      const normal =
        inj.normalX != null && inj.normalY != null && inj.normalZ != null
          ? resolveInjuryNormal(
              [Number(inj.normalX), Number(inj.normalY), Number(inj.normalZ)],
              worldPos,
            )
          : resolveInjuryNormal(undefined, worldPos);
      const zoneLabel = markerZoneLabel(bodyRegions, {
        bodyRegionId: inj.bodyRegionId,
        zoneId: inj.zoneId,
      });

      return {
        id: `active-${inj.id}`,
        position: worldPos,
        normal,
        needsSurfaceSnap,
        zoneId: inj.zoneId ?? undefined,
        label: zoneLabel || inj.injuryType,
        severity: inj.severity,
        status: inj.status,
        diagnosedAt: inj.diagnosedAt,
        description: inj.description,
        resolved: resolvedConditionIds.includes(inj.id),
        canResolve: true,
      };
    });
  }, [activeInjuries, bodyRegions, resolvedConditionIds]);

  const newMarkerData = useMemo<InjuryMarkerData[]>(
    () =>
      markers.map((m) => ({
        ...m,
        label: markerZoneLabel(bodyRegions, m),
      })),
    [markers, bodyRegions],
  );

  const combinedMarkers = useMemo(
    () => [...activeMarkerData, ...newMarkerData],
    [activeMarkerData, newMarkerData],
  );

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
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
          {markers.length > 0 && !readOnly && (
            <Button
              type="button"
              variant="tertiary"
              size="sm"
              onClick={() => setMarkers([])}
              className="text-xs text-[var(--color-danger)] hover:bg-[var(--color-danger-soft)]"
            >
              Clear all markers
            </Button>
          )}
        </div>
        <span className="text-xs text-[var(--color-text-secondary)]">
          {activeInjuries.length > 0
            ? `${activeInjuries.length} active · ${markers.length} new`
            : `${markers.length} marker${markers.length === 1 ? '' : 's'} placed`}
        </span>
      </div>

      <div className="relative">
        <UrgentHorseInjuryViewer
          injuries={combinedMarkers}
          isAddMode={isAddMode}
          readOnly={readOnly}
          onPointerHit={handleInjuryHit}
          onDeleteInjury={handleDeleteMarker}
          onUpdateInjury={handleUpdateMarker}
          onToggleResolve={handleToggleResolve}
          onModelReady={() => setModelReady(true)}
        />
        {!modelReady && (
          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-2 rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)]">
            <Spinner />
            <span className="text-xs text-[var(--color-text-muted)]">Loading 3D model…</span>
          </div>
        )}
      </div>

      {fieldErrors?.injuryMarkers && (
        <p className="mt-1 text-xs font-medium text-[var(--color-danger)]">
          {fieldErrors.injuryMarkers}
        </p>
      )}
    </div>
  );
}
