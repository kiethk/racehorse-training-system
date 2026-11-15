import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { ManagerAnalyticsPageView } from '@/features/manager-analytics/components/ManagerAnalyticsPageView';

export const metadata: Metadata = {
  title: 'Exploratory Data Analysis | Manager | RTMS',
};

export default function ManagerAnalyticsPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <PageContainer>
        <ManagerAnalyticsPageView />
      </PageContainer>
    </RoleGuard>
  );
}
