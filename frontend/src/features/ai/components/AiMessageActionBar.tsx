'use client';

import { useState } from 'react';
import { Copy, Check, RotateCw } from 'lucide-react';
import { Button } from '@/components/ui';

export function AiMessageActionBar({
  content,
  onRegenerate,
  isStopped,
  isLast,
  isLoading,
}: {
  content: string;
  onRegenerate: () => void;
  isStopped?: boolean;
  isLast: boolean;
  isLoading: boolean;
}) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback
    }
  };

  return (
    <div
      className={`-ml-2 flex items-center gap-1 pt-1 text-xs text-[var(--color-text-secondary)] transition-opacity ${
        isLast
          ? 'opacity-100'
          : 'opacity-0 max-md:opacity-100 group-hover:opacity-100 group-focus-within:opacity-100'
      }`}
    >
      {content.trim() && (
        <Button
          type="button"
          variant="tertiary"
          onClick={handleCopy}
          aria-label="Copy content"
          title="Copy"
          className="flex size-8 cursor-pointer items-center justify-center rounded-lg p-0 hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] transition-colors"
        >
          {copied ? (
            <Check className="size-3.5 text-[var(--color-primary)]" aria-hidden="true" />
          ) : (
            <Copy className="size-3.5" aria-hidden="true" />
          )}
        </Button>
      )}

      <Button
        type="button"
        variant="tertiary"
        onClick={onRegenerate}
        disabled={isLoading}
        aria-label="Regenerate response"
        title="Regenerate"
        className="flex size-8 cursor-pointer items-center justify-center rounded-lg p-0 hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] transition-colors disabled:opacity-40"
      >
        <RotateCw className="size-3.5" aria-hidden="true" />
      </Button>

      {isStopped && (
        <span className="ml-1 text-[11px] text-[var(--color-text-secondary)] font-medium">
          Stopped
        </span>
      )}
    </div>
  );
}
