'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { stableApi } from '../services/stableService';
import type { AreaType } from '../types';
import type { AssignmentDraft, StableSnapshot } from '../types/manager';
import { assignmentBatch, createDraft } from '../lib/managerAssignments';

async function fetchSnapshot(): Promise<StableSnapshot> {
  const [areas, stalls, horses, trainers, grooms] = await Promise.all([
    stableApi.getAreas(), stableApi.getStalls(), stableApi.getHorses(), stableApi.getTrainers(), stableApi.getGrooms(),
  ]);
  return { areas, stalls, horses, trainers, grooms };
}

export function useManagerStable() {
  const [data, setData] = useState<StableSnapshot | null>(null);
  const [draft, setDraft] = useState<AssignmentDraft | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [creating, setCreating] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [needsReload, setNeedsReload] = useState(false);
  const request = useRef(0);
  const mutation = useRef(false);

  const reload = useCallback(async (savedMessage?: string) => {
    const version = ++request.current;
    setLoading(true); setLoadError(null);
    try {
      const next = await fetchSnapshot();
      if (version !== request.current) return;
      setData(next); setDraft(createDraft(next)); setActionError(null); setNeedsReload(false);
    } catch (error) {
      if (version !== request.current) return;
      setLoadError(savedMessage ? `${savedMessage} Refresh failed. Retry to load current data.`
        : displayError(error, 'Unable to load stable data.'));
      setNeedsReload(true);
    } finally {
      if (version === request.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    const version = ++request.current;
    fetchSnapshot().then(next => {
      if (!active || version !== request.current) return;
      setData(next); setDraft(createDraft(next));
    }).catch(error => {
      if (!active || version !== request.current) return;
      setLoadError(displayError(error, 'Unable to load stable data.')); setNeedsReload(true);
    }).finally(() => {
      if (active && version === request.current) setLoading(false);
    });
    return () => { active = false; };
  }, []);

  const discard = () => {
    if (data) setDraft(createDraft(data));
    setActionError(null);
  };

  const save = async () => {
    if (!data || !draft || mutation.current || needsReload) return;
    mutation.current = true; setSaving(true); setActionError(null);
    try {
      const response = await stableApi.saveAssignments(assignmentBatch(data, draft));
      const updatedAreas = new Map(response.updatedAreas.map(a => [a.id, a]));
      const updatedStalls = new Map(response.updatedStalls.map(s => [s.id, s]));
      const saved = { ...data, areas: data.areas.map(a => updatedAreas.get(a.id) ?? a),
        stalls: data.stalls.map(s => updatedStalls.get(s.id) ?? s) };
      setData(saved); setDraft(createDraft(saved));
      toast.success('Assignments saved');
      await reload('Assignments saved.');
    } catch (error) {
      setActionError(displayError(error, 'Unable to save assignments.'));
    } finally {
      mutation.current = false; setSaving(false);
    }
  };

  const addArea = async (type: AreaType) => {
    if (mutation.current || needsReload) return;
    mutation.current = true; setCreating(true); setActionError(null);
    try {
      const created = await stableApi.createArea(type);
      // Creation has committed. Do not repeat it if the subsequent GET fails.
      if (data) {
        const next = { ...data, areas: [...data.areas, created] };
        setData(next); setDraft(createDraft(next));
      }
      toast.success(`Area ${created.code} created`);
      await reload(`Area ${created.code} created.`);
    } catch (error) {
      setActionError(displayError(error, 'Unable to create area.'));
    } finally {
      mutation.current = false; setCreating(false);
    }
  };

  return { data, draft, setDraft, loading, saving, creating, loadError, actionError,
    needsReload, reload, discard, save, addArea, setActionError };
}
