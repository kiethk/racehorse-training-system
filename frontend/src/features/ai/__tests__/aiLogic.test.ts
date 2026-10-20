import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  isProposalExpired,
  extractDiffKeys,
  generateIdempotencyKey,
  getQuickPromptsByRole,
} from '../lib/aiUtils.ts';

describe('AI Logic & Quick Prompts Utility Tests', () => {
  it('correctly detects TTL expiration', () => {
    assert.equal(isProposalExpired(300), false);
    assert.equal(isProposalExpired(1), false);
    assert.equal(isProposalExpired(0), true);
    assert.equal(isProposalExpired(-5), true);
  });

  it('accurately identifies modified diff keys', () => {
    const current = { intensity: 'LOW', duration: 30, note: 'Normal' };
    const proposed = { intensity: 'HIGH', duration: 45, note: 'Normal' };

    const diffKeys = extractDiffKeys(current, proposed);
    assert.deepEqual(diffKeys.sort(), ['duration', 'intensity'].sort());
  });

  it('generates unique valid idempotency keys', () => {
    const key1 = generateIdempotencyKey(42);
    const key2 = generateIdempotencyKey(42);

    assert.ok(key1.startsWith('commit-42-'));
    assert.ok(key2.startsWith('commit-42-'));
    assert.notEqual(key1, key2);
  });

  it('tailors quick prompts according to role', () => {
    const vetPrompts = getQuickPromptsByRole('VETERINARIAN');
    assert.ok(vetPrompts.some((p) => p.includes('RAG') || p.includes('viêm gân')));

    const trainerPrompts = getQuickPromptsByRole('HEAD_TRAINER');
    assert.ok(trainerPrompts.some((p) => p.includes('Lịch tập') || p.includes('đào tạo')));

    const groomPrompts = getQuickPromptsByRole('GROOM');
    assert.ok(groomPrompts.some((p) => p.includes('Checklist') || p.includes('chuồng') || p.includes('Chuồng')));

    const managerPrompts = getQuickPromptsByRole('CLUB_MANAGER');
    assert.ok(managerPrompts.includes('Danh sách nhân sự & bác sĩ'));
    assert.ok(managerPrompts.includes('Vẽ biểu đồ công suất chuồng trại'));
    assert.ok(managerPrompts.includes('Các đơn nhập chuồng chờ duyệt'));
    assert.ok(managerPrompts.includes('Nhật ký kiểm toán gần đây'));

    const defaultPrompts = getQuickPromptsByRole('HORSE_OWNER');
    assert.ok(defaultPrompts.some((p) => p.includes('sức khỏe') || p.includes('chiến mã') || p.includes('ngựa')));
  });
});
