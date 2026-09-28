interface HorseAvatarProps {
  name: string;
  image?: string;
  size?: number;
  rounded?: 'sm' | 'md';
}

export function HorseAvatar({ name, image, size = 36, rounded = 'sm' }: HorseAvatarProps) {
  const radius = rounded === 'md' ? 'rounded-[var(--radius-md)]' : 'rounded-[var(--radius-sm)]';

  if (!image) {
    return (
      <span
        aria-label={`${name} profile`}
        className={`flex shrink-0 items-center justify-center bg-[var(--color-surface-muted)] text-[11px] font-semibold text-[var(--color-text-muted)] ${radius}`}
        style={{ width: size, height: size }}
      >
        {name.slice(0, 2).toUpperCase()}
      </span>
    );
  }

  return (
    <img
      src={image}
      alt={`${name} profile`}
      width={size}
      height={size}
      loading="lazy"
      style={{ width: size, height: size }}
      className={`shrink-0 object-cover ${radius} bg-[var(--color-surface-muted)]`}
    />
  );
}
