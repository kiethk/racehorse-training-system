'use client';

import '@/lib/pdfPolyfill';
import { AuthProvider } from '@/context/AuthContext';
import { NotificationProvider } from '@/features/notifications/context/NotificationContext';
import { AiChatProvider } from '@/features/ai';
import { Toaster } from '@/components/ui/Toaster';
import { TooltipProvider } from '@/components/ui/Tooltip';
import type { ReactNode } from 'react';

export function Providers({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <NotificationProvider>
        <AiChatProvider>
          <TooltipProvider delayDuration={200}>{children}</TooltipProvider>
          <Toaster />
        </AiChatProvider>
      </NotificationProvider>
    </AuthProvider>
  );
}
