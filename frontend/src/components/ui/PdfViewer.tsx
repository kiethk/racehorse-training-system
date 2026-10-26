'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { loadPdfJs } from '@/lib/pdfThumbnail';
import { Button } from './Button';
import { Icon } from './Icon';

interface PdfViewerProps {
  /** The PDF blob or file */
  blob: Blob;
  /** Optional title for accessibility */
  title?: string;
  /** Zoom percentage (e.g. 100, 125, 150) */
  zoom?: number;
  /** Callback when zoom changes from inside viewer */
  onZoomChange?: (zoom: number) => void;
  /** Callback if rendering fails */
  onError?: (error: string) => void;
}

interface PageInfo {
  pageNumber: number;
  width: number;
  height: number;
}

export function PdfViewer({
  blob,
  title,
  zoom = 100,
  onZoomChange,
  onError,
}: PdfViewerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(true);
  const [numPages, setNumPages] = useState<number>(0);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [pageInfoList, setPageInfoList] = useState<PageInfo[]>([]);
  const renderTasksRef = useRef<Map<number, { cancel: () => void }>>(new Map());
  const canvasRefs = useRef<Map<number, HTMLCanvasElement>>(new Map());

  // Load document and calculate page dimensions
  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    async function initPdf() {
      try {
        setLoading(true);
        setErrorMsg(null);
        const { getDocument } = await loadPdfJs();
        if (!active) return;

        const arrayBuffer = await blob.arrayBuffer();
        if (!active) return;

        const loadingTask = getDocument({ data: new Uint8Array(arrayBuffer) });
        controller.signal.addEventListener('abort', () => {
          void loadingTask.destroy();
        });

        const doc = await loadingTask.promise;
        if (!active) return;

        setNumPages(doc.numPages);
        const pages: PageInfo[] = [];
        for (let i = 1; i <= doc.numPages; i++) {
          const page = await doc.getPage(i);
          const vp = page.getViewport({ scale: 1 });
          pages.push({ pageNumber: i, width: vp.width, height: vp.height });
        }

        if (active) {
          setPageInfoList(pages);
          setLoading(false);
        }
      } catch (err) {
        if (!active) return;
        const msg = err instanceof Error ? err.message : 'Unable to render PDF document.';
        setErrorMsg(msg);
        setLoading(false);
        onError?.(msg);
      }
    }

    void initPdf();

    return () => {
      active = false;
      controller.abort();
    };
  }, [blob, onError]);

  // Render pages onto canvases whenever zoom, pageInfoList, or blob changes
  const renderAllPages = useCallback(async () => {
    if (pageInfoList.length === 0) return;

    try {
      const { getDocument } = await loadPdfJs();
      const arrayBuffer = await blob.arrayBuffer();
      const doc = await getDocument({ data: new Uint8Array(arrayBuffer) }).promise;

      // Cancel previous render tasks
      renderTasksRef.current.forEach((task) => task.cancel());
      renderTasksRef.current.clear();

      const dpr = typeof window !== 'undefined' ? Math.max(window.devicePixelRatio || 1, 1) : 1;
      const scaleMultiplier = (zoom / 100) * 1.25;

      for (const info of pageInfoList) {
        const canvas = canvasRefs.current.get(info.pageNumber);
        if (!canvas) continue;

        const page = await doc.getPage(info.pageNumber);
        const viewport = page.getViewport({ scale: scaleMultiplier });

        canvas.width = Math.floor(viewport.width * dpr);
        canvas.height = Math.floor(viewport.height * dpr);
        canvas.style.width = `${Math.floor(viewport.width)}px`;
        canvas.style.height = `${Math.floor(viewport.height)}px`;

        const ctx = canvas.getContext('2d');
        if (!ctx) continue;

        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

        const renderTask = page.render({
          canvasContext: ctx,
          viewport,
        });

        renderTasksRef.current.set(info.pageNumber, {
          cancel: () => renderTask.cancel(),
        });

        try {
          await renderTask.promise;
        } catch (e: unknown) {
          // Ignore cancelled renders
          if (e && typeof e === 'object' && 'name' in e && e.name === 'RenderingCancelledException') {
            continue;
          }
        }
      }
    } catch {
      // Document re-render error handled silently
    }
  }, [blob, pageInfoList, zoom]);

  useEffect(() => {
    void renderAllPages();
    return () => {
      renderTasksRef.current.forEach((task) => task.cancel());
      renderTasksRef.current.clear();
    };
  }, [renderAllPages]);

  if (loading) {
    return (
      <div className="flex h-full min-h-[300px] flex-col items-center justify-center gap-3 p-6 text-sm text-[var(--color-text-muted)]">
        <span className="h-6 w-6 animate-spin rounded-full border-2 border-current border-t-transparent" aria-hidden="true" />
        <p>Rendering document preview…</p>
      </div>
    );
  }

  if (errorMsg) {
    return (
      <div role="alert" className="flex h-full min-h-[300px] flex-col items-center justify-center p-6 text-center text-sm text-[var(--color-danger)]">
        <Icon name="alert-triangle" size={28} className="mb-2 opacity-80" />
        <p className="font-semibold">Unable to display PDF preview</p>
        <p className="mt-1 text-xs text-[var(--color-text-muted)]">{errorMsg}</p>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col">
      {/* Zoom / Page toolbar */}
      <div className="sticky top-0 z-10 flex shrink-0 items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface)]/95 px-4 py-2 backdrop-blur-xs text-xs text-[var(--color-text-secondary)]">
        <span className="font-medium">
          {numPages} {numPages === 1 ? 'page' : 'pages'}
        </span>
        {onZoomChange && (
          <div className="flex items-center gap-1.5">
            <Button
              type="button"
              size="sm"
              variant="tertiary"
              icon="minus"
              aria-label="Zoom out PDF"
              disabled={zoom <= 50}
              onClick={() => onZoomChange(Math.max(50, zoom - 25))}
            />
            <span className="w-12 text-center font-mono font-medium" aria-live="polite">
              {zoom}%
            </span>
            <Button
              type="button"
              size="sm"
              variant="tertiary"
              icon="plus"
              aria-label="Zoom in PDF"
              disabled={zoom >= 250}
              onClick={() => onZoomChange(Math.min(250, zoom + 25))}
            />
            <Button
              type="button"
              size="sm"
              variant="tertiary"
              onClick={() => onZoomChange(100)}
            >
              Fit
            </Button>
          </div>
        )}
      </div>

      {/* Pages scroll container */}
      <div
        ref={containerRef}
        role="region"
        aria-label={title || 'PDF document viewer'}
        className="min-h-0 flex-1 overflow-auto overscroll-contain bg-slate-900/10 dark:bg-black/40 p-4 sm:p-6"
      >
        <div className="flex flex-col items-center gap-6">
          {pageInfoList.map((info) => (
            <div
              key={info.pageNumber}
              className="flex flex-col items-center gap-1.5"
            >
              <div className="overflow-hidden rounded-sm bg-white shadow-md ring-1 ring-black/10 dark:ring-white/10">
                <canvas
                  ref={(el) => {
                    if (el) canvasRefs.current.set(info.pageNumber, el);
                    else canvasRefs.current.delete(info.pageNumber);
                  }}
                  className="block max-w-none"
                />
              </div>
              {numPages > 1 && (
                <span className="text-[11px] font-medium text-[var(--color-text-muted)]">
                  Page {info.pageNumber} of {numPages}
                </span>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
