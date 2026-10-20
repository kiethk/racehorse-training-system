import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  parseChartConfig,
  describeArc,
  polarToCartesian,
} from '../lib/chartUtils.ts';

describe('AI SVG Chart Parsing and Geometry Tests', () => {
  it('correctly parses a Donut chart JSON payload', () => {
    const rawJson = JSON.stringify({
      type: 'donut',
      title: 'Công suất chuồng trại',
      data: [
        { label: 'Đang nuôi', value: 45, color: '#10b981' },
        { label: 'Cách ly', value: 5, color: '#f59e0b' },
        { label: 'Trống', value: 10, color: '#94a3b8' },
      ],
    });

    const config = parseChartConfig(rawJson);
    assert.ok(config !== null);
    assert.equal(config.type, 'donut');
    assert.equal(config.title, 'Công suất chuồng trại');
    assert.equal(config.total, 60);
    assert.equal(config.data.length, 3);
    assert.equal(config.data[0].label, 'Đang nuôi');
    assert.equal(config.data[0].value, 45);
    assert.equal(config.data[0].percentage, 75);
    assert.equal(config.data[1].percentage, (5 / 60) * 100);
    assert.equal(config.data[2].percentage, (10 / 60) * 100);
  });

  it('correctly parses a Pie chart JSON payload with markdown code fence', () => {
    const rawWithFence = `\`\`\`chart
    {
      "type": "pie",
      "title": "Phân loại giống ngựa",
      "items": [
        { "name": "Thoroughbred", "count": "30" },
        { "name": "Arabian", "count": "10" }
      ]
    }
    \`\`\``;

    const config = parseChartConfig(rawWithFence);
    assert.ok(config !== null);
    assert.equal(config.type, 'pie');
    assert.equal(config.title, 'Phân loại giống ngựa');
    assert.equal(config.total, 40);
    assert.equal(config.data[0].label, 'Thoroughbred');
    assert.equal(config.data[0].value, 30);
    assert.equal(config.data[0].percentage, 75);
    assert.equal(config.data[1].label, 'Arabian');
    assert.equal(config.data[1].value, 10);
    assert.equal(config.data[1].percentage, 25);
  });

  it('correctly parses a Bar chart with labels and values parallel arrays', () => {
    const rawJson = JSON.stringify({
      type: 'bar',
      heading: 'Số buổi tập theo tuần',
      orientation: 'vertical',
      labels: ['T2', 'T3', 'T4', 'T5'],
      values: [8, 12, 10, 15],
    });

    const config = parseChartConfig(rawJson);
    assert.ok(config !== null);
    assert.equal(config.type, 'bar');
    assert.equal(config.title, 'Số buổi tập theo tuần');
    assert.equal(config.orientation, 'vertical');
    assert.equal(config.data.length, 4);
    assert.equal(config.data[0].label, 'T2');
    assert.equal(config.data[0].value, 8);
    assert.equal(config.data[3].label, 'T5');
    assert.equal(config.data[3].value, 15);
    assert.equal(config.total, 45);
  });

  it('gracefully rejects invalid JSON or unsupported chart types', () => {
    assert.equal(parseChartConfig(''), null);
    assert.equal(parseChartConfig('{ not valid json }'), null);
    assert.equal(parseChartConfig(JSON.stringify({ type: 'radar', data: [1, 2] })), null);
    assert.equal(parseChartConfig(JSON.stringify({ type: 'donut', data: [] })), null);
    assert.equal(parseChartConfig(JSON.stringify({ type: 'donut' })), null);
  });

  it('calculates polarToCartesian coordinates accurately', () => {
    const center = 100;
    const radius = 50;

    // 0 rad = top (12 o'clock) -> x = cx, y = cy - r
    const top = polarToCartesian(center, center, radius, 0);
    assert.ok(Math.abs(top.x - 100) < 0.0001);
    assert.ok(Math.abs(top.y - 50) < 0.0001);

    // PI / 2 rad = right (3 o'clock) -> x = cx + r, y = cy
    const right = polarToCartesian(center, center, radius, Math.PI / 2);
    assert.ok(Math.abs(right.x - 150) < 0.0001);
    assert.ok(Math.abs(right.y - 100) < 0.0001);

    // PI rad = bottom (6 o'clock) -> x = cx, y = cy + r
    const bottom = polarToCartesian(center, center, radius, Math.PI);
    assert.ok(Math.abs(bottom.x - 100) < 0.0001);
    assert.ok(Math.abs(bottom.y - 150) < 0.0001);
  });

  it('generates valid SVG arc paths for both donut and pie geometries', () => {
    // Donut slice: innerR = 50, outerR = 80
    const donutPath = describeArc(100, 100, 80, 50, 0, Math.PI / 2);
    assert.ok(donutPath.startsWith('M'));
    assert.ok(donutPath.includes('A 80 80'));
    assert.ok(donutPath.includes('A 50 50'));
    assert.ok(donutPath.endsWith('Z'));

    // Pie slice: innerR = 0, outerR = 80
    const piePath = describeArc(100, 100, 80, 0, 0, Math.PI / 2);
    assert.ok(piePath.startsWith('M 100 100'));
    assert.ok(piePath.includes('A 80 80'));
    assert.ok(piePath.endsWith('Z'));

    // Full 360 circle slice (2 * PI)
    const fullCirclePath = describeArc(100, 100, 80, 50, 0, 2 * Math.PI);
    assert.ok(fullCirclePath.includes('A 80 80'));
    assert.ok(fullCirclePath.includes('A 50 50'));
  });
});
