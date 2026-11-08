import { apiGet, apiPost, apiPatch } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type { StaffSummary, StaffCreationRequest, StaffCreationResponse, StaffDetailResponse, StaffUpdateRequest } from '../types';

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export async function getStaffList(): Promise<ApiResponse<StaffSummary[]>> {
  return apiGet<ApiResponse<StaffSummary[]>>('/api/staff');
}

export async function getStaffDetail(id: number): Promise<ApiResponse<StaffDetailResponse>> {
  return apiGet<ApiResponse<StaffDetailResponse>>(`/api/staff/${id}`);
}

export async function updateStaff(id: number, request: StaffUpdateRequest): Promise<ApiResponse<StaffDetailResponse>> {
  return apiPatch<ApiResponse<StaffDetailResponse>>(`/api/staff/${id}`, request);
}

export async function createStaff(request: StaffCreationRequest): Promise<ApiResponse<StaffCreationResponse>> {
  return apiPost<ApiResponse<StaffCreationResponse>>('/api/staff', request);
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
  return apiGet<ApiResponse<EligibleReplacementTrainer[]>>(`/api/staff/trainers/${id}/eligible-replacements`);
}

export async function updateStaffStatus(
  id: number,
  active: boolean,
  replacementTrainerId?: number
): Promise<ApiResponse<StaffSummary>> {
  return apiPatch<ApiResponse<StaffSummary>>(`/api/staff/${id}/status`, {
    active,
    replacementTrainerId,
  });
}

