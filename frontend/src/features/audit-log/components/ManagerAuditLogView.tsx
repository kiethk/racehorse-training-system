'use client';

import { TBody, THead, Table, Td, Th, Tr } from '@/components/ui/Table';
import { Notice } from '@/components/ui/Notice';
import { cn } from '@/lib/cn';
import { useEffect, useState, useMemo } from 'react';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { FilterBar } from '@/components/ui/FilterBar';
import { FormField } from '@/components/ui/FormField';
import { Input, SearchInput, Select } from '@/components/ui/Input';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { auditLogApi } from '../services/api';
import type { AuditLogItem, AuditLogFilters, PageResponse } from '../types';

const STATUS_OPTIONS = [
  { label: 'All Results', value: 'ALL' },
  { label: 'Success', value: '200' },
  { label: 'Failed / Rejected', value: '400' },
] as const;

const initialFilters: AuditLogFilters = {
  search: '',
  httpMethod: 'ALL',
  statusCode: 'ALL',
  from: '',
  to: '',
  page: 0,
  size: 20,
};

function formatRole(role: string | null): string {
  if (!role) return 'System';
  switch (role) {
    case 'CLUB_MANAGER': return 'Club Manager';
    case 'HEAD_TRAINER': return 'Head Trainer';
    case 'VETERINARIAN': return 'Veterinarian';
    case 'GROOM': return 'Groom';
    case 'HORSE_OWNER': return 'Horse Owner';
    default: return role;
  }
}

function formatAction(action: string | null): string {
  if (!action) return 'System Operation';
  if (action === 'CARE_SCHEDULE_AUTO_ASSIGNED') return 'Care Schedule Auto Assigned';
  if (action === 'CARE_SCHEDULE_RESCHEDULED') return 'Reschedule Medical Care';
  if (action.includes('_') && action === action.toUpperCase()) {
    return action
      .toLowerCase()
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
  }
  return action;
}

function formatDetails(log: AuditLogItem): string {
  if (log.details && log.details.trim().length > 0) {
    return log.details;
  }
  if (log.action) {
    const act = formatAction(log.action);
    return log.entityName ? `${act} on ${log.entityName}` : act;
  }
  return 'System processed operation successfully.';
}

function OutcomeText({ success }: { success: boolean }) {
  if (success) {
    return (
      <span className="font-semibold text-emerald-600 dark:text-emerald-400">
        Success
      </span>
    );
  }

  return (
    <span className="font-semibold text-rose-600 dark:text-rose-400">
      Failed
    </span>
  );
}

export function ManagerAuditLogView() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const urlFilters: AuditLogFilters = useMemo(() => {
    return {
      search: searchParams.get('search') || '',
      httpMethod: searchParams.get('httpMethod') || 'ALL',
      statusCode: searchParams.get('statusCode') || 'ALL',
      from: searchParams.get('from') || '',
      to: searchParams.get('to') || '',
      page: Number(searchParams.get('page')) || 0,
      size: Number(searchParams.get('size')) || 20,
    };
  }, [searchParams]);

  const [data, setData] = useState<PageResponse<AuditLogItem> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [draft, setDraft] = useState<AuditLogFilters>(urlFilters);

  useEffect(() => {
    let active = true;
    async function loadLogs() {
      try {
        setLoading(true);
        setError(null);
        const result = await auditLogApi.getAuditLogs(urlFilters);
        if (active) setData(result);
      } catch (err) {
        console.error('Failed to load audit logs:', err);
        if (active) setError('Failed to load activity logs. Please try again.');
      } finally {
        if (active) setLoading(false);
      }
    }
    loadLogs();
    return () => { active = false; };
  }, [urlFilters]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setDraft(urlFilters);
  }, [urlFilters]);

  const updateUrl = (filters: AuditLogFilters) => {
    const params = new URLSearchParams();
    if (filters.search) params.set('search', filters.search.trim());
    if (filters.httpMethod !== 'ALL') params.set('httpMethod', filters.httpMethod);
    if (filters.statusCode !== 'ALL') params.set('statusCode', filters.statusCode);
    if (filters.from) params.set('from', filters.from);
    if (filters.to) params.set('to', filters.to);
    if (filters.page > 0) params.set('page', String(filters.page));
    if (filters.size !== 20) params.set('size', String(filters.size));
    
    const qs = params.toString();
    router.push(qs ? `${pathname}?${qs}` : pathname);
  };

  const apply = () => {
    updateUrl({ ...draft, search: draft.search.trim(), page: 0 });
  };

  const clear = () => {
    setDraft(initialFilters);
    updateUrl(initialFilters);
  };

  const handlePageChange = (newPage: number) => {
    updateUrl({ ...urlFilters, page: newPage });
  };

  if (loading && !data) {
    return (
      <Panel>
        <ListSkeleton rows={10} />
      </Panel>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold tracking-tight text-[var(--color-text-primary)]">Activity & Audit Logs</h1>
        <p className="text-sm text-[var(--color-text-secondary)] mt-1">
          Review historical staff activities, operational decisions, and intake reviews across the club.
        </p>
      </div>

      <FilterBar
        className="[&_input]:min-h-9 [&_select]:min-h-9"
        onSubmit={(event) => { event.preventDefault(); apply(); }}
      >
        <FormField label="Search Activity or User" className="min-w-[15rem] flex-[1.6]">
          <SearchInput
            label="Search Activity or User"
            value={draft.search}
            onChange={(search) => setDraft({ ...draft, search })}
            placeholder="Search by person, action, or target ID..."
          />
        </FormField>
        <FormField label="Result Status" className="min-w-[9rem] flex-1">
          <Select value={draft.statusCode} onChange={(e) => setDraft({ ...draft, statusCode: e.target.value })}>
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="From Date" className="min-w-[9.5rem] flex-1">
          <Input type="date" value={draft.from} onChange={(e) => setDraft({ ...draft, from: e.target.value })} />
        </FormField>
        <FormField label="To Date" className="min-w-[9.5rem] flex-1">
          <Input type="date" min={draft.from || undefined} value={draft.to} onChange={(e) => setDraft({ ...draft, to: e.target.value })} />
        </FormField>
        <div className="flex shrink-0 items-center gap-2">
          <Button type="submit" variant="primary">Filter</Button>
          <Button type="button" variant="secondary" onClick={clear}>Reset</Button>
        </div>
      </FilterBar>

      {error && <Notice tone="error">{error}</Notice>}

      <Panel className="overflow-hidden">
        {(!data || data.content.length === 0) ? (
          <EmptyState 
            title="No activity logs found" 
            description="Try adjusting your search criteria or dates to view past events." 
            action={<Button size="sm" onClick={clear}>Clear filters</Button>} 
          />
        ) : (
          <div className="flex flex-col h-full">
            <Table bare className={loading ? 'whitespace-nowrap opacity-60 transition-opacity' : 'whitespace-nowrap transition-opacity'}>
              <THead>
                <Tr>
                  <Th className="px-6 py-4">Timestamp</Th>
                  <Th className="px-6 py-4">Actor</Th>
                  <Th className="px-6 py-4">Role</Th>
                  <Th className="px-6 py-4">Activity</Th>
                  <Th className="px-6 py-4 min-w-[20rem]">Activity Summary</Th>
                  <Th className="px-6 py-4 text-center">Outcome</Th>
                </Tr>
              </THead>
              <TBody>
                {data.content.map((log) => {
                  const displayAction = formatAction(log.action);
                  const displayDetails = formatDetails(log);

                  return (
                    <Tr key={log.id} className="hover:bg-[var(--color-surface-muted)] transition-colors">
                      <Td className="px-6 py-4 text-[var(--color-text-secondary)] whitespace-nowrap text-xs font-mono">
                        {new Date(log.createdAt).toLocaleString('en-GB', { 
                          day: '2-digit', month: 'short', year: 'numeric',
                          hour: '2-digit', minute: '2-digit'
                        })}
                      </Td>
                      <Td className="px-6 py-4">
                        {log.actorName ? (
                          <div>
                            <span className="font-semibold block text-[var(--color-text-primary)] text-sm">{log.actorName}</span>
                            {log.actorEmail && <span className="text-xs text-[var(--color-text-muted)]">{log.actorEmail}</span>}
                          </div>
                        ) : (
                          <span className="text-[var(--color-text-secondary)] italic text-sm">System / Guest</span>
                        )}
                      </Td>
                      <Td className="px-6 py-4">
                        <span className="text-sm text-[var(--color-text-secondary)] font-medium">
                          {formatRole(log.actorRole)}
                        </span>
                      </Td>
                      <Td className="px-6 py-4">
                        <span className="font-semibold text-sm text-[var(--color-text-primary)]">
                          {displayAction}
                        </span>
                      </Td>
                      <Td className="px-6 py-4">
                        <div className="max-w-md lg:max-w-xl">
                          <p className="text-sm text-[var(--color-text-primary)] leading-snug line-clamp-2" title={displayDetails}>
                            {displayDetails}
                          </p>
                          {log.entityName && (
                            <div className="mt-1 flex items-center gap-1.5 text-xs text-[var(--color-text-muted)]">
                              <span>Target:</span>
                              <span className="font-medium text-[var(--color-text-secondary)]">{log.entityName}</span>
                              {log.entityId && (
                                <span className="bg-[var(--color-surface-muted)] px-1.5 py-0.2 rounded font-mono text-[11px]">
                                  #{log.entityId}
                                </span>
                              )}
                            </div>
                          )}
                        </div>
                      </Td>
                      <Td className="px-6 py-4 text-center">
                        <OutcomeText success={log.success} />
                      </Td>
                    </Tr>
                  );
                })}
              </TBody>
            </Table>
            
            {/* Pagination footer */}
            <div className="flex items-center justify-between border-t border-[var(--color-border)] px-6 py-3">
              <div className="text-sm text-[var(--color-text-secondary)]">
                Showing <span className="font-medium text-[var(--color-text-primary)]">{data.pageable.pageNumber * data.pageable.pageSize + 1}</span> to <span className="font-medium text-[var(--color-text-primary)]">{Math.min((data.pageable.pageNumber + 1) * data.pageable.pageSize, data.totalElements)}</span> of <span className="font-medium text-[var(--color-text-primary)]">{data.totalElements}</span> activities
              </div>
              <div className="flex items-center gap-2">
                <Button 
                  variant="secondary" 
                  size="sm" 
                  disabled={data.first || loading} 
                  onClick={() => handlePageChange(data.pageable.pageNumber - 1)}
                >
                  Previous
                </Button>
                <Button 
                  variant="secondary" 
                  size="sm" 
                  disabled={data.last || loading} 
                  onClick={() => handlePageChange(data.pageable.pageNumber + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          </div>
        )}
      </Panel>
    </div>
  );
}
