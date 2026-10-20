'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  EmptyState,
  ListSkeleton,
  Notice,
  Panel,
  Pill,
  ProgressBar,
  Table,
  TBody,
  Td,
  Th,
  THead,
  Tr,
} from '@/components/ui';
import { displayError } from '@/lib/display';
import { preventiveCareService } from '../../services/preventiveCareService';
import type { PreventiveCoverage } from '../../types/preventive';

/** Up to date + due soon: the horse is still protected today. */
const covered = (row: PreventiveCoverage) => row.ok + row.dueSoon;

export function CoverageTab() {
  const [rows, setRows] = useState<PreventiveCoverage[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setRows(await preventiveCareService.coverage());
    } catch (cause) {
      setError(displayError(cause, 'Could not load vaccine coverage.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  if (loading) return <ListSkeleton />;
  if (error) {
    return (
      <Notice tone="error" title="Could not load">
        {error} <Button variant="link" size="sm" onClick={load}>Try again</Button>
      </Notice>
    );
  }
  if (rows.length === 0 || rows[0].totalHorses === 0) {
    return (
      <Panel padded>
        <EmptyState icon="shield" title="No horses in the stable" description="Coverage appears once horses are admitted." />
      </Panel>
    );
  }

  return (
    <Panel>
      <Table bare aria-label="Vaccine coverage">
        <THead>
          <Tr>
            <Th>Protocol</Th>
            <Th className="w-[40%]">Distribution</Th>
            <Th className="text-right">Covered</Th>
            <Th className="text-right">Overdue</Th>
            <Th className="text-right">No record</Th>
          </Tr>
        </THead>
        <TBody>
          {rows.map((row) => {
            const percent = Math.round((covered(row) / row.totalHorses) * 100);
            return (
              <Tr key={row.protocolId}>
                <Td>
                  <div className="flex items-center gap-2">
                    <span className="font-semibold">{row.name}</span>
                    {row.mandatory ? <Pill tone="info" size="sm">Mandatory</Pill> : <Pill tone="neutral" size="sm">Recommended</Pill>}
                  </div>
                </Td>
                <Td>
                  <ProgressBar
                    label={`${row.name} coverage`}
                    max={row.totalHorses}
                    segments={[
                      { value: row.ok, tone: 'success', label: 'Up to date' },
                      { value: row.dueSoon, tone: 'warning', label: 'Due soon' },
                      { value: row.overdue, tone: 'danger', label: 'Overdue' },
                      { value: row.missing + row.none, tone: 'neutral', label: 'No record' },
                    ]}
                  />
                </Td>
                <Td className="text-right tabular-nums">
                  <span className="font-semibold">{percent}%</span>
                  <span className="ml-1 text-xs text-[var(--color-text-muted)]">{covered(row)}/{row.totalHorses}</span>
                </Td>
                <Td className="text-right tabular-nums text-[var(--color-danger)]">{row.overdue}</Td>
                <Td className="text-right tabular-nums">{row.missing + row.none}</Td>
              </Tr>
            );
          })}
        </TBody>
      </Table>
    </Panel>
  );
}
