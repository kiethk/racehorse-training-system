'use client';

import { TBody, THead, Table, Td, Th, Tr } from '@/components/ui/Table';
import { useState, useEffect, useMemo, Fragment } from 'react';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Checkbox, SearchInput } from '@/components/ui/Input';
import { accessControlService } from '../services/accessControlService';
import { AccessControlMatrixResponse } from '../types';
import { EmptyState, ListSkeleton } from '@/components/ui/states';

const PERMISSION_GROUPS = [
  { key: 'user-account', label: 'User & Account Management' },
  { key: 'horse', label: 'Horse Management' },
  { key: 'pedigree', label: 'Pedigree' },
  { key: 'preventive-care', label: 'Preventive Care' },
  { key: 'medical', label: 'Medical & Veterinary' },
  { key: 'training', label: 'Training' },
  { key: 'admission', label: 'Admission Management' },
  { key: 'stable', label: 'Stable Management' },
  { key: 'groom', label: 'Groom Operations' },
  { key: 'race', label: 'Race Management' }
];

const OTHER_GROUP_KEY = 'other';
const OTHER_GROUP_LABEL = 'Other';

function getPermissionGroupKey(permissionCode: string): string {
  if (permissionCode.startsWith('USER_')) return 'user-account';
  
  if (permissionCode.startsWith('HORSE_PEDIGREE_')) return 'pedigree';
  
  if (permissionCode.startsWith('PREVENTIVE_CARE_')) return 'preventive-care';
  
  if (
    permissionCode.startsWith('HEALTH_RECORD_') ||
    permissionCode.startsWith('INJURY_RECORD_') ||
    permissionCode.startsWith('TREATMENT_PLAN_') ||
    permissionCode.startsWith('VET_EXAM_') ||
    permissionCode.startsWith('HORSE_HEALTH_') ||
    permissionCode.startsWith('HORSE_TRAINING_LOCK_') ||
    permissionCode.startsWith('HORSE_BODY_REGION_')
  ) {
    return 'medical';
  }
  
  if (permissionCode.startsWith('COURSE_') || permissionCode.startsWith('TRAINING_')) return 'training';
  
  if (permissionCode.startsWith('ADMISSION_')) return 'admission';
  
  if (permissionCode.startsWith('AREA_') || permissionCode.startsWith('STABLE_STALL_')) return 'stable';
  
  if (permissionCode.startsWith('GROOM_')) return 'groom';
  
  if (permissionCode.startsWith('RACE_REGISTRATION_')) return 'race';
  
  // Specific HORSE_* prefixes have already been caught above.
  if (permissionCode.startsWith('HORSE_')) return 'horse';
  
  return OTHER_GROUP_KEY;
}

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
  
  // Set of expanded group keys
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set());

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

  const groupedPermissions = useMemo(() => {
    if (!data) return [];
    
    // Group permissions
    const groupsMap = new Map<string, typeof data.permissions>();
    
    // Initialize groups
    PERMISSION_GROUPS.forEach(g => groupsMap.set(g.key, []));
    groupsMap.set(OTHER_GROUP_KEY, []);
    
    // Assign to groups
    data.permissions.forEach(p => {
      const key = getPermissionGroupKey(p.code);
      const list = groupsMap.get(key) || [];
      list.push(p);
      groupsMap.set(key, list);
    });

    const lowerSearch = searchTerm.trim().toLowerCase();
    const result = [];
    
    for (const group of PERMISSION_GROUPS) {
      const perms = groupsMap.get(group.key) || [];
      const matchedPerms = perms.filter(p => 
        !lowerSearch || 
        p.code.toLowerCase().includes(lowerSearch) || 
        (p.description && p.description.toLowerCase().includes(lowerSearch))
      );
      
      if (matchedPerms.length > 0) {
        result.push({
          key: group.key,
          label: group.label,
          permissions: matchedPerms,
          totalCount: perms.length
        });
      }
    }
    
    // Other group
    const otherPerms = groupsMap.get(OTHER_GROUP_KEY) || [];
    const matchedOther = otherPerms.filter(p => 
      !lowerSearch || 
      p.code.toLowerCase().includes(lowerSearch) || 
      (p.description && p.description.toLowerCase().includes(lowerSearch))
    );
    
    if (matchedOther.length > 0) {
      result.push({
        key: OTHER_GROUP_KEY,
        label: OTHER_GROUP_LABEL,
        permissions: matchedOther,
        totalCount: otherPerms.length
      });
    }
    
    return result;
  }, [data, searchTerm]);

  const isGroupCollapsed = (groupKey: string) => {
    if (searchTerm.trim()) {
      return false; // Force expand during search
    }
    return !expandedGroups.has(groupKey);
  };

  const toggleGroup = (groupKey: string) => {
    if (searchTerm.trim()) return; // Disable collapsing while searching
    setExpandedGroups(prev => {
      const next = new Set(prev);
      if (next.has(groupKey)) {
        next.delete(groupKey);
      } else {
        next.add(groupKey);
      }
      return next;
    });
  };

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
            <p className="mt-1 text-sm text-[var(--color-text-secondary)] max-w-2xl">
              Manage what different roles can view and do within the system. Check a box to grant permission, or uncheck to revoke it.
            </p>
          </div>
          
          <div className="flex items-center gap-2 self-start">
            {saveSuccess && (
              <span className="text-sm font-medium text-[var(--color-success)] flex items-center gap-1">
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
          <div className="mt-4 rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-sm text-[var(--color-danger)]">
            <div className="flex items-start gap-2">
              <Icon name="alert-triangle" size={16} className="mt-0.5 shrink-0" />
              <span>{saveError}</span>
            </div>
          </div>
        )}
      </Panel>

      <Panel padded>
        <SearchInput
          className="mb-4 max-w-md"
          value={searchTerm}
          onChange={setSearchTerm}
          placeholder="Search permissions by code or description..."
        />

        <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)]">
          <Table bare className="min-w-[800px]">
            <THead>
              <Tr className="border-b border-[var(--color-border)] bg-[var(--color-surface)]">
                <Th className="p-3 font-semibold text-[var(--color-text-primary)] min-w-[250px]">
                  Permission
                </Th>
                {data.roles.map(role => (
                  <Th key={role.roleId} className="p-3 font-semibold text-[var(--color-text-primary)] text-center w-[120px]">
                    {formatRoleName(role.roleName)}
                  </Th>
                ))}
              </Tr>
            </THead>
            <TBody>
              {groupedPermissions.length === 0 ? (
                <Tr>
                  <Td colSpan={data.roles.length + 1} className="p-8 text-center text-[var(--color-text-muted)]">
                    No permissions found matching &quot;{searchTerm}&quot;
                  </Td>
                </Tr>
              ) : (
                groupedPermissions.map(group => (
                  <Fragment key={group.key}>
                    <Tr 
                      className="bg-[var(--color-surface-muted)] hover:bg-[var(--color-surface-muted)]/80 transition-colors cursor-pointer"
                      onClick={() => toggleGroup(group.key)}
                    >
                      <Td colSpan={data.roles.length + 1} className="p-3">
                        <div className="flex items-center gap-2 font-semibold text-[var(--color-text-primary)]">
                          <Icon 
                            name={isGroupCollapsed(group.key) ? 'chevron-right' : 'chevron-down'} 
                            size={16} 
                            className="text-[var(--color-text-muted)]" 
                          />
                          {group.label} <span className="text-[var(--color-text-muted)] font-normal text-xs">({group.totalCount})</span>
                        </div>
                      </Td>
                    </Tr>
                    {!isGroupCollapsed(group.key) && group.permissions.map(permission => (
                      <Tr key={permission.id} className="hover:bg-[var(--color-surface-muted)] transition-colors">
                        <Td className="p-3 pl-8">
                          <div className="font-medium text-[var(--color-text-primary)] font-metric text-xs">
                            {permission.code}
                          </div>
                          <div className="text-xs text-[var(--color-text-secondary)] mt-0.5">
                            {permission.description}
                          </div>
                        </Td>
                        {data.roles.map(role => {
                          const isChecked = dirtyMatrix[role.roleId]?.includes(permission.code) || false;
                          const disabled = isSafetyDisabled(role.roleName, permission.code);
                          
                          return (
                            <Td key={role.roleId} className="p-3 text-center align-middle">
                              <Checkbox
                                className="inline-flex justify-center"
                                title={disabled ? 'This permission is required to manage access control.' : undefined}
                                label={<span className="sr-only">{`${permission.code} for ${formatRoleName(role.roleName)}`}</span>}
                                checked={isChecked}
                                onChange={() => handleToggle(role.roleId, permission.code, disabled)}
                                disabled={disabled || saving}
                              />
                            </Td>
                          );
                        })}
                      </Tr>
                    ))}
                  </Fragment>
                ))
              )}
            </TBody>
          </Table>
        </div>
      </Panel>
    </div>
  );
}
