import type { HorseFitnessTrendItem } from '../../types';
import { DateLabels, GridLines, LegendItem, Series, TrendChartCard, type GridTick } from './ChartParts';
import type { WorkoutHover } from './ChartTooltips';

const MIN = 0;
const MAX = 10;
const TICKS: GridTick[] = [0, 2, 4, 6, 8, 10].map((value) => ({ value }));

/** Performance rating (1-10) of each completed session. */
export function PerformanceChart({
  trend,
  onHoverWorkout,
}: {
  trend: HorseFitnessTrendItem[];
  onHoverWorkout: (hover: WorkoutHover | null) => void;
}) {
  return (
    <TrendChartCard
      title="Performance rating (1–10)"
      legend={<LegendItem color="success" shape="dot">Session performance rating</LegendItem>}
    >
      <GridLines ticks={TICKS} min={MIN} max={MAX} />
      <DateLabels trend={trend} />
      <Series trend={trend} value={(item) => item.performanceRating} min={MIN} max={MAX} color="success" strokeWidth={2.5} radius={4.5} onHover={onHoverWorkout} />
    </TrendChartCard>
  );
}
