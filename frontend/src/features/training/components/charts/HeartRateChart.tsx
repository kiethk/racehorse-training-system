import type { HorseFitnessTrendItem, TrainingRules } from '../../types';
import { DateLabels, GridLines, LegendItem, Series, TrendChartCard, type GridTick } from './ChartParts';
import type { WorkoutHover } from './ChartTooltips';

const MIN = 50;
const MAX = 250;
const BASE_TICKS = [60, 140, 240];

/**
 * Heart rate per session. The three safety limits are the ones the backend alerts on (served as training rules),
 * so the chart can never disagree with the alerts. Without the rules the limit lines are simply left out.
 */
export function HeartRateChart({
  trend,
  rules,
  onHover,
}: {
  trend: HorseFitnessTrendItem[];
  rules: TrainingRules | null;
  onHover: (hover: WorkoutHover | null) => void;
}) {
  const avgLimit = rules?.averageHeartRateLimit ?? 180;
  const ticks: GridTick[] = BASE_TICKS.map((value) => ({ value }));
  if (rules) {
    ticks.push({ value: rules.maxHeartRateLimit, emphasis: 'danger' });
    ticks.push({ value: avgLimit, emphasis: 'info' });
    ticks.push({ value: rules.recoveryHeartRateLimit, emphasis: 'warning' });
  } else {
    ticks.push({ value: 180 });
  }

  return (
    <TrendChartCard
      title="Heart rate (BPM) and safety limits"
      legend={
        <>
          <LegendItem color="danger">Max HR</LegendItem>
          <LegendItem color="info">Avg HR</LegendItem>
          <LegendItem color="warning">Recovery HR</LegendItem>
          {rules && (
            <span className="text-xs font-medium text-[var(--color-text-secondary)]">
              Limits: <span className="text-[var(--color-danger)]">max {rules.maxHeartRateLimit} bpm</span> ·{' '}
              <span className="text-[var(--color-info)]">avg {avgLimit} bpm</span> ·{' '}
              <span className="text-[var(--color-warning)]">recovery {rules.recoveryHeartRateLimit} bpm</span>
            </span>
          )}
        </>
      }
    >
      <GridLines ticks={ticks} min={MIN} max={MAX} />
      <DateLabels trend={trend} />
      <Series trend={trend} value={(item) => item.maxHeartRate} min={MIN} max={MAX} color="danger" onHover={onHover} />
      <Series trend={trend} value={(item) => item.averageHeartRate} min={MIN} max={MAX} color="info" onHover={onHover} />
      <Series trend={trend} value={(item) => item.recoveryHeartRate} min={MIN} max={MAX} color="warning" onHover={onHover} />
    </TrendChartCard>
  );
}
