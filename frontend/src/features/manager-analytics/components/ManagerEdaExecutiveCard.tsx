'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { Panel, Pill, Button, Icon, Skeleton, Notice } from '@/components/ui';
import { managerEdaService } from '../services/managerEdaService';
import type { ManagerEdaOverviewResponse } from '../types/managerEda';
import { formatAdmissionStage } from '../lib/edaFormatters';

export const ManagerEdaExecutiveCard: React.FC = () => {
  const [data, setData] = useState<ManagerEdaOverviewResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        setLoading(true);
        setError(null);
        const res = await managerEdaService.getOverview(90);
        if (active) setData(res);
      } catch (err: any) {
        if (active) setError(err?.message || 'Unable to load executive EDA analytics.');
      } finally {
        if (active) setLoading(false);
      }
    }
    load();
    return () => {
      active = false;
    };
  }, []);

  return (
    <Panel padded className="space-y-4 border border-[var(--color-border)]">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">
            Facility Operations Overview
          </h2>
          <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
            Diagnostic monitoring of vet delays, trainer adherence, quarantine pressure, and admission SLAs.
          </p>
        </div>

        <Link
          href="/manager/analytics"
          className="text-xs font-semibold text-[var(--color-primary)] hover:underline inline-flex items-center gap-1 shrink-0"
        >
          View Operations & Performance <Icon name="chevron-right" size={12} />
        </Link>
      </div>

      {error && <Notice tone="error">{error}</Notice>}

      {loading && !data ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          <Skeleton className="h-20 w-full rounded-md" />
          <Skeleton className="h-20 w-full rounded-md" />
          <Skeleton className="h-20 w-full rounded-md" />
          <Skeleton className="h-20 w-full rounded-md" />
        </div>
      ) : data ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Vet Delay Status */}
          <div className="p-2.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] space-y-1">
            <span className="text-[11px] text-[var(--color-text-muted)] uppercase tracking-wider font-medium">
              Vet Rollover Delays
            </span>
            <div className="flex items-center justify-between">
              <span className="text-base font-bold text-[var(--color-text-primary)]">
                {(data.vetDelays.clinicRolloverRate * 100).toFixed(1)}%
              </span>
              {data.vetDelays.systemicUnderstaffingAlert ? (
                <Pill tone="danger" size="sm">
                  Understaffed
                </Pill>
              ) : data.vetDelays.veterinarians.some((v) => v.individualDelinquencyAlert) ? (
                <Pill tone="warning" size="sm">
                  Staff Delay Alert
                </Pill>
              ) : (
                <Pill tone="success" size="sm">
                  Stable
                </Pill>
              )}
            </div>
            <p className="text-[11px] text-[var(--color-text-secondary)] truncate">
              {data.vetDelays.totalRolloverEvents} rollover(s) recorded
            </p>
          </div>

          {/* Trainer Adherence */}
          <div className="p-2.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] space-y-1">
            <span className="text-[11px] text-[var(--color-text-muted)] uppercase tracking-wider font-medium">
              Trainer Adherence
            </span>
            <div className="flex items-center justify-between">
              <span className="text-base font-bold text-[var(--color-text-primary)]">
                {data.trainerEvaluation.overallAdherenceRate.toFixed(1)}%
              </span>
              <Pill
                tone={data.trainerEvaluation.overallAdherenceRate >= 80 ? 'success' : 'warning'}
                size="sm"
              >
                {data.trainerEvaluation.overallAdherenceRate >= 80 ? 'Optimal' : 'Needs Review'}
              </Pill>
            </div>
            <p className="text-[11px] text-[var(--color-text-secondary)] truncate">
              {data.trainerEvaluation.totalWorkoutsCompleted} / {data.trainerEvaluation.totalWorkoutsScheduled} ran
            </p>
          </div>

          {/* Quarantine Buffer */}
          <div className="p-2.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] space-y-1">
            <span className="text-[11px] text-[var(--color-text-muted)] uppercase tracking-wider font-medium">
              Quarantine Buffer
            </span>
            <div className="flex items-center justify-between">
              <span className="text-base font-bold text-[var(--color-text-primary)]">
                {data.stallOccupancy.inflowBufferMargin > 0
                  ? `+${data.stallOccupancy.inflowBufferMargin}`
                  : data.stallOccupancy.inflowBufferMargin}
              </span>
              <Pill
                tone={data.stallOccupancy.quarantineStressAlert ? 'danger' : 'success'}
                size="sm"
              >
                {data.stallOccupancy.quarantineStressAlert ? 'Buffer Alert' : 'Safe'}
              </Pill>
            </div>
            <p className="text-[11px] text-[var(--color-text-secondary)] truncate">
              {data.stallOccupancy.quarantineAvailable} avail vs. {data.stallOccupancy.awaitingArrivalAdmissions} inbound
            </p>
          </div>

          {/* Admission Pipeline Bottleneck */}
          <div className="p-2.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] space-y-1">
            <span className="text-[11px] text-[var(--color-text-muted)] uppercase tracking-wider font-medium">
              Intake Clearance
            </span>
            <div className="flex items-center justify-between">
              <span className="text-base font-bold text-[var(--color-text-primary)]">
                {data.admissionFunnel.approvalRate.toFixed(1)}%
              </span>
              <Pill
                tone={data.admissionFunnel.bottleneckStage !== 'NONE' ? 'warning' : 'success'}
                size="sm"
              >
                {data.admissionFunnel.bottleneckStage !== 'NONE'
                  ? formatAdmissionStage(data.admissionFunnel.bottleneckStage)
                  : 'Fast'}
              </Pill>
            </div>
            <p className="text-[11px] text-[var(--color-text-secondary)] truncate">
              {data.admissionFunnel.approvedCount} / {data.admissionFunnel.totalApplications} cleared
            </p>
          </div>
        </div>
      ) : null}
    </Panel>
  );
};
