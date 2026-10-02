import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { ManagerAuditLogView } from '@/features/audit-log/components/ManagerAuditLogView';

export const metadata: Metadata = {
  title: 'Audit Log | Manager',
};

export default function ManagerAuditLogPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <AppShell>
        <PageContainer>
          <ManagerAuditLogView />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
