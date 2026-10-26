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

async function ensureCorrectMimeType(rawBlob: Blob, source: string): Promise<Blob> {
  // If rawBlob already has a specific image or pdf type (not generic octet-stream or empty), keep it
  if (rawBlob.type && rawBlob.type !== 'application/octet-stream') {
    return rawBlob;
  }

  const clean = source.split('?')[0].toLowerCase();
  if (clean.endsWith('.pdf')) {
    return new Blob([rawBlob], { type: 'application/pdf' });
  }
  if (clean.endsWith('.jpg') || clean.endsWith('.jpeg')) {
    return new Blob([rawBlob], { type: 'image/jpeg' });
  }
  if (clean.endsWith('.png')) {
    return new Blob([rawBlob], { type: 'image/png' });
  }
  if (clean.endsWith('.webp')) {
    return new Blob([rawBlob], { type: 'image/webp' });
  }

  // Sniff magic bytes from rawBlob header
  try {
    const buffer = await rawBlob.slice(0, 16).arrayBuffer();
    const bytes = new Uint8Array(buffer);
    if (bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
      return new Blob([rawBlob], { type: 'image/jpeg' });
    }
    if (bytes.length >= 8 && bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47) {
      return new Blob([rawBlob], { type: 'image/png' });
    }
    if (
      bytes.length >= 12 &&
      bytes[0] === 0x52 &&
      bytes[1] === 0x49 &&
      bytes[2] === 0x46 &&
      bytes[3] === 0x46 &&
      bytes[8] === 0x57 &&
      bytes[9] === 0x45 &&
      bytes[10] === 0x42 &&
      bytes[11] === 0x50
    ) {
      return new Blob([rawBlob], { type: 'image/webp' });
    }
    if (bytes.length >= 4 && bytes[0] === 0x25 && bytes[1] === 0x50 && bytes[2] === 0x44 && bytes[3] === 0x46) {
      return new Blob([rawBlob], { type: 'application/pdf' });
    }
  } catch {
    // If arrayBuffer fails for any reason, continue to heuristics
  }

  // If path relates to horse photo or avatar, default to image/jpeg so <img> tag doesn't fail
  if (clean.includes('/photo') || clean.includes('avatar') || clean.includes('horse-photo')) {
    return new Blob([rawBlob], { type: 'image/jpeg' });
  }

  return rawBlob;
}

export async function fetchProtectedBlobUrl(source: string, signal?: AbortSignal): Promise<string> {
  const cached = blobUrlCache.get(source);
  if (cached) return cached;

  if (signal?.aborted) {
    throw new DOMException('The user aborted a request.', 'AbortError');
  }

  const existing = inFlightRequests.get(source);
  if (existing) return existing;

  // Global fetch is decoupled from individual component abort signals so unmounting one component
  // does not abort concurrent or subsequent requests sharing this URL or break the cache.
  const promise = (async () => {
    try {
      const rawBlob = await apiGetBlob(source);
      const blob = await ensureCorrectMimeType(rawBlob, source);
      const url = URL.createObjectURL(blob);
      cacheBlobUrl(source, url);
      return url;
    } finally {
      inFlightRequests.delete(source);
    }
  })();

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

    let active = true;
    fetchProtectedBlobUrl(source)
      .then((url) => {
        if (active) {
          setLoaded({ source, url });
        }
      })
      .catch(() => {
        if (active) {
          setLoaded({ source, url: null });
        }
      });

    return () => {
      active = false;
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
    let active = true;

    list.forEach((source) => {
      if (blobUrlCache.has(source)) {
        const cachedUrl = blobUrlCache.get(source)!;
        setLoaded((current) => ({
          key,
          urls: { ...(current.key === key ? current.urls : {}), [source]: cachedUrl },
        }));
        return;
      }

      fetchProtectedBlobUrl(source)
        .then((url) => {
          if (!active) return;
          setLoaded((current) => ({
            key,
            urls: { ...(current.key === key ? current.urls : {}), [source]: url },
          }));
        })
        .catch(() => {
          if (!active) return;
          setLoaded((current) => ({
            key,
            urls: { ...(current.key === key ? current.urls : {}), [source]: null },
          }));
        });
    });

    return () => {
      active = false;
    };
  }, [key]);

  const urls = loaded.key === key ? loaded.urls : {};
  return Object.fromEntries(sources.map((source) => [
    source,
    { url: urls[source] ?? (blobUrlCache.get(source) ?? null), failed: source in urls && urls[source] === null },
  ]));
}

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (m) => {
    switch (m) {
      case '&': return '&amp;';
      case '<': return '&lt;';
      case '>': return '&gt;';
      case '"': return '&quot;';
      case "'": return '&#39;';
      default: return m;
    }
  });
}

/**
 * In Chromium browsers, navigating top-level window directly to a `blob:` URL of a PDF
 * (e.g. `tab.location.href = blobUrl`) triggers an automatic force-download instead of viewing.
 * Embedding the blob URL inside a full-window iframe renders Chromium's native PDF plugin
 * inline with all viewer controls without triggering the forced download.
 */
function renderViewerInTab(tab: Window, objectUrl: string, title: string, isPdf: boolean): void {
  const safeTitle = escapeHtml(title || 'Document Preview');
  try {
    tab.document.title = title || 'Document Preview';
    if (isPdf) {
      tab.document.body.style.margin = '0';
      tab.document.body.style.padding = '0';
      tab.document.body.style.width = '100%';
      tab.document.body.style.height = '100%';
      tab.document.body.style.overflow = 'hidden';
      tab.document.body.style.backgroundColor = '#525659';
      tab.document.body.innerHTML = `
        <iframe
          src="${objectUrl}"
          title="${safeTitle}"
          style="width:100%;height:100%;border:none;display:block;"
        ></iframe>
      `;
    } else {
      tab.document.body.style.margin = '0';
      tab.document.body.style.padding = '0';
      tab.document.body.style.width = '100%';
      tab.document.body.style.height = '100%';
      tab.document.body.style.overflow = 'auto';
      tab.document.body.style.backgroundColor = '#0f172a';
      tab.document.body.style.display = 'flex';
      tab.document.body.style.alignItems = 'center';
      tab.document.body.style.justifyContent = 'center';
      tab.document.body.innerHTML = `
        <img
          src="${objectUrl}"
          alt="${safeTitle}"
          style="max-width:100%;max-height:100%;object-fit:contain;padding:16px;"
        />
      `;
    }
  } catch {
    try {
      tab.location.href = objectUrl;
    } catch {
      // ignore
    }
  }
}

/**
 * Opens a local or existing blob URL in a new tab without triggering Chromium auto-download.
 */
export function openBlobInTab(blobUrl: string, title: string = 'Document Preview', isPdf?: boolean): void {
  const tab = window.open('', '_blank');
  if (!tab) return;
  const determinedIsPdf = isPdf !== undefined
    ? isPdf
    : (title.toLowerCase().endsWith('.pdf') || blobUrl.toLowerCase().includes('.pdf'));
  renderViewerInTab(tab, blobUrl, title, determinedIsPdf);
}

/**
 * Opens a stored file in a new browser tab. The tab is opened first inside the click event
 * to avoid pop-up blocking, displays a loading state, and then renders the preview inline.
 */
export async function openProtectedFile(source: string, title?: string): Promise<void> {
  const tab = window.open('', '_blank');
  const docTitle = title || 'Document Preview';

  if (tab) {
    try {
      tab.document.write(`
        <!DOCTYPE html>
        <html>
          <head>
            <meta charset="utf-8">
            <title>${escapeHtml(docTitle)}</title>
            <style>
              html, body {
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                overflow: hidden;
                background-color: #0f172a;
                display: flex;
                align-items: center;
                justify-content: center;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                color: #94a3b8;
              }
              .loader { display: flex; flex-direction: column; align-items: center; gap: 12px; }
              .spinner {
                width: 28px;
                height: 28px;
                border: 3px solid rgba(255,255,255,0.15);
                border-top-color: #38bdf8;
                border-radius: 50%;
                animation: spin 0.8s linear infinite;
              }
              @keyframes spin { to { transform: rotate(360deg); } }
            </style>
          </head>
          <body>
            <div class="loader">
              <div class="spinner"></div>
              <div>Loading preview…</div>
            </div>
          </body>
        </html>
      `);
      tab.document.close();
    } catch {
      // ignore write errors
    }
  }

  try {
    const objectUrl = await fetchProtectedBlobUrl(source);
    if (!tab || tab.closed) return;
    const isPdf = (title && title.toLowerCase().endsWith('.pdf'))
      || source.toLowerCase().includes('.pdf')
      || (!source.toLowerCase().includes('photo') && !/\.(jpe?g|png|webp)$/i.test(source));
    renderViewerInTab(tab, objectUrl, docTitle, isPdf);
  } catch (error) {
    if (tab && !tab.closed) {
      try {
        tab.document.body.innerHTML = `
          <div style="display:flex;align-items:center;justify-content:center;height:100%;color:#f87171;font-family:sans-serif;text-align:center;padding:20px;">
            Unable to load document preview.
          </div>
        `;
      } catch {
        // ignore
      }
    }
    throw error;
  }
}
