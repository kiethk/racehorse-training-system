'use client';

import { useEffect, useId, useRef, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { admissionsApi } from '../../services/api';
import type { AdmissionDocument } from '../../types';

type LoadedFile = { url: string; kind: 'image' | 'pdf' | 'unsupported' };

export function AdmissionDocumentPreview({ document, label, fileUrl, onClose }: {
  document: AdmissionDocument;
  label: string;
  fileUrl: string;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const [file, setFile] = useState<LoadedFile | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [zoom, setZoom] = useState(100);
  const filename = document.originalFileName || label;

  useEffect(() => {
    const dialog = dialogRef.current!;
    const previousFocus = window.document.activeElement;
    const previousOverflow = window.document.body.style.overflow;
    dialog.showModal();
    window.document.body.style.overflow = 'hidden';
    return () => {
      dialog.close();
      window.document.body.style.overflow = previousOverflow;
      if (previousFocus instanceof HTMLElement) previousFocus.focus();
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let objectUrl: string | undefined;
    admissionsApi.getDocumentFile(document.fileUrl, controller.signal).then((blob) => {
      if (controller.signal.aborted) return;
      objectUrl = URL.createObjectURL(blob);
      const kind = ['image/jpeg', 'image/png', 'image/webp'].includes(blob.type)
        ? 'image' : blob.type === 'application/pdf' ? 'pdf' : 'unsupported';
      setFile({ url: objectUrl, kind });
    }).catch(() => {
      if (!controller.signal.aborted) setError('Unable to load this file. Try opening it in a new tab.');
    });
    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [document.fileUrl]);

  const actionClass = 'inline-flex h-9 items-center justify-center gap-1.5 rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm font-medium text-[var(--color-text-primary)] hover:bg-[var(--color-surface-muted)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]';

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onCancel={(event) => { event.preventDefault(); onClose(); }}
      onClick={(event) => { if (event.target === event.currentTarget) onClose(); }}
      className="fixed inset-0 m-auto h-dvh max-h-dvh w-screen max-w-none overflow-hidden border border-[var(--color-border)] bg-[var(--color-surface)] p-0 text-[var(--color-text-primary)] shadow-xl backdrop:bg-black/50 sm:h-[90dvh] sm:w-[calc(100%_-_3rem)] sm:max-w-5xl sm:rounded-[var(--radius-lg)]"
    >
      <div className="flex h-full min-h-0 flex-col">
        <header className="flex shrink-0 items-start gap-3 border-b border-[var(--color-border)] px-4 py-3 sm:px-5">
          <Icon name="file-text" size={20} className="mt-1 shrink-0 text-[var(--color-text-muted)]" />
          <div className="min-w-0 flex-1">
            <h2 id={titleId} className="break-words text-lg font-semibold">{filename}</h2>
            <p className="mt-0.5 text-xs text-[var(--color-text-muted)]">{label}</p>
          </div>
          <Button type="button" variant="tertiary" icon="x" aria-label="Close preview" onClick={onClose} />
        </header>

        <div className="flex shrink-0 flex-wrap items-center gap-2 border-b border-[var(--color-border)] px-4 py-2 sm:px-5">
          {file && <a href={file.url} download={filename} className={actionClass}><Icon name="download" />Download</a>}
          <a href={file && file.kind !== 'unsupported' ? file.url : fileUrl} target="_blank" rel="noreferrer" className={actionClass}><Icon name="external-link" />Open in new tab</a>
          {file?.kind === 'image' && !error && (
            <div className="ml-auto flex items-center gap-1">
              <Button type="button" size="sm" variant="tertiary" icon="minus" aria-label="Zoom out" disabled={zoom === 100} onClick={() => setZoom((value) => value - 25)} />
              <span className="w-11 text-center text-xs" aria-live="polite">{zoom}%</span>
              <Button type="button" size="sm" variant="tertiary" icon="plus" aria-label="Zoom in" disabled={zoom === 200} onClick={() => setZoom((value) => value + 25)} />
              <Button type="button" size="sm" variant="tertiary" onClick={() => setZoom(100)}>Fit</Button>
            </div>
          )}
        </div>

        <div className="min-h-0 flex-1 overflow-auto overscroll-contain bg-[var(--color-surface-muted)]" aria-busy={!file && !error}>
          {error ? (
            <div role="alert" className="flex h-full items-center justify-center p-6 text-center text-sm">{error}</div>
          ) : !file ? (
            <div role="status" className="flex h-full items-center justify-center gap-2 text-sm text-[var(--color-text-muted)]"><span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" aria-hidden="true" />Loading document…</div>
          ) : file.kind === 'image' ? (
            <div className="relative" style={{ width: `${zoom}%`, height: `${zoom}%` }}>
              {/* Blob URLs are authenticated local previews; no image optimization request is needed. */}
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={file.url} alt={filename} width={1600} height={1000} className="h-full w-full object-contain p-3" onError={() => setError('This image could not be displayed. Download it or open it in a new tab.')} />
            </div>
          ) : file.kind === 'pdf' ? (
            <div className="flex h-full flex-col">
              <p className="shrink-0 px-4 py-2 text-xs text-[var(--color-text-muted)]">If the PDF viewer is unavailable, download the file or open it in a new tab.</p>
              <iframe src={file.url} title={`Preview of ${filename}`} className="min-h-0 w-full flex-1 border-0" onError={() => setError('This PDF could not be displayed. Download it or open it in a new tab.')} />
            </div>
          ) : (
            <div className="flex h-full items-center justify-center p-6 text-center text-sm">Preview is available for JPEG, PNG, WebP and PDF files. Download this file or open it in a new tab.</div>
          )}
        </div>
        {document.note && <p className="max-h-24 shrink-0 overflow-auto whitespace-pre-wrap break-words border-t border-[var(--color-border)] px-4 py-3 text-xs text-[var(--color-text-secondary)]"><span className="font-medium">Note: </span>{document.note}</p>}
      </div>
    </dialog>
  );
}
