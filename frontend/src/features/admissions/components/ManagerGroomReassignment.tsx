'use client';

import { useEffect, useState } from 'react';
import { Button, ConfirmDialog, FormField, Select, Notice, ListSkeleton } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { admissionsApi } from '../services/api';
import type { AdmissionDetailResponse, QuarantineGroomOption } from '../types';
import { AdmissionSideCard } from '../shared/components/AdmissionInfoSection';

export function ManagerGroomReassignment({ detail, onSuccess }: {
  detail: AdmissionDetailResponse;
  onSuccess: () => Promise<void>;
}) {
  const [grooms, setGrooms] = useState<QuarantineGroomOption[] | null>(null);
  const [selected, setSelected] = useState(String(detail.assignedGroomId ?? ''));
  const [error, setError] = useState<string | null>(null);
  const [reload, setReload] = useState(0);
  const [confirming, setConfirming] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;
    admissionsApi.getQuarantineGrooms()
      .then(rows => { if (active) { setGrooms(rows); setError(null); } })
      .catch(cause => { if (active) setError(displayError(cause, 'Unable to load quarantine grooms.')); });
    return () => { active = false; };
  }, [reload]);

  const target = grooms?.find(groom => String(groom.groomId) === selected);
  const reassign = async () => {
    if (!target || saving) return;
    setSaving(true);
    setError(null);
    try {
      await admissionsApi.reassignGroom(detail.admissionId, target.groomId);
      setConfirming(false);
      toast.success('Admission reassigned');
      await onSuccess();
    } catch (cause) {
      const message = displayError(cause, 'Unable to reassign admission.');
      setError(message);
      setConfirming(false);
      toast.error(message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <AdmissionSideCard title="Groom review">
      {error && <Notice tone="error">{error}</Notice>}
      {!grooms && !error && <ListSkeleton rows={2} />}
      {!grooms && error && <Button onClick={() => { setError(null); setReload(value => value + 1); }}>Retry</Button>}
      {grooms?.length === 0 && <Notice tone="warning">No quarantine grooms available</Notice>}
      {grooms && grooms.length > 0 && (
        <>
          <FormField label="Assigned groom">
            <Select value={selected} onChange={event => setSelected(event.target.value)} disabled={saving}>
              {!grooms.some(groom => groom.groomId === detail.assignedGroomId) && (
                <option value={String(detail.assignedGroomId ?? '')}>{detail.assignedGroomName ?? 'Select a groom'}</option>
              )}
              {grooms.map(groom => (
                <option key={groom.groomId} value={groom.groomId}>
                  {`${groom.name} · ${groom.workload} ${groom.workload === 1 ? 'horse' : 'horses'}${groom.full ? ' · Full' : ''}`}
                </option>
              ))}
            </Select>
          </FormField>
          <Button variant="primary" className="w-full" disabled={!target || saving} onClick={() => setConfirming(true)}>Reassign</Button>
        </>
      )}
      <ConfirmDialog
        open={confirming}
        title={`Reassign admission to ${target?.name ?? 'selected groom'}?`}
        confirmLabel="Reassign"
        loading={saving}
        confirmDisabled={!target}
        onConfirm={() => void reassign()}
        onCancel={() => { if (!saving) setConfirming(false); }}
      />
    </AdmissionSideCard>
  );
}
