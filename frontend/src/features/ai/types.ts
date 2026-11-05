/**
 * AI Assistant Types (Read-Only / RAG AI & Persistent Conversations)
 */

export interface AiConversation {
  id: number;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface AiChatMessageDetail {
  id: number;
  conversationId: number;
  role: 'user' | 'assistant' | 'system';
  content: string;
  toolCalls?: string;
  createdAt: string;
}

export interface AiChatMessageDto {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface AiChatRequestDto {
  message: string;
  conversationId?: number | null;
  conversationHistory?: AiChatMessageDto[];
}

export interface AiChatResponseDto {
  reply: string;
  conversationId?: number;
  executedTools?: Record<string, unknown>[];
  pendingProposals?: unknown[];
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
  toolCalls?: string;
  pendingProposals?: unknown[];
  status?: 'sending' | 'streaming' | 'done' | 'error';
  errorMessage?: string;
}
