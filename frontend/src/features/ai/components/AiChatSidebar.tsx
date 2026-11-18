'use client';

import { useState } from 'react';
import { Plus } from 'lucide-react';
import { Button, FormField, IconButton, Input } from '@/components/ui';
import type { AiConversation } from '../types';

interface AiChatSidebarProps {
  isExpanded: boolean;
  showHistory: boolean;
  setShowHistory: (show: boolean) => void;
  conversations: AiConversation[];
  currentConversationId: number | null;
  isLoadingConversations: boolean;
  selectConversation: (id: number) => Promise<void>;
  createNewChat: () => void;
  deleteChat: (id: number) => Promise<void>;
  renameChat: (id: number, title: string) => Promise<void>;
}

export function AiChatSidebar({
  isExpanded,
  showHistory,
  setShowHistory,
  conversations,
  currentConversationId,
  isLoadingConversations,
  selectConversation,
  createNewChat,
  deleteChat,
  renameChat,
}: AiChatSidebarProps) {
  const [editingChatId, setEditingChatId] = useState<number | null>(null);
  const [editingTitle, setEditingTitle] = useState('');

  if (!isExpanded && !showHistory) return null;

  const handleStartRename = (id: number, currentTitle: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setEditingChatId(id);
    setEditingTitle(currentTitle);
  };

  const handleSaveRename = (id: number, e: React.FormEvent | React.FocusEvent) => {
    e.preventDefault();
    if (editingTitle.trim()) {
      void renameChat(id, editingTitle.trim());
    }
    setEditingChatId(null);
  };

  return (
    <div
      className={`flex flex-col border-r border-[var(--color-border)] bg-[var(--color-surface-muted)]/90 backdrop-blur-xs transition-all duration-300 ease-in-out ${
        isExpanded
          ? showHistory
            ? 'w-64 shrink-0 translate-x-0 opacity-100'
            : 'w-0 -translate-x-full overflow-hidden border-r-0 opacity-0 pointer-events-none'
          : 'absolute inset-0 z-20 bg-[var(--color-surface)] animate-in fade-in'
      }`}
    >
      <div className="flex items-center justify-between border-b border-[var(--color-border)] p-2.5">
        <span className="text-[11px] font-semibold text-[var(--color-text-primary)]">
          Conversation History ({conversations.length})
        </span>
        <div className="flex items-center gap-1">
          <Button
            type="button"
            variant="primary"
            size="sm"
            onClick={() => {
              createNewChat();
              if (!isExpanded) setShowHistory(false);
            }}
            className="flex h-auto cursor-pointer items-center gap-1 rounded-lg bg-[var(--color-primary)] px-2 py-1 text-[10px] font-medium text-white shadow-xs hover:bg-[var(--color-primary-hover)] transition-colors"
          >
            <Plus className="size-3" />
            <span>New Chat</span>
          </Button>
          {!isExpanded && (
            <IconButton
              icon="x"
              size="sm"
              label="Close conversation history"
              onClick={() => setShowHistory(false)}
              className="h-auto w-auto rounded-lg p-1 text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] [&>svg]:size-3.5"
            />
          )}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-1.5 space-y-1">
        {isLoadingConversations && conversations.length === 0 ? (
          <div className="p-4 text-center text-xs text-[var(--color-text-secondary)]">
            Loading history...
          </div>
        ) : conversations.length === 0 ? (
          <div className="p-4 text-center text-xs text-[var(--color-text-secondary)]">
            No saved conversations yet.
          </div>
        ) : (
          conversations.map((c) => {
            const isActive = c.id === currentConversationId;
            const isEditing = editingChatId === c.id;

            return (
              <div
                key={c.id}
                onClick={() => {
                  if (!isEditing) {
                    void selectConversation(c.id);
                    if (!isExpanded) setShowHistory(false);
                  }
                }}
                className={`group flex items-center justify-between rounded-xl px-2.5 py-2 text-xs transition-colors cursor-pointer border ${
                  isActive
                    ? 'bg-[var(--color-primary)]/10 text-[var(--color-primary)] font-semibold border-[var(--color-primary)]/20'
                    : 'text-[var(--color-text-primary)] border-transparent hover:bg-[var(--color-surface-muted)]'
                }`}
              >
                <div className="min-w-0 flex-1 pr-1.5">
                  {isEditing ? (
                    <form onSubmit={(e) => handleSaveRename(c.id, e)} className="pr-1">
                      <FormField label="Conversation title" className="w-full space-y-0 [&>label]:sr-only">
                        <Input
                          type="text"
                          value={editingTitle}
                          onChange={(e) => setEditingTitle(e.target.value)}
                          onBlur={(e) => handleSaveRename(c.id, e)}
                          autoFocus
                          className="w-full rounded bg-[var(--color-surface)] px-1.5 py-0.5 text-xs text-[var(--color-text-primary)] border border-[var(--color-border-strong)] outline-none"
                        />
                      </FormField>
                    </form>
                  ) : (
                    <>
                      <div className="truncate text-[11px] leading-snug">{c.title}</div>
                      <div className="text-[9px] text-[var(--color-text-secondary)] font-normal">
                        {new Date(c.updatedAt).toLocaleDateString([], {
                          month: 'numeric',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </div>
                    </>
                  )}
                </div>

                <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                  {!isEditing && (
                    <IconButton
                      icon="pencil"
                      size="sm"
                      label="Rename"
                      onClick={(e) => handleStartRename(c.id, c.title, e)}
                      className="h-auto w-auto rounded p-1 text-[var(--color-text-secondary)] hover:bg-[var(--color-surface)] hover:text-[var(--color-text-primary)] transition-colors [&>svg]:size-3"
                    />
                  )}
                  <IconButton
                    icon="trash"
                    size="sm"
                    label="Delete conversation"
                    onClick={(e) => {
                      e.stopPropagation();
                      void deleteChat(c.id);
                    }}
                    className="h-auto w-auto rounded p-1 text-[var(--color-text-secondary)] hover:bg-red-500/10 hover:text-red-600 transition-colors [&>svg]:size-3"
                  />
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
