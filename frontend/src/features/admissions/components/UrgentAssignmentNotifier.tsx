'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { openAuthenticatedEventStream } from '@/services/api';
import type { UrgentAssignmentAlert } from '../types';
import { admissionsApi } from '../services/api';
import { UrgentAssignmentBanner } from './UrgentAssignmentBanner';

const RECONNECT_DELAY_MS = 5000;
const MAX_RECONNECT_DELAY_MS = 60000;
const SEEN_IDS_KEY = 'vet:urgent:seen_ids';
const INITIALIZED_KEY = 'vet:urgent:initialized';

function readSeenIds(): Set<number> {
  try {
    const raw = sessionStorage.getItem(SEEN_IDS_KEY);
    if (!raw) return new Set();
    return new Set(JSON.parse(raw) as number[]);
  } catch {
    return new Set();
  }
}

function writeSeenIds(set: Set<number>) {
  try {
    sessionStorage.setItem(SEEN_IDS_KEY, JSON.stringify([...set]));
  } catch {
    // ignore storage errors
  }
}

function isSessionInitialized(): boolean {
  try {
    return sessionStorage.getItem(INITIALIZED_KEY) === 'true';
  } catch {
    return false;
  }
}

function markSessionInitialized() {
  try {
    sessionStorage.setItem(INITIALIZED_KEY, 'true');
  } catch {
    // ignore storage errors
  }
}

export function UrgentAssignmentNotifier() {
  const router = useRouter();
  const pathname = usePathname();
  const [alerts, setAlerts] = useState<UrgentAssignmentAlert[]>([]);
  const seenIdsRef = useRef<Set<number>>(readSeenIds());

  // Mark an alert as seen so it will never pop up again in this session
  const markAsSeen = useCallback((scheduleId: number) => {
    seenIdsRef.current.add(scheduleId);
    writeSeenIds(seenIdsRef.current);
  }, []);

  // If currently viewing an urgent case page, mark it as seen immediately
  useEffect(() => {
    const match = pathname?.match(/\/veterinarian\/urgent\/(\d+)/);
    if (match) {
      const scheduleId = Number(match[1]);
      if (!Number.isNaN(scheduleId)) {
        markAsSeen(scheduleId);
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setAlerts((current) => current.filter((item) => item.scheduleId !== scheduleId));
      }
    }
  }, [pathname, markAsSeen]);

  const restore = useCallback(async () => {
    try {
      const pending = await admissionsApi.getPendingUrgentAlerts();
      if (!isSessionInitialized()) {
        // Initial session baseline: record existing database cases silently
        // without popping up any banner or alert!
        for (const item of pending) {
          seenIdsRef.current.add(item.scheduleId);
        }
        writeSeenIds(seenIdsRef.current);
        markSessionInitialized();
        return;
      }

      // Session is already running: only notify on truly new cases that appeared
      // after the session was initialized
      const brandNew = pending.filter((item) => !seenIdsRef.current.has(item.scheduleId));
      if (brandNew.length > 0) {
        setAlerts((current) => {
          const bySchedule = new Map(current.map((item) => [item.scheduleId, item]));
          for (const alert of brandNew) {
            bySchedule.set(alert.scheduleId, alert);
          }
          return [...bySchedule.values()].sort((a, b) => new Date(a.reportedAt).getTime() - new Date(b.reportedAt).getTime());
        });
      }
    } catch {
      // A reconnect or the next visibility change retries restoration.
    }
  }, []);

  const handleBannerDismiss = useCallback((alert: UrgentAssignmentAlert) => {
    markAsSeen(alert.scheduleId);
    setAlerts((current) => current.filter((item) => item.scheduleId !== alert.scheduleId));
  }, [markAsSeen]);

  useEffect(() => {
    void restore();

    let cancelled = false;
    let controller: AbortController | null = null;
    let pollInterval: NodeJS.Timeout | null = null;
    let failures = 0;

    const startPolling = () => {
      if (pollInterval) return;
      pollInterval = setInterval(() => {
        void restore();
      }, 20000); // 20s fallback when the stream cannot be kept open
    };

    const connect = async () => {
      for (;;) {
        if (cancelled) return;
        controller = new AbortController();
        let delay = RECONNECT_DELAY_MS;
        try {
          // fetch, not EventSource: EventSource cannot send the Authorization header.
          await openAuthenticatedEventStream('/api/care-schedules/urgent-alerts/stream', {
            signal: controller.signal,
            onEvent: (event, data) => {
              if (event !== 'urgent-assigned') return;
              try {
                const incoming = JSON.parse(data) as UrgentAssignmentAlert;
                // Live real-time incident assigned during this session:
                if (!seenIdsRef.current.has(incoming.scheduleId)) {
                  setAlerts((current) => {
                    const bySchedule = new Map(current.map((item) => [item.scheduleId, item]));
                    bySchedule.set(incoming.scheduleId, incoming);
                    return [...bySchedule.values()].sort((a, b) => new Date(a.reportedAt).getTime() - new Date(b.reportedAt).getTime());
                  });
                }
              } catch {
                void restore();
              }
            },
          });
          failures = 0;
        } catch {
          failures += 1;
          delay = Math.min(RECONNECT_DELAY_MS * 2 ** (failures - 1), MAX_RECONNECT_DELAY_MS);
          if (!cancelled) {
            startPolling();
            void restore();
          }
        }
        if (cancelled) return;
        // The server closes the stream after its timeout; a broken stream retries the same way.
        await new Promise((resolve) => setTimeout(resolve, delay));
      }
    };

    void connect();

    const onVisible = () => {
      if (document.visibilityState === 'visible') {
        void restore();
      }
    };

    document.addEventListener('visibilitychange', onVisible);
    window.addEventListener('focus', onVisible);

    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', onVisible);
      window.removeEventListener('focus', onVisible);
      if (pollInterval) clearInterval(pollInterval);
      controller?.abort();
    };
  }, [restore]);

  const active = alerts
    .filter((alert) => pathname !== `/veterinarian/urgent/${alert.scheduleId}`)
    .sort((a, b) => new Date(a.reportedAt).getTime() - new Date(b.reportedAt).getTime());

  function openCase(alert: UrgentAssignmentAlert) {
    markAsSeen(alert.scheduleId);
    setAlerts((current) => current.filter((item) => item.scheduleId !== alert.scheduleId));
    router.push(`/veterinarian/urgent/${alert.scheduleId}`);
  }

  return (
    <UrgentAssignmentBanner
      alerts={active}
      onOpenCase={openCase}
      onDismiss={handleBannerDismiss}
    />
  );
}
