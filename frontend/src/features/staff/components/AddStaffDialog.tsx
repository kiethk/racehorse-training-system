import { useState, useEffect, type FormEvent } from 'react';
import { Button, FormField, Input, Modal, Notice, Pill, Select } from '@/components/ui';
import { formatEnumLabel } from '@/lib/display';
import type { StaffCreationRequest, StaffCreationResponse, StaffSummary } from '../types';

const FORM_ID = 'add-staff-form';

interface AddStaffDialogProps {
  open: boolean;
  onClose: () => void;
  /** Called with the creation request; resolves with the backend response. */
  onSubmit: (request: StaffCreationRequest) => Promise<StaffCreationResponse | null>;
  loading: boolean;
  headTrainers: StaffSummary[];
}

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
      <Modal
        open
        onClose={onClose}
        title="Staff created"
        footer={<Button variant="primary" onClick={onClose}>Done</Button>}
      >
        <div className="space-y-4">
          <Notice tone="success">
            <strong>{creationResult.fullName}</strong> ({formatEnumLabel(creationResult.role)}) created successfully.
          </Notice>

          {/* HEAD_TRAINER: show assigned areas */}
          {creationResult.role === 'HEAD_TRAINER' && (
            <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4 text-sm">
              <p className="mb-2 font-medium text-[var(--color-text-primary)]">Auto-assigned areas</p>
              {creationResult.assignedAreaCodes && creationResult.assignedAreaCodes.length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {creationResult.assignedAreaCodes.map((code) => (
                    <Pill key={code} tone="primary">Area {code}</Pill>
                  ))}
                </div>
              ) : (
                <p className="text-[var(--color-text-muted)]">No areas were available for assignment at this time. You can assign areas later.</p>
              )}
            </div>
          )}

          {/* GROOM: show trainer + stall block */}
          {creationResult.role === 'GROOM' && (
            creationResult.assignmentStatus === 'UNASSIGNED' ? (
              <Notice tone="warning">
                Groom created successfully, but no Trainer with an available stall block is currently available.
              </Notice>
            ) : (
              <div className="space-y-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4 text-sm">
                <div>
                  <p className="mb-1 font-medium text-[var(--color-text-primary)]">Trainer</p>
                  <p className="text-[var(--color-text-secondary)]">{creationResult.trainerName}</p>
                </div>
                <div>
                  <p className="mb-1 font-medium text-[var(--color-text-primary)]">Assigned stall block</p>
                  <p className="mb-1.5 text-xs text-[var(--color-text-muted)]">Area <strong>{creationResult.assignedAreaCode}</strong></p>
                  <div className="flex flex-wrap gap-2">
                    {(creationResult.assignedStallCodes ?? []).map((code) => (
                      <Pill key={code} tone="primary">{code}</Pill>
                    ))}
                  </div>
                </div>
              </div>
            )
          )}
        </div>
      </Modal>
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
    <Modal
      open
      onClose={onClose}
      size="lg"
      dismissible={!loading}
      title="Add staff member"
      footer={(
        <>
          <Button variant="secondary" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button type="submit" form={FORM_ID} variant="primary" loading={loading}>
            Create account
          </Button>
        </>
      )}
    >
        <form id={FORM_ID} onSubmit={handleSubmit} noValidate>
          {error && <Notice tone="error" className="mb-5">{error}</Notice>}

          <div className="grid gap-6 md:grid-cols-2">
            <div className="space-y-4">
              <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Basic Info</h3>
              <FormField label="Role" required>
                <Select
                  value={role}
                  // eslint-disable-next-line @typescript-eslint/no-explicit-any
                  onChange={(e) => setRole(e.target.value as any)}
                  disabled={loading}
                >
                  <option value="GROOM">Groom</option>
                  <option value="VETERINARIAN">Veterinarian</option>
                  <option value="HEAD_TRAINER">Head Trainer</option>
                </Select>
              </FormField>

              <FormField label="Full Name" required>
                <Input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="e.g. John Doe"
                  required
                  disabled={loading}
                />
              </FormField>

              <FormField label="Email" required>
                <Input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@rtms.com"
                  required
                  disabled={loading}
                />
              </FormField>

              <FormField label="Password" required>
                <Input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  disabled={loading}
                />
              </FormField>

              <FormField label="Phone">
                <Input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  disabled={loading}
                />
              </FormField>

              <FormField label="Address">
                <Input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  disabled={loading}
                />
              </FormField>
            </div>

            <div className="space-y-4 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-4">
              <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Role Profile</h3>
              
              {role === 'VETERINARIAN' && (
                <>
                  <FormField label="License Number" required>
                    <Input
                      type="text"
                      value={licenseNumber}
                      onChange={(e) => setLicenseNumber(e.target.value)}
                      required
                      disabled={loading}
                    />
                  </FormField>
                  <FormField label="License Issued Date">
                    <Input
                      type="date"
                      value={licenseIssuedDate}
                      onChange={(e) => setLicenseIssuedDate(e.target.value)}
                      disabled={loading}
                    />
                  </FormField>
                  <FormField label="Specialization">
                    <Input
                      type="text"
                      value={specialization}
                      onChange={(e) => setSpecialization(e.target.value)}
                      disabled={loading}
                    />
                  </FormField>
                </>
              )}

              {role === 'HEAD_TRAINER' && (
                <>
                  <FormField label="Certification Number" required>
                    <Input
                      type="text"
                      value={certificationNumber}
                      onChange={(e) => setCertificationNumber(e.target.value)}
                      required
                      disabled={loading}
                    />
                  </FormField>
                  <FormField label="Certification Issued Date">
                    <Input
                      type="date"
                      value={certificationIssuedDate}
                      onChange={(e) => setCertificationIssuedDate(e.target.value)}
                      disabled={loading}
                    />
                  </FormField>
                  <Notice tone="info" title="Auto area assignment">
                    The system will automatically assign up to 2 available REGULAR areas to this trainer upon creation.
                  </Notice>
                </>
              )}

              {role === 'GROOM' && (
                <Notice tone="info" title="Auto assignment">
                  Trainer and stall block will be assigned automatically based on current workload and available capacity.
                </Notice>
              )}
            </div>
          </div>
        </form>
    </Modal>
  );
}
