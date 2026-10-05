'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { createAuthenticatedEventSource } from '@/services/api';
import type { UrgentAssignmentAlert } from '../types';
import { admissionsApi } from '../services/api';
import { UrgentAssignmentModal } from './UrgentAssignmentModal';

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
    const source = createAuthenticatedEventSource('/api/care-schedules/urgent-alerts/stream');
    source.addEventListener('urgent-assigned', (event) => {
      try {
        mergeAlerts([JSON.parse((event as MessageEvent<string>).data) as UrgentAssignmentAlert]);
      } catch {
        void restore();
      }
    });
    source.onerror = () => void restore();
    const onVisible = () => document.visibilityState === 'visible' && void restore();
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      document.removeEventListener('visibilitychange', onVisible);
      source.close();
    };
  }, [mergeAlerts, restore]);

  const active = alerts.find((alert) => pathname !== `/veterinarian/urgent/${alert.scheduleId}`) ?? null;

  function openCase(alert: UrgentAssignmentAlert) {
    setAlerts((current) => current.filter((item) => item.scheduleId !== alert.scheduleId));
    router.push(`/veterinarian/urgent/${alert.scheduleId}`);
  }

  return <UrgentAssignmentModal alert={active} onOpenCase={openCase} />;
}
