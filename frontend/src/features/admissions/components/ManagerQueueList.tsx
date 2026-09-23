'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { admissionsApi } from '../services/api';
import type { AdmissionSummaryResponse } from '../types';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { MetricCard } from '@/components/ui/MetricCard';

export function ManagerQueueList() {
  const [admissions, setAdmissions] = useState<AdmissionSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadQueue() {
      try {
        setLoading(true);
        setError(null);
        const data = await admissionsApi.getManagerReviewQueue();
        setAdmissions(data);
      } catch (err) {
        console.error('Failed to load manager queue:', err);
        setError('Failed to load admissions queue. Please try again.');
      } finally {
        setLoading(false);
      }
    }
    loadQueue();
  }, []);

  if (loading) {
    return (
      <Panel>
        <ListSkeleton rows={4} />
      </Panel>
    );
  }

  if (error) {
    return (
      <Panel>
        <EmptyState
          icon="alert-triangle"
          title="Error loading queue"
          description={error}
          action={
            <Button variant="secondary" onClick={() => window.location.reload()}>
              Retry
            </Button>
          }
        />
      </Panel>
    );
  }

  if (admissions.length === 0) {
    return (
      <Panel>
        <EmptyState
          icon="check"
          title="All caught up!"
          description="There are no admissions waiting for manager review."
        />
      </Panel>
    );
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <MetricCard 
          label="Awaiting my review" 
          value={admissions.length} 
          unit="applications" 
          icon="clock" 
          tone="warning" 
        />
      </div>

      <Panel className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-[var(--color-border)] px-4 py-3">
          <SectionTitle>Admissions requiring final approval</SectionTitle>
          <span className="text-[11px] font-medium text-[var(--color-text-muted)]">
            {admissions.length} open
          </span>
        </div>
        <ul className="divide-y divide-[var(--color-border)]">
          {admissions.map((admission) => (
            <li key={admission.admissionId}>
              <div className="flex w-full items-center gap-3 px-4 py-3 hover:bg-[var(--color-surface-subtle)] transition-colors">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-[var(--radius-sm)] bg-[var(--color-surface-muted)] text-[var(--color-text-muted)]">
                  <Icon name="clipboard" size={20} />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="truncate text-[13px] font-medium text-[var(--color-text-primary)]">
                    {admission.candidateName}
                  </div>
                  <div className="truncate text-[11px] text-[var(--color-text-muted)]">
                    {admission.breed} · Submitted {new Date(admission.submittedAt).toLocaleDateString()}
                  </div>
                </div>
                <div className="hidden sm:block">
                  <Pill tone="warning" size="sm">
                    MANAGER REVIEW
                  </Pill>
                </div>
                <div className="ml-4 shrink-0">
                  <Link href={`/manager/admissions/${admission.admissionId}`} passHref legacyBehavior>
                    <Button variant="primary" size="sm">
                      Review
                    </Button>
                  </Link>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </Panel>
    </div>
  );
}
