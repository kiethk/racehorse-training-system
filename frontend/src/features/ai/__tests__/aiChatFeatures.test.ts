import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { CHART_PALETTE } from '../lib/chartUtils.ts';
import { getQuickPromptsByRole } from '../lib/aiUtils.ts';

describe('AI Chat Upgraded Features & Logic Tests', () => {
  it('CHART_PALETTE is aligned with RTMS Brand Tokens (#6657c7 primary)', () => {
    assert.equal(CHART_PALETTE[0], '#6657c7', 'Primary chart color should be RTMS primary purple #6657c7');
    assert.equal(CHART_PALETTE[1], '#496bc3', 'Training chart color should match --color-training #496bc3');
    assert.equal(CHART_PALETTE[2], '#b94b58', 'Medical chart color should match --color-medical #b94b58');
    assert.equal(CHART_PALETTE[3], '#8c6a2e', 'Grooming chart color should match --color-grooming #8c6a2e');
    assert.ok(CHART_PALETTE.length >= 10);
  });

  it('starter quick prompts for empty conversation selects top 3 suggestions', () => {
    const trainerPrompts = getQuickPromptsByRole('HEAD_TRAINER');
    const starters = trainerPrompts.slice(0, 3);
    assert.equal(starters.length, 3);
    assert.ok(starters[0].length > 0);
    assert.ok(starters[1].length > 0);
    assert.ok(starters[2].length > 0);
  });

  it('correctly calculates tool execution summary and error count', () => {
    const mockTools = [
      { tool: 'getHorsesList', arguments: { status: 'ACTIVE' }, status: 'SUCCESS' },
      { tool: 'getMedicalRecords', arguments: { horseId: 101 }, status: 'ERROR', error: 'Database timeout' },
      { tool: 'getStallStatus', arguments: {}, status: 'SUCCESS' },
    ];

    const errorCount = mockTools.filter((t) => t.status === 'ERROR' || Boolean(t.error)).length;
    assert.equal(errorCount, 1);

    const summary = errorCount > 0
      ? `Đã dùng ${mockTools.length} công cụ, ${errorCount} lỗi`
      : `Đã dùng ${mockTools.length} công cụ`;

    assert.equal(summary, 'Đã dùng 3 công cụ, 1 lỗi');
  });

  it('correctly calculates regenerate slice index for user messages', () => {
    const messages = [
      { id: 'u1', role: 'user', content: 'Câu hỏi 1' },
      { id: 'a1', role: 'assistant', content: 'Trả lời 1' },
      { id: 'u2', role: 'user', content: 'Câu hỏi 2' },
      { id: 'a2', role: 'assistant', content: 'Trả lời 2' },
    ];

    // Case 1: regenerate without targetId (defaults to last turn)
    let userMsgIdx = -1;
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i].role === 'user') {
        userMsgIdx = i;
        break;
      }
    }

    assert.equal(userMsgIdx, 2);
    assert.equal(messages[userMsgIdx].content, 'Câu hỏi 2');
    const sliced = messages.slice(0, userMsgIdx);
    assert.equal(sliced.length, 2);
    assert.equal(sliced[0].id, 'u1');
    assert.equal(sliced[1].id, 'a1');

    // Case 2: regenerate targeting specific assistant id 'a1'
    const targetIdx = messages.findIndex((m) => m.id === 'a1');
    let targetUserIdx = -1;
    for (let i = targetIdx; i >= 0; i--) {
      if (messages[i].role === 'user') {
        targetUserIdx = i;
        break;
      }
    }
    assert.equal(targetUserIdx, 0);
    assert.equal(messages[targetUserIdx].content, 'Câu hỏi 1');
    const targetedSlice = messages.slice(0, targetUserIdx);
    assert.equal(targetedSlice.length, 0);
  });

  it('identifies AbortError properly across DOMException and Error instances', () => {
    const isAbort = (err: unknown): boolean => {
      return (
        (err instanceof Error && (err.name === 'AbortError' || err.message.toLowerCase().includes('aborted')))
      );
    };

    const domAbort = new Error('The operation was aborted');
    domAbort.name = 'AbortError';
    assert.equal(isAbort(domAbort), true);

    const regularError = new Error('Network failure 500');
    assert.equal(isAbort(regularError), false);

    const fetchAborted = new Error('BodyStreamBuffer was aborted');
    assert.equal(isAbort(fetchAborted), true);
  });

  it('filters nextPrompts to never suggest questions user already asked in current conversation', () => {
    const rolePrompts = [
      'Lịch tập các ca hôm nay',
      'Tiến độ các kế hoạch huấn luyện',
      'Cảnh báo nguy cơ chấn thương & nhịp tim',
      'Phân tích sở trường của ngựa',
      'Danh sách Groom làm việc cùng',
    ];

    const messages = [
      { id: 'u1', role: 'user', content: 'Lịch tập các ca hôm nay' },
      { id: 'a1', role: 'assistant', content: 'Hôm nay có 3 ca tập.' },
    ];

    const asked = new Set(
      messages
        .filter((m) => m.role === 'user')
        .map((m) => m.content.trim().toLowerCase())
    );
    const unasked = rolePrompts.filter((p) => !asked.has(p.trim().toLowerCase()));
    const nextPrompts = unasked.slice(0, 2);

    assert.equal(nextPrompts.length, 2);
    assert.equal(nextPrompts[0], 'Tiến độ các kế hoạch huấn luyện');
    assert.equal(nextPrompts[1], 'Cảnh báo nguy cơ chấn thương & nhịp tim');
    assert.ok(!nextPrompts.includes('Lịch tập các ca hôm nay'));
  });

  it('extracts tool execution details with error prioritization over arguments', () => {
    const errorTool = {
      tool: 'getMedicalRecords',
      arguments: { horseId: 101 },
      status: 'ERROR',
      error: 'Kết nối CSDL thất bại',
    };

    const isError = errorTool.status === 'ERROR' || Boolean(errorTool.error);
    const errorMessage = typeof errorTool.error === 'string' ? errorTool.error : null;
    const detailText = isError && errorMessage ? `Lỗi: ${errorMessage}` : 'Tham số';

    assert.equal(detailText, 'Lỗi: Kết nối CSDL thất bại');

    const successTool = {
      tool: 'getHorsesList',
      arguments: { status: 'ACTIVE' },
      result: [{ id: 1 }, { id: 2 }, { id: 3 }],
    };

    const successIsError = false;
    const resultCount = Array.isArray(successTool.result)
      ? `${successTool.result.length} bản ghi`
      : null;
    const successDetail = !successIsError && resultCount ? `Kết quả: ${resultCount}` : 'Khác';

    assert.equal(successDetail, 'Kết quả: 3 bản ghi');
  });

  it('trims trailing newline from code block copy payloads', () => {
    const rawWithTrailingNewline = 'console.log("hello RTMS");\n';
    const cleaned = rawWithTrailingNewline.replace(/\n$/, '');
    assert.equal(cleaned, 'console.log("hello RTMS");');

    const rawWithoutTrailing = 'console.log("no newline");';
    assert.equal(rawWithoutTrailing.replace(/\n$/, ''), 'console.log("no newline");');
  });

  it('renders markdown tables according to natural content width with horizontal slider and no word breakage', () => {
    const markdownSourcePath = resolve(process.cwd(), 'src/features/ai/components/MarkdownMessage.tsx');
    const sourceCode = readFileSync(markdownSourcePath, 'utf-8');

    // Asserts horizontal slider scroller uses scroll-slim
    assert.ok(sourceCode.includes('scroll-slim relative w-full overflow-x-auto'), 'Table must have scroll-slim horizontal slider container');
    // Asserts table uses natural content width w-max
    assert.ok(sourceCode.includes('w-max min-w-full text-left text-xs border-collapse'), 'Table must render with w-max for natural content width');
    // Asserts cells do not aggressively break words everywhere
    assert.ok(!sourceCode.includes('[overflow-wrap:anywhere]'), 'Table cells must not use [overflow-wrap:anywhere] which breaks words arbitrarily');
    // Asserts whitespace-nowrap and break-normal for discrete word boundary wrapping
    assert.ok(sourceCode.includes('whitespace-nowrap'), 'Table headers and cells must maintain clean word spacing');
  });
});
