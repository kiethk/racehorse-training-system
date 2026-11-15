'use client';

import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  ReferenceLine,
} from 'recharts';
import { Panel, Pill, DataTable } from '@/components/ui';
import type { DataTableColumn } from '@/components/ui/DataTable';
import type { TrainerEvaluationEdaDto, TrainerPerformanceDto } from '../types/managerEda';

interface TrainerEvaluationEdaViewProps {
  data: TrainerEvaluationEdaDto;
}

export const TrainerEvaluationEdaView: React.FC<TrainerEvaluationEdaViewProps> = ({ data }) => {
  const chartData = data.trainers.map((t) => ({
    name: t.trainerName,
    adherence: t.adherenceRate,
    rating: t.averageRating > 0 ? Math.round(t.averageRating * 100) / 10 : 0,
    speedGain: t.speedGainKmh,
    hrGain: t.hrRecoveryImprovementBpm,
    completed: t.completedWorkouts,
    scheduled: t.scheduledWorkouts,
  }));

  const columns: DataTableColumn<TrainerPerformanceDto>[] = [
    {
      id: 'trainer',
      header: 'Trainer',
      render: (row: TrainerPerformanceDto) => (
        <div>
          <span className="font-semibold text-xs text-[var(--color-text-primary)]">
            {row.trainerName}
          </span>
          <span className="block text-[11px] text-[var(--color-text-muted)]">
            ID: #{row.trainerId}
          </span>
        </div>
      ),
    },
    {
      id: 'workouts',
      header: 'Workouts (Ran / Total)',
      render: (row: TrainerPerformanceDto) => (
        <span className="text-xs">
          <strong className="text-[var(--color-text-primary)]">{row.completedWorkouts}</strong> / {row.scheduledWorkouts}
        </span>
      ),
    },
    {
      id: 'adherence',
      header: 'Adherence',
      render: (row: TrainerPerformanceDto) => (
        <Pill
          tone={row.adherenceRate >= 80 ? 'success' : row.adherenceRate >= 60 ? 'warning' : 'danger'}
          size="sm"
        >
          {row.adherenceRate.toFixed(1)}%
        </Pill>
      ),
    },
    {
      id: 'rating',
      header: 'Average Rating',
      render: (row: TrainerPerformanceDto) => (
        <span className="text-xs font-semibold text-[var(--color-text-primary)]">
          {row.averageRating > 0 ? `${row.averageRating.toFixed(1)} / 10` : '—'}
        </span>
      ),
    },
    {
      id: 'speedGain',
      header: 'Speed Improvement (km/h)',
      render: (row: TrainerPerformanceDto) => (
        <span
          className={`text-xs font-mono font-bold ${
            row.speedGainKmh > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-500'
          }`}
        >
          {row.speedGainKmh > 0 ? `+${row.speedGainKmh.toFixed(2)} km/h` : `${row.speedGainKmh.toFixed(2)} km/h`}
        </span>
      ),
    },
    {
      id: 'hrGain',
      header: 'Cardio Recovery Improvement (bpm)',
      render: (row: TrainerPerformanceDto) => (
        <span
          className={`text-xs font-mono font-bold ${
            row.hrRecoveryImprovementBpm > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-500'
          }`}
        >
          {row.hrRecoveryImprovementBpm > 0
            ? `-${row.hrRecoveryImprovementBpm.toFixed(1)} bpm`
            : `${row.hrRecoveryImprovementBpm.toFixed(1)} bpm`}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* Top Metrics Row */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Overall Workout Adherence</span>
          <div className="text-2xl font-bold text-[var(--color-text-primary)]">
            {data.overallAdherenceRate.toFixed(1)}%
          </div>
        </Panel>

        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Total Training Sessions</span>
          <div className="text-2xl font-bold text-[var(--color-text-primary)]">
            {data.totalWorkoutsCompleted}{' '}
            <span className="text-sm font-normal text-[var(--color-text-muted)]">
              / {data.totalWorkoutsScheduled} workouts
            </span>
          </div>
        </Panel>

        <Panel padded className="space-y-1">
          <span className="text-xs text-[var(--color-text-muted)]">Active Trainers</span>
          <div className="text-2xl font-bold text-[var(--color-text-primary)]">
            {data.trainers.length}
          </div>
        </Panel>
      </div>

      {/* Trainer Performance Chart */}
      <Panel padded className="space-y-3">
        <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
          Trainer Workload Adherence vs. Performance Rating
        </h3>

        <div className="h-72 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 15, right: 20, left: 15, bottom: 35 }}>
              <CartesianGrid vertical={false} strokeDasharray="3 3" opacity={0.2} />
              <XAxis dataKey="name" tick={{ fontSize: 11 }} interval={0} angle={-15} textAnchor="end" />
              <YAxis unit="%" domain={[0, 100]} width={45} tick={{ fontSize: 11 }} />
              <Tooltip
                formatter={(val: unknown, name: unknown) => [
                  `${val}%`,
                  name === 'adherence' ? 'Workout Adherence' : 'Rating Index',
                ]}
              />
              <Legend wrapperStyle={{ fontSize: 12, paddingTop: 8 }} />
              <Bar dataKey="adherence" name="Adherence Rate (%)" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              <Bar dataKey="rating" name="Rating Index (%)" fill="#10b981" radius={[4, 4, 0, 0]} />
              <ReferenceLine
                y={80}
                stroke="#10b981"
                strokeDasharray="4 4"
                strokeWidth={2}
                                label={{ value: 'Target 80%', fill: '#10b981', fontSize: 11, fontWeight: 700, position: 'insideTopRight' }}
              />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Panel>

      {/* Table of Trainers */}
      <Panel padded className="space-y-3">
        <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
          Trainer Workout Performance Summary
        </h3>
        <DataTable
          ariaLabel="Trainer performance statistics"
          rows={data.trainers}
          columns={columns}
          getRowKey={(row: TrainerPerformanceDto) => row.trainerId}
          pageSize={10}
          emptyTitle="No trainer workout records found"
        />
      </Panel>
    </div>
  );
};
