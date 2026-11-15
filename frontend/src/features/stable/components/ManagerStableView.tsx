'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { Button, ConfirmDialog, EmptyState, FilterBar, IconButton, LinkButton, ListSkeleton, Modal, Notice, PageHeader, Panel, Pill, ScreenLayout, SearchInput, SegmentedControl, Tabs } from '@/components/ui';
import type { Area, AreaType, StableStall } from '../types';
import type { AssignmentDraft, AssignmentTarget, DragStaff } from '../types/manager';
import { useManagerStable } from '../hooks/useManagerStable';
import { areaView, compareAreas, editCount, horseMap, overlayDraft, personName, planAssignment, staffLocations, trainerChangeReason } from '../lib/managerAssignments';
import { toast } from '@/lib/toast';
import { ChangeHorseStatusModal } from '@/features/horses/components/ChangeHorseStatusModal';
import { StallDetailPanel } from '../shared/components/StallDetailPanel';
import { StableMapView } from '../shared/components/StableMapView';
import { blockNumberOf } from '../shared/lib/stallLayout';
import { AreaCard } from './AreaCard';
import { StableStaffPanel } from './StableStaffPanel';
import { StaffAssignmentPicker } from './StaffAssignmentPicker';
import { staffDragDrop } from './staffDragDrop';
import { DeleteAreaDialog } from './DeleteAreaDialog';

type Preview = ReturnType<typeof planAssignment>;

export function ManagerStableView() {
  return <staffDragDrop.Provider><ManagerStableContent /></staffDragDrop.Provider>;
}

function ManagerStableContent() {
  const searchParams = useSearchParams();
  const stallCode = searchParams.get('stall');
  const targetAreaId = searchParams.get('areaId') ? Number(searchParams.get('areaId')) : null;
  const targetStallId = searchParams.get('stallId') ? Number(searchParams.get('stallId')) : null;
  const targetStallCode = searchParams.get('stallCode')?.trim() || null;

  const state = useManagerStable();
  const { data, draft, setDraft, loading, saving, creating, deleting, loadError, actionError, needsReload,
    reload, discard, save, addArea, deleteArea, setActionError } = state;
  const [activeTab, setActiveTab] = useState<AreaType>('REGULAR');
  const [attentionOnly, setAttentionOnly] = useState(false);
  const [confirmAddArea, setConfirmAddArea] = useState<AreaType | null>(null);
  const [search, setSearch] = useState('');
  const { activeItem: dragging, cancel: cancelDrag } = staffDragDrop.useDragDrop();
  const [selectedStaff, setSelectedStaff] = useState<DragStaff | null>(null);
  const [picker, setPicker] = useState<AssignmentTarget | null>(null);
  const [preview, setPreview] = useState<Preview | null>(null);
  const [pendingAction, setPendingAction] = useState<{ run: () => void } | null>(null);
  const [selectedStall, setSelectedStall] = useState<StableStall | null>(null);
  const appliedStallCode = useRef<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Area | null>(null);
  const [statusOpen, setStatusOpen] = useState(false);
  // Cards hold the drag-and-drop staffing; the map shows the same (draft) state as a plan or 3D barn.
  const [layout, setLayout] = useState<'cards' | 'map'>('cards');
  useEffect(() => {
    if (!stallCode || !data || appliedStallCode.current === stallCode) return;
    const stall = data.stalls.find((item) => item.stallCode === stallCode);
    if (!stall) return;
    appliedStallCode.current = stallCode;
    const area = data.areas.find((item) => item.id === stall.areaId);
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (area) setActiveTab(area.type);
    setSelectedStall(stall);
    setLayout('map');
  }, [stallCode, data]);
  const count = data && draft ? editCount(data, draft) : 0;
  const dirty = count > 0;
  const busy = loading || saving || creating || deleting;
  const editDisabled = busy || needsReload || deleteTarget != null;
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
      setPicker(null); cancelDrag();
      if (next.changes.some(c => c.before !== 'Unassigned')) setPreview(next);
      else stage(next.draft);
    } catch (error) {
      // Shown as a toast too: the inline notice sits at the top of the page, often out of view (e.g. on the map).
      setActionError((error as Error).message); cancelDrag();
      toast.error('Assignment not possible', (error as Error).message);
    }
  };
  const assign = (target: AssignmentTarget, staff: DragStaff) => {
    if (target.kind !== staff.kind) return;
    propose(target, staff.personId, staff);
  };
  /**
   * A trainer runs whole areas, so putting one on an area that already has a trainer swaps their areas.
   * With two other areas it is ambiguous which one to swap, so the picker lets the Manager choose.
   */
  const assignTrainer = (areaId: number, personId: number) => {
    if (!data || !draft || draft.trainers[areaId] === personId) return;
    const others = staffLocations(data, draft, 'trainer', personId).filter(t => t.areaId !== areaId);
    const target: AssignmentTarget = { kind: 'trainer', areaId };
    if (draft.trainers[areaId] != null && others.length > 1) { setPicker(target); return; }
    const source = draft.trainers[areaId] != null ? others[0] : undefined;
    assign(target, { kind: 'trainer', personId, source });
  };
  const pick = (target: AssignmentTarget) => {
    if (selectedStaff?.kind === target.kind) assign(target, selectedStaff);
    else setPicker(target);
  };
  let canReturn = false;
  if (data && draft && dragging?.source) {
    try { planAssignment(data, draft, dragging.source, null); canReturn = true; } catch { /* A staffed occupied block can only be swapped. */ }
  }
  useEffect(() => {
    if (!data) return;

    if (targetStallId || targetStallCode) {
      const stall = data.stalls.find(
        (s) =>
          (targetStallId && s.id === targetStallId) ||
          (targetStallCode && s.stallCode.toLowerCase() === targetStallCode.toLowerCase())
      );
      if (stall) {
        const area = data.areas.find((a) => a.id === stall.areaId);
        if (area && area.type !== activeTab) {
          // eslint-disable-next-line react-hooks/set-state-in-effect
          setActiveTab(area.type);
        }
        setSelectedStall(stall);
        const timer = setTimeout(() => {
          const el = document.getElementById(`stall-cell-${stall.id}`) || document.getElementById(`area-card-${stall.areaId}`);
          el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 200);
        return () => clearTimeout(timer);
      } else {
        toast.info(`Stall #${targetStallId || targetStallCode} was not found.`);
      }
    }

    if (targetAreaId) {
      const area = data.areas.find((a) => a.id === targetAreaId);
      if (area) {
        if (area.type !== activeTab) {
          setActiveTab(area.type);
        }
        const timer = setTimeout(() => {
          const el = document.getElementById(`area-card-${targetAreaId}`);
          el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 200);
        return () => clearTimeout(timer);
      } else {
        toast.info(`Area #${targetAreaId} was removed or does not exist.`);
      }
    }
  }, [data, targetAreaId, targetStallId, targetStallCode]);

  const highlightedStallIds = useMemo(() => {
    const ids = new Set<number>();
    if (targetStallId) ids.add(targetStallId);
    if (targetStallCode && data) {
      const found = data.stalls.find((s) => s.stallCode.toLowerCase() === targetStallCode.toLowerCase());
      if (found) ids.add(found.id);
    }
    if (selectedStall) ids.add(selectedStall.id);
    const q = search.trim().toLowerCase();
    if (!q || !data) return ids;
    data.stalls.forEach(stall => {
      const horse = horses.get(stall.id);
      if (stall.stallCode.toLowerCase().includes(q)) {
        ids.add(stall.id);
      } else if (horse && (horse.name.toLowerCase().includes(q) || (horse.breed && horse.breed.toLowerCase().includes(q)) || (horse.registrationNumber && horse.registrationNumber.toLowerCase().includes(q)))) {
        ids.add(stall.id);
      }
    });
    return ids;
  }, [search, data, horses, targetStallId, targetStallCode]);

  const displayed = useMemo(() => {
    const list = attentionOnly ? views.filter(v => v.attention > 0) : views;
    const q = search.trim().toLowerCase();
    if (!q || !data) return list;
    return list.filter(v => {
      if (v.area.code.toLowerCase().includes(q) || v.area.name.toLowerCase().includes(q)) return true;
      const trainer = draft?.trainers[v.area.id] != null ? personName(data, 'trainer', draft.trainers[v.area.id]) : '';
      if (trainer.toLowerCase().includes(q)) return true;
      return v.stalls.some(stall => {
        if (stall.stallCode.toLowerCase().includes(q)) return true;
        const groomId = draft?.grooms[stall.id];
        if (groomId != null && personName(data, 'groom', groomId).toLowerCase().includes(q)) return true;
        const horse = horses.get(stall.id);
        if (horse && (horse.name.toLowerCase().includes(q) || (horse.breed && horse.breed.toLowerCase().includes(q)) || (horse.registrationNumber && horse.registrationNumber.toLowerCase().includes(q)))) {
          return true;
        }
        return false;
      });
    });
  }, [views, attentionOnly, search, data, draft, horses]);
  const attention = views.reduce((n, v) => n + v.attention, 0);
  const occupied = views.reduce((n, v) => n + v.occupied, 0);
  const totalStalls = views.reduce((n, v) => n + v.stalls.length, 0);
  const projected = data && draft ? overlayDraft(data, draft) : null;
  const selected = projected?.stalls.find(s => s.id === selectedStall?.id) ?? null;
  const mapAreas = projected ? projected.areas.filter(a => a.type === activeTab) : [];

  const selectedHorse = selected ? horses.get(selected.id) : undefined;
  const selectedArea = selected ? projected?.areas.find((a) => a.id === selected.areaId) : undefined;
  const canChangeStatus = selectedHorse?.currentStatus === 'ELIGIBLE' || selectedHorse?.currentStatus === 'INACTIVE';
  const stallDetail = selected && projected ? (
    <StallDetailPanel
      onClose={layout === 'map' ? () => setSelectedStall(null) : undefined}
      stall={selected}
      area={selectedArea}
      horse={selectedHorse}
      groomName={personName(projected, 'groom', selected.groomId)}
      groomId={selected.groomId}
      trainerName={selectedArea?.type === 'REGULAR' ? personName(projected, 'trainer', draft?.trainers[selected.areaId] ?? null) : undefined}
      trainerId={selectedArea?.type === 'REGULAR' ? (draft?.trainers[selected.areaId] ?? selectedArea.trainerId ?? null) : null}
      hint=""
      actions={selectedHorse ? <>
        <LinkButton href={`/manager/horses/${selectedHorse.id}`} variant="secondary" icon="external-link">Horse details</LinkButton>
        {canChangeStatus && <Button variant={selectedHorse.currentStatus === 'INACTIVE' ? 'primary' : 'warning'}
          icon={selectedHorse.currentStatus === 'INACTIVE' ? 'check' : 'alert-triangle'} disabled={busy}
          // A status change reloads the stable, so unsaved staffing changes are confirmed first.
          onClick={() => guard(() => setStatusOpen(true))}>
          {selectedHorse.currentStatus === 'INACTIVE' ? 'Restore to Active' : 'Change status'}
        </Button>}
      </> : undefined}
    />
  ) : null;

  return (
    <ScreenLayout variant="dashboard">
      <PageHeader title="Stable" actions={<div className="flex items-center gap-2">
        <SegmentedControl<'cards' | 'map'> label="Layout" value={layout} onChange={(next) => { setLayout(next); setSelectedStaff(null); cancelDrag(); }}
          options={[{ value: 'cards', label: 'Cards', icon: 'grid' }, { value: 'map', label: 'Map', icon: 'layers' }]} />
        <Button size="sm" variant="secondary" icon="refresh" disabled={busy} loading={loading && !!data}
          onClick={() => guard(() => { void reload(); })}>Refresh</Button>
        <Button size="sm" variant="primary" icon="plus" disabled={busy || needsReload || !data} loading={creating}
          onClick={() => guard(() => { setConfirmAddArea(activeTab); })}>Add area</Button>
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
          setActiveTab(id as AreaType); setAttentionOnly(false); setSelectedStaff(null); cancelDrag();
        }} />
        <FilterBar className="p-3 gap-3 items-center justify-between" onSubmit={(e) => e.preventDefault()}>
          <div className="flex flex-1 items-center gap-3 min-w-[220px] max-w-md">
            <SearchInput
              value={search}
              onChange={setSearch}
              placeholder="Search horse, stall (e.g. A04), or staff..."
              className="w-full text-sm"
            />
          </div>
          <div className="flex flex-wrap items-center gap-3 text-sm">
            <div className="hidden sm:flex items-center gap-2 text-xs text-[var(--color-text-secondary)]">
              <span><strong className="font-semibold text-[var(--color-text-primary)]">{views.length}</strong> areas</span>
              <span aria-hidden="true" className="text-[var(--color-border-strong)]">/</span>
              <span><strong className="font-semibold text-[var(--color-text-primary)]">{totalStalls}</strong> stalls</span>
              <span aria-hidden="true" className="text-[var(--color-border-strong)]">/</span>
              <span><strong className="font-semibold text-[var(--color-text-primary)]">{occupied}</strong> occupied</span>
            </div>
            <Button
              variant={attentionOnly ? 'secondary' : 'tertiary'}
              size="sm"
              aria-pressed={attentionOnly}
              onClick={() => setAttentionOnly(v => !v)}
            >
              Needs attention{attention > 0 ? ` (${attention})` : ''}
            </Button>
          </div>
        </FilterBar>
        {selectedStaff && <div className="flex items-center justify-between rounded-[var(--radius-md)] bg-[var(--color-primary-soft)] px-3 py-2 text-sm text-[var(--color-primary)]" role="status">
          <span>
            {personName(data, selectedStaff.kind, selectedStaff.personId)} · Choose {selectedStaff.kind === 'trainer' ? 'an area' : 'a block'}
            {selectedStaff.kind === 'trainer' && (() => {
              const locked = data.areas.filter(a => a.type === 'REGULAR' && trainerChangeReason(data, a.id)).map(a => `Area ${a.code}`);
              return locked.length > 0
                ? <span className="block text-xs text-[var(--color-text-secondary)]">Trainer cannot change while an area has horses or unfinished training: {locked.join(', ')}</span>
                : null;
            })()}
          </span>
          <IconButton icon="x" label="Cancel staff selection" size="sm" onClick={() => setSelectedStaff(null)} />
        </div>}
        {layout === 'map' && projected ? (
          <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_15rem]">
          <StableMapView
            lens="trainer"
            areas={mapAreas}
            stalls={projected.stalls}
            horseByStallId={horses}
            selectedStallId={selectedStall?.id ?? null}
            onSelectStall={(stall) => {
              // With a person picked in the staff list, clicking a stall assigns them (trainer: its area; groom: its block).
              if (stall && selectedStaff) {
                if (selectedStaff.kind === 'trainer') assignTrainer(stall.areaId, selectedStaff.personId);
                else pick({ kind: 'groom', areaId: stall.areaId, blockNumber: blockNumberOf(stall.stallNumber) });
                return;
              }
              setSelectedStall(stall);
            }}
            onMoveGroom={editDisabled ? undefined : (area, groomId, toBlockNumber) => {
              const from = projected.stalls.find(st => st.areaId === area.id && st.groomId === groomId);
              assign({ kind: 'groom', areaId: area.id, blockNumber: toBlockNumber }, {
                kind: 'groom',
                personId: groomId,
                source: from ? { kind: 'groom', areaId: area.id, blockNumber: blockNumberOf(from.stallNumber) } : undefined,
              });
            }}
            onPickTrainer={editDisabled ? undefined : (area) => {
              // With a trainer picked in the staff list, assign them straight away; otherwise choose one.
              if (selectedStaff?.kind === 'trainer') assignTrainer(area.id, selectedStaff.personId);
              else setPicker({ kind: 'trainer', areaId: area.id });
            }}
            personName={(kind, id) => personName(projected, kind, id)}
            detail={stallDetail}
            legendNote={dirty ? 'Unsaved staffing changes are shown on the map.' : undefined}
            emptyTitle="No areas yet"
            cellFooter={(stall) => (
              <span className="block truncate text-xs text-[var(--color-text-secondary)]">
                {stall.groomId ? personName(projected, 'groom', stall.groomId) : <span className="italic text-[var(--color-text-muted)]">No groom</span>}
              </span>
            )}
          />
          <StableStaffPanel key={`map-${activeTab}`} data={data} draft={draft} activeTab={activeTab} disabled={editDisabled}
            selected={selectedStaff} canReturn={canReturn} onSelect={setSelectedStaff}
            onReturn={staff => { if (staff.source) propose(staff.source, null); }} />
          </div>
        ) : (
        <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_15rem]">
          <div className="min-w-0">
            {displayed.length ? <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
              {displayed.map(view => <AreaCard key={view.area.id} view={view} data={data} draft={draft} horses={horses}
                highlightedStallIds={highlightedStallIds}
                isAreaHighlighted={targetAreaId === view.area.id}
                selectedStallId={selectedStall?.id}
                disabled={editDisabled} onPick={pick}
                onDelete={() => guard(() => { cancelDrag(); setSelectedStaff(null); setActionError(null); setDeleteTarget(view.area); })}
                onAssign={assign} onRemove={target => propose(target, null)} onSelectStall={setSelectedStall} />)}
            </div> : <Panel padded><EmptyState icon="building" title={attentionOnly ? 'No areas need attention' : 'No areas yet'}
              action={attentionOnly ? <Button onClick={() => setAttentionOnly(false)}>Show all areas</Button> : undefined} /></Panel>}
          </div>
          <StableStaffPanel key={activeTab} data={data} draft={draft} activeTab={activeTab} disabled={editDisabled}
            selected={selectedStaff} canReturn={canReturn} onSelect={setSelectedStaff}
            onReturn={staff => { if (staff.source) propose(staff.source, null); }} />
        </div>
        )}
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
            <ConfirmDialog
        open={confirmAddArea != null}
        title={`Add new ${confirmAddArea === 'REGULAR' ? 'Regular' : 'Quarantine'} Area?`}
        tone="primary"
        confirmLabel="Add area"
        loading={creating}
        onCancel={() => setConfirmAddArea(null)}
        onConfirm={async () => {
          if (!confirmAddArea) return;
          const targetType = confirmAddArea;
          await addArea(targetType);
          setConfirmAddArea(null);
        }}
        description={
          <p>
            A new {confirmAddArea === 'REGULAR' ? 'Regular' : 'Quarantine'} Area with 12 stalls (4 blocks) will be created and added to the stable.
          </p>
        }
      />
      {deleteTarget && <DeleteAreaDialog key={deleteTarget.id} area={deleteTarget} deleting={deleting} actionError={actionError}
        onClose={() => setDeleteTarget(null)} onDelete={async () => {
          const deleted = await deleteArea(deleteTarget.id);
          if (deleted) { setDeleteTarget(null); setSelectedStall(null); }
          return deleted;
        }} />}
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
      {/* The map shows the stall in its side column; the cards open it here. */}
      <Modal open={layout === 'cards' && selected != null} title="Stall details" onClose={() => setSelectedStall(null)}>
        {layout === 'cards' && stallDetail}
      </Modal>
      <ChangeHorseStatusModal
        horse={statusOpen ? selectedHorse ?? null : null}
        open={statusOpen && selectedHorse != null}
        onClose={() => setStatusOpen(false)}
        onSuccess={() => { setStatusOpen(false); void reload(); }}
      />
    </ScreenLayout>
  );
}
