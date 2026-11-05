'use client';

import { useRef, type ChangeEvent } from 'react';
import { Avatar } from './Avatar';
import { Button } from './Button';
import { Icon } from './Icon';
import { FileInput } from './Input';

/** Photo picker with a camera overlay. The caller owns validation, preview and saving. */
export function AvatarUpload({ name, image, disabled, onChange, describedBy }: {
  name: string;
  image?: string | null;
  disabled?: boolean;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
  describedBy?: string;
}) {
  const input = useRef<HTMLInputElement>(null);

  return (
    <div className="shrink-0">
      <Button type="button" variant="tertiary" disabled={disabled}
        aria-label="Change profile photo" aria-describedby={describedBy}
        title="Change profile photo" onClick={() => input.current?.click()}
        className="group relative h-24 w-24 overflow-hidden rounded-full p-0">
        <Avatar name={name} image={image} size="lg" />
        <span aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 bottom-0 flex h-1/2 items-center justify-center bg-[var(--color-image-overlay)] text-[var(--color-text-inverse)] opacity-0 transition-opacity duration-[var(--duration-fast)] group-hover:opacity-100 group-focus-visible:opacity-100 [@media(hover:none)]:opacity-100 motion-reduce:transition-none">
          <Icon name="camera" size={22} />
        </span>
      </Button>
      <FileInput ref={input} accept="image/jpeg,image/png" disabled={disabled}
        aria-label="Profile photo file" className="hidden"
        onChange={event => { onChange(event); event.currentTarget.value = ''; }} />
    </div>
  );
}
