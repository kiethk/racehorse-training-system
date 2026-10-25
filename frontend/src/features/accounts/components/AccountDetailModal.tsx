'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Button, FormField, Icon, Input, Modal, Notice, Pill, Select, Spinner, StatusBadge } from '@/components/ui';
import { displayError, formatDateTime, formatEnumLabel } from '@/lib/display';
import { getAccountDetail, updateAccount } from '../services/accountService';
import type { AccountDetailResponse, AccountSummary, AccountUpdateRequest } from '../types';

interface AccountDetailModalProps {
  userId: number | null;
  onClose: () => void;
  onUpdated?: () => void;
  headTrainers: AccountSummary[];
}

const readonlyCls = 'text-sm font-medium text-[var(--color-text-primary)]';
const labelCls = 'mb-1 text-xs font-medium text-[var(--color-text-muted)]';

interface EditFormState {
  fullName: string;
  phone: string;
  address: string;
  // Vet
  licenseNumber: string;
  licenseIssuedDate: string;
  specialization: string;
  // Trainer
  certificationNumber: string;
  certificationIssuedDate: string;
  // Groom — '' means "Unassigned"
  trainerId: string;
}

function initForm(detail: AccountDetailResponse): EditFormState {
  const p = detail.profile as Record<string, unknown> | undefined;
  return {
    fullName: detail.fullName ?? '',
    phone: detail.phone ?? '',
    address: detail.address ?? '',
    licenseNumber: String(p?.licenseNumber ?? ''),
    licenseIssuedDate: String(p?.licenseIssuedDate ?? ''),
    specialization: String(p?.specialization ?? ''),
    certificationNumber: String(p?.certificationNumber ?? ''),
    certificationIssuedDate: String(p?.certificationIssuedDate ?? ''),
    trainerId: p?.trainerId != null ? String(p.trainerId) : '',
  };
}

export function AccountDetailModal({
  userId,
  onClose,
  onUpdated,
  headTrainers,
}: AccountDetailModalProps) {
  const [detail, setDetail] = useState<AccountDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [editMode, setEditMode] = useState(false);
  const [form, setForm] = useState<EditFormState | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');

  async function fetchDetail(id: number) {
    try {
      setLoading(true);
      setError('');
      const res = await getAccountDetail(id);
      setDetail(res.data);
    } catch (err: unknown) {
      setError(displayError(err, 'Unable to load account details.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (userId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setEditMode(false);
      setForm(null);
      setSaveError('');
      fetchDetail(userId);
    } else {
      setDetail(null);
      setError('');
    }
  }, [userId]);

  function handleStartEdit() {
    if (!detail) return;
    setForm(initForm(detail));
    setEditMode(true);
    setSaveError('');
  }

  function handleCancelEdit() {
    setEditMode(false);
    setForm(null);
    setSaveError('');
  }

  async function handleSave() {
    if (!detail || !form || !userId) return;
    setSaving(true);
    setSaveError('');
    try {
      const req: AccountUpdateRequest = {
        fullName: form.fullName || undefined,
        phone: form.phone || undefined,
        address: form.address || undefined,
      };

      if (detail.role === 'VETERINARIAN') {
        req.licenseNumber = form.licenseNumber || undefined;
        req.licenseIssuedDate = form.licenseIssuedDate || undefined;
        req.specialization = form.specialization || undefined;
      } else if (detail.role === 'HEAD_TRAINER') {
        req.certificationNumber = form.certificationNumber || undefined;
        req.certificationIssuedDate = form.certificationIssuedDate || undefined;
      } else if (detail.role === 'GROOM') {
        req.trainerIdProvided = true;
        req.trainerId = form.trainerId !== '' ? Number(form.trainerId) : null;
      }

      const res = await updateAccount(userId, req);
      setDetail(res.data);
      setEditMode(false);
      setForm(null);
      onUpdated?.();
    } catch (err: unknown) {
      setSaveError(err instanceof Error ? err.message : 'Failed to save changes.');
    } finally {
      setSaving(false);
    }
  }

  if (!userId) return null;

  function field(label: string, value: string, editNode?: React.ReactNode) {
    if (editMode && editNode) return <FormField label={label}>{editNode}</FormField>;
    return (
      <div>
        <div className={labelCls}>{label}</div>
        <div className={readonlyCls}>{value || '—'}</div>
      </div>
    );
  }

  const isStaffRole =
    detail?.role === 'GROOM' || detail?.role === 'HEAD_TRAINER' || detail?.role === 'VETERINARIAN';
  const isOwner = detail?.role === 'HORSE_OWNER';
  const isAdmin = detail?.role === 'CLUB_MANAGER';

  return (
    <Modal
      open
      onClose={onClose}
      size="lg"
      dismissible={!saving}
      title={editMode ? 'Edit Account Profile' : 'Account Details'}
      footer={
        !loading && !error && detail ? (
          editMode ? (
            <>
              <Button variant="secondary" onClick={handleCancelEdit} disabled={saving}>
                Cancel
              </Button>
              <Button variant="primary" loading={saving} onClick={() => void handleSave()}>
                Save Changes
              </Button>
            </>
          ) : (
            <>
              <Button variant="secondary" onClick={onClose}>
                Close
              </Button>
              <Button variant="primary" onClick={handleStartEdit}>
                Edit Profile
              </Button>
            </>
          )
        ) : undefined
      }
    >
      <div>
        {loading && (
          <div className="flex h-36 items-center justify-center">
            <Spinner />
          </div>
        )}

        {error && <Notice tone="error">{error}</Notice>}
        {saveError && <Notice tone="error" className="mb-4">{saveError}</Notice>}

        {!loading && !error && detail && (
          <div className="space-y-6">
            {/* Primary Profile Details */}
            <div>
              <h3 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">
                General Information
              </h3>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                {field(
                  'Full Name',
                  detail.fullName,
                  <Input
                    value={form?.fullName ?? ''}
                    onChange={(e) => setForm((f) => (f ? { ...f, fullName: e.target.value } : f))}
                    disabled={saving}
                  />
                )}

                <div>
                  <div className={labelCls}>Email Address</div>
                  <div className={readonlyCls}>{detail.email}</div>
                </div>

                {field(
                  'Phone Number',
                  detail.phone ?? '',
                  <Input
                    value={form?.phone ?? ''}
                    onChange={(e) => setForm((f) => (f ? { ...f, phone: e.target.value } : f))}
                    disabled={saving}
                  />
                )}

                <div>
                  <div className={labelCls}>Role</div>
                  <div className="mt-0.5">
                    <span className="text-sm font-medium text-[var(--color-text-primary)]">
                      {formatEnumLabel(detail.role)}
                    </span>
                  </div>
                </div>

                <div>
                  <div className={labelCls}>Account Status</div>
                  <div className="mt-0.5">
                    <Pill tone={detail.active ? 'success' : 'neutral'}>
                      {detail.active ? 'Active' : 'Inactive'}
                    </Pill>
                  </div>
                </div>

                <div>
                  <div className={labelCls}>Registered Date</div>
                  <div className={readonlyCls}>
                    {detail.createdAt ? formatDateTime(detail.createdAt) : '—'}
                  </div>
                </div>

                <div className="sm:col-span-2">
                  {field(
                    'Mailing Address',
                    detail.address ?? '',
                    <Input
                      value={form?.address ?? ''}
                      onChange={(e) => setForm((f) => (f ? { ...f, address: e.target.value } : f))}
                      disabled={saving}
                    />
                  )}
                </div>
              </div>
            </div>

            <div className="h-px bg-[var(--color-border)]" />

            {/* Role-Specific Context Section */}
            {isOwner && (
              <div>
                <div className="mb-3 flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
                    Owned Racehorses ({detail.ownedHorses?.length ?? 0})
                  </h3>
                  <Link
                    href="/manager/horses"
                    className="text-xs font-medium text-[var(--color-primary)] hover:underline"
                  >
                    View All Horses →
                  </Link>
                </div>

                {!detail.ownedHorses || detail.ownedHorses.length === 0 ? (
                  <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4 text-center text-sm text-[var(--color-text-secondary)]">
                    No racehorses are currently registered under this owner account.
                  </div>
                ) : (
                  <div className="divide-y divide-[var(--color-border)] rounded-lg border border-[var(--color-border)] overflow-hidden">
                    {detail.ownedHorses.map((horse) => (
                      <Link
                        key={horse.id}
                        href={`/manager/horses/${horse.id}`}
                        onClick={onClose}
                        className="group flex items-center justify-between p-3 text-sm transition-colors hover:bg-[var(--color-surface-hover)] cursor-pointer"
                      >
                        <div className="min-w-0 pr-3">
                          <div className="font-semibold text-[var(--color-text-primary)] transition-colors group-hover:text-[var(--color-primary)]">
                            {horse.name}
                          </div>
                          <div className="text-xs text-[var(--color-text-muted)]">
                            UELN: {horse.registrationNumber || 'N/A'} • Stall: {horse.stableLocation || 'Unassigned'}
                          </div>
                        </div>
                        <div className="flex items-center gap-2 shrink-0">
                          {['CANDIDATE', 'ELIGIBLE', 'REJECTED'].includes(horse.currentStatus ?? '') ? (
                            <StatusBadge status={horse.currentStatus as 'CANDIDATE' | 'ELIGIBLE' | 'REJECTED'} size="sm" />
                          ) : (
                            <Pill tone="neutral" size="sm">
                              {formatEnumLabel(horse.currentStatus || 'UNKNOWN')}
                            </Pill>
                          )}
                          <Icon
                            name="chevron-right"
                            size={16}
                            className="text-[var(--color-text-muted)] transition-transform group-hover:translate-x-0.5 group-hover:text-[var(--color-primary)]"
                          />
                        </div>
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            )}

            {isAdmin && (
              <div>
                <h3 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">
                  Administrator Privileges
                </h3>
                <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4 text-sm text-[var(--color-text-secondary)]">
                  This user possesses full managerial and administrative access over admissions, horses, stables, staff accounts, and system audit logs.
                </div>
              </div>
            )}

            {isStaffRole && (
              <div>
                <h3 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">
                  Role-Specific Profile
                </h3>

                {detail.role === 'GROOM' && (
                  <div className="space-y-4">
                    <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4 space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
                          Facility & Stalls Allocation
                        </span>
                        <div className="flex items-center gap-2">
                          <Pill tone={detail.assignmentStatus === 'ASSIGNED' ? 'success' : 'neutral'} size="sm">
                            {detail.assignmentStatus === 'ASSIGNED' ? 'Assigned' : 'Unassigned'}
                          </Pill>
                          <Link
                            href="/manager/stable"
                            className="text-xs font-medium text-[var(--color-primary)] hover:underline"
                          >
                            Stable Map →
                          </Link>
                        </div>
                      </div>

                      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 text-sm">
                        <div>
                          <div className={labelCls}>Facility Type</div>
                          <div className={readonlyCls}>
                            {detail.assignedAreaType === 'QUARANTINE'
                              ? 'Quarantine Facility'
                              : detail.assignedAreaCode
                              ? 'Regular Barn Area'
                              : 'None'}
                          </div>
                        </div>

                        <div>
                          <div className={labelCls}>Barn Area</div>
                          <div className={readonlyCls}>
                            {detail.assignedAreaCode ? `Area ${detail.assignedAreaCode}` : 'Unassigned'}
                          </div>
                        </div>

                        <div className="sm:col-span-2">
                          <div className={labelCls}>Assigned Stalls</div>
                          <div className="mt-1 flex flex-wrap gap-1.5">
                            {detail.assignedStallCodes && detail.assignedStallCodes.length > 0 ? (
                              detail.assignedStallCodes.map((stall) => (
                                <Pill
                                  key={stall}
                                  tone={detail.assignedAreaType === 'QUARANTINE' ? 'warning' : 'primary'}
                                  size="sm"
                                >
                                  {stall}
                                </Pill>
                              ))
                            ) : (
                              <span className="text-sm text-[var(--color-text-muted)]">No stalls allocated</span>
                            )}
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                      {detail.assignedAreaType === 'QUARANTINE' ? (
                        <div className="sm:col-span-2">
                          <div className={labelCls}>Supervising Head Trainer</div>
                          <div className="mt-0.5 text-xs text-[var(--color-text-muted)]">
                            Not Applicable — Quarantine grooms operate independently under veterinary and facility protocols.
                          </div>
                        </div>
                      ) : editMode ? (
                        <FormField label="Assigned Head Trainer">
                          <Select
                            value={form?.trainerId ?? ''}
                            onChange={(e) =>
                              setForm((f) => (f ? { ...f, trainerId: e.target.value } : f))
                            }
                            disabled={saving}
                          >
                            <option value="">Unassigned</option>
                            {headTrainers.map((t) => (
                              <option key={t.userId} value={String(t.userId)}>
                                {t.fullName} (#{t.userId})
                              </option>
                            ))}
                          </Select>
                        </FormField>
                      ) : (
                        <div>
                          <div className={labelCls}>Assigned Head Trainer</div>
                          <div className={readonlyCls}>
                            {detail.trainerName
                              ? `${detail.trainerName} (#${detail.trainerId ?? ''})`
                              : (() => {
                                  const p = detail.profile as Record<string, unknown> | undefined;
                                  if (!p?.trainerId) return 'Not Assigned';
                                  const found = headTrainers.find((t) => t.userId === Number(p.trainerId));
                                  return found ? `${found.fullName} (#${found.userId})` : `#${String(p.trainerId)}`;
                                })()}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {detail.role === 'VETERINARIAN' && (
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    {field(
                      'License Number',
                      String((detail.profile as Record<string, unknown>)?.licenseNumber ?? ''),
                      <Input
                        value={form?.licenseNumber ?? ''}
                        onChange={(e) =>
                          setForm((f) => (f ? { ...f, licenseNumber: e.target.value } : f))
                        }
                        disabled={saving}
                      />
                    )}
                    {field(
                      'License Issued Date',
                      String((detail.profile as Record<string, unknown>)?.licenseIssuedDate ?? ''),
                      <Input
                        type="date"
                        value={form?.licenseIssuedDate ?? ''}
                        onChange={(e) =>
                          setForm((f) => (f ? { ...f, licenseIssuedDate: e.target.value } : f))
                        }
                        disabled={saving}
                      />
                    )}
                    {field(
                      'Clinical Specialization',
                      String((detail.profile as Record<string, unknown>)?.specialization ?? ''),
                      <Input
                        value={form?.specialization ?? ''}
                        onChange={(e) =>
                          setForm((f) => (f ? { ...f, specialization: e.target.value } : f))
                        }
                        disabled={saving}
                      />
                    )}
                  </div>
                )}

                {detail.role === 'HEAD_TRAINER' && (
                  <div className="space-y-4">
                    <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4 space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
                          Facility & Areas Allocation
                        </span>
                        <div className="flex items-center gap-2">
                          <Pill
                            tone={detail.managedAreaCodes && detail.managedAreaCodes.length > 0 ? 'success' : 'neutral'}
                            size="sm"
                          >
                            {detail.managedAreaCodes && detail.managedAreaCodes.length > 0
                              ? `${detail.managedAreaCodes.length} Areas Managed`
                              : 'Unassigned'}
                          </Pill>
                          <Link
                            href="/manager/stable"
                            className="text-xs font-medium text-[var(--color-primary)] hover:underline"
                          >
                            Stable Map →
                          </Link>
                        </div>
                      </div>

                      <div>
                        <div className={labelCls}>Managed Regular Barn Areas</div>
                        <div className="mt-1 flex flex-wrap gap-1.5">
                          {detail.managedAreaCodes && detail.managedAreaCodes.length > 0 ? (
                            detail.managedAreaCodes.map((code) => (
                              <Pill key={code} tone="primary" size="sm">
                                Area {code}
                              </Pill>
                            ))
                          ) : (
                            <span className="text-sm text-[var(--color-text-muted)]">
                              No regular barn areas currently assigned.
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                      {field(
                        'Certification Number',
                        String((detail.profile as Record<string, unknown>)?.certificationNumber ?? ''),
                        <Input
                          value={form?.certificationNumber ?? ''}
                          onChange={(e) =>
                            setForm((f) => (f ? { ...f, certificationNumber: e.target.value } : f))
                          }
                          disabled={saving}
                        />
                      )}
                      {field(
                        'Certification Issued Date',
                        String((detail.profile as Record<string, unknown>)?.certificationIssuedDate ?? ''),
                        <Input
                          type="date"
                          value={form?.certificationIssuedDate ?? ''}
                          onChange={(e) =>
                            setForm((f) => (f ? { ...f, certificationIssuedDate: e.target.value } : f))
                          }
                          disabled={saving}
                        />
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}
