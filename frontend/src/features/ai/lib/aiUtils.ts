/**
 * Tiện ích hỗ trợ AI Assistant & Quick Prompts
 */

/** @deprecated HITL proposal expiration utility */
export function isProposalExpired(secondsLeft: number): boolean {
  return secondsLeft <= 0;
}

/** @deprecated HITL diff keys extraction */
export function extractDiffKeys(
  current: Record<string, unknown> = {},
  proposed: Record<string, unknown> = {}
): string[] {
  const allKeys = new Set([...Object.keys(current), ...Object.keys(proposed)]);
  return Array.from(allKeys).filter((key) => current[key] !== proposed[key]);
}

/** @deprecated HITL idempotency key generator */
export function generateIdempotencyKey(actionId: number): string {
  const ts = Date.now();
  const rand = Math.random().toString(36).substring(2, 7);
  return `commit-${actionId}-${ts}-${rand}`;
}

export function getQuickPromptsByRole(role?: string): string[] {
  const norm = role?.toUpperCase();
  if (norm === 'HEAD_TRAINER' || norm === 'TRAINER') {
    return [
      'Lịch tập các ca hôm nay',
      'Tiến độ các khóa đào tạo',
      'Cảnh báo nguy cơ chấn thương & nhịp tim',
      'Phân tích sở trường của ngựa',
    ];
  }
  if (norm === 'VETERINARIAN' || norm === 'VET') {
    return [
      'Tra cứu ca viêm gân tương tự qua RAG',
      'Lịch chăm sóc y tế định kỳ',
      'Báo cáo sự cố chuồng trại 48h qua',
      'Danh sách ngựa đang cách ly',
    ];
  }
  if (norm === 'GROOM' || norm === 'STABLE_STAFF') {
    return [
      'Checklist công việc hôm nay (3 nguồn)',
      'Chuồng và ngựa tôi phụ trách',
      'Cảnh báo ăn uống & vận động của ngựa',
      'Kiểm tra vật tư chuồng',
    ];
  }
  if (norm === 'CLUB_MANAGER' || norm === 'RACING_MANAGER') {
    return [
      'Danh sách nhân sự & bác sĩ',
      'Vẽ biểu đồ công suất chuồng trại',
      'Các đơn nhập chuồng chờ duyệt',
      'Nhật ký kiểm toán gần đây',
    ];
  }
  if (norm === 'HORSE_OWNER') {
    return [
      'Danh sách chiến mã của tôi',
      'Lịch tập sắp tới của ngựa',
      'Tóm tắt sức khỏe và lần khám gần nhất',
      'Tiến độ khóa huấn luyện của ngựa',
    ];
  }
  return [
    'Danh sách chiến mã của tôi',
    'Lịch tập sắp tới của ngựa',
    'Tóm tắt sức khỏe và lần khám gần nhất',
    'Tiến độ khóa huấn luyện của ngựa',
  ];
}
