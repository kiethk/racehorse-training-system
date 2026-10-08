import { useEffect, type ReactNode } from 'react';
import { Button } from './Button';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: 'primary' | 'danger';
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
  confirmDisabled?: boolean;
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  tone = 'primary',
  onConfirm,
  onCancel,
  loading = false,
  confirmDisabled = false,
}: ConfirmDialogProps) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !loading) {
        e.stopImmediatePropagation();
        e.preventDefault();
        onCancel();
      }
    };
    window.addEventListener('keydown', onKey, true);
    return () => window.removeEventListener('keydown', onKey, true);
  }, [open, onCancel, loading]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={() => !loading && onCancel()} aria-hidden="true" />
      <div
        role="dialog"
        aria-modal="true"
        className="relative max-h-[calc(100dvh-2rem)] w-full max-w-md overflow-y-auto rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-xl shadow-black/10 sm:p-6"
      >
        <h2 className="text-[16px] font-semibold text-[var(--color-text-primary)]">{title}</h2>
        <div className="mt-2 text-[13px] leading-relaxed text-[var(--color-text-secondary)]">
          {description}
        </div>
        <div className="mt-5 flex flex-col justify-end gap-2 sm:flex-row">
          <Button variant="secondary" onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </Button>
          <Button variant={tone === 'danger' ? 'destructive' : 'primary'} onClick={onConfirm} disabled={loading || confirmDisabled}>
            {loading ? 'Submitting...' : confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
