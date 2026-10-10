'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/Button';
import { SearchInput, Select } from '@/components/ui/Input';
import { PageHeader } from '@/components/ui/PageHeader';
import { ScreenLayout } from '@/components/ui/ScreenLayout';
import { FilterBar } from '@/components/ui/FilterBar';
import { MetricCard } from '@/components/ui/MetricCard';
import { Notice } from '@/components/ui/Notice';
import { ListSkeleton } from '@/components/ui/states';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { getStaffList, createStaff, updateStaffStatus } from '../services/staffService';
import type { StaffSummary, StaffCreationRequest, StaffCreationResponse } from '../types';
import { StaffTable } from './StaffTable';
import { AddStaffDialog } from './AddStaffDialog';
import { StaffDetailModal } from './StaffDetailModal';

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
      setError(displayError(err, 'Unable to load the staff list.'));
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
      toast.error(displayError(err, 'Unable to update staff status.'));
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
    <ScreenLayout variant="list">
      <PageHeader
        title="Staff"
        description="Manage employee accounts and roles."
        actions={<Button variant="primary" icon="plus" onClick={() => setAddDialogOpen(true)}>Add Staff</Button>}
      />

      {successMsg && (
        <Notice tone="success">{successMsg}</Notice>
      )}

      {error && (
        <Notice tone="error">{error}</Notice>
      )}

      {/* Statistic Cards */}
      {!loading && staff.length > 0 && (
        <div className="grid grid-cols-1 gap-3 min-[480px]:grid-cols-2 lg:grid-cols-4">
          {[
            { label: 'Total Staff', count: staff.length, role: 'ALL' },
            { label: 'Grooms', count: staff.filter(s => s.role === 'GROOM').length, role: 'GROOM' },
            { label: 'Trainers', count: staff.filter(s => s.role === 'HEAD_TRAINER').length, role: 'HEAD_TRAINER' },
            { label: 'Veterinarians', count: staff.filter(s => s.role === 'VETERINARIAN').length, role: 'VETERINARIAN' },
          ].map((card) => (
            <MetricCard
              key={card.role}
              label={card.label}
              value={card.count}
              onClick={() => setRoleFilter(card.role)}
              active={roleFilter === card.role}
            />
          ))}
        </div>
      )}

      {/* Filters */}
      <FilterBar className="[&_input]:min-h-9 [&_select]:min-h-9" onSubmit={(event) => event.preventDefault()}>
        <SearchInput
          className="min-w-[14rem] flex-1"
          value={search}
          onChange={setSearch}
          placeholder="Search name or email..."
        />
        <div className="min-w-[10rem]">
          <Select aria-label="Role" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="ALL">All roles</option>
            <option value="GROOM">Groom</option>
            <option value="VETERINARIAN">Veterinarian</option>
            <option value="HEAD_TRAINER">Head Trainer</option>
          </Select>
        </div>
        <div className="min-w-[9rem]">
          <Select aria-label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
        </div>
      </FilterBar>

      {/* Main Content */}
      {loading ? <ListSkeleton rows={6} /> : (
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
    </ScreenLayout>
  );
}
