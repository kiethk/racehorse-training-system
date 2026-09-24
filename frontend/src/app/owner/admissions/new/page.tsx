import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { OwnerAdmissionForm } from '@/features/admissions/components/OwnerAdmissionForm';

export default function NewOwnerAdmissionPage() {
  return <RoleGuard allowedRoles={['HORSE_OWNER']}>
    <AppShell><PageContainer>
      <div className="mb-5">
        <h1 className="text-2xl font-semibold">New horse admission</h1>
        <p className="text-sm text-[var(--color-text-secondary)]">
          Submit a candidate profile for Groom review.
        </p>
      </div>
      <OwnerAdmissionForm />
    </PageContainer></AppShell>
  </RoleGuard>;
}
