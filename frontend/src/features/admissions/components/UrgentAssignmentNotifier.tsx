'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { openAuthenticatedEventStream } from '@/services/api';
import type { UrgentAssignmentAlert } from '../types';
import { admissionsApi } from '../services/api';
import { UrgentAssignmentModal } from './UrgentAssignmentModal';

const RECONNECT_DELAY_MS = 5000;

export function UrgentAssignmentNotifier() {
  const router = useRouter();
  const pathname = usePathname();
  const [alerts, setAlerts] = useState<UrgentAssignmentAlert[]>([]);
  const versions = useRef(new Map<number, number>());

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

    const startPolling = () => {
      if (pollInterval) return;
      pollInterval = setInterval(() => {
        void restore();
      }, 20000); // 20s fallback when the stream cannot be kept open
    };

    const connect = async () => {
      for (;;) {
        controller = new AbortController();
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
        } catch {
          if (!cancelled) {
            startPolling();
            void restore();
          }
          return;
        }
        if (cancelled) return;
        // The server closes the stream after its timeout: reconnect and keep listening.
        await new Promise((resolve) => setTimeout(resolve, RECONNECT_DELAY_MS));
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

  const active = alerts.find((alert) => pathname !== `/veterinarian/urgent/${alert.scheduleId}`) ?? null;

  function openCase(alert: UrgentAssignmentAlert) {
    setAlerts((current) => current.filter((item) => item.scheduleId !== alert.scheduleId));
    router.push(`/veterinarian/urgent/${alert.scheduleId}`);
  }

  return <UrgentAssignmentModal alert={active} onOpenCase={openCase} />;
}
