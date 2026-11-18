'use client';

import { createContext, type ReactNode, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { toast } from '@/lib/toast';
import {
  deleteConversation,
  getConversations,
  renameConversation,
  sendChatMessage,
} from '../services/aiService';
import type { AiConversation, ChatMessage } from '../types';
import { useConversationSelection } from '../hooks/useConversationSelection';

export interface AiChatContextValue {
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  toggleOpen: () => void;
  messages: ChatMessage[];
  isLoading: boolean;
  sendMessage: (content: string, overrideHistory?: ChatMessage[]) => Promise<void>;
  stopGenerating: () => void;
  regenerateMessage: (id?: string) => Promise<void>;
  clearConversation: () => void;
  // Persistent Conversation Threads
  conversations: AiConversation[];
  currentConversationId: number | null;
  isLoadingConversations: boolean;
  selectConversation: (id: number) => Promise<void>;
  createNewChat: () => void;
  deleteChat: (id: number) => Promise<void>;
  renameChat: (id: number, title: string) => Promise<void>;
  refreshConversations: () => Promise<void>;
}

const defaultAiChatValue: AiChatContextValue = {
  isOpen: false,
  setIsOpen: () => {},
  toggleOpen: () => {},
  messages: [],
  isLoading: false,
  sendMessage: async () => {},
  stopGenerating: () => {},
  regenerateMessage: async () => {},
  clearConversation: () => {},
  conversations: [],
  currentConversationId: null,
  isLoadingConversations: false,
  selectConversation: async () => {},
  createNewChat: () => {},
  deleteChat: async () => {},
  renameChat: async () => {},
  refreshConversations: async () => {},
};

const AiChatContext = createContext<AiChatContextValue | null>(null);

export function AiChatProvider({ children }: { children: ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [conversations, setConversations] = useState<AiConversation[]>([]);
  const [currentConversationId, setCurrentConversationId] = useState<number | null>(null);
  const [isLoadingConversations, setIsLoadingConversations] = useState(false);

  const abortControllerRef = useRef<AbortController | null>(null);
  const messagesRef = useRef<ChatMessage[]>(messages);

  useEffect(() => {
    messagesRef.current = messages;
  }, [messages]);

  const toggleOpen = useCallback(() => setIsOpen((prev) => !prev), []);

  const refreshConversations = useCallback(async () => {
    try {
      setIsLoadingConversations(true);
      const list = await getConversations();
      setConversations(list || []);
    } catch {
      // Ignored if user not yet logged in
    } finally {
      setIsLoadingConversations(false);
    }
  }, []);

  // Automatically load the conversation list when the chat window opens
  useEffect(() => {
    if (isOpen) {
      let isMounted = true;
      void Promise.resolve().then(() => {
        if (!isMounted) return;
        setIsLoadingConversations(true);
        return getConversations()
          .then((list) => {
            if (isMounted) setConversations(list || []);
          })
          .catch(() => {
            // Ignored if user not yet logged in
          })
          .finally(() => {
            if (isMounted) setIsLoadingConversations(false);
          });
      });
      return () => {
        isMounted = false;
      };
    }
  }, [isOpen]);

  // Cancel the request if the component unmounts
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    };
  }, []);

  const selectConversation = useConversationSelection({
    abortControllerRef,
    messagesRef,
    setIsLoading,
    setCurrentConversationId,
    setMessages,
  });

  const createNewChat = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setIsLoading(false);
    setCurrentConversationId(null);
    messagesRef.current = [];
    setMessages([]);
  }, []);

  const stopGenerating = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setIsLoading(false);
  }, []);

  const deleteChat = useCallback(
    async (id: number) => {
      // Optimistic Update
      setConversations((prev) => prev.filter((c) => c.id !== id));
      if (currentConversationId === id) {
        createNewChat();
      }

      try {
        await deleteConversation(id);
        toast.success('Conversation deleted');
      } catch (err: unknown) {
        void refreshConversations();
        const msg = err instanceof Error ? err.message : 'Error deleting conversation';
        toast.error(msg);
      }
    },
    [currentConversationId, createNewChat, refreshConversations]
  );

  const renameChat = useCallback(async (id: number, title: string) => {
    const trimmed = title.trim();
    if (!trimmed) return;
    setConversations((prev) => prev.map((c) => (c.id === id ? { ...c, title: trimmed } : c)));
    try {
      const updated = await renameConversation(id, trimmed);
      setConversations((prev) => prev.map((c) => (c.id === id ? { ...c, title: updated.title } : c)));
      toast.success('Conversation title updated');
    } catch (err: unknown) {
      void refreshConversations();
      const msg = err instanceof Error ? err.message : 'Error renaming conversation';
      toast.error(msg);
    }
  }, [refreshConversations]);

  const sendMessage = useCallback(
    async (text: string, overrideHistory?: ChatMessage[]) => {
      const trimmed = text.trim();
      if (!trimmed || isLoading) return;

      const userMsgId = `usr-${Date.now()}`;
      const userMessage: ChatMessage = {
        id: userMsgId,
        role: 'user',
        content: trimmed,
        timestamp: new Date().toISOString(),
        status: 'done',
      };

      const baseMessages = overrideHistory ?? messagesRef.current;
      const newMessages = [...baseMessages, userMessage];
      messagesRef.current = newMessages;
      setMessages(newMessages);
      setIsLoading(true);

      const controller = new AbortController();
      abortControllerRef.current = controller;

      const historyPayload = newMessages
        .filter((m) => m.role === 'user' || m.role === 'assistant')
        .slice(-6)
        .map((m) => ({
          role: m.role,
          content: m.content,
        }));

      try {
        const response = await sendChatMessage(
          {
            message: trimmed,
            conversationId: currentConversationId,
            conversationHistory: historyPayload,
          },
          controller.signal
        );

        if (response.conversationId && response.conversationId !== currentConversationId) {
          setCurrentConversationId(response.conversationId);
        }

        let executedTools = response.executedTools;
        const rawResponse = response as unknown as Record<string, unknown>;
        if (!executedTools && typeof rawResponse.toolCalls === 'string') {
          try {
            const parsed = JSON.parse(rawResponse.toolCalls);
            if (Array.isArray(parsed)) executedTools = parsed;
          } catch {}
        }

        const assistantMsgId = `ast-${Date.now()}`;
        const assistantMessage: ChatMessage = {
          id: assistantMsgId,
          role: 'assistant',
          content: response.reply || 'Your request has been processed.',
          timestamp: new Date().toISOString(),
          executedTools,
          toolCalls: response.executedTools ? JSON.stringify(response.executedTools) : undefined,
          status: 'done',
        };

        const updatedMessages = [...messagesRef.current, assistantMessage];
        messagesRef.current = updatedMessages;
        setMessages(updatedMessages);
        void refreshConversations();
      } catch (err: unknown) {
        const isAbort =
          (err instanceof DOMException && err.name === 'AbortError') ||
          (err instanceof Error && (err.name === 'AbortError' || err.message.toLowerCase().includes('aborted')));

        if (isAbort) {
          const stoppedMsg: ChatMessage = {
            id: `ast-stopped-${Date.now()}`,
            role: 'assistant',
            content: '',
            timestamp: new Date().toISOString(),
            status: 'stopped',
            isStopped: true,
          };
          const updatedMessages = [...messagesRef.current, stoppedMsg];
          messagesRef.current = updatedMessages;
          setMessages(updatedMessages);
        } else {
          const errorMsg = err instanceof Error ? err.message : 'An error occurred while communicating with the AI assistant.';
          toast.error(errorMsg);

          const errorAssistantMsg: ChatMessage = {
            id: `ast-err-${Date.now()}`,
            role: 'assistant',
            content: 'The assistant could not respond.',
            errorMessage: errorMsg,
            timestamp: new Date().toISOString(),
            status: 'error',
          };
          const updatedMessages = [...messagesRef.current, errorAssistantMsg];
          messagesRef.current = updatedMessages;
          setMessages(updatedMessages);
        }
      } finally {
        abortControllerRef.current = null;
        setIsLoading(false);
      }
    },
    [currentConversationId, isLoading, refreshConversations]
  );

  const regenerateMessage = useCallback(
    async (targetId?: string) => {
      if (isLoading) return;
      const current = messagesRef.current;
      let idx = targetId ? current.findIndex((m) => m.id === targetId) : current.length - 1;
      if (idx < 0) idx = current.length - 1;

      let userMsgIdx = -1;
      for (let i = idx; i >= 0; i--) {
        if (current[i].role === 'user') {
          userMsgIdx = i;
          break;
        }
      }
      if (userMsgIdx === -1) return;

      const userPrompt = current[userMsgIdx].content;
      const sliced = current.slice(0, userMsgIdx);
      messagesRef.current = sliced;
      setMessages(sliced);

      await sendMessage(userPrompt, sliced);
    },
    [isLoading, sendMessage]
  );

  return (
    <AiChatContext.Provider
      value={{
        isOpen,
        setIsOpen,
        toggleOpen,
        messages,
        isLoading,
        sendMessage,
        stopGenerating,
        regenerateMessage,
        clearConversation: createNewChat,
        conversations,
        currentConversationId,
        isLoadingConversations,
        selectConversation,
        createNewChat,
        deleteChat,
        renameChat,
        refreshConversations,
      }}
    >
      {children}
    </AiChatContext.Provider>
  );
}

export function useAiChat(): AiChatContextValue {
  const ctx = useContext(AiChatContext);
  return ctx || defaultAiChatValue;
}
