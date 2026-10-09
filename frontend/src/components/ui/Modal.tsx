'use client';

import type { ReactNode } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { cn } from '@/lib/cn';
import { IconButton } from './Button';

type Size = 'sm' | 'md' | 'lg' | 'xl';

const sizes: Record<Size, string> = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-5xl',
};

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: ReactNode;
  description?: ReactNode;
  size?: Size;
  /** When false, Escape, the backdrop and the close button do nothing (e.g. while submitting). */
  dismissible?: boolean;
  /** Buttons row, rendered in a sticky footer. */
  footer?: ReactNode;
  /** Use `confirm` for dialogs that must sit above another modal. */
  layer?: 'modal' | 'confirm';
  tone?: 'default' | 'danger';
  children?: ReactNode;
  className?: string;
}

/**
 * The one modal for the app: focus trap, Escape, scroll lock and focus return
 * come from Radix; backdrop, radius, shadow and motion come from tokens.
 */
export function Modal({
  open,
  onClose,
  title,
  description,
  size = 'md',
  dismissible = true,
  footer,
  layer = 'modal',
  tone = 'default',
  children,
  className,
}: ModalProps) {
  const zIndex = layer === 'confirm' ? 'z-[var(--z-confirm)]' : 'z-[var(--z-modal)]';
  const block = (event: Event) => {
    if (!dismissible) event.preventDefault();
  };

  return (
    <Dialog.Root open={open} onOpenChange={(next) => !next && dismissible && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay
          className={cn(
            'fixed inset-0 bg-black/40 backdrop-blur-[2px]',
            'data-[state=open]:animate-in data-[state=open]:fade-in data-[state=open]:duration-200',
            'data-[state=closed]:animate-out data-[state=closed]:fade-out data-[state=closed]:duration-150',
            zIndex,
          )}
        />
        <Dialog.Content
          onEscapeKeyDown={block}
          onPointerDownOutside={block}
          onInteractOutside={block}
          // Radix warns when there is no description unless this is explicitly undefined.
          {...(description ? {} : { 'aria-describedby': undefined })}
          className={cn(
            'fixed left-1/2 top-1/2 flex max-h-[calc(100dvh-2rem)] w-[calc(100vw-2rem)] -translate-x-1/2 -translate-y-1/2 flex-col',
            'rounded-[var(--radius-lg)] border bg-[var(--color-surface)] shadow-[var(--shadow-popover)] outline-none',
            'data-[state=open]:animate-in data-[state=open]:fade-in data-[state=open]:zoom-in-95 data-[state=open]:duration-200',
            'data-[state=closed]:animate-out data-[state=closed]:fade-out data-[state=closed]:zoom-out-95 data-[state=closed]:duration-150',
            tone === 'danger' ? 'border-2 border-[var(--color-danger)]' : 'border-[var(--color-border)]',
            sizes[size],
            zIndex,
            className,
          )}
        >
          <header className="flex shrink-0 items-start justify-between gap-4 border-b border-[var(--color-border)] px-5 py-4">
            <div className="min-w-0">
              <Dialog.Title className="text-lg font-semibold leading-snug text-[var(--color-text-primary)]">
                {title}
              </Dialog.Title>
              {description && (
                <Dialog.Description className="mt-1 text-sm leading-relaxed text-[var(--color-text-secondary)]">
                  {description}
                </Dialog.Description>
              )}
            </div>
            {dismissible && (
              <Dialog.Close asChild>
                <IconButton icon="x" label="Close" size="sm" className="-mr-1.5 -mt-0.5 shrink-0" />
              </Dialog.Close>
            )}
          </header>

          {children && <div className="scroll-slim min-h-0 flex-1 overflow-y-auto px-5 py-4">{children}</div>}

          {footer && (
            <footer className="flex shrink-0 flex-col-reverse justify-end gap-2 border-t border-[var(--color-border)] px-5 py-3 sm:flex-row">
              {footer}
            </footer>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
