'use client';

import { useState } from 'react';
import { ImageLightbox, ImageThumbnail } from '@/components/ui/ImageLightbox';
import { useProtectedFileUrls } from '@/lib/protectedFile';

interface ArrivalPhoto {
  id: number;
  documentType: string;
  fileUrl: string;
  originalFileName: string | null;
}

export function ArrivalPhotosBlock({ documents }: { documents: ArrivalPhoto[] }) {
  const [index, setIndex] = useState<number | null>(null);
  const photos = documents.filter(document => document.documentType === 'ARRIVAL_PHOTO');
  const sources = useProtectedFileUrls(photos.map(photo => photo.fileUrl));
  if (photos.length === 0) return null;

  const images = photos.flatMap(photo => {
    const url = sources[photo.fileUrl]?.url;
    return url ? [{ src: url, alt: photo.originalFileName || 'Arrival photo' }] : [];
  });

  return (
    <div className="space-y-3">
      <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Arrival photos</h3>
      <div className="grid grid-cols-3 gap-2">
        {images.map((image, photoIndex) => (
          <ImageThumbnail key={image.src} src={image.src} alt={image.alt} onClick={() => setIndex(photoIndex)} />
        ))}
      </div>
      <ImageLightbox images={images} index={index} onIndexChange={setIndex} onClose={() => setIndex(null)} />
    </div>
  );
}
