/**
 * Tiện ích hỗ trợ AI Assistant & Quick Prompts
 */

export function getQuickPromptsByRole(role?: string): string[] {
  const norm = role?.toUpperCase();
  if (norm === 'HEAD_TRAINER' || norm === 'TRAINER') {
    return [
      'Lịch tập các ca hôm nay',
      'Tiến độ các kế hoạch huấn luyện',
      'Cảnh báo nguy cơ chấn thương & nhịp tim',
      'Phân tích sở trường của ngựa',
      'Danh sách Groom làm việc cùng',
    ];
  }
  if (norm === 'VETERINARIAN' || norm === 'VET') {
    return [
      'Tổng quan ngựa đang & chờ khám',
      'Tra cứu ca bệnh lâm sàng qua RAG',
      'Lịch chăm sóc y tế định kỳ',
      'Danh sách ngựa cách ly y tế',
      'Báo cáo sự cố chuồng trại 48h qua',
    ];
  }
  if (norm === 'GROOM' || norm === 'STABLE_STAFF') {
    return [
      'Checklist công việc hôm nay (3 nguồn)',
      'Chuồng và ngựa tôi phụ trách',
      'Cảnh báo ăn uống & vận động của ngựa',
      'Tra cứu vị trí chuồng & người phụ trách',
    ];
  }
  if (norm === 'CLUB_MANAGER' || norm === 'RACING_MANAGER' || norm === 'ADMIN') {
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
