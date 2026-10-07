import Link from 'next/link';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { PageHeader } from '@/components/ui/PageHeader';
import { ScreenLayout } from '@/components/ui/ScreenLayout';
import { OwnerAdmissionForm } from '@/features/admissions/components/OwnerAdmissionForm';

export default function NewOwnerAdmissionPage() {
  return (
    <RoleGuard allowedRoles={['HORSE_OWNER']}>
      <AppShell>
        <PageContainer>
          <ScreenLayout variant="form">
            <Link href="/owner/admissions" className="inline-flex min-h-11 items-center text-sm font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]">← Back to applications</Link>
            <PageHeader
              title="New horse admission"
              description={<>Enter your horse&apos;s information and select supporting documents before submitting.</>}
            />
            <OwnerAdmissionForm />
          </ScreenLayout>
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
