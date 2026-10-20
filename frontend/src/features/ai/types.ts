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
  /** @deprecated HITL action proposals have been removed */
  pendingProposals?: ActionProposalDto[];
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
  toolCalls?: string;
  /** @deprecated HITL action proposals have been removed */
  pendingProposals?: ActionProposalDto[];
  status?: 'sending' | 'streaming' | 'done' | 'error';
  errorMessage?: string;
}

/**
 * @deprecated Legacy HITL action proposal types - retained for backward compatibility
 */
export interface ActionProposalDto {
  actionId: number;
  actionToken: string;
  toolName: string;
  title: string;
  targetEntityId: string;
  currentValues: Record<string, unknown>;
  proposedValues: Record<string, unknown>;
  reason: string;
  expiresInSeconds: number;
}

/** @deprecated Legacy HITL action proposal types */
export interface ActionCommitRequestDto {
  idempotencyKey: string;
  actionToken: string;
}

/** @deprecated Legacy HITL action proposal types */
export interface ActionCommitResponseDto {
  actionId: number;
  status: 'PENDING' | 'COMMITTED' | 'REJECTED' | 'EXPIRED';
  toolName: string;
  targetEntityId: string;
  message: string;
}

/** @deprecated Legacy HITL action proposal types */
export type ProposalLocalStatus = 'PENDING' | 'COMMITTING' | 'COMMITTED' | 'REJECTED' | 'EXPIRED';
