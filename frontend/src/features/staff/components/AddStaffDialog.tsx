'use client';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Button, FormField, Input, Modal, Notice, Select } from '@/components/ui';
import { cn } from '@/lib/cn';
import { displayError } from '@/lib/display';
import { localToday, validateStaffCreation, type StaffCreationErrors } from '../lib/creation';
import type { StaffCreationRequest, StaffCreationResponse } from '../types';
import { StaffCreationResult } from './StaffCreationResult';

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (request: StaffCreationRequest) => Promise<StaffCreationResponse | null>;
  loading: boolean;
}
export function AddStaffDialog(props: Props) {
  return props.open ? <StaffCreationSession {...props} /> : null;
}
function StaffCreationSession({ onClose, onSubmit, loading }: Props) {
  const [draft, setDraft] = useState<StaffCreationRequest>({ role: 'GROOM', fullName: '', email: '', password: '' });
  const [errors, setErrors] = useState<StaffCreationErrors>({});
  const [error, setError] = useState('');
  const [result, setResult] = useState<StaffCreationResponse | null>(null);
  const pending = useRef(false);
  const pendingFocus = useRef<string | null>(null);
  useEffect(() => {
    if (!loading && pendingFocus.current) {
      document.getElementById(pendingFocus.current)?.focus();
      pendingFocus.current = null;
    }
  }, [loading, errors]);
  const change = (field: keyof StaffCreationRequest, value: string) => {
    setDraft(previous => ({ ...previous, [field]: value }));
    setErrors(previous => ({ ...previous, [field]: undefined }));
  };
  const input = (field: keyof StaffCreationRequest, label: string, options: { type?: string; required?: boolean; max?: number; autoComplete?: string } = {}) => (
    <FormField label={label} htmlFor={`staff-${field}`} required={options.required} error={errors[field]}>
      <Input type={options.type ?? 'text'} value={draft[field] ?? ''} disabled={loading} maxLength={options.max}
        max={options.type === 'date' ? localToday() : undefined} autoComplete={options.autoComplete}
        onChange={event => change(field, event.target.value)} />
    </FormField>
  );
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (loading || pending.current) return;
    const request: StaffCreationRequest = { role: draft.role, fullName: draft.fullName.trim(), email: draft.email.trim().toLowerCase(),
      password: draft.password, phone: draft.phone?.trim() || undefined, address: draft.address?.trim() || undefined,
      ...(draft.role === 'HEAD_TRAINER' ? { certificationNumber: draft.certificationNumber?.trim(), certificationIssuedDate: draft.certificationIssuedDate || undefined } : {}),
      ...(draft.role === 'VETERINARIAN' ? { licenseNumber: draft.licenseNumber?.trim(), licenseIssuedDate: draft.licenseIssuedDate || undefined, specialization: draft.specialization?.trim() || undefined } : {}),
    };
    const nextErrors = validateStaffCreation(request);
    setErrors(nextErrors); setError('');
    const first = Object.keys(nextErrors)[0];
    if (first) { document.getElementById(`staff-${first}`)?.focus(); return; }
    pending.current = true;
    try {
      const created = await onSubmit(request);
      if (!created) throw new Error('Unable to create staff.');
      setResult(created);
    } catch (cause) {
      const message = displayError(cause, 'Unable to create staff.');
      if (message.toLowerCase().includes('email already')) {
        pendingFocus.current = 'staff-email'; setErrors({ email: message });
      } else setError(message);
    } finally { pending.current = false; }
  }
  if (result) return <StaffCreationResult result={result} onClose={onClose} />;
  const groom = draft.role === 'GROOM';
  return <Modal open title="Add staff member" size={groom ? 'md' : 'lg'} onClose={onClose} dismissible={!loading}
    footer={<><Button variant="secondary" disabled={loading} onClick={onClose}>Cancel</Button>
      <Button type="submit" form="add-staff-form" variant="primary" loading={loading}>Create account</Button></>}>
    <form id="add-staff-form" onSubmit={submit} noValidate className="space-y-5">
      {error && <Notice tone="error">{error}</Notice>}
      <FormField label="Role" htmlFor="staff-role" required>
        <Select value={draft.role} disabled={loading} onChange={event => {
          change('role', event.target.value); setErrors({}); setError('');
        }}><option value="GROOM">Groom</option><option value="HEAD_TRAINER">Head Trainer</option><option value="VETERINARIAN">Veterinarian</option></Select>
      </FormField>
      <div className={cn('grid gap-6', !groom && 'grid-cols-2')}>
        <div className="space-y-4">
          {input('fullName', 'Full name', { required: true, max: 255, autoComplete: 'name' })}
          {input('email', 'Email', { type: 'email', required: true, max: 255, autoComplete: 'off' })}
          <div className={cn('grid gap-4', groom && 'grid-cols-2')}>
            {input('password', 'Password', { type: 'password', required: true, max: 72, autoComplete: 'new-password' })}
            {input('phone', 'Phone', { type: 'tel', max: 20, autoComplete: 'tel' })}
          </div>
          {input('address', 'Address', { max: 255, autoComplete: 'street-address' })}
        </div>
        {!groom && <div className="space-y-4 border-l border-[var(--color-border)] pl-6">
          <h3 className="text-sm font-semibold">{draft.role === 'HEAD_TRAINER' ? 'Certification' : 'Professional details'}</h3>
          {draft.role === 'HEAD_TRAINER' ? <>
            {input('certificationNumber', 'Certification number', { required: true, max: 100 })}
            {input('certificationIssuedDate', 'Issued date', { type: 'date' })}
          </> : <>
            {input('licenseNumber', 'License number', { required: true, max: 100 })}
            {input('licenseIssuedDate', 'Issued date', { type: 'date' })}
            {input('specialization', 'Specialization', { max: 255 })}
          </>}
        </div>}
      </div>
    </form>
  </Modal>;
}
