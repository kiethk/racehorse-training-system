/* eslint-disable no-restricted-syntax */
'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { AuthCard } from '@/components/auth/AuthCard';
import { Button, FormField, Icon, Input, Notice } from '@/components/ui';
import { registerOwner } from '@/services/auth';

const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
const PHONE_REGEX = /^(0|\+84)[3|5|7|8|9][0-9]{8}$|^[+]?[0-9]{9,15}$/;

export default function RegisterPage() {
  const router = useRouter();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  function validate(): boolean {
    const nextErrors: Record<string, string> = {};
    const trimmedName = fullName.trim();
    const trimmedEmail = email.trim();
    const trimmedPhone = phone.trim();
    const trimmedAddress = address.trim();

    // 1. Full name
    if (!trimmedName) {
      nextErrors.fullName = 'Full name is required.';
    } else if (trimmedName.length < 2 || trimmedName.length > 100) {
      nextErrors.fullName = 'Full name must be between 2 and 100 characters.';
    }

    // 2. Email
    if (!trimmedEmail) {
      nextErrors.email = 'Email address is required.';
    } else if (trimmedEmail.length > 255) {
      nextErrors.email = 'Email address cannot exceed 255 characters.';
    } else if (!EMAIL_REGEX.test(trimmedEmail)) {
      nextErrors.email = 'Please enter a valid email address (e.g. name@domain.com).';
    }

    // 3. Phone (Optional)
    if (trimmedPhone) {
      if (trimmedPhone.length > 20 || !PHONE_REGEX.test(trimmedPhone)) {
        nextErrors.phone = 'Please enter a valid phone number (10 digits starting with 0 or international format).';
      }
    }

    // 4. Address (Optional)
    if (trimmedAddress && trimmedAddress.length > 255) {
      nextErrors.address = 'Address cannot exceed 255 characters.';
    }

    // 5. Password
    if (!password) {
      nextErrors.password = 'Password is required.';
    } else if (password.length < 8) {
      nextErrors.password = 'Password must be at least 8 characters long.';
    } else if (password.length > 72) {
      nextErrors.password = 'Password cannot exceed 72 characters.';
    }

    // 6. Confirm Password
    if (!confirmPassword) {
      nextErrors.confirmPassword = 'Confirmation password is required.';
    } else if (password !== confirmPassword) {
      nextErrors.confirmPassword = 'Passwords do not match.';
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;

    setServerError('');
    if (!validate()) return;

    setSubmitting(true);

    try {
      await registerOwner({
        fullName: fullName.trim(),
        email: email.trim().toLowerCase(),
        password,
        phone: phone.trim() || undefined,
        address: address.trim() || undefined,
      });

      // Redirect to login page with registration hint
      router.push('/login?registered=1');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Registration failed. Please try again.';
      setServerError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  const hasMinLength = password.length >= 8;

  return (
    <AuthCard
      title="Create Horse Owner Account"
      description="Register your owner account to submit racehorse admissions and access training telemetry."
      cardWidth="md"
      footer={(
        <>
          Already registered?{' '}
          <Link href="/login" className="font-semibold text-[var(--color-primary)] underline-offset-2 hover:underline">
            Sign in to your account
          </Link>
        </>
      )}
    >
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {serverError && <Notice tone="error">{serverError}</Notice>}

        {/* Full Name */}
        <FormField label="Full name" htmlFor="register-fullname" required error={errors.fullName}>
          <Input
            id="register-fullname"
            type="text"
            value={fullName}
            onChange={(e) => {
              setFullName(e.target.value);
              if (errors.fullName) setErrors((prev) => ({ ...prev, fullName: '' }));
            }}
            placeholder="Nguyen Van A"
            autoComplete="name"
            disabled={submitting}
          />
        </FormField>

        {/* Email Address */}
        <FormField label="Email address" htmlFor="register-email" required error={errors.email}>
          <Input
            id="register-email"
            type="email"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              if (errors.email) setErrors((prev) => ({ ...prev, email: '' }));
            }}
            placeholder="owner@example.com"
            autoComplete="email"
            disabled={submitting}
          />
        </FormField>

        {/* Contact Grid: Phone & Address in 1 row */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <FormField label="Phone (optional)" htmlFor="register-phone" error={errors.phone}>
            <Input
              id="register-phone"
              type="tel"
              value={phone}
              onChange={(e) => {
                setPhone(e.target.value);
                if (errors.phone) setErrors((prev) => ({ ...prev, phone: '' }));
              }}
              placeholder="0901 234 567"
              autoComplete="tel"
              disabled={submitting}
            />
          </FormField>

          <FormField label="Address (optional)" htmlFor="register-address" error={errors.address}>
            <Input
              id="register-address"
              type="text"
              value={address}
              onChange={(e) => {
                setAddress(e.target.value);
                if (errors.address) setErrors((prev) => ({ ...prev, address: '' }));
              }}
              placeholder="Ho Chi Minh City"
              autoComplete="street-address"
              disabled={submitting}
            />
          </FormField>
        </div>

        {/* Password */}
        <FormField
          label="Password"
          htmlFor="register-password"
          required
          error={errors.password}
          hint={
            password ? (
              <span className={`inline-flex items-center gap-1.5 text-xs ${hasMinLength ? 'text-emerald-500 font-medium' : 'text-[var(--color-text-muted)]'}`}>
                <Icon name={hasMinLength ? 'check' : 'circle'} size={12} />
                At least 8 characters
              </span>
            ) : undefined
          }
        >
          <div className="relative">
            <Input
              id="register-password"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (errors.password) setErrors((prev) => ({ ...prev, password: '' }));
              }}
              placeholder="Create a secure password"
              autoComplete="new-password"
              className="pr-10"
              disabled={submitting}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)] transition-colors focus:outline-none"
              tabIndex={-1}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              <Icon name={showPassword ? 'eye-off' : 'eye'} size={18} />
            </button>
          </div>
        </FormField>

        {/* Confirm Password */}
        <FormField label="Confirm password" htmlFor="register-confirm-password" required error={errors.confirmPassword}>
          <div className="relative">
            <Input
              id="register-confirm-password"
              type={showConfirmPassword ? 'text' : 'password'}
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                if (errors.confirmPassword) setErrors((prev) => ({ ...prev, confirmPassword: '' }));
              }}
              placeholder="Repeat your password"
              autoComplete="new-password"
              className="pr-10"
              disabled={submitting}
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)] transition-colors focus:outline-none"
              tabIndex={-1}
              aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
            >
              <Icon name={showConfirmPassword ? 'eye-off' : 'eye'} size={18} />
            </button>
          </div>
        </FormField>

        <Button
          type="submit"
          id="register-submit"
          variant="primary"
          className="h-10 w-full mt-2 font-medium shadow-md"
          loading={submitting}
        >
          Create account
        </Button>
      </form>
    </AuthCard>
  );
}
