/**
 * Tiện ích toán học và xử lý dữ liệu biểu đồ AI SVG
 */

export interface ChartDataItem {
  label: string;
  value: number;
  color: string;
  percentage: number;
}

export interface ChartConfig {
  type: 'donut' | 'pie' | 'bar';
  title?: string;
  subtitle?: string;
  unit?: string;
  orientation?: 'horizontal' | 'vertical';
  data: ChartDataItem[];
  total: number;
}

export const CHART_PALETTE = [
  '#10b981', // emerald-500
  '#3b82f6', // blue-500
  '#f59e0b', // amber-500
  '#8b5cf6', // violet-500
  '#ec4899', // pink-500
  '#06b6d4', // cyan-500
  '#f97316', // orange-500
  '#14b8a6', // teal-500
  '#6366f1', // indigo-500
  '#e11d48', // rose-600
];

/**
 * Trích xuất toạ độ từ toạ độ cực (Polar) sang Descartes (Cartesian)
 * Góc 0 rad tính từ 12 giờ (đỉnh trên cùng).
 */
export function polarToCartesian(
  cx: number,
  cy: number,
  r: number,
  angleInRadians: number
): { x: number; y: number } {
  return {
    x: cx + r * Math.sin(angleInRadians),
    y: cy - r * Math.cos(angleInRadians),
  };
}

/**
 * Sinh path SVG cho cung tròn (Donut slice hoặc Pie slice)
 */
export function describeArc(
  cx: number,
  cy: number,
  r: number,
  innerR: number,
  startAngle: number,
  endAngle: number
): string {
  const angleDiff = endAngle - startAngle;

  // Nếu là vòng tròn đầy (360 độ hoặc xấp xỉ 2PI)
  if (angleDiff >= 2 * Math.PI - 0.001) {
    const midAngle = startAngle + Math.PI;
    const p1 = describeArc(cx, cy, r, innerR, startAngle, midAngle);
    const p2 = describeArc(cx, cy, r, innerR, midAngle, endAngle);
    return `${p1} ${p2}`;
  }

  const startOuter = polarToCartesian(cx, cy, r, startAngle);
  const endOuter = polarToCartesian(cx, cy, r, endAngle);
  const largeArcFlag = angleDiff > Math.PI ? 1 : 0;

  // Pie slice (hình quạt tròn đầy)
  if (innerR <= 0) {
    return `M ${cx} ${cy} L ${startOuter.x} ${startOuter.y} A ${r} ${r} 0 ${largeArcFlag} 1 ${endOuter.x} ${endOuter.y} Z`;
  }

  // Donut slice (hình vành khăn)
  const startInner = polarToCartesian(cx, cy, innerR, startAngle);
  const endInner = polarToCartesian(cx, cy, innerR, endAngle);

  return [
    `M ${startInner.x} ${startInner.y}`,
    `L ${startOuter.x} ${startOuter.y}`,
    `A ${r} ${r} 0 ${largeArcFlag} 1 ${endOuter.x} ${endOuter.y}`,
    `L ${endInner.x} ${endInner.y}`,
    `A ${innerR} ${innerR} 0 ${largeArcFlag} 0 ${startInner.x} ${startInner.y}`,
    'Z',
  ].join(' ');
}

/**
 * Parse và chuẩn hoá cấu hình biểu đồ từ JSON raw
 */
export function parseChartConfig(rawText: string): ChartConfig | null {
  if (!rawText || !rawText.trim()) return null;

  try {
    let cleaned = rawText.trim();
    // Bỏ markdown block markers nếu có
    if (cleaned.startsWith('```')) {
      cleaned = cleaned.replace(/^```[a-zA-Z0-9_-]*\s*/, '').replace(/\s*```$/, '');
    }

    const obj = JSON.parse(cleaned);
    if (!obj || typeof obj !== 'object' || Array.isArray(obj)) {
      return null;
    }

    const rawType = String(obj.type || '').toLowerCase().trim();
    if (rawType !== 'donut' && rawType !== 'pie' && rawType !== 'bar') {
      return null;
    }

    // Trích xuất mảng dữ liệu (hỗ trợ nhiều tên trường data/items/series/slices/values)
    let rawItems: any[] = [];
    if (Array.isArray(obj.data)) {
      rawItems = obj.data;
    } else if (Array.isArray(obj.items)) {
      rawItems = obj.items;
    } else if (Array.isArray(obj.series)) {
      rawItems = obj.series;
    } else if (Array.isArray(obj.slices)) {
      rawItems = obj.slices;
    } else if (Array.isArray(obj.labels) && Array.isArray(obj.values)) {
      rawItems = obj.labels.map((lbl: string, idx: number) => ({
        label: lbl,
        value: obj.values[idx],
        color: obj.colors?.[idx],
      }));
    }

    if (!rawItems || rawItems.length === 0) {
      return null;
    }

    const items: ChartDataItem[] = [];
    for (let i = 0; i < rawItems.length; i++) {
      const it = rawItems[i];
      if (!it || typeof it !== 'object') continue;

      const label = String(
        it.label ?? it.name ?? it.title ?? it.category ?? it.key ?? `Mục ${i + 1}`
      ).trim();

      const rawVal = it.value ?? it.count ?? it.total ?? it.amount ?? it.val ?? 0;
      let numVal = typeof rawVal === 'number' ? rawVal : parseFloat(String(rawVal));
      if (isNaN(numVal) || numVal < 0) numVal = 0;

      const color =
        it.color || it.fill || CHART_PALETTE[i % CHART_PALETTE.length];

      items.push({
        label,
        value: numVal,
        color,
        percentage: 0,
      });
    }

    if (items.length === 0) return null;

    const total = items.reduce((sum, item) => sum + item.value, 0);
    items.forEach((item) => {
      item.percentage = total > 0 ? (item.value / total) * 100 : 0;
    });

    return {
      type: rawType as 'donut' | 'pie' | 'bar',
      title: obj.title || obj.heading || obj.name,
      subtitle: obj.subtitle || obj.description,
      unit: obj.unit,
      orientation: obj.orientation === 'vertical' ? 'vertical' : 'horizontal',
      data: items,
      total,
    };
  } catch {
    return null;
  }
}
