'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { openAuthenticatedEventStream } from '@/services/api';
import type { UrgentAssignmentAlert } from '../types';
import { admissionsApi } from '../services/api';
import { UrgentAssignmentBanner } from './UrgentAssignmentBanner';

const RECONNECT_DELAY_MS = 5000;
const MAX_RECONNECT_DELAY_MS = 60000;
const SNOOZE_MS = 10 * 60 * 1000; // remind again after 10 minutes
const SNOOZE_KEY = 'vet:urgent:snoozed';

function readSnooze(): Map<number, number> {
  try {
    const raw = sessionStorage.getItem(SNOOZE_KEY);
    if (!raw) return new Map();
    return new Map(JSON.parse(raw) as [number, number][]);
  } catch {
    return new Map();
  }
}

function writeSnooze(map: Map<number, number>) {
  try {
    sessionStorage.setItem(SNOOZE_KEY, JSON.stringify([...map.entries()]));
  } catch {
    // ignore storage errors
  }
}

export function UrgentAssignmentNotifier() {
  const router = useRouter();
  const pathname = usePathname();
  const [alerts, setAlerts] = useState<UrgentAssignmentAlert[]>([]);
  const versions = useRef(new Map<number, number>());
  const snoozed = useRef<Map<number, number>>(readSnooze());

  const mergeAlerts = useCallback((incoming: UrgentAssignmentAlert[]) => {
    setAlerts((current) => {
      const bySchedule = new Map(current.map((item) => [item.scheduleId, item]));
      for (const alert of incoming) {
        const seenVersion = versions.current.get(alert.scheduleId);
        if (seenVersion !== undefined && seenVersion >= alert.eventId) continue;
        versions.current.set(alert.scheduleId, alert.eventId);
        bySchedule.set(alert.scheduleId, alert);
      }
      return [...bySchedule.values()].sort((a, b) => new Date(a.reportedAt).getTime() - new Date(b.reportedAt).getTime());
    });
  }, []);

  const dismiss = useCallback((alert: UrgentAssignmentAlert) => {
    const next = new Map(snoozed.current);
    next.set(alert.scheduleId, Date.now() + SNOOZE_MS);
    snoozed.current = next;
    writeSnooze(next);
    setAlerts((current) => current.filter((a) => a.scheduleId !== alert.scheduleId));
  }, []);

  const restore = useCallback(async () => {
    try {
      mergeAlerts(await admissionsApi.getPendingUrgentAlerts());
    } catch {
      // A reconnect or the next visibility change retries restoration.
    }
  }, [mergeAlerts]);

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
                mergeAlerts([JSON.parse(data) as UrgentAssignmentAlert]);
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
  }, [mergeAlerts, restore]);

  const now = Date.now();
  const active = alerts
    .filter((alert) => {
      if (pathname === `/veterinarian/urgent/${alert.scheduleId}`) return false;
      const until = snoozed.current.get(alert.scheduleId);
      if (until && until > now) return false;
      return true;
    })
    .sort((a, b) => new Date(a.reportedAt).getTime() - new Date(b.reportedAt).getTime());

  function openCase(alert: UrgentAssignmentAlert) {
    const next = new Map(snoozed.current);
    next.delete(alert.scheduleId);
    snoozed.current = next;
    writeSnooze(next);
    setAlerts((current) => current.filter((item) => item.scheduleId !== alert.scheduleId));
    router.push(`/veterinarian/urgent/${alert.scheduleId}`);
  }

  return <UrgentAssignmentBanner alerts={active} onOpenCase={openCase} onDismiss={dismiss} />;
}
