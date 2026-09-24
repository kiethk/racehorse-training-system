'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { Panel } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { ownerAdmissionApi } from '@/features/admissions/services/ownerApi';
import type { AdmissionSummaryResponse } from '@/features/admissions/types';

export default function OwnerAdmissionsPage() {
  const [rows, setRows] = useState<AdmissionSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    ownerAdmissionApi.list().then(setRows).catch(e => setError(String(e))).finally(() => setLoading(false));
  }, []);
  return <RoleGuard allowedRoles={['HORSE_OWNER']}><AppShell><PageContainer>
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
            <div><p className="font-semibold">{a.candidateName}</p>
              <p className="text-sm text-[var(--color-text-secondary)]">
                Application #{a.admissionId} · {a.submittedAt ? new Date(a.submittedAt).toLocaleDateString() : 'Just submitted'}
              </p><p className="text-sm">{a.status.replaceAll('_', ' ')}</p></div>
            <Link className="underline text-[var(--color-primary)]" href={`/owner/admissions/${a.admissionId}`}>View details</Link>
          </li>)}
        </ul></Panel>}
    </div>
  </PageContainer></AppShell></RoleGuard>;
}
