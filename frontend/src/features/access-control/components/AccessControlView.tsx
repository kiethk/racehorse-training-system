'use client';

import { useState, useEffect, useMemo } from 'react';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { accessControlService } from '../services/accessControlService';
import { AccessControlMatrixResponse } from '../types';
import { EmptyState, ListSkeleton } from '@/components/ui/states';

export function AccessControlView() {
  const [data, setData] = useState<AccessControlMatrixResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Map of roleId -> array of selected permission codes
  const [dirtyMatrix, setDirtyMatrix] = useState<Record<number, string[]>>({});
  
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    // eslint-disable-next-line react-hooks/immutability
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const matrixData = await accessControlService.getAccessControlMatrix();
      setData(matrixData);
      
      // Initialize dirtyMatrix with current state
      const initial: Record<number, string[]> = {};
      matrixData.roles.forEach(role => {
        initial[role.roleId] = [...role.permissionCodes];
      });
      setDirtyMatrix(initial);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to load access control data'));
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = (roleId: number, code: string, disabled: boolean) => {
    if (disabled) return;
    
    setSaveSuccess(false);
    setSaveError(null);

    setDirtyMatrix(prev => {
      const current = prev[roleId] || [];
      if (current.includes(code)) {
        return { ...prev, [roleId]: current.filter(c => c !== code) };
      }
      return { ...prev, [roleId]: [...current, code] };
    });
  };

  const isSafetyDisabled = (roleName: string, code: string) => {
    return roleName === 'CLUB_MANAGER' && code === 'USER_MANAGE';
  };

  // Compute dirty roles
  const dirtyRoles = useMemo(() => {
    if (!data) return [];
    
    return data.roles.filter(role => {
      const original = role.permissionCodes;
      const current = dirtyMatrix[role.roleId] || [];
      
      if (original.length !== current.length) return true;
      
      // Check if every original is in current
      return !original.every(code => current.includes(code));
    });
  }, [data, dirtyMatrix]);

  const handleSave = async () => {
    if (dirtyRoles.length === 0) return;
    
    try {
      setSaving(true);
      setSaveError(null);
      setSaveSuccess(false);
      
      // Save all dirty roles in parallel
      await Promise.all(
        dirtyRoles.map(role => 
          accessControlService.updateRolePermissions(role.roleId, {
            permissionCodes: dirtyMatrix[role.roleId]
          })
        )
      );
      
      // Refresh to get true original state
      await fetchData();
      setSaveSuccess(true);
      
      // Hide success message after 3 seconds
      setTimeout(() => {
        setSaveSuccess(false);
      }, 3000);
      
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      setSaveError(err.response?.data?.message || err.message || 'Failed to save access control changes');
    } finally {
      setSaving(false);
    }
  };

  // Filter permissions based on search
  const filteredPermissions = useMemo(() => {
    if (!data) return [];
    
    if (!searchTerm.trim()) {
      return data.permissions;
    }
    
    const lowerSearch = searchTerm.toLowerCase();
    return data.permissions.filter(p => 
      p.code.toLowerCase().includes(lowerSearch) || 
      (p.description && p.description.toLowerCase().includes(lowerSearch))
    );
  }, [data, searchTerm]);

  const formatRoleName = (name: string) => {
    return name.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ');
  };

  if (loading) return (
    <div className="space-y-4">
      <ListSkeleton rows={8} />
    </div>
  );
  
  if (error) return (
    <div className="space-y-4">
      <EmptyState 
        icon="alert-triangle" 
        title="Failed to load access control" 
        description={error.message}
        action={<Button variant="primary" onClick={fetchData}>Try Again</Button>} 
      />
    </div>
  );
  
  if (!data) return null;

  return (
    <div className="space-y-4">
      <Panel padded>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <SectionTitle>Role-based access control</SectionTitle>
            <p className="mt-1 text-[13px] text-[var(--color-text-secondary)] max-w-2xl">
              Manage what different roles can view and do within the system. Check a box to grant permission, or uncheck to revoke it.
            </p>
          </div>
          
          <div className="flex items-center gap-2 self-start">
            {saveSuccess && (
              <span className="text-[13px] font-medium text-[var(--color-success)] flex items-center gap-1">
                <Icon name="check" size={14} /> Saved
              </span>
            )}
            
            <Button 
              variant="primary" 
              onClick={handleSave}
              disabled={dirtyRoles.length === 0 || saving}
              loading={saving}
            >
              <Icon name="check" size={16} />
              Save Changes {dirtyRoles.length > 0 && `(${dirtyRoles.length})`}
            </Button>
          </div>
        </div>
        
        {saveError && (
          <div className="mt-4 rounded-md bg-[var(--color-danger-muted)] p-3 text-[13px] text-[var(--color-danger)]">
            <div className="flex items-start gap-2">
              <Icon name="alert-triangle" size={16} className="mt-0.5 shrink-0" />
              <span>{saveError}</span>
            </div>
          </div>
        )}
      </Panel>

      <Panel padded>
        <div className="mb-4 flex items-center">
          <div className="relative w-full max-w-md">
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-[var(--color-text-muted)]">
              <Icon name="search" size={16} />
            </div>
            <input
              type="text"
              placeholder="Search permissions by code or description..."
              className="h-9 w-full rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] pl-9 pr-3 text-[13px] text-[var(--color-text-primary)] placeholder-[var(--color-text-muted)] focus:border-[var(--color-primary)] focus:outline-none focus:ring-1 focus:ring-[var(--color-primary)]"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        <div className="overflow-x-auto rounded-md border border-[var(--color-border)]">
          <table className="w-full min-w-[800px] border-collapse text-left text-[13px]">
            <thead>
              <tr className="border-b border-[var(--color-border)] bg-[var(--color-surface-muted)]">
                <th className="p-3 font-semibold text-[var(--color-text-primary)] min-w-[250px]">
                  Permission
                </th>
                {data.roles.map(role => (
                  <th key={role.roleId} className="p-3 font-semibold text-[var(--color-text-primary)] text-center w-[120px]">
                    {formatRoleName(role.roleName)}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--color-border)]">
              {filteredPermissions.length === 0 ? (
                <tr>
                  <td colSpan={data.roles.length + 1} className="p-8 text-center text-[var(--color-text-muted)]">
                    No permissions found matching &quot;{searchTerm}&quot;
                  </td>
                </tr>
              ) : (
                filteredPermissions.map(permission => (
                  <tr key={permission.id} className="hover:bg-[var(--color-surface-muted)] transition-colors">
                    <td className="p-3">
                      <div className="font-medium text-[var(--color-text-primary)] font-mono text-[12px]">
                        {permission.code}
                      </div>
                      <div className="text-[12px] text-[var(--color-text-secondary)] mt-0.5">
                        {permission.description}
                      </div>
                    </td>
                    {data.roles.map(role => {
                      const isChecked = dirtyMatrix[role.roleId]?.includes(permission.code) || false;
                      const disabled = isSafetyDisabled(role.roleName, permission.code);
                      
                      return (
                        <td key={role.roleId} className="p-3 text-center align-middle">
                          <label 
                            className={`inline-flex cursor-pointer items-center justify-center p-1 ${disabled ? 'cursor-not-allowed opacity-50' : ''}`}
                            title={disabled ? "This permission is required to manage access control." : `Toggle ${permission.code} for ${formatRoleName(role.roleName)}`}
                          >
                            <input
                              type="checkbox"
                              className="h-4 w-4 rounded border-[var(--color-border)] text-[var(--color-primary)] focus:ring-[var(--color-primary)] cursor-pointer disabled:cursor-not-allowed"
                              checked={isChecked}
                              onChange={() => handleToggle(role.roleId, permission.code, disabled)}
                              disabled={disabled || saving}
                            />
                          </label>
                        </td>
                      );
                    })}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  );
}
