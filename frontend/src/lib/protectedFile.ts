'use client';

import { useEffect, useState } from 'react';
import { apiGetBlob } from '@/services/api';

/** Module-level in-memory cache for protected file blob URLs to prevent redundant network downloads */
const blobUrlCache = new Map<string, string>();
const inFlightRequests = new Map<string, Promise<string>>();
const MAX_BLOB_CACHE_SIZE = 100;

function cacheBlobUrl(source: string, url: string): void {
  if (blobUrlCache.size >= MAX_BLOB_CACHE_SIZE) {
    const oldestKey = blobUrlCache.keys().next().value;
    if (oldestKey) {
      const oldUrl = blobUrlCache.get(oldestKey);
      if (oldUrl) URL.revokeObjectURL(oldUrl);
      blobUrlCache.delete(oldestKey);
    }
  }
  blobUrlCache.set(source, url);
}

export async function fetchProtectedBlobUrl(source: string, signal?: AbortSignal): Promise<string> {
  const cached = blobUrlCache.get(source);
  if (cached) return cached;

  const existing = inFlightRequests.get(source);
  if (existing) return existing;

  const promise = apiGetBlob(source, signal)
    .then((blob) => {
      const url = URL.createObjectURL(blob);
      cacheBlobUrl(source, url);
      inFlightRequests.delete(source);
      return url;
    })
    .catch((err) => {
      inFlightRequests.delete(source);
      throw err;
    });

  inFlightRequests.set(source, promise);
  return promise;
}

/**
 * Stored files are served by API endpoints that check the caller's permission, and an <img> or <a>
 * cannot send the access token. This loads the file with the token and returns a temporary blob URL
 * to display instead. Returns null while loading, when there is no source, and when loading fails.
 */
export function useProtectedFileUrl(source: string | null | undefined): { url: string | null; failed: boolean } {
  const direct = source && /^(blob:|data:)/.test(source) ? source : null;
  const initialCached = source ? blobUrlCache.get(source) ?? null : null;

  const [loaded, setLoaded] = useState<{ source: string; url: string | null } | null>(() => {
    if (direct) return { source: direct, url: direct };
    if (initialCached && source) return { source, url: initialCached };
    return null;
  });

  useEffect(() => {
    if (!source || direct) return;

    if (blobUrlCache.has(source)) {
      setLoaded({ source, url: blobUrlCache.get(source)! });
      return;
    }

    const controller = new AbortController();
    fetchProtectedBlobUrl(source, controller.signal)
      .then((url) => {
        if (!controller.signal.aborted) {
          setLoaded({ source, url });
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setLoaded({ source, url: null });
        }
      });

    return () => {
      controller.abort();
    };
  }, [source, direct]);

  if (direct) return { url: direct, failed: false };
  const current = loaded && loaded.source === source ? loaded : null;
  return { url: current?.url ?? null, failed: current !== null && current.url === null };
}

/**
 * `useProtectedFileUrl` for a list of files, e.g. every photo in a gallery. Returns one entry per source:
 * `url` is null while loading or when loading failed (`failed` tells the two apart).
 */
export function useProtectedFileUrls(sources: readonly string[]): Record<string, { url: string | null; failed: boolean }> {
  const key = sources.join('\n');
  const [loaded, setLoaded] = useState<{ key: string; urls: Record<string, string | null> }>({ key: '', urls: {} });

  useEffect(() => {
    const list = key ? key.split('\n') : [];
    const controller = new AbortController();

    list.forEach((source) => {
      if (blobUrlCache.has(source)) {
        const cachedUrl = blobUrlCache.get(source)!;
        setLoaded((current) => ({
          key,
          urls: { ...(current.key === key ? current.urls : {}), [source]: cachedUrl },
        }));
        return;
      }

      fetchProtectedBlobUrl(source, controller.signal)
        .then((url) => {
          if (controller.signal.aborted) return;
          setLoaded((current) => ({
            key,
            urls: { ...(current.key === key ? current.urls : {}), [source]: url },
          }));
        })
        .catch(() => {
          if (!controller.signal.aborted) {
            setLoaded((current) => ({
              key,
              urls: { ...(current.key === key ? current.urls : {}), [source]: null },
            }));
          }
        });
    });

    return () => {
      controller.abort();
    };
  }, [key]);

  const urls = loaded.key === key ? loaded.urls : {};
  return Object.fromEntries(sources.map((source) => [
    source,
    { url: urls[source] ?? (blobUrlCache.get(source) ?? null), failed: source in urls && urls[source] === null },
  ]));
}

/**
 * Opens a stored file in a new browser tab. The tab is opened first, inside the click, so the
 * browser does not treat it as a pop-up; the file is then loaded with the access token.
 */
export async function openProtectedFile(source: string): Promise<void> {
  const tab = window.open('', '_blank');
  try {
    const objectUrl = await fetchProtectedBlobUrl(source);
    if (tab) tab.location.href = objectUrl;
    else window.open(objectUrl, '_blank');
  } catch (error) {
    tab?.close();
    throw error;
  }
}
