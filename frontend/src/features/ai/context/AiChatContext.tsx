'use client';

import {
  createContext,
  useContext,
  useState,
  useCallback,
  useMemo,
  type ReactNode,
} from 'react';
import { toast } from '@/lib/toast';
import { useAuth } from '@/context/AuthContext';
import { sendChatMessage, commitAction } from '../services/aiService';
import type { ChatMessage, ActionProposalDto } from '../types';

export interface AiChatContextValue {
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  toggleOpen: () => void;
  messages: ChatMessage[];
  isLoading: boolean;
  pendingProposalsCount: number;
  sendMessage: (content: string) => Promise<void>;
  commitProposal: (actionId: number, actionToken: string) => Promise<boolean>;
  rejectProposal: (actionId: number) => void;
  clearConversation: () => void;
}

const defaultAiChatValue: AiChatContextValue = {
  isOpen: false,
  setIsOpen: () => {},
  toggleOpen: () => {},
  messages: [],
  isLoading: false,
  pendingProposalsCount: 0,
  sendMessage: async () => {},
  commitProposal: async () => false,
  rejectProposal: () => {},
  clearConversation: () => {},
};

const AiChatContext = createContext<AiChatContextValue | null>(null);

export function AiChatProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    {
      id: 'welcome',
      role: 'assistant',
      content: `Xin chào! Tôi là Trợ lý AI RTMS Copilot. Tôi có thể hỗ trợ bạn tra cứu hồ sơ bệnh án (RAG), kiểm tra bài tập, gợi ý điều chỉnh lịch hoặc đề xuất các thao tác nghiệp vụ có phê duyệt HITL.`,
      timestamp: new Date().toISOString(),
    },
  ]);

  const toggleOpen = useCallback(() => setIsOpen((prev) => !prev), []);

  // Tính số lượng proposal đang PENDING
  const pendingProposalsCount = useMemo(() => {
    return messages.reduce((count, msg) => {
      if (msg.pendingProposals) {
        return count + msg.pendingProposals.length;
      }
      return count;
    }, 0);
  }, [messages]);

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
          pendingProposals: response.pendingProposals || [],
          status: 'done',
        };

        setMessages((prev) => [...prev, assistantMessage]);

        // Nếu có đề xuất cần duyệt và chat đang đóng, tự động mở hoặc thông báo
        if (response.pendingProposals && response.pendingProposals.length > 0) {
          toast.info(`Có ${response.pendingProposals.length} đề xuất hành động cần bạn phê duyệt.`);
        }
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

  const commitProposal = useCallback(
    async (actionId: number, actionToken: string): Promise<boolean> => {
      const idempotencyKey = `commit-${actionId}-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
      try {
        const res = await commitAction({
          idempotencyKey,
          actionToken,
        });

        toast.success(res.message || 'Hành động đã được phê duyệt và cập nhật thành công!');

        // Cập nhật lại messages để gỡ proposal khỏi danh sách pending
        setMessages((prev) =>
          prev.map((msg) => {
            if (!msg.pendingProposals) return msg;
            return {
              ...msg,
              pendingProposals: msg.pendingProposals.filter((p) => p.actionId !== actionId),
            };
          })
        );
        return true;
      } catch (err: unknown) {
        const errorMsg = err instanceof Error ? err.message : 'Phê duyệt thất bại';
        toast.error(errorMsg);
        return false;
      }
    },
    []
  );

  const rejectProposal = useCallback((actionId: number) => {
    toast.info('Đã từ chối áp dụng đề xuất');
    setMessages((prev) =>
      prev.map((msg) => {
        if (!msg.pendingProposals) return msg;
        return {
          ...msg,
          pendingProposals: msg.pendingProposals.filter((p) => p.actionId !== actionId),
        };
      })
    );
  }, []);

  const clearConversation = useCallback(() => {
    setMessages([
      {
        id: `welcome-${Date.now()}`,
        role: 'assistant',
        content: `Cuộc trò chuyện đã được làm mới. Bạn cần hỗ trợ gì hôm nay?`,
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
        pendingProposalsCount,
        sendMessage,
        commitProposal,
        rejectProposal,
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
