'use client';

import { RoleGuard } from '@/components/auth/RoleGuard';
import { RoleLanding } from '@/components/auth/RoleLanding';

export default function TrainerPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <RoleLanding role="HEAD_TRAINER" />
    </RoleGuard>
  );
}
