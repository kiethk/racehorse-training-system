'use client';

import { useEffect, useId, useRef, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { PdfViewer } from '@/components/ui/PdfViewer';

interface LocalFilePreviewModalProps {
  file: File;
  previewUrl: string;
  onClose: () => void;
}

export function LocalFilePreviewModal({ file, previewUrl, onClose }: LocalFilePreviewModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const [zoom, setZoom] = useState(100);
  const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
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

  const actionClass =
    'inline-flex h-9 items-center justify-center gap-1.5 rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm font-medium text-[var(--color-text-primary)] hover:bg-[var(--color-surface-muted)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]';

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
      className="fixed inset-0 m-auto h-dvh max-h-dvh w-screen max-w-none overflow-hidden border border-[var(--color-border)] bg-[var(--color-surface)] p-0 text-[var(--color-text-primary)] shadow-[var(--shadow-popover)] backdrop:bg-black/50 sm:h-[90dvh] sm:w-[calc(100%_-_3rem)] sm:max-w-5xl sm:rounded-[var(--radius-lg)]"
    >
      <div className="flex h-full min-h-0 flex-col">
        <header className="flex shrink-0 items-start gap-3 border-b border-[var(--color-border)] px-4 py-3 sm:px-5">
          <Icon name={isPdf ? 'file-text' : 'image'} size={20} className="mt-1 shrink-0 text-[var(--color-text-muted)]" />
          <div className="min-w-0 flex-1">
            <h2 id={titleId} className="break-words text-lg font-semibold">
              {file.name}
            </h2>
            <p className="mt-0.5 text-xs text-[var(--color-text-muted)]">
              {(file.size / (1024 * 1024)).toFixed(2)} MB · {isPdf ? 'PDF Document' : file.type || 'File'}
            </p>
          </div>
          <Button type="button" variant="tertiary" icon="x" aria-label="Close preview" onClick={onClose} />
        </header>

        <div className="flex shrink-0 flex-wrap items-center gap-2 border-b border-[var(--color-border)] px-4 py-2 sm:px-5">
          <a href={previewUrl} download={file.name} className={actionClass}>
            <Icon name="download" />
            Download
          </a>
          <a href={previewUrl} target="_blank" rel="noreferrer" className={actionClass}>
            <Icon name="external-link" />
            Open in new tab
          </a>
          {!isPdf && (
            <div className="ml-auto flex items-center gap-1">
              <Button
                type="button"
                size="sm"
                variant="tertiary"
                icon="minus"
                aria-label="Zoom out"
                disabled={zoom === 100}
                onClick={() => setZoom((value) => value - 25)}
              />
              <span className="w-11 text-center text-xs" aria-live="polite">{zoom}%</span>
              <Button
                type="button"
                size="sm"
                variant="tertiary"
                icon="plus"
                aria-label="Zoom in"
                disabled={zoom === 200}
                onClick={() => setZoom((value) => value + 25)}
              />
              <Button type="button" size="sm" variant="tertiary" onClick={() => setZoom(100)}>Fit</Button>
            </div>
          )}
        </div>

        <div className="min-h-0 flex-1 overflow-auto overscroll-contain bg-[var(--color-surface-muted)]">
          {isPdf ? (
            <PdfViewer
              blob={file}
              title={`Preview of ${file.name}`}
              zoom={zoom}
              onZoomChange={setZoom}
            />
          ) : (
            <div className="flex h-full items-center justify-center p-3">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={previewUrl}
                alt={file.name}
                style={{ width: `${zoom}%`, height: `${zoom}%` }}
                className="max-h-full max-w-full object-contain"
              />
            </div>
          )}
        </div>
      </div>
    </dialog>
  );
}
