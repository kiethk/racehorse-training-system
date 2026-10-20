import { apiGet, apiPost, apiUpload } from '@/services/api';
import type { TodayTaskItem } from '../types';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
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

  /**
   * Completes a block of routine tasks in one action, with an optional note and up to five optional photos
   * shared by every task in it. Sent as multipart through the shared client, which adds the access token.
   */
  completeTasks: async (taskIds: number[], evidence?: { note?: string; photos?: File[] }): Promise<void> => {
    const body = new FormData();
    taskIds.forEach((id) => body.append('taskIds', String(id)));
    if (evidence?.note?.trim()) body.append('note', evidence.note.trim());
    evidence?.photos?.forEach((photo) => body.append('files', photo));
    await apiUpload<ApiResponse<unknown>>('/api/groom-daily-tasks/complete', body);
  },

  generateRoutine: async (date?: string): Promise<void> => {
    const qs = date ? `?date=${date}` : '';
    await apiPost<ApiResponse<unknown>>(`/api/groom-daily-tasks/generate-routine${qs}`, {});
  },
};
