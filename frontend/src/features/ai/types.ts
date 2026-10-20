/**
 * AI Assistant & Two-Phase HITL Types
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
  pendingProposals?: ActionProposalDto[];
}

export interface ActionCommitRequestDto {
  idempotencyKey: string;
  actionToken: string;
}

export interface ActionCommitResponseDto {
  actionId: number;
  status: 'PENDING' | 'COMMITTED' | 'REJECTED' | 'EXPIRED';
  toolName: string;
  targetEntityId: string;
  message: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
  pendingProposals?: ActionProposalDto[];
  status?: 'sending' | 'streaming' | 'done' | 'error';
  errorMessage?: string;
}

export type ProposalLocalStatus = 'PENDING' | 'COMMITTING' | 'COMMITTED' | 'REJECTED' | 'EXPIRED';
