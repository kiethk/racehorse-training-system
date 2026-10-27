'use client';

import { useEffect } from 'react';
import { Button, Icon, IconButton } from '@/components/ui';
import type { UrgentAssignmentAlert } from '../types';

const AUTO_CLOSE_MS = 15000;

interface Props {
  alerts: UrgentAssignmentAlert[];
  onOpenCase: (alert: UrgentAssignmentAlert) => void;
  onDismiss: (alert: UrgentAssignmentAlert) => void;
}

/**
 * Floating toast-style alert pills, stacked and centred like a toast. Non-modal:
 * they float above the workspace without blocking it.
 */
export function UrgentAssignmentBanner({ alerts, onOpenCase, onDismiss }: Props) {
  if (alerts.length === 0) return null;

  return (
    <div
      role="region"
      aria-label="Urgent case alerts"
      className="pointer-events-none fixed left-1/2 top-[calc(var(--topbar-height)+0.75rem)] z-[var(--z-toast)] w-fit max-w-[calc(100vw-2rem)] -translate-x-1/2 px-2"
    >
      <div className="flex flex-col items-start gap-2">
        {alerts.map((alert) => (
          <AlertPill key={alert.scheduleId} alert={alert} onOpenCase={onOpenCase} onDismiss={onDismiss} />
        ))}
      </div>
    </div>
  );
}

function AlertPill({
  alert,
  onOpenCase,
  onDismiss,
}: {
  alert: UrgentAssignmentAlert;
  onOpenCase: (alert: UrgentAssignmentAlert) => void;
  onDismiss: (alert: UrgentAssignmentAlert) => void;
}) {
  useEffect(() => {
    const timer = setTimeout(() => onDismiss(alert), AUTO_CLOSE_MS);
    return () => clearTimeout(timer);
  }, [alert, onDismiss]);

  return (
    <div
      className="pointer-events-auto flex max-w-[30rem] items-center gap-2.5 rounded-[var(--radius-md)] bg-[var(--color-danger)] py-2 pl-2.5 pr-1.5 text-[var(--color-text-inverse)] shadow-[var(--shadow-popover)]"
      title={alert.description}
    >
      <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-white">
        <Icon name="alert-triangle" size={14} className="text-[var(--color-danger)]" />
      </span>
      <p className="min-w-0 flex-1 truncate text-sm font-medium">
        Urgent case assigned · {alert.horseName} · #{alert.horseId}
      </p>
      <Button
        variant="link"
        size="sm"
        className="shrink-0 text-[var(--color-text-inverse)] hover:text-[var(--color-text-inverse)]"
        onClick={() => onOpenCase(alert)}
      >
        Open
      </Button>
      <IconButton
        icon="x"
        size="sm"
        variant="tertiary"
        label={`Dismiss alert for ${alert.horseName}`}
        className="shrink-0 bg-transparent text-[var(--color-text-inverse)] hover:bg-white/15 hover:text-[var(--color-text-inverse)]"
        onClick={() => onDismiss(alert)}
      />
    </div>
  );
}
