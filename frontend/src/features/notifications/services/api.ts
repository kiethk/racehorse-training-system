import { apiGet, apiPatch } from '@/services/api';
import type {
  NotificationItem,
  NotificationPageResponse,
  UnreadCountResponse,
} from '../types';

interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export const notificationApi = {
  getNotifications: async (status?: 'ALL' | 'UNREAD' | 'READ', page = 0, size = 20): Promise<NotificationPageResponse> => {
    const params = new URLSearchParams();
    if (status && status !== 'ALL') params.set('status', status);
    params.set('page', String(page));
    params.set('size', String(size));
    const response = await apiGet<ApiResponse<NotificationPageResponse>>(`/api/notifications?${params}`);
    return response.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const response = await apiGet<ApiResponse<UnreadCountResponse>>('/api/notifications/unread-count');
    return response.data.unreadCount;
  },

  markAsRead: async (id: number): Promise<NotificationItem> => {
    const response = await apiPatch<ApiResponse<NotificationItem>>(`/api/notifications/${id}/read`, {});
    return response.data;
  },

  markAllAsRead: async (): Promise<{ updated: number }> => {
    const response = await apiPatch<ApiResponse<{ updated: number }>>('/api/notifications/read-all', {});
    return response.data;
  },
};
