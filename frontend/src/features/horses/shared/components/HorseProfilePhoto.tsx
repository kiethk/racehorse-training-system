'use client';

import { useState } from 'react';
import { HorseAvatar, Icon, Skeleton } from '@/components/ui';
import { ImageLightbox } from '@/components/ui/ImageLightbox';
import { fileVariantUrl } from '@/lib/fileVariant';
import { useProtectedFileUrl } from '@/lib/protectedFile';

/** Protected portrait with full lightbox preview on click. */
export function HorseProfilePhoto({ name, image }: { name: string; image?: string | null }) {
  // The 128 px portrait uses the small copy; the original is only downloaded when the lightbox opens.
  const { url, failed } = useProtectedFileUrl(fileVariantUrl(image, 'THUMB'));
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const { url: originalUrl } = useProtectedFileUrl(lightboxOpen ? image : null);

  if (image && !url && !failed) {
    return <Skeleton className="h-32 w-32 shrink-0 rounded-[var(--radius-lg)]" />;
  }

  if (url) {
    return (
      <>
        <button
          type="button"
          onClick={() => setLightboxOpen(true)}
          className="group relative h-32 w-32 shrink-0 cursor-pointer overflow-hidden rounded-[var(--radius-md)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          title={`Click to preview full photo of ${name}`}
        >
          <HorseAvatar name={name} image={url} size={128} rounded="md" />
          <span className="absolute inset-0 flex items-center justify-center gap-1 rounded-[var(--radius-md)] bg-black/40 text-xs font-semibold text-white opacity-0 transition-opacity group-hover:opacity-100 backdrop-blur-[1px]">
            <Icon name="maximize" size={16} />
            Preview
          </span>
        </button>

        {lightboxOpen && (
          <ImageLightbox
            images={[{ src: originalUrl ?? url, alt: `${name}'s official photo` }]}
            index={0}
            onIndexChange={() => undefined}
            onClose={() => setLightboxOpen(false)}
          />
        )}
      </>
    );
  }

  return (
    <div className="flex h-32 w-32 shrink-0 flex-col items-center justify-center gap-2 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)]">
      <span className="flex h-14 w-14 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
        <Icon name="horse" size={32} />
      </span>
      <span role="status" className="text-xs text-[var(--color-text-muted)]">
        {failed ? 'Photo unavailable' : 'No photo'}
      </span>
    </div>
  );
}
