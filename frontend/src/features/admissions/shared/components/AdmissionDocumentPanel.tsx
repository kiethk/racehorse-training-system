'use client';

import { useState } from 'react';
import { Button, Icon, Select } from '@/components/ui';
import { ImageLightbox } from '@/components/ui/ImageLightbox';
import type { AdmissionDocument } from '../../types';
import { isImageDocument, isPdfDocument } from '../lib/documentKind';
import { documentNames } from './AdmissionDocumentsSection';
import { useAdmissionDocumentFile } from './useAdmissionDocumentFile';

interface AdmissionDocumentPanelProps {
  documents: AdmissionDocument[];
  documentId: number;
  onSelectDocument: (id: number) => void;
  onClose: () => void;
}

function DocumentContent({ document }: { document: AdmissionDocument }) {
  const { file, error, setError } = useAdmissionDocumentFile(document);
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const label = documentNames[document.documentType] ?? document.documentType;
  const filename = document.originalFileName ?? label;

  return (
    <div className="min-h-0 flex-1 overflow-auto bg-[var(--color-surface-muted)]" aria-busy={!file && !error}>
      {error ? (
        <div role="alert" className="p-4 text-sm">{error}</div>
      ) : !file ? (
        <div role="status" className="p-4 text-sm text-[var(--color-text-muted)]">Loading document…</div>
      ) : file.kind === 'image' && isImageDocument(document) ? (
        <>
          <Button type="button" variant="tertiary" aria-label={`Zoom ${label}`} className="flex h-full min-h-64 w-full items-center justify-center p-3" onClick={() => setLightboxOpen(true)}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={file.url} alt={filename} className="max-h-full max-w-full object-contain" onError={() => setError('This image could not be displayed.')} />
          </Button>
          <ImageLightbox images={[{ src: file.url, alt: filename }]} index={lightboxOpen ? 0 : null} onIndexChange={() => {}} onClose={() => setLightboxOpen(false)} />
        </>
      ) : file.kind === 'pdf' && isPdfDocument(document) ? (
        <iframe src={file.url} className="h-full min-h-96 w-full border-0" aria-label={`Preview of ${filename}`} />
      ) : (
        <div className="p-4 text-sm">Preview is available for image and PDF documents.</div>
      )}
    </div>
  );
}

export function AdmissionDocumentPanel({ documents, documentId, onSelectDocument, onClose }: AdmissionDocumentPanelProps) {
  const document = documents.find((item) => item.id === documentId);
  if (!document) return null;
  const label = documentNames[document.documentType] ?? document.documentType;

  return (
    <section aria-label="Admission document" className="flex min-h-96 min-w-0 flex-col overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-panel)] lg:sticky lg:top-[var(--sticky-top)] lg:h-[calc(100dvh-var(--sticky-top)-1rem)] lg:max-h-[calc(100dvh-var(--sticky-top)-1rem)]">
      <div className="flex items-center justify-between gap-2 border-b border-[var(--color-border)] p-3">
        <h3 className="truncate text-sm font-semibold">{label}</h3>
        <Button type="button" variant="tertiary" size="sm" aria-label="Close document" className="p-1" onClick={onClose}><Icon name="x" size={14} /></Button>
      </div>
      <div className="border-b border-[var(--color-border)] p-3">
        <Select aria-label="Choose document" value={documentId} onChange={(event) => onSelectDocument(Number(event.target.value))}>
          {documents.map((item) => (
            <option key={item.id} value={item.id}>{documentNames[item.documentType] ?? item.documentType}</option>
          ))}
        </Select>
      </div>
      <DocumentContent key={document.id} document={document} />
    </section>
  );
}
