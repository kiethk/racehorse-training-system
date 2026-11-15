import { apiGet, apiPost, apiPatch } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type {
  AccountSummary,
  AccountDetailResponse,
  AccountCreationRequest,
  AccountCreationResponse,
  AccountUpdateRequest,
} from '../types';

export async function getAccountList(): Promise<ApiResponse<AccountSummary[]>> {
  return apiGet<ApiResponse<AccountSummary[]>>('/api/accounts');
}

export async function getAccountDetail(id: number): Promise<ApiResponse<AccountDetailResponse>> {
  return apiGet<ApiResponse<AccountDetailResponse>>(`/api/accounts/${id}`);
}

export async function updateAccount(
  id: number,
  request: AccountUpdateRequest
): Promise<ApiResponse<AccountDetailResponse>> {
  return apiPatch<ApiResponse<AccountDetailResponse>>(`/api/accounts/${id}`, request);
}

export async function createAccount(
  request: AccountCreationRequest
): Promise<ApiResponse<AccountCreationResponse>> {
  return apiPost<ApiResponse<AccountCreationResponse>>('/api/accounts', request);
}

export interface EligibleReplacementTrainer {
  id: number;
  trainerId?: number;
  fullName: string;
  email: string;
  managedAreasCount: number;
  remainingCapacity: number;
  canAccept: boolean;
}

export async function getEligibleReplacementTrainers(
  id: number
): Promise<ApiResponse<EligibleReplacementTrainer[]>> {
  return apiGet<ApiResponse<EligibleReplacementTrainer[]>>(`/api/accounts/trainers/${id}/eligible-replacements`);
}

export async function updateAccountStatus(
  id: number,
  active: boolean,
  replacementTrainerId?: number
): Promise<ApiResponse<AccountSummary>> {
  return apiPatch<ApiResponse<AccountSummary>>(`/api/accounts/${id}/status`, {
    active,
    replacementTrainerId,
  });
}
