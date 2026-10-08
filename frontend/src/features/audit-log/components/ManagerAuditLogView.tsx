'use client';

import { useEffect, useState, useMemo } from 'react';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { auditLogApi } from '../services/api';
import type { AuditLogItem, AuditLogFilters, PageResponse } from '../types';

const HTTP_METHODS = ['ALL', 'POST', 'PUT', 'PATCH', 'DELETE'] as const;
const STATUS_CODES = ['ALL', '200', '201', '204', '400', '401', '403', '404', '409', '500'] as const;

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

function StatusBadge({ code }: { code: number }) {
  let colorClass = 'bg-[var(--color-surface-muted)] text-[var(--color-text-primary)] border border-[var(--color-border)]';
  
  if (code >= 200 && code < 300) {
    colorClass = 'bg-[var(--color-success-soft)] text-[var(--color-success)] border border-[var(--color-success-border)]';
  } else if (code >= 400 && code < 500) {
    colorClass = 'bg-[var(--color-warning-soft)] text-[var(--color-warning-strong)] border border-[var(--color-warning-border)]';
  } else if (code >= 500) {
    colorClass = 'bg-[var(--color-danger-soft)] text-[var(--color-danger)] border border-[var(--color-danger-border)]';
  }

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium ${colorClass}`}>
      {code}
    </span>
  );
}

function MethodBadge({ method }: { method: string }) {
  let colorClass = 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)]';
  
  switch (method) {
    case 'POST': colorClass = 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'; break;
    case 'PUT': colorClass = 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'; break;
    case 'PATCH': colorClass = 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400'; break;
    case 'DELETE': colorClass = 'bg-[var(--color-danger-soft)] text-[var(--color-danger)]'; break;
  }

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold ${colorClass}`}>
      {method}
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
          Method
          <select 
            value={draft.httpMethod} 
            onChange={(e) => setDraft({ ...draft, httpMethod: e.target.value })} 
            className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          >
            {HTTP_METHODS.map((m) => <option key={m} value={m}>{m === 'ALL' ? 'All Methods' : m}</option>)}
          </select>
        </label>

        <label className="block text-xs font-medium text-[var(--color-text-secondary)]">
          Status
          <select 
            value={draft.statusCode} 
            onChange={(e) => setDraft({ ...draft, statusCode: e.target.value })} 
            className="mt-1.5 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          >
            {STATUS_CODES.map((s) => <option key={s} value={s}>{s === 'ALL' ? 'All Statuses' : s}</option>)}
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
                    <th className="px-6 py-4">Method</th>
                    <th className="px-6 py-4">Request</th>
                    <th className="px-6 py-4">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-border)] text-[var(--color-text-primary)] relative">
                  {loading && (
                    <tr>
                      <td colSpan={6} className="absolute inset-0 bg-[var(--color-surface)]/50 backdrop-blur-[1px] z-10" />
                    </tr>
                  )}
                  {data.content.map((log) => (
                    <tr key={log.id} className="hover:bg-[var(--color-surface-muted)] transition-colors">
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
                      <td className="px-6 py-4">
                        <MethodBadge method={log.httpMethod} />
                      </td>
                      <td className="px-6 py-4">
                        <span className="font-metric text-xs text-[var(--color-text-primary)] max-w-xs md:max-w-md lg:max-w-lg truncate block" title={log.requestPath}>
                          {log.requestPath}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <StatusBadge code={log.statusCode} />
                      </td>
                    </tr>
                  ))}
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
