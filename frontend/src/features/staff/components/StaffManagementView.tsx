'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { getStaffList, createStaff, updateStaffStatus } from '../services/staffService';
import type { StaffSummary, StaffCreationRequest, StaffCreationResponse } from '../types';
import { StaffTable } from './StaffTable';
import { AddStaffDialog } from './AddStaffDialog';
import { StaffDetailModal } from './StaffDetailModal';

const inputClassName =
  'h-9 rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-[13px] text-[var(--color-text-primary)] outline-none transition-colors placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-focus)] focus:ring-1 focus:ring-[var(--color-focus)]';

export function StaffManagementView() {
  const [staff, setStaff] = useState<StaffSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Filtering & Search
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Dialog & Actions
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [addLoading, setAddLoading] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [successMsg, setSuccessMsg] = useState('');
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);

  useEffect(() => {
    fetchStaff();
  }, []);

  async function fetchStaff() {
    try {
      setLoading(true);
      setError('');
      const res = await getStaffList();
      setStaff(res.data || []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to fetch staff list');
    } finally {
      setLoading(false);
    }
  }

  async function handleCreateStaff(req: StaffCreationRequest): Promise<StaffCreationResponse | null> {
    setAddLoading(true);
    try {
      const res = await createStaff(req);
      // Refresh the list in the background; dialog shows assignment summary
      void fetchStaff();
      setSuccessMsg('Staff member created successfully.');
      setTimeout(() => setSuccessMsg(''), 5000);
      return res.data ?? null;
    } finally {
      setAddLoading(false);
    }
  }

  async function handleToggleStatus(userId: number, currentStatus: boolean) {
    setActionLoadingId(userId);
    try {
      await updateStaffStatus(userId, !currentStatus);
      // Update local state directly to be snappy
      setStaff(prev => prev.map(s => s.userId === userId ? { ...s, active: !currentStatus } : s));
      setSuccessMsg('Status updated successfully.');
      setTimeout(() => setSuccessMsg(''), 5000);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to update status');
    } finally {
      setActionLoadingId(null);
    }
  }

  const filteredStaff = staff.filter((s) => {
    const matchesSearch = s.fullName.toLowerCase().includes(search.toLowerCase()) || 
                          s.email.toLowerCase().includes(search.toLowerCase());
    const matchesRole = roleFilter === 'ALL' || s.role === roleFilter;
    const matchesStatus = statusFilter === 'ALL' || (statusFilter === 'ACTIVE' ? s.active : !s.active);
    return matchesSearch && matchesRole && matchesStatus;
  });

  const headTrainers = staff.filter(s => s.role === 'HEAD_TRAINER' && s.active);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-[20px] font-semibold tracking-tight text-[var(--color-text-primary)]">Staff</h1>
          <p className="mt-1 text-[13px] text-[var(--color-text-secondary)]">
            Manage employee accounts and roles.
          </p>
        </div>
        <Button variant="primary" icon="plus" onClick={() => setAddDialogOpen(true)}>
          Add Staff
        </Button>
      </div>

      {successMsg && (
        <div className="flex items-start gap-2 rounded-[var(--radius-sm)] bg-[var(--color-success-soft,#f0fdf4)] px-3 py-2.5 text-[12px] text-[var(--color-success,#16a34a)]">
          <Icon name="check" size={14} className="mt-0.5 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {error && (
        <div className="flex items-start gap-2 rounded-[var(--radius-sm)] bg-[var(--color-danger-soft)] px-3 py-2.5 text-[12px] text-[var(--color-danger)]">
          <Icon name="alert-triangle" size={14} className="mt-0.5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Statistic Cards */}
      {!loading && staff.length > 0 && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          {[
            { label: 'Total Staff', count: staff.length, role: 'ALL' },
            { label: 'Grooms', count: staff.filter(s => s.role === 'GROOM').length, role: 'GROOM' },
            { label: 'Trainers', count: staff.filter(s => s.role === 'HEAD_TRAINER').length, role: 'HEAD_TRAINER' },
            { label: 'Veterinarians', count: staff.filter(s => s.role === 'VETERINARIAN').length, role: 'VETERINARIAN' },
          ].map((card) => (
            <button
              key={card.role}
              onClick={() => setRoleFilter(card.role)}
              className={`flex flex-col rounded-[var(--radius-lg)] border p-4 text-left shadow-sm transition-colors ${
                roleFilter === card.role
                  ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]'
                  : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:border-[var(--color-primary)]/50 hover:bg-[var(--color-surface-muted)]'
              }`}
            >
              <span className="text-[12px] font-medium text-[var(--color-text-secondary)]">{card.label}</span>
              <span className="mt-1 text-[24px] font-semibold text-[var(--color-text-primary)]">{card.count}</span>
            </button>
          ))}
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-sm">
        <div className="relative min-w-[200px] flex-1">
          <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-[var(--color-text-muted)]">
            <Icon name="search" size={14} />
          </div>
          <input
            type="text"
            placeholder="Search name or email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className={`${inputClassName} w-full pl-8`}
          />
        </div>
        
        <select
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
          className={`${inputClassName} min-w-[140px]`}
        >
          <option value="ALL">All Roles</option>
          <option value="GROOM">Groom</option>
          <option value="VETERINARIAN">Veterinarian</option>
          <option value="HEAD_TRAINER">Head Trainer</option>
        </select>

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className={`${inputClassName} min-w-[120px]`}
        >
          <option value="ALL">All Status</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>
      </div>

      {/* Main Content */}
      {loading ? (
        <div className="flex h-40 items-center justify-center">
          <span className="h-6 w-6 animate-spin rounded-full border-2 border-[var(--color-primary)] border-t-transparent" />
        </div>
      ) : (
        <StaffTable 
          staff={filteredStaff} 
          onToggleStatus={handleToggleStatus} 
          loadingActionId={actionLoadingId} 
          onRowClick={(id) => setSelectedUserId(id)}
        />
      )}

      <AddStaffDialog 
        open={addDialogOpen} 
        onClose={() => setAddDialogOpen(false)} 
        onSubmit={handleCreateStaff}
        loading={addLoading}
        headTrainers={headTrainers}
      />

      <StaffDetailModal 
        userId={selectedUserId} 
        onClose={() => setSelectedUserId(null)}
        headTrainers={headTrainers}
        onUpdated={() => {
          void fetchStaff();
          setSuccessMsg('Staff updated successfully.');
          setTimeout(() => setSuccessMsg(''), 5000);
        }}
      />
    </div>
  );
}
