'use client';

import { useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { ownerAdmissionApi, type CandidateForm } from '../services/ownerApi';
import type { AdmissionDocumentType } from '../types/owner';

const TYPES: {value: AdmissionDocumentType; label: string}[] = [
  {value: 'HORSE_PHOTO', label: 'Horse photo'},
  {value: 'REGISTRATION_DOCUMENT', label: 'Registration document'},
  {value: 'PEDIGREE_CERTIFICATE', label: 'Pedigree certificate'},
  {value: 'VACCINATION_RECORD', label: 'Vaccination record'},
  {value: 'DEWORMING_RECORD', label: 'Deworming record'},
  {value: 'HEALTH_CERTIFICATE', label: 'Health certificate'},
  {value: 'PREVIOUS_MEDICAL_RECORD', label: 'Previous medical record'},
  {value: 'PREVIOUS_INJURY_RECORD', label: 'Previous injury record'},
];
const inputClass = 'w-full rounded-md border border-[var(--color-border-strong)] bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)]';

export function OwnerAdmissionForm() {
  const router = useRouter();
  const [form, setForm] = useState<CandidateForm>({name: '', breed: '', dateOfBirth: null, registrationNumber: null, registryName: null, sireName: null, sireRegistrationNumber: null, damName: null, damRegistrationNumber: null, pedigreeNotes: null});
  const [uploads, setUploads] = useState<{file: File; documentType: AdmissionDocumentType; recordDate: string; note: string}[]>([]);
  const [selectedType, setSelectedType] = useState<AdmissionDocumentType>('HORSE_PHOTO');
  const [recordDate, setRecordDate] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [createdId, setCreatedId] = useState<number | null>(null);

  const fields: {key: keyof CandidateForm; label: string; required?: boolean; type?: string; hint?: string}[] = [
    {key:'name', label:'Horse name', required:true},
    {key:'breed', label:'Breed'},
    {key:'dateOfBirth', label:'Date of birth', type:'date'},
    {key:'registrationNumber', label:'UELN / registration number', hint:'15 alphanumeric characters, if known'},
    {key:'registryName', label:'Registry name'},
    {key:'sireName', label:'Sire name'},
    {key:'sireRegistrationNumber', label:'Sire UELN', hint:'15 alphanumeric characters, if known'},
    {key:'damName', label:'Dam name'},
    {key:'damRegistrationNumber', label:'Dam UELN', hint:'15 alphanumeric characters, if known'},
  ];
  function change(key: keyof CandidateForm, value: string) {
    setForm((current: CandidateForm) => ({...current, [key]: value}));
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    let id = createdId;
    try {
      if (id === null) {
        const created = await ownerAdmissionApi.create(
          Object.fromEntries(Object.entries(form).filter(([, value]) => value !== '')) as CandidateForm
        );
        id = created.admissionId;
        setCreatedId(id);
      }
      for (const upload of uploads) {
        await ownerAdmissionApi.upload(
          id, upload.file, upload.documentType, upload.recordDate || '', upload.note || ''
        );
        setUploads(current => current.filter(item => item !== upload));
      }
      router.push(`/owner/admissions/${id}`);
    } catch (cause) {
      setError(
        `${cause instanceof Error ? cause.message : 'Unable to submit admission.'} ` +
        (id !== null ? `Admission #${id} was created. Retry to upload remaining documents.` : '')
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit} className="space-y-5">
      <Panel padded className="space-y-4">
        <h2 className="text-lg font-semibold">Candidate horse</h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {fields.map(field => (
            <label key={field.key} className="space-y-1">
              <span className="block text-sm font-medium">{field.label}{field.required ? ' *' : ''}</span>
              <input className={inputClass} type={field.type || 'text'}
                value={form[field.key] || ''} required={field.required}
                maxLength={field.key.toLowerCase().includes('registration') ? 15 : 255}
                pattern={field.key.toLowerCase().includes('registration') ? '[a-zA-Z0-9]{15}' : undefined}
                max={field.type === 'date' ? new Date().toISOString().slice(0,10) : undefined}
                placeholder={field.hint}
                disabled={createdId !== null}
                onChange={e => change(field.key,e.target.value)} />
            </label>
          ))}
        </div>
        <label className="block space-y-1">
          <span className="text-sm font-medium">Pedigree notes</span>
          <textarea className={inputClass} rows={3} maxLength={2000}
            value={form.pedigreeNotes || ''} disabled={createdId !== null}
            onChange={e => change('pedigreeNotes',e.target.value)} />
        </label>
      </Panel>
      <Panel padded className="space-y-4">
        <h2 className="text-lg font-semibold">Supporting documents</h2>
        <p className="text-sm text-[var(--color-text-secondary)]">PDF or image, up to 10 MB per file. Uploaded documents are locked once Groom begins reviewing.</p>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          <label className="space-y-1 text-sm">Document type
            <select className={inputClass} value={selectedType}
              onChange={e => setSelectedType(e.target.value as AdmissionDocumentType)}>
              {TYPES.map(type => <option key={type.value} value={type.value}>{type.label}</option>)}
            </select>
          </label>
          <label className="space-y-1 text-sm">Record date
            <input className={inputClass} type="date" value={recordDate}
              onChange={e=>setRecordDate(e.target.value)} />
          </label>
        </div>
        <label className="block space-y-1 text-sm">Document notes
          <input className={inputClass} value={note} maxLength={2000}
            onChange={e=>setNote(e.target.value)} />
        </label>
        <input aria-label="Select document" type="file" accept=".pdf,.jpg,.jpeg,.png,.webp"
          onChange={e=>{
            const file=e.target.files?.[0];
            if (!file) return;
            if(file.size>10*1024*1024){setError('Each document must be 10 MB or smaller.');return;}
            setUploads(current=>[...current,{file,documentType:selectedType,recordDate,note}]);
            setError('');
            e.target.value='';
          }} />
        {uploads.length > 0 && (
          <ul className="space-y-1">
            {uploads.map((item,index)=>(
              <li key={index} className="flex items-center justify-between text-sm">
                <span>{item.file.name} — {item.documentType.replaceAll('_',' ')}</span>
                <button type="button" className="text-[var(--color-danger)]"
                  onClick={()=>setUploads(current=>current.filter((_,i)=>i!==index))}>Remove</button>
              </li>
            ))}
          </ul>
        )}
      </Panel>
      {error && <p role="alert" className="text-sm text-[var(--color-danger)]">{error}</p>}
      <div className="flex justify-end gap-3">
        <Button type="button" onClick={()=>router.push('/owner/admissions')}>Cancel</Button>
        <Button variant="primary" type="submit" loading={busy}>
          {createdId === null ? 'Submit admission' : 'Retry document uploads'}
        </Button>
      </div>
    </form>
  );
}
