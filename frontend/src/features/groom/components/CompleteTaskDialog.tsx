'use client';

import { useEffect, useId, useState, type ChangeEvent, type FormEvent } from 'react';
import { Button, FileInput, FormField, Modal, Notice, Textarea } from '@/components/ui';
import { displayError } from '@/lib/display';
import { groomApi } from '../services/groomService';
import type { TodayTaskItem } from '../types';

const MAX_NOTE_LENGTH = 1000;
const MAX_PHOTO_BYTES = 10 * 1024 * 1024;

interface CompleteTaskDialogProps {
  /** The task being completed; null keeps the dialog closed. */
  task: TodayTaskItem | null;
  onClose: () => void;
  onCompleted: () => Promise<void> | void;
}

/** Confirms a daily task. The note and the photo are both optional: most routine tasks need neither. */
export function CompleteTaskDialog({ task, onClose, onCompleted }: CompleteTaskDialogProps) {
  const formId = useId();
  const [note, setNote] = useState('');
  const [photo, setPhoto] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!photo) return;
    const url = URL.createObjectURL(photo);
    // eslint-disable-next-line react-hooks/set-state-in-effect -- the preview URL exists only while this photo is selected
    setPreviewUrl(url);
    return () => {
      URL.revokeObjectURL(url);
      setPreviewUrl(null);
    };
  }, [photo]);

  function reset() {
    setNote('');
    setPhoto(null);
    setError(null);
  }

  function handleClose() {
    if (submitting) return;
    reset();
    onClose();
  }

  function handlePhotoChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    setError(null);
    if (file && !file.type.startsWith('image/')) {
      setError('The photo must be a JPEG, PNG or WebP image.');
      event.target.value = '';
      setPhoto(null);
      return;
    }
    if (file && file.size > MAX_PHOTO_BYTES) {
      setError('The photo is too large. The limit is 10 MB.');
      event.target.value = '';
      setPhoto(null);
      return;
    }
    setPhoto(file);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!task) return;
    setSubmitting(true);
    setError(null);
    try {
      await groomApi.completeTask(task.refId, { note, photo });
      reset();
      onClose();
      await onCompleted();
    } catch (err) {
      setError(displayError(err, 'Unable to complete the task. Please try again.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      open={task !== null}
      onClose={handleClose}
      dismissible={!submitting}
      title="Complete task"
      description={task ? `${task.startTime.substring(0, 5)} · ${task.title} · ${task.horseName}` : undefined}
      footer={
        <>
          <Button variant="secondary" onClick={handleClose} disabled={submitting}>Cancel</Button>
          <Button type="submit" form={formId} variant="primary" icon="check" loading={submitting}>
            Mark as completed
          </Button>
        </>
      }
    >
      <form id={formId} onSubmit={handleSubmit} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}

        <FormField
          label="Note"
          hint={`Optional. Anything worth passing on, for example appetite or behaviour. ${note.length}/${MAX_NOTE_LENGTH}`}
        >
          <Textarea
            rows={3}
            maxLength={MAX_NOTE_LENGTH}
            value={note}
            onChange={(event) => setNote(event.target.value)}
            placeholder="Ate the full ration, no leftovers…"
            disabled={submitting}
          />
        </FormField>

        <FormField label="Photo" hint="Optional. JPEG, PNG or WebP, up to 10 MB.">
          <FileInput accept="image/jpeg,image/png,image/webp" onChange={handlePhotoChange} disabled={submitting} />
        </FormField>

        {previewUrl && (
          // eslint-disable-next-line @next/next/no-img-element -- local preview of a file that is not uploaded yet
          <img
            src={previewUrl}
            alt="Selected photo preview"
            className="max-h-48 rounded-[var(--radius-md)] border border-[var(--color-border)] object-contain"
          />
        )}
      </form>
    </Modal>
  );
}
