'use client';

import { useMemo, useState } from 'react';
import { Button, IconButton } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { ImageLightbox } from '@/components/ui/ImageLightbox';
import { displayError, formatDate } from '@/lib/display';
import { openProtectedFile, useProtectedFileUrls } from '@/lib/protectedFile';
import { toast } from '@/lib/toast';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import type { AdmissionDocument } from '../../types';
import { isImageDocument } from '../lib/documentKind';
import { AdmissionDocumentPreview } from './AdmissionDocumentPreview';
import { AdmissionImageGallery } from './AdmissionImageGallery';

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

const typeName = (document: AdmissionDocument) => documentNames[document.documentType] || document.documentType;
const fileName = (document: AdmissionDocument) => document.originalFileName || typeName(document);

export function AdmissionDocumentsSection({ documents, assetUrl }: { documents: AdmissionDocument[]; assetUrl: (url: string) => string }) {
  const [selectedDocument, setSelectedDocument] = useState<AdmissionDocument | null>(null);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);

  // Images open in the lightbox; PDFs and unknown files keep the document preview.
  const imageDocuments = useMemo(() => documents.filter(isImageDocument), [documents]);
  const imageSources = useMemo(() => imageDocuments.map((document) => document.fileUrl), [imageDocuments]);
  const loaded = useProtectedFileUrls(imageSources);
  const gallery = imageDocuments.map((document) => ({
    key: document.id,
    url: loaded[document.fileUrl]?.url ?? null,
    failed: loaded[document.fileUrl]?.failed ?? false,
    alt: `${fileName(document)} · ${typeName(document)}`,
    caption: typeName(document),
  }));
  // The lightbox shows only loaded images; map gallery positions to lightbox positions and back.
  const viewable = gallery.flatMap((image, index) => (image.url ? [{ index, src: image.url, alt: image.alt }] : []));
  const openImage = (galleryIndex: number) => {
    const position = viewable.findIndex((image) => image.index === galleryIndex);
    if (position >= 0) setLightboxIndex(position);
  };

  const openDocument = (document: AdmissionDocument) => {
    const galleryIndex = imageDocuments.indexOf(document);
    // An image that could not be loaded falls back to the document preview, which explains the failure.
    if (galleryIndex >= 0 && gallery[galleryIndex].url) openImage(galleryIndex);
    else setSelectedDocument(document);
  };

  return (
    <>
      <Panel padded>
        <div className="flex items-center justify-between gap-3">
          <SectionTitle>Documents</SectionTitle>
          <span className="text-xs text-[var(--color-text-muted)]">{documents.length} files</span>
        </div>
        <AdmissionImageGallery images={gallery} onOpen={openImage} />
        {documents.length ? (
          <ul className="mt-4 space-y-2 text-sm">
            {documents.map((document) => (
              <li key={document.id} className="flex items-start gap-2.5 rounded-[var(--radius-md)] border border-[var(--color-border)] p-2.5 transition-colors hover:bg-[var(--color-surface-muted)]">
                <Icon name={isImageDocument(document) ? 'image' : 'file-text'} className="mt-0.5 shrink-0" />
                <div className="min-w-0 flex-1">
                  <Button
                    variant="link"
                    aria-haspopup="dialog"
                    onClick={() => openDocument(document)}
                    className="block max-w-full truncate text-left"
                  >
                    {fileName(document)}
                  </Button>
                  <p className="mt-0.5 truncate text-xs text-[var(--color-text-muted)]">
                    {typeName(document)} · Recorded {date(document.recordDate)} · Uploaded {date(document.uploadedAt)}
                  </p>
                </div>
                <IconButton
                  icon="external-link"
                  size="sm"
                  label={`Open ${fileName(document)} in new tab`}
                  onClick={() => {
                    openProtectedFile(document.fileUrl).catch((error) => toast.error(displayError(error, 'Unable to open the file.')));
                  }}
                  className="shrink-0"
                />
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-4 text-sm italic text-[var(--color-text-muted)]">No documents attached.</p>
        )}
      </Panel>
      {selectedDocument && (
        <AdmissionDocumentPreview
          key={selectedDocument.id}
          document={selectedDocument}
          label={typeName(selectedDocument)}
          fileUrl={assetUrl(selectedDocument.fileUrl)}
          onClose={() => setSelectedDocument(null)}
        />
      )}
      <ImageLightbox
        images={viewable}
        index={lightboxIndex}
        onIndexChange={setLightboxIndex}
        onClose={() => setLightboxIndex(null)}
      />
    </>
  );
}
