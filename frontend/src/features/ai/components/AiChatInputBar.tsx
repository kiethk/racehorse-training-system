'use client';

import { useRef, useEffect } from 'react';
import { ArrowUp, Square } from 'lucide-react';
import { Button, FormField, Textarea } from '@/components/ui';

interface AiChatInputBarProps {
  input: string;
  setInput: (value: string) => void;
  isLoading: boolean;
  isOpen: boolean;
  onSend: () => void | Promise<void>;
  onStop: () => void;
}

export function AiChatInputBar({
  input,
  setInput,
  isLoading,
  isOpen,
  onSend,
  onStop,
}: AiChatInputBarProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (isOpen) {
      setTimeout(() => textareaRef.current?.focus(), 150);
    }
  }, [isOpen]);

  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    const computedHeight = Math.max(el.scrollHeight, 56);
    const finalHeight = Math.min(computedHeight, 160);
    el.style.height = `${finalHeight}px`;
    el.style.overflowY = computedHeight > 160 ? 'auto' : 'hidden';
  }, [input]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void onSend();
    }
  };

  return (
    <div className="shrink-0 border-t border-[var(--color-border)] bg-[var(--color-surface)] p-3">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (isLoading) {
            onStop();
          } else {
            void onSend();
          }
        }}
        className="mx-auto flex items-end gap-2 rounded-3xl border border-[var(--color-border-strong)] bg-[var(--color-surface)] p-2 focus-within:border-[var(--color-focus)] focus-within:ring-2 focus-within:ring-[var(--color-focus)]/25 shadow-xs transition-shadow"
      >
        <FormField
          label="Ask about training schedule, horse health, stabling..."
          className="min-w-0 flex-1 space-y-0 [&>label]:sr-only"
        >
          <Textarea
            ref={textareaRef}
            rows={2}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask about training schedule, horse health, stabling..."
            className="max-h-[160px] min-h-[56px] flex-1 resize-none border-0 border-transparent bg-transparent px-3.5 py-2.5 text-xs sm:text-sm text-[var(--color-text-primary)] placeholder:text-[var(--color-text-secondary)] shadow-none rounded-none outline-none focus-visible:border-transparent focus-visible:ring-0 focus-visible:ring-transparent focus-visible:outline-none leading-normal overflow-y-hidden [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
          />
        </FormField>

        {isLoading ? (
          <Button
            type="button"
            variant="primary"
            onClick={onStop}
            aria-label="Stop generating"
            title="Stop generating"
            className="grid size-10 shrink-0 cursor-pointer place-items-center rounded-2xl bg-[var(--color-primary)] text-white hover:bg-[var(--color-primary-hover)] transition-all active:scale-95 shadow-xs"
          >
            <Square className="size-3.5 fill-current" aria-hidden="true" />
          </Button>
        ) : (
          <Button
            type="submit"
            variant="primary"
            disabled={!input.trim()}
            aria-label="Send message"
            title="Send message"
            className="grid size-10 shrink-0 cursor-pointer place-items-center rounded-2xl bg-[var(--color-primary)] text-white hover:bg-[var(--color-primary-hover)] transition-all active:scale-95 disabled:cursor-not-allowed disabled:opacity-30 shadow-xs"
          >
            <ArrowUp className="size-4" aria-hidden="true" />
          </Button>
        )}
      </form>
    </div>
  );
}
