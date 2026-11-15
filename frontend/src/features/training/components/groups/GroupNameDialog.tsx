'use client';

import { useState } from 'react';
import { Button, FormField, Input, Modal, Notice } from '@/components/ui';

/** One small dialog for naming a new group and for renaming an existing one. */
export function GroupNameDialog({
  open,
  initialName,
  title,
  submitting,
  error,
  onSubmit,
  onClose,
}: {
  open: boolean;
  initialName: string;
  title: string;
  submitting: boolean;
  error: string | null;
  onSubmit: (name: string) => void;
  onClose: () => void;
}) {
  return (
    <Modal open={open} onClose={onClose} title={title} size="sm" dismissible={!submitting}>
      {/* Remounting on open resets the field to the group's current name. */}
      {open && (
        <NameForm initialName={initialName} submitting={submitting} error={error} onSubmit={onSubmit} onClose={onClose} />
      )}
    </Modal>
  );
}

function NameForm({
  initialName,
  submitting,
  error,
  onSubmit,
  onClose,
}: {
  initialName: string;
  submitting: boolean;
  error: string | null;
  onSubmit: (name: string) => void;
  onClose: () => void;
}) {
  const [name, setName] = useState(initialName);

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (name.trim()) onSubmit(name.trim());
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <FormField label="Group name" required>
        <Input value={name} maxLength={100} autoFocus onChange={(event) => setName(event.target.value)} />
      </FormField>
      {error && <Notice tone="error">{error}</Notice>}
      <div className="flex justify-end gap-2">
        <Button type="button" variant="tertiary" onClick={onClose} disabled={submitting}>Cancel</Button>
        <Button type="submit" variant="primary" disabled={submitting || !name.trim()}>
          {submitting ? 'Saving...' : 'Save'}
        </Button>
      </div>
    </form>
  );
}
