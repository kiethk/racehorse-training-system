'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button, ConfirmDialog, Notice, Spinner } from '@/components/ui';
import { displayError } from '@/lib/display';
import type { Area } from '../types';
import type { AreaDeletionCheck } from '../types/manager';
import { stableApi } from '../services/stableService';

export function DeleteAreaDialog({ area, deleting, actionError, onDelete, onClose }: {
  area: Area;
  deleting: boolean;
  actionError: string | null;
  onDelete: () => Promise<boolean>;
  onClose: () => void;
}) {
  const [check, setCheck] = useState<AreaDeletionCheck | null>(null);
  const [checking, setChecking] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [version, setVersion] = useState(0);
  useEffect(() => {
    let active = true;
    stableApi.getDeletionCheck(area.id).then(result => {
      if (active) { setCheck(result); setError(null); }
    }).catch(cause => {
      if (active) setError(displayError(cause, 'Unable to check this Area.'));
    }).finally(() => { if (active) setChecking(false); });
    return () => { active = false; };
  }, [area.id, version]);
  const retry = useCallback(() => { setChecking(true); setCheck(null); setError(null); setVersion(v => v + 1); }, []);

  return <ConfirmDialog open title={`Delete Area ${area.code}?`} tone="danger" confirmLabel="Delete area"
    loading={deleting} confirmDisabled={checking || !!error || !check?.canDelete}
    onCancel={onClose} onConfirm={() => { void onDelete().then(deleted => { if (!deleted) retry(); }); }}
    description={<div className="space-y-3">
      {checking ? <div className="flex items-center gap-2"><Spinner size="sm" />Checking Area...</div>
        : error ? <Notice tone="error">{error} <Button variant="link" size="sm" onClick={retry}>Retry check</Button></Notice>
          : check?.canDelete ? <p>Area {area.code} and its {check.stallCount} stalls will be removed from active use. Add area can restore them later.</p>
            : <div><p className="font-medium text-[var(--color-text-primary)]">This Area cannot be deleted.</p>
              <ul className="mt-2 list-disc space-y-1 pl-5">{check?.reasons.map(reason => <li key={reason}>{reason}</li>)}</ul></div>}
      {actionError && <Notice tone="error">{actionError}</Notice>}
    </div>} />;
}
