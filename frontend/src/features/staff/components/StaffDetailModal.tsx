'use client';

import { useEffect, useState } from 'react';
import { Button, FormField, Input, Modal, Notice, Pill, Select, Spinner } from '@/components/ui';
import { displayError, formatDateTime, formatEnumLabel } from '@/lib/display';
import { getStaffDetail, updateStaff } from '../services/staffService';
import type { StaffDetailResponse, StaffSummary, StaffUpdateRequest } from '../types';

interface StaffDetailModalProps {
  userId: number | null;
  onClose: () => void;
  onUpdated?: () => void;
  headTrainers: StaffSummary[];
}

const readonlyCls = 'text-sm text-[var(--color-text-primary)]';
const labelCls = 'mb-0.5 text-xs text-[var(--color-text-muted)]';

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

function initForm(detail: StaffDetailResponse): EditFormState {
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

export function StaffDetailModal({ userId, onClose, onUpdated, headTrainers }: StaffDetailModalProps) {
  const [detail, setDetail] = useState<StaffDetailResponse | null>(null);
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
      const res = await getStaffDetail(id);
      setDetail(res.data);
    } catch (err: unknown) {
      setError(displayError(err, 'Unable to load staff details.'));
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
      const req: StaffUpdateRequest = {
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

      const res = await updateStaff(userId, req);
      setDetail(res.data);
      setEditMode(false);
      setForm(null);
      onUpdated?.();
    } catch (err: unknown) {
      setSaveError(err instanceof Error ? err.message : 'Failed to save changes');
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
        <div className={readonlyCls}>{value || '-'}</div>
      </div>
    );
  }

  return (
    <Modal
      open
      onClose={onClose}
      size="lg"
      dismissible={!saving}
      title={editMode ? 'Staff profile — editing' : 'Staff profile'}
      footer={!loading && !error && detail ? (
        editMode ? (
          <>
            <Button variant="secondary" onClick={handleCancelEdit} disabled={saving}>
              Cancel
            </Button>
            <Button variant="primary" loading={saving} onClick={() => { void handleSave(); }}>
              Save changes
            </Button>
          </>
        ) : (
          <>
            <Button variant="secondary" onClick={onClose}>
              Close
            </Button>
            <Button variant="primary" onClick={handleStartEdit}>
              Update
            </Button>
          </>
        )
      ) : undefined}
    >
        <div>
          {loading && (
            <div className="flex h-32 items-center justify-center">
              <Spinner />
            </div>
          )}

          {error && (
            <Notice tone="error">{error}</Notice>
          )}

          {saveError && (
            <Notice tone="error" className="mb-4">{saveError}</Notice>
          )}

          {!loading && !error && detail && (
            <div className="space-y-6">
              {/* User Information */}
              <div>
                <h3 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">User Information</h3>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {field('Full Name', detail.fullName, (
                    <Input
                                            value={form?.fullName ?? ''}
                      onChange={e => setForm(f => f ? { ...f, fullName: e.target.value } : f)}
                      disabled={saving}
                    />
                  ))}
                  {/* Email — read-only always */}
                  <div>
                    <div className={labelCls}>Email</div>
                    <div className={readonlyCls}>{detail.email}</div>
                  </div>
                  {field('Phone', detail.phone ?? '', (
                    <Input
                                            value={form?.phone ?? ''}
                      onChange={e => setForm(f => f ? { ...f, phone: e.target.value } : f)}
                      disabled={saving}
                    />
                  ))}
                  {/* Role — read-only always */}
                  <div>
                    <div className={labelCls}>Role</div>
                    <div className="mt-0.5">
                      <Pill tone="info">{formatEnumLabel(detail.role)}</Pill>
                    </div>
                  </div>
                  {/* Status — read-only, handled via table action */}
                  <div>
                    <div className={labelCls}>Status</div>
                    <div className="mt-0.5">
                      <Pill tone={detail.active ? 'success' : 'neutral'}>
                        {detail.active ? 'Active' : 'Inactive'}
                      </Pill>
                    </div>
                  </div>
                  <div>
                    <div className={labelCls}>Created At</div>
                    <div className={readonlyCls}>
                      {detail.createdAt ? formatDateTime(detail.createdAt) : '-'}
                    </div>
                  </div>
                  <div className="sm:col-span-2">
                    {field('Address', detail.address ?? '', (
                      <Input
                                                value={form?.address ?? ''}
                        onChange={e => setForm(f => f ? { ...f, address: e.target.value } : f)}
                        disabled={saving}
                      />
                    ))}
                  </div>
                </div>
              </div>

              <div className="h-px bg-[var(--color-border)]" />

              {/* Role-Specific Profile */}
              <div>
                <h3 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">Role-Specific Profile</h3>

                {!detail.profile && !editMode && (
                  <div className="text-sm italic text-[var(--color-text-secondary)]">
                    No profile information available.
                  </div>
                )}

                {detail.role === 'GROOM' && (
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    {editMode ? (
                      <FormField label="Assigned Head Trainer">
                        <Select
                          value={form?.trainerId ?? ''}
                          onChange={e => setForm(f => f ? { ...f, trainerId: e.target.value } : f)}
                          disabled={saving}
                        >
                          <option value="">Unassigned</option>
                          {headTrainers.map(t => (
                            <option key={t.userId} value={String(t.userId)}>
                              {t.fullName} — #{t.userId}
                            </option>
                          ))}
                        </Select>
                      </FormField>
                    ) : (
                      <div>
                        <div className={labelCls}>Assigned Head Trainer</div>
                        <div className={readonlyCls}>
                          {(() => {
                            const p = detail.profile as Record<string, unknown> | undefined;
                            if (!p?.trainerId) return 'Not Assigned';
                            const found = headTrainers.find(t => t.userId === Number(p.trainerId));
                            return found ? `${found.fullName} — #${found.userId}` : `#${String(p.trainerId)}`;
                          })()}
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {detail.role === 'VETERINARIAN' && (
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    {field('License Number', String((detail.profile as Record<string, unknown>)?.licenseNumber ?? ''), (
                      <Input
                                                value={form?.licenseNumber ?? ''}
                        onChange={e => setForm(f => f ? { ...f, licenseNumber: e.target.value } : f)}
                        disabled={saving}
                      />
                    ))}
                    {field('License Issued Date', String((detail.profile as Record<string, unknown>)?.licenseIssuedDate ?? ''), (
                      <Input
                        type="date"
                                                value={form?.licenseIssuedDate ?? ''}
                        onChange={e => setForm(f => f ? { ...f, licenseIssuedDate: e.target.value } : f)}
                        disabled={saving}
                      />
                    ))}
                    {field('Specialization', String((detail.profile as Record<string, unknown>)?.specialization ?? ''), (
                      <Input
                                                value={form?.specialization ?? ''}
                        onChange={e => setForm(f => f ? { ...f, specialization: e.target.value } : f)}
                        disabled={saving}
                      />
                    ))}
                  </div>
                )}

                {detail.role === 'HEAD_TRAINER' && (
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    {field('Certification Number', String((detail.profile as Record<string, unknown>)?.certificationNumber ?? ''), (
                      <Input
                                                value={form?.certificationNumber ?? ''}
                        onChange={e => setForm(f => f ? { ...f, certificationNumber: e.target.value } : f)}
                        disabled={saving}
                      />
                    ))}
                    {field('Certification Issued Date', String((detail.profile as Record<string, unknown>)?.certificationIssuedDate ?? ''), (
                      <Input
                        type="date"
                                                value={form?.certificationIssuedDate ?? ''}
                        onChange={e => setForm(f => f ? { ...f, certificationIssuedDate: e.target.value } : f)}
                        disabled={saving}
                      />
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
    </Modal>
  );
}
