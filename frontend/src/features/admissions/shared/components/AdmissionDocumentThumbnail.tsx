'use client';

import { Icon, ImageThumbnail, Skeleton, type IconName } from '@/components/ui';
import { cn } from '@/lib/cn';
import { usePdfFirstPage } from '@/lib/pdfThumbnail';

const SIZE = 'h-20 w-28 shrink-0';

interface TileProps {
  /** Picture URL once ready; null while loading or when it failed. */
  url: string | null;
  failed: boolean;
  alt: string;
  onOpen: () => void;
  /** Shown in place of the picture when it cannot be drawn. */
  fallbackIcon: IconName;
  imageClassName?: string;
}

/** One fixed-size tile: the picture when ready, a skeleton while loading, a placeholder when it failed. */
function Tile({ url, failed, alt, onOpen, fallbackIcon, imageClassName }: TileProps) {
  if (url) return <ImageThumbnail src={url} alt={alt} onClick={onOpen} className={SIZE} imageClassName={imageClassName} />;
  if (failed) {
    return (
      <div
        className={cn(
          SIZE,
          'flex flex-col items-center justify-center gap-1 rounded-[var(--radius-md)] border border-dashed',
          'border-[var(--color-border)] text-xs text-[var(--color-text-muted)]',
        )}
      >
        <Icon name={fallbackIcon} />
        No preview
      </div>
    );
  }
  return <Skeleton className={cn(SIZE, 'rounded-[var(--radius-md)]')} />;
}

/** The picture of an image document, shown in its row instead of the file name. Opens the lightbox. */
export function AdmissionImageThumbnail(props: Omit<TileProps, 'fallbackIcon' | 'imageClassName'>) {
  return <Tile {...props} fallbackIcon="image" />;
}

/**
 * The first page of a PDF document, drawn with pdf.js (loaded only when a PDF is on screen). The top of the
 * page is shown, where titles usually are. Opens the document preview.
 */
export function AdmissionPdfThumbnail({ source, alt, onOpen }: { source: string; alt: string; onOpen: () => void }) {
  const { url, failed } = usePdfFirstPage(source);
  return <Tile url={url} failed={failed} alt={alt} onOpen={onOpen} fallbackIcon="file-text" imageClassName="object-top" />;
}
