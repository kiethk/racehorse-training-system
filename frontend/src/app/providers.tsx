'use client';

import { AuthProvider } from '@/context/AuthContext';
import { NotificationProvider } from '@/features/notifications/context/NotificationContext';
import { Toaster } from '@/components/ui/Toaster';
import { TooltipProvider } from '@/components/ui/Tooltip';
import type { ReactNode } from 'react';

export function Providers({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <NotificationProvider>
        <TooltipProvider delayDuration={200}>{children}</TooltipProvider>
        <Toaster />
      </NotificationProvider>
    </AuthProvider>
  );
}
