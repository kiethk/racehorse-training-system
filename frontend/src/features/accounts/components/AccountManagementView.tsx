'use client';

import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '@/context/AuthContext';
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
import { getAccountList, createAccount, updateAccountStatus } from '../services/accountService';
import type { AccountSummary, AccountCreationRequest, AccountCreationResponse } from '../types';
import { AccountTable } from './AccountTable';
import { AddAccountDialog } from './AddAccountDialog';
import { AccountDetailModal } from './AccountDetailModal';

type GroupFilter = 'ALL' | 'STAFF' | 'HORSE_OWNER' | 'CLUB_MANAGER';

export function AccountManagementView() {
  const { user: currentUser } = useAuth();
  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filtering & Search
  const [search, setSearch] = useState('');
  const [groupFilter, setGroupFilter] = useState<GroupFilter>('ALL');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Dialog & Actions
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [addLoading, setAddLoading] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);

  useEffect(() => {
    void fetchAccounts();
  }, []);

  async function fetchAccounts() {
    try {
      setLoading(true);
      setError('');
      const res = await getAccountList();
      setAccounts(res.data || []);
    } catch (err: unknown) {
      setError(displayError(err, 'Unable to load the accounts directory.'));
    } finally {
      setLoading(false);
    }
  }

  async function handleCreateAccount(
    req: AccountCreationRequest
  ): Promise<AccountCreationResponse | null> {
    setAddLoading(true);
    try {
      const res = await createAccount(req);
      if (!res.success || !res.data) {
        throw new Error(res.message || 'Unable to create account.');
      }
      void fetchAccounts();
      toast.success('Account created successfully');
      return res.data;
    } finally {
      setAddLoading(false);
    }
  }

  async function handleToggleStatus(userId: number, currentStatus: boolean) {
    if (userId === currentUser?.userId && currentStatus) {
      toast.error('You cannot deactivate your own administrator account.');
      return;
    }

    setActionLoadingId(userId);
    try {
      await updateAccountStatus(userId, !currentStatus);
      setAccounts((prev) =>
        prev.map((acc) => (acc.userId === userId ? { ...acc, active: !currentStatus } : acc))
      );
      toast.success('Account status updated');
    } catch (err: unknown) {
      toast.error(displayError(err, 'Unable to update account status.'));
    } finally {
      setActionLoadingId(null);
    }
  }

  // Handle group change: reset role filter if incompatible
  function handleSelectGroup(group: GroupFilter) {
    setGroupFilter(group);
    setRoleFilter('ALL');
  }

  const isStaffRole = (role: string) =>
    role === 'GROOM' || role === 'HEAD_TRAINER' || role === 'VETERINARIAN';

  // Counts for KPI cards
  const staffCount = useMemo(
    () => accounts.filter((a) => isStaffRole(a.role)).length,
    [accounts]
  );
  const ownerCount = useMemo(
    () => accounts.filter((a) => a.role === 'HORSE_OWNER').length,
    [accounts]
  );
  const managerCount = useMemo(
    () => accounts.filter((a) => a.role === 'CLUB_MANAGER').length,
    [accounts]
  );

  const filteredAccounts = useMemo(() => {
    const list = accounts.filter((acc) => {
      const q = search.trim().toLowerCase();
      const matchesSearch =
        !q ||
        acc.fullName.toLowerCase().includes(q) ||
        acc.email.toLowerCase().includes(q);

      // Group filter
      let matchesGroup = true;
      if (groupFilter === 'STAFF') {
        matchesGroup = isStaffRole(acc.role);
      } else if (groupFilter === 'HORSE_OWNER') {
        matchesGroup = acc.role === 'HORSE_OWNER';
      } else if (groupFilter === 'CLUB_MANAGER') {
        matchesGroup = acc.role === 'CLUB_MANAGER';
      }

      // Detailed role filter
      const matchesRole = roleFilter === 'ALL' || acc.role === roleFilter;

      // Status filter
      const matchesStatus =
        statusFilter === 'ALL' || (statusFilter === 'ACTIVE' ? acc.active : !acc.active);

      return matchesSearch && matchesGroup && matchesRole && matchesStatus;
    });

    // Option 1 sorting: Pin current user at top, then newest created (createdAt DESC / userId DESC)
    return list.slice().sort((a, b) => {
      const isASelf = currentUser?.userId != null && a.userId === currentUser.userId;
      const isBSelf = currentUser?.userId != null && b.userId === currentUser.userId;
      if (isASelf && !isBSelf) return -1;
      if (!isASelf && isBSelf) return 1;

      const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      if (timeB !== timeA) {
        return timeB - timeA;
      }
      return b.userId - a.userId;
    });
  }, [accounts, search, groupFilter, roleFilter, statusFilter, currentUser?.userId]);

  const headTrainers = useMemo(
    () => accounts.filter((a) => a.role === 'HEAD_TRAINER' && a.active),
    [accounts]
  );

  return (
    <ScreenLayout variant="list">
      <PageHeader
        title="Accounts"
        actions={
          <Button variant="primary" icon="plus" onClick={() => setAddDialogOpen(true)}>
            Add Account
          </Button>
        }
      />

      {error && <Notice tone="error">{error}</Notice>}

      {/* Metric KPI Cards with 1-click filtering */}
      {!loading && accounts.length > 0 && (
        <div className="grid grid-cols-1 gap-3 min-[480px]:grid-cols-2 lg:grid-cols-4">
          {[
            { label: 'Total Accounts', count: accounts.length, group: 'ALL' as GroupFilter },
            { label: 'Staff Members', count: staffCount, group: 'STAFF' as GroupFilter },
            { label: 'Horse Owners', count: ownerCount, group: 'HORSE_OWNER' as GroupFilter },
            { label: 'Administrators', count: managerCount, group: 'CLUB_MANAGER' as GroupFilter },
          ].map((card) => (
            <MetricCard
              key={card.group}
              label={card.label}
              value={card.count}
              onClick={() => handleSelectGroup(card.group)}
              active={groupFilter === card.group}
            />
          ))}
        </div>
      )}

      {/* Segmented Group Tabs */}
      <div className="flex border-b border-[var(--color-border)] gap-2">
        {[
          { id: 'ALL' as GroupFilter, label: 'All Accounts' },
          { id: 'STAFF' as GroupFilter, label: 'Staff Members' },
          { id: 'HORSE_OWNER' as GroupFilter, label: 'Horse Owners' },
          { id: 'CLUB_MANAGER' as GroupFilter, label: 'Administrators' },
        ].map((tab) => {
          const isActive = groupFilter === tab.id;
          return (
            // eslint-disable-next-line no-restricted-syntax
            <button
              key={tab.id}
              type="button"
              onClick={() => handleSelectGroup(tab.id)}
              className={`pb-2.5 pt-1 px-3 text-sm font-medium transition-colors border-b-2 -mb-px ${
                isActive
                  ? 'border-[var(--color-primary)] text-[var(--color-primary)]'
                  : 'border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:border-[var(--color-border)]'
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Filter Bar */}
      <FilterBar className="[&_input]:min-h-9 [&_select]:min-h-9" onSubmit={(e) => e.preventDefault()}>
        <SearchInput
          className="min-w-[14rem] flex-1"
          value={search}
          onChange={setSearch}
          placeholder="Search by name or email..."
        />

        {/* Dynamic Role Dropdown depending on active tab */}
        {(groupFilter === 'ALL' || groupFilter === 'STAFF') && (
          <div className="min-w-[11rem]">
            <Select
              aria-label="Filter by role"
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
            >
              <option value="ALL">All roles</option>
              <option value="GROOM">Groom</option>
              <option value="HEAD_TRAINER">Head Trainer</option>
              <option value="VETERINARIAN">Veterinarian</option>
              {groupFilter === 'ALL' && (
                <>
                  <option value="HORSE_OWNER">Horse Owner</option>
                  <option value="CLUB_MANAGER">Club Manager</option>
                </>
              )}
            </Select>
          </div>
        )}

        <div className="min-w-[9.5rem]">
          <Select
            aria-label="Filter by status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
        </div>
      </FilterBar>

      {/* Main Table Content */}
      {loading ? (
        <ListSkeleton rows={6} />
      ) : (
        <AccountTable
          accounts={filteredAccounts}
          onToggleStatus={handleToggleStatus}
          loadingActionId={actionLoadingId}
          onRowClick={(id) => setSelectedUserId(id)}
          currentUserId={currentUser?.userId}
        />
      )}

      {/* Add Account Modal */}
      <AddAccountDialog
        open={addDialogOpen}
        onClose={() => setAddDialogOpen(false)}
        onSubmit={handleCreateAccount}
        loading={addLoading}
        defaultRole={groupFilter}
      />

      {/* Account Detail Modal */}
      <AccountDetailModal
        userId={selectedUserId}
        onClose={() => setSelectedUserId(null)}
        headTrainers={headTrainers}
        onUpdated={() => {
          void fetchAccounts();
          toast.success('Account profile updated successfully.');
        }}
      />
    </ScreenLayout>
  );
}
