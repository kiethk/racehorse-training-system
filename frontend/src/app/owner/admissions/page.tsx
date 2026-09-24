'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { Panel } from '@/components/ui/Panel';
import { ownerAdmissionsApi } from '@/features/admissions/services/ownerApi';
import type { AdmissionSummaryResponse } from '@/features/admissions/types/owner';

export default function OwnerAdmissionsPage() {
  const [admissions,setAdmissions]=useState<AdmissionSummaryResponse[]>([]);
  const [loading,setLoading]=useState(true);
  const [error,setError]=useState('');
  useEffect(()=>{
    ownerAdmissionsApi.list().then(setAdmissions)
      .catch(e=>setError(e instanceof Error?e.message:'Unable to load admissions'))
      .finally(()=>setLoading(false));
  },[]);
  return <RoleGuard allowedRoles={['HORSE_OWNER']}>
    <AppShell><PageContainer>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div><h1 className="text-2xl font-semibold">My admissions</h1>
          <p className="text-sm text-[var(--color-text-secondary)]">Application history and current review statuses</p></div>
        <Link href="/owner/admissions/new" className="rounded-md bg-[var(--color-primary)] px-4 py-2 text-sm font-semibold text-white">
          New admission
        </Link>
      </div>
      <Panel padded>
        {loading ? <p>Loading admissions…</p> : error ? <p role="alert">{error}</p>
          : admissions.length===0 ? <p>No admissions yet. Create your first application.</p>
          : <ul className="divide-y divide-[var(--color-border)]">
            {admissions.map(a=><li key={a.admissionId} className="py-3">
              <Link href={`/owner/admissions/${a.admissionId}`} className="flex items-center justify-between gap-3">
                <div><p className="font-semibold">{a.candidateName}</p>
                  <p className="text-xs text-[var(--color-text-muted)]">
                    #{a.admissionId} · {a.breed || 'Breed not specified'} · {new Date(a.submittedAt).toLocaleDateString()}
                  </p></div>
                <span className="rounded-full bg-[var(--color-primary-soft)] px-3 py-1 text-xs font-medium text-[var(--color-primary)]">
                  {a.status.replaceAll('_',' ')}
                </span>
              </Link>
            </li>)}
          </ul>}
      </Panel>
    </PageContainer></AppShell>
  </RoleGuard>;
}
