import { apiGet, apiPost } from '@/services/api';
import type { TodayTaskItem } from '../types';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

async function patchWithMessage<T>(path: string): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'PATCH',
    credentials: 'include',
  });
  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(payload?.message || `API error: ${res.status}`);
  }
  return payload?.data !== undefined ? payload.data : (payload as T);
}

export const groomApi = {
  getTodayTasks: async (date?: string, groomId?: number): Promise<TodayTaskItem[]> => {
    const params = new URLSearchParams();
    if (date) params.set('date', date);
    if (groomId) params.set('groomId', String(groomId));
    const qs = params.toString();
    const url = `/api/groom-daily-tasks/today${qs ? `?${qs}` : ''}`;
    return (await apiGet<ApiResponse<TodayTaskItem[]>>(url)).data;
  },

  completeTask: async (id: number): Promise<void> => {
    await patchWithMessage(`/api/groom-daily-tasks/${id}/complete`);
  },

  generateRoutine: async (date?: string): Promise<void> => {
    const qs = date ? `?date=${date}` : '';
    await apiPost<ApiResponse<unknown>>(`/api/groom-daily-tasks/generate-routine${qs}`, {});
  },
};
