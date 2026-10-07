'use client';

import { useState } from 'react';
import { Icon } from '@/components/ui/Icon';
import { formatDate } from '@/lib/display';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import type { AdmissionDocument } from '../../types';
import { AdmissionDocumentPreview } from './AdmissionDocumentPreview';

const documentNames: Record<string, string> = {
  HORSE_PHOTO: 'Horse photo',
  REGISTRATION_DOCUMENT: 'Registration document',
  PEDIGREE_CERTIFICATE: 'Pedigree certificate',
  VACCINATION_RECORD: 'Vaccination record',
  DEWORMING_RECORD: 'Deworming record',
  HEALTH_CERTIFICATE: 'Health certificate',
  PREVIOUS_MEDICAL_RECORD: 'Previous medical record',
  PREVIOUS_INJURY_RECORD: 'Previous injury record',
};

function date(value: string | null) {
  return value ? formatDate(value) : 'Not recorded';
}

export function AdmissionDocumentsSection({ documents, assetUrl }: { documents: AdmissionDocument[]; assetUrl: (url: string) => string }) {
  const [selectedDocument, setSelectedDocument] = useState<AdmissionDocument | null>(null);
  return (
    <>
      <Panel padded className="bg-[var(--color-surface)]">
        <div className="flex items-center justify-between gap-3">
          <SectionTitle>Documents</SectionTitle>
          <span className="text-[11px] text-[var(--color-text-muted)]">{documents.length} files</span>
        </div>
        {documents.length ? (
          <ul className="mt-4 space-y-2 text-[12px]">
            {documents.map((document) => (
              <li key={document.id} className="flex items-start gap-2 rounded border border-[var(--color-border)] p-2 transition-colors hover:bg-[var(--color-surface-muted)]">
                <Icon name={document.documentType === 'HORSE_PHOTO' ? 'image' : 'file-text'} className="mt-0.5 shrink-0" />
                <div className="min-w-0 flex-1">
                  <button type="button" onClick={() => setSelectedDocument(document)} aria-haspopup="dialog" className="block max-w-full truncate rounded text-left font-medium text-[var(--color-primary)] hover:underline focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
                    {document.originalFileName || documentNames[document.documentType] || document.documentType}
                  </button>
                  <p className="mt-0.5 truncate text-[11px] text-[var(--color-text-muted)]">
                    {documentNames[document.documentType] || document.documentType} · Recorded {date(document.recordDate)} · Uploaded {date(document.uploadedAt)}
                  </p>
                </div>
                <a href={assetUrl(document.fileUrl)} target="_blank" rel="noreferrer" aria-label={`Open ${document.originalFileName || documentNames[document.documentType] || document.documentType} in new tab`} className="shrink-0 rounded p-1 text-[var(--color-text-muted)] hover:text-[var(--color-primary)] focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]">
                  <Icon name="external-link" size={14} />
                </a>
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-4 text-[12px] italic text-[var(--color-text-muted)]">No documents attached.</p>
        )}
      </Panel>
      {selectedDocument && (
        <AdmissionDocumentPreview
          key={selectedDocument.id}
          document={selectedDocument}
          label={documentNames[selectedDocument.documentType] || selectedDocument.documentType}
          fileUrl={assetUrl(selectedDocument.fileUrl)}
          onClose={() => setSelectedDocument(null)}
        />
      )}
    </>
  );
}
