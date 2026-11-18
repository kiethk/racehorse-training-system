'use client';

import { useCallback, type Dispatch, type MutableRefObject, type SetStateAction } from 'react';
import { toast } from '@/lib/toast';
import { getConversationMessages } from '../services/aiService';
import type { ChatMessage } from '../types';

type UseConversationSelectionOptions = {
  abortControllerRef: MutableRefObject<AbortController | null>;
  messagesRef: MutableRefObject<ChatMessage[]>;
  setIsLoading: Dispatch<SetStateAction<boolean>>;
  setCurrentConversationId: Dispatch<SetStateAction<number | null>>;
  setMessages: Dispatch<SetStateAction<ChatMessage[]>>;
};

export function useConversationSelection({
  abortControllerRef,
  messagesRef,
  setIsLoading,
  setCurrentConversationId,
  setMessages,
}: UseConversationSelectionOptions) {
  return useCallback(async (id: number) => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    try {
      setIsLoading(true);
      setCurrentConversationId(id);
      const history = await getConversationMessages(id);
      if (history && history.length > 0) {
        const mapped = history.map((message) => {
          let executedTools: Record<string, unknown>[] | undefined;
          if (message.toolCalls) {
            try {
              const parsed = JSON.parse(message.toolCalls);
              if (Array.isArray(parsed)) executedTools = parsed;
            } catch {}
          }
          return {
            id: `msg-${message.id}`,
            role: message.role,
            content: message.content,
            timestamp: message.createdAt,
            toolCalls: message.toolCalls,
            executedTools,
            status: 'done' as const,
          };
        });
        messagesRef.current = mapped;
        setMessages(mapped);
      } else {
        messagesRef.current = [];
        setMessages([]);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Unable to load conversation history';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  }, [abortControllerRef, messagesRef, setCurrentConversationId, setIsLoading, setMessages]);
}
