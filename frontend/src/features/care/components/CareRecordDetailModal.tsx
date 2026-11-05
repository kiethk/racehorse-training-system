'use client';

import { Modal, Icon } from '@/components/ui';
import { Button } from '@/components/ui/Button';
import { RecordDetailView } from './RecordDetailView';
import type { HealthRecord } from '@/features/health/types';

interface CareRecordDetailModalProps {
  /** Completed care schedule id; when null and `record` is set the view shows that record alone. */
  scheduleId: number | null;
  open: boolean;
  onClose: () => void;
  onScheduleUpdated?: () => void;
  /** Complete record entity (e.g. from the horse health-records list) — full HealthRecord fields. */
  record?: HealthRecord | null;
  horseName?: string;
}

/**
 * Thin modal shell around the shared RecordDetailView. The full-viewport
 * experience lives at /veterinarian/records; modal consumers (admission
 * queue, calendar tab) keep their in-context overlay.
 */
export function CareRecordDetailModal({
  scheduleId,
  open,
  onClose,
  onScheduleUpdated,
  record = null,
  horseName,
}: CareRecordDetailModalProps) {
  if (!open) return null;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={
        <div className="flex items-center gap-2">
          <Icon name="stethoscope" size={20} className="text-[var(--color-primary)]" />
          <span>{scheduleId ? `Care Schedule #${scheduleId}` : `Clinical Record #${record?.id ?? ''}`}</span>
        </div>
      }
      description="Clinical examination record, health assessment, and active treatment regimens."
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
      }
    >
      <RecordDetailView
        scheduleId={scheduleId}
        record={record}
        horseName={horseName}
        onHideBack
        onScheduleUpdated={onScheduleUpdated}
      />
    </Modal>
  );
}
