import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { ProfileView } from '@/features/profile/components/ProfileView';

export const metadata: Metadata = { title: 'My profile | RTMS' };

export default function MyProfilePage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER', 'HORSE_OWNER', 'HEAD_TRAINER', 'GROOM', 'VETERINARIAN']}>
      <PageContainer><ProfileView /></PageContainer>
    </RoleGuard>
  );
}
