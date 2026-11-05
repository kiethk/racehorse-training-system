/** Unwraps various backend response shapes into a typed array safely. */
export function unwrapArray<T>(res: unknown): T[] {
  if (Array.isArray(res)) return res as T[];
  if (res && typeof res === 'object') {
    if ('data' in res) {
      const data = (res as { data: unknown }).data;
      if (Array.isArray(data)) return data as T[];
      if (data && typeof data === 'object' && 'content' in data && Array.isArray((data as { content: unknown }).content)) {
        return (data as { content: T[] }).content;
      }
    }
    if ('content' in res && Array.isArray((res as { content: unknown }).content)) {
      return (res as { content: T[] }).content;
    }
  }
  return [];
}

/** Unwraps backend response object safely. */
export function unwrapData<T>(res: unknown): T {
  if (res && typeof res === 'object' && 'data' in res) {
    return (res as { data: T }).data;
  }
  return res as T;
}

/** Normalizes a preventive due item whether the backend sends flat or nested protocol properties. */
export function normalizeDueItem(item: any): any {
  if (!item) return item;
  if (!item.protocol && (item.protocolId != null || item.status != null)) {
    return {
      horseId: item.horseId,
      horseName: item.horseName ?? `Horse #${item.horseId}`,
      horseStatus: item.horseStatus,
      protocol: {
        horseId: item.horseId,
        protocolId: item.protocolId ?? item.id ?? 0,
        code: item.code ?? '',
        name: item.name ?? item.protocolName ?? 'Unknown Protocol',
        category: item.category ?? 'VACCINE',
        mandatory: Boolean(item.mandatory),
        lastDoseOn: item.lastDoseOn ?? null,
        nextDueOn: item.nextDueOn ?? null,
        status: item.status ?? 'NONE',
      },
    };
  }
  return item;
}
