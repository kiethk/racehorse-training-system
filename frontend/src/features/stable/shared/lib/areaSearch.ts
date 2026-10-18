import type { Area, AreaType } from '../../types';

export interface AreaGroup {
  type: AreaType;
  areas: Area[];
}

const GROUP_ORDER: AreaType[] = ['REGULAR', 'QUARANTINE'];

/**
 * Areas whose code or name contains `query` (case-insensitive), grouped regular first, then quarantine.
 * `areas` should already be ordered (see `orderStableAreas`); the order inside each group is kept.
 */
export function filterAreas(areas: readonly Area[], query: string): AreaGroup[] {
  const needle = query.trim().toLowerCase();
  const matches = needle
    ? areas.filter((area) => area.code.toLowerCase().includes(needle) || area.name.toLowerCase().includes(needle))
    : [...areas];
  return GROUP_ORDER
    .map((type) => ({ type, areas: matches.filter((area) => area.type === type) }))
    .filter((group) => group.areas.length > 0);
}
