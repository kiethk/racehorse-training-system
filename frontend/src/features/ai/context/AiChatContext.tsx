'use client';

import { createContext, type ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import { toast } from '@/lib/toast';
import {
  deleteConversation,
  getConversationMessages,
  getConversations,
  renameConversation,
  sendChatMessage,
} from '../services/aiService';
import type { AiConversation, ChatMessage } from '../types';

export interface AiChatContextValue {
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  toggleOpen: () => void;
  messages: ChatMessage[];
  isLoading: boolean;
  sendMessage: (content: string) => Promise<void>;
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

const WELCOME_MESSAGE: ChatMessage = {
  id: 'welcome',
  role: 'assistant',
  content: 'Xin chào! Tôi là Trợ lý AI cho hệ thống RTMS. Tôi có thể hỗ trợ tra cứu lịch tập, sức khỏe, chuồng trại và các dữ liệu liên quan.',
  timestamp: new Date().toISOString(),
  status: 'done',
};

export function AiChatProvider({ children }: { children: ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([WELCOME_MESSAGE]);
  const [conversations, setConversations] = useState<AiConversation[]>([]);
  const [currentConversationId, setCurrentConversationId] = useState<number | null>(null);
  const [isLoadingConversations, setIsLoadingConversations] = useState(false);

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

  // Tự động tải danh sách hội thoại khi mở khung chat
  useEffect(() => {
    if (isOpen) {
      void refreshConversations();
    }
  }, [isOpen, refreshConversations]);

  const selectConversation = useCallback(async (id: number) => {
    try {
      setIsLoading(true);
      setCurrentConversationId(id);
      const history = await getConversationMessages(id);
      if (history && history.length > 0) {
        setMessages(
          history.map((m) => ({
            id: `msg-${m.id}`,
            role: m.role,
            content: m.content,
            timestamp: m.createdAt,
            toolCalls: m.toolCalls,
            status: 'done',
          }))
        );
      } else {
        setMessages([
          {
            id: `welcome-${id}`,
            role: 'assistant',
            content: 'Cuộc trò chuyện này chưa có tin nhắn nào. Bạn có thể bắt đầu đặt câu hỏi!',
            timestamp: new Date().toISOString(),
            status: 'done',
          },
        ]);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể tải lịch sử trò chuyện';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const createNewChat = useCallback(() => {
    setCurrentConversationId(null);
    setMessages([
      {
        id: `welcome-${Date.now()}`,
        role: 'assistant',
        content: 'Bắt đầu cuộc trò chuyện mới. Bạn cần hỗ trợ gì hôm nay?',
        timestamp: new Date().toISOString(),
        status: 'done',
      },
    ]);
  }, []);

  const deleteChat = useCallback(
    async (id: number) => {
      try {
        await deleteConversation(id);
        setConversations((prev) => prev.filter((c) => c.id !== id));
        toast.success('Đã xóa cuộc trò chuyện');
        if (currentConversationId === id) {
          createNewChat();
        }
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Lỗi khi xóa cuộc trò chuyện';
        toast.error(msg);
      }
    },
    [currentConversationId, createNewChat]
  );

  const renameChat = useCallback(async (id: number, title: string) => {
    try {
      const updated = await renameConversation(id, title);
      setConversations((prev) => prev.map((c) => (c.id === id ? { ...c, title: updated.title } : c)));
      toast.success('Đã cập nhật tiêu đề cuộc trò chuyện');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Lỗi khi đổi tên cuộc trò chuyện';
      toast.error(msg);
    }
  }, []);

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

      // Chuẩn bị conversationHistory từ các tin trước làm fallback context
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
          conversationId: currentConversationId,
          conversationHistory: historyPayload,
        });

        // Nếu Backend sinh mới hoặc cập nhật conversationId
        if (response.conversationId && response.conversationId !== currentConversationId) {
          setCurrentConversationId(response.conversationId);
        }

        const assistantMsgId = `ast-${Date.now()}`;
        const assistantMessage: ChatMessage = {
          id: assistantMsgId,
          role: 'assistant',
          content: response.reply || 'Đã xử lý xong yêu cầu của bạn.',
          timestamp: new Date().toISOString(),
          status: 'done',
        };

        setMessages((prev) => [...prev, assistantMessage]);

        // Cập nhật lại danh sách sidebar cuộc trò chuyện
        void refreshConversations();
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
    [currentConversationId, messages, isLoading, refreshConversations]
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
