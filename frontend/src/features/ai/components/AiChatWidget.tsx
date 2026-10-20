'use client';

import { useState, useRef, useEffect, useMemo } from 'react';
import { Icon } from '@/components/ui/Icon';
import { useAuth } from '@/context/AuthContext';
import { useAiChat } from '../context/AiChatContext';
import { MarkdownMessage } from './MarkdownMessage';

export function AiChatWidget() {
  const { user } = useAuth();
  const {
    isOpen,
    toggleOpen,
    setIsOpen,
    messages,
    isLoading,
    sendMessage,
    clearConversation,
  } = useAiChat();

  const [input, setInput] = useState('');
  const [isExpanded, setIsExpanded] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  // Tự động cuộn xuống dưới cùng khi có tin nhắn mới hoặc đang loading
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isLoading, isOpen, isExpanded]);

  // Focus vào input khi mở widget
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => inputRef.current?.focus(), 150);
    }
  }, [isOpen]);

  const handleSend = async () => {
    if (!input.trim() || isLoading) return;
    const text = input;
    setInput('');
    await sendMessage(text);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // Quick prompts theo role của user
  const quickPrompts = useMemo(() => {
    const role = user?.role?.toUpperCase();
    if (role === 'HEAD_TRAINER' || role === 'TRAINER') {
      return [
        'Lịch tập các ca hôm nay',
        'Tiến độ các khóa đào tạo',
        'Cảnh báo nguy cơ chấn thương & nhịp tim',
        'Phân tích sở trường của ngựa',
      ];
    }
    if (role === 'VETERINARIAN' || role === 'VET') {
      return [
        'Tra cứu ca viêm gân tương tự qua RAG',
        'Lịch chăm sóc y tế định kỳ',
        'Báo cáo sự cố chuồng trại 48h qua',
        'Danh sách ngựa đang cách ly',
      ];
    }
    if (role === 'GROOM' || role === 'STABLE_STAFF') {
      return [
        'Checklist công việc hôm nay (3 nguồn)',
        'Chuồng và ngựa tôi phụ trách',
        'Cảnh báo ăn uống & vận động của ngựa',
        'Kiểm tra vật tư chuồng',
      ];
    }
    if (role === 'CLUB_MANAGER' || role === 'RACING_MANAGER') {
      return [
        'Báo cáo công suất chuồng trại',
        'Tình hình sức khỏe toàn đàn',
        'Phân bổ khu vực và huấn luyện viên',
        'Đơn đăng ký nhập đàn chờ duyệt',
      ];
    }
    if (role === 'HORSE_OWNER') {
      return [
        'Danh sách chiến mã của tôi',
        'Lịch tập sắp tới của ngựa',
        'Tóm tắt sức khỏe và lần khám gần nhất',
        'Tiến độ khóa huấn luyện của ngựa',
      ];
    }
    // Default fallback (Read-Only)
    return [
      'Danh sách chiến mã của tôi',
      'Lịch tập sắp tới của ngựa',
      'Tóm tắt sức khỏe và lần khám gần nhất',
      'Tiến độ khóa huấn luyện của ngựa',
    ];
  }, [user?.role]);

  return (
    <>
      {/* Nút Tròn Nổi (Floating Action Button) ở góc dưới phải */}
      <div className="fixed bottom-6 right-6 z-50 flex items-center">
        <button
          type="button"
          onClick={toggleOpen}
          aria-label="Mở Trợ lý AI RTMS Copilot"
          className="group relative flex h-14 w-14 items-center justify-center rounded-full bg-emerald-600 text-white shadow-xl shadow-emerald-600/30 transition-all duration-300 hover:scale-105 hover:bg-emerald-700 active:scale-95 dark:bg-emerald-500 dark:hover:bg-emerald-600"
        >
          {isOpen ? (
            <Icon name="x" size={24} className="transition-transform group-hover:rotate-90" />
          ) : (
            <Icon name="sparkles" size={24} className="animate-pulse" />
          )}
        </button>
      </div>

      {/* Cửa sổ chat nổi: Chế độ thường (380x560px) hoặc Phóng to (800x720px) */}
      {isOpen && (
        <div
          role="dialog"
          aria-label="Cửa sổ Trợ lý AI"
          className={`fixed z-50 flex flex-col overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] shadow-2xl shadow-neutral-900/15 backdrop-blur-md transition-all duration-300 ease-in-out animate-in fade-in slide-in-from-bottom-4 ${
            isExpanded
              ? 'bottom-6 right-6 h-[720px] w-[800px] max-w-[calc(100vw-32px)] max-h-[calc(100vh-48px)]'
              : 'bottom-22 right-6 h-[560px] w-[380px] max-w-[calc(100vw-32px)] max-h-[calc(100vh-110px)]'
          }`}
        >
          {/* Header */}
          <div className="flex h-14 shrink-0 items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface-muted)] px-3.5">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-emerald-600/10 text-emerald-600 dark:bg-emerald-400/15 dark:text-emerald-400">
                <Icon name="bot" size={18} />
              </div>
              <div className="leading-tight">
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-semibold text-[var(--color-text-primary)]">
                    RTMS Copilot
                  </span>
                  <span className="flex h-2 w-2 rounded-full bg-emerald-500" title="Online" />
                  {isExpanded && (
                    <span className="rounded bg-emerald-600/10 px-1.5 py-0.5 text-[9px] font-medium text-emerald-600 dark:bg-emerald-400/10 dark:text-emerald-400">
                      Mở rộng
                    </span>
                  )}
                </div>
                <span className="text-[10px] text-[var(--color-text-secondary)]">
                  {user?.role ? `Quyền: ${user.role}` : 'AI Assistant'}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-1">
              {/* Nút Phóng to / Thu nhỏ */}
              <button
                type="button"
                onClick={() => setIsExpanded(!isExpanded)}
                title={isExpanded ? 'Thu nhỏ cửa sổ' : 'Phóng to cửa sổ'}
                className="flex h-7 w-7 items-center justify-center rounded-lg text-[var(--color-text-secondary)] transition-colors hover:bg-neutral-200/60 hover:text-[var(--color-text-primary)] dark:hover:bg-neutral-800"
              >
                <Icon name={isExpanded ? 'minimize' : 'maximize'} size={14} />
              </button>

              {/* Nút Làm mới hội thoại */}
              <button
                type="button"
                onClick={clearConversation}
                title="Làm mới đoạn hội thoại"
                className="flex h-7 w-7 items-center justify-center rounded-lg text-[var(--color-text-secondary)] transition-colors hover:bg-neutral-200/60 hover:text-[var(--color-text-primary)] dark:hover:bg-neutral-800"
              >
                <Icon name="refresh" size={14} />
              </button>

              {/* Nút Ẩn cửa sổ */}
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                title="Thu nhỏ cửa sổ"
                className="flex h-7 w-7 items-center justify-center rounded-lg text-[var(--color-text-secondary)] transition-colors hover:bg-neutral-200/60 hover:text-[var(--color-text-primary)] dark:hover:bg-neutral-800"
              >
                <Icon name="minus" size={14} />
              </button>
            </div>
          </div>

          {/* Danh sách tin nhắn */}
          <div className="flex-1 overflow-y-auto px-3.5 py-3 space-y-3">
            {messages.map((msg) => {
              const isUser = msg.role === 'user';
              return (
                <div
                  key={msg.id}
                  className={`flex flex-col ${isUser ? 'items-end' : 'items-start'}`}
                >
                  <div
                    className={`rounded-2xl px-3.5 py-2 text-xs leading-relaxed shadow-xs ${
                      isUser
                        ? 'max-w-[85%] rounded-tr-none bg-emerald-600 text-white dark:bg-emerald-700'
                        : 'max-w-[95%] rounded-tl-none border border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-primary)]'
                    }`}
                  >
                    {isUser ? (
                      <p className="whitespace-pre-wrap">{msg.content}</p>
                    ) : (
                      <MarkdownMessage content={msg.content} />
                    )}
                  </div>

                  <span className="mt-1 px-1 text-[9px] text-[var(--color-text-secondary)]">
                    {new Date(msg.timestamp).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
              );
            })}

            {/* Trạng thái AI đang xử lý */}
            {isLoading && (
              <div className="flex items-center gap-2 rounded-2xl rounded-tl-none border border-[var(--color-border)] bg-[var(--color-surface-muted)] px-3.5 py-2.5 text-xs text-[var(--color-text-secondary)]">
                <Icon name="sparkles" size={13} className="animate-spin text-emerald-600 dark:text-emerald-400" />
                <span>Copilot đang suy nghĩ và tra cứu...</span>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Quick Prompts */}
          <div className="shrink-0 border-t border-[var(--color-border)] bg-[var(--color-surface-muted)]/50 px-3 py-1.5">
            <div className="flex gap-1.5 overflow-x-auto pb-1 scrollbar-none">
              {quickPrompts.map((prompt, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => sendMessage(prompt)}
                  disabled={isLoading}
                  className="shrink-0 rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-0.5 text-[10px] text-[var(--color-text-secondary)] transition-colors hover:border-emerald-500 hover:text-emerald-600 active:scale-95 disabled:opacity-50 dark:hover:text-emerald-400"
                >
                  {prompt}
                </button>
              ))}
            </div>
          </div>

          {/* Input & Nút Gửi */}
          <div className="shrink-0 border-t border-[var(--color-border)] bg-[var(--color-surface)] p-2.5">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSend();
              }}
              className="flex items-end gap-1.5"
            >
              <textarea
                ref={inputRef}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Hỏi AI hoặc yêu cầu tra cứu..."
                rows={1}
                disabled={isLoading}
                className="max-h-24 min-h-[38px] flex-1 resize-none rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-muted)] px-3 py-2 text-xs text-[var(--color-text-primary)] placeholder-[var(--color-text-secondary)] outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
              />
              <button
                type="submit"
                disabled={!input.trim() || isLoading}
                aria-label="Gửi tin nhắn"
                className="flex h-[38px] w-[38px] shrink-0 items-center justify-center rounded-xl bg-emerald-600 text-white transition-colors hover:bg-emerald-700 active:scale-95 disabled:opacity-40 dark:bg-emerald-700 dark:hover:bg-emerald-600"
              >
                <Icon name="send" size={15} />
              </button>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
