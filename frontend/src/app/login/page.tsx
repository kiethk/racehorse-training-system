'use client';

import { FormEvent, useEffect, useState, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { AuthCard } from '@/components/auth/AuthCard';
import { Button, FormField, Icon, Input, LoadingScreen, Notice } from '@/components/ui';
import { useAuth } from '@/context/AuthContext';
import { getRoleRoute } from '@/lib/roleRoute';

function LoginForm() {
  const { user, loading, isAuthenticated, login } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const justRegistered = searchParams.get('registered') === '1';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Redirect already-authenticated users to their role page
  useEffect(() => {
    if (!loading && isAuthenticated && user) {
      router.replace(getRoleRoute(user.role));
    }
  }, [loading, isAuthenticated, user, router]);

  function validate(): boolean {
    let valid = true;
    setEmailError('');
    setPasswordError('');
    setError('');

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setEmailError('Email is required.');
      valid = false;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      setEmailError('Please enter a valid email address.');
      valid = false;
    }

    if (!password) {
      setPasswordError('Password is required.');
      valid = false;
    }

    return valid;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;

    if (!validate()) return;

    setSubmitting(true);
    setError('');

    try {
      await login(email.trim(), password);
      // AuthContext.login() sets user; useEffect above will redirect
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Invalid email or password.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  // While restoring session, show a spinner (root page.tsx handles loading)
  if (loading) return <LoadingScreen fullScreen />;

  // Already authenticated — show nothing while redirect fires
  if (isAuthenticated) return null;

  return (
    <AuthCard
      title="Sign in to RTMS"
      description="Access racehorse conditioning, veterinary records, and stable operations."
      footer={(
        <>
          New to Riverside?{' '}
          <Link href="/register" className="font-semibold text-[var(--color-primary)] underline-offset-2 hover:underline">
            Register as Horse Owner
          </Link>
        </>
      )}
      note="Staff credentials are provided by Club Management. Contact administration if you require access."
    >
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        {/* Success banner after registration */}
        {justRegistered && (
          <Notice tone="success" title="Account Created">
            Registration confirmed! Please sign in with your email and password.
          </Notice>
        )}

        {error && <Notice tone="error">{error}</Notice>}

        <FormField label="Email address" htmlFor="login-email" required error={emailError}>
          <Input
            id="login-email"
            type="email"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              if (emailError) setEmailError('');
            }}
            placeholder="name@riversidertms.com"
            autoComplete="username"
            disabled={submitting}
          />
        </FormField>

        <FormField label="Password" htmlFor="login-password" required error={passwordError}>
          <div className="relative">
            <Input
              id="login-password"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (passwordError) setPasswordError('');
              }}
              placeholder="Enter your password"
              autoComplete="current-password"
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

        <Button
          type="submit"
          id="login-submit"
          variant="primary"
          className="h-10 w-full font-medium"
          loading={submitting}
        >
          Sign in
        </Button>
      </form>
    </AuthCard>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<LoadingScreen fullScreen />}>
      <LoginForm />
    </Suspense>
  );
}
