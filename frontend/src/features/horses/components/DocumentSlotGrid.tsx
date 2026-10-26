'use client';

import { useRef, useState } from 'react';
import { Button, FileInput, Icon, Pill } from '@/components/ui';
import { cn } from '@/lib/cn';
import { formatDate } from '@/lib/display';
import { AdmissionDocumentPreview } from '@/features/admissions/shared/components/AdmissionDocumentPreview';
import { buildSlotStates, documentTypeLabel, validateDocumentFile, type SlotState } from '../lib/horseEdit';
import type { HorseDocumentSlot, HorseDocumentType } from '../types';

interface DocumentSlotGridProps {
  slots: HorseDocumentSlot[];
  /** Files picked in this form, not uploaded yet. Omit for a read-only grid. */
  chosen?: Partial<Record<HorseDocumentType, File>>;
  onChoose?: (type: HorseDocumentType, file: File | null) => void;
}

/**
 * One tile per document type. Existing files can be viewed and replaced; missing ones show a "+"
 * upload target. A new file stays local until the change request is submitted, and only becomes
 * the horse's document once the groom approves it.
 */
export function DocumentSlotGrid({ slots, chosen, onChoose }: DocumentSlotGridProps) {
  const editable = Boolean(onChoose);
  const [errors, setErrors] = useState<Partial<Record<HorseDocumentType, string>>>({});
  const [preview, setPreview] = useState<HorseDocumentSlot | null>(null);
  const states = buildSlotStates(slots, chosen ?? {});

  const pick = (type: HorseDocumentType, file: File | undefined) => {
    if (!file) return;
    const error = validateDocumentFile(type, file);
    setErrors((prev) => ({ ...prev, [type]: error ?? undefined }));
    if (!error) onChoose?.(type, file);
  };

  return (
    <>
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {states.map((state) => (
          <SlotTile
            key={stateType(state)}
            state={state}
            editable={editable}
            error={errors[stateType(state)]}
            onPick={(file) => pick(stateType(state), file)}
            onUndo={() => onChoose?.(stateType(state), null)}
            onView={setPreview}
          />
        ))}
      </ul>
      {preview?.fileUrl && (
        <AdmissionDocumentPreview
          document={{
            id: preview.documentId ?? 0,
            documentType: preview.documentType,
            fileUrl: preview.fileUrl,
            originalFileName: preview.originalFileName,
            recordDate: null,
            note: null,
            uploadedAt: preview.updatedAt ?? '',
          }}
          label={documentTypeLabel(preview.documentType)}
          fileUrl={preview.fileUrl}
          onClose={() => setPreview(null)}
        />
      )}
    </>
  );
}

function stateType(state: SlotState): HorseDocumentType {
  return state.kind === 'current' ? state.slot.documentType : state.type;
}

function SlotTile({ state, editable, error, onPick, onUndo, onView }: {
  state: SlotState;
  editable: boolean;
  error?: string;
  onPick: (file: File | undefined) => void;
  onUndo: () => void;
  onView: (slot: HorseDocumentSlot) => void;
}) {
  const input = useRef<HTMLInputElement>(null);
  const type = stateType(state);
  const label = documentTypeLabel(type);
  const errorId = `slot-error-${type}`;
  const openPicker = () => input.current?.click();

  return (
    <li
      className={cn(
        'flex min-h-[88px] flex-col justify-between gap-2 rounded-[var(--radius-md)] border p-3',
        state.kind === 'empty' ? 'border-dashed border-[var(--color-border)]' : 'border-[var(--color-border-subtle)]',
        error && 'border-[var(--color-danger)]',
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-sm font-medium text-[var(--color-text-primary)]">{label}</p>
          {state.kind === 'current' && (
            <p className="truncate text-xs text-[var(--color-text-muted)]">
              {state.slot.originalFileName || 'Uploaded file'}
              {' · '}
              {state.slot.source === 'EDIT_REQUEST'
                ? `Updated ${state.slot.updatedAt ? formatDate(state.slot.updatedAt) : ''}`
                : 'From admission'}
            </p>
          )}
          {state.kind === 'pending' && (
            <p className="truncate text-xs text-[var(--color-text-muted)]">{state.fileName}</p>
          )}
          {state.kind === 'empty' && <p className="text-xs text-[var(--color-text-muted)]">No file yet</p>}
        </div>
        {state.kind === 'pending' && (
          <Pill tone="info" size="sm">{state.replaces ? 'Replacement' : 'New'}</Pill>
        )}
      </div>

      <div className="flex flex-wrap gap-2">
        {state.kind === 'current' && (
          <Button type="button" size="sm" variant="tertiary" icon="external-link" onClick={() => onView(state.slot)}>View</Button>
        )}
        {editable && state.kind === 'current' && (
          <Button type="button" size="sm" variant="secondary" icon="upload" onClick={openPicker}>Replace</Button>
        )}
        {editable && state.kind === 'empty' && (
          <Button type="button" size="sm" variant="secondary" icon="plus" onClick={openPicker} aria-label={`Upload ${label}`}>
            Upload
          </Button>
        )}
        {editable && state.kind === 'pending' && (
          <Button type="button" size="sm" variant="tertiary" icon="x" onClick={onUndo}>Undo</Button>
        )}
      </div>

      {error && (
        <p id={errorId} role="alert" className="flex items-center gap-1 text-xs text-[var(--color-danger)]">
          <Icon name="alert-triangle" size={12} />{error}
        </p>
      )}

      {editable && (
        <FileInput
          ref={input}
          className="sr-only"
          tabIndex={-1}
          aria-hidden
          aria-describedby={error ? errorId : undefined}
          accept={type === 'HORSE_PHOTO' ? 'image/jpeg,image/png,image/webp' : 'application/pdf,image/jpeg,image/png,image/webp'}
          onChange={(event) => {
            onPick(event.target.files?.[0]);
            event.target.value = '';
          }}
        />
      )}
    </li>
  );
}
