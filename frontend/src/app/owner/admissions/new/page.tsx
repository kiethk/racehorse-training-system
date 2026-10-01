import Link from 'next/link';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { OwnerAdmissionForm } from '@/features/admissions/components/OwnerAdmissionForm';

export default function NewOwnerAdmissionPage() {
  return (
    <RoleGuard allowedRoles={['HORSE_OWNER']}>
      <AppShell>
        <PageContainer>
          <div className="max-w-4xl space-y-5">
            <Link href="/owner/admissions" className="text-sm font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]">← Back to applications</Link>
            <div>
              <h1 className="text-2xl font-semibold">New horse admission</h1>
              <p className="text-sm text-[var(--color-text-secondary)]">
                Enter your horse&apos;s information and select supporting documents before submitting.
              </p>
            </div>
            <OwnerAdmissionForm />
          </div>
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
