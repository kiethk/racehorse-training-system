'use client';

import { useEffect, useMemo, useState } from 'react';
import { Button, ConfirmDialog, EmptyState, IconButton, ListSkeleton, Modal, Notice, PageHeader, Panel, Pill, ScreenLayout, Tabs } from '@/components/ui';
import type { AreaType, StableStall } from '../types';
import type { AssignmentDraft, AssignmentTarget, DragStaff } from '../types/manager';
import { useManagerStable } from '../hooks/useManagerStable';
import { areaView, compareAreas, editCount, horseMap, overlayDraft, personName, planAssignment } from '../lib/managerAssignments';
import { StallDetailPanel } from '../shared/components/StallDetailPanel';
import { AreaCard } from './AreaCard';
import { StableStaffPanel } from './StableStaffPanel';
import { StaffAssignmentPicker } from './StaffAssignmentPicker';

type Preview = ReturnType<typeof planAssignment>;

export function ManagerStableView() {
  const state = useManagerStable();
  const { data, draft, setDraft, loading, saving, creating, loadError, actionError, needsReload,
    reload, discard, save, addArea, setActionError } = state;
  const [activeTab, setActiveTab] = useState<AreaType>('REGULAR');
  const [attentionOnly, setAttentionOnly] = useState(false);
  const [dragging, setDragging] = useState<DragStaff | null>(null);
  const [selectedStaff, setSelectedStaff] = useState<DragStaff | null>(null);
  const [picker, setPicker] = useState<AssignmentTarget | null>(null);
  const [preview, setPreview] = useState<Preview | null>(null);
  const [pendingAction, setPendingAction] = useState<{ run: () => void } | null>(null);
  const [selectedStall, setSelectedStall] = useState<StableStall | null>(null);
  const count = data && draft ? editCount(data, draft) : 0;
  const dirty = count > 0;
  const busy = loading || saving || creating;
  const editDisabled = busy || needsReload;
  const horses = useMemo(() => horseMap(data ?? { horses: [] }), [data]);
  const views = useMemo(() => data && draft ? data.areas.filter(a => a.type === activeTab).sort(compareAreas)
    .map(a => areaView(data, draft, a, horses)) : [], [data, draft, activeTab, horses]);

  useEffect(() => {
    if (!dirty) return;
    const prevent = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = ''; };
    window.addEventListener('beforeunload', prevent);
    return () => window.removeEventListener('beforeunload', prevent);
  }, [dirty]);

  const guard = (run: () => void) => {
    if (dirty) setPendingAction({ run }); else run();
  };
  const stage = (next: AssignmentDraft) => {
    setDraft(next); setActionError(null); setPicker(null); setPreview(null); setSelectedStaff(null);
  };
  const propose = (target: AssignmentTarget, personId: number | null, drag?: DragStaff) => {
    if (!data || !draft || editDisabled) return;
    try {
      const next = planAssignment(data, draft, target, personId, drag);
      setPicker(null); setDragging(null);
      if (next.changes.some(c => c.before !== 'Unassigned')) setPreview(next);
      else stage(next.draft);
    } catch (error) {
      setActionError((error as Error).message); setDragging(null);
    }
  };
  const assign = (target: AssignmentTarget, staff: DragStaff) => {
    if (target.kind !== staff.kind) return;
    propose(target, staff.personId, staff);
  };
  const pick = (target: AssignmentTarget) => {
    if (selectedStaff?.kind === target.kind) assign(target, selectedStaff);
    else setPicker(target);
  };
  let canReturn = false;
  if (data && draft && dragging?.source) {
    try { planAssignment(data, draft, dragging.source, null); canReturn = true; } catch { /* A staffed occupied block can only be swapped. */ }
  }
  const displayed = attentionOnly ? views.filter(v => v.attention > 0) : views;
  const attention = views.reduce((n, v) => n + v.attention, 0);
  const occupied = views.reduce((n, v) => n + v.occupied, 0);
  const totalStalls = views.reduce((n, v) => n + v.stalls.length, 0);
  const projected = data && draft ? overlayDraft(data, draft) : null;
  const selected = projected?.stalls.find(s => s.id === selectedStall?.id) ?? null;

  return (
    <ScreenLayout variant="dashboard">
      <PageHeader title="Stable" actions={<div className="flex items-center gap-2">
        <Button size="sm" variant="secondary" icon="refresh" disabled={busy} loading={loading && !!data}
          onClick={() => guard(() => { void reload(); })}>Refresh</Button>
        <Button size="sm" variant="primary" icon="plus" disabled={busy || needsReload || !data} loading={creating}
          onClick={() => guard(() => { void addArea(activeTab); })}>Add area</Button>
      </div>} />
      {loadError && <Notice tone="error">{loadError} <Button variant="link" size="sm" disabled={busy}
        onClick={() => guard(() => { void reload(); })}>Retry refresh</Button></Notice>}
      {actionError && <Notice tone="error" onDismiss={() => setActionError(null)}>{actionError}</Notice>}
      {!data || !draft ? loading ? <ListSkeleton rows={4} /> : <Panel padded><EmptyState title="Unable to load Stable" icon="building"
        action={<Button onClick={() => { void reload(); }}>Retry</Button>} /></Panel> : <>
        <Tabs tabs={[
          { id: 'REGULAR', label: 'Regular', count: data.areas.filter(a => a.type === 'REGULAR').length },
          { id: 'QUARANTINE', label: 'Quarantine', count: data.areas.filter(a => a.type === 'QUARANTINE').length },
        ]} active={activeTab} onChange={id => {
          setActiveTab(id as AreaType); setAttentionOnly(false); setSelectedStaff(null); setDragging(null);
        }} />
        <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
          <div className="flex flex-wrap items-center gap-3 text-[var(--color-text-secondary)]">
            <span><strong className="font-semibold text-[var(--color-text-primary)]">{views.length}</strong> areas</span>
            <span aria-hidden="true" className="text-[var(--color-border-strong)]">/</span>
            <span><strong className="font-semibold text-[var(--color-text-primary)]">{totalStalls}</strong> stalls</span>
            <span aria-hidden="true" className="text-[var(--color-border-strong)]">/</span>
            <span><strong className="font-semibold text-[var(--color-text-primary)]">{occupied}</strong> occupied</span>
          </div>
          <Button variant={attentionOnly ? 'secondary' : 'tertiary'} size="sm" aria-pressed={attentionOnly}
            onClick={() => setAttentionOnly(v => !v)}>Needs attention{attention > 0 ? ` (${attention})` : ''}</Button>
        </div>
        {selectedStaff && <div className="flex items-center justify-between rounded-[var(--radius-md)] bg-[var(--color-primary-soft)] px-3 py-2 text-sm text-[var(--color-primary)]" role="status">
          <span>{personName(data, selectedStaff.kind, selectedStaff.personId)} · Choose {selectedStaff.kind === 'trainer' ? 'an area' : 'a block'}</span>
          <IconButton icon="x" label="Cancel staff selection" size="sm" onClick={() => setSelectedStaff(null)} />
        </div>}
        <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_15rem]">
          <div className="min-w-0">
            {displayed.length ? <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
              {displayed.map(view => <AreaCard key={view.area.id} view={view} data={data} draft={draft} horses={horses}
                disabled={editDisabled} dragging={dragging} onDrag={setDragging} onPick={pick}
                onAssign={assign} onRemove={target => propose(target, null)} onSelectStall={setSelectedStall} />)}
            </div> : <Panel padded><EmptyState icon="building" title={attentionOnly ? 'No areas need attention' : 'No areas yet'}
              action={attentionOnly ? <Button onClick={() => setAttentionOnly(false)}>Show all areas</Button> : undefined} /></Panel>}
          </div>
          <StableStaffPanel key={activeTab} data={data} draft={draft} activeTab={activeTab} disabled={editDisabled}
            dragging={dragging} selected={selectedStaff} canReturn={canReturn} onDrag={setDragging} onSelect={setSelectedStaff}
            onReturn={staff => { if (staff.source) propose(staff.source, null); }} />
        </div>
        {dirty && <Panel className="sticky bottom-4 z-10 flex items-center justify-between gap-3 px-4 py-3 shadow-[var(--shadow-popover)]">
          <div className="flex items-center gap-2 text-sm"><Pill tone="primary" size="sm">{count}</Pill>Pending {count === 1 ? 'change' : 'changes'}</div>
          <div className="flex gap-2">
            <Button variant="secondary" disabled={busy} onClick={() => { discard(); setSelectedStaff(null); }}>Discard</Button>
            <Button variant="primary" icon="check" disabled={editDisabled} loading={saving} onClick={() => { void save(); }}>Save changes</Button>
          </div>
        </Panel>}
      </>}
      {picker && data && draft && <StaffAssignmentPicker key={`${picker.kind}:${picker.areaId}:${picker.kind === 'groom' ? picker.blockNumber : ''}`}
        data={data} draft={draft} target={picker} onClose={() => setPicker(null)} onAssign={assign} />}
      <ConfirmDialog open={preview != null} title={preview && preview.changes.length > 1 ? 'Swap assignments?' : 'Change assignment?'}
        confirmLabel="Apply to draft" onCancel={() => setPreview(null)} onConfirm={() => { if (preview) stage(preview.draft); }}
        description={<div className="space-y-3">{preview?.changes.map(change => <div key={change.label}>
          <p className="font-medium text-[var(--color-text-primary)]">{change.label}</p>
          <p>{change.before} → <span className="font-medium text-[var(--color-text-primary)]">{change.after}</span></p>
          {change.note && <p className="mt-1 text-xs">{change.note}</p>}
        </div>)}</div>} />
      <ConfirmDialog open={pendingAction != null} title="Discard pending changes?" description={`${count} ${count === 1 ? 'change has' : 'changes have'} not been saved.`}
        confirmLabel="Discard and continue" onCancel={() => setPendingAction(null)} onConfirm={() => {
          const action = pendingAction; discard(); setSelectedStaff(null); setPendingAction(null); action?.run();
        }} />
      <Modal open={selected != null} title="Stall details" onClose={() => setSelectedStall(null)}>
        {selected && projected && <StallDetailPanel stall={selected} area={projected.areas.find(a => a.id === selected.areaId)}
          horse={horses.get(selected.id)} hint="">
          <dl className="space-y-2 text-sm"><div className="flex justify-between gap-3"><dt className="text-[var(--color-text-secondary)]">Groom</dt>
            <dd className="text-right">{personName(projected, 'groom', selected.groomId)}</dd></div>
            {projected.areas.find(a => a.id === selected.areaId)?.type === 'REGULAR' && <div className="flex justify-between gap-3">
              <dt className="text-[var(--color-text-secondary)]">Trainer</dt><dd className="text-right">{personName(projected, 'trainer', draft?.trainers[selected.areaId] ?? null)}</dd>
            </div>}
          </dl>
        </StallDetailPanel>}
      </Modal>
    </ScreenLayout>
  );
}
