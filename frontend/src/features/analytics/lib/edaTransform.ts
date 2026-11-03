import type { CorrelationStrength } from '../types/eda';

export interface CorrelationToneInfo {
  label: string;
  tone: 'success' | 'warning' | 'info' | 'neutral' | 'danger';
  description: string;
}

export function formatCorrelationStrength(strength: CorrelationStrength, pearsonR: number): CorrelationToneInfo {
  const sign = pearsonR > 0 ? '+' : '';
  const rFormatted = `${sign}${pearsonR.toFixed(2)}`;

  switch (strength) {
    case 'STRONG_POSITIVE':
      return {
        label: `Tương quan thuận mạnh (${rFormatted})`,
        tone: 'info',
        description: 'Tốc độ tăng cao đi kèm với nhịp tim hồi phục tăng rõ rệt.',
      };
    case 'MODERATE_POSITIVE':
      return {
        label: `Tương quan thuận vừa (${rFormatted})`,
        tone: 'neutral',
        description: 'Có xu hướng tăng nhịp tim khi tăng tốc độ bài tập.',
      };
    case 'STRONG_NEGATIVE':
      return {
        label: `Tương quan nghịch mạnh (${rFormatted})`,
        tone: 'success',
        description: 'Tốc độ cao nhưng nhịp tim hồi phục lại thấp (chỉ dấu thể lực vượt trội).',
      };
    case 'MODERATE_NEGATIVE':
      return {
        label: `Tương quan nghịch vừa (${rFormatted})`,
        tone: 'success',
        description: 'Chiến mã duy trì tốc độ tốt với mức tiêu hao nhịp tim vừa phải.',
      };
    case 'WEAK':
      return {
        label: `Tương quan yếu (${rFormatted})`,
        tone: 'neutral',
        description: 'Không có mối liên hệ tuyến tính rõ rệt giữa hai chỉ số này.',
      };
    case 'NO_VARIANCE':
      return {
        label: 'Dữ liệu đồng nhất',
        tone: 'neutral',
        description: 'Các giá trị đo được đều bằng nhau nên không xác định được hệ số tương quan.',
      };
    case 'INSUFFICIENT_DATA':
    default:
      return {
        label: 'Chưa đủ dữ liệu',
        tone: 'warning',
        description: 'Cần ít nhất 2 buổi tập có đầy đủ chỉ số tốc độ và nhịp tim để phân tích.',
      };
  }
}

export function calculateTrendLinePoints(
  minX: number,
  maxX: number,
  slope: number,
  intercept: number
): { x: number; y: number }[] {
  if (minX >= maxX || isNaN(slope) || isNaN(intercept)) {
    return [];
  }
  return [
    { x: minX, y: Math.round((slope * minX + intercept) * 10) / 10 },
    { x: maxX, y: Math.round((slope * maxX + intercept) * 10) / 10 },
  ];
}
