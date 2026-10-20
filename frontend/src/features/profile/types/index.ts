export interface UpdateMyProfileRequest {
  fullName: string;
  phone: string | null;
  address: string | null;
  removeAvatar: boolean;
}
