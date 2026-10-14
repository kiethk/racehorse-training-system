'use client';

import { useId, useState, type ReactNode } from 'react';
import { Tabs, type TabItem } from '@/components/ui';

export interface HorseDetailTab extends TabItem {
  content: ReactNode;
}

/** Shared underlined tabs. Callers supply only the tabs their actor may access. */
export function HorseDetailTabs({ tabs }: { tabs: readonly HorseDetailTab[] }) {
  const [active, setActive] = useState(tabs[0]?.id);
  const panelId = useId();
  const current = tabs.find((tab) => tab.id === active) ?? tabs[0];

  if (!current) return null;

  return (
    <div className="min-w-0 space-y-5">
      <Tabs
        tabs={tabs.map(({ id, label, icon, count }) => ({ id, label, icon, count }))}
        active={current.id}
        onChange={setActive}
      />
      <div
        key={current.id}
        id={panelId}
        role="tabpanel"
        aria-label={current.label}
        tabIndex={0}
        className="min-w-0 space-y-5 outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
      >
        {current.content}
      </div>
    </div>
  );
}
