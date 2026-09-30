import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { AuditLogPlaceholder } from '@/features/management/components/AuditLogPlaceholder';

export const metadata: Metadata = {
  title: 'Audit Log | Manager',
};

export default function ManagerAuditLogPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <AppShell>
        <PageContainer>
          <div>
            <h1 className="text-[20px] font-semibold tracking-tight text-[var(--color-text-primary)] mb-4">
              Audit Log
            </h1>
            <AuditLogPlaceholder />
          </div>
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
