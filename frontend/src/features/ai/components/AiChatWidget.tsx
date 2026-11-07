/* eslint-disable no-restricted-syntax */
'use client';

import { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import {
  Sparkles,
  ArrowUp,
  Square,
  ArrowDown,
  Plus,
  Minimize2,
  Maximize2,
  PanelLeft,
  X,
  RotateCw,
  Copy,
  Check,
  ChevronRight,
  AlertCircle,
  Trash2,
  Loader2,
  Pencil,
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { useAiChat } from '../context/AiChatContext';
import { getQuickPromptsByRole, getRoleDisplayName } from '../lib/aiUtils';
import { MarkdownMessage } from './MarkdownMessage';
import { formatClockLabel, formatDateTime } from '@/lib/display';

const TOOL_LABELS: Record<string, string> = {
  getHorsesList: 'Retrieve horses list',
  getHorseDetails: 'View horse details',
  getTrainingPlans: 'Check training plans',
  getMedicalRecords: 'Inspect medical records',
  getUrgentAlerts: 'Scan urgent medical alerts',
  getStallStatus: 'Check stable occupancy',
  getStaffSchedule: 'Look up staff schedule',
  queryClubMetrics: 'Synthesize club metrics',
  generateChartData: 'Visualize data chart',
  getTrainerSchedule: 'Look up trainer schedule',
  getTrainerPlansSummary: 'Check trainer plans summary',
  getTrainerHorses: 'Retrieve trainer horses',
  getTrainerGroups: 'List training groups',
  getWorkoutHistory: 'Check workout history',
  getMyAssignedHorses: 'List assigned horses & stalls',
  getDailyGroomTasks: 'Retrieve daily groom tasks',
  getTodayAggregatedTasks: 'Check aggregated tasks',
  getMyReportedIncidents: 'List reported incidents',
  getHorseStallLocation: 'Locate horse stall',
  getVetExaminationOverview: 'Inspect veterinary exam overview',
  getQuarantineStatus: 'Check quarantine status',
  getHorseVitalSignsTrend: 'Analyze vital signs trend',
  getActivePrescriptions: 'Check active prescriptions',
  getPendingAdmissionExams: 'Check pending admission exams',
  getUrgentCareAlerts: 'Scan urgent care alerts',
  evaluateTrainingResumptionReadiness: 'Evaluate training resumption readiness',
  getClubHealthOverview: 'Review club health overview',
  getStableOccupancyMetrics: 'Check stable occupancy metrics',
  getSystemStaffOverview: 'Inspect staff overview',
  getAdmissionRequestsOverview: 'Review admission requests',
  getRecentAuditLogs: 'Review recent audit logs',
  getMyHorses: 'Retrieve owned horses',
  getMyHorseAdmissions: 'Check admission applications',
  getMyHorseUpcomingSchedule: 'Look up upcoming schedule',
  getMyHorsePedigreeInfo: 'Inspect horse pedigree',
  getHorseSpecialtyAnalysis: 'Analyze horse specialties',
  recommendOptimalRaceDistanceAndTrack: 'Recommend optimal distance & track',
  recommendNextWorkoutAdjustment: 'Recommend next workout adjustment',
  recommendTrainerStrategy: 'Recommend training strategy',
  analyzeStableCapacityAndStallBottlenecks: 'Analyze stable bottlenecks',
  analyzeIncidentPatternsAndRecurringRisks: 'Analyze recurring risks',
};

function formatToolName(name: string): string {
  if (TOOL_LABELS[name]) return TOOL_LABELS[name];
  // Chuyển camelCase sang chuỗi chữ thường dễ đọc
  return name.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase());
}

function formatToolArgs(args: unknown): string | null {
  if (!args || typeof args !== 'object') return null;
  const entries = Object.entries(args as Record<string, unknown>);
  if (entries.length === 0) return null;
  return entries.map(([k, v]) => `${k}: ${String(v)}`).join(', ');
}

/**
 * Khối Accordion hiển thị các bước gọi Tool của AI
 */
function ToolExecutionSteps({
  tools,
  isRunning,
}: {
  tools?: Record<string, unknown>[];
  isRunning?: boolean;
}) {
  const [expanded, setExpanded] = useState(false);

  if (!tools || tools.length === 0) {
    if (!isRunning) return null;
    return (
      <div className="flex h-7 items-center gap-2 rounded-md text-xs text-[var(--color-text-secondary)]">
        <Loader2 className="size-3.5 animate-spin text-[var(--color-primary)]" />
        <span>Processing data...</span>
      </div>
    );
  }

  const errorCount = tools.filter(
    (t) => t.status === 'ERROR' || Boolean(t.error)
  ).length;

  const summary = errorCount > 0
    ? `Used ${tools.length} tools, ${errorCount} errors`
    : `Used ${tools.length} tools`;

  return (
    <div className="my-1 text-xs">
      <button
        type="button"
        onClick={() => setExpanded(!expanded)}
        aria-expanded={expanded}
        className="flex h-7 cursor-pointer items-center gap-1.5 rounded-md text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] outline-hidden transition-colors"
      >
        <ChevronRight
          className={`size-3.5 transition-transform duration-200 ${
            expanded ? 'rotate-90' : ''
          }`}
        />
        <span>{summary}</span>
      </button>

      {/* Grid-based animated accordion */}
      <div
        className={`grid transition-[grid-template-rows] duration-200 ease-out ${
          expanded ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'
        }`}
      >
        <div className="overflow-hidden" inert={!expanded}>
          <ul className="ml-[7px] my-1 space-y-2 border-l border-[var(--color-border-strong)] pl-4 py-1">
            {tools.map((tool, idx) => {
              const toolName = String(tool.tool || tool.name || `Tool ${idx + 1}`);
              const isError = tool.status === 'ERROR' || Boolean(tool.error);
              const errorMessage = typeof tool.error === 'string' ? tool.error : null;
              const argsSummary = formatToolArgs(tool.arguments || tool.input);
              const resultCount =
                !isError && Array.isArray(tool.result)
                  ? `${tool.result.length} records`
                  : null;
              const detailText = isError && errorMessage
                ? `Error: ${errorMessage}`
                : resultCount
                ? `Result: ${resultCount}`
                : argsSummary;

              return (
                <li key={idx} className="flex items-start gap-2">
                  {isError ? (
                    <AlertCircle className="size-3.5 text-red-600 shrink-0 mt-0.5" />
                  ) : (
                    <Check className="size-3.5 text-[var(--color-text-secondary)] shrink-0 mt-0.5" />
                  )}
                  <div className="min-w-0">
                    <p className="font-medium text-[var(--color-text-primary)]">
                      {formatToolName(toolName)}
                    </p>
                    {detailText && (
                      <p className="text-[11px] text-[var(--color-text-secondary)] truncate max-w-[280px] sm:max-w-[400px]">
                        {detailText}
                      </p>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        </div>
      </div>
    </div>
  );
}

/**
 * Hàng nút thao tác dưới mỗi câu trả lời của AI (Copy, Regenerate, Tag dừng)
 */
function MessageActionBar({
  content,
  onRegenerate,
  isStopped,
  isLast,
  isLoading,
}: {
  content: string;
  onRegenerate: () => void;
  isStopped?: boolean;
  isLast: boolean;
  isLoading: boolean;
}) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback
    }
  };

  return (
    <div
      className={`-ml-2 flex items-center gap-1 pt-1 text-xs text-[var(--color-text-secondary)] transition-opacity ${
        isLast
          ? 'opacity-100'
          : 'opacity-0 max-md:opacity-100 group-hover:opacity-100 group-focus-within:opacity-100'
      }`}
    >
      {/* -ml-2 bù px-2 của nút: icon đầu thẳng cột chữ câu trả lời (chuẩn chat.md N11) */}
      {content.trim() && (
        <button
          type="button"
          onClick={handleCopy}
          aria-label="Copy content"
          title="Copy"
          className="flex size-8 cursor-pointer items-center justify-center rounded-lg hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] transition-colors"
        >
          {copied ? (
            <Check className="size-3.5 text-[var(--color-primary)]" />
          ) : (
            <Copy className="size-3.5" />
          )}
        </button>
      )}

      <button
        type="button"
        onClick={onRegenerate}
        disabled={isLoading}
        aria-label="Regenerate response"
        title="Regenerate"
        className="flex size-8 cursor-pointer items-center justify-center rounded-lg hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] transition-colors disabled:opacity-40"
      >
        <RotateCw className="size-3.5" />
      </button>

      {isStopped && (
        <span className="ml-1 text-[11px] text-[var(--color-text-secondary)] font-medium">
          Stopped
        </span>
      )}
    </div>
  );
}

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
  const [showScrollBottom, setShowScrollBottom] = useState(false);
  const [editingChatId, setEditingChatId] = useState<number | null>(null);
  const [editingTitle, setEditingTitle] = useState('');

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Cuộn xuống dưới cùng khi có tin mới hoặc đang load
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

  // Kiểm tra vị trí cuộn để hiện nút Scroll to bottom
  const handleScroll = () => {
    const el = scrollContainerRef.current;
    if (!el) return;
    const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    setShowScrollBottom(distanceToBottom > 80);
  };

  // Tự động focus ô nhập khi mở khung chat
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => textareaRef.current?.focus(), 150);
    }
  }, [isOpen]);

  // Tự động điều chỉnh chiều cao textarea theo nội dung
  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 160)}px`;
  }, [input]);

  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
  };

  const handleSend = async () => {
    if (!input.trim() || isLoading) return;
    const text = input;
    setInput('');
    await sendMessage(text);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void handleSend();
    }
  };

  // Quick prompts ban đầu theo Role
  const rolePrompts = useMemo(() => getQuickPromptsByRole(user?.role), [user?.role]);
  const starterPrompts = useMemo(() => rolePrompts.slice(0, 3), [rolePrompts]);

  // Gợi ý hỏi tiếp dưới câu trả lời cuối cùng: lọc bỏ các câu hỏi user đã gửi
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
    <>
      {/* Floating Action Button (FAB) góc dưới phải */}
      <div
        className={`fixed bottom-6 right-6 z-50 flex items-center ${
          isOpen && isExpanded ? 'hidden pointer-events-none' : ''
        }`}
      >
        <button
          type="button"
          onClick={toggleOpen}
          aria-label={isOpen ? 'Close RTMS Copilot' : 'Open RTMS Copilot'}
          className="group relative flex h-14 w-14 cursor-pointer items-center justify-center rounded-full bg-[var(--color-primary)] text-white shadow-xl shadow-[var(--color-primary)]/25 transition-all duration-300 hover:scale-105 hover:bg-[var(--color-primary-hover)] active:scale-95 focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] outline-none"
        >
          {isOpen ? (
            <X className="size-6 transition-transform group-hover:rotate-90" />
          ) : (
            <Sparkles className="size-6 animate-pulse" />
          )}
        </button>
      </div>

      {/* Cửa sổ Chat nổi */}
      {isOpen && (
        <div
          role="dialog"
          aria-label="RTMS Copilot Dialog"
          className={`fixed z-50 flex flex-col overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] shadow-2xl shadow-neutral-900/15 backdrop-blur-md transition-all duration-300 ease-in-out animate-in fade-in slide-in-from-bottom-4 ${
            isExpanded
              ? 'bottom-6 right-6 h-[720px] w-[800px] max-w-[calc(100vw-32px)] max-h-[calc(100vh-48px)]'
              : 'bottom-22 right-6 h-[560px] w-[380px] max-w-[calc(100vw-32px)] max-h-[calc(100vh-110px)]'
          }`}
        >
          {/* Header */}
          <div className="flex h-14 shrink-0 items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface-muted)] px-3.5">
            <div className="flex items-center gap-2.5">
              {/* Nút bật/tắt Lịch sử hội thoại */}
              <button
                type="button"
                onClick={() => setShowHistory(!showHistory)}
                title={showHistory ? 'Close conversation history' : 'Conversation history'}
                className={`flex h-8 w-8 cursor-pointer items-center justify-center rounded-xl transition-colors ${
                  showHistory
                    ? 'bg-[var(--color-primary)] text-white'
                    : 'bg-[var(--color-primary)]/10 text-[var(--color-primary)] hover:bg-[var(--color-primary)]/20'
                }`}
              >
                <PanelLeft className="size-4" />
              </button>

              <div className="leading-tight">
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-semibold text-[var(--color-text-primary)]">
                    RTMS Copilot
                  </span>
                  <span className="flex h-2 w-2 rounded-full bg-[var(--color-primary)] animate-pulse" title="Ready" />
                  {isExpanded && (
                    <span className="rounded bg-[var(--color-primary)]/10 px-1.5 py-0.5 text-[9px] font-medium text-[var(--color-primary)]">
                      Mở rộng
                    </span>
                  )}
                </div>
                <span className="text-[10px] text-[var(--color-text-secondary)]">
                  {getRoleDisplayName(user?.role)}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-1">
              {/* Nút Đoạn chat mới (+ New Chat) */}
              <button
                type="button"
                onClick={createNewChat}
                title="New chat"
                className="flex size-7 cursor-pointer items-center justify-center rounded-lg text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-surface)] hover:text-[var(--color-text-primary)]"
              >
                <Plus className="size-4" />
              </button>

              {/* Nút Phóng to / Thu nhỏ */}
              <button
                type="button"
                onClick={() => setIsExpanded(!isExpanded)}
                title={isExpanded ? 'Minimize window' : 'Maximize window'}
                className="flex size-7 cursor-pointer items-center justify-center rounded-lg text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-surface)] hover:text-[var(--color-text-primary)]"
              >
                {isExpanded ? <Minimize2 className="size-3.5" /> : <Maximize2 className="size-3.5" />}
              </button>

              {/* Nút Đóng widget */}
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                title="Close window"
                className="flex size-7 cursor-pointer items-center justify-center rounded-lg text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-surface)] hover:text-[var(--color-text-primary)]"
              >
                <X className="size-4" />
              </button>
            </div>
          </div>

          {/* Body: Sidebar Lịch sử + Vùng Chat chính */}
          <div className="relative flex flex-1 overflow-hidden">
            {/* Sidebar Lịch sử hội thoại */}
            {(isExpanded || showHistory) && (
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
                    <button
                      type="button"
                      onClick={() => {
                        createNewChat();
                        if (!isExpanded) setShowHistory(false);
                      }}
                      className="flex cursor-pointer items-center gap-1 rounded-lg bg-[var(--color-primary)] px-2 py-1 text-[10px] font-medium text-white shadow-xs hover:bg-[var(--color-primary-hover)] transition-colors"
                    >
                      <Plus className="size-3" />
                      <span>New Chat</span>
                    </button>
                    {!isExpanded && (
                      <button
                        type="button"
                        onClick={() => setShowHistory(false)}
                        className="rounded-lg p-1 text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]"
                      >
                        <X className="size-3.5" />
                      </button>
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
                                <input
                                  type="text"
                                  value={editingTitle}
                                  onChange={(e) => setEditingTitle(e.target.value)}
                                  onBlur={(e) => handleSaveRename(c.id, e)}
                                  autoFocus
                                  className="w-full rounded bg-[var(--color-surface)] px-1.5 py-0.5 text-xs text-[var(--color-text-primary)] border border-[var(--color-border-strong)] outline-none"
                                />
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
                              <button
                                type="button"
                                title="Rename"
                                onClick={(e) => handleStartRename(c.id, c.title, e)}
                                className="rounded p-1 text-[var(--color-text-secondary)] hover:bg-[var(--color-surface)] hover:text-[var(--color-text-primary)] transition-colors"
                              >
                                <Pencil className="size-3" />
                              </button>
                            )}
                            <button
                              type="button"
                              title="Delete conversation"
                              onClick={(e) => {
                                e.stopPropagation();
                                void deleteChat(c.id);
                              }}
                              className="rounded p-1 text-[var(--color-text-secondary)] hover:bg-red-500/10 hover:text-red-600 transition-colors"
                            >
                              <Trash2 className="size-3" />
                            </button>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            )}

            {/* Khu vực Chat Chính */}
            <div className="relative flex flex-1 flex-col overflow-hidden">
              {/* Danh sách tin nhắn cuộn */}
              <div
                ref={scrollContainerRef}
                onScroll={handleScroll}
                className="flex-1 overflow-y-auto px-4 py-4 space-y-6"
              >
                {/* Trạng thái rỗng: Cuộc trò chuyện mới ("Suggested Prompts") */}
                {messages.length === 0 && (
                  <div className="flex flex-col justify-end min-h-[300px] space-y-3 pb-2">
                    <p className="text-xs font-medium text-[var(--color-text-secondary)]">
                      Suggested Prompts
                    </p>
                    <div className="flex flex-col gap-2">
                      {starterPrompts.map((prompt, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => void sendMessage(prompt)}
                          disabled={isLoading}
                          className="block w-fit max-w-full cursor-pointer rounded-xl border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3.5 py-2 text-left text-xs text-[var(--color-text-primary)] outline-hidden hover:bg-[var(--color-surface-muted)] transition-colors shadow-2xs"
                        >
                          {prompt}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* Danh sách các tin nhắn */}
                {messages.map((msg, index) => {
                  const isUser = msg.role === 'user';
                  const isLastAssistant = index === lastAssistantIdx;

                  if (isUser) {
                    return (
                      <div key={msg.id} className="flex flex-col items-end">
                        {/* Bong bóng tin người dùng: bo tròn 3xl bên phải, không màu nhấn */}
                        <p className="ml-auto w-fit max-w-[80%] rounded-3xl bg-[var(--color-surface)] border border-[var(--color-border-strong)] px-4 py-2.5 text-xs sm:text-sm text-[var(--color-text-primary)] whitespace-pre-wrap [overflow-wrap:anywhere] shadow-xs">
                          {msg.content}
                        </p>
                        <span className="mt-1 pr-2 text-[10px] text-[var(--color-text-secondary)]">
                          {new Date(msg.timestamp).toLocaleTimeString([], {
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </span>
                      </div>
                    );
                  }

                  // Tin nhắn Assistant: không bong bóng, không avatar, thẳng mép trái
                  return (
                    <div key={msg.id} className="group flex flex-col items-start w-full">
                      <div className="max-w-[65ch] w-full space-y-2 text-left text-xs sm:text-sm leading-relaxed text-[var(--color-text-primary)] text-pretty">
                        {/* Tool Execution Steps Accordion */}
                        {msg.executedTools && msg.executedTools.length > 0 && (
                          <ToolExecutionSteps tools={msg.executedTools} />
                        )}

                        {/* Nội dung tin nhắn hoặc thông báo lỗi */}
                        {msg.status === 'error' ? (
                          <div className="space-y-1.5 rounded-xl border border-red-500/20 bg-red-500/5 p-3 text-left">
                            <p className="text-xs sm:text-sm font-medium text-red-600">
                              Assistant could not respond
                            </p>
                            <p className="text-[11px] text-[var(--color-text-secondary)]">
                              {msg.errorMessage || 'Server is busy or encountered a connection issue.'}
                            </p>
                            <button
                              type="button"
                              onClick={() => void regenerateMessage(msg.id)}
                              className="mt-1 inline-flex cursor-pointer items-center gap-1.5 rounded-xl border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2.5 py-1 text-xs text-[var(--color-text-primary)] hover:bg-[var(--color-surface-muted)] transition-colors"
                            >
                              <RotateCw className="size-3" />
                              <span>Retry</span>
                            </button>
                          </div>
                        ) : (
                          <MarkdownMessage content={msg.content} />
                        )}

                        {/* Hàng nút thao tác (Copy, Regenerate, Stopped tag) */}
                        {msg.status !== 'error' && (
                          <MessageActionBar
                            content={msg.content}
                            onRegenerate={() => void regenerateMessage(msg.id)}
                            isStopped={msg.isStopped}
                            isLast={isLastAssistant}
                            isLoading={isLoading}
                          />
                        )}

                        {/* Gợi ý hỏi tiếp dưới câu trả lời cuối cùng */}
                        {isLastAssistant && !isLoading && nextPrompts.length > 0 && (
                          <div className="space-y-1.5 pt-2">
                            <p className="text-xs text-[var(--color-text-secondary)]">Follow-up suggestions</p>
                            <div className="flex flex-col gap-1.5">
                              {nextPrompts.map((p, idx) => (
                                <button
                                  key={idx}
                                  type="button"
                                  onClick={() => void sendMessage(p)}
                                  className="block w-fit max-w-full cursor-pointer rounded-xl border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-left text-xs text-[var(--color-text-primary)] outline-hidden hover:bg-[var(--color-surface-muted)] transition-colors truncate"
                                >
                                  {p}
                                </button>
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

                {/* Trạng thái đang tải / suy nghĩ */}
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

              {/* Nút Cuộn xuống nhanh (Scroll to bottom) */}
              {showScrollBottom && (
                <button
                  type="button"
                  onClick={() => scrollToBottom(true)}
                  aria-label="Scroll to bottom"
                  title="Scroll to bottom"
                  className="absolute bottom-20 left-1/2 -translate-x-1/2 z-10 flex size-8 cursor-pointer items-center justify-center rounded-full border border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-text-primary)] shadow-md hover:bg-[var(--color-surface-muted)] transition-all"
                >
                  <ArrowDown className="size-4" />
                </button>
              )}

              {/* Ô soạn dính đáy (Sticky Composer) */}
              <div className="shrink-0 border-t border-[var(--color-border)] bg-[var(--color-surface)] p-3">
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    if (isLoading) {
                      stopGenerating();
                    } else {
                      void handleSend();
                    }
                  }}
                  className="mx-auto flex items-end gap-2 rounded-3xl border border-[var(--color-border-strong)] bg-[var(--color-surface)] p-2 focus-within:border-[var(--color-focus)] focus-within:ring-2 focus-within:ring-[var(--color-focus)]/25 shadow-xs transition-shadow"
                >
                  <textarea
                    ref={textareaRef}
                    rows={1}
                    value={input}
                    onChange={handleInputChange}
                    onKeyDown={handleKeyDown}
                    placeholder="Ask about training schedule, horse health, stabling..."
                    className="max-h-36 min-h-[40px] flex-1 resize-none bg-transparent px-3 py-2 text-xs sm:text-sm text-[var(--color-text-primary)] placeholder:text-[var(--color-text-secondary)] outline-hidden leading-relaxed"
                  />

                  {isLoading ? (
                    <button
                      type="button"
                      onClick={stopGenerating}
                      aria-label="Stop generating"
                      title="Stop generating"
                      className="grid size-10 shrink-0 cursor-pointer place-items-center rounded-2xl bg-[var(--color-primary)] text-white hover:bg-[var(--color-primary-hover)] transition-all active:scale-95 shadow-xs"
                    >
                      <Square className="size-3.5 fill-current" />
                    </button>
                  ) : (
                    <button
                      type="submit"
                      disabled={!input.trim()}
                      aria-label="Send message"
                      title="Send message"
                      className="grid size-10 shrink-0 cursor-pointer place-items-center rounded-2xl bg-[var(--color-primary)] text-white hover:bg-[var(--color-primary-hover)] transition-all active:scale-95 disabled:cursor-not-allowed disabled:opacity-30 shadow-xs"
                    >
                      <ArrowUp className="size-4" />
                    </button>
                  )}
                </form>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
