'use client';

import { useState } from 'react';
import { cn } from '@/lib/cn';
import { fileVariantUrl, type FileVariant } from '@/lib/fileVariant';
import { useProtectedFileUrl } from '@/lib/protectedFile';
import { ImageLightbox } from './ImageLightbox';
import { Skeleton } from './Skeleton';

interface ProtectedImageProps {
  /** API path (or URL) of a stored image. It is loaded with the access token. */
  src: string;
  alt: string;
  className?: string;
  /** Copy shown on the page: THUMB for small tiles (up to 128 px), PREVIEW (default) for larger images. */
  size?: FileVariant;
  /** Wraps the image in a control that opens it full size directly in a lightbox preview. */
  openOnClick?: boolean;
}

/** An image stored by the backend. Use this instead of <img> for any file served by a protected endpoint. */
export function ProtectedImage({ src, alt, className, size = 'PREVIEW', openOnClick = false }: ProtectedImageProps) {
  const { url, failed } = useProtectedFileUrl(fileVariantUrl(src, size));
  const [viewing, setViewing] = useState(false);
  // The original is only downloaded once the lightbox opens; the copy on the page stands in until it arrives.
  const { url: originalUrl } = useProtectedFileUrl(viewing ? src : null);

  if (failed) {
    return <p className="text-xs italic text-[var(--color-text-muted)]">The image could not be loaded.</p>;
  }
  if (!url) {
    return <Skeleton className={cn('h-40 w-full max-w-sm', className)} />;
  }

  // eslint-disable-next-line @next/next/no-img-element -- blob URLs cannot go through the image optimizer
  const image = <img src={url} alt={alt} className={className} />;
  if (!openOnClick) return image;

  return (
    <>
      <button
        type="button"
        onClick={() => setViewing(true)}
        aria-label={`Preview full size: ${alt}`}
        className="block w-full cursor-zoom-in overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
      >
        {image}
      </button>
      {viewing && (
        <ImageLightbox
          images={[{ src: originalUrl ?? url, alt }]}
          index={0}
          onIndexChange={() => undefined}
          onClose={() => setViewing(false)}
        />
      )}
    </>
  );
}
