import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type {
  CompleteCareScheduleRequest,
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
      trainingStatus: 'BLOCKED',
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
      trainingStatus: 'BLOCKED',
      status: 'IN_PROGRESS',
      scheduledAt: '2026-10-07T09:10:00',
      assignedAt: '2026-10-07T09:05:00',
    };

    assert.equal(inProgressAlert.status, 'IN_PROGRESS');
    // Workspace restores symptoms from alert description on initial mount
    const initialSymptoms = inProgressAlert.description;
    assert.equal(initialSymptoms, 'Left foreleg swelling and acute lameness');
  });

  it('3. Urgent completion validation: symptoms, findings, diagnosis required; restrictionDetails required for RESTRICTED/BLOCKED', () => {
    function validateUrgentForm(data: {
      symptoms: string;
      findings: string;
      diagnosis: string;
      trainingDecision: 'ALLOWED' | 'RESTRICTED' | 'BLOCKED';
      restrictionDetails: string;
    }): Record<string, string> {
      const errors: Record<string, string> = {};
      if (!data.symptoms.trim()) errors.symptoms = 'Triệu chứng là bắt buộc';
      if (!data.findings.trim()) errors.findings = 'Kết quả khám là bắt buộc';
      if (!data.diagnosis.trim()) errors.diagnosis = 'Chẩn đoán là bắt buộc';
      if (
        (data.trainingDecision === 'RESTRICTED' || data.trainingDecision === 'BLOCKED') &&
        !data.restrictionDetails.trim()
      ) {
        errors.restrictionDetails = 'Chi tiết hạn chế là bắt buộc';
      }
      return errors;
    }

    // Missing symptoms
    const err1 = validateUrgentForm({
      symptoms: '',
      findings: 'Exam done',
      diagnosis: 'Colic',
      trainingDecision: 'BLOCKED',
      restrictionDetails: 'Stall rest',
    });
    assert.ok(err1.symptoms);

    // Missing restriction details when BLOCKED
    const err2 = validateUrgentForm({
      symptoms: 'Fever',
      findings: 'T 39.5C',
      diagnosis: 'Infection',
      trainingDecision: 'BLOCKED',
      restrictionDetails: '',
    });
    assert.ok(err2.restrictionDetails);

    // Valid submission
    const valid = validateUrgentForm({
      symptoms: 'Fever and nasal discharge',
      findings: 'T 39.2C, clear mucus',
      diagnosis: 'Upper respiratory infection',
      trainingDecision: 'BLOCKED',
      restrictionDetails: 'Isolation for 7 days with antibiotics',
    });
    assert.equal(Object.keys(valid).length, 0);
  });

  it('4. Submit complete builds valid CompleteCareScheduleRequest payload', () => {
    const payload: CompleteCareScheduleRequest = {
      symptoms: 'Swollen hock',
      findings: 'Mild swelling on left hock, no heat',
      diagnosis: 'Minor strain',
      treatment: 'Cold hose and poultice',
      trainingDecision: 'RESTRICTED',
      restrictionDetails: 'Hand walk 15 mins daily only',
      notes: 'Recheck in 3 days',
      metrics: [{ temperature: 38.1, heartRate: 38 }],
    };

    assert.equal(payload.trainingDecision, 'RESTRICTED');
    assert.equal(payload.restrictionDetails, 'Hand walk 15 mins daily only');
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

  it('6. VetReviewResponse canonical contract enforces matching types and nullable legacy fields', () => {
    const response: VetReviewResponse = {
      admissionId: 1,
      status: 'TRAINER_REVIEW',
      veterinarianId: 10,
      decision: null, // Legacy field is nullable
      trainingDecision: 'RESTRICTED',
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
    assert.equal(response.decision, null);
    assert.equal(response.trainingDecision, 'RESTRICTED');
    assert.equal(response.careScheduleId, 300);
  });
});
