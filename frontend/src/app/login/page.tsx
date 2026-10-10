'use client';

import { FormEvent, useEffect, useState, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { AuthCard } from '@/components/auth/AuthCard';
import { Button, FormField, Input, LoadingScreen, Notice } from '@/components/ui';
import { useAuth } from '@/context/AuthContext';
import { getRoleRoute } from '@/lib/roleRoute';

function LoginForm() {
  const { user, loading, isAuthenticated, login } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const justRegistered = searchParams.get('registered') === '1';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Redirect already-authenticated users to their role page
  useEffect(() => {
    if (!loading && isAuthenticated && user) {
      router.replace(getRoleRoute(user.role));
    }
  }, [loading, isAuthenticated, user, router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError('');

    try {
      await login(email, password);
      // AuthContext.login() sets user; useEffect above will redirect
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'An unexpected error occurred';
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
      description="Access horse, training, veterinary and stable operations based on your role."
      footer={(
        <>
          Don&apos;t have an account?{' '}
          <Link href="/register" className="font-medium text-[var(--color-primary)] underline-offset-2 hover:underline">
            Create account
          </Link>
        </>
      )}
      note="Use your assigned RTMS credentials."
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <FormField label="Work email" htmlFor="login-email" required>
          <Input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="name@riversidertms.com"
            autoComplete="username"
          />
        </FormField>

        <FormField label="Password" htmlFor="login-password" required>
          <Input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter your password"
            autoComplete="current-password"
          />
        </FormField>

        {/* Success banner after registration */}
        {justRegistered && <Notice tone="success">Account created successfully. Please sign in.</Notice>}

        {error && <Notice tone="error">{error}</Notice>}

        <Button type="submit" id="login-submit" variant="primary" className="h-10 w-full" loading={submitting}>
          Sign in
        </Button>
      </form>
    </AuthCard>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-[var(--color-background)]" />}>
      <LoginForm />
    </Suspense>
  );
}
