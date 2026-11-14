/**
 * Tiện ích hỗ trợ AI Assistant & Quick Prompts
 */

import type { Role } from '../../../types/auth.ts';
import { ROLE_LABELS } from '../../../lib/roleRoute.ts';

export const PROMPTS_BY_ROLE: Record<Role, string[]> = {
  HEAD_TRAINER: [
    'Lịch tập các ca hôm nay',
    'Tiến độ các kế hoạch huấn luyện',
    'Cảnh báo nguy cơ chấn thương & nhịp tim',
    'Phân tích sở trường của ngựa',
    'Danh sách Groom làm việc cùng',
  ],
  VETERINARIAN: [
    'Tổng quan ngựa đang & chờ khám',
    'Tra cứu ca bệnh lâm sàng qua RAG',
    'Lịch chăm sóc y tế định kỳ',
    'Danh sách ngựa cách ly y tế',
    'Báo cáo sự cố chuồng trại 48h qua',
  ],
  GROOM: [
    'Checklist công việc hôm nay (3 nguồn)',
    'Chuồng và ngựa tôi phụ trách',
    'Cảnh báo ăn uống & vận động của ngựa',
    'Tra cứu vị trí chuồng & người phụ trách',
  ],
  CLUB_MANAGER: [
    'Danh sách nhân sự & bác sĩ',
    'Vẽ biểu đồ công suất chuồng trại',
    'Các đơn nhập chuồng chờ duyệt',
    'Nhật ký kiểm toán gần đây',
  ],
  HORSE_OWNER: [
    'Danh sách chiến mã của tôi',
    'Lịch tập sắp tới của ngựa',
    'Tóm tắt sức khỏe và lần khám gần nhất',
    'Tiến độ khóa huấn luyện của ngựa',
  ],
};

/**
 * Trả về tên hiển thị chuẩn thân thiện cho vai trò của người dùng (thay vì enum thô như HEAD_TRAINER)
 */
export function getRoleDisplayName(role?: Role | string | null): string {
  if (!role) return 'Trợ lý AI';
  return (ROLE_LABELS as Record<string, string>)[role] || role;
}

export function getQuickPromptsByRole(role?: Role | string | null): string[] {
  if (!role) return PROMPTS_BY_ROLE.HORSE_OWNER;
  const norm = role.toUpperCase();
  if (norm === 'HEAD_TRAINER' || norm === 'TRAINER') {
    return PROMPTS_BY_ROLE.HEAD_TRAINER;
  }
  if (norm === 'VETERINARIAN' || norm === 'VET') {
    return PROMPTS_BY_ROLE.VETERINARIAN;
  }
  if (norm === 'GROOM' || norm === 'STABLE_STAFF') {
    return PROMPTS_BY_ROLE.GROOM;
  }
  if (norm === 'CLUB_MANAGER' || norm === 'RACING_MANAGER' || norm === 'ADMIN') {
    return PROMPTS_BY_ROLE.CLUB_MANAGER;
  }
  if (norm === 'HORSE_OWNER' || norm === 'OWNER') {
    return PROMPTS_BY_ROLE.HORSE_OWNER;
  }
  return PROMPTS_BY_ROLE.HORSE_OWNER;
}
