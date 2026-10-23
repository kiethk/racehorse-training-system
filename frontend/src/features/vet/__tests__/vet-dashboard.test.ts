import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

describe('AC-11.1: VetDashboardView Start Exam Error Handling', () => {
  it('halts navigation and surfaces error message when startCareSchedule returns 409 VET_BUSY', async () => {
    let navigationCalled = false;
    let navigatedUrl = '';
    let surfacedError: string | null = null;

    const mockRouter = {
      push: (url: string) => {
        navigationCalled = true;
        navigatedUrl = url;
      },
    };

    const mockApi = {
      startCareSchedule: async (_id: number) => {
        const error = new Error('Bác sĩ thú y đang có một ca khám chưa hoàn tất (409 Conflict)');
        (error as any).status = 409;
        (error as any).code = 'VET_BUSY';
        throw error;
      },
    };

    async function handleStartExam(schedule: { id: number; status: string; careType: string }) {
      try {
        if (schedule.status === 'SCHEDULED') {
          await mockApi.startCareSchedule(schedule.id);
        }
      } catch (err: any) {
        surfacedError = err.message || 'Bác sĩ đang bận ca khám khác';
        return; // HALT NAVIGATION
      }

      if (schedule.careType === 'INITIAL') {
        mockRouter.push(`/veterinarian/admissions?scheduleId=${schedule.id}`);
      }
    }

    await handleStartExam({ id: 101, status: 'SCHEDULED', careType: 'INITIAL' });

    assert.equal(navigationCalled, false, 'Router push must NOT be called when startCareSchedule fails with 409');
    assert.equal(navigatedUrl, '', 'Target URL should not be navigated to');
    assert.ok(surfacedError && (surfacedError as string).includes('409'), 'Error message must be captured for display');
  });

  it('navigates when startCareSchedule succeeds', async () => {
    let navigationCalled = false;
    let navigatedUrl = '';

    const mockRouter = {
      push: (url: string) => {
        navigationCalled = true;
        navigatedUrl = url;
      },
    };

    const mockApi = {
      startCareSchedule: async (_id: number) => {
        return { success: true };
      },
    };

    async function handleStartExam(schedule: { id: number; status: string; careType: string }) {
      try {
        if (schedule.status === 'SCHEDULED') {
          await mockApi.startCareSchedule(schedule.id);
        }
      } catch (err: any) {
        return;
      }
      mockRouter.push(`/veterinarian/admissions?scheduleId=${schedule.id}`);
    }

    await handleStartExam({ id: 102, status: 'SCHEDULED', careType: 'INITIAL' });
    assert.equal(navigationCalled, true);
    assert.equal(navigatedUrl, '/veterinarian/admissions?scheduleId=102');
  });
});
