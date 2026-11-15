import { useEffect, useState } from 'react';
import { admissionsApi } from '../../services/api';
import type { AdmissionDocument } from '../../types';
import { isImageDocument, isPdfDocument } from '../lib/documentKind';

export type LoadedAdmissionFile = { url: string; kind: 'image' | 'pdf' | 'unsupported'; blob: Blob };

export function useAdmissionDocumentFile(document: AdmissionDocument) {
  const [file, setFile] = useState<LoadedAdmissionFile | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let objectUrl: string | undefined;
    admissionsApi.getDocumentFile(document.fileUrl, controller.signal).then((rawBlob) => {
      if (controller.signal.aborted) return;
      const fn = (document.originalFileName || '').toLowerCase();
      const urlPath = document.fileUrl.split('?')[0].toLowerCase();
      const isPdf = rawBlob.type === 'application/pdf'
        || fn.endsWith('.pdf') || urlPath.endsWith('.pdf') || isPdfDocument(document);
      const isImage = ['image/jpeg', 'image/png', 'image/webp'].includes(rawBlob.type)
        || /\.(jpe?g|png|webp)$/i.test(fn) || isImageDocument(document);

      let blob = rawBlob;
      if (isPdf && rawBlob.type !== 'application/pdf') {
        blob = new Blob([rawBlob], { type: 'application/pdf' });
      } else if (isImage && !['image/jpeg', 'image/png', 'image/webp'].includes(rawBlob.type)) {
        const mime = fn.endsWith('.png') ? 'image/png' : fn.endsWith('.webp') ? 'image/webp' : 'image/jpeg';
        blob = new Blob([rawBlob], { type: mime });
      }
      objectUrl = URL.createObjectURL(blob);
      setFile({ url: objectUrl, kind: isImage ? 'image' : isPdf ? 'pdf' : 'unsupported', blob });
    }).catch(() => {
      if (!controller.signal.aborted) setError('Unable to load this file. Try opening it in a new tab.');
    });
    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [document]);

  return { file, error, setError };
}
