'use client';

import { TBody, THead, Table, Td, Th, Tr } from '@/components/ui/Table';
import { Notice } from '@/components/ui/Notice';
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
    colorClass = 'bg-[var(--color-success-soft)] text-[var(--color-success)] border border-[var(--color-success)]';
  } else if (code >= 400 && code < 500) {
    colorClass = 'bg-[var(--color-warning-soft)] text-[var(--color-warning)] border border-[var(--color-warning)]';
  } else if (code >= 500) {
    colorClass = 'bg-[var(--color-danger-soft)] text-[var(--color-danger)] border border-[var(--color-danger)]';
  }

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${colorClass}`}>
      {code}
    </span>
  );
}

function MethodBadge({ method }: { method: string }) {
  let colorClass = 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)]';
  
  switch (method) {
    case 'POST': colorClass = 'bg-[var(--color-info-soft)] text-[var(--color-info)]'; break;
    case 'PUT': colorClass = 'bg-[var(--color-warning-soft)] text-[var(--color-warning)]'; break;
    case 'PATCH': colorClass = 'bg-[var(--color-isolated-soft)] text-[var(--color-isolated)]'; break;
    case 'DELETE': colorClass = 'bg-[var(--color-danger-soft)] text-[var(--color-danger)]'; break;
  }

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-bold ${colorClass}`}>
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
      <FilterBar
        className="[&_input]:min-h-9 [&_select]:min-h-9"
        onSubmit={(event) => { event.preventDefault(); apply(); }}
      >
        <FormField label="Search request path" className="min-w-[14rem] flex-[1.4]">
          <SearchInput
            label="Search request path"
            value={draft.search}
            onChange={(search) => setDraft({ ...draft, search })}
            placeholder="e.g. /api/horses"
          />
        </FormField>
        <FormField label="Method" className="min-w-[8rem] flex-1">
          <Select value={draft.httpMethod} onChange={(e) => setDraft({ ...draft, httpMethod: e.target.value })}>
            {HTTP_METHODS.map((m) => <option key={m} value={m}>{m === 'ALL' ? 'All methods' : m}</option>)}
          </Select>
        </FormField>
        <FormField label="Status" className="min-w-[8rem] flex-1">
          <Select value={draft.statusCode} onChange={(e) => setDraft({ ...draft, statusCode: e.target.value })}>
            {STATUS_CODES.map((s) => <option key={s} value={s}>{s === 'ALL' ? 'All statuses' : s}</option>)}
          </Select>
        </FormField>
        <FormField label="From" className="min-w-[9.5rem] flex-1">
          <Input type="date" value={draft.from} onChange={(e) => setDraft({ ...draft, from: e.target.value })} />
        </FormField>
        <FormField label="To" className="min-w-[9.5rem] flex-1">
          <Input type="date" min={draft.from || undefined} value={draft.to} onChange={(e) => setDraft({ ...draft, to: e.target.value })} />
        </FormField>
        <div className="flex shrink-0 items-center gap-2">
          <Button type="submit" variant="primary">Apply</Button>
          <Button type="button" variant="secondary" onClick={clear}>Clear</Button>
        </div>
      </FilterBar>

      {error && <Notice tone="error">{error}</Notice>}

      <Panel className="overflow-hidden">
        {(!data || data.content.length === 0) ? (
          <EmptyState 
            title="No audit logs found" 
            description="Change the filters or clear them to see other records." 
            action={<Button size="sm" onClick={clear}>Clear filters</Button>} 
          />
        ) : (
          <div className="flex flex-col h-full">
            <Table bare className={loading ? 'whitespace-nowrap opacity-60 transition-opacity' : 'whitespace-nowrap transition-opacity'}>
                <THead>
                  <Tr>
                    <Th className="px-6 py-4">Time</Th>
                    <Th className="px-6 py-4">Actor</Th>
                    <Th className="px-6 py-4">Role</Th>
                    <Th className="px-6 py-4">Method</Th>
                    <Th className="px-6 py-4">Request</Th>
                    <Th className="px-6 py-4">Status</Th>
                  </Tr>
                </THead>
                <TBody>
                  {data.content.map((log) => (
                    <Tr key={log.id} className="hover:bg-[var(--color-surface-muted)] transition-colors">
                      <Td className="px-6 py-4 text-[var(--color-text-secondary)] whitespace-nowrap">
                        {new Date(log.createdAt).toLocaleString('en-GB', { 
                          day: '2-digit', month: 'short', year: 'numeric',
                          hour: '2-digit', minute: '2-digit', second: '2-digit'
                        })}
                      </Td>
                      <Td className="px-6 py-4">
                        {log.actorName ? (
                          <div>
                            <span className="font-semibold block text-[var(--color-text-primary)]">{log.actorName}</span>
                            {log.actorEmail && <span className="text-xs text-[var(--color-text-muted)]">{log.actorEmail}</span>}
                          </div>
                        ) : (
                          <span className="text-[var(--color-text-secondary)] italic">System / Unauthenticated</span>
                        )}
                      </Td>
                      <Td className="px-6 py-4">
                        <span className="text-[var(--color-text-secondary)]">{formatRole(log.actorRole)}</span>
                      </Td>
                      <Td className="px-6 py-4">
                        <MethodBadge method={log.httpMethod} />
                      </Td>
                      <Td className="px-6 py-4">
                        <span className="font-metric text-xs text-[var(--color-text-primary)] max-w-xs md:max-w-md lg:max-w-lg truncate block" title={log.requestPath}>
                          {log.requestPath}
                        </span>
                      </Td>
                      <Td className="px-6 py-4">
                        <StatusBadge code={log.statusCode} />
                      </Td>
                    </Tr>
                  ))}
                </TBody>
              </Table>
            
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
