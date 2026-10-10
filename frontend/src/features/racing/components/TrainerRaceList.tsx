'use client';

import { TBody, THead, Table, Td, Th, Tr } from '@/components/ui/Table';
import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { Button, LinkButton } from '@/components/ui/Button';
import { SearchInput } from '@/components/ui/Input';
import { Tabs } from '@/components/ui/Tabs';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { PageHeader } from '@/components/ui/PageHeader';
import { ScreenLayout } from '@/components/ui/ScreenLayout';
import { displayError } from '@/lib/display';
import { racingService } from '../services/racingService';
import type { RaceRegistrationResponse, RaceRegistrationStatus } from '../types';

type FilterTab = 'ALL' | RaceRegistrationStatus;

export function TrainerRaceList() {
  const [items, setItems] = useState<RaceRegistrationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<FilterTab>('ALL');
  const [search, setSearch] = useState('');

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await racingService.listMine();
      setItems(data);
    } catch (err) {
      console.error('Failed to load race nominations:', err);
      setError(displayError(err, 'Unable to load race nominations.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  const counts = useMemo(() => {
    return {
      ALL: items.length,
      PENDING: items.filter((i) => i.status === 'PENDING').length,
      APPROVED: items.filter((i) => i.status === 'APPROVED').length,
      REJECTED: items.filter((i) => i.status === 'REJECTED').length,
    };
  }, [items]);

  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      if (activeTab !== 'ALL' && item.status !== activeTab) {
        return false;
      }
      if (search.trim()) {
        const q = search.toLowerCase();
        const matchRace = item.raceName.toLowerCase().includes(q);
        const matchCategory = item.raceCategory.toLowerCase().includes(q);
        const matchHorse = (item.horseName || '').toLowerCase().includes(q);
        const matchLoc = item.location.toLowerCase().includes(q);
        return matchRace || matchCategory || matchHorse || matchLoc;
      }
      return true;
    });
  }, [items, activeTab, search]);

  const renderStatusBadge = (status: RaceRegistrationStatus) => {
    switch (status) {
      case 'PENDING':
        return <Pill tone="warning" size="sm">Pending</Pill>;
      case 'APPROVED':
        return <Pill tone="success" size="sm">Approved</Pill>;
      case 'REJECTED':
        return <Pill tone="danger" size="sm">Rejected</Pill>;
      default:
        return <Pill tone="neutral" size="sm">{status}</Pill>;
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

  return (
    <ScreenLayout variant="list">
      <PageHeader
        title="Race nominations"
        description="Research races and submit internal nominations for management review."
        actions={<LinkButton href="/trainer/racing/new" variant="primary" icon="plus">Create nomination</LinkButton>}
      />

      {/* Tabs & Search */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <Tabs
          size="sm"
          active={activeTab}
          onChange={(id) => setActiveTab(id as typeof activeTab)}
          tabs={[
            { id: 'ALL', label: 'All', count: counts.ALL },
            { id: 'PENDING', label: 'Pending', count: counts.PENDING },
            { id: 'APPROVED', label: 'Approved', count: counts.APPROVED },
            { id: 'REJECTED', label: 'Rejected', count: counts.REJECTED },
          ]}
        />

        <SearchInput
          className="w-full sm:w-72"
          value={search}
          onChange={setSearch}
          placeholder="Search races, horses, or locations…"
        />
      </div>

      {/* Content */}
      {loading ? (
        <Panel padded>
          <ListSkeleton rows={5} />
        </Panel>
      ) : error ? (
        <Panel padded>
          <EmptyState
            icon="alert-triangle"
            title="Unable to load nominations"
            description={error}
            action={
              <Button variant="secondary" size="sm" onClick={loadData}>
            Try again
              </Button>
            }
          />
        </Panel>
      ) : items.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="clipboard"
            title="You have not submitted any nominations"
            description="Research a race and submit a nomination for management review."
            action={
              <Link href="/trainer/racing/new">
                <Button variant="primary" size="sm">
            Create nomination
                </Button>
              </Link>
            }
          />
        </Panel>
      ) : filteredItems.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="search"
            title="No matching nominations found"
            description="Try changing the filters or search terms."
          />
        </Panel>
      ) : (
        <Table>
              <THead>
                <Tr>
                <Th className="px-4 py-3">Race & Category</Th>
                <Th className="px-4 py-3">Horse</Th>
                <Th className="px-4 py-3">Event Date & Location</Th>
                <Th className="px-4 py-3">Status</Th>
                <Th className="px-4 py-3">Submitted</Th>
                <Th className="px-4 py-3 text-right">Actions</Th>
                </Tr>
              </THead>
              <TBody>
                {filteredItems.map((item) => (
                  <Tr key={item.id} className="hover:bg-[var(--color-surface-muted)]/50 transition">
                    <Td className="px-4 py-3.5">
                      <div className="font-semibold text-[var(--color-text-primary)]">
                        {item.raceName}
                      </div>
                      <div className="text-xs text-[var(--color-text-secondary)]">
                        {item.raceCategory}
                      </div>
                    </Td>
                    <Td className="px-4 py-3.5">
                      <div className="font-medium text-[var(--color-text-primary)] flex items-center gap-1.5">
                        <span>🏇</span>
                      <span>{item.horseName || `Horse #${item.horseId}`}</span>
                      </div>
                      {item.horseRegistrationNumber && (
                        <div className="text-xs text-[var(--color-text-muted)] font-metric">
                          {item.horseRegistrationNumber}
                        </div>
                      )}
                    </Td>
                    <Td className="px-4 py-3.5">
                      <div className="font-medium text-[var(--color-text-primary)]">
                        📅 {formatDate(item.eventDate)}
                        {item.eventTime && ` • ${item.eventTime.substring(0, 5)}`}
                      </div>
                      <div className="text-xs text-[var(--color-text-secondary)] truncate max-w-xs" title={item.location}>
                        📍 {item.location}
                      </div>
                    </Td>
                    <Td className="px-4 py-3.5">
                      {renderStatusBadge(item.status)}
                    </Td>
                    <Td className="px-4 py-3.5 text-xs text-[var(--color-text-muted)]">
                      {formatDateTime(item.createdAt)}
                    </Td>
                    <Td className="px-4 py-3.5 text-right">
                      <Link href={`/trainer/racing/${item.id}`}>
                        <Button variant="secondary" size="sm">
                        View nomination
                        </Button>
                      </Link>
                    </Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
      )}
    </ScreenLayout>
  );
}
