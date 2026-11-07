import type { StaffCreationRequest } from '../types/index';

export type StaffCreationErrors = Partial<Record<keyof StaffCreationRequest, string>>;
export function localToday() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

export function generateSecurePassword(): string {
  const upper = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
  const lower = 'abcdefghijkmnpqrstuvwxyz';
  const digits = '23456789';
  const special = '@#$%&*!';
  const all = upper + lower + digits + special;
  const chars = [
    upper[Math.floor(Math.random() * upper.length)],
    lower[Math.floor(Math.random() * lower.length)],
    digits[Math.floor(Math.random() * digits.length)],
    special[Math.floor(Math.random() * special.length)],
  ];
  for (let i = chars.length; i < 12; i++) {
    chars.push(all[Math.floor(Math.random() * all.length)]);
  }
  return chars.sort(() => Math.random() - 0.5).join('');
}

export function validateStaffCreation(value: StaffCreationRequest, today = localToday()): StaffCreationErrors {
  const errors: StaffCreationErrors = {};
  if (!value.fullName.trim()) errors.fullName = 'Full name is required.';
  else if (value.fullName.trim().length > 255) errors.fullName = 'Use at most 255 characters.';
  if (!value.email.trim() || !/^[^\s@]+@[^\s@]+$/.test(value.email.trim()) || value.email.trim().length > 255) errors.email = 'Enter a valid email address.';
  if (!value.password?.trim()) errors.password = 'Password is required.';
  else if (value.password.length < 8) errors.password = 'Password must be between 8 and 72 characters.';
  else if (new TextEncoder().encode(value.password).length > 72) errors.password = 'Use at most 72 UTF-8 bytes.';
  if (value.phone?.trim()) {
    const trimmed = value.phone.trim();
    if (trimmed.length > 20 || !/^((0|\+84)[3|5|7|8|9][0-9]{8}|[+]?[0-9]{10,15})$/.test(trimmed)) {
      errors.phone = 'Enter a valid phone number (10-15 digits).';
    }
  }
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
