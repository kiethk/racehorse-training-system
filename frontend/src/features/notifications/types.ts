export type NotificationType =
  | 'ADMISSION_VET_ASSIGNED'
  | 'ADMISSION_TRAINER_ASSIGNED'
  | 'CARE_SCHEDULE_URGENT_ASSIGNED'
  | 'URGENT_CASE_CLOSED'
  | 'ROUTINE_ASSIGNED'
  | 'CARE_SCHEDULE_PAUSED'
  | 'CARE_SCHEDULE_CANCELLED'
  | 'ADMISSION_NEW_FOR_GROOM'
  | 'ADMISSION_GROOM_ASSIGNED'
  | 'ADMISSION_WAITING_FOR_ARRIVAL'
  | 'ADMISSION_APPROVED'
  | 'ADMISSION_REJECTED'
  | 'ADMISSION_ARRIVAL_EXPIRED'
  | 'HORSE_TRAINING_ALLOWED'
  | 'HORSE_EDIT_REQUESTED'
  | 'HORSE_EDIT_REVIEWED'
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

export interface NotificationFeedResponse {
  content: NotificationItem[];
  nextCursor: string | null;
  hasMore: boolean;
}

export interface NotificationPageResponse {
  content: NotificationItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
