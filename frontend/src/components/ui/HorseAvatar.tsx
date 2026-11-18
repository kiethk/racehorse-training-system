'use client';

import { useState } from 'react';
import { fileVariantUrl } from '@/lib/fileVariant';
import { useProtectedFileUrl } from '@/lib/protectedFile';

interface HorseAvatarProps {
  name: string;
  /** API path or URL of the horse photo. It is loaded with the access token. */
  image?: string | null;
  size?: number;
  rounded?: 'sm' | 'md';
}

export function HorseAvatar({ name, image, size = 36, rounded = 'sm' }: HorseAvatarProps) {
  const radius = rounded === 'md' ? 'rounded-[var(--radius-md)]' : 'rounded-[var(--radius-sm)]';
  const [prevImage, setPrevImage] = useState(image);
  const [failedImage, setFailedImage] = useState<string | null>(null);

  if (image !== prevImage) {
    setPrevImage(image);
    setFailedImage(null);
  }

  // Avatars are at most 128 px, so the small copy is enough.
  const { url } = useProtectedFileUrl(fileVariantUrl(image, 'THUMB'));
  const initials = name && name.trim() ? name.trim().slice(0, 2).toUpperCase() : 'HR';

  if (!image || !url || failedImage === image) {
    return (
      <span
        role="img"
        aria-label={`${name} profile`}
        className={`flex shrink-0 items-center justify-center bg-[var(--color-surface-muted)] ${size >= 64 ? 'text-lg font-bold' : 'text-[11px] font-semibold'} text-[var(--color-text-muted)] ${radius}`}
        style={{ width: size, height: size }}
      >
        {initials}
      </span>
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={url}
      alt={`${name} photo`}
      width={size}
      height={size}
      loading="lazy"
      onError={() => setFailedImage(image)}
      style={{ width: size, height: size }}
      className={`shrink-0 object-cover ${radius} bg-[var(--color-surface-muted)]`}
    />
  );
}
