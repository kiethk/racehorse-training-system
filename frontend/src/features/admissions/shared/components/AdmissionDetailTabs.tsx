'use client';

import { useState, type ReactNode } from 'react';
import { Tabs } from '@/components/ui/Tabs';

export interface AdmissionDetailTab {
  id: string;
  label: string;
  count?: number;
  content: ReactNode;
}

/** Tab strip plus the active tab's sections: the main column of every admission detail screen. */
export function AdmissionDetailTabs({ tabs }: { tabs: AdmissionDetailTab[] }) {
  const [active, setActive] = useState(tabs[0]?.id);
  const current = tabs.find((tab) => tab.id === active) ?? tabs[0];

  return (
    <div className="min-w-0 space-y-5">
      <Tabs tabs={tabs.map(({ id, label, count }) => ({ id, label, count }))} active={current.id} onChange={setActive} />
      <div key={current.id} className="space-y-5 animate-in fade-in slide-in-from-bottom-1 duration-200">
        {current.content}
      </div>
    </div>
  );
}
