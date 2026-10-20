/**
 * Tiện ích hỗ trợ AI Assistant & HITL Card
 */

export function isProposalExpired(secondsLeft: number): boolean {
  return secondsLeft <= 0;
}

export function extractDiffKeys(
  current: Record<string, unknown> = {},
  proposed: Record<string, unknown> = {}
): string[] {
  const allKeys = new Set([...Object.keys(current), ...Object.keys(proposed)]);
  return Array.from(allKeys).filter((key) => current[key] !== proposed[key]);
}

export function generateIdempotencyKey(actionId: number): string {
  const ts = Date.now();
  const rand = Math.random().toString(36).substring(2, 7);
  return `commit-${actionId}-${ts}-${rand}`;
}

export function getQuickPromptsByRole(role?: string): string[] {
  const norm = role?.toUpperCase();
  if (norm === 'VETERINARIAN' || norm === 'VET') {
    return [
      'Tra cứu ca viêm gân tương tự qua RAG',
      'Kiểm tra tiền sử sốt của ngựa',
      'Gợi ý phác đồ hồi phục sau chấn thương',
    ];
  }
  if (norm === 'HEAD_TRAINER' || norm === 'TRAINER') {
    return [
      'Kiểm tra các bài tập workout hôm nay',
      'Đề xuất giảm cường độ bài tập workout #1',
      'Tổng hợp khối lượng tập tuần này',
    ];
  }
  if (norm === 'GROOM' || norm === 'STABLE_STAFF') {
    return [
      'Danh sách nhiệm vụ chăm sóc hôm nay',
      'Kiểm tra chuồng ngựa khu A',
      'Ngựa nào cần khẩu phần ăn đặc biệt?',
    ];
  }
  if (norm === 'CLUB_MANAGER' || norm === 'RACING_MANAGER') {
    return [
      'Báo cáo tổng quan sức chứa chuồng',
      'Thống kê tình trạng sức khỏe toàn đàn',
      'Danh sách ngựa đăng ký nhập đàn mới',
    ];
  }
  return [
    'Hồ sơ sức khỏe tổng quát của ngựa',
    'Lịch trình tập luyện tuần này',
    'Thông tin chuồng và người chăm sóc',
  ];
}
