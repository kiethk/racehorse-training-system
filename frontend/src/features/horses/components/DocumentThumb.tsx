'use client';

import { useEffect, useState } from 'react';
import { Button, Icon, ImageThumbnail, Skeleton } from '@/components/ui';
import { cn } from '@/lib/cn';
import { useProtectedFileUrl } from '@/lib/protectedFile';
import { renderPdfFirstPage, usePdfFirstPage } from '@/lib/pdfThumbnail';
import { isImageDocument, isPdfDocument } from '@/features/admissions/shared/lib/documentKind';
import type { HorseDocumentType } from '../types';

const BOX = 'h-44 w-full rounded-[var(--radius-md)] border border-[var(--color-border-subtle)] bg-[var(--color-surface-muted)]';
const PDF_WIDTH = 480;

/** A large, clickable preview: the image itself, or the first page of a PDF. */
function Frame({ url, failed, alt, onOpen, pdf }: {
  url: string | null; failed: boolean; alt: string; onOpen: () => void; pdf: boolean;
}) {
  if (url) {
    return (
      <ImageThumbnail src={url} alt={alt} onClick={onOpen}
        className={cn(BOX, 'h-44')} imageClassName={pdf ? 'object-cover object-top' : 'object-contain'} />
    );
  }
  if (!failed) return <Skeleton className={BOX} />;
  return (
    <div className={cn(BOX, 'flex flex-col items-center justify-center gap-2 text-xs text-[var(--color-text-muted)]')}>
      <Icon name={pdf ? 'file-text' : 'image'} size={24} />
      No preview
      <Button type="button" size="sm" variant="tertiary" onClick={onOpen}>Open file</Button>
    </div>
  );
}

/** A stored document, fetched with the access token. */
export function StoredDocumentThumb({ fileUrl, documentType, fileName, onOpen }: {
  fileUrl: string; documentType: HorseDocumentType; fileName: string | null; onOpen: () => void;
}) {
  const doc = { documentType, fileUrl, originalFileName: fileName };
  const image = isImageDocument(doc);
  const pdf = !image && isPdfDocument(doc);
  const imageState = useProtectedFileUrl(image ? fileUrl : null);
  const pdfState = usePdfFirstPage(pdf ? fileUrl : null, PDF_WIDTH);
  const state = image ? imageState : pdf ? pdfState : { url: null, failed: true };
  return <Frame {...state} alt={fileName ?? documentType} onOpen={onOpen} pdf={!image} />;
}

/** A file picked in the browser and not uploaded yet. */
export function LocalDocumentThumb({ file, onOpen }: { file: File; onOpen: () => void }) {
  const pdf = file.type === 'application/pdf';
  const [state, setState] = useState<{ file: File; url: string | null } | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let objectUrl: string | undefined;
    const ready = (blob: Blob) => {
      if (controller.signal.aborted) return;
      objectUrl = URL.createObjectURL(blob);
      setState({ file, url: objectUrl });
    };
    if (pdf) {
      renderPdfFirstPage(file, PDF_WIDTH, controller.signal).then(ready)
        .catch(() => { if (!controller.signal.aborted) setState({ file, url: null }); });
    } else {
      ready(file);
    }
    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [file, pdf]);

  const current = state && state.file === file ? state : null;
  return <Frame url={current?.url ?? null} failed={current !== null && current.url === null} alt={file.name} onOpen={onOpen} pdf={pdf} />;
}
