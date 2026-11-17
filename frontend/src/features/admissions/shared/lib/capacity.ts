import type { AdmissionDetailResponse } from '../../types';

type Capacity = AdmissionDetailResponse['capacity'];

/**
 * Manager may approve out of quarantine only when every horse still in quarantine can move to a
 * regular stall. When the backend payload predates the field, keep the button enabled and let the
 * server be the source of truth (it returns 409 REGULAR_RESERVE_INSUFFICIENT).
 */
export function canApproveAdmission(
  capacity?: Pick<Capacity, 'approvalCapacityAvailable'> | null,
): boolean {
  return capacity?.approvalCapacityAvailable ?? true;
}
