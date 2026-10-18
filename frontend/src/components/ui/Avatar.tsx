'use client';

import { useState } from 'react';
import { cn } from '@/lib/cn';
import { useProtectedFileUrl } from '@/lib/protectedFile';

function getInitials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .substring(0, 2)
    .toUpperCase();
}

/** Person photo or initials. Protected API images and local blob previews share the same component. */
export function Avatar({
  name,
  image,
  size = 'md',
  className,
}: {
  name: string;
  image?: string | null;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}) {
  const { url } = useProtectedFileUrl(image);
  const [failedImage, setFailedImage] = useState<string | null>(null);
  const sizes = { sm: 'h-7 w-7 text-[11px]', md: 'h-8 w-8 text-xs', lg: 'h-24 w-24 text-2xl' };
  if (url && failedImage !== image) {
    return (
      // eslint-disable-next-line @next/next/no-img-element -- authenticated files use temporary blob URLs
      <img src={url} alt="" aria-hidden="true" onError={() => setFailedImage(image ?? null)}
        className={cn('shrink-0 rounded-full object-cover', sizes[size], className)} />
    );
  }
  return (
    <span
      aria-hidden="true"
      className={cn(
        'flex shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-soft)] font-semibold text-[var(--color-primary)]',
        sizes[size],
        className,
      )}
    >
      {getInitials(name)}
    </span>
  );
}
