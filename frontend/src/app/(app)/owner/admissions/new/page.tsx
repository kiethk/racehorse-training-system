import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { LinkButton } from '@/components/ui/Button';
import { PageHeader } from '@/components/ui/PageHeader';
import { ScreenLayout } from '@/components/ui/ScreenLayout';
import { OwnerAdmissionForm } from '@/features/admissions/components/OwnerAdmissionForm';

export default function NewOwnerAdmissionPage() {
  return (
    <RoleGuard allowedRoles={['HORSE_OWNER']}>
      <PageContainer>
        <ScreenLayout variant="form">
          <div>
            <LinkButton href="/owner/admissions" variant="tertiary" size="sm" icon="arrow-left" className="-ml-2.5">
              Back to admissions
            </LinkButton>
          </div>
          <PageHeader
            title="New horse admission"
            description={<>Enter your horse&apos;s information and select supporting documents before submitting.</>}
          />
          <OwnerAdmissionForm />
        </ScreenLayout>
      </PageContainer>
    </RoleGuard>
  );
}
