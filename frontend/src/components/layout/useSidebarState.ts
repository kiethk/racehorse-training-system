'use client';

import { useCallback, useSyncExternalStore } from 'react';

const STORAGE_KEY = 'rtms_sidebar_collapsed';
const DESKTOP_QUERY = '(min-width: 64rem)';

const listeners = new Set<() => void>();

function subscribePreference(onChange: () => void) {
  listeners.add(onChange);
  window.addEventListener('storage', onChange);
  return () => {
    listeners.delete(onChange);
    window.removeEventListener('storage', onChange);
  };
}

function readPreference(): boolean {
  try {
    return window.localStorage.getItem(STORAGE_KEY) === '1';
  } catch {
    return false;
  }
}

function subscribeDesktop(onChange: () => void) {
  const query = window.matchMedia(DESKTOP_QUERY);
  query.addEventListener('change', onChange);
  return () => query.removeEventListener('change', onChange);
}

/**
 * Sidebar collapse state. The user's choice is a UI preference kept in localStorage;
 * below the desktop breakpoint the sidebar is always collapsed.
 */
export function useSidebarState() {
  const preferCollapsed = useSyncExternalStore(subscribePreference, readPreference, () => false);
  const isDesktop = useSyncExternalStore(
    subscribeDesktop,
    () => window.matchMedia(DESKTOP_QUERY).matches,
    () => true,
  );

  const toggle = useCallback(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, readPreference() ? '0' : '1');
    } catch {
      // Ignore storage write errors; the preference just won't persist.
    }
    listeners.forEach((listener) => listener());
  }, []);

  return { collapsed: preferCollapsed || !isDesktop, canToggle: isDesktop, toggle };
}
