'use client';

import { Icon, ImageThumbnail, Skeleton } from '@/components/ui';
import { cn } from '@/lib/cn';

export interface GalleryImage {
  key: string | number;
  /** Blob URL once loaded; null while loading or when it failed. */
  url: string | null;
  failed: boolean;
  alt: string;
  caption: string;
}

/**
 * Thumbnails of the images attached to an admission. Each opens the lightbox on that image; images that are
 * still loading or failed are shown as placeholders so the grid does not jump.
 */
export function AdmissionImageGallery({ images, onOpen }: { images: GalleryImage[]; onOpen: (index: number) => void }) {
  if (images.length === 0) return null;
  return (
    // Fixed-size tiles that wrap: the documents panel can be a narrow side column or a full-width tab.
    <ul className="mt-4 flex flex-wrap gap-2" aria-label="Images">
      {images.map((image, index) => (
        <li key={image.key} className="w-28 min-w-0">
          {image.url ? (
            <ImageThumbnail src={image.url} alt={image.alt} onClick={() => onOpen(index)} className="h-24" />
          ) : image.failed ? (
            <div
              className={cn(
                'flex h-24 flex-col items-center justify-center gap-1 rounded-[var(--radius-md)] border border-dashed',
                'border-[var(--color-border)] text-xs text-[var(--color-text-muted)]',
              )}
            >
              <Icon name="image" />
              Not available
            </div>
          ) : (
            <Skeleton className="h-24 w-full rounded-[var(--radius-md)]" />
          )}
          <p className="mt-1 truncate text-xs text-[var(--color-text-muted)]" title={image.caption}>{image.caption}</p>
        </li>
      ))}
    </ul>
  );
}
