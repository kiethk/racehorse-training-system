import { apiPost, getAccessToken } from '@/services/api';
import type {
  AiChatRequestDto,
  AiChatResponseDto,
  ActionCommitRequestDto,
  ActionCommitResponseDto,
} from '../types';

/**
 * Gửi tin nhắn đồng bộ đến AI Agent Orchestrator.
 */
export async function sendChatMessage(request: AiChatRequestDto): Promise<AiChatResponseDto> {
  return apiPost<AiChatResponseDto>('/api/v1/ai/chat', request);
}

/**
 * Xác nhận thực thi hành động HITL (Commit) với Action Token và Idempotency Key.
 */
export async function commitAction(request: ActionCommitRequestDto): Promise<ActionCommitResponseDto> {
  return apiPost<ActionCommitResponseDto>('/api/v1/ai/actions/commit', request);
}

/**
 * Gửi tin nhắn và nhận phản hồi SSE streaming từ AI Agent.
 */
export async function streamChatMessage(
  request: AiChatRequestDto,
  onChunk: (text: string) => void,
  signal?: AbortSignal
): Promise<void> {
  const configuredApiUrl = process.env.NEXT_PUBLIC_API_URL?.trim();
  const baseUrl = (configuredApiUrl || (process.env.NODE_ENV === 'development' ? 'http://localhost:8080' : '')).replace(/\/+$/, '');
  const token = getAccessToken();

  const response = await fetch(`${baseUrl}/api/v1/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    credentials: 'include',
    body: JSON.stringify(request),
    signal,
  });

  if (!response.ok) {
    const errorText = await response.text().catch(() => 'Lỗi kết nối streaming AI');
    throw new Error(errorText || `API error: ${response.status}`);
  }

  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error('ReadableStream không được trình duyệt hỗ trợ');
  }

  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed.startsWith('data:')) {
        const dataContent = trimmed.substring(5).trim();
        if (dataContent === '[DONE]') {
          return;
        }
        if (dataContent) {
          onChunk(dataContent);
        }
      }
    }
  }
}
