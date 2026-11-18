'use client';

import { useMemo, useState } from 'react';
import { Sparkles, X, GripVertical } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { Button, IconButton } from '@/components/ui';
import { useAiChat } from '../context/AiChatContext';
import { getQuickPromptsByRole, getRoleDisplayName } from '../lib/aiUtils';
import { useChatWindowDrag } from '../hooks/useChatWindowDrag';
import { AiChatSidebar } from './AiChatSidebar';
import { AiChatInputBar } from './AiChatInputBar';
import { AiMessageList } from './AiMessageList';

export function AiChatWidget() {
  const { user } = useAuth();
  const {
    isOpen,
    toggleOpen,
    setIsOpen,
    messages,
    isLoading,
    sendMessage,
    stopGenerating,
    regenerateMessage,
    conversations,
    currentConversationId,
    isLoadingConversations,
    selectConversation,
    createNewChat,
    deleteChat,
    renameChat,
  } = useAiChat();

  const [input, setInput] = useState('');
  const [isExpanded, setIsExpanded] = useState(false);
  const [showHistory, setShowHistory] = useState(false);

  const {
    dialogRef,
    fabRef,
    anchor,
    dialogPosition,
    isDraggingAny,
    handleHeaderPointerDown,
    handleHeaderPointerMove,
    handleHeaderPointerUp,
    handleFabPointerDown,
    handleFabPointerMove,
    handleFabPointerUp,
    handleFabPointerCancel,
    handleFabClick,
    handleResetPosition,
  } = useChatWindowDrag(isExpanded, toggleOpen);

  const handleSend = async () => {
    if (!input.trim() || isLoading) return;
    const text = input;
    setInput('');
    await sendMessage(text);
  };

  // Initial quick prompts based on role
  const rolePrompts = useMemo(() => getQuickPromptsByRole(user?.role), [user?.role]);
  const starterPrompts = useMemo(() => rolePrompts.slice(0, 3), [rolePrompts]);

  // Follow-up suggestions under the latest response: exclude questions already sent by the user
  const nextPrompts = useMemo(() => {
    if (messages.length === 0) return [];
    const asked = new Set(
      messages
        .filter((m) => m.role === 'user')
        .map((m) => m.content.trim().toLowerCase())
    );
    const unasked = rolePrompts.filter((p) => !asked.has(p.trim().toLowerCase()));
    return unasked.slice(0, 2);
  }, [rolePrompts, messages]);

  const lastAssistantIdx = useMemo(() => {
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i].role === 'assistant') return i;
    }
    return -1;
  }, [messages]);

  return (
    <>
      {/* Floating Action Button (FAB) */}
      <div
        ref={fabRef}
        style={
          anchor
            ? {
                left: `${anchor.x}px`,
                top: `${anchor.y}px`,
                bottom: 'auto',
                right: 'auto',
              }
            : undefined
        }
        className={`fixed z-50 flex items-center ${
          !anchor ? 'bottom-6 right-6' : ''
        } ${
          isOpen && isExpanded ? 'hidden pointer-events-none' : ''
        }`}
      >
        <Button
          type="button"
          variant="primary"
          onClick={handleFabClick}
          onDoubleClick={handleResetPosition}
          onPointerDown={handleFabPointerDown}
          onPointerMove={handleFabPointerMove}
          onPointerUp={handleFabPointerUp}
          onPointerCancel={handleFabPointerCancel}
          onLostPointerCapture={handleFabPointerCancel}
          aria-label={isOpen ? 'Close RTMS Copilot' : 'Open RTMS Copilot'}
          title="Click to open • Drag to move • Double-click to reset position"
          className={`group relative flex h-14 w-14 items-center justify-center rounded-full bg-[var(--color-primary)] p-0 text-white shadow-xl shadow-[var(--color-primary)]/25 outline-none select-none touch-none ${
            isDraggingAny
              ? 'cursor-grabbing scale-105 transition-none shadow-2xl'
              : 'cursor-grab transition-all duration-300 hover:scale-105 hover:bg-[var(--color-primary-hover)] active:scale-95 focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]'
          }`}
        >
          {isOpen ? (
            <X className="size-6 transition-transform group-hover:rotate-90 pointer-events-none" />
          ) : (
            <Sparkles className="size-6 animate-pulse pointer-events-none" />
          )}
        </Button>
      </div>

      {/* Floating chat window */}
      {isOpen && (
        <div
          ref={dialogRef}
          role="dialog"
          aria-label="RTMS Copilot Dialog"
          style={
            dialogPosition
              ? {
                  left: `${dialogPosition.x}px`,
                  top: `${dialogPosition.y}px`,
                  bottom: 'auto',
                  right: 'auto',
                }
              : undefined
          }
          className={`fixed z-50 flex flex-col overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] shadow-2xl shadow-neutral-900/15 backdrop-blur-md ${
            isDraggingAny
              ? 'select-none transition-none'
              : 'transition-all duration-300 ease-in-out'
          } ${
            !dialogPosition
              ? isExpanded
                ? 'bottom-6 right-6'
                : 'bottom-22 right-6'
              : ''
          } ${
            isExpanded
              ? 'h-[720px] w-[800px] max-w-[calc(100vw-32px)] max-h-[calc(100vh-48px)]'
              : 'h-[560px] w-[380px] max-w-[calc(100vw-32px)] max-h-[calc(100vh-110px)]'
          } animate-in fade-in slide-in-from-bottom-4`}
        >
          {/* Header */}
          <div
            onPointerDown={handleHeaderPointerDown}
            onPointerMove={handleHeaderPointerMove}
            onPointerUp={handleHeaderPointerUp}
            onPointerCancel={handleHeaderPointerUp}
            onLostPointerCapture={handleHeaderPointerUp}
            onDoubleClick={handleResetPosition}
            className={`flex h-14 shrink-0 items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface-muted)] px-3.5 select-none touch-none ${
              isDraggingAny ? 'cursor-grabbing' : 'cursor-grab'
            }`}
            title="Drag to move the chat window • Double-click to reset position"
          >
            <div className="flex items-center gap-2">
              <GripVertical
                className="size-3.5 text-[var(--color-text-secondary)]/50 shrink-0 cursor-grab active:cursor-grabbing"
                aria-hidden="true"
              />
              {/* Toggle conversation history */}
              <IconButton
                icon="panel-left"
                size="sm"
                label={showHistory ? 'Close conversation history' : 'Conversation history'}
                onClick={() => setShowHistory(!showHistory)}
                title={showHistory ? 'Close conversation history' : 'Conversation history'}
                className={`flex h-8 w-8 cursor-pointer items-center justify-center rounded-xl px-0 transition-colors [&>svg]:size-4 ${
                  showHistory
                    ? 'bg-[var(--color-primary)] text-white'
                    : 'bg-[var(--color-primary)]/10 text-[var(--color-primary)] hover:bg-[var(--color-primary)]/20'
                }`}
              />

              <div className="leading-tight">
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-semibold text-[var(--color-text-primary)]">
                    RTMS Copilot
                  </span>
                  <span className="flex h-2 w-2 rounded-full bg-[var(--color-primary)] animate-pulse" title="Ready" />
                  {isExpanded && (
                    <span className="rounded bg-[var(--color-primary)]/10 px-1.5 py-0.5 text-[9px] font-medium text-[var(--color-primary)]">
                      Expanded
                    </span>
                  )}
                </div>
                <span className="text-[10px] text-[var(--color-text-secondary)]">
                  {getRoleDisplayName(user?.role)}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-1">
              {/* New chat button */}
              <IconButton
                icon="plus"
                size="sm"
                label="New chat"
                onClick={createNewChat}
                title="New chat"
                className="flex size-7 cursor-pointer items-center justify-center rounded-lg px-0 text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-surface)] hover:text-[var(--color-text-primary)] [&>svg]:size-4"
              />

              {/* Maximize / minimize button */}
              <IconButton
                icon={isExpanded ? 'minimize' : 'maximize'}
                size="sm"
                label={isExpanded ? 'Minimize window' : 'Maximize window'}
                onClick={() => setIsExpanded(!isExpanded)}
                title={isExpanded ? 'Minimize window' : 'Maximize window'}
                className="flex size-7 cursor-pointer items-center justify-center rounded-lg px-0 text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-surface)] hover:text-[var(--color-text-primary)] [&>svg]:size-3.5"
              />

              {/* Close widget button */}
              <IconButton
                icon="x"
                size="sm"
                label="Close window"
                onClick={() => setIsOpen(false)}
                title="Close window"
                className="flex size-7 cursor-pointer items-center justify-center rounded-lg px-0 text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-surface)] hover:text-[var(--color-text-primary)] [&>svg]:size-4"
              />
            </div>
          </div>

          {/* Body: History sidebar + main chat area */}
          <div className="relative flex flex-1 overflow-hidden">
            {(isExpanded || showHistory) && (
              <AiChatSidebar
                isExpanded={isExpanded}
                showHistory={showHistory}
                setShowHistory={setShowHistory}
                conversations={conversations}
                currentConversationId={currentConversationId}
                isLoadingConversations={isLoadingConversations}
                selectConversation={selectConversation}
                createNewChat={createNewChat}
                deleteChat={deleteChat}
                renameChat={renameChat}
              />
            )}

            {/* Main chat area */}
            <div className="relative flex flex-1 flex-col overflow-hidden">
              <AiMessageList
                messages={messages}
                isLoading={isLoading}
                starterPrompts={starterPrompts}
                nextPrompts={nextPrompts}
                lastAssistantIdx={lastAssistantIdx}
                isOpen={isOpen}
                isExpanded={isExpanded}
                onSendMessage={sendMessage}
                onRegenerateMessage={regenerateMessage}
              />
              <AiChatInputBar
                input={input}
                setInput={setInput}
                isLoading={isLoading}
                isOpen={isOpen}
                onSend={handleSend}
                onStop={stopGenerating}
              />
            </div>
          </div>
        </div>
      )}
    </>
  );
}
