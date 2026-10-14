'use client';

import { HorseAvatar, Icon, Skeleton } from '@/components/ui';
import { useProtectedFileUrl } from '@/lib/protectedFile';

/** Protected portrait with an explicit, honest empty/error state. */
export function HorseProfilePhoto({ name, image }: { name: string; image?: string | null }) {
  const { url, failed } = useProtectedFileUrl(image);

  if (image && !url && !failed) {
    return <Skeleton className="h-32 w-32 shrink-0 rounded-[var(--radius-lg)]" />;
  }

  if (url) {
    return <HorseAvatar name={name} image={url} size={128} rounded="md" />;
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
