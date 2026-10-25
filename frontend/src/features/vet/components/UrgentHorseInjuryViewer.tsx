'use client';

import React, { Suspense, useMemo, useRef } from 'react';
import { Canvas } from '@react-three/fiber';
import { Html, OrbitControls, useGLTF } from '@react-three/drei';
import * as THREE from 'three';
import type { ThreeEvent } from '@react-three/fiber';

import { zoneForPoint, zoneById } from '@/features/vet/lib/injuryZones';

export interface InjuryMarkerData {
  id: string;
  position: [number, number, number];
  normal?: [number, number, number];
  zoneId?: string;
  label?: string;
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
}

const MODEL_URL = '/models/urgent-horse.glb';

function HorseModel({
  isAddMode,
  readOnly,
  onPointerHit,
}: Pick<UrgentHorseInjuryViewerProps, 'isAddMode' | 'readOnly' | 'onPointerHit'>) {
  const groupRef = useRef<THREE.Group>(null);
  const { scene } = useGLTF(MODEL_URL);

  const transform = useMemo(() => {
    scene.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        child.castShadow = true;
        child.receiveShadow = true;
      }
    });

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
      normal = [
        Number(worldNormal.x.toFixed(4)),
        Number(worldNormal.y.toFixed(4)),
        Number(worldNormal.z.toFixed(4)),
      ];
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
    <group ref={groupRef} scale={transform.scale} position={transform.position}>
      <primitive
        object={scene}
        onClick={handleClick}
        onPointerOver={handlePointerOver}
        onPointerOut={handlePointerOut}
      />
    </group>
  );
}

function Marker({ injury }: { injury: InjuryMarkerData }) {
  return (
    <group position={injury.position}>
      <mesh>
        <sphereGeometry args={[0.045, 16, 16]} />
        <meshStandardMaterial color="#ef4444" emissive="#ef4444" emissiveIntensity={0.8} />
      </mesh>
      {injury.label ? (
        <Html distanceFactor={8} position={[0, 0.12, 0]} center>
          <div className="rounded bg-red-600 px-1.5 py-0.5 text-[10px] font-semibold text-white shadow">
            {injury.label}
          </div>
        </Html>
      ) : null}
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
}: UrgentHorseInjuryViewerProps) {
  return (
    <ViewerErrorBoundary fallback={<Fallback2D injuries={injuries} />}>
      <div className="h-[420px] w-full overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)]">
        <Canvas camera={{ position: [3.2, 1.6, 3.2], fov: 45 }} shadows>
          <ambientLight intensity={1.2} />
          <directionalLight position={[6, 12, 6]} intensity={1.8} castShadow />
          <directionalLight position={[-6, 6, -6]} intensity={0.8} />
          <Suspense fallback={<Html center><span className="text-xs text-[var(--color-text-muted)]">Loading 3D horse…</span></Html>}>
            <HorseModel isAddMode={isAddMode} readOnly={readOnly} onPointerHit={onPointerHit} />
            {injuries.map((injury) => (
              <Marker key={injury.id} injury={injury} />
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
