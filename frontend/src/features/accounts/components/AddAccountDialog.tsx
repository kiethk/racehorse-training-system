'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Button, FormField, Input, Modal, Notice, Select } from '@/components/ui';
import { cn } from '@/lib/cn';
import { displayError } from '@/lib/display';
import type { AccountCreationRequest, AccountCreationResponse } from '../types';
import { AccountCreationResult } from './AccountCreationResult';

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (request: AccountCreationRequest) => Promise<AccountCreationResponse | null>;
  loading: boolean;
  defaultRole?: string;
}

export function AddAccountDialog(props: Props) {
  return props.open ? <AccountCreationSession {...props} /> : null;
}

function AccountCreationSession({ onClose, onSubmit, loading, defaultRole }: Props) {
  const [draft, setDraft] = useState<AccountCreationRequest>({
    role: defaultRole && defaultRole !== 'ALL' && defaultRole !== 'STAFF' ? defaultRole : 'GROOM',
    fullName: '',
    email: '',
    password: '',
  });
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const [error, setError] = useState('');
  const [result, setResult] = useState<AccountCreationResponse | null>(null);
  const [lastSubmittedPassword, setLastSubmittedPassword] = useState('');
  const pending = useRef(false);
  const pendingFocus = useRef<string | null>(null);

  useEffect(() => {
    if (!loading && pendingFocus.current) {
      document.getElementById(pendingFocus.current)?.focus();
      pendingFocus.current = null;
    }
  }, [loading, errors]);

  const change = (field: keyof AccountCreationRequest, value: string) => {
    setDraft((prev) => ({ ...prev, [field]: value }));
    setErrors((prev) => ({ ...prev, [field]: undefined }));
  };

  const isStaffRole =
    draft.role === 'GROOM' || draft.role === 'HEAD_TRAINER' || draft.role === 'VETERINARIAN';
  const isTrainer = draft.role === 'HEAD_TRAINER';
  const isVet = draft.role === 'VETERINARIAN';
  const hasSideSection = isTrainer || isVet;

  function validate(req: AccountCreationRequest): Record<string, string> {
    const errs: Record<string, string> = {};
    if (!req.fullName.trim()) errs.fullName = 'Full name is required.';
    if (!req.email.trim()) errs.email = 'Email address is required.';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(req.email.trim())) {
      errs.email = 'Please enter a valid email address.';
    }
    if (!req.password) errs.password = 'Password is required.';
    else if (req.password.length < 6) errs.password = 'Password must be at least 6 characters.';

    if (isVet && (!req.licenseNumber || !req.licenseNumber.trim())) {
      errs.licenseNumber = 'License number is required for Veterinarians.';
    }
    if (isTrainer && (!req.certificationNumber || !req.certificationNumber.trim())) {
      errs.certificationNumber = 'Certification number is required for Head Trainers.';
    }
    return errs;
  }

  const input = (
    field: keyof AccountCreationRequest,
    label: string,
    options: { type?: string; required?: boolean; max?: number; autoComplete?: string } = {}
  ) => (
    <FormField label={label} htmlFor={`account-${field}`} required={options.required} error={errors[field]}>
      <Input
        id={`account-${field}`}
        type={options.type ?? 'text'}
        value={(draft[field] as string) ?? ''}
        disabled={loading}
        maxLength={options.max}
        autoComplete={options.autoComplete}
        onChange={(event) => change(field, event.target.value)}
      />
    </FormField>
  );

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (loading || pending.current) return;

    const request: AccountCreationRequest = {
      role: draft.role,
      fullName: draft.fullName.trim(),
      email: draft.email.trim().toLowerCase(),
      password: draft.password,
      phone: draft.phone?.trim() || undefined,
      address: draft.address?.trim() || undefined,
      ...(isTrainer
        ? {
            certificationNumber: draft.certificationNumber?.trim(),
            certificationIssuedDate: draft.certificationIssuedDate || undefined,
          }
        : {}),
      ...(isVet
        ? {
            licenseNumber: draft.licenseNumber?.trim(),
            licenseIssuedDate: draft.licenseIssuedDate || undefined,
            specialization: draft.specialization?.trim() || undefined,
          }
        : {}),
    };

    const nextErrors = validate(request);
    setErrors(nextErrors);
    setError('');

    const first = Object.keys(nextErrors)[0];
    if (first) {
      document.getElementById(`account-${first}`)?.focus();
      return;
    }

    pending.current = true;
    try {
      const created = await onSubmit(request);
      if (!created) throw new Error('Unable to create account.');
      setLastSubmittedPassword(draft.password);
      setResult(created);
    } catch (cause) {
      const message = displayError(cause, 'Unable to create account.');
      if (message.toLowerCase().includes('email already')) {
        pendingFocus.current = 'account-email';
        setErrors({ email: message });
      } else {
        setError(message);
      }
    } finally {
      pending.current = false;
    }
  }

  if (result) {
    return (
      <AccountCreationResult
        result={result}
        initialPassword={lastSubmittedPassword}
        onClose={onClose}
      />
    );
  }

  return (
    <Modal
      open
      title="Create New Account"
      size={hasSideSection ? 'lg' : 'md'}
      onClose={onClose}
      dismissible={!loading}
      footer={
        <>
          <Button variant="secondary" disabled={loading} onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="add-account-form" variant="primary" loading={loading}>
            Create Account
          </Button>
        </>
      }
    >
      <form id="add-account-form" onSubmit={submit} noValidate className="space-y-5">
        {error && <Notice tone="error">{error}</Notice>}

        <FormField label="Account Role" htmlFor="account-role" required>
          <Select
            id="account-role"
            value={draft.role}
            disabled={loading}
            onChange={(event) => {
              change('role', event.target.value);
              setErrors({});
              setError('');
            }}
          >
            <optgroup label="Staff Members">
              <option value="GROOM">Groom</option>
              <option value="HEAD_TRAINER">Head Trainer</option>
              <option value="VETERINARIAN">Veterinarian</option>
            </optgroup>
            <optgroup label="External Clients & Administration">
              <option value="HORSE_OWNER">Horse Owner</option>
              <option value="CLUB_MANAGER">Club Manager (Administrator)</option>
            </optgroup>
          </Select>
        </FormField>

        {draft.role === 'CLUB_MANAGER' && (
          <Notice tone="warning">
            This account will have full administrative privileges across the entire platform.
          </Notice>
        )}

        {draft.role === 'HORSE_OWNER' && (
          <Notice tone="info">
            Horse Owner accounts can access their owned horses and register admission applications.
          </Notice>
        )}

        <div className={cn('grid gap-6', hasSideSection && 'grid-cols-2')}>
          <div className="space-y-4">
            {input('fullName', 'Full Name', { required: true, max: 255, autoComplete: 'name' })}
            {input('email', 'Email Address', { type: 'email', required: true, max: 255, autoComplete: 'off' })}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              {input('password', 'Initial Password', {
                type: 'password',
                required: true,
                max: 72,
                autoComplete: 'new-password',
              })}
              {input('phone', 'Phone Number', { type: 'tel', max: 20, autoComplete: 'tel' })}
            </div>
            {input('address', 'Mailing Address', { max: 255, autoComplete: 'street-address' })}
          </div>

          {hasSideSection && (
            <div className="space-y-4 border-l border-[var(--color-border)] pl-6">
              <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
                {isTrainer ? 'Trainer Certification' : 'Professional License'}
              </h3>
              {isTrainer ? (
                <>
                  {input('certificationNumber', 'Certification Number', { required: true, max: 100 })}
                  {input('certificationIssuedDate', 'Issued Date', { type: 'date' })}
                </>
              ) : (
                <>
                  {input('licenseNumber', 'License Number', { required: true, max: 100 })}
                  {input('licenseIssuedDate', 'Issued Date', { type: 'date' })}
                  {input('specialization', 'Clinical Specialization', { max: 255 })}
                </>
              )}
            </div>
          )}
        </div>
      </form>
    </Modal>
  );
}
