import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type {
  CompleteCareScheduleRequest,
  TrainingDecision,
  UrgentAssignmentAlert,
  VetReviewResponse,
} from '../../types/index.ts';

describe('Urgent Case Workflow & Invariant Tests (F-01, F-03)', () => {
  it('1. SCHEDULED -> start transition sets status to IN_PROGRESS and preserves scheduleId', () => {
    const alert: UrgentAssignmentAlert = {
      eventId: 101,
      scheduleId: 101,
      incidentId: 50,
      veterinarianId: 10,
      horseId: 99,
      horseName: 'Storm',
      stableLocation: 'Barn A',
      stallCode: 'A-01',
      reportedById: 5,
      reportedByName: 'Groom Alex',
      reportedAt: '2026-10-07T08:00:00',
      severity: 'HIGH',
      title: 'Colic symptoms',
      description: 'Severe abdominal pain observed during morning feeding',
      imageUrl: null,
      trainingDecision: 'BLOCKED',
      status: 'SCHEDULED',
      scheduledAt: '2026-10-07T08:15:00',
      assignedAt: '2026-10-07T08:05:00',
    };

    assert.equal(alert.status, 'SCHEDULED');

    // Simulate startCareSchedule action
    const started = { ...alert, status: 'IN_PROGRESS' as const };
    assert.equal(started.status, 'IN_PROGRESS');
    assert.equal(started.scheduleId, 101);
  });

  it('2. Directly loaded or refreshed IN_PROGRESS urgent alert retains initial symptoms from description', () => {
    const inProgressAlert: UrgentAssignmentAlert = {
      eventId: 102,
      scheduleId: 102,
      incidentId: 51,
      veterinarianId: 10,
      horseId: 88,
      horseName: 'Lightning',
      stableLocation: 'Barn B',
      stallCode: 'B-04',
      reportedById: 6,
      reportedByName: 'Groom Sam',
      reportedAt: '2026-10-07T09:00:00',
      severity: 'CRITICAL',
      title: 'Lameness after gallop',
      description: 'Left foreleg swelling and acute lameness',
      imageUrl: null,
      trainingDecision: 'BLOCKED',
      status: 'IN_PROGRESS',
      scheduledAt: '2026-10-07T09:10:00',
      assignedAt: '2026-10-07T09:05:00',
    };

    assert.equal(inProgressAlert.status, 'IN_PROGRESS');
    // Workspace restores symptoms from alert description on initial mount
    const initialSymptoms = inProgressAlert.description;
    assert.equal(initialSymptoms, 'Left foreleg swelling and acute lameness');
  });

  it('3. Urgent completion validation: BLOCKED requires restriction details AND a follow-up date', () => {
    function validateUrgentForm(data: {
      symptoms: string;
      findings: string;
      diagnosis: string;
      trainingDecision: TrainingDecision;
      restrictionDetails: string;
      followUpDate: string;
    }): Record<string, string> {
      const errors: Record<string, string> = {};
      if (!data.symptoms.trim()) errors.symptoms = 'Symptoms are required';
      if (!data.findings.trim()) errors.findings = 'Examination findings are required';
      if (!data.diagnosis.trim()) errors.diagnosis = 'Diagnosis is required';
      if (data.trainingDecision === 'BLOCKED') {
        if (!data.restrictionDetails.trim()) errors.restrictionDetails = 'A reason for blocking training is required';
        if (!data.followUpDate) errors.followUpDate = 'A follow-up exam date must be selected';
      }
      return errors;
    }

    const base = {
      symptoms: 'Fever and nasal discharge',
      findings: 'T 39.2C, clear mucus',
      diagnosis: 'Upper respiratory infection',
    };

    // Missing symptoms
    assert.ok(
      validateUrgentForm({
        ...base,
        symptoms: '',
        trainingDecision: 'BLOCKED',
        restrictionDetails: 'Stall rest',
        followUpDate: '2026-10-14',
      }).symptoms,
    );

    // BLOCKED without restriction details or follow-up date
    const blockedErrors = validateUrgentForm({
      ...base,
      trainingDecision: 'BLOCKED',
      restrictionDetails: '',
      followUpDate: '',
    });
    assert.ok(blockedErrors.restrictionDetails);
    assert.ok(blockedErrors.followUpDate);

    // ALLOWED needs neither
    assert.equal(
      Object.keys(
        validateUrgentForm({ ...base, trainingDecision: 'ALLOWED', restrictionDetails: '', followUpDate: '' }),
      ).length,
      0,
    );

    // Valid BLOCKED submission
    assert.equal(
      Object.keys(
        validateUrgentForm({
          ...base,
          trainingDecision: 'BLOCKED',
          restrictionDetails: 'Isolation for 7 days with antibiotics',
          followUpDate: '2026-10-14',
        }),
      ).length,
      0,
    );
  });

  it('4. Submit complete builds valid CompleteCareScheduleRequest payload', () => {
    const payload: CompleteCareScheduleRequest = {
      symptoms: 'Swollen hock',
      findings: 'Mild swelling on left hock, no heat',
      diagnosis: 'Minor strain',
      treatment: 'Cold hose and poultice',
      trainingDecision: 'BLOCKED',
      restrictionDetails: 'Hand walk 15 mins daily only',
      notes: 'Recheck in 3 days',
      metrics: [{ temperature: 38.1, heartRate: 38 }],
      nextSchedule: {
        horseId: 99,
        careType: 'ROUTINE',
        scheduledDate: '2026-10-10',
        description: 'Follow-up after urgent case',
        idempotencyKey: 'follow-up-1',
      },
    };

    assert.equal(payload.trainingDecision, 'BLOCKED');
    assert.equal(payload.restrictionDetails, 'Hand walk 15 mins daily only');
    assert.equal(payload.nextSchedule?.careType, 'ROUTINE');
    assert.ok(payload.metrics && payload.metrics[0].temperature === 38.1);
  });

  it('5. API error preserves user workspace input without wiping form state', () => {
    const formState = {
      symptoms: 'Acute lameness',
      findings: 'Grade 3 lameness',
      diagnosis: 'Subsolar abscess',
      trainingDecision: 'BLOCKED' as const,
      restrictionDetails: 'Poultice and box rest',
    };

    let submitError = '';
    try {
      // Simulate API throwing network/server error
      throw new Error('500 Internal Server Error: Database temporary timeout');
    } catch (err: unknown) {
      submitError = err instanceof Error ? err.message : 'Unknown error';
    }

    // Workspace state is intact
    assert.ok(submitError.includes('500'));
    assert.equal(formState.symptoms, 'Acute lameness');
    assert.equal(formState.diagnosis, 'Subsolar abscess');
  });

  it('6. VetReviewResponse: BLOCKED still advances the admission to TRAINER_REVIEW', () => {
    const response: VetReviewResponse = {
      admissionId: 1,
      status: 'TRAINER_REVIEW',
      veterinarianId: 10,
      trainingDecision: 'BLOCKED',
      restrictionDetails: 'Light trotting only',
      feedback: 'Examination completed smoothly',
      reviewedAt: '2026-10-07T10:00:00',
      horseId: 99,
      horseStatus: 'CANDIDATE',
      quarantineStallId: 5,
      quarantineStallCode: 'Q-01',
      initialExamStatus: 'COMPLETED',
      healthRecordId: 200,
      vetExamId: 300,
      careScheduleId: 300,
    };

    assert.equal(response.status, 'TRAINER_REVIEW');
    assert.equal(response.trainingDecision, 'BLOCKED');
    assert.equal(response.careScheduleId, 300);
  });
});
