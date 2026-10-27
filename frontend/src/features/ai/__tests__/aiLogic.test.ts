import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { getQuickPromptsByRole } from '../lib/aiUtils.ts';

describe('AI Quick Prompts Role-Based Scoping Tests', () => {
  it('tailors quick prompts for Trainer roles (HEAD_TRAINER & TRAINER alias)', () => {
    const trainerPrompts = getQuickPromptsByRole('HEAD_TRAINER');
    assert.ok(trainerPrompts.some((p) => p.includes('Lịch tập') || p.includes('huấn luyện')));
    assert.ok(trainerPrompts.some((p) => p.includes('Groom')));

    const aliasPrompts = getQuickPromptsByRole('trainer');
    assert.deepEqual(trainerPrompts, aliasPrompts);
  });

  it('tailors quick prompts for Veterinarian roles (VETERINARIAN & VET alias)', () => {
    const vetPrompts = getQuickPromptsByRole('VETERINARIAN');
    assert.ok(vetPrompts.some((p) => p.includes('RAG') || p.includes('bệnh')));
    assert.ok(vetPrompts.some((p) => p.includes('cách ly')));

    const aliasPrompts = getQuickPromptsByRole('vet');
    assert.deepEqual(vetPrompts, aliasPrompts);
  });

  it('tailors quick prompts for Groom roles (GROOM & STABLE_STAFF alias)', () => {
    const groomPrompts = getQuickPromptsByRole('GROOM');
    assert.ok(groomPrompts.some((p) => p.includes('Checklist') || p.includes('chuồng')));
    assert.ok(groomPrompts.some((p) => p.includes('vị trí chuồng')));

    const aliasPrompts = getQuickPromptsByRole('stable_staff');
    assert.deepEqual(groomPrompts, aliasPrompts);
  });

  it('tailors quick prompts for Club Manager roles (CLUB_MANAGER, RACING_MANAGER, ADMIN alias)', () => {
    const managerPrompts = getQuickPromptsByRole('CLUB_MANAGER');
    assert.ok(managerPrompts.includes('Danh sách nhân sự & bác sĩ'));
    assert.ok(managerPrompts.includes('Vẽ biểu đồ công suất chuồng trại'));
    assert.ok(managerPrompts.includes('Các đơn nhập chuồng chờ duyệt'));
    assert.ok(managerPrompts.includes('Nhật ký kiểm toán gần đây'));

    const adminPrompts = getQuickPromptsByRole('ADMIN');
    assert.deepEqual(managerPrompts, adminPrompts);
  });

  it('returns appropriate fallback prompts for Horse Owner and anonymous users', () => {
    const ownerPrompts = getQuickPromptsByRole('HORSE_OWNER');
    assert.ok(ownerPrompts.some((p) => p.includes('chiến mã')));

    const anonymousPrompts = getQuickPromptsByRole(undefined);
    assert.ok(anonymousPrompts.length > 0);
    assert.deepEqual(ownerPrompts, anonymousPrompts);
  });
});
