'use client';

import { useEffect, useState } from 'react';
import { Notice, PageHeader, ScreenLayout, Tabs } from '@/components/ui';
import { useAuth } from '@/context/AuthContext';
import { displayError } from '@/lib/display';
import { preventiveCareService } from '../../services/preventiveCareService';
import type { PreventiveProtocol } from '../../types/preventive';
import { CalendarTab } from './CalendarTab';
import { CoverageTab } from './CoverageTab';
import { DueWorkTab } from './DueWorkTab';
import { ProtocolsTab } from './ProtocolsTab';

type TabId = 'due' | 'calendar' | 'coverage' | 'protocols';

/** Preventive care hub (docs/preventive-care-flow.md section 7). */
export function PreventiveCareHub() {
  const { user } = useAuth();
  const [tab, setTab] = useState<TabId>('due');
  const [protocols, setProtocols] = useState<PreventiveProtocol[]>([]);
  const [protocolsLoading, setProtocolsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    preventiveCareService
      .listProtocols()
      .then((data) => setProtocols(Array.isArray(data) ? data : []))
      .catch((cause) => setError(displayError(cause, 'Could not load preventive protocols.')))
      .finally(() => setProtocolsLoading(false));
  }, []);

  return (
    <ScreenLayout variant="list">
      <PageHeader
        title="Preventive Care"
      />
      {error && <Notice tone="error" title="Could not load" onDismiss={() => setError(null)}>{error}</Notice>}
      <Tabs
        active={tab}
        onChange={(id) => setTab(id as TabId)}
        tabs={[
          { id: 'due', label: 'To do', icon: 'clipboard' },
          { id: 'calendar', label: 'Calendar', icon: 'calendar' },
          { id: 'coverage', label: 'By vaccine', icon: 'shield' },
          { id: 'protocols', label: 'Protocols', icon: 'file-text' },
        ]}
      />
      {tab === 'due' && <DueWorkTab protocols={protocols} canRecord={user?.role === 'VETERINARIAN'} />}
      {tab === 'calendar' && <CalendarTab />}
      {tab === 'coverage' && <CoverageTab />}
      {tab === 'protocols' && <ProtocolsTab protocols={protocols} loading={protocolsLoading} />}
    </ScreenLayout>
  );
}
