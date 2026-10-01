'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';



import { Panel } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { admissionsApi } from '@/features/admissions/services/api';
import { ownerAdmissionApi } from '@/features/admissions/services/ownerApi';
import type { AdmissionSummaryResponse } from '../types';
import { AdmissionStatusBadge } from '../shared/components/AdmissionStatusBadge';

function AdmissionHorseAvatar({ name, imageUrl }: { name: string; imageUrl?: string | null }) {
  const [loadedImage, setLoadedImage] = useState<{ source: string; url: string } | null>(null);
  const image = loadedImage && loadedImage.source === imageUrl ? loadedImage.url : null;

  useEffect(() => {
    if (!imageUrl) return;

    const controller = new AbortController();
    let objectUrl: string | undefined;
    admissionsApi.getDocumentFile(imageUrl, controller.signal).then((blob) => {
      if (controller.signal.aborted) return;
      if (!blob.type.startsWith('image/')) return;
      objectUrl = URL.createObjectURL(blob);
      setLoadedImage({ source: imageUrl, url: objectUrl });
    }).catch(() => undefined);

    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [imageUrl]);

  return <HorseAvatar name={name} image={image} size={48} rounded="md" />;
}

export function OwnerAdmissionsListView() {
  const [rows, setRows] = useState<AdmissionSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    ownerAdmissionApi.list().then(setRows).catch(e => setError(String(e))).finally(() => setLoading(false));
  }, []);
  return (
    <div className="space-y-5">
      <header className="flex items-center justify-between gap-4">
        <div><h1 className="text-2xl font-semibold">My admissions</h1>
          <p className="text-sm text-[var(--color-text-secondary)]">Track each application and its review status.</p></div>
        <Link href="/owner/admissions/new"><Button variant="primary">New admission</Button></Link>
      </header>
      {loading ? <p>Loading admissions…</p> : error ? <p role="alert">{error}</p> :
        rows.length === 0 ? <Panel padded>No admission history yet.</Panel> :
        <Panel><ul className="divide-y divide-[var(--color-border)]">
          {rows.map(a => <li key={a.admissionId} className="p-4 flex items-center justify-between gap-4">
            <div className="flex min-w-0 items-center gap-3">
              <AdmissionHorseAvatar name={a.candidateName} imageUrl={a.imageUrl} />
              <div className="min-w-0"><p className="font-semibold">{a.candidateName}</p>
              <p className="text-sm text-[var(--color-text-secondary)]">
                Application #{a.admissionId} · {a.submittedAt ? new Date(a.submittedAt).toLocaleDateString() : 'Just submitted'}
              </p><AdmissionStatusBadge status={a.status} size="sm" /></div>
            </div>
            <Link className="shrink-0 whitespace-nowrap underline text-[var(--color-primary)]" href={`/owner/admissions/${a.admissionId}`}>View details</Link>
          </li>)}
        </ul></Panel>}
    </div>
  );
}

