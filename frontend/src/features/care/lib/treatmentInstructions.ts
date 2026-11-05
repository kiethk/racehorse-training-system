import type { PrescriptionJsonItem, PrescriptionRoute } from '../types';

export interface ParsedTreatmentInstructions {
  isJson: boolean;
  items: PrescriptionJsonItem[];
  rawText: string;
}

/**
 * Safely parses the treatment plan's instructions string.
 * If formatted as a JSON array of prescriptions, returns the structured items.
 * If formatted as legacy plain text or invalid JSON, falls back gracefully.
 */
export function parseTreatmentInstructions(rawInstructions: string | null | undefined): ParsedTreatmentInstructions {
  const rawText = (rawInstructions ?? '').trim();
  if (!rawText) {
    return { isJson: false, items: [], rawText: '' };
  }

  try {
    const parsed = JSON.parse(rawText);
    if (Array.isArray(parsed) && parsed.length > 0) {
      const items: PrescriptionJsonItem[] = parsed.map((item) => {
        const rawRoute = (item.route ?? 'ORAL').toString().toUpperCase().trim();
        let normalizedRoute: PrescriptionRoute = 'ORAL';
        if (rawRoute === 'INJECTION_IV' || rawRoute === 'IV') {
          normalizedRoute = 'INJECTION_IV';
        } else if (rawRoute === 'INJECTION_IM' || rawRoute === 'IM') {
          normalizedRoute = 'INJECTION_IM';
        } else if (rawRoute === 'FEED_ADDITIVE' || rawRoute === 'FEED' || rawRoute === 'FEED ADDITIVE') {
          normalizedRoute = 'FEED_ADDITIVE';
        } else if (rawRoute === 'TOPICAL') {
          normalizedRoute = 'TOPICAL';
        } else if (rawRoute === 'ORAL') {
          normalizedRoute = 'ORAL';
        }

        return {
          medication_name: String(item.medication_name ?? item.medicationName ?? 'Unknown Medication'),
          dosage: String(item.dosage ?? ''),
          frequency: String(item.frequency ?? ''),
          route: normalizedRoute,
          duration: Number(item.duration ?? 0),
          notes: item.notes ? String(item.notes) : undefined,
        };
      });

      return {
        isJson: true,
        items,
        rawText,
      };
    }
  } catch {
    // Fall back to plain text
  }

  return {
    isJson: false,
    items: [],
    rawText,
  };
}
