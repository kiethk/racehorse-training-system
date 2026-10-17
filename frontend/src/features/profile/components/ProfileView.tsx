'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button, DetailSkeleton, EmptyState, PageHeader, ScreenLayout } from '@/components/ui';
import { useAuth } from '@/context/AuthContext';
import { displayError } from '@/lib/display';
import type { AuthUser } from '@/types/auth';
import { profileService } from '../services/profileService';
import { ProfileEditor } from './ProfileEditor';

export function ProfileView() {
  const { user } = useAuth();
  return user ? <ProfileContent key={user.userId} /> : null;
}

function ProfileContent() {
  const { refreshUser } = useAuth();
  const [profile, setProfile] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    let active = true;
    profileService.get().then(response => {
      if (!response.success) throw new Error(response.message || 'Unable to load profile.');
      if (active) setProfile(response.data);
    }).catch(cause => {
      if (active) setError(displayError(cause, 'Unable to load profile.'));
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [attempt]);

  const saved = useCallback(async (updated: AuthUser) => {
    setProfile(updated);
    setRevision(value => value + 1);
    await refreshUser();
  }, [refreshUser]);

  return (
    <ScreenLayout variant="form" className="max-w-5xl">
      <PageHeader title="My profile" />
      {loading ? <DetailSkeleton /> : error || !profile ? (
        <EmptyState title="Profile unavailable" description={error || 'Your profile could not be found.'}
          action={<Button onClick={() => { setError(null); setLoading(true); setAttempt(value => value + 1); }}>Retry</Button>} />
      ) : (
        <ProfileEditor key={`${profile.userId}:${revision}`} profile={profile} onSaved={saved}
          onReset={() => setRevision(value => value + 1)} />
      )}
    </ScreenLayout>
  );
}
