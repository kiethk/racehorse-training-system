'use client';

import { IconButton } from '@/components/ui';
import { Tooltip } from '@/components/ui/Tooltip';
import { useAiChat } from '../context/AiChatContext';

export function AiChatTopBarButton() {
  const { toggleOpen, isOpen } = useAiChat();

  return (
    <Tooltip content="RTMS Copilot AI Assistant" side="bottom">
      <IconButton
        icon="sparkles"
        label="Open RTMS Copilot AI Assistant"
        onClick={toggleOpen}
        aria-expanded={isOpen}
        className="relative rounded-[var(--radius-md)] text-[var(--color-text-secondary)] hover:text-[var(--color-primary)]"
      />
    </Tooltip>
  );
}
