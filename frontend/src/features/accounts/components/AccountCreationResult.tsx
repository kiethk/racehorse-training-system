'use client';

import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { Button, LinkButton, Modal, Pill } from '@/components/ui';
import { formatEnumLabel } from '@/lib/display';
import type { AccountCreationResponse } from '../types';

interface Props {
  result: AccountCreationResponse;
  initialPassword?: string;
  onClose: () => void;
}

export function AccountCreationResult({ result, initialPassword, onClose }: Props) {
  const [copied, setCopied] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const isStaffAllocation = result.role === 'GROOM' || result.role === 'HEAD_TRAINER';
  const assigned =
    result.role === 'GROOM'
      ? result.assignmentStatus === 'ASSIGNED'
      : !!result.assignedAreaCodes?.length;

  const handleCopyCredentials = async () => {
    const textToCopy = `Account Credentials:
Full Name: ${result.fullName}
Email: ${result.email}
Role: ${formatEnumLabel(result.role)}
${initialPassword ? `Initial Password: ${initialPassword}` : ''}`;

    try {
      await navigator.clipboard.writeText(textToCopy.trim());
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      // Fallback if clipboard API is unavailable
    }
  };

  return (
    <Modal
      open
      title="Account Created Successfully"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={handleCopyCredentials}>
            {copied ? 'Copied to Clipboard!' : 'Copy Login Details'}
          </Button>
          {isStaffAllocation && (
            <LinkButton href="/manager/stable" onClick={onClose} variant="secondary">
              Open Stable
            </LinkButton>
          )}
          <Button variant="primary" onClick={onClose}>
            Done
          </Button>
        </>
      }
    >
      <div className="space-y-5">
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4 space-y-2">
          <div className="flex items-center justify-between">
            <p className="text-base font-semibold text-[var(--color-text-primary)]">{result.fullName}</p>
            <span className="text-sm font-medium text-[var(--color-text-primary)]">
              {formatEnumLabel(result.role)}
            </span>
          </div>
          <p className="text-sm text-[var(--color-text-secondary)]">{result.email}</p>
          {result.phone && (
            <p className="text-xs text-[var(--color-text-muted)]">Phone: {result.phone}</p>
          )}
          {result.address && (
            <p className="text-xs text-[var(--color-text-muted)]">Address: {result.address}</p>
          )}
          {initialPassword && (
            <div className="mt-2 pt-2 border-t border-[var(--color-border)] flex items-center justify-between">
              <div>
                <span className="text-xs text-[var(--color-text-muted)]">Initial Password: </span>
                <span className="font-mono text-xs font-semibold text-[var(--color-text-primary)]">
                  {showPassword ? initialPassword : '•'.repeat(Math.max(initialPassword.length, 8))}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setShowPassword(prev => !prev)}
                className="text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)] transition-colors p-1 rounded"
                title={showPassword ? 'Hide password' : 'Show password'}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
              </button>
            </div>
          )}
        </div>

        {/* Staff Specific Allocation Details */}
        {isStaffAllocation && (
          <div className="space-y-4 border-t border-[var(--color-border)] pt-4">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-[var(--color-text-primary)]">Facility Assignment</p>
              <Pill tone={assigned ? 'success' : 'warning'}>
                {assigned ? 'Assigned' : 'Unassigned'}
              </Pill>
            </div>

            {!assigned ? (
              <p className="text-sm text-[var(--color-text-secondary)]">
                {result.role === 'GROOM'
                  ? 'No available three-stall block at this time. Account remains unassigned.'
                  : 'No available regular barn areas at this time.'}
              </p>
            ) : result.role === 'HEAD_TRAINER' ? (
              <div className="flex flex-wrap gap-2">
                {result.assignedAreaCodes?.map((code) => (
                  <Pill key={code} tone="primary">
                    Area {code}
                  </Pill>
                ))}
              </div>
            ) : (
              <dl className="space-y-2.5 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-[var(--color-text-secondary)]">Barn Area</dt>
                  <dd className="font-medium text-[var(--color-text-primary)]">{result.assignedAreaCode}</dd>
                </div>
                {result.assignedAreaType === 'QUARANTINE' ? (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[var(--color-text-secondary)]">Facility Type</dt>
                    <dd className="font-medium text-[var(--color-text-primary)]">Quarantine</dd>
                  </div>
                ) : (
                  <div className="flex justify-between gap-4">
                    <dt className="text-[var(--color-text-secondary)]">Supervising Trainer</dt>
                    <dd className="font-medium text-[var(--color-text-primary)]">{result.trainerName || 'None'}</dd>
                  </div>
                )}
                <div className="flex items-start justify-between gap-4">
                  <dt className="text-[var(--color-text-secondary)]">Assigned Stalls</dt>
                  <dd className="flex flex-wrap justify-end gap-1.5">
                    {result.assignedStallCodes?.map((code) => (
                      <Pill key={code} tone="primary">
                        {code}
                      </Pill>
                    ))}
                  </dd>
                </div>
              </dl>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}
