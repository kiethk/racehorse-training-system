'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import {
  BackLink,
  Button,
  EmptyState,
  FieldLabel,
  FormField,
  Input,
  ListSkeleton,
  Notice,
  PageHeader,
  Panel,
  ScreenLayout,
  SectionTitle,
  Select,
} from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { useGroupPlanPreview } from '../hooks/useGroupPlanPreview';
import { toIsoDate, tomorrowIso } from '../lib/dateRange';
import { trainingService } from '../services/trainingService';
import type { Course, TrainingDay, TrainingGroup } from '../types';
import { DAYS_OF_WEEK } from './enrollment/ScheduleStep';
import { GroupPlanPreviewPanel } from './GroupPlanPreviewPanel';

const DEFAULT_DAYS: TrainingDay[] = ['MONDAY', 'WEDNESDAY', 'FRIDAY'];

/**
 * Assigns a course to a group in two steps: (1) the group and the course, (2) the first date and the weekdays with a live
 * preview. The backend decides every time and checks everything again when the plan is created.
 */
export function GroupPlanWizard() {
  const router = useRouter();
  const initialGroupId = useSearchParams().get('groupId');

  const [groups, setGroups] = useState<TrainingGroup[]>([]);
  const [courses, setCourses] = useState<Course[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [step, setStep] = useState<1 | 2>(1);
  const [groupId, setGroupId] = useState<number | ''>(initialGroupId ? Number(initialGroupId) : '');
  const [courseId, setCourseId] = useState<number | ''>('');
  const [startDate, setStartDate] = useState(() => tomorrowIso());
  const [trainingDays, setTrainingDays] = useState<TrainingDay[]>(DEFAULT_DAYS);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const today = toIsoDate(new Date());
  const { preview, error: previewError, pending } = useGroupPlanPreview({ groupId, courseId, startDate, trainingDays });

  useEffect(() => {
    let current = true;
    Promise.all([trainingService.getGroups(), trainingService.getCourses()])
      .then(([groupList, courseList]) => {
        if (!current) return;
        setGroups(groupList);
        setCourses(courseList);
      })
      .catch((cause) => current && setLoadError(displayError(cause, 'Unable to load groups and courses.')))
      .finally(() => current && setLoading(false));
    return () => {
      current = false;
    };
  }, []);

  function toggleDay(day: TrainingDay) {
    setTrainingDays((current) => (current.includes(day) ? current.filter((value) => value !== day) : [...current, day]));
  }

  async function create() {
    if (groupId === '' || courseId === '') return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const created = await trainingService.createGroupPlan({ groupId, courseId, startDate, trainingDays });
      toast.success('Course assigned.');
      router.push(`/trainer/plans/${created.plan.id}`);
    } catch (cause) {
      setSubmitError(displayError(cause, 'Unable to assign the course.'));
      setSubmitting(false);
    }
  }

  if (loading) return <ScreenLayout variant="form"><Panel padded><ListSkeleton rows={3} /></Panel></ScreenLayout>;
  if (loadError) {
    return (
      <ScreenLayout variant="form">
        <Panel padded><EmptyState icon="alert-triangle" title="Unable to load" description={loadError} /></Panel>
      </ScreenLayout>
    );
  }

  const group = groups.find((candidate) => candidate.id === groupId);
  const inPast = startDate !== '' && startDate < today;

  return (
    <ScreenLayout variant="form">
      <BackLink href="/trainer/plans">Back to plans</BackLink>
      <PageHeader title="Assign a course" />

      <Panel padded className="space-y-4">
        <SectionTitle>1. Group and course</SectionTitle>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField label="Group" required>
            <Select
              value={groupId}
              onChange={(event) => setGroupId(event.target.value ? Number(event.target.value) : '')}
            >
              <option value="">Choose a group</option>
              {groups.map((candidate) => (
                <option key={candidate.id} value={candidate.id} disabled={candidate.members.length === 0}>
                  {candidate.name} ({candidate.members.length})
                </option>
              ))}
            </Select>
          </FormField>
          <FormField label="Course" required>
            <Select
              value={courseId}
              onChange={(event) => setCourseId(event.target.value ? Number(event.target.value) : '')}
            >
              <option value="">Choose a course</option>
              {courses.map((candidate) => (
                <option key={candidate.id} value={candidate.id}>
                  {candidate.name} ({candidate.totalSessions})
                </option>
              ))}
            </Select>
          </FormField>
        </div>
        {step === 1 && (
          <div className="flex justify-end">
            <Button variant="primary" disabled={groupId === '' || courseId === ''} onClick={() => setStep(2)}>
              Continue
            </Button>
          </div>
        )}
      </Panel>

      {step === 2 && group && (
        <Panel padded className="space-y-4">
          <SectionTitle>2. Dates for {group.name}</SectionTitle>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FormField
              label="Start date"
              required
              error={inPast ? 'The start date cannot be in the past.' : undefined}
            >
              <Input type="date" min={today} value={startDate} onChange={(event) => setStartDate(event.target.value)} />
            </FormField>
            <div className="space-y-1.5">
              <FieldLabel>Training days *</FieldLabel>
              <div className="flex flex-wrap gap-1.5">
                {DAYS_OF_WEEK.map((day) => {
                  const active = trainingDays.includes(day.value);
                  return (
                    <Button
                      key={day.value}
                      size="sm"
                      type="button"
                      variant={active ? 'primary' : 'secondary'}
                      aria-pressed={active}
                      onClick={() => toggleDay(day.value)}
                    >
                      {day.label}
                    </Button>
                  );
                })}
              </div>
            </div>
          </div>

          {previewError && <Notice tone="error">{previewError}</Notice>}
          {preview && <GroupPlanPreviewPanel preview={preview} stale={pending} onUseDate={setStartDate} />}
          {submitError && <Notice tone="error">{submitError}</Notice>}

          <div className="flex justify-end gap-2">
            <Button variant="tertiary" onClick={() => setStep(1)} disabled={submitting}>Back</Button>
            <Button
              variant="primary"
              disabled={submitting || pending || !preview?.canCreate || inPast}
              onClick={create}
            >
              {submitting ? 'Assigning...' : 'Assign course'}
            </Button>
          </div>
        </Panel>
      )}
    </ScreenLayout>
  );
}
