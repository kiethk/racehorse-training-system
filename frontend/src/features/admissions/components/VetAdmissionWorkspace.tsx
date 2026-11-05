'use client';

import { useState } from 'react';
import { SegmentedControl } from '@/components/ui/SegmentedControl';
import { CareRecordDetailModal } from '@/features/care/components/CareRecordDetailModal';
import { AdmissionPipeline } from '@/features/admissions/shared/components/AdmissionPipeline';
import type { PipelineDetail } from '@/features/admissions/shared/components/AdmissionPipeline';

type TabKey = 'horse' | 'form' | 'pipeline' | 'record';

const TABS: Array<{ value: TabKey; label: string }> = [
  { value: 'horse', label: 'Horse info' },
  { value: 'form', label: 'Admission form' },
  { value: 'pipeline', label: 'Pipeline' },
  { value: 'record', label: 'View record' },
];

interface VetAdmissionWorkspaceProps {
  careScheduleId: number;
  horseId: number;
  horseName?: string;
  pipelineDetail?: PipelineDetail;
  children: React.ReactNode;
}

export function VetAdmissionWorkspace({ careScheduleId, pipelineDetail, children }: VetAdmissionWorkspaceProps) {
  const [tab, setTab] = useState<TabKey>('form');
  const [showRecord, setShowRecord] = useState(false);

  const handleTabChange = (v: TabKey) => {
    setTab(v);
    if (v === 'record') {
      setShowRecord(true);
    }
  };

  return (
    <div className="space-y-4">
      <SegmentedControl
        label="Admission tabs"
        value={tab}
        onChange={(v) => handleTabChange(v as TabKey)}
        options={TABS.map((t) => ({ value: t.value, label: t.label }))}
      />
      {tab === 'horse' && (
        <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] p-4 text-sm text-[var(--color-text-muted)]">
          Horse info tab — reuse shared horse detail components (TODO).
        </div>
      )}
      {tab === 'form' && children}
      {tab === 'pipeline' && (
        pipelineDetail ? (
          <AdmissionPipeline detail={pipelineDetail} />
        ) : (
          <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] p-4 text-sm text-[var(--color-text-muted)]">
            Pipeline tab — Vet → Trainer flow (admission detail not provided).
          </div>
        )
      )}
      {tab === 'record' && (
        <CareRecordDetailModal
          scheduleId={careScheduleId}
          open={showRecord}
          onClose={() => setShowRecord(false)}
          onScheduleUpdated={() => {}}
        />
      )}
    </div>
  );
}