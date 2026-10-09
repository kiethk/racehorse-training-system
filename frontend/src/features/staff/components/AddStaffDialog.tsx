import { useState, useEffect, type FormEvent } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import type { StaffCreationRequest, StaffCreationResponse, StaffSummary } from '../types';

interface AddStaffDialogProps {
  open: boolean;
  onClose: () => void;
  /** Called with the creation request; resolves with the backend response. */
  onSubmit: (request: StaffCreationRequest) => Promise<StaffCreationResponse | null>;
  loading: boolean;
  headTrainers: StaffSummary[];
}

const inputClassName =
  'mt-1 h-9 w-full rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 text-[13px] text-[var(--color-text-primary)] outline-none transition-colors placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-focus)] focus:ring-1 focus:ring-[var(--color-focus)]';

export function AddStaffDialog({ open, onClose, onSubmit, loading, headTrainers }: AddStaffDialogProps) {
  const [role, setRole] = useState<'GROOM' | 'VETERINARIAN' | 'HEAD_TRAINER'>('GROOM');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');

  // Vet specific
  const [licenseNumber, setLicenseNumber] = useState('');
  const [licenseIssuedDate, setLicenseIssuedDate] = useState('');
  const [specialization, setSpecialization] = useState('');

  // Trainer specific
  const [certificationNumber, setCertificationNumber] = useState('');
  const [certificationIssuedDate, setCertificationIssuedDate] = useState('');

  // Groom specific
  // Removed manual trainerId

  const [error, setError] = useState('');
  const [creationResult, setCreationResult] = useState<StaffCreationResponse | null>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && !loading && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose, loading]);

  // Reset form when dialog opens
  useEffect(() => {
    if (open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setRole('GROOM');
      setFullName('');
      setEmail('');
      setPassword('');
      setPhone('');
      setAddress('');
      setLicenseNumber('');
      setLicenseIssuedDate('');
      setSpecialization('');
      setCertificationNumber('');
      setCertificationIssuedDate('');
      setError('');
      setCreationResult(null);
    }
  }, [open]);

  if (!open) return null;

  // If we have a result to show, render the assignment summary screen
  if (creationResult) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" aria-hidden="true" />
        <div
          role="dialog"
          aria-modal="true"
          className="relative flex w-full max-w-lg flex-col rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-2xl"
        >
          <div className="flex items-center justify-between border-b border-[var(--color-border)] px-6 py-4">
            <h2 className="text-[16px] font-semibold text-[var(--color-text-primary)]">Staff Created</h2>
            <button
              type="button"
              onClick={onClose}
              className="rounded p-1 text-[var(--color-text-muted)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]"
            >
              <Icon name="x" size={16} />
            </button>
          </div>

          <div className="p-6 space-y-4">
            {/* Success banner */}
            <div className="flex items-start gap-2 rounded-[var(--radius-sm)] bg-[var(--color-success-soft,#f0fdf4)] px-3 py-2.5 text-[12px] text-[var(--color-success,#16a34a)]">
              <Icon name="check" size={14} className="mt-0.5 shrink-0" />
              <span>
                <strong>{creationResult.fullName}</strong> ({creationResult.role.replace('_', ' ')}) created successfully.
              </span>
            </div>

            {/* HEAD_TRAINER: show assigned areas */}
            {creationResult.role === 'HEAD_TRAINER' && (
              <div className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4 text-[13px]">
                <p className="font-medium text-[var(--color-text-primary)] mb-2">Auto-assigned Areas</p>
                {creationResult.assignedAreaCodes && creationResult.assignedAreaCodes.length > 0 ? (
                  <div className="flex gap-2 flex-wrap">
                    {creationResult.assignedAreaCodes.map((code) => (
                      <span key={code} className="rounded px-2 py-0.5 bg-[var(--color-primary-soft)] text-[var(--color-primary)] font-metric font-semibold text-[12px]">
                        Area {code}
                      </span>
                    ))}
                  </div>
                ) : (
                  <p className="text-[var(--color-text-muted)]">No areas were available for assignment at this time. You can assign areas later.</p>
                )}
              </div>
            )}

            {/* GROOM: show trainer + stall block */}
            {creationResult.role === 'GROOM' && (
              <div className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4 text-[13px] space-y-3">
                {creationResult.assignmentStatus === 'UNASSIGNED' ? (
                  <div className="flex items-start gap-2 rounded-[var(--radius-sm)] bg-amber-50 border border-amber-200 px-3 py-2 text-[12px] text-amber-700">
                    <Icon name="alert-triangle" size={13} className="mt-0.5 shrink-0" />
                    <span>Groom created successfully, but no Trainer with an available stall block is currently available.</span>
                  </div>
                ) : (
                  <>
                    <div>
                      <p className="font-medium text-[var(--color-text-primary)] mb-1">Trainer</p>
                      <p className="text-[var(--color-text-secondary)]">{creationResult.trainerName}</p>
                    </div>
                    <div>
                      <p className="font-medium text-[var(--color-text-primary)] mb-1">Assigned Stall Block</p>
                      <div className="space-y-1">
                        <p className="text-[var(--color-text-muted)] text-[12px]">Area <strong>{creationResult.assignedAreaCode}</strong></p>
                        <div className="flex gap-2 flex-wrap">
                          {(creationResult.assignedStallCodes ?? []).map((code) => (
                            <span key={code} className="rounded px-2 py-0.5 bg-[var(--color-primary-soft)] text-[var(--color-primary)] font-metric font-semibold text-[12px]">
                              {code}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>
                  </>
                )}
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3 border-t border-[var(--color-border)] px-6 py-4">
            <Button type="button" variant="primary" onClick={onClose}>
              Done
            </Button>
          </div>
        </div>
      </div>
    );
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (loading) return;
    setError('');

    if (!fullName.trim() || !email.trim() || !password) {
      setError('Full name, email, and password are required.');
      return;
    }

    if (role === 'VETERINARIAN' && !licenseNumber.trim()) {
      setError('License number is required for Veterinarians.');
      return;
    }

    if (role === 'HEAD_TRAINER' && !certificationNumber.trim()) {
      setError('Certification number is required for Head Trainers.');
      return;
    }

    const request: StaffCreationRequest = {
      fullName: fullName.trim(),
      email: email.trim(),
      password,
      role,
      phone: phone.trim() || undefined,
      address: address.trim() || undefined,
    };

    if (role === 'VETERINARIAN') {
      request.licenseNumber = licenseNumber.trim();
      request.licenseIssuedDate = licenseIssuedDate || undefined;
      request.specialization = specialization.trim() || undefined;
    } else if (role === 'HEAD_TRAINER') {
      request.certificationNumber = certificationNumber.trim();
      request.certificationIssuedDate = certificationIssuedDate || undefined;
    }

    try {
      const result = await onSubmit(request);
      if (result) setCreationResult(result);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to create staff');
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={() => !loading && onClose()} aria-hidden="true" />
      <div
        role="dialog"
        aria-modal="true"
        className="relative flex w-full max-w-2xl max-h-[90vh] flex-col rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-2xl"
      >
        <div className="flex items-center justify-between border-b border-[var(--color-border)] px-6 py-4">
          <h2 className="text-[16px] font-semibold text-[var(--color-text-primary)]">Add Staff Member</h2>
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="rounded p-1 text-[var(--color-text-muted)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] disabled:opacity-50"
          >
            <Icon name="x" size={16} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6" noValidate>
          {error && (
            <div className="mb-6 flex items-start gap-2 rounded-[var(--radius-sm)] bg-[var(--color-danger-soft)] px-3 py-2.5 text-[12px] text-[var(--color-danger)]">
              <Icon name="alert-triangle" size={14} className="mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="grid gap-6 md:grid-cols-2">
            <div className="space-y-4">
              <h3 className="text-[13px] font-semibold text-[var(--color-text-primary)]">Basic Info</h3>
              <label className="block">
                <span className="text-[12px] font-medium text-[var(--color-text-primary)]">Role <span className="text-[var(--color-danger)]">*</span></span>
                <select
                  value={role}
                  // eslint-disable-next-line @typescript-eslint/no-explicit-any
                  onChange={(e) => setRole(e.target.value as any)}
                  className={inputClassName}
                  disabled={loading}
                >
                  <option value="GROOM">Groom</option>
                  <option value="VETERINARIAN">Veterinarian</option>
                  <option value="HEAD_TRAINER">Head Trainer</option>
                </select>
              </label>

              <label className="block">
                <span className="text-[12px] font-medium text-[var(--color-text-primary)]">Full Name <span className="text-[var(--color-danger)]">*</span></span>
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className={inputClassName}
                  placeholder="e.g. John Doe"
                  required
                  disabled={loading}
                />
              </label>

              <label className="block">
                <span className="text-[12px] font-medium text-[var(--color-text-primary)]">Email <span className="text-[var(--color-danger)]">*</span></span>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className={inputClassName}
                  placeholder="name@rtms.com"
                  required
                  disabled={loading}
                />
              </label>

              <label className="block">
                <span className="text-[12px] font-medium text-[var(--color-text-primary)]">Password <span className="text-[var(--color-danger)]">*</span></span>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className={inputClassName}
                  required
                  disabled={loading}
                />
              </label>

              <label className="block">
                <span className="text-[12px] font-medium text-[var(--color-text-primary)]">Phone</span>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className={inputClassName}
                  disabled={loading}
                />
              </label>

              <label className="block">
                <span className="text-[12px] font-medium text-[var(--color-text-primary)]">Address</span>
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className={inputClassName}
                  disabled={loading}
                />
              </label>
            </div>

            <div className="space-y-4 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4">
              <h3 className="text-[13px] font-semibold text-[var(--color-text-primary)]">Role Profile</h3>
              
              {role === 'VETERINARIAN' && (
                <>
                  <label className="block">
                    <span className="text-[12px] font-medium text-[var(--color-text-primary)]">License Number <span className="text-[var(--color-danger)]">*</span></span>
                    <input
                      type="text"
                      value={licenseNumber}
                      onChange={(e) => setLicenseNumber(e.target.value)}
                      className={inputClassName}
                      required
                      disabled={loading}
                    />
                  </label>
                  <label className="block">
                    <span className="text-[12px] font-medium text-[var(--color-text-primary)]">License Issued Date</span>
                    <input
                      type="date"
                      value={licenseIssuedDate}
                      onChange={(e) => setLicenseIssuedDate(e.target.value)}
                      className={inputClassName}
                      disabled={loading}
                    />
                  </label>
                  <label className="block">
                    <span className="text-[12px] font-medium text-[var(--color-text-primary)]">Specialization</span>
                    <input
                      type="text"
                      value={specialization}
                      onChange={(e) => setSpecialization(e.target.value)}
                      className={inputClassName}
                      disabled={loading}
                    />
                  </label>
                </>
              )}

              {role === 'HEAD_TRAINER' && (
                <>
                  <label className="block">
                    <span className="text-[12px] font-medium text-[var(--color-text-primary)]">Certification Number <span className="text-[var(--color-danger)]">*</span></span>
                    <input
                      type="text"
                      value={certificationNumber}
                      onChange={(e) => setCertificationNumber(e.target.value)}
                      className={inputClassName}
                      required
                      disabled={loading}
                    />
                  </label>
                  <label className="block">
                    <span className="text-[12px] font-medium text-[var(--color-text-primary)]">Certification Issued Date</span>
                    <input
                      type="date"
                      value={certificationIssuedDate}
                      onChange={(e) => setCertificationIssuedDate(e.target.value)}
                      className={inputClassName}
                      disabled={loading}
                    />
                  </label>
                  <div className="rounded-[var(--radius-sm)] bg-blue-50 border border-blue-200 px-3 py-2 text-[12px] text-blue-700">
                    <p className="font-medium mb-0.5">Auto Area Assignment</p>
                    <p>The system will automatically assign up to 2 available REGULAR areas to this trainer upon creation.</p>
                  </div>
                </>
              )}

              {role === 'GROOM' && (
                <>
                  <div className="rounded-[var(--radius-sm)] bg-blue-50 border border-blue-200 px-3 py-2 text-[12px] text-blue-700">
                    <p className="font-medium mb-0.5">Auto Assignment</p>
                    <p>Trainer and stall block will be assigned automatically based on current workload and available capacity.</p>
                  </div>
                </>
              )}
            </div>
          </div>
          
          <div className="mt-8 flex justify-end gap-3 border-t border-[var(--color-border)] pt-4">
            <Button type="button" variant="secondary" onClick={onClose} disabled={loading}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={loading}>
              Create Account
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
