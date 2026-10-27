'use client';

import { useState } from 'react';
import { Button, FormField, Input, Modal, Notice, Select, Textarea } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { incidentApi } from '@/features/groom/services/incidentService';
import type { IncidentSeverity } from '@/features/groom/types';

interface ReportIncidentDialogProps {
  horseId: number;
  horseName?: string;
  initialTitle?: string;
  initialDescription?: string;
  initialSeverity?: IncidentSeverity;
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

const SEVERITY_OPTIONS: { value: IncidentSeverity; label: string }[] = [
  { value: 'LOW', label: 'Low — Monitor, no urgent intervention required' },
  { value: 'MEDIUM', label: 'Medium — Veterinary review needed today' },
  { value: 'HIGH', label: 'High — Immediate veterinary review required' },
  { value: 'CRITICAL', label: 'Critical — Health emergency, immediate intervention' },
];

export function ReportIncidentDialog({
  horseId,
  horseName,
  initialTitle = '',
  initialDescription = '',
  initialSeverity = 'HIGH',
  open,
  onClose,
  onSuccess,
}: ReportIncidentDialogProps) {
  const [title, setTitle] = useState(initialTitle);
  const [description, setDescription] = useState(initialDescription);
  const [severity, setSeverity] = useState<IncidentSeverity>(initialSeverity);
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) return null;

  const handleClose = () => {
    setTitle(initialTitle);
    setDescription(initialDescription);
    setSeverity(initialSeverity);
    setFile(null);
    setError(null);
    onClose();
  };

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) {
      setError('Title is required.');
      return;
    }
    if (!description.trim()) {
      setError('Description is required.');
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      const created = await incidentApi.create({
        horseId,
        title: title.trim(),
        description: description.trim(),
        severity,
      });

      if (file) {
        try {
          await incidentApi.uploadImage(created.id, file);
        } catch (imgErr) {
          console.error('Failed to attach image to incident:', imgErr);
          toast.warning('Incident reported, but the attached image could not be uploaded.');
        }
      }

      toast.success('Incident reported to veterinary team.');
      handleClose();
      onSuccess?.();
    } catch (err) {
      setError(displayError(err, 'Unable to report incident.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      open
      onClose={handleClose}
      dismissible={!submitting}
      title="Report health incident to veterinarian"
      description={`Submit an urgent incident report for ${horseName ?? `Horse #${horseId}`}. This will notify the veterinary team.`}
      footer={(
        <>
          <Button variant="secondary" onClick={handleClose} disabled={submitting}>
            Cancel
          </Button>
          <Button variant="destructive" type="submit" form="report-incident-form" loading={submitting}>
            Send incident report
          </Button>
        </>
      )}
    >
      <form id="report-incident-form" onSubmit={handleSubmit} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}

        <FormField label="Incident title" required>
          <Input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Heart rate exceeded safety threshold during workout"
            required
          />
        </FormField>

        <FormField label="Severity level" required>
          <Select
            value={severity}
            onChange={(e) => setSeverity(e.target.value as IncidentSeverity)}
          >
            {SEVERITY_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </Select>
        </FormField>

        <FormField label="Incident description / Observations" required>
          <Textarea
            rows={4}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Describe observations, symptoms, heart rate spikes, lameness or unusual behavior during training…"
            required
          />
        </FormField>

        <FormField label="Attach photo / evidence (optional)">
          <Input
            type="file"
            accept="image/png,image/jpeg,image/webp"
            onChange={(e) => {
              const selected = e.target.files?.[0] ?? null;
              if (selected && selected.size > 10 * 1024 * 1024) {
                setError('Image file must not exceed 10 MB.');
                return;
              }
              setFile(selected);
            }}
          />
        </FormField>
      </form>
    </Modal>
  );
}
