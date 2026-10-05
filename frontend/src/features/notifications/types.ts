export type NotificationType =
  | 'ADMISSION_VET_ASSIGNED'
  | 'ADMISSION_TRAINER_ASSIGNED'
  | string;

export interface NotificationItem {
  id: number;
  notificationType: NotificationType;
  title: string;
  message: string;
  referenceType?: string | null;
  referenceId?: number | null;
  read: boolean;
  createdAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export interface NotificationPageResponse {
  content: NotificationItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
