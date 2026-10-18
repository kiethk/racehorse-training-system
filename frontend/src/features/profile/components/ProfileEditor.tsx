'use client';

import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { AvatarUpload, Button, FormField, Input, Notice, Panel, Pill, SectionTitle, Textarea } from '@/components/ui';
import { displayError, formatDate } from '@/lib/display';
import { ROLE_LABELS } from '@/lib/roleRoute';
import { toast } from '@/lib/toast';
import type { AuthUser } from '@/types/auth';
import { profileService } from '../services/profileService';

interface Props {
  profile: AuthUser;
  onSaved: (profile: AuthUser) => void;
  onReset: () => void;
}

export function ProfileEditor({ profile, onSaved, onReset }: Props) {
  const [fullName, setFullName] = useState(profile.fullName);
  const [phone, setPhone] = useState(profile.phone ?? '');
  const [address, setAddress] = useState(profile.address ?? '');
  const [photo, setPhoto] = useState<{ file: File; preview: string } | null>(null);
  const [removeAvatar, setRemoveAvatar] = useState(false);
  const [photoError, setPhotoError] = useState<string>();
  const [nameError, setNameError] = useState<string>();
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const dirty = fullName !== profile.fullName || phone !== (profile.phone ?? '') || address !== (profile.address ?? '') || !!photo || removeAvatar;
  const preview = photo?.preview ?? (removeAvatar ? null : profile.avatarUrl);

  useEffect(() => () => { if (photo) URL.revokeObjectURL(photo.preview); }, [photo]);

  function choosePhoto(event: ChangeEvent<HTMLInputElement>) {
    const file = event.currentTarget.files?.[0];
    if (!file) return;
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024 || file.size === 0) {
      event.currentTarget.value = '';
      setPhotoError('Choose a JPG or PNG image up to 5 MB.');
      return;
    }
    setPhotoError(undefined);
    setRemoveAvatar(false);
    setPhoto({ file, preview: URL.createObjectURL(file) });
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving || !dirty || photoError) return;
    if (!fullName.trim()) {
      setNameError('Full name is required.');
      document.getElementById('profile-full-name')?.focus();
      return;
    }
    setError(null);
    setSaving(true);
    try {
      const response = await profileService.update({ fullName: fullName.trim(), phone: phone.trim(), address: address.trim(), removeAvatar }, photo?.file ?? null);
      if (!response.success) throw new Error(response.message || 'Unable to save profile.');
      toast.success('Profile updated');
      onSaved(response.data);
    } catch (cause) {
      setError(displayError(cause, 'Unable to save profile.'));
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-5">
      {error && <Notice tone="error">{error}</Notice>}
      <Panel padded className="flex items-center gap-6 p-6">
        <AvatarUpload name={fullName || profile.fullName} image={preview} disabled={saving}
          onChange={choosePhoto} describedBy={photoError ? 'profile-photo-error' : undefined} />
        <div className="min-w-0 flex-1 space-y-3">
          <div className="space-y-1">
            <h2 className="truncate text-xl font-semibold text-[var(--color-text-primary)]">{profile.fullName}</h2>
            <p className="truncate text-sm text-[var(--color-text-secondary)]">{profile.email}</p>
            <Pill tone="neutral">{ROLE_LABELS[profile.role]}</Pill>
          </div>
          {(preview || photoError) && <Button type="button" variant="link" disabled={saving}
            onClick={() => { setPhoto(null); setPhotoError(undefined); setRemoveAvatar(!!profile.avatarUrl); }}>{preview ? 'Remove photo' : 'Clear selection'}</Button>}
          {photoError && <p id="profile-photo-error" role="alert" className="text-sm text-[var(--color-danger)]">{photoError}</p>}
        </div>
      </Panel>

      <div className="grid gap-5 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)]">
        <Panel padded className="space-y-5 p-6">
          <SectionTitle>Personal information</SectionTitle>
          <FormField label="Full name" htmlFor="profile-full-name" required error={nameError}>
            <Input value={fullName} maxLength={255} autoComplete="name" disabled={saving}
              onChange={event => { setFullName(event.target.value); setNameError(undefined); }} />
          </FormField>
          <FormField label="Phone number" htmlFor="profile-phone">
            <Input type="tel" value={phone} maxLength={20} autoComplete="tel" disabled={saving} onChange={event => setPhone(event.target.value)} />
          </FormField>
          <FormField label="Address" htmlFor="profile-address">
            <Textarea value={address} rows={3} maxLength={255} autoComplete="street-address" disabled={saving} onChange={event => setAddress(event.target.value)} />
          </FormField>
        </Panel>
        <Panel padded className="space-y-5 self-start p-6">
          <SectionTitle>Account</SectionTitle>
          <FormField label="Email" htmlFor="profile-email"><Input value={profile.email} readOnly type="email" /></FormField>
          <FormField label="Role" htmlFor="profile-role"><Input value={ROLE_LABELS[profile.role]} readOnly /></FormField>
          {profile.createdAt && <div className="space-y-1"><p className="text-xs font-medium text-[var(--color-text-secondary)]">Member since</p><p className="text-sm">{formatDate(profile.createdAt)}</p></div>}
          <ProfessionalDetails profile={profile} />
        </Panel>
      </div>
      <div className="flex justify-end gap-2">
        <Button type="button" onClick={onReset} disabled={!dirty || saving}>Cancel</Button>
        <Button type="submit" variant="primary" loading={saving} disabled={!dirty || !!photoError}>Save changes</Button>
      </div>
    </form>
  );
}

function ProfessionalDetails({ profile }: { profile: AuthUser }) {
  if (!profile.profile || typeof profile.profile !== 'object') return null;
  const details = profile.profile as Record<string, unknown>;
  const fields = profile.role === 'VETERINARIAN'
    ? [['License number', details.licenseNumber], ['Specialization', details.specialization]]
    : profile.role === 'HEAD_TRAINER' ? [['Certification number', details.certificationNumber]] : [];
  const visible = fields.filter((entry): entry is [string, string] => typeof entry[1] === 'string' && !!entry[1]);
  if (!visible.length) return null;
  return <dl className="space-y-4 border-t border-[var(--color-border)] pt-5">{visible.map(([label, value]) => (
    <div key={label}><dt className="text-xs font-medium text-[var(--color-text-secondary)]">{label}</dt><dd className="mt-1 break-words text-sm">{value}</dd></div>
  ))}</dl>;
}
