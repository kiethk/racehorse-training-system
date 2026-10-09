import { cn } from '@/lib/cn';

function getInitials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .substring(0, 2)
    .toUpperCase();
}

/** Initials avatar for a person. For horses use HorseAvatar. */
export function Avatar({
  name,
  size = 'md',
  className,
}: {
  name: string;
  size?: 'sm' | 'md';
  className?: string;
}) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'flex shrink-0 items-center justify-center rounded-full bg-[var(--color-primary-soft)] font-semibold text-[var(--color-primary)]',
        size === 'sm' ? 'h-7 w-7 text-[11px]' : 'h-8 w-8 text-xs',
        className,
      )}
    >
      {getInitials(name)}
    </span>
  );
}
