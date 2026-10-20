'use client';

import { RoleGuard } from '@/components/auth/RoleGuard';
import { RoleLanding } from '@/components/auth/RoleLanding';

export default function OwnerPage() {
  return (
    <RoleGuard allowedRoles={['HORSE_OWNER']}>
      <RoleLanding role="HORSE_OWNER" />
    </RoleGuard>
  );
}
