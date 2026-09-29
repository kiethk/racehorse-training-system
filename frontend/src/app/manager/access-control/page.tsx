import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { AccessControlView } from '@/features/access-control/components/AccessControlView';

export const metadata: Metadata = {
  title: 'Access Control | Manager',
};

export default function ManagerAccessControlPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <AppShell>
        <PageContainer>
          <div>
            <h1 className="text-[20px] font-semibold tracking-tight text-[var(--color-text-primary)] mb-4">
              Access Control
            </h1>
            <AccessControlView />
          </div>
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
