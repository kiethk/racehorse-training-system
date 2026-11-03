'use client';

import Link from 'next/link';
import { TBody, THead, Table, Td, Th, Tr } from '@/components/ui/Table';
import { Notice } from '@/components/ui/Notice';
import { useEffect, useState, useMemo } from 'react';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { FilterBar } from '@/components/ui/FilterBar';
import { FormField } from '@/components/ui/FormField';
import { Input, SearchInput, Select } from '@/components/ui/Input';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { auditLogApi } from '../services/api';
import { extractAuditSummary } from '../lib/auditExport';
import { resolveEntityUrl, resolveLogModule } from '../lib/auditDiff';
import { AuditLogDetailModal } from './AuditLogDetailModal';
import { AuditLogArchiveModal } from './AuditLogArchiveModal';
import { AuditLogExportModal } from './AuditLogExportModal';
import { ModuleBadge } from './ModuleBadge';
import type { AuditLogItem, AuditLogFilters, PageResponse, AuditArchiveStatus } from '../types';

const STATUS_OPTIONS = [
  { label: 'All Results', value: 'ALL' },
  { label: 'Success', value: '200' },
  { label: 'Failed / Rejected', value: '400' },
] as const;

const MODULE_OPTIONS = [
  { label: 'All Modules', value: 'ALL' },
  { label: 'Admissions', value: 'ADMISSIONS' },
  { label: 'Medical & Care', value: 'MEDICAL' },
  { label: 'Stables & Stalls', value: 'STABLE' },
  { label: 'Horse Lifecycle', value: 'HORSES' },
  { label: 'Staff & Accounts', value: 'STAFF' },
  { label: 'Training & Courses', value: 'TRAINING' },
  { label: 'Racing & Registration', value: 'RACING' },
  { label: 'System & Admin', value: 'SYSTEM' },
] as const;

const ROLE_OPTIONS = [
  { label: 'All Roles', value: 'ALL' },
  { label: 'Club Manager', value: 'CLUB_MANAGER' },
  { label: 'Head Trainer', value: 'HEAD_TRAINER' },
  { label: 'Veterinarian', value: 'VETERINARIAN' },
  { label: 'Groom', value: 'GROOM' },
  { label: 'Horse Owner', value: 'HORSE_OWNER' },
] as const;

const initialFilters: AuditLogFilters = {
  search: '',
  httpMethod: 'ALL',
  statusCode: 'ALL',
  module: 'ALL',
  role: 'ALL',
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

function formatAction(action: string | null, method?: string, path?: string): string {
  if (action) {
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
  if (method && path) {
    return `${method} ${path}`;
  }
  return 'System Activity';
}

function OutcomeText({ success }: { success: boolean }) {
  if (success) {
    return (
      <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-600 dark:text-emerald-400">
        <span className="size-1.5 rounded-full bg-emerald-500" />
        Success
      </span>
    );
  }

  return (
    <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-rose-600 dark:text-rose-400">
      <span className="size-1.5 rounded-full bg-rose-500" />
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
      module: searchParams.get('module') || 'ALL',
      role: searchParams.get('role') || 'ALL',
      from: searchParams.get('from') || '',
      to: searchParams.get('to') || '',
      page: Number(searchParams.get('page')) || 0,
      size: Number(searchParams.get('size')) || 20,
    };
  }, [searchParams]);

  const [data, setData] = useState<PageResponse<AuditLogItem> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedLog, setSelectedLog] = useState<AuditLogItem | null>(null);
  const [archiveStatus, setArchiveStatus] = useState<AuditArchiveStatus | null>(null);
  const [isArchiveModalOpen, setIsArchiveModalOpen] = useState(false);
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  
  const [draft, setDraft] = useState<AuditLogFilters>(urlFilters);

  const loadArchiveStatus = async () => {
    try {
      const status = await auditLogApi.getArchiveStatus();
      setArchiveStatus(status);
    } catch (err) {
      console.error('Failed to load archive status:', err);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadArchiveStatus();
  }, []);

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
    if (filters.module && filters.module !== 'ALL') params.set('module', filters.module);
    if (filters.role && filters.role !== 'ALL') params.set('role', filters.role);
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
      {/* Header with Title and Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-[var(--color-text-primary)]">
            Activity & Audit Logs
          </h1>
          <p className="text-sm text-[var(--color-text-secondary)] mt-1">
            Review historical staff activities, domain approvals, and intake decisions across the club.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="secondary"
            onClick={() => setIsArchiveModalOpen(true)}
            className={
              archiveStatus?.requiresMaintenance
                ? 'border-rose-400 text-rose-700 bg-rose-50/50 hover:bg-rose-100/50 dark:border-rose-800 dark:text-rose-300 dark:bg-rose-950/30'
                : ''
            }
            title={
              archiveStatus?.requiresMaintenance
                ? `Maintenance Notice: ${archiveStatus.agedCount} logs older than 30 days detected`
                : 'Archive and clean database audit trail'
            }
          >
            {archiveStatus?.requiresMaintenance && (
              <span className="relative flex size-2 mr-1">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-rose-400 opacity-75" />
                <span className="relative inline-flex size-2 rounded-full bg-rose-600" />
              </span>
            )}
            <Icon name="archive" className="size-4 mr-1.5" />
            Archive &amp; Purge
          </Button>
          <Button
            type="button"
            variant="secondary"
            onClick={() => setIsExportModalOpen(true)}
          >
            <Icon name="download" className="size-4 mr-1.5" />
            Export CSV
          </Button>
        </div>
      </div>

      {/* Advanced Filter Bar */}
      <FilterBar
        className="[&_input]:min-h-9 [&_select]:min-h-9"
        onSubmit={(event) => { event.preventDefault(); apply(); }}
      >
        <FormField label="Search Activity or User" className="min-w-[13rem] flex-[1.4]">
          <SearchInput
            label="Search Activity or User"
            value={draft.search}
            onChange={(search) => setDraft({ ...draft, search })}
            placeholder="Search by actor, action, or ID..."
          />
        </FormField>
        <FormField label="Module" className="min-w-[9.5rem] flex-1">
          <Select
            value={draft.module || 'ALL'}
            onChange={(e) => setDraft({ ...draft, module: e.target.value })}
          >
            {MODULE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="Actor Role" className="min-w-[9.5rem] flex-1">
          <Select
            value={draft.role || 'ALL'}
            onChange={(e) => setDraft({ ...draft, role: e.target.value })}
          >
            {ROLE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="Result Status" className="min-w-[8.5rem] flex-1">
          <Select
            value={draft.statusCode}
            onChange={(e) => setDraft({ ...draft, statusCode: e.target.value })}
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="From Date" className="min-w-[8.5rem] flex-1">
          <Input
            type="date"
            value={draft.from}
            onChange={(e) => setDraft({ ...draft, from: e.target.value })}
          />
        </FormField>
        <FormField label="To Date" className="min-w-[8.5rem] flex-1">
          <Input
            type="date"
            min={draft.from || undefined}
            value={draft.to}
            onChange={(e) => setDraft({ ...draft, to: e.target.value })}
          />
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
            <Table bare className={loading ? 'opacity-60 transition-opacity' : 'transition-opacity'}>
              <THead>
                <Tr>
                  <Th className="px-5 py-3.5 w-[11rem]">Timestamp</Th>
                  <Th className="px-5 py-3.5 w-[14rem]">Actor & Role</Th>
                  <Th className="px-5 py-3.5 min-w-[15rem]">Module & Activity</Th>
                  <Th className="px-5 py-3.5 w-[12rem]">Target Entity</Th>
                  <Th className="px-5 py-3.5 w-[11rem] text-right">Outcome & Inspect</Th>
                </Tr>
              </THead>
              <TBody>
                {data.content.map((log) => {
                  const displayAction = formatAction(log.action, log.httpMethod, log.requestPath);
                  const targetEntityUrl = resolveEntityUrl(log.entityName, log.entityId);
                  const moduleName = resolveLogModule(log.module, log.entityName, log.requestPath, log.action);

                  return (
                    <Tr
                      key={log.id}
                      className="hover:bg-[var(--color-surface-muted)] transition-colors cursor-pointer"
                      onClick={() => setSelectedLog(log)}
                    >
                      {/* 1. Timestamp */}
                      <Td className="px-5 py-3.5 text-[var(--color-text-secondary)] text-xs font-mono whitespace-nowrap">
                        {new Date(log.createdAt).toLocaleString('en-GB', { 
                          day: '2-digit', month: 'short', year: 'numeric',
                          hour: '2-digit', minute: '2-digit'
                        })}
                      </Td>

                      {/* 2. Actor & Role (stacked) */}
                      <Td className="px-5 py-3.5">
                        <div className="space-y-0.5">
                          {log.actorName ? (
                            <span className="font-semibold block text-[var(--color-text-primary)] text-sm truncate max-w-[13rem]">
                              {log.actorName}
                            </span>
                          ) : (
                            <span className="text-[var(--color-text-secondary)] italic text-sm">System / Guest</span>
                          )}
                          <div className="flex items-center gap-1.5 flex-wrap">
                            <span className="text-xs px-1.5 py-0.2 rounded bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)] font-medium">
                              {formatRole(log.actorRole)}
                            </span>
                            {log.actorEmail && (
                              <span className="text-[11px] text-[var(--color-text-muted)] truncate max-w-[11rem] font-mono">
                                {log.actorEmail}
                              </span>
                            )}
                          </div>
                        </div>
                      </Td>

                      {/* 3. Module & Activity (stacked) */}
                      <Td className="px-5 py-3.5">
                        <div className="space-y-1">
                          <ModuleBadge moduleName={moduleName} />
                          <div className="font-semibold text-sm text-[var(--color-text-primary)] leading-snug">
                            {displayAction}
                          </div>
                        </div>
                      </Td>

                      {/* 4. Target Entity with link */}
                      <Td className="px-5 py-3.5">
                        {log.entityName ? (
                          targetEntityUrl ? (
                            <Link
                              href={targetEntityUrl}
                              onClick={(e) => e.stopPropagation()}
                              className="font-medium text-sm text-[var(--color-primary)] hover:underline inline-flex items-center gap-1.5 max-w-[11rem] truncate"
                              title={`View ${log.entityName} ${log.entityId != null ? '#' + log.entityId : ''}`}
                            >
                              <span>{log.entityName}</span>
                              {log.entityId != null && (
                                <span className="bg-[var(--color-primary-soft)] px-1.5 py-0.2 rounded font-mono text-[11px] font-semibold">
                                  #{log.entityId}
                                </span>
                              )}
                            </Link>
                          ) : (
                            <div className="inline-flex items-center gap-1.5 text-sm text-[var(--color-text-primary)]">
                              <span>{log.entityName}</span>
                              {log.entityId != null && (
                                <span className="bg-[var(--color-surface-muted)] px-1.5 py-0.2 rounded font-mono text-[11px] text-[var(--color-text-secondary)]">
                                  #{log.entityId}
                                </span>
                              )}
                            </div>
                          )
                        ) : (
                          <span className="text-xs text-[var(--color-text-muted)] italic">-</span>
                        )}
                      </Td>

                      {/* 5. Outcome & Inspect (inline together) */}
                      <Td className="px-5 py-3.5 text-right" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center justify-end gap-3">
                          <OutcomeText success={log.success} />
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => setSelectedLog(log)}
                          >
                            Inspect
                          </Button>
                        </div>
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

      {/* Audit Log Detail & State Diff Modal */}
      <AuditLogDetailModal
        item={selectedLog}
        open={selectedLog != null}
        onClose={() => setSelectedLog(null)}
      />

      {/* Archive & Purge Modal */}
      <AuditLogArchiveModal
        open={isArchiveModalOpen}
        onClose={() => setIsArchiveModalOpen(false)}
        status={archiveStatus}
        onPurged={() => {
          auditLogApi.getAuditLogs(urlFilters).then(setData).catch(console.error);
          loadArchiveStatus();
        }}
      />

      {/* Export CSV Modal (Export without purging) */}
      <AuditLogExportModal
        open={isExportModalOpen}
        onClose={() => setIsExportModalOpen(false)}
        currentFilters={urlFilters}
        totalCount={archiveStatus?.totalCount ?? data?.totalElements}
        filteredCount={data?.totalElements}
      />
    </div>
  );
}
