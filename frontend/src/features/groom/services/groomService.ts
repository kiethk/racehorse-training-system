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
   * Completes a routine task with what the Groom recorded. The note and the photo are both optional.
   * Sent as multipart through the shared client, which adds the access token.
   */
  completeTask: async (id: number, evidence?: { note?: string; photo?: File | null }): Promise<void> => {
    const body = new FormData();
    if (evidence?.note?.trim()) body.append('note', evidence.note.trim());
    if (evidence?.photo) body.append('file', evidence.photo);
    await apiUpload<ApiResponse<unknown>>(`/api/groom-daily-tasks/${id}/complete`, body);
  },

  generateRoutine: async (date?: string): Promise<void> => {
    const qs = date ? `?date=${date}` : '';
    await apiPost<ApiResponse<unknown>>(`/api/groom-daily-tasks/generate-routine${qs}`, {});
  },
};
