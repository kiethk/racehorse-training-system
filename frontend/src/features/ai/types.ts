/**
 * AI Assistant Types (Read-Only / RAG AI)
 */

export interface AiChatMessageDto {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface AiChatRequestDto {
  message: string;
  conversationHistory?: AiChatMessageDto[];
}

export interface AiChatResponseDto {
  reply: string;
  executedTools?: Record<string, unknown>[];
  /** @deprecated HITL action proposals have been removed */
  pendingProposals?: ActionProposalDto[];
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
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
