import type { StaffCreationRequest } from '../types/index';

export type StaffCreationErrors = Partial<Record<keyof StaffCreationRequest, string>>;
export function localToday() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}
export function validateStaffCreation(value: StaffCreationRequest, today = localToday()): StaffCreationErrors {
  const errors: StaffCreationErrors = {};
  if (!value.fullName.trim()) errors.fullName = 'Full name is required.';
  else if (value.fullName.trim().length > 255) errors.fullName = 'Use at most 255 characters.';
  if (!value.email.trim() || !/^[^\s@]+@[^\s@]+$/.test(value.email.trim()) || value.email.trim().length > 255) errors.email = 'Enter a valid email address.';
  if (!value.password?.trim()) errors.password = 'Password is required.';
  else if (new TextEncoder().encode(value.password).length > 72) errors.password = 'Use at most 72 UTF-8 bytes.';
  if ((value.phone?.trim().length ?? 0) > 20) errors.phone = 'Use at most 20 characters.';
  if ((value.address?.trim().length ?? 0) > 255) errors.address = 'Use at most 255 characters.';
  if (value.role === 'HEAD_TRAINER') {
    if (!value.certificationNumber?.trim()) errors.certificationNumber = 'Certification number is required.';
    else if (value.certificationNumber.trim().length > 100) errors.certificationNumber = 'Use at most 100 characters.';
    if (value.certificationIssuedDate && value.certificationIssuedDate > today) errors.certificationIssuedDate = 'Issued date cannot be in the future.';
  }
  if (value.role === 'VETERINARIAN') {
    if (!value.licenseNumber?.trim()) errors.licenseNumber = 'License number is required.';
    else if (value.licenseNumber.trim().length > 100) errors.licenseNumber = 'Use at most 100 characters.';
    if (value.licenseIssuedDate && value.licenseIssuedDate > today) errors.licenseIssuedDate = 'Issued date cannot be in the future.';
    if ((value.specialization?.trim().length ?? 0) > 255) errors.specialization = 'Use at most 255 characters.';
  }
  return errors;
}
