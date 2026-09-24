'use client';

import { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { Panel } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { ownerAdmissionApi, type OwnerAdmissionDetail } from '@/features/admissions/services/ownerApi';

const TYPES = [
  'HORSE_PHOTO','REGISTRATION_DOCUMENT','PEDIGREE_CERTIFICATE','VACCINATION_RECORD',
  'DEWORMING_RECORD','HEALTH_CERTIFICATE','PREVIOUS_MEDICAL_RECORD','PREVIOUS_INJURY_RECORD'
];

export default function OwnerAdmissionDetailPage() {
  const params = useParams();
  const search = useSearchParams();
  const id = Number(params.id);
  const [detail, setDetail] = useState<OwnerAdmissionDetail | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [type, setType] = useState(TYPES[0]);
  const [date, setDate] = useState('');
  const [note, setNote] = useState('');
  useEffect(() => {
    if (!Number.isSafeInteger(id) || id < 1) return;
    ownerAdmissionApi.detail(id).then(setDetail).catch(e=>setError(String(e)));
  }, [id]);
  async function upload(e: React.FormEvent) {
    e.preventDefault(); if (!file) return;
    setBusy(true); setError('');
    try {
      await ownerAdmissionApi.upload(id, file, type, date, note);
      setDetail(await ownerAdmissionApi.detail(id)); setFile(null); setNote(''); setDate('');
      const field = document.getElementById('document-file') as HTMLInputElement | null;
      if (field) field.value = '';
    } catch (err) { setError(err instanceof Error ? err.message : 'Upload failed'); }
    finally { setBusy(false); }
  }
  const locked = detail?.status !== 'GROOM_REVIEW';
  const apiRoot = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  return <RoleGuard allowedRoles={['HORSE_OWNER']}><AppShell><PageContainer>
    <div className="max-w-4xl space-y-5">
      <Link href="/owner/admissions" className="underline text-sm">← My admissions</Link>
      {search.get('created') === '1' && <p className="rounded bg-[var(--color-success-soft)] p-3">Admission submitted. Add supporting documents below.</p>}
      {error && <p role="alert" className="text-[var(--color-danger)]">{error}</p>}
      {!detail ? <p>Loading admission…</p> : <>
        <header><h1 className="text-2xl font-semibold">{detail.candidate.name}</h1>
          <p>Application #{detail.admissionId} · {detail.status.replaceAll('_',' ')}</p></header>
        <Panel padded><h2 className="font-semibold mb-3">Submitted horse profile</h2>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
            {Object.entries(detail.candidate).map(([key,value]) =>
              <div key={key}><dt className="font-semibold">{key.replace(/([A-Z])/g,' $1')}</dt>
                <dd>{value || 'Not provided'}</dd></div>)}
          </dl></Panel>
        <Panel padded><h2 className="font-semibold mb-3">Admission documents</h2>
          {detail.documents.length === 0 ? <p className="text-sm mb-4">No documents uploaded.</p> :
            <ul className="space-y-2 mb-4">{detail.documents.map(d =>
              <li key={d.id} className="flex justify-between gap-3 border-b border-[var(--color-border)] py-2">
                <span>{d.documentType.replaceAll('_',' ')}</span>
                <a className="underline text-[var(--color-primary)]" href={d.fileUrl.startsWith('/') ? apiRoot+d.fileUrl : d.fileUrl}
                  target="_blank" rel="noopener noreferrer">View / download</a>
              </li>)}</ul>}
          {!locked ? <form onSubmit={upload} className="space-y-3 border-t border-[var(--color-border)] pt-4">
            <p className="text-sm">Add files before Groom begins reviewing your application.</p>
            <label className="block text-sm">Document type
              <select className="block w-full border rounded p-2" value={type} onChange={e=>setType(e.target.value)}>
                {TYPES.map(t=><option key={t} value={t}>{t.replaceAll('_',' ')}</option>)}</select></label>
            <label className="block text-sm">File (PDF or image, maximum 10 MB)
              <input id="document-file" required type="file" accept=".pdf,.png,.jpg,.jpeg,.webp"
                className="block mt-1" onChange={e=>setFile(e.target.files?.[0] || null)}/></label>
            <label className="block text-sm">Record date
              <input type="date" className="block border rounded p-2" value={date}
                onChange={e=>setDate(e.target.value)}/></label>
            <label className="block text-sm">Note
              <textarea maxLength={2000} className="block w-full border rounded p-2" value={note}
                onChange={e=>setNote(e.target.value)}/></label>
            <Button type="submit" variant="primary" disabled={!file} loading={busy}>Upload document</Button>
          </form> : <p className="text-sm">Documents are locked for review.</p>}
        </Panel>
        <Panel padded><h2 className="font-semibold mb-3">Review progress</h2>
          {(['groomFeedback','vetFeedback','trainerFeedback','managerFeedback'] as const).map(k =>
            <p className="text-sm mb-2" key={k}><strong>{k.replace('Feedback','')}:</strong> {detail[k] || 'Pending'}</p>)}
        </Panel>
      </>}
    </div>
  </PageContainer></AppShell></RoleGuard>;
}
