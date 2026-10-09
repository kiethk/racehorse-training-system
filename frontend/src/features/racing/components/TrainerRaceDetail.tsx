'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { DetailSkeleton, EmptyState } from '@/components/ui/states';
import { displayError } from '@/lib/display';
import { racingService } from '../services/racingService';
import type { RaceRegistrationResponse, RaceRegistrationStatus } from '../types';

interface TrainerRaceDetailProps {
  id: number;
}

export function TrainerRaceDetail({ id }: TrainerRaceDetailProps) {
  const [data, setData] = useState<RaceRegistrationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchDetail() {
      try {
        setLoading(true);
        setError(null);
        const res = await racingService.getMine(id);
        setData(res);
      } catch (err) {
        console.error('Failed to load nomination details:', err);
        setError(displayError(err, 'Nomination not found or you do not have permission to view it.'));
      } finally {
        setLoading(false);
      }
    }
    fetchDetail();
  }, [id]);

  const renderStatusBadge = (status: RaceRegistrationStatus) => {
    switch (status) {
      case 'PENDING':
        return <Pill tone="warning" size="md">Pending</Pill>;
      case 'APPROVED':
        return <Pill tone="success" size="md">Approved</Pill>;
      case 'REJECTED':
        return <Pill tone="danger" size="md">Rejected</Pill>;
      default:
        return <Pill tone="neutral" size="md">{status}</Pill>;
    }
  };

  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return '—';
    try {
      const [y, m, d] = dateStr.split('-');
      if (y && m && d) return `${d}/${m}/${y}`;
      return new Date(dateStr).toLocaleDateString('en-GB');
    } catch {
      return dateStr;
    }
  };

  const formatDateTime = (dtStr?: string | null) => {
    if (!dtStr) return '—';
    try {
      const dt = new Date(dtStr);
      return `${dt.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })} ${dt.toLocaleDateString('en-GB')}`;
    } catch {
      return dtStr;
    }
  };

  if (loading) {
    return (
      <Panel padded>
        <DetailSkeleton />
      </Panel>
    );
  }

  if (error || !data) {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Unable to display nomination"
          description={error || 'This nomination does not exist or has been removed.'}
          action={
            <Link href="/trainer/racing">
              <Button variant="secondary" size="sm">
                ← Back to list
              </Button>
            </Link>
          }
        />
      </Panel>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-[var(--color-border)] pb-4">
        <div>
          <Link
            href="/trainer/racing"
            className="inline-flex items-center text-[12px] text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition mb-1"
          >
            ← Race nominations
          </Link>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-[20px] font-bold text-[var(--color-text-primary)]">
              {data.raceName}
            </h1>
            {renderStatusBadge(data.status)}
          </div>
          <p className="mt-0.5 text-[13px] text-[var(--color-text-secondary)]">
            Category: <strong className="text-[var(--color-text-primary)]">{data.raceCategory}</strong> • Submitted: {formatDateTime(data.createdAt)}
          </p>
        </div>

        <Link href="/trainer/racing">
          <Button variant="secondary">
            Back to list
          </Button>
        </Link>
      </div>

      {/* Source warning */}
      <div className="rounded-[var(--radius-md)] border border-[var(--color-info)] bg-[var(--color-info-soft)] p-3 text-[12px] text-[var(--color-info)] flex items-center gap-2">
        <span>ℹ️</span>
        <span>
          <strong>Note:</strong> The trainer provided the race information. Management will verify it before approval.
        </span>
      </div>

      {/* Manager feedback block (if already reviewed) */}
      {data.status !== 'PENDING' && (
        <div
          className={`rounded-[var(--radius-md)] border p-4 text-[13px] ${
            data.status === 'APPROVED'
              ? 'border-[var(--color-success)] bg-[var(--color-success-soft)] text-[var(--color-success)]'
              : 'border-[var(--color-danger)] bg-[var(--color-danger-soft)] text-[var(--color-danger)]'
          }`}
        >
          <div className="font-bold flex items-center gap-2 mb-1">
            <span>{data.status === 'APPROVED' ? '✅' : '❌'}</span>
            <span>
              Management feedback ({data.status === 'APPROVED' ? 'Approved' : 'Nomination rejected'})
            </span>
          </div>
          {data.reviewedAt && (
            <div className="text-[12px] opacity-80 mb-2">
              Reviewed: {formatDateTime(data.reviewedAt)}
            </div>
          )}
          <div className="rounded bg-[var(--color-surface)]/80 p-3 border border-current/20 text-[var(--color-text-primary)]">
            {data.managerFeedback || 'No additional comments.'}
          </div>
        </div>
      )}

      {/* Two-column grid: horse & race information */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Column 1: horse */}
        <div className="space-y-6">
          <Panel padded>
            <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-3 flex items-center gap-2 border-b border-[var(--color-border)] pb-2">
              Nominated horse
            </h2>

            <div className="space-y-3 text-[13px]">
              <div>
                <span className="text-[var(--color-text-secondary)]">Horse:</span>{' '}
                <strong className="text-[var(--color-text-primary)] text-[15px]">
                  {data.horseName || `Horse #${data.horseId}`}
                </strong>
              </div>

              {data.horseRegistrationNumber && (
                <div>
                  <span className="text-[var(--color-text-secondary)]">UELN / Registration number:</span>{' '}
                  <span className="font-metric font-medium text-[var(--color-text-primary)]">
                    {data.horseRegistrationNumber}
                  </span>
                </div>
              )}

              <div className="mt-3 pt-3 border-t border-[var(--color-border)]">
                <span className="text-[var(--color-text-secondary)] block font-medium mb-1">
                  Trainer assessment / Nomination reason:
                </span>
                <p className="whitespace-pre-wrap rounded bg-[var(--color-surface-muted)] p-3 text-[var(--color-text-primary)] border border-[var(--color-border)]">
                  {data.selectionReason}
                </p>
              </div>
            </div>
          </Panel>

          {/* Column 1: reference information */}
          <Panel padded>
            <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-3 flex items-center gap-2 border-b border-[var(--color-border)] pb-2">
              Technical details & Notes
            </h2>

            <div className="space-y-3 text-[13px]">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <span className="text-[var(--color-text-secondary)] block">Distance:</span>
                  <span className="font-medium text-[var(--color-text-primary)]">
                    {data.distanceMeters ? `${data.distanceMeters.toLocaleString()} m` : '—'}
                  </span>
                </div>
                <div>
                  <span className="text-[var(--color-text-secondary)] block">Track surface:</span>
                  <span className="font-medium text-[var(--color-text-primary)]">
                    {data.trackType || '—'}
                  </span>
                </div>
              </div>

              {data.prizeDetails && (
                <div className="pt-2 border-t border-[var(--color-border)]">
                  <span className="text-[var(--color-text-secondary)] block font-medium mb-1">
                    Prize structure:
                  </span>
                  <p className="whitespace-pre-wrap rounded bg-[var(--color-surface-muted)] p-2.5 text-[var(--color-text-primary)] border border-[var(--color-border)]">
                    {data.prizeDetails}
                  </p>
                </div>
              )}

              {data.trainerNotes && (
                <div className="pt-2 border-t border-[var(--color-border)]">
                  <span className="text-[var(--color-text-secondary)] block font-medium mb-1">
                    Notes / Entry requirements / Logistics:
                  </span>
                  <p className="whitespace-pre-wrap rounded bg-[var(--color-surface-muted)] p-2.5 text-[var(--color-text-primary)] border border-[var(--color-border)]">
                    {data.trainerNotes}
                  </p>
                </div>
              )}
            </div>
          </Panel>
        </div>

        {/* Column 2: race details */}
        <div className="space-y-6">
          <Panel padded>
            <h2 className="text-[15px] font-semibold text-[var(--color-text-primary)] mb-3 flex items-center gap-2 border-b border-[var(--color-border)] pb-2">
              Race / Event details
            </h2>

            <div className="space-y-3 text-[13px]">
              <div>
                <span className="text-[var(--color-text-secondary)] block">Event:</span>
                <span className="font-semibold text-[var(--color-text-primary)] text-[14px]">
                  {data.raceName}
                </span>
              </div>

              <div>
                <span className="text-[var(--color-text-secondary)] block">Category / Stage:</span>
                <span className="font-medium text-[var(--color-text-primary)]">
                  {data.raceCategory}
                </span>
              </div>

              <div>
                <span className="text-[var(--color-text-secondary)] block">Racecourse & Location:</span>
                <span className="font-medium text-[var(--color-text-primary)]">
                  📍 {data.location}
                </span>
              </div>

              <div className="grid grid-cols-1 gap-4 border-t border-[var(--color-border)] pt-2 sm:grid-cols-2">
                <div>
                  <span className="text-[var(--color-text-secondary)] block">Event date:</span>
                  <span className="font-medium text-[var(--color-text-primary)]">
                    📅 {formatDate(data.eventDate)}
                  </span>
                </div>
                <div>
                  <span className="text-[var(--color-text-secondary)] block">Start time:</span>
                  <span className="font-medium text-[var(--color-text-primary)]">
                    {data.eventTime ? data.eventTime.substring(0, 5) : 'Not announced'}
                  </span>
                </div>
              </div>

              <div className="pt-2 border-t border-[var(--color-border)]">
                <span className="text-[var(--color-text-secondary)] block">Organizer:</span>
                <span className="font-medium text-[var(--color-text-primary)]">
                  {data.organizer || '—'}
                </span>
              </div>

              <div>
                <span className="text-[var(--color-text-secondary)] block">Official nomination deadline:</span>
                <span className="font-medium text-[var(--color-text-primary)]">
                  {data.nominationDeadline ? formatDate(data.nominationDeadline) : '—'}
                </span>
              </div>

              {data.sourceUrl && (
                <div className="pt-2 border-t border-[var(--color-border)]">
                  <span className="text-[var(--color-text-secondary)] block font-medium mb-1">
                    Rules / Official URL:
                  </span>
                  <a
                    href={data.sourceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-[var(--color-primary)] hover:underline break-all text-[12px] inline-flex items-center gap-1"
                  >
                    <span>🔗 {data.sourceUrl}</span>
                    <span className="text-[10px]">↗</span>
                  </a>
                </div>
              )}
            </div>
          </Panel>
        </div>
      </div>
    </div>
  );
}
