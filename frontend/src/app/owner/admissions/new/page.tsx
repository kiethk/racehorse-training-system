'use client';

import { useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { Panel } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { ownerAdmissionApi, type CandidateForm } from '@/features/admissions/services/ownerApi';

type Editable = Record<keyof CandidateForm, string>;
const initial: Editable = {
  name: '', breed: '', dateOfBirth: '', registrationNumber: '', registryName: '',
  sireName: '', sireRegistrationNumber: '', damName: '', damRegistrationNumber: '', pedigreeNotes: '',
};
const fields: {key: keyof CandidateForm; label: string; type?: string; required?: boolean}[] = [
  {key:'name', label:'Horse name', required:true}, {key:'breed', label:'Breed'},
  {key:'dateOfBirth', label:'Date of birth', type:'date'},
  {key:'registrationNumber', label:'UELN / Registration number (15 characters)'},
  {key:'registryName', label:'Registry name'}, {key:'sireName', label:'Sire name'},
  {key:'sireRegistrationNumber', label:'Sire registration number (15 characters)'},
  {key:'damName', label:'Dam name'},
  {key:'damRegistrationNumber', label:'Dam registration number (15 characters)'},
];

export default function NewOwnerAdmissionPage() {
  const router = useRouter();
  const [form, setForm] = useState<Editable>(initial);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const update = (key: keyof CandidateForm, value: string) => setForm(old => ({...old, [key]: value}));

  async function submit(e: FormEvent) {
    e.preventDefault(); setBusy(true); setError('');
    try {
      const payload = Object.fromEntries(Object.entries(form).map(([k,v]) => [k, v.trim() || null])) as CandidateForm;
      const result = await ownerAdmissionApi.create(payload);
      router.push(`/owner/admissions/${result.admissionId}?created=1`);
    } catch (err) { setError(err instanceof Error ? err.message : 'Unable to submit admission'); }
    finally { setBusy(false); }
  }
  return <RoleGuard allowedRoles={['HORSE_OWNER']}><AppShell><PageContainer>
    <div className="max-w-3xl space-y-5">
      <Link href="/owner/admissions" className="text-sm underline">← My admissions</Link>
      <div><h1 className="text-2xl font-semibold">New horse admission</h1>
        <p className="text-sm text-[var(--color-text-secondary)]">Create an application. You can add documents immediately after submission.</p></div>
      <form onSubmit={submit} className="space-y-4">
        <Panel padded><h2 className="font-semibold mb-4">Candidate horse and pedigree</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {fields.map(f => <label key={f.key} className="block text-sm">
              <span className="block mb-1">{f.label}{f.required ? ' *' : ''}</span>
              <input className="w-full rounded border border-[var(--color-border-strong)] p-2 bg-white"
                value={form[f.key]} required={f.required} type={f.type || 'text'}
                maxLength={f.key.toLowerCase().includes('registrationnumber') ? 15 : 255}
                pattern={f.key.toLowerCase().includes('registrationnumber') ? '[A-Za-z0-9]{15}' : undefined}
                onChange={e => update(f.key,e.target.value)} />
            </label>)}
          </div><label className="block text-sm mt-4"><span className="block mb-1">Pedigree notes</span>
            <textarea className="w-full rounded border border-[var(--color-border-strong)] p-2"
              rows={4} maxLength={2000} value={form.pedigreeNotes}
              onChange={e => update('pedigreeNotes', e.target.value)} /></label>
        </Panel>
        {error && <p role="alert" className="text-[var(--color-danger)]">{error}</p>}
        <Button type="submit" variant="primary" loading={busy}>Submit admission</Button>
      </form>
    </div>
  </PageContainer></AppShell></RoleGuard>;
}
