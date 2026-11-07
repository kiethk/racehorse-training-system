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
  '#6657c7', // primary purple
  '#496bc3', // training blue
  '#b94b58', // medical red
  '#8c6a2e', // grooming brown
  '#5d4fb5', // racing indigo
  '#28756e', // finance teal
  '#1f7a4d', // success green
  '#8a5b08', // warning amber
  '#c33c3c', // danger red
  '#315fa8', // info blue
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

    let rawType = String(obj.type || '').toLowerCase().trim();
    if (rawType === 'line') {
      rawType = 'bar';
    }
    if (rawType !== 'donut' && rawType !== 'pie' && rawType !== 'bar') {
      return null;
    }

    // Trích xuất mảng dữ liệu (hỗ trợ nhiều tên trường data/items/series/slices/values)
    let rawItems: unknown[] = [];
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
      const it = rawItems[i] as Record<string, unknown> | null;
      if (!it || typeof it !== 'object') continue;

      const label = String(
        it.label ?? it.name ?? it.title ?? it.category ?? it.key ?? `Mục ${i + 1}`
      ).trim();

      const rawVal = it.value ?? it.count ?? it.total ?? it.amount ?? it.val ?? 0;
      let numVal = typeof rawVal === 'number' ? rawVal : parseFloat(String(rawVal));
      if (isNaN(numVal) || numVal < 0) numVal = 0;

      const color =
        typeof it.color === 'string'
          ? it.color
          : typeof it.fill === 'string'
          ? it.fill
          : CHART_PALETTE[i % CHART_PALETTE.length];

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
