'use client';

import { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import type { AdmissionSummaryResponse } from '../types';
import { AdmissionStatusBadge } from '../shared/components/AdmissionStatusBadge';

type Tab = 'PENDING' | 'REVIEWED';

interface TrainerQueueFilters {
  tab: Tab;
  candidateName: string;
  submittedFrom: string;
  submittedTo: string;
}

export function TrainerAdmissionsListView() {
  const { user } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const urlFilters: TrainerQueueFilters = useMemo(() => {
    const rawTab = searchParams.get('tab') as Tab | null;
    const safeTab: Tab = rawTab === 'REVIEWED' ? 'REVIEWED' : 'PENDING';
    return {
      tab: safeTab,
      candidateName: searchParams.get('candidateName') || '',
      submittedFrom: searchParams.get('submittedFrom') || '',
      submittedTo: searchParams.get('submittedTo') || '',
    };
  }, [searchParams]);

  const [admissions, setAdmissions] = useState<AdmissionSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [draft, setDraft] = useState<TrainerQueueFilters>(urlFilters);

  useEffect(() => {
    let active = true;
    async function loadQueue() {
      try {
        setLoading(true);
        setError(null);
        const data = await trainerAdmissionsApi.getAll();
        if (active) setAdmissions(data);
      } catch (err) {
        console.error('Failed to load admissions:', err);
        if (active) setError('Failed to load admission applications. Please try again.');
      } finally {
        if (active) setLoading(false);
      }
    }
    loadQueue();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setDraft(urlFilters);
  }, [urlFilters]);

  // Counts by review status
  const pendingCount = useMemo(
    () => admissions.filter((a) => a.status === 'TRAINER_REVIEW').length,
    [admissions],
  );

  const reviewedCount = useMemo(
    () =>
      admissions.filter(
        (a) => a.trainerReviewedAt !== null && a.trainerId === user?.userId,
      ).length,
    [admissions, user?.userId],
  );

  // Filter admissions list
  const filteredAdmissions = useMemo(() => {
    return admissions.filter((a) => {
      // Filter by review type
      if (urlFilters.tab === 'PENDING') {
        if (a.status !== 'TRAINER_REVIEW') return false;
      } else if (urlFilters.tab === 'REVIEWED') {
        if (!a.trainerReviewedAt || a.trainerId !== user?.userId) return false;
      }

      // Filter by horse name
      if (
        urlFilters.candidateName &&
        !a.candidateName.toLowerCase().includes(urlFilters.candidateName.toLowerCase())
      ) {
        return false;
      }

      // Filter by submission date
      if (urlFilters.submittedFrom || urlFilters.submittedTo) {
        const submittedTime = new Date(a.submittedAt).getTime();
        if (urlFilters.submittedFrom) {
          const fromTime = new Date(urlFilters.submittedFrom).getTime();
          if (submittedTime < fromTime) return false;
        }
        if (urlFilters.submittedTo) {
          const toTime = new Date(urlFilters.submittedTo).getTime() + 24 * 60 * 60 * 1000 - 1;
          if (submittedTime > toTime) return false;
        }
      }

      return true;
    });
  }, [admissions, urlFilters, user?.userId]);

  const updateUrl = (filters: TrainerQueueFilters) => {
    const params = new URLSearchParams();
    if (filters.tab !== 'PENDING') params.set('tab', filters.tab);
    if (filters.candidateName) params.set('candidateName', filters.candidateName);
    if (filters.submittedFrom) params.set('submittedFrom', filters.submittedFrom);
    if (filters.submittedTo) params.set('submittedTo', filters.submittedTo);
    const qs = params.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname);
  };

  const handleTabChange = (nextTab: Tab) => {
    const next = { ...draft, tab: nextTab };
    setDraft(next);
    updateUrl(next);
  };

  const applyFilters = (e: React.FormEvent) => {
    e.preventDefault();
    updateUrl(draft);
  };

  const clearFilters = () => {
    const reset: TrainerQueueFilters = {
      tab: draft.tab,
      candidateName: '',
      submittedFrom: '',
      submittedTo: '',
    };
    setDraft(reset);
    updateUrl(reset);
  };

  const detailQuery = useMemo(() => {
    const p = new URLSearchParams();
    if (urlFilters.tab !== 'PENDING') p.set('tab', urlFilters.tab);
    if (urlFilters.candidateName) p.set('candidateName', urlFilters.candidateName);
    if (urlFilters.submittedFrom) p.set('submittedFrom', urlFilters.submittedFrom);
    if (urlFilters.submittedTo) p.set('submittedTo', urlFilters.submittedTo);
    const s = p.toString();
    return s ? `?${s}` : '';
  }, [urlFilters]);

  if (loading) return <ListSkeleton rows={6} />;

  return (
    <div className="space-y-5">
      {/* Header */}
      <div>
        <h1 className="text-[20px] font-bold tracking-tight text-[var(--color-text-primary)]">
          Horse Admissions
        </h1>
        <p className="text-[13px] text-[var(--color-text-secondary)]">
          Assess racing potential and readiness of candidate horses in the quarantine facility.
        </p>
      </div>

      {/* Filter Toolbar (Merged review type filter) */}
      <form
        onSubmit={applyFilters}
        className="grid grid-cols-1 gap-3 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 xl:items-end"
      >
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Application Type
          <select
            value={draft.tab}
            onChange={(e) => handleTabChange(e.target.value as Tab)}
            className="mt-1 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          >
            <option value="PENDING">Pending Evaluation ({pendingCount})</option>
            <option value="REVIEWED">Evaluated by You ({reviewedCount})</option>
          </select>
        </label>

        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Candidate Horse Name
          <div className="relative mt-1">
            <input
              type="text"
              placeholder="Search horse name..."
              value={draft.candidateName}
              onChange={(e) => setDraft({ ...draft, candidateName: e.target.value })}
              className="h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] pl-8 pr-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
            />
            <Icon
              name="search"
              size={14}
              className="absolute left-2.5 top-2.5 text-[var(--color-text-muted)]"
            />
          </div>
        </label>

        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Submitted From
          <input
            type="date"
            value={draft.submittedFrom}
            onChange={(e) => setDraft({ ...draft, submittedFrom: e.target.value })}
            className="mt-1 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          />
        </label>

        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Submitted To
          <input
            type="date"
            min={draft.submittedFrom || undefined}
            value={draft.submittedTo}
            onChange={(e) => setDraft({ ...draft, submittedTo: e.target.value })}
            className="mt-1 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          />
        </label>

        <div className="flex items-center gap-2">
          <Button type="submit" variant="primary" size="sm">
            Apply
          </Button>
          <Button type="button" variant="secondary" size="sm" onClick={clearFilters}>
            Reset
          </Button>
        </div>
      </form>

      {error && (
        <div
          role="alert"
          className="border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]"
        >
          {error}
        </div>
      )}

      {/* Admissions Table */}
      <Panel className="overflow-hidden">
        {filteredAdmissions.length === 0 ? (
          <EmptyState
            title="No admission records found"
            description="Try adjusting your search filters or switch the application type."
            action={<Button size="sm" onClick={clearFilters}>Reset Filters</Button>}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm whitespace-nowrap">
              <thead className="border-b border-[var(--color-border)] text-xs font-medium uppercase tracking-wider text-[var(--color-text-muted)]">
                <tr>
                  <th className="px-6 py-3.5">Candidate Horse</th>
                  <th className="px-6 py-3.5">Status</th>
                  <th className="px-6 py-3.5">Quarantine Stall</th>
                  <th className="px-6 py-3.5">Submitted Date</th>
                  {draft.tab === 'REVIEWED' && (
                    <th className="px-6 py-3.5">Evaluated Date</th>
                  )}
                  <th className="px-6 py-3.5 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--color-border)] text-[var(--color-text-primary)]">
                {filteredAdmissions.map((item) => (
                  <tr
                    key={item.admissionId}
                    className="transition-colors hover:bg-[var(--color-surface-muted)]"
                  >
                    <td className="px-6 py-3.5">
                      <div className="flex items-center gap-3">
                        <HorseAvatar
                          name={item.candidateName}
                          image={item.imageUrl}
                          size={32}
                          rounded="md"
                        />
                        <div>
                          <span className="font-semibold block">{item.candidateName}</span>
                          <span className="text-[11px] text-[var(--color-text-muted)]">
                            {item.breed || 'Unknown breed'}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-3.5">
                      <AdmissionStatusBadge status={item.status} />
                    </td>
                    <td className="px-6 py-3.5 text-[12px] text-[var(--color-text-secondary)]">
                      {item.quarantineStallCode ? (
                        <span className="inline-flex items-center gap-1 font-medium text-[var(--color-text-primary)]">
                          Stall {item.quarantineStallCode}
                        </span>
                      ) : (
                        <span className="text-[var(--color-text-muted)] italic">Unassigned</span>
                      )}
                    </td>
                    <td className="px-6 py-3.5 text-[12px] text-[var(--color-text-secondary)]">
                      {new Date(item.submittedAt).toLocaleDateString('en-US', {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                      })}
                    </td>
                    {draft.tab === 'REVIEWED' && (
                      <td className="px-6 py-3.5 text-[12px] text-[var(--color-text-secondary)]">
                        {item.trainerReviewedAt
                          ? new Date(item.trainerReviewedAt).toLocaleDateString('en-US', {
                              year: 'numeric',
                              month: 'short',
                              day: 'numeric',
                            })
                          : '—'}
                      </td>
                    )}
                    <td className="px-6 py-3.5 text-right">
                      <Link
                        href={`/trainer/admissions/${item.admissionId}${detailQuery}`}
                        className={`inline-flex items-center justify-center rounded-[var(--radius-sm)] px-4 py-1.5 text-xs font-semibold transition-colors ${
                          item.status === 'TRAINER_REVIEW'
                            ? 'bg-[var(--color-primary)] text-[var(--color-text-inverse)] hover:opacity-90'
                            : 'bg-[var(--color-primary-soft)] text-[var(--color-primary)] hover:bg-[var(--color-primary-subtle)]'
                        }`}
                      >
                        {item.status === 'TRAINER_REVIEW' ? 'Evaluate' : 'View'}
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </div>
  );
}
