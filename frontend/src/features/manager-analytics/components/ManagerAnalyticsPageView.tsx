'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import {
  ScreenLayout,
  PageHeader,
  Panel,
  Button,
  Icon,
  Notice,
  Skeleton,
} from '@/components/ui';
import { managerEdaService } from '../services/managerEdaService';
import type { ManagerEdaOverviewResponse } from '../types/managerEda';
import { VetDelaysEdaView } from './VetDelaysEdaView';
import { TrainerEvaluationEdaView } from './TrainerEvaluationEdaView';
import { StallOccupancyEdaView } from './StallOccupancyEdaView';
import { AdmissionFunnelEdaView } from './AdmissionFunnelEdaView';

type TabKey = 'vets' | 'trainers' | 'occupancy' | 'admissions';

export const ManagerAnalyticsPageView: React.FC = () => {
  const [days, setDays] = useState<number>(90);
  const [activeTab, setActiveTab] = useState<TabKey>('vets');
  const [data, setData] = useState<ManagerEdaOverviewResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async (isRefresh = false) => {
    try {
      if (isRefresh) setRefreshing(true);
      else setLoading(true);
      setError(null);
      const res = await managerEdaService.getOverview(days);
      setData(res);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      setError(err?.message || 'Unable to load executive EDA analytics.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [days]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  const tabs: { key: TabKey; label: string }[] = [
    { key: 'vets', label: 'Veterinary Delays & Rollovers' },
    { key: 'trainers', label: 'Trainer Evaluation & Progress' },
    { key: 'occupancy', label: 'Barn Occupancy & Quarantine' },
    { key: 'admissions', label: 'Herd Intake & Admission Funnel' },
  ];

  return (
    <ScreenLayout variant="dashboard">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <PageHeader
            title="Operations & Performance Intelligence"
          />

          <div className="flex items-center gap-2 self-start sm:self-center shrink-0">
            {/* Time Window Selector */}
            <div className="inline-flex rounded-lg border border-[var(--color-border)] p-0.5 bg-[var(--color-surface)]">
              {[30, 90, 180, 365].map((d) => (
                // eslint-disable-next-line no-restricted-syntax
                <button
                  key={d}
                  type="button"
                  onClick={() => setDays(d)}
                  className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${
                    days === d
                      ? 'bg-[var(--color-primary)] text-white'
                      : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
                  }`}
                >
                  {d}d
                </button>
              ))}
            </div>

            <Button
              size="sm"
              variant="secondary"
              icon="refresh"
              loading={refreshing}
              onClick={() => loadData(true)}
            >
              Refresh
            </Button>
          </div>
        </div>

      {error && <Notice tone="error">{error}</Notice>}

      {/* Primary Navigation Tabs */}
      <div className="border-b border-[var(--color-border)] flex overflow-x-auto gap-2">
        {tabs.map((tab) => {
          const isActive = activeTab === tab.key;
          return (
            // eslint-disable-next-line no-restricted-syntax
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key)}
              className={`flex items-center gap-2 py-2.5 px-3 border-b-2 text-xs font-semibold whitespace-nowrap transition-colors ${
                isActive
                  ? 'border-[var(--color-primary)] text-[var(--color-primary)]'
                  : 'border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:border-[var(--color-border)]'
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Main Tab Content */}
      {loading && !data ? (
        <div className="space-y-4">
          <Skeleton className="h-24 w-full rounded-md" />
          <Skeleton className="h-64 w-full rounded-md" />
          <Skeleton className="h-48 w-full rounded-md" />
        </div>
      ) : data ? (
        <div>
          {activeTab === 'vets' && <VetDelaysEdaView data={data.vetDelays} />}
          {activeTab === 'trainers' && <TrainerEvaluationEdaView data={data.trainerEvaluation} />}
          {activeTab === 'occupancy' && <StallOccupancyEdaView data={data.stallOccupancy} />}
          {activeTab === 'admissions' && <AdmissionFunnelEdaView data={data.admissionFunnel} />}
        </div>
      ) : null}
    </ScreenLayout>
  );
};
