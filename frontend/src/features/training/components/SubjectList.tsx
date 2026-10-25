'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button, EmptyState, ListSkeleton, Modal, PageSection, Panel, Pill } from '@/components/ui';
import { displayError, formatNumber } from '@/lib/display';
import { toast } from '@/lib/toast';
import { categoryLabel, intensityOption, surfaceLabel } from '../lib/subjectOptions';
import { trainingService } from '../services/trainingService';
import type { CreateSubjectRequest, Subject } from '../types';
import { SubjectForm } from './SubjectForm';

function SubjectCard({ subject, onOpen }: { subject: Subject; onOpen: () => void }) {
  const intensity = intensityOption(subject.intensityLevel);
  return (
    <Panel padded className="flex h-full flex-col justify-between gap-3">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <h3 className="text-base font-semibold text-[var(--color-text-primary)]">{subject.name}</h3>
          <p className="text-xs text-[var(--color-text-muted)]">{categoryLabel(subject.category)}</p>
        </div>
        <Pill tone={intensity.tone} size="sm">{intensity.label}</Pill>
      </div>
      <div className="border-t border-[var(--color-border)] pt-3">
        <Button size="sm" onClick={onOpen}>View details</Button>
      </div>
    </Panel>
  );
}

function SubjectDetailModal({ subject, onClose }: { subject: Subject | null; onClose: () => void }) {
  return (
    <Modal
      open={subject !== null}
      onClose={onClose}
      size="sm"
      title={subject?.name ?? ''}
      description={subject ? categoryLabel(subject.category) : undefined}
      footer={<Button onClick={onClose}>Close</Button>}
    >
      {subject && (
        <div className="space-y-3 text-sm">
          {subject.description && <p className="text-[var(--color-text-secondary)]">{subject.description}</p>}
          <dl className="grid grid-cols-2 gap-x-4 gap-y-2">
            <dt className="text-[var(--color-text-muted)]">Intensity</dt>
            <dd><Pill tone={intensityOption(subject.intensityLevel).tone} size="sm">{intensityOption(subject.intensityLevel).label}</Pill></dd>
            <dt className="text-[var(--color-text-muted)]">Duration</dt>
            <dd className="font-medium">{subject.durationMinutes} min</dd>
            <dt className="text-[var(--color-text-muted)]">Distance</dt>
            <dd className="font-medium">{formatNumber(subject.targetDistanceMeters)} m</dd>
            <dt className="text-[var(--color-text-muted)]">Surface</dt>
            <dd className="font-medium">{surfaceLabel(subject.surfaceType)}</dd>
          </dl>
        </div>
      )}
    </Modal>
  );
}

export function SubjectList() {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [detail, setDetail] = useState<Subject | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setSubjects(await trainingService.getSubjects());
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the subject library.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  async function createSubject(request: CreateSubjectRequest) {
    setFormError(null);
    setSubmitting(true);
    try {
      await trainingService.createSubject(request);
      toast.success('Subject created.');
      setShowForm(false);
      await load();
    } catch (cause) {
      setFormError(displayError(cause, 'Unable to create the subject.'));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading && subjects.length === 0) return <Panel padded><ListSkeleton rows={4} /></Panel>;

  if (error) {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Unable to load the subject library"
          description={error}
          action={<Button size="sm" icon="refresh" onClick={load}>Try again</Button>}
        />
      </Panel>
    );
  }

  return (
    <PageSection
      title={`Subject library (${subjects.length})`}
      actions={
        <Button variant="primary" size="sm" onClick={() => setShowForm((open) => !open)}>
          {showForm ? 'Close form' : 'Create subject'}
        </Button>
      }
    >
      {showForm && (
        <SubjectForm submitting={submitting} error={formError} onSubmit={createSubject} onCancel={() => setShowForm(false)} />
      )}

      {subjects.length === 0 ? (
        <Panel padded>
          <EmptyState icon="clipboard" title="No subjects yet" description="Create your first subject to start building a training course." />
        </Panel>
      ) : (
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
          {subjects.map((subject) => <SubjectCard key={subject.id} subject={subject} onOpen={() => setDetail(subject)} />)}
        </div>
      )}

      <SubjectDetailModal subject={detail} onClose={() => setDetail(null)} />
    </PageSection>
  );
}
