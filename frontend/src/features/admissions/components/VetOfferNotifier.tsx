'use client';

import { useCallback, useEffect, useState } from 'react';
import { ApiError, getServerTime } from '@/services/api';
import { admissionsApi } from '../services/api';
import type { PendingVetOfferResponse } from '../types';
import { VetOfferModal } from './VetOfferModal';

export const VET_OFFERS_CHANGED_EVENT = 'rtms:vet-offers-changed';

function formatCountdown(expiresAt: string): string {
  if (!expiresAt) return 'Expired';
  const targetTime = new Date(expiresAt).getTime();
  if (Number.isNaN(targetTime)) return 'Expired';
  const diffMs = targetTime - getServerTime();
  if (diffMs <= 0) return 'Expired';
  const totalSeconds = Math.floor(diffMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}m ${seconds.toString().padStart(2, '0')}s`;
}

function errorText(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 409) return 'This offer is no longer available. The Vet inbox is being refreshed.';
    return error.message;
  }
  return error instanceof Error ? error.message : 'Unable to respond to the offer. Please try again.';
}

export function VetOfferNotifier() {
  const [offers, setOffers] = useState<PendingVetOfferResponse[]>([]);
  const [actioningOfferId, setActioningOfferId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [, setTick] = useState(0);

  const refreshOffers = useCallback(async () => {
    try {
      const pending = await admissionsApi.getPendingOffers();
      setOffers(pending);
    } catch {
      // Authentication and short network interruptions are retried by the next poll.
    }
  }, []);

  useEffect(() => {
    const initialTimer = window.setTimeout(refreshOffers, 0);
    const pollTimer = window.setInterval(refreshOffers, 5000);
    const countdownTimer = window.setInterval(() => setTick((value) => value + 1), 1000);
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') void refreshOffers();
    };
    const handleOffersChanged = () => void refreshOffers();

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener(VET_OFFERS_CHANGED_EVENT, handleOffersChanged);
    return () => {
      window.clearTimeout(initialTimer);
      window.clearInterval(pollTimer);
      window.clearInterval(countdownTimer);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener(VET_OFFERS_CHANGED_EVENT, handleOffersChanged);
    };
  }, [refreshOffers]);

  // FR-ADM-07: Blocking modal is reserved for URGENT offers only; INITIAL/ROUTINE are managed in-page
  const activeOffer =
    offers.find((offer) => offer.careType === 'URGENT' && formatCountdown(offer.expiresAt) !== 'Expired') ?? null;
  const countdown = activeOffer ? formatCountdown(activeOffer.expiresAt) : '';

  async function respond(offer: PendingVetOfferResponse, decision: 'ACCEPT' | 'DENY') {
    try {
      setActioningOfferId(offer.id);
      setError('');
      if (decision === 'ACCEPT') {
        await admissionsApi.acceptOffer(offer.id);
      } else {
        await admissionsApi.declineOffer(offer.id);
      }
      setOffers((current) => current.filter((item) => item.id !== offer.id));
      window.dispatchEvent(new Event(VET_OFFERS_CHANGED_EVENT));
    } catch (cause) {
      setError(errorText(cause));
      await refreshOffers();
    } finally {
      setActioningOfferId(null);
    }
  }

  return (
    <VetOfferModal
      offer={activeOffer}
      countdown={countdown}
      actioning={activeOffer ? actioningOfferId === activeOffer.id : false}
      error={error}
      onAccept={(offer) => void respond(offer, 'ACCEPT')}
      onDeny={(offer) => void respond(offer, 'DENY')}
    />
  );
}
