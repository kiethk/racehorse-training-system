'use client';

import { useRef, useCallback, useEffect, useState } from 'react';
import { ArrowDown, Loader2, RotateCw } from 'lucide-react';
import { Button } from '@/components/ui';
import { MarkdownMessage } from './MarkdownMessage';
import { AiToolExecutionSteps } from './AiToolExecutionSteps';
import { AiMessageActionBar } from './AiMessageActionBar';
import { formatClockLabel } from '@/lib/display';
import type { ChatMessage } from '../types';

interface AiMessageListProps {
  messages: ChatMessage[];
  isLoading: boolean;
  starterPrompts: string[];
  nextPrompts: string[];
  lastAssistantIdx: number;
  isOpen: boolean;
  isExpanded: boolean;
  onSendMessage: (text: string) => void | Promise<void>;
  onRegenerateMessage: (id: string) => void | Promise<void>;
}

export function AiMessageList({
  messages,
  isLoading,
  starterPrompts,
  nextPrompts,
  lastAssistantIdx,
  isOpen,
  isExpanded,
  onSendMessage,
  onRegenerateMessage,
}: AiMessageListProps) {
  const [showScrollBottom, setShowScrollBottom] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = useCallback((smooth = true) => {
    messagesEndRef.current?.scrollIntoView({
      behavior: smooth ? 'smooth' : 'auto',
    });
  }, []);

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isLoading, isOpen, isExpanded, scrollToBottom]);

  const handleScroll = () => {
    const el = scrollContainerRef.current;
    if (!el) return;
    const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    setShowScrollBottom(distanceToBottom > 80);
  };

  return (
    <div className="relative flex flex-1 flex-col overflow-hidden">
      <div
        ref={scrollContainerRef}
        onScroll={handleScroll}
        className={`flex-1 overflow-y-auto px-4 py-4 ${
          messages.length === 0 ? 'flex flex-col justify-end' : 'space-y-6'
        }`}
      >
        {/* Empty state: New conversation ("Suggested Prompts") */}
        {messages.length === 0 && (
          <div className="flex flex-col justify-end w-full space-y-3 pb-2">
            <p className="text-xs font-medium text-[var(--color-text-secondary)]">
              Suggested Prompts
            </p>
            <div className="flex flex-col gap-2">
              {starterPrompts.map((prompt, idx) => (
                <Button
                  key={idx}
                  type="button"
                  variant="secondary"
                  onClick={() => void onSendMessage(prompt)}
                  disabled={isLoading}
                  className="block h-auto w-fit max-w-full cursor-pointer whitespace-normal rounded-xl border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3.5 py-2 text-left text-xs text-[var(--color-text-primary)] outline-hidden hover:bg-[var(--color-surface-muted)] transition-colors shadow-2xs"
                >
                  {prompt}
                </Button>
              ))}
            </div>
          </div>
        )}

        {/* Message list */}
        {messages.map((msg, index) => {
          const isUser = msg.role === 'user';
          const isLastAssistant = index === lastAssistantIdx;

          if (isUser) {
            return (
              <div key={msg.id} className="flex flex-col items-end">
                <p className="ml-auto w-fit max-w-[80%] rounded-3xl bg-[var(--color-surface)] border border-[var(--color-border-strong)] px-4 py-2.5 text-xs sm:text-sm text-[var(--color-text-primary)] whitespace-pre-wrap [overflow-wrap:anywhere] shadow-xs">
                  {msg.content}
                </p>
                <span className="mt-1 pr-2 text-[10px] text-[var(--color-text-secondary)]">
                  {formatClockLabel(msg.timestamp)}
                </span>
              </div>
            );
          }

          return (
            <div key={msg.id} className="group flex flex-col items-start w-full">
              <div className="max-w-[65ch] w-full space-y-2 text-left text-xs sm:text-sm leading-relaxed text-[var(--color-text-primary)] text-pretty">
                {msg.executedTools && msg.executedTools.length > 0 && (
                  <AiToolExecutionSteps tools={msg.executedTools} />
                )}

                {msg.status === 'error' ? (
                  <div className="space-y-1.5 rounded-xl border border-red-500/20 bg-red-500/5 p-3 text-left">
                    <p className="text-xs sm:text-sm font-medium text-red-600">
                      Assistant could not respond
                    </p>
                    <p className="text-[11px] text-[var(--color-text-secondary)]">
                      {msg.errorMessage || 'Server is busy or encountered a connection issue.'}
                    </p>
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => void onRegenerateMessage(msg.id)}
                      className="mt-1 inline-flex h-auto cursor-pointer items-center gap-1.5 rounded-xl border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2.5 py-1 text-xs text-[var(--color-text-primary)] hover:bg-[var(--color-surface-muted)] transition-colors"
                    >
                      <RotateCw className="size-3" aria-hidden="true" />
                      <span>Retry</span>
                    </Button>
                  </div>
                ) : (
                  <MarkdownMessage content={msg.content} />
                )}

                {msg.status !== 'error' && (
                  <AiMessageActionBar
                    content={msg.content}
                    onRegenerate={() => void onRegenerateMessage(msg.id)}
                    isStopped={msg.isStopped}
                    isLast={isLastAssistant}
                    isLoading={isLoading}
                  />
                )}

                {isLastAssistant && !isLoading && nextPrompts.length > 0 && (
                  <div className="space-y-1.5 pt-2">
                    <p className="text-xs text-[var(--color-text-secondary)]">Follow-up suggestions</p>
                    <div className="flex flex-col gap-1.5">
                      {nextPrompts.map((p, idx) => (
                        <Button
                          key={idx}
                          type="button"
                          variant="secondary"
                          onClick={() => void onSendMessage(p)}
                          className="block h-auto w-fit max-w-full cursor-pointer whitespace-normal rounded-xl border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-left text-xs text-[var(--color-text-primary)] outline-hidden hover:bg-[var(--color-surface-muted)] transition-colors truncate"
                        >
                          {p}
                        </Button>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              <span className="mt-1 px-1 text-[9px] text-[var(--color-text-secondary)]">
                {formatClockLabel(msg.timestamp)}
              </span>
            </div>
          );
        })}

        {isLoading && (
          <div className="max-w-[65ch] space-y-2 text-left">
            <div className="flex items-center gap-2 text-xs text-[var(--color-text-secondary)]">
              <Loader2 className="size-3.5 animate-spin text-[var(--color-primary)]" />
              <span>Copilot is generating a response...</span>
              <span className="inline-block h-[1.1em] w-2 rounded-xs bg-[var(--color-text-primary)]/30 align-text-bottom" />
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {showScrollBottom && (
        <Button
          type="button"
          variant="secondary"
          onClick={() => scrollToBottom(true)}
          aria-label="Scroll to bottom"
          title="Scroll to bottom"
          className="absolute bottom-20 left-1/2 -translate-x-1/2 z-10 flex size-8 cursor-pointer items-center justify-center rounded-full border border-[var(--color-border-strong)] bg-[var(--color-surface)] p-0 text-[var(--color-text-primary)] shadow-md hover:bg-[var(--color-surface-muted)] transition-all"
        >
          <ArrowDown className="size-4" aria-hidden="true" />
        </Button>
      )}
    </div>
  );
}
