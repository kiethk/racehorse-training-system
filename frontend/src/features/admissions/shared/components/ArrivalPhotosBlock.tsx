'use client';

import { useState } from 'react';
import { ImageLightbox, ImageThumbnail } from '@/components/ui/ImageLightbox';
import { fileVariantUrl } from '@/lib/fileVariant';
import { useProtectedFileUrl, useProtectedFileUrls } from '@/lib/protectedFile';

interface ArrivalPhoto {
  id: number;
  documentType: string;
  fileUrl: string;
  originalFileName: string | null;
}

export function ArrivalPhotosBlock({ documents }: { documents: ArrivalPhoto[] }) {
  const [index, setIndex] = useState<number | null>(null);
  const photos = documents.filter(document => document.documentType === 'ARRIVAL_PHOTO');
  // Tiles show the small copy; only the photo open in the lightbox downloads its original.
  const sources = useProtectedFileUrls(photos.map(photo => fileVariantUrl(photo.fileUrl, 'THUMB')));
  const thumbnails = photos.flatMap(photo => {
    const url = sources[fileVariantUrl(photo.fileUrl, 'THUMB')]?.url;
    return url ? [{ src: url, alt: photo.originalFileName || 'Arrival photo', original: photo.fileUrl }] : [];
  });
  const shown = index === null ? null : thumbnails[index];
  const { url: originalUrl } = useProtectedFileUrl(shown ? shown.original : null);
  if (photos.length === 0) return null;

  const images = thumbnails.map(({ src, alt }) => ({ src, alt }));
  const lightboxImages = thumbnails.map(({ src, alt }, position) => ({
    src: position === index && originalUrl ? originalUrl : src,
    alt,
  }));

  return (
    <div className="space-y-3">
      <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Arrival photos</h3>
      <div className="grid grid-cols-3 gap-2">
        {images.map((image, photoIndex) => (
          <ImageThumbnail key={image.src} src={image.src} alt={image.alt} onClick={() => setIndex(photoIndex)} />
        ))}
      </div>
      <ImageLightbox images={lightboxImages} index={index} onIndexChange={setIndex} onClose={() => setIndex(null)} />
    </div>
  );
}
