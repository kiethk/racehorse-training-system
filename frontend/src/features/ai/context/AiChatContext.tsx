'use client';

import { createContext, type ReactNode, useCallback, useContext, useState } from 'react';
import { toast } from '@/lib/toast';
import { sendChatMessage } from '../services/aiService';
import type { ChatMessage } from '../types';

export interface AiChatContextValue {
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  toggleOpen: () => void;
  messages: ChatMessage[];
  isLoading: boolean;
  sendMessage: (content: string) => Promise<void>;
  clearConversation: () => void;
}

const defaultAiChatValue: AiChatContextValue = {
  isOpen: false,
  setIsOpen: () => {},
  toggleOpen: () => {},
  messages: [],
  isLoading: false,
  sendMessage: async () => {},
  clearConversation: () => {},
};

const AiChatContext = createContext<AiChatContextValue | null>(null);

export function AiChatProvider({ children }: { children: ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    {
      id: 'welcome',
      role: 'assistant',
      content: 'Xin chào! Tôi là Trợ lý AI cho hệ thống RTMS.',
      timestamp: new Date().toISOString(),
    },
  ]);

  const toggleOpen = useCallback(() => setIsOpen((prev) => !prev), []);

  const sendMessage = useCallback(
    async (text: string) => {
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

      // Đưa tin nhắn của user vào danh sách
      setMessages((prev) => [...prev, userMessage]);
      setIsLoading(true);

      // Chuẩn bị conversationHistory từ các tin trước
      const historyPayload = messages
        .filter((m) => m.role === 'user' || m.role === 'assistant')
        .slice(-6)
        .map((m) => ({
          role: m.role,
          content: m.content,
        }));

      try {
        const response = await sendChatMessage({
          message: trimmed,
          conversationHistory: historyPayload,
        });

        const assistantMsgId = `ast-${Date.now()}`;
        const assistantMessage: ChatMessage = {
          id: assistantMsgId,
          role: 'assistant',
          content: response.reply || 'Đã xử lý xong yêu cầu của bạn.',
          timestamp: new Date().toISOString(),
          status: 'done',
        };

        setMessages((prev) => [...prev, assistantMessage]);
      } catch (err: unknown) {
        const errorMsg = err instanceof Error ? err.message : 'Có lỗi khi trò chuyện với AI';
        toast.error(errorMsg);

        const errorAssistantMsg: ChatMessage = {
          id: `ast-err-${Date.now()}`,
          role: 'assistant',
          content: `Xin lỗi, không thể kết nối hoặc nhận phản hồi từ AI: ${errorMsg}`,
          timestamp: new Date().toISOString(),
          status: 'error',
          errorMessage: errorMsg,
        };
        setMessages((prev) => [...prev, errorAssistantMsg]);
      } finally {
        setIsLoading(false);
      }
    },
    [messages, isLoading]
  );

  const clearConversation = useCallback(() => {
    setMessages([
      {
        id: `welcome-${Date.now()}`,
        role: 'assistant',
        content: 'Cuộc trò chuyện đã được làm mới. Bạn cần hỗ trợ gì hôm nay?',
        timestamp: new Date().toISOString(),
      },
    ]);
  }, []);

  return (
    <AiChatContext.Provider
      value={{
        isOpen,
        setIsOpen,
        toggleOpen,
        messages,
        isLoading,
        sendMessage,
        clearConversation,
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
