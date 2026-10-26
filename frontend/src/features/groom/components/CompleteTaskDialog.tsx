'use client';

import { useEffect, useId, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import {
  Button,
  Checkbox,
  FileInput,
  FormField,
  IconButton,
  ImageLightbox,
  ImageThumbnail,
  Modal,
  Notice,
  Textarea,
} from '@/components/ui';
import { displayError } from '@/lib/display';
import type { TaskBlock } from '../lib/groupTasks';
import { groomApi } from '../services/groomService';

const MAX_NOTE_LENGTH = 1000;
const MAX_PHOTOS = 5;
const MAX_PHOTO_BYTES = 10 * 1024 * 1024;

interface CompleteTaskDialogProps {
  /** The block being completed; null keeps the dialog closed. */
  block: TaskBlock | null;
  onClose: () => void;
  onCompleted: () => Promise<void> | void;
}

interface SelectedPhoto {
  file: File;
  url: string;
}

/**
 * Completes the pending horses of a block in one action. Every horse starts ticked; the groom unticks any
 * horse that was not done, which then stays pending. The note and the photos are optional and shared by the
 * ticked horses: most routine duties need neither.
 */
export function CompleteTaskDialog({ block, onClose, onCompleted }: CompleteTaskDialogProps) {
  const formId = useId();
  const [note, setNote] = useState('');
  const [photos, setPhotos] = useState<SelectedPhoto[]>([]);
  const [viewing, setViewing] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Horses the groom unticked; empty means "all ticked", the default for every block.
  const [skipped, setSkipped] = useState<ReadonlySet<number>>(new Set());

  // Preview URLs are released when a photo is removed, when the dialog resets, and when it unmounts.
  const photosRef = useRef(photos);
  useEffect(() => {
    photosRef.current = photos;
  }, [photos]);
  useEffect(() => () => photosRef.current.forEach((photo) => URL.revokeObjectURL(photo.url)), []);

  function reset() {
    photos.forEach((photo) => URL.revokeObjectURL(photo.url));
    setNote('');
    setPhotos([]);
    setViewing(null);
    setError(null);
    setSkipped(new Set());
  }

  const pending = block?.pending ?? [];
  const chosen = pending.filter((task) => !skipped.has(task.refId));
  const allTicked = chosen.length === pending.length;

  function toggleHorse(refId: number, ticked: boolean) {
    setError(null);
    setSkipped((current) => {
      const next = new Set(current);
      if (ticked) next.delete(refId); else next.add(refId);
      return next;
    });
  }

  function handleClose() {
    if (submitting) return;
    reset();
    onClose();
  }

  function handlePhotosChange(event: ChangeEvent<HTMLInputElement>) {
    const chosen = Array.from(event.target.files ?? []);
    event.target.value = '';   // picking the same file again after removing it must fire a change
    setError(null);

    const rejected = chosen.filter((file) => !file.type.startsWith('image/') || file.size > MAX_PHOTO_BYTES);
    const accepted = chosen.filter((file) => !rejected.includes(file));
    const room = MAX_PHOTOS - photos.length;
    if (rejected.length > 0) {
      setError('Photos must be JPEG, PNG or WebP images of at most 10 MB each.');
    } else if (accepted.length > room) {
      setError(`You can attach at most ${MAX_PHOTOS} photos.`);
    }
    const added = accepted.slice(0, Math.max(room, 0)).map((file) => ({ file, url: URL.createObjectURL(file) }));
    if (added.length > 0) setPhotos((current) => [...current, ...added]);
  }

  function removePhoto(index: number) {
    URL.revokeObjectURL(photos[index].url);
    setPhotos((current) => current.filter((_, i) => i !== index));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!block) return;
    if (chosen.length === 0) {
      setError('Tick at least one horse that was done.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await groomApi.completeTasks(chosen.map((task) => task.refId), {
        note,
        photos: photos.map((photo) => photo.file),
      });
      reset();
      onClose();
      await onCompleted();
    } catch (err) {
      setError(displayError(err, 'Unable to complete the tasks. Please try again.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <Modal
        open={block !== null}
        onClose={handleClose}
        dismissible={!submitting}
        title={block && block.pending.length > 1 ? `Complete ${block.pending.length} tasks` : 'Complete task'}
        description={block ? `${block.startTime.substring(0, 5)} · ${block.title}` : undefined}
        footer={
          <>
            <Button variant="secondary" onClick={handleClose} disabled={submitting}>Cancel</Button>
            <Button type="submit" form={formId} variant="primary" icon="check" loading={submitting}
              disabled={chosen.length === 0}>
              {pending.length > 1 ? `Mark ${chosen.length} as completed` : 'Mark as completed'}
            </Button>
          </>
        }
      >
        <form id={formId} onSubmit={handleSubmit} className="space-y-4">
          {error && <Notice tone="error">{error}</Notice>}

          {pending.length > 1 && (
            <fieldset className="space-y-2" aria-label="Horses done">
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-medium text-[var(--color-text-primary)]">
                  Horses done · {chosen.length}/{pending.length}
                </span>
                <Button type="button" variant="link" size="sm" disabled={submitting}
                  onClick={() => { setError(null); setSkipped(allTicked ? new Set(pending.map((task) => task.refId)) : new Set()); }}>
                  {allTicked ? 'Untick all' : 'Tick all'}
                </Button>
              </div>
              <ul className="divide-y divide-[var(--color-border)] rounded-[var(--radius-md)] border border-[var(--color-border)]">
                {pending.map((task) => (
                  <li key={task.refId} className="px-3 py-2">
                    <Checkbox
                      label={task.horseName}
                      checked={!skipped.has(task.refId)}
                      onChange={(event) => toggleHorse(task.refId, event.target.checked)}
                      disabled={submitting}
                    />
                  </li>
                ))}
              </ul>
              <p className="text-xs text-[var(--color-text-muted)]">Unticked horses stay pending, so you can complete them later.</p>
            </fieldset>
          )}

          <FormField
            label="Note"
            hint={`Optional, shared by the ticked horses. ${note.length}/${MAX_NOTE_LENGTH}`}
          >
            <Textarea
              rows={3}
              maxLength={MAX_NOTE_LENGTH}
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="All ate the full ration, no leftovers…"
              disabled={submitting}
            />
          </FormField>

          <FormField label="Photos" hint={`Optional. Up to ${MAX_PHOTOS} JPEG, PNG or WebP images, 10 MB each. ${photos.length}/${MAX_PHOTOS}`}>
            <FileInput
              accept="image/jpeg,image/png,image/webp"
              multiple
              onChange={handlePhotosChange}
              disabled={submitting || photos.length >= MAX_PHOTOS}
            />
          </FormField>

          {photos.length > 0 && (
            <ul className="grid grid-cols-3 gap-2 sm:grid-cols-5" aria-label="Selected photos">
              {photos.map((photo, index) => (
                <li key={photo.url} className="relative">
                  <ImageThumbnail
                    src={photo.url}
                    alt={`selected photo ${index + 1}`}
                    onClick={() => setViewing(index)}
                  />
                  <IconButton
                    icon="x"
                    size="sm"
                    variant="secondary"
                    label={`Remove photo ${index + 1}`}
                    onClick={() => removePhoto(index)}
                    disabled={submitting}
                    className="absolute right-1 top-1"
                  />
                </li>
              ))}
            </ul>
          )}
        </form>
      </Modal>

      <ImageLightbox
        images={photos.map((photo) => ({ src: photo.url, alt: photo.file.name }))}
        index={viewing}
        onIndexChange={setViewing}
        onClose={() => setViewing(null)}
      />
    </>
  );
}
