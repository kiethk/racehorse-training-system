'use client';

import { Icon } from '@/components/ui/Icon';
import { Tooltip } from '@/components/ui/Tooltip';
import { useAiChat } from '../context/AiChatContext';

export function AiChatTopBarButton() {
  const { toggleOpen, isOpen } = useAiChat();

  return (
    <Tooltip content="Trợ lý AI RTMS Copilot" side="bottom">
      <button
        type="button"
        onClick={toggleOpen}
        aria-label="Mở Trợ lý AI RTMS Copilot"
        aria-expanded={isOpen}
        className="relative flex h-9 w-9 items-center justify-center rounded-[var(--radius-md)] text-[var(--color-text-secondary)] outline-none transition-colors hover:bg-[var(--color-surface-muted)] hover:text-emerald-600 focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
      >
        <Icon name="sparkles" size={18} className="text-emerald-600 dark:text-emerald-400" />
      </button>
    </Tooltip>
  );
}
