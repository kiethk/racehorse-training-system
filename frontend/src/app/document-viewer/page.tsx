'use client';

import { Suspense, useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { PdfViewer } from '@/components/ui/PdfViewer';
import { apiGetBlob } from '@/services/api';

function ViewerContent() {
  const searchParams = useSearchParams();
  const fileUrl = searchParams.get('url') || '';
  const title = searchParams.get('name') || 'Document';

  const [blob, setBlob] = useState<Blob | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [zoom, setZoom] = useState(100);

  const isPdf = title.toLowerCase().endsWith('.pdf')
    || fileUrl.toLowerCase().includes('.pdf')
    || (blob && blob.type === 'application/pdf');

  const isImage = /\.(jpe?g|png|webp)$/i.test(title)
    || (blob && ['image/jpeg', 'image/png', 'image/webp'].includes(blob.type));

  useEffect(() => {
    if (!fileUrl) {
      setError('No document URL provided.');
      setLoading(false);
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);

    // If source is a blob created in current session
    if (fileUrl.startsWith('blob:') || fileUrl.startsWith('data:')) {
      fetch(fileUrl)
        .then((res) => res.blob())
        .then((resBlob) => {
          if (!active) return;
          setBlob(resBlob);
          setLoading(false);
        })
        .catch((err) => {
          if (!active) return;
          setError(err instanceof Error ? err.message : 'Unable to load local file.');
          setLoading(false);
        });
      return;
    }

    // Otherwise load through authenticated API
    apiGetBlob(fileUrl)
      .then((resBlob) => {
        if (!active) return;
        setBlob(resBlob);
        setLoading(false);
      })
      .catch((err) => {
        if (!active) return;
        setError(err instanceof Error ? err.message : 'Unable to load protected file from server.');
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [fileUrl]);

  const handleDownload = () => {
    if (!blob) return;
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = title;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  };

  return (
    <div className="flex h-screen w-screen flex-col overflow-hidden bg-slate-900 text-slate-100">
      {/* Top toolbar */}
      <header className="flex h-12 shrink-0 items-center justify-between border-b border-slate-700 bg-slate-800/90 px-4 backdrop-blur-sm">
        <div className="flex items-center gap-2 overflow-hidden pr-4">
          <Icon name={isPdf ? 'file-text' : isImage ? 'image' : 'file'} size={18} className="text-sky-400 shrink-0" />
          <h1 className="truncate text-sm font-medium text-slate-100" title={title}>
            {title}
          </h1>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {blob && (
            <Button
              type="button"
              size="sm"
              variant="secondary"
              icon="download"
              onClick={handleDownload}
            >
              Download
            </Button>
          )}
          <Button
            type="button"
            size="sm"
            variant="tertiary"
            icon="x"
            aria-label="Close tab"
            onClick={() => window.close()}
          />
        </div>
      </header>

      {/* Main viewer container */}
      <main className="flex min-h-0 flex-1 flex-col overflow-hidden bg-slate-950">
        {loading ? (
          <div className="flex h-full flex-col items-center justify-center gap-3 text-sm text-slate-400">
            <span className="h-6 w-6 animate-spin rounded-full border-2 border-sky-400 border-t-transparent" />
            <p>Loading document preview…</p>
          </div>
        ) : error ? (
          <div className="flex h-full flex-col items-center justify-center p-6 text-center text-sm text-rose-400">
            <Icon name="alert-triangle" size={32} className="mb-2 opacity-80" />
            <p className="font-semibold">Unable to display document</p>
            <p className="mt-1 text-xs text-slate-400">{error}</p>
          </div>
        ) : blob && isPdf ? (
          <PdfViewer
            blob={blob}
            title={title}
            zoom={zoom}
            onZoomChange={setZoom}
            onError={(err) => setError(err)}
          />
        ) : blob && isImage ? (
          <div className="flex h-full items-center justify-center overflow-auto p-4">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={URL.createObjectURL(blob)}
              alt={title}
              className="max-h-full max-w-full object-contain rounded shadow-lg"
            />
          </div>
        ) : (
          <div className="flex h-full flex-col items-center justify-center p-6 text-center text-sm text-slate-400">
            <p>Preview is only available for PDF and image documents.</p>
            {blob && (
              <Button type="button" size="sm" variant="primary" className="mt-4" onClick={handleDownload}>
                Download File
              </Button>
            )}
          </div>
        )}
      </main>
    </div>
  );
}

export default function DocumentViewerPage() {
  return (
    <Suspense
      fallback={
        <div className="flex h-screen w-screen items-center justify-center bg-slate-900 text-slate-400">
          <span className="h-6 w-6 animate-spin rounded-full border-2 border-sky-400 border-t-transparent" />
        </div>
      }
    >
      <ViewerContent />
    </Suspense>
  );
}
