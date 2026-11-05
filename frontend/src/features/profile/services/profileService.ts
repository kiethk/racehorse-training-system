import { apiUpload } from '@/services/api';
import { getCurrentUser } from '@/services/auth';
import type { ApiResponse } from '@/types/horse';
import type { AuthUser } from '@/types/auth';
import type { UpdateMyProfileRequest } from '../types';

export const profileService = {
  get: getCurrentUser,
  update(profile: UpdateMyProfileRequest, avatar: File | null) {
    const body = new FormData();
    body.append('profile', new Blob([JSON.stringify(profile)], { type: 'application/json' }));
    if (avatar) body.append('avatar', avatar);
    return apiUpload<ApiResponse<AuthUser>>('/api/auth/me/profile', body);
  },
};
