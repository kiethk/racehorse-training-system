'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { AuthCard } from '@/components/auth/AuthCard';
import { Button, FormField, Input, Notice } from '@/components/ui';
import { registerOwner } from '@/services/auth';

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
    <AuthCard
      title="Create an account"
      description="Register as a Horse Owner to submit admission applications."
      footer={(
        <>
          Already have an account?{' '}
          <Link href="/login" className="font-medium text-[var(--color-primary)] underline-offset-2 hover:underline">
            Sign in
          </Link>
        </>
      )}
    >
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <FormField label="Full name" htmlFor="register-fullname" required>
          <Input
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            placeholder="Nguyen Van A"
            autoComplete="name"
          />
        </FormField>

        <FormField label="Email" htmlFor="register-email" required>
          <Input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="yourname@example.com"
            autoComplete="email"
          />
        </FormField>

        <FormField label="Phone (optional)" htmlFor="register-phone">
          <Input
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="0901 234 567"
            autoComplete="tel"
          />
        </FormField>

        <FormField label="Address (optional)" htmlFor="register-address">
          <Input
            type="text"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            placeholder="123 Nguyen Hue, Ho Chi Minh City"
            autoComplete="street-address"
          />
        </FormField>

        <FormField label="Password" htmlFor="register-password" required>
          <Input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Create a password"
            autoComplete="new-password"
          />
        </FormField>

        <FormField label="Confirm password" htmlFor="register-confirm-password" required>
          <Input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Repeat your password"
            autoComplete="new-password"
          />
        </FormField>

        {error && <Notice tone="error">{error}</Notice>}

        <Button type="submit" id="register-submit" variant="primary" className="h-10 w-full" loading={submitting}>
          Create account
        </Button>
      </form>
    </AuthCard>
  );
}
