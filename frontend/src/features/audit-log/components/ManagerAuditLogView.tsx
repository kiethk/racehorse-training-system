'use client';

import React, { useEffect, useState, useMemo } from 'react';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { auditLogApi } from '../services/api';
import type { AuditLogItem, AuditLogFilters, PageResponse } from '../types';

const STATUS_CODES = [
  { value: 'ALL', label: 'All Results' },
  { value: '200', label: 'Success (200)' },
  { value: '201', label: 'Created (201)' },
  { value: '204', label: 'No Content (204)' },
  { value: '400', label: 'Bad Request (400)' },
  { value: '401', label: 'Unauthorized (401)' },
  { value: '403', label: 'Forbidden (403)' },
  { value: '404', label: 'Not Found (404)' },
  { value: '409', label: 'Conflict (409)' },
  { value: '500', label: 'Server Error (500)' },
] as const;

const CATEGORIES = [
  { value: '', label: 'All Categories' },
  { value: '/api/admissions', label: 'Admission' },
  { value: '/api/horses', label: 'Horse' },
  { value: '/api/staff', label: 'Staff' },
  { value: '/api/stalls', label: 'Stable' },
  { value: '/api/health-records', label: 'Health' },
  { value: '/api/training', label: 'Training' },
  { value: '/api/auth', label: 'Access Control' },
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
  if (!role) return '-';
  switch (role) {
    case 'CLUB_MANAGER': return 'Club Manager';
    case 'HEAD_TRAINER': return 'Head Trainer';
    case 'VETERINARIAN': return 'Veterinarian';
    case 'GROOM': return 'Groom';
    case 'HORSE_OWNER': return 'Horse Owner';
    default: return role;
  }
}

function ResultBadge({ code }: { code: number }) {
  let colorClass = 'bg-[var(--color-surface-muted)] text-[var(--color-text-primary)] border border-[var(--color-border)]';
  let label = 'Unknown';
  
  if (code >= 200 && code < 300) {
    colorClass = 'bg-[var(--color-success-soft)] text-[var(--color-success)] border border-[var(--color-success-border)]';
    label = 'Success';
  } else if (code >= 400 && code < 500) {
    colorClass = 'bg-[var(--color-warning-soft)] text-[var(--color-warning-strong)] border border-[var(--color-warning-border)]';
    label = 'Failed';
  } else if (code >= 500) {
    colorClass = 'bg-[var(--color-danger-soft)] text-[var(--color-danger)] border border-[var(--color-danger-border)]';
    label = 'Failed';
  }

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium ${colorClass}`}>
      {label}
    </span>
  );
}

import { mapAuditLogEvent } from '../services/audit-event-mapper';

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
  const [expandedId, setExpandedId] = useState<number | null>(null);
  
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
        if (active) setError('Failed to load audit logs. Please try again.');
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
    updateUrl({ ...draft, search: draft.search.trim(), page: 0 }); // Reset to page 0 on filter apply
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
      <form
        className="grid gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 sm:grid-cols-2 xl:grid-cols-[minmax(220px,1fr)_minmax(120px,auto)_minmax(120px,auto)_minmax(140px,auto)_minmax(140px,auto)_auto] xl:items-end"
        onSubmit={(event) => { event.preventDefault(); apply(); }}
      >
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Search request path
          <span className="relative mt-1.5 block">
            <Icon name="search" size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]" />
            <input 
              value={draft.search} 
              onChange={(e) => setDraft({ ...draft, search: e.target.value })} 
              placeholder="e.g. /api/horses" 
              className="h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] pl-9 pr-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" 
            />
          </span>
        </label>
        
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Activity category
          <select 
            value={CATEGORIES.find(c => c.value && draft.search.includes(c.value))?.value || ''} 
            onChange={(e) => setDraft({ ...draft, search: e.target.value })} 
            className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          >
            {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
          </select>
        </label>

        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Result
          <select 
            value={draft.statusCode} 
            onChange={(e) => setDraft({ ...draft, statusCode: e.target.value })} 
            className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          >
            {STATUS_CODES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        </label>

        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          From
          <input 
            type="date" 
            value={draft.from} 
            onChange={(e) => setDraft({ ...draft, from: e.target.value })} 
            className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" 
          />
        </label>
        
        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          To
          <input 
            type="date" 
            min={draft.from || undefined} 
            value={draft.to} 
            onChange={(e) => setDraft({ ...draft, to: e.target.value })} 
            className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" 
          />
        </label>

        <div className="flex items-center gap-2 sm:col-span-2 xl:col-span-1">
          <Button type="submit" variant="primary" size="sm">Apply</Button>
          <Button type="button" variant="secondary" size="sm" onClick={clear}>Clear</Button>
        </div>
      </form>

      {error && <div role="alert" className="border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{error}</div>}

      <Panel className="overflow-hidden">
        {(!data || data.content.length === 0) ? (
          <EmptyState 
            title="No audit logs found" 
            description="Change the filters or clear them to see other records." 
            action={<Button size="sm" onClick={clear}>Clear filters</Button>} 
          />
        ) : (
          <div className="flex flex-col h-full">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm whitespace-nowrap">
                <thead className="border-b border-[var(--color-border)] text-xs font-medium text-[var(--color-text-muted)] uppercase tracking-wider bg-[var(--color-surface-muted)]">
                  <tr>
                    <th className="px-6 py-4">Time</th>
                    <th className="px-6 py-4">Actor</th>
                    <th className="px-6 py-4">Role</th>
                    <th className="px-6 py-4">Activity</th>
                    <th className="px-6 py-4">Target</th>
                    <th className="px-6 py-4">Result</th>
                    <th className="px-6 py-4"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-border)] text-[var(--color-text-primary)] relative">
                  {loading && (
                    <tr>
                      <td colSpan={7} className="absolute inset-0 bg-[var(--color-surface)]/50 backdrop-blur-[1px] z-10" />
                    </tr>
                  )}
                  {data.content.map((log) => {
                    const mapped = mapAuditLogEvent(log.httpMethod, log.requestPath, log.statusCode);
                    const isExpanded = expandedId === log.id;
                    
                    return (
                      <React.Fragment key={log.id}>
                        <tr 
                          className="hover:bg-[var(--color-surface-muted)] transition-colors cursor-pointer"
                          onClick={() => setExpandedId(isExpanded ? null : log.id)}
                        >
                          <td className="px-6 py-4 text-[var(--color-text-secondary)] whitespace-nowrap">
                            {new Date(log.createdAt).toLocaleString('en-GB', { 
                              day: '2-digit', month: 'short', year: 'numeric',
                              hour: '2-digit', minute: '2-digit', second: '2-digit'
                            })}
                          </td>
                          <td className="px-6 py-4">
                            {log.actorName ? (
                              <div>
                                <span className="font-semibold block text-[var(--color-text-primary)]">{log.actorName}</span>
                                {log.actorEmail && <span className="text-[11px] text-[var(--color-text-muted)]">{log.actorEmail}</span>}
                              </div>
                            ) : (
                              <span className="text-[var(--color-text-secondary)] italic">System / Unauthenticated</span>
                            )}
                          </td>
                          <td className="px-6 py-4">
                            <span className="text-[var(--color-text-secondary)]">{formatRole(log.actorRole)}</span>
                          </td>
                          <td className="px-6 py-4 font-medium text-[var(--color-text-primary)]">
                            {mapped.activity}
                          </td>
                          <td className="px-6 py-4 text-[var(--color-text-secondary)]">
                            {mapped.target}
                          </td>
                          <td className="px-6 py-4">
                            <ResultBadge code={log.statusCode} />
                          </td>
                          <td className="px-6 py-4 text-right">
                            <Icon name={isExpanded ? 'chevron-up' : 'chevron-down'} size={16} className="text-[var(--color-text-muted)]" />
                          </td>
                        </tr>
                        {isExpanded && (
                          <tr className="bg-[var(--color-surface-subtle)] border-b border-[var(--color-border)]">
                            <td colSpan={7} className="px-6 py-4">
                              <div className="flex flex-col gap-2 text-sm text-[var(--color-text-secondary)]">
                                <span className="font-semibold text-xs uppercase tracking-wider text-[var(--color-text-muted)] mb-1">Technical Details</span>
                                <div className="grid grid-cols-[100px_1fr] gap-2">
                                  <span className="font-medium">HTTP Method</span>
                                  <span className="font-mono text-xs bg-[var(--color-surface)] px-1.5 py-0.5 rounded border border-[var(--color-border)] w-fit">{log.httpMethod}</span>
                                </div>
                                <div className="grid grid-cols-[100px_1fr] gap-2">
                                  <span className="font-medium">Request Path</span>
                                  <span className="font-mono text-xs bg-[var(--color-surface)] px-1.5 py-0.5 rounded border border-[var(--color-border)] break-all">{log.requestPath}</span>
                                </div>
                                <div className="grid grid-cols-[100px_1fr] gap-2">
                                  <span className="font-medium">Status Code</span>
                                  <span className="font-mono text-xs bg-[var(--color-surface)] px-1.5 py-0.5 rounded border border-[var(--color-border)] w-fit">{log.statusCode}</span>
                                </div>
                              </div>
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
            
            {/* Pagination footer */}
            <div className="flex items-center justify-between border-t border-[var(--color-border)] px-6 py-3">
              <div className="text-sm text-[var(--color-text-secondary)]">
                Showing <span className="font-medium text-[var(--color-text-primary)]">{data.pageable.pageNumber * data.pageable.pageSize + 1}</span> to <span className="font-medium text-[var(--color-text-primary)]">{Math.min((data.pageable.pageNumber + 1) * data.pageable.pageSize, data.totalElements)}</span> of <span className="font-medium text-[var(--color-text-primary)]">{data.totalElements}</span> results
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
