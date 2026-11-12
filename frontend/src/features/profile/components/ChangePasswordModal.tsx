'use client';

import { useState, type FormEvent } from 'react';
import { Button, FormField, Input, Modal, Notice } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { changePassword } from '@/services/auth';

interface Props {
  open: boolean;
  onClose: () => void;
}

export function ChangePasswordModal({ open, onClose }: Props) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<{ current?: string; new?: string; confirm?: string }>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  if (!open) return null;

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (loading) return;

    const errs: typeof errors = {};
    if (!currentPassword) errs.current = 'Current password is required.';
    if (!newPassword) errs.new = 'New password is required.';
    else if (newPassword.length < 8 || newPassword.length > 72) {
      errs.new = 'New password must be between 8 and 72 characters.';
    }
    if (newPassword && confirmPassword !== newPassword) {
      errs.confirm = 'Confirm password does not match new password.';
    }

    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setError(null);
    setLoading(true);

    try {
      const res = await changePassword({ currentPassword, newPassword });
      if (!res.success) {
        throw new Error(res.message || 'Failed to change password.');
      }
      toast.success('Password updated successfully.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setErrors({});
      setError(null);
      setLoading(false);
      onClose();
    } catch (cause) {
      setError(displayError(cause, 'Unable to change password.'));
      setLoading(false);
    }
  }

  return (
    <Modal
      open
      title="Change password"
      size="sm"
      onClose={onClose}
      dismissible={!loading}
      footer={
        <>
          <Button variant="secondary" disabled={loading} onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="change-password-form" variant="primary" loading={loading}>
            Update password
          </Button>
        </>
      }
    >
      <form id="change-password-form" onSubmit={submit} className="space-y-4" noValidate>
        {error && <Notice tone="error">{error}</Notice>}

        <FormField label="Current password" htmlFor="current-password" required error={errors.current}>
          <Input
            id="current-password"
            type="password"
            value={currentPassword}
            disabled={loading}
            autoComplete="current-password"
            onChange={e => {
              setCurrentPassword(e.target.value);
              setErrors(prev => ({ ...prev, current: undefined }));
            }}
          />
        </FormField>

        <FormField label="New password" htmlFor="new-password" required error={errors.new}>
          <Input
            id="new-password"
            type="password"
            value={newPassword}
            disabled={loading}
            maxLength={72}
            autoComplete="new-password"
            placeholder="Min 8 characters"
            onChange={e => {
              setNewPassword(e.target.value);
              setErrors(prev => ({ ...prev, new: undefined }));
            }}
          />
        </FormField>

        <FormField label="Confirm new password" htmlFor="confirm-password" required error={errors.confirm}>
          <Input
            id="confirm-password"
            type="password"
            value={confirmPassword}
            disabled={loading}
            maxLength={72}
            autoComplete="new-password"
            onChange={e => {
              setConfirmPassword(e.target.value);
              setErrors(prev => ({ ...prev, confirm: undefined }));
            }}
          />
        </FormField>
      </form>
    </Modal>
  );
}

