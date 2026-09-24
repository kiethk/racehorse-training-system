'use client';

import { use, useEffect, useState } from 'react';
import Link from 'next/link';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { Panel } from '@/components/ui/Panel';
import { ownerAdmissionsApi } from '@/features/admissions/services/ownerApi';
import type { OwnerAdmissionDetail, AdmissionDocumentType } from '@/features/admissions/types/owner';

const documentTypes: AdmissionDocumentType[] = [
  'HORSE_PHOTO','REGISTRATION_DOCUMENT','PEDIGREE_CERTIFICATE','VACCINATION_RECORD',
  'DEWORMING_RECORD','HEALTH_CERTIFICATE','PREVIOUS_MEDICAL_RECORD','PREVIOUS_INJURY_RECORD',
];
const apiUrl = process.env.NEXT_PUBLIC_API_URL || '';

export default function OwnerAdmissionDetailPage({params}:{params:Promise<{id:string}>}) {
  const {id}=use(params);
  const admissionId=Number(id);
  const [detail,setDetail]=useState<OwnerAdmissionDetail|null>(null);
  const [error,setError]=useState('');
  const [uploadError,setUploadError]=useState('');
  const [uploading,setUploading]=useState(false);
  const [file,setFile]=useState<File|null>(null);
  const [type,setType]=useState<AdmissionDocumentType>('HORSE_PHOTO');
  const [loading,setLoading]=useState(true);
  useEffect(()=>{
    if(!Number.isSafeInteger(admissionId)||admissionId<=0){setError('Invalid admission ID');setLoading(false);return;}
    ownerAdmissionsApi.detail(admissionId).then(setDetail)
      .catch(e=>setError(e instanceof Error?e.message:'Unable to load admission'))
      .finally(()=>setLoading(false));
  },[admissionId]);
  async function upload(){
    if(!file) return;
    setUploading(true);setUploadError('');
    try{
      await ownerAdmissionsApi.uploadDocument(admissionId,file,type);
      setFile(null);
      setDetail(await ownerAdmissionsApi.detail(admissionId));
    }catch(e){setUploadError(e instanceof Error?e.message:'Upload failed');}
    finally{setUploading(false);}
  }
  const fields: {key:keyof NonNullable<OwnerAdmissionDetail['candidate']>; label:string}[]=[
    {key:'name',label:'Name'},{key:'breed',label:'Breed'},{key:'dateOfBirth',label:'Date of birth'},
    {key:'registrationNumber',label:'UELN'},{key:'registryName',label:'Registry'},
    {key:'sireName',label:'Sire'},{key:'sireRegistrationNumber',label:'Sire UELN'},
    {key:'damName',label:'Dam'},{key:'damRegistrationNumber',label:'Dam UELN'},
    {key:'pedigreeNotes',label:'Pedigree notes'},
  ];
  return <RoleGuard allowedRoles={['HORSE_OWNER']}>
    <AppShell><PageContainer>
      <Link href="/owner/admissions" className="text-sm text-[var(--color-primary)]">← My admissions</Link>
      {loading ? <p className="mt-5">Loading…</p> : error ? <p role="alert">{error}</p> : detail &&
        <div className="mt-5 space-y-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h1 className="text-2xl font-semibold">Admission #{detail.admissionId}</h1>
            <span className="rounded-full bg-[var(--color-primary-soft)] px-3 py-1 text-sm">
              {detail.status.replaceAll('_',' ')}
            </span>
          </div>
          <Panel padded>
            <h2 className="mb-4 font-semibold">Candidate profile · Submitted {new Date(detail.submittedAt).toLocaleDateString()}</h2>
            <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              {fields.map(field=><div key={field.key}>
                <dt className="text-xs text-[var(--color-text-muted)]">{field.label}</dt>
                <dd className="mt-1 text-sm">{detail.candidate[field.key] || '—'}</dd>
              </div>)}
            </dl>
          </Panel>
          <Panel padded className="space-y-4">
            <h2 className="font-semibold">Documents</h2>
            {detail.documents.length===0 ? <p>No documents uploaded.</p> :
              <ul className="space-y-2">{detail.documents.map(doc=><li key={doc.id}>
                <a className="text-sm text-[var(--color-primary)] underline"
                  href={doc.fileUrl.startsWith('/') ? `${apiUrl}${doc.fileUrl}` : doc.fileUrl}
                  target="_blank" rel="noopener noreferrer">
                    {doc.documentType.replaceAll('_',' ')} · {doc.uploadedAt.slice(0,10)}
                </a>
              </li>)}</ul>}
            {detail.status==='GROOM_REVIEW' && <div className="space-y-3 border-t pt-3">
              <p className="text-xs text-[var(--color-text-secondary)]">Upload is available until Groom starts reviewing.</p>
              <select className="rounded border p-2 text-sm" value={type}
                onChange={e=>setType(e.target.value as AdmissionDocumentType)}>
                {documentTypes.map(t=><option key={t} value={t}>{t.replaceAll('_',' ')}</option>)}
              </select>
              <input aria-label="Choose document" type="file" accept=".pdf,.jpg,.jpeg,.png,.webp"
                onChange={e=>setFile(e.target.files?.[0]||null)} />
              <button type="button" onClick={upload} disabled={!file||uploading}
                className="rounded bg-[var(--color-primary)] px-3 py-2 text-sm text-white disabled:opacity-50">
                {uploading?'Uploading…':'Upload document'}
              </button>
              {uploadError && <p role="alert" className="text-[var(--color-danger)]">{uploadError}</p>}
            </div>}
          </Panel>
          {[
            ['Groom feedback',detail.groomFeedback],['Veterinarian feedback',detail.vetFeedback],
            ['Trainer feedback',detail.trainerFeedback],['Manager feedback',detail.managerFeedback]
          ].filter(([,value])=>value).map(([label,value])=><Panel key={label} padded>
            <h2 className="font-semibold">{label}</h2><p className="mt-2 text-sm">{value}</p>
          </Panel>)}
        </div>}
    </PageContainer></AppShell>
  </RoleGuard>;
}
