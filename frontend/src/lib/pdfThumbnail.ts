'use client';

import { useEffect, useState } from 'react';
import { apiGetBlob } from '@/services/api';

type PdfJs = typeof import('pdfjs-dist');

let pdfjs: Promise<PdfJs> | null = null;

/** pdf.js is large, so it is only downloaded the first time a PDF thumbnail is needed. */
export function loadPdfJs(): Promise<PdfJs> {
  pdfjs ??= import('pdfjs-dist').then((module) => {
    // Parsing runs in a worker so a big PDF never blocks the page.
    if (typeof window !== 'undefined') {
      module.GlobalWorkerOptions.workerSrc = '/pdf.worker.min.mjs';
    } else {
      module.GlobalWorkerOptions.workerSrc = new URL('pdfjs-dist/build/pdf.worker.min.mjs', import.meta.url).toString();
    }
    return module;
  }).catch((error) => {
    pdfjs = null; // Allow a retry after a failed download.
    throw error;
  });
  return pdfjs;
}

/** Renders the first page of a PDF to a PNG `width` pixels wide. */
export async function renderPdfFirstPage(pdf: Blob, width: number, signal?: AbortSignal): Promise<Blob> {
  const { getDocument } = await loadPdfJs();
  signal?.throwIfAborted();
  const task = getDocument({ data: new Uint8Array(await pdf.arrayBuffer()) });
  const abort = () => { void task.destroy(); };
  signal?.addEventListener('abort', abort, { once: true });
  try {
    const document = await task.promise;
    const page = await document.getPage(1);
    const viewport = page.getViewport({ scale: width / page.getViewport({ scale: 1 }).width });
    const canvas = window.document.createElement('canvas');
    canvas.width = Math.ceil(viewport.width);
    canvas.height = Math.ceil(viewport.height);
    await page.render({ canvas, viewport }).promise;
    signal?.throwIfAborted();
    return await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Could not draw the PDF page.'))), 'image/png');
    });
  } finally {
    signal?.removeEventListener('abort', abort);
    void task.destroy();
  }
}

/**
 * A preview image of the first page of a stored PDF, loaded with the access token. `url` is null while
 * rendering and when it failed (`failed` tells the two apart). The blob URL is revoked on unmount.
 */
export function usePdfFirstPage(source: string | null, width = 240): { url: string | null; failed: boolean } {
  const [state, setState] = useState<{ source: string; url: string | null } | null>(null);

  useEffect(() => {
    if (!source) return;
    const controller = new AbortController();
    let objectUrl: string | undefined;
    apiGetBlob(source, controller.signal)
      .then((pdf) => renderPdfFirstPage(pdf, width, controller.signal))
      .then((png) => {
        if (controller.signal.aborted) return;
        objectUrl = URL.createObjectURL(png);
        setState({ source, url: objectUrl });
      })
      .catch(() => {
        if (!controller.signal.aborted) setState({ source, url: null });
      });
    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [source, width]);

  const current = state && state.source === source ? state : null;
  return { url: current?.url ?? null, failed: current !== null && current.url === null };
}
