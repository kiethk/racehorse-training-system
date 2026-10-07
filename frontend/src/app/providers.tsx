'use client';

import { AuthProvider } from '@/context/AuthContext';
import { NotificationProvider } from '@/features/notifications/context/NotificationContext';
import type { ReactNode } from 'react';

export function Providers({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <NotificationProvider>{children}</NotificationProvider>
    </AuthProvider>
  );
}
