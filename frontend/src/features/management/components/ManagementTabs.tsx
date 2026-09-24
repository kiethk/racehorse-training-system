'use client';

import { useState } from 'react';
import { Tabs } from '@/components/ui/Tabs';
import { ManagerQueueList } from '@/features/admissions/components/ManagerQueueList';
import { AccessControlPlaceholder } from './AccessControlPlaceholder';
import { AuditLogPlaceholder } from './AuditLogPlaceholder';

type ManagementTab = 'admission' | 'access' | 'audit';

const tabs = [
  { id: 'admission', label: 'Admission', icon: 'users' as const },
  { id: 'access',    label: 'Access control', icon: 'shield' as const },
  { id: 'audit',     label: 'Audit log', icon: 'clipboard' as const },
];

export function ManagementTabs() {
  const [activeTab, setActiveTab] = useState<ManagementTab>('admission');

  return (
    <div className="space-y-4">
      {/* Page heading — matches ManagementScreen title */}
      <div>
        <h1 className="text-[20px] font-semibold tracking-tight text-[var(--color-text-primary)]">
          Management
        </h1>
      </div>

      {/* Secondary tab strip — matches ui-reference Tabs style */}
      <div className="mb-4">
        <Tabs
          tabs={tabs}
          active={activeTab}
          onChange={(id) => setActiveTab(id as ManagementTab)}
        />
      </div>

      {/* Tab content */}
      {activeTab === 'admission' && <ManagerQueueList />}
      {activeTab === 'access'    && <AccessControlPlaceholder />}
      {activeTab === 'audit'     && <AuditLogPlaceholder />}
    </div>
  );
}
