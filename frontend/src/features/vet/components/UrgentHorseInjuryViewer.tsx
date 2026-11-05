'use client';

import React, { Suspense, useMemo, useRef, useState, useEffect, useCallback } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls, useGLTF, Html } from '@react-three/drei';
import * as THREE from 'three';
import type { ThreeEvent } from '@react-three/fiber';

import {
  zoneForPoint,
  zoneById,
  resolveInjuryWorldPosition,
  resolveInjuryNormal,
  isNormalizedPoint,
} from '@/features/vet/lib/injuryZones';
import { Button } from '@/components/ui/Button';
import { Icon, IconButton, Textarea } from '@/components/ui';
import { formatDate } from '@/lib/display';

export interface InjuryMarkerData {
  id: string;
  position: [number, number, number];
  normal?: [number, number, number];
  zoneId?: string;
  label?: string;
  severity?: string;
  status?: string;
  diagnosedAt?: string;
  description?: string;
  /**
   * Callers that pre-resolve a legacy/seeded marker (normalized DB coords)
   * into world coordinates set this so the Marker still surface-snaps;
   * otherwise the presence of a computed normal skips the snap.
   */
  needsSurfaceSnap?: boolean;
}

interface UrgentHorseInjuryViewerProps {
  injuries: InjuryMarkerData[];
  isAddMode: boolean;
  readOnly?: boolean;
  onPointerHit?: (hit: {
    position: [number, number, number];
    normal: [number, number, number];
    zoneId: string;
  }) => void;
  onDeleteInjury?: (id: string) => void;
  onUpdateInjury?: (id: string, updates: Partial<InjuryMarkerData>) => void;
}

const MODEL_URL = '/models/urgent-horse.glb';

function HorseModel({
  isAddMode,
  readOnly,
  onPointerHit,
  groupRef,
  onReady,
}: Pick<UrgentHorseInjuryViewerProps, 'isAddMode' | 'readOnly' | 'onPointerHit'> & {
  groupRef?: React.RefObject<THREE.Group | null>;
  onReady?: () => void;
}) {
  const localRef = useRef<THREE.Group>(null);
  const targetRef = groupRef || localRef;
  const { scene } = useGLTF(MODEL_URL);

  // Signal once the model is committed, so markers mount a tick later and the
  // drei <Html> portal does not race the Suspense swap (it flushed empty).
  useEffect(() => {
    onReady?.();
  }, [onReady]);

  const transform = useMemo(() => {
    const box = new THREE.Box3().setFromObject(scene);
    const size = new THREE.Vector3();
    box.getSize(size);
    const center = new THREE.Vector3();
    box.getCenter(center);

    const targetHeight = 2.4;
    const height = Math.max(size.y, 0.001);
    const scale = targetHeight / height;

    return {
      scale,
      position: [
        -center.x * scale,
        -box.min.y * scale - 1.3,
        -center.z * scale,
      ] as [number, number, number],
    };
  }, [scene]);

  const handleClick = (event: ThreeEvent<PointerEvent>) => {
    if (!isAddMode || readOnly) return;
    event.stopPropagation();

    const hitPoint = event.point;
    let normal: [number, number, number] = [0, 1, 0];
    if (event.face && event.object) {
      const normalMatrix = new THREE.Matrix3().getNormalMatrix(event.object.matrixWorld);
      const worldNormal = event.face.normal.clone().applyMatrix3(normalMatrix).normalize();
      normal = resolveInjuryNormal(
        [
          Number(worldNormal.x.toFixed(4)),
          Number(worldNormal.y.toFixed(4)),
          Number(worldNormal.z.toFixed(4)),
        ],
        [hitPoint.x, hitPoint.y, hitPoint.z],
      );
    } else {
      normal = resolveInjuryNormal(undefined, [hitPoint.x, hitPoint.y, hitPoint.z]);
    }

    const local = {
      x: (hitPoint.x - transform.position[0]) / transform.scale,
      y: (hitPoint.y - transform.position[1]) / transform.scale,
      z: (hitPoint.z - transform.position[2]) / transform.scale,
    };
    const zoneId = zoneForPoint(local);

    onPointerHit?.({
      position: [
        Number(hitPoint.x.toFixed(4)),
        Number(hitPoint.y.toFixed(4)),
        Number(hitPoint.z.toFixed(4)),
      ],
      normal,
      zoneId,
    });
  };

  const handlePointerOver = (event: ThreeEvent<PointerEvent>) => {
    event.stopPropagation();
    if (isAddMode && !readOnly) {
      document.body.style.cursor = 'crosshair';
    } else {
      document.body.style.cursor = 'grab';
    }
  };

  const handlePointerOut = () => {
    document.body.style.cursor = 'auto';
  };

  return (
    <group ref={targetRef} scale={transform.scale} position={transform.position}>
      <primitive
        object={scene}
        onClick={handleClick}
        onPointerOver={handlePointerOver}
        onPointerOut={handlePointerOut}
      />
    </group>
  );
}

/**
 * Snaps a target position onto the outer surface vertex of the horse model.
 * Tries multiple probe directions and keeps the surface hit closest to the
 * target, so a stale/normalized mapping still lands on the body instead of
 * floating beside it. Runs at most once per legacy/normalized marker on mount.
 */
function snapToHorseSurface(
  pos: THREE.Vector3,
  suggestedNormal: THREE.Vector3,
  horseGroup: THREE.Group,
  raycaster: THREE.Raycaster,
): { point: THREE.Vector3; normal: THREE.Vector3 } | null {
  const SNAP_RADIUS = 1.2;
  const probe = (origin: THREE.Vector3, dir: THREE.Vector3) => {
    raycaster.set(origin, dir);
    const hit = raycaster.intersectObject(horseGroup, true)[0];
    return hit && hit.face ? { point: hit.point, hit } : null;
  };

  // Candidates: along the suggested normal, both lateral sides, both ends, above.
  const norm = suggestedNormal.clone().normalize();
  const candidates: Array<{ origin: THREE.Vector3; dir: THREE.Vector3 }> = [
    { origin: pos.clone().addScaledVector(norm, 2), dir: norm.clone().negate() },
    { origin: new THREE.Vector3(2.5, pos.y, pos.z), dir: new THREE.Vector3(-1, 0, 0) },
    { origin: new THREE.Vector3(-2.5, pos.y, pos.z), dir: new THREE.Vector3(1, 0, 0) },
    { origin: new THREE.Vector3(pos.x, pos.y, 2.5), dir: new THREE.Vector3(0, 0, -1) },
    { origin: new THREE.Vector3(pos.x, pos.y, -2.5), dir: new THREE.Vector3(0, 0, 1) },
    { origin: new THREE.Vector3(pos.x, pos.y + 2.5, pos.z), dir: new THREE.Vector3(0, -1, 0) },
  ];

  let best: { point: THREE.Vector3; normal: THREE.Vector3 } | null = null;
  for (const { origin, dir } of candidates) {
    const found = probe(origin, dir);
    if (!found) continue;
    if (found.point.distanceTo(pos) >= SNAP_RADIUS) continue;
    const normalMatrix = new THREE.Matrix3().getNormalMatrix(found.hit.object.matrixWorld);
    const worldNormal = found.hit.face!.normal.clone().applyMatrix3(normalMatrix).normalize();
    const surfacePoint = found.point.clone().addScaledVector(worldNormal, 0.02);
    if (!best || surfacePoint.distanceTo(pos) < best.point.distanceTo(pos)) {
      best = { point: surfacePoint, normal: worldNormal };
    }
  }
  return best;
}

/**
 * 3D Marker with Integrated Drei <Html> Annotation:
 * - Native 3D multi-directional projection: tag and anchor pin move in 3D with the horse as camera orbits.
 * - Single anchor: glowing red dot with sharp outer ring and diagonal leader line leading out to the tag.
 * - No standalone 3D sphere: the SVG target pin itself serves as the sole, crisp anatomical pinpoint.
 * - Smooth opacity transition when orbiting to the opposite side of the horse body.
 */
function Marker({
  injury,
  horseGroupRef,
  isAddMode,
  readOnly,
  onDelete,
  onUpdate,
}: {
  injury: InjuryMarkerData;
  horseGroupRef: React.RefObject<THREE.Group | null>;
  isAddMode?: boolean;
  readOnly?: boolean;
  onDelete?: (id: string) => void;
  onUpdate?: (id: string, updates: Partial<InjuryMarkerData>) => void;
}) {
  const groupRef = useRef<THREE.Group>(null);
  const tagWrapperRef = useRef<HTMLDivElement>(null);
  const snapAttemptedRef = useRef(false);
  const [open, setOpen] = useState(false);

  const initialWorldPos = useMemo<[number, number, number]>(() => {
    return resolveInjuryWorldPosition(injury.position);
  }, [injury.position]);

  const resolvedNormal = useMemo<[number, number, number]>(() => {
    return resolveInjuryNormal(injury.normal, initialWorldPos);
  }, [injury.normal, initialWorldPos]);

  const currentNormal = useRef<THREE.Vector3>(
    new THREE.Vector3(resolvedNormal[0], resolvedNormal[1], resolvedNormal[2]),
  );

  const markerWorldPos = useRef(new THREE.Vector3());
  const ndcPos = useRef(new THREE.Vector3());
  const raycaster = useRef(new THREE.Raycaster());

  useFrame(({ camera }) => {
    if (!groupRef.current || !tagWrapperRef.current) return;

    // Single-shot surface snap on first frame ONLY for legacy/normalized coordinates
    if (!snapAttemptedRef.current) {
      snapAttemptedRef.current = true;
      if (horseGroupRef?.current && (injury.needsSurfaceSnap || !injury.normal || isNormalizedPoint(injury.position))) {
        groupRef.current.getWorldPosition(markerWorldPos.current);
        const snapped = snapToHorseSurface(
          markerWorldPos.current,
          currentNormal.current,
          horseGroupRef.current,
          raycaster.current,
        );
        if (snapped) {
          groupRef.current.position.copy(snapped.point);
          currentNormal.current.copy(snapped.normal);
        }
      }
    }

    groupRef.current.getWorldPosition(markerWorldPos.current);
    ndcPos.current.copy(markerWorldPos.current).project(camera);

    // Behind camera plane or outside clip frustum [-1, 1]
    const behindCamera = ndcPos.current.z > 1 || ndcPos.current.z < -1;
    if (behindCamera) {
      tagWrapperRef.current.style.display = 'none';
      return;
    }

    tagWrapperRef.current.style.display = 'block';

    // Multi-directional side detection:
    // Determine whether the marker is facing the camera or occluded by the horse's solid body
    const mx = markerWorldPos.current.x;
    const my = markerWorldPos.current.y;
    const mz = markerWorldPos.current.z;
    const cx = camera.position.x;
    const cy = camera.position.y;
    const cz = camera.position.z;

    // Top of the horse (withers, spine, crest, head top) is visible from almost all angles above ground
    const isTop = my > 0.35 && cy > -0.5;

    let isOppositeSide = false;
    if (!isTop) {
      // Lateral flank check:
      // Marker on left flank (mx > 0.08) dims only when camera orbits to far right (cx < -0.6)
      // Marker on right flank (mx < -0.08) dims only when camera orbits to far left (cx > 0.6)
      if (mx > 0.08 && cx < -0.6) {
        isOppositeSide = true;
      } else if (mx < -0.08 && cx > 0.6) {
        isOppositeSide = true;
      }

      // Longitudinal check (front head/chest vs rear rump):
      // In urgent-horse.glb: +Z is Front/Nose, -Z is Rear/Tail
      if (mz > 0.5 && cz < -1.6) {
        isOppositeSide = true;
      } else if (mz < -0.5 && cz > 1.6) {
        isOppositeSide = true;
      }
    }

    tagWrapperRef.current.style.opacity = isOppositeSide ? '0.2' : '1';
    tagWrapperRef.current.style.pointerEvents = isOppositeSide ? 'none' : 'auto';
  });

  const label = injury.label || (injury.zoneId && zoneById(injury.zoneId)?.label) || 'INJURY';
  const hasDetail = Boolean(
    injury.severity ||
      injury.status ||
      injury.diagnosedAt ||
      injury.description ||
      (!readOnly && onUpdate),
  );

  return (
    <group position={initialWorldPos} ref={groupRef}>
      <Html
        position={[0, 0, 0]}
        style={{
          pointerEvents: 'none',
        }}
      >
        <div
          ref={tagWrapperRef}
          className="pointer-events-auto relative select-none"
          style={{ willChange: 'opacity, transform', transition: 'opacity 0.2s ease-out' }}
        >
          {/* Medical SVG target pin: glowing red dot with outer ring and diagonal leader line */}
          <svg
            className="absolute left-0 top-0 overflow-visible pointer-events-none"
            width="1"
            height="1"
            aria-hidden
          >
            {/* Outer soft glowing aura */}
            <circle
              cx="0"
              cy="0"
              r="11"
              fill="none"
              stroke="var(--color-danger)"
              strokeWidth="0.75"
              opacity="0.35"
            />
            {/* Middle sharp ring */}
            <circle
              cx="0"
              cy="0"
              r="7"
              fill="none"
              stroke="var(--color-danger)"
              strokeWidth="1.5"
              opacity="0.85"
            />
            {/* Inner solid red dot */}
            <circle cx="0" cy="0" r="3.5" fill="var(--color-danger)" />
            {/* Diagonal leader line out to the tag button */}
            <line x1="0" y1="0" x2="34" y2="-28" stroke="var(--color-danger)" strokeWidth="1.5" />
          </svg>

          {/* Clickable hotspot over the target pin dot so user can click the dot itself to open details */}
          <IconButton
            icon="injury-dot"
            label={label}
            aria-expanded={open}
            variant="tertiary"
            size="sm"
            className="absolute -translate-x-1/2 -translate-y-1/2 h-6 w-6 rounded-full cursor-pointer bg-transparent p-0 hover:bg-transparent focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
            style={{ left: 0, top: 0 }}
            onClick={(e) => {
              if (isAddMode) return;
              e.stopPropagation();
              setOpen((o) => !o);
            }}
          />

          {/* Small tag placed at the end of the leader line */}
          <div
            className="absolute -translate-y-1/2 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-popover)]"
            style={{ left: 34, top: -28 }}
          >
            <div className="flex items-center">
              <Button
                type="button"
                variant="tertiary"
                size="sm"
                aria-expanded={open}
                onClick={(e) => {
                  if (isAddMode) return;
                  e.stopPropagation();
                  setOpen((o) => !o);
                }}
                className="flex items-center gap-1 px-1.5 py-0.5 text-[10px] font-semibold"
              >
                <span className="h-1.5 w-1.5 rounded-full bg-[var(--color-danger)]" aria-hidden />
                {label}
                <Icon
                  name={open ? 'chevron-down' : 'chevron-right'}
                  size={10}
                  className="text-[var(--color-text-muted)]"
                />
              </Button>
              {!readOnly && onDelete && (
                <IconButton
                  label="Delete this marker"
                  icon="x"
                  size="sm"
                  className="mr-1 h-5 w-5 rounded text-[var(--color-text-muted)] hover:bg-[var(--color-danger-subtle)] hover:text-[var(--color-danger)]"
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete(injury.id);
                  }}
                />
              )}
            </div>
            {open && hasDetail && (
              <div className="min-w-44 max-w-56 space-y-1.5 border-t border-[var(--color-border)] px-2 py-1.5 text-[10px] text-[var(--color-text-secondary)]">
                {!readOnly && onUpdate ? (
                  <div className="space-y-1">
                    <label className="block text-[9px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
                      Ghi chú lâm sàng / Note:
                    </label>
                    <Textarea
                      rows={2}
                      placeholder="Nhập ghi chú chấn thương..."
                      value={injury.description || ''}
                      onChange={(e) => onUpdate(injury.id, { description: e.target.value })}
                      className="w-full resize-none rounded border border-[var(--color-border)] bg-[var(--color-surface)] p-1 text-[10px] text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                ) : (
                  injury.description && (
                    <div>
                      <span className="text-[9px] font-bold uppercase tracking-wider text-[var(--color-text-muted)] block">
                        Note:
                      </span>
                      <p className="leading-snug">{injury.description}</p>
                    </div>
                  )
                )}
                {injury.severity && (
                  <div>
                    Severity: <strong>{injury.severity}</strong>
                  </div>
                )}
                {injury.status && (
                  <div>
                    Status: <strong>{injury.status}</strong>
                  </div>
                )}
                {injury.diagnosedAt && <div>Diagnosed: {formatDate(injury.diagnosedAt)}</div>}
              </div>
            )}
          </div>
        </div>
      </Html>
    </group>
  );
}

class ViewerErrorBoundary extends React.Component<{ children: React.ReactNode; fallback: React.ReactNode }, { hasError: boolean }> {
  constructor(props: { children: React.ReactNode; fallback: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }
  static getDerivedStateFromError() {
    return { hasError: true };
  }
  render() {
    if (this.state.hasError) return this.props.fallback;
    return this.props.children;
  }
}

function Fallback2D({ injuries }: { injuries: InjuryMarkerData[] }) {
  const W = 400;
  const H = 200;
  const counts: Record<string, number> = {};
  const pts = injuries.map((inj) => {
    const zone = (inj.zoneId && zoneById(inj.zoneId)) || undefined;
    const anchor = zone ? zone.anchor : null;
    if (zone) counts[zone.id] = (counts[zone.id] || 0) + 1;
    const idx = zone ? counts[zone.id] - 1 : 0;
    const px = anchor
      ? Math.round(anchor.x * W + (idx % 3) * 10 - 10)
      : Math.round(((inj.position[0] + 2) / 4) * W);
    const py = anchor
      ? Math.round(anchor.y * H + Math.floor(idx / 3) * 10 - 5)
      : Math.round((1 - (inj.position[1] + 0.5) / 3) * H);
    return { inj, px: Math.max(8, Math.min(W - 8, px)), py: Math.max(8, Math.min(H - 8, py)) };
  });
  return (
    <div className="flex h-[420px] w-full flex-col gap-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3">
      <span className="text-xs font-semibold text-[var(--color-text-muted)]">3D view unavailable — 2D marker diagram</span>
      <svg viewBox={`0 0 ${W} ${H}`} className="h-full w-full" role="img" aria-label="Horse body diagram with injury markers">
        {/* simplified horse silhouette (side view) */}
        <path
          d="M60,150 Q80,120 120,110 L170,100 Q200,95 230,100 L290,105 Q320,108 340,120 L360,140 Q370,150 360,160 L330,165 Q300,168 260,166 L200,164 Q160,162 120,160 L80,158 Q60,156 60,150 Z"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
          className="text-[var(--color-text-muted)]"
        />
        {pts.map(({ inj, px, py }, i) => (
          <g key={inj.id}>
            <circle cx={px} cy={py} r={6} fill="#ef4444" opacity={0.85} />
            <text x={px + 8} y={py + 3} fontSize="10" fill="currentColor" className="text-[var(--color-text-primary)]">
              {inj.label || `M${i + 1}`}
            </text>
          </g>
        ))}
      </svg>
      {injuries.length === 0 && (
        <span className="text-xs text-[var(--color-text-muted)]">No injury markers recorded.</span>
      )}
    </div>
  );
}

export default function UrgentHorseInjuryViewer({
  injuries,
  isAddMode,
  readOnly,
  onPointerHit,
  onDeleteInjury,
  onUpdateInjury,
}: UrgentHorseInjuryViewerProps) {
  const horseGroupRef = useRef<THREE.Group>(null);
  // Markers mount only after the model committed (see HorseModel effect) — a marker
  // <Html> portal racing the Suspense swap renders an empty overlay.
  const [markersReady, setMarkersReady] = useState(false);
  const handleModelReady = useCallback(() => setMarkersReady(true), []);

  return (
    <ViewerErrorBoundary fallback={<Fallback2D injuries={injuries} />}>
      <div className="relative h-[420px] w-full overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)]">
        <Canvas
          camera={{ position: [3.2, 1.6, 3.2], fov: 45 }}
          gl={{ powerPreference: 'high-performance', antialias: true }}
        >
          <ambientLight intensity={1.2} />
          <directionalLight position={[6, 12, 6]} intensity={1.8} />
          <directionalLight position={[-6, 6, -6]} intensity={0.8} />
          <Suspense fallback={null}>
            <HorseModel
              groupRef={horseGroupRef}
              isAddMode={isAddMode}
              readOnly={readOnly}
              onPointerHit={onPointerHit}
              onReady={handleModelReady}
            />
            {(markersReady ? injuries : []).map((injury) => (
              <Marker
                key={injury.id}
                injury={injury}
                horseGroupRef={horseGroupRef}
                isAddMode={isAddMode}
                readOnly={readOnly}
                onDelete={onDeleteInjury}
                onUpdate={onUpdateInjury}
              />
            ))}
            <gridHelper args={[14, 28, '#0d9488', '#1e293b']} position={[0, -1.3, 0]} />
          </Suspense>
          <OrbitControls enableDamping minDistance={1.2} maxDistance={9} />
        </Canvas>
      </div>
    </ViewerErrorBoundary>
  );
}

useGLTF.preload(MODEL_URL);
