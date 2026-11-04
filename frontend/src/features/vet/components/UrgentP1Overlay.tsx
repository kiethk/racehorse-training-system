'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { usePathname } from 'next/navigation';
import {
  Button,
  HorseAvatar,
  Icon,
  Modal,
  Panel,
  Pill,
  ProtectedImage,
} from '@/components/ui';
import { formatDateTime } from '@/lib/display';
import type { UrgentAssignmentAlert } from '@/features/admissions/types';
import {
  DEFAULT_SNOOZE_MS,
  DISMISSED_SESSION_KEY,
  getNextSnoozeExpiry,
  getEligibleP1Alerts,
  selectP1OverlayAlert,
  shouldPlayEmergencySound,
  SNOOZE_STORAGE_KEY,
} from '../lib/urgentOverlayUtils';

interface Props {
  alerts: UrgentAssignmentAlert[];
  onOpenCase: (alert: UrgentAssignmentAlert) => void;
  onSnooze: (alert: UrgentAssignmentAlert) => void;
}

function readStorageMap(key: string, isSession = false): Map<number, number> {
  if (typeof window === 'undefined') return new Map();
  try {
    const storage = isSession ? window.sessionStorage : window.localStorage;
    const raw = storage.getItem(key);
    if (!raw) return new Map();
    return new Map(JSON.parse(raw) as [number, number][]);
  } catch {
    return new Map();
  }
}

function writeStorageMap(key: string, map: Map<number, number>, isSession = false) {
  if (typeof window === 'undefined') return;
  try {
    const storage = isSession ? window.sessionStorage : window.localStorage;
    storage.setItem(key, JSON.stringify([...map.entries()]));
  } catch {
    // Ignore storage quota or permission errors
  }
}

/**
 * Plays a pulsing emergency beep pattern (3 pulses) using Web Audio API.
 * Suppressed if reduced motion is requested.
 */
function playEmergencyAlarm(audioCtxRef: React.MutableRefObject<AudioContext | null>) {
  if (typeof window === 'undefined') return;
  const prefersReducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
  if (!shouldPlayEmergencySound(prefersReducedMotion)) return;

  try {
    const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!AudioContextClass) return;

    if (!audioCtxRef.current || audioCtxRef.current.state === 'closed') {
      audioCtxRef.current = new AudioContextClass();
    }
    const ctx = audioCtxRef.current;
    if (ctx.state === 'suspended') {
      void ctx.resume();
    }

    const now = ctx.currentTime;
    // 3 short pulsing beeps at 880 Hz (A5)
    for (let i = 0; i < 3; i++) {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, now + i * 0.25);

      gain.gain.setValueAtTime(0, now + i * 0.25);
      gain.gain.linearRampToValueAtTime(0.2, now + i * 0.25 + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, now + i * 0.25 + 0.15);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now + i * 0.25);
      osc.stop(now + i * 0.25 + 0.16);
    }
  } catch {
    // Gracefully ignore audio autoplay policy errors
  }
}

export function UrgentP1Overlay({ alerts, onOpenCase, onSnooze }: Props) {
  const pathname = usePathname();
  const [snoozed, setSnoozed] = useState<Map<number, number>>(() => readStorageMap(SNOOZE_STORAGE_KEY));
  const [dismissed, setDismissed] = useState<Set<number>>(() => {
    const rawMap = readStorageMap(DISMISSED_SESSION_KEY, true);
    return new Set(rawMap.keys());
  });

  const audioCtxRef = useRef<AudioContext | null>(null);
  const alarmedRef = useRef<Set<number>>(new Set());

  // Clean up audio context on unmount
  useEffect(() => {
    const ctxRef = audioCtxRef;
    return () => {
      const ctx = ctxRef.current;
      if (ctx && ctx.state !== 'closed') {
        void ctx.close();
      }
    };
  }, []);

  const [currentIndex, setCurrentIndex] = useState(0);
  const eligibleAlerts = getEligibleP1Alerts(alerts, pathname, snoozed, dismissed);
  const safeIndex = Math.min(currentIndex, Math.max(0, eligibleAlerts.length - 1));
  const targetAlert = eligibleAlerts[safeIndex] ?? null;

  // Trigger emergency alarm once when an unhandled P1 alert first appears
  useEffect(() => {
    if (!targetAlert) return;
    if (alarmedRef.current.has(targetAlert.scheduleId)) return;

    alarmedRef.current.add(targetAlert.scheduleId);
    playEmergencyAlarm(audioCtxRef);
  }, [targetAlert]);

  const handleSnooze = useCallback(
    (alert: UrgentAssignmentAlert) => {
      const expiry = getNextSnoozeExpiry(Date.now(), DEFAULT_SNOOZE_MS);
      setSnoozed((prev) => {
        const next = new Map(prev);
        next.set(alert.scheduleId, expiry);
        writeStorageMap(SNOOZE_STORAGE_KEY, next);
        return next;
      });
      setCurrentIndex((prev) => Math.max(0, prev - 1));
      onSnooze(alert);
    },
    [onSnooze],
  );

  const handleDismiss = useCallback(
    (alert: UrgentAssignmentAlert) => {
      setDismissed((prev) => {
        const next = new Set(prev);
        next.add(alert.scheduleId);
        const dismissedMap = new Map([...next].map((id) => [id, Date.now()]));
        writeStorageMap(DISMISSED_SESSION_KEY, dismissedMap, true);
        return next;
      });
    },
    [],
  );

  const handleOpenCase = useCallback(
    (alert: UrgentAssignmentAlert) => {
      handleDismiss(alert);
      setCurrentIndex((prev) => Math.max(0, prev - 1));
      onOpenCase(alert);
    },
    [handleDismiss, onOpenCase],
  );

  if (!targetAlert) return null;

  return (
    <Modal
      open={Boolean(targetAlert)}
      onClose={() => handleSnooze(targetAlert)}
      size="lg"
      tone="danger"
      layer="confirm"
      title={
        <div className="flex items-center gap-2.5 text-[var(--color-danger)]">
          <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[var(--color-danger-light,#fee2e2)] text-[var(--color-danger)]">
            <Icon name="alert-triangle" size={18} />
          </span>
          <span className="font-bold">Urgent Case Assigned</span>
          {eligibleAlerts.length > 1 && (
            <span className="ml-1 rounded-full border border-[var(--color-danger)]/30 bg-[var(--color-danger-light,#fee2e2)] px-2 py-0.5 text-xs font-semibold text-[var(--color-danger)]">
              Case {safeIndex + 1} of {eligibleAlerts.length}
            </span>
          )}
        </div>
      }
      description="Immediate clinical attention required. The horse has been locked from all training activities."
      footer={
        <div className="flex w-full items-center justify-between gap-3">
          <Button
            variant="secondary"
            size="md"
            onClick={() => handleSnooze(targetAlert)}
          >
            Remind me in 10 minutes
          </Button>

          {eligibleAlerts.length > 1 && (
            <div className="flex items-center gap-1.5">
              <Button
                variant="secondary"
                size="sm"
                icon="chevron-left"
                disabled={safeIndex === 0}
                onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
                aria-label="Previous urgent case"
              >
                Prev
              </Button>
              <span className="px-1 text-xs font-semibold text-[var(--color-text-secondary)]">
                {safeIndex + 1} / {eligibleAlerts.length}
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={safeIndex >= eligibleAlerts.length - 1}
                onClick={() => setCurrentIndex((prev) => Math.min(eligibleAlerts.length - 1, prev + 1))}
                aria-label="Next urgent case"
              >
                Next
                <Icon name="chevron-right" size={14} className="ml-1" />
              </Button>
            </div>
          )}

          <Button
            variant="primary"
            size="md"
            className="!bg-[var(--color-danger)] hover:!bg-[var(--color-danger)]/90 text-white"
            onClick={() => handleOpenCase(targetAlert)}
          >
            Open Case
            <Icon name="chevron-right" size={16} className="ml-1" />
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        {/* Horse Identity Banner */}
        <div className="flex items-center justify-between rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle,#f8fafc)] p-3">
          <div className="flex items-center gap-3">
            <HorseAvatar
              name={targetAlert.horseName}
              image={targetAlert.imageUrl}
              size={48}
              rounded="md"
            />
            <div>
              <p className="text-base font-bold text-[var(--color-text-primary)]">
                {targetAlert.horseName}
                <span className="ml-2 text-xs font-normal text-[var(--color-text-muted)]">
                  #{targetAlert.horseId}
                </span>
              </p>
              <p className="text-xs text-[var(--color-text-secondary)]">
                Stall {targetAlert.stallCode ?? 'Unassigned'} · {targetAlert.stableLocation ?? 'Main Barn'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Pill size="md" tone={targetAlert.severity === 'CRITICAL' ? 'danger' : 'warning'}>
              {targetAlert.severity} PRIORITY
            </Pill>
          </div>
        </div>

        {/* Incident Details Card */}
        <Panel padded className="space-y-2 border-l-4 border-l-[var(--color-danger)]">
          <div className="flex items-center justify-between">
            <h4 className="text-sm font-semibold text-[var(--color-text-primary)]">
              {targetAlert.title}
            </h4>
            <span className="text-xs text-[var(--color-text-muted)]">
              Reported {formatDateTime(targetAlert.reportedAt)}
            </span>
          </div>

          <p className="text-sm leading-relaxed text-[var(--color-text-secondary)] whitespace-pre-wrap">
            {targetAlert.description}
          </p>

          <div className="pt-2 border-t border-[var(--color-border)] flex items-center justify-between text-xs text-[var(--color-text-muted)]">
            <span>
              Reported by: <strong className="text-[var(--color-text-primary)]">{targetAlert.reportedByName ?? 'Staff'}</strong>
              {targetAlert.reportedByRole ? ` (${targetAlert.reportedByRole})` : ''}
            </span>
            <span className="font-mono">Case #{targetAlert.scheduleId}</span>
          </div>
        </Panel>

        {/* Attached photo evidence if present */}
        {targetAlert.imageUrl && (
          <div className="rounded-[var(--radius-md)] overflow-hidden border border-[var(--color-border)] max-h-48">
            <ProtectedImage
              src={targetAlert.imageUrl}
              alt={`Incident evidence for ${targetAlert.horseName}`}
              className="w-full h-48 object-cover"
            />
          </div>
        )}
      </div>
    </Modal>
  );
}
