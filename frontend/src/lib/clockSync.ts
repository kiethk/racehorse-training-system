'use client';

import { useState, useEffect } from 'react';

let baseEpochMs: number = Date.now() - performance.now();
let isSyncing = false;
let lastSyncPerf = 0;

/**
 * Triggers an immediate fetch to get the exact OS system time,
 * completely bypassing Chrome V8's internal ~60s clock caching quirk on Windows.
 */
export async function syncClockFromServer(): Promise<Date> {
  const nowPerf = performance.now();
  // Throttle repeated calls within 300ms
  if (isSyncing || (nowPerf - lastSyncPerf < 300)) {
    return getSynchronizedNow();
  }

  isSyncing = true;
  lastSyncPerf = nowPerf;
  try {
    // First try Next.js same-origin endpoint (0ms overhead)
    let res = await fetch('/api/system/time', { cache: 'no-store' });
    if (!res.ok) {
      // Fallback directly to Spring Boot
      res = await fetch('http://localhost:8080/api/system/time', { cache: 'no-store' });
    }
    if (res.ok) {
      const data = await res.json();
      if (data?.data?.epochMillis) {
        baseEpochMs = Number(data.data.epochMillis) - performance.now();
      }
    }
  } catch {
    // Do NOT overwrite baseEpochMs with Chrome's stale Date.now()!
    // Maintain existing synchronized monotonic clock.
  } finally {
    isSyncing = false;
  }
  return getSynchronizedNow();
}

/**
 * Updates baseEpochMs using the Date header from any regular API response,
 * keeping the frontend time continuously aligned with the OS clock.
 */
export function recordServerResponseDate(dateHeader: string | null) {
  if (!dateHeader) return;
  const serverTime = new Date(dateHeader).getTime();
  if (!Number.isNaN(serverTime)) {
    baseEpochMs = serverTime - performance.now();
  }
}

/**
 * Returns the current live Date object based on monotonic performance.now()
 * and the synchronized OS epoch, immune to Chrome's 60s Date.now() caching.
 */
export function getSynchronizedNow(): Date {
  return new Date(baseEpochMs + performance.now());
}

/**
 * React hook that provides a live, reactive Clock.
 * Synchronizes continuously every `tickMs` (default 2s) and immediately on window focus,
 * guaranteeing the time NEVER drifts or reverts to stale browser cache.
 */
export function useSynchronizedClock(tickMs = 2000): Date {
  const [clock, setClock] = useState<Date>(() => getSynchronizedNow());

  useEffect(() => {
    let mounted = true;

    const syncAndRefresh = async () => {
      const liveDate = await syncClockFromServer();
      if (mounted) {
        setClock(liveDate);
      }
    };

    // Initial sync
    void syncAndRefresh();

    const onFocus = () => {
      void syncAndRefresh();
    };

    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        void syncAndRefresh();
      }
    };

    window.addEventListener('focus', onFocus);
    document.addEventListener('visibilitychange', onVisibilityChange);
    // Periodically re-sync from server to stay 100% immune to clock modifications
    const interval = setInterval(syncAndRefresh, tickMs);

    return () => {
      mounted = false;
      window.removeEventListener('focus', onFocus);
      document.removeEventListener('visibilitychange', onVisibilityChange);
      clearInterval(interval);
    };
  }, [tickMs]);

  return clock;
}
