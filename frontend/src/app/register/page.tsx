'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Icon } from '@/components/ui/Icon';
import { BrandLogo } from '@/components/ui/BrandLogo';
import { registerOwner } from '@/services/auth';

const inputClassName =
  'mt-1 h-10 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-[13px] text-[var(--color-text-primary)] outline-none transition-colors placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-focus)] focus:ring-2 focus:ring-[var(--color-focus)]/30';

export default function RegisterPage() {
  const router = useRouter();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;

    // Client-side validation
    if (!fullName.trim()) {
      setError('Full name is required.');
      return;
    }
    if (!email.trim()) {
      setError('Email is required.');
      return;
    }
    if (!password) {
      setError('Password is required.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      await registerOwner({
        fullName: fullName.trim(),
        email: email.trim(),
        password,
        phone: phone.trim() || undefined,
        address: address.trim() || undefined,
      });
      // Redirect to login with a success hint
      router.push('/login?registered=1');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'An unexpected error occurred.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-[var(--color-background)] px-4 py-10">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="mb-6 flex items-center justify-center gap-2">
          <BrandLogo className="h-9 w-9" />
          <span className="text-[18px] font-semibold tracking-tight text-[var(--color-text-primary)]">
            RTMS
          </span>
        </div>

        <section className="rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 shadow-xl shadow-black/5">
          <div className="mb-6">
            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-[var(--color-primary)]">
              Riverside Training Club
            </p>
            <h1 className="mt-2 text-[22px] font-semibold tracking-tight text-[var(--color-text-primary)]">
              Create an account
            </h1>
            <p className="mt-1.5 text-[13px] leading-relaxed text-[var(--color-text-secondary)]">
              Register as a Horse Owner to submit admission applications.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {/* Full name */}
            <label className="block">
              <span className="text-[12px] font-medium text-[var(--color-text-primary)]">
                Full name <span className="text-[var(--color-danger)]">*</span>
              </span>
              <input
                id="register-fullname"
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className={inputClassName}
                placeholder="Nguyen Van A"
                autoComplete="name"
                required
              />
            </label>

            {/* Email */}
            <label className="block">
              <span className="text-[12px] font-medium text-[var(--color-text-primary)]">
                Email <span className="text-[var(--color-danger)]">*</span>
              </span>
              <input
                id="register-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={inputClassName}
                placeholder="yourname@example.com"
                autoComplete="email"
                required
              />
            </label>

            {/* Phone (optional) */}
            <label className="block">
              <span className="text-[12px] font-medium text-[var(--color-text-primary)]">
                Phone
                <span className="ml-1 text-[11px] font-normal text-[var(--color-text-muted)]">(optional)</span>
              </span>
              <input
                id="register-phone"
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className={inputClassName}
                placeholder="0901 234 567"
                autoComplete="tel"
              />
            </label>

            {/* Address (optional) */}
            <label className="block">
              <span className="text-[12px] font-medium text-[var(--color-text-primary)]">
                Address
                <span className="ml-1 text-[11px] font-normal text-[var(--color-text-muted)]">(optional)</span>
              </span>
              <input
                id="register-address"
                type="text"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                className={inputClassName}
                placeholder="123 Nguyen Hue, Ho Chi Minh City"
                autoComplete="street-address"
              />
            </label>

            {/* Password */}
            <label className="block">
              <span className="text-[12px] font-medium text-[var(--color-text-primary)]">
                Password <span className="text-[var(--color-danger)]">*</span>
              </span>
              <input
                id="register-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={inputClassName}
                placeholder="Create a password"
                autoComplete="new-password"
                required
              />
            </label>

            {/* Confirm password */}
            <label className="block">
              <span className="text-[12px] font-medium text-[var(--color-text-primary)]">
                Confirm password <span className="text-[var(--color-danger)]">*</span>
              </span>
              <input
                id="register-confirm-password"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className={inputClassName}
                placeholder="Repeat your password"
                autoComplete="new-password"
                required
              />
            </label>

            {/* Error message */}
            {error && (
              <div
                role="alert"
                className="flex items-start gap-2 rounded-[var(--radius-sm)] bg-[var(--color-danger-soft)] px-3 py-2.5 text-[12px] text-[var(--color-danger)]"
              >
                <Icon name="alert-triangle" size={14} className="mt-0.5 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Submit */}
            <button
              type="submit"
              id="register-submit"
              disabled={submitting}
              className="flex h-10 w-full items-center justify-center gap-2 rounded-[var(--radius-sm)] bg-[var(--color-primary)] px-3 text-[13px] font-medium text-[var(--color-text-inverse)] transition-colors hover:bg-[var(--color-primary-hover)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] disabled:cursor-not-allowed disabled:opacity-50"
            >
              {submitting && (
                <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent" />
              )}
              Create account
            </button>
          </form>

          {/* Link back to login */}
          <p className="mt-5 text-center text-[12px] text-[var(--color-text-muted)]">
            Already have an account?{' '}
            <Link
              href="/login"
              className="font-medium text-[var(--color-primary)] underline-offset-2 hover:underline"
            >
              Sign in
            </Link>
          </p>
        </section>
      </div>
    </main>
  );
}
