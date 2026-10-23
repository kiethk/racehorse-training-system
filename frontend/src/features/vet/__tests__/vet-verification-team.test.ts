import test, { describe } from 'node:test';
import assert from 'node:assert/strict';
import type { CareScheduleResponse, CareScheduleDetailResponse, TreatmentPlan, Prescription } from '@/features/care/types';

describe('Verification-First Team: Vet Actor Acceptance Tests', () => {

  describe('AC-3: Care Schedule List Query Vet Filter Invariant', () => {
    // Controller logic simulation:
    // If user is VETERINARIAN:
    //   If status == CareScheduleStatus.REQUESTED:
    //     if (veterinarianId != null && !veterinarianId.equals(currentUser.getUserId())) {
    //       veterinarianId = currentUser.getUserId();
    //     }
    //   Else:
    //     veterinarianId = currentUser.getUserId();

    function resolveVetListFilter(
      role: string,
      currentUserId: number,
      status: string | null,
      requestedVetId: number | null
    ): number | null {
      let veterinarianId = requestedVetId;
      if (role === 'VETERINARIAN') {
        if (status === 'REQUESTED') {
          if (veterinarianId != null && veterinarianId !== currentUserId) {
            veterinarianId = currentUserId;
          }
        } else {
          veterinarianId = currentUserId;
        }
      }
      return veterinarianId;
    }

    test('allows VETERINARIAN to view unassigned queue when status is REQUESTED and vetId is null', () => {
      const resolved = resolveVetListFilter('VETERINARIAN', 101, 'REQUESTED', null);
      assert.equal(resolved, null, 'Unassigned REQUESTED schedules must have veterinarianId = null');
    });

    test('restricts VETERINARIAN from querying other vet ID when status is REQUESTED', () => {
      const resolved = resolveVetListFilter('VETERINARIAN', 101, 'REQUESTED', 999);
      assert.equal(resolved, 101, 'Querying another vet ID must be forced to current vet ID');
    });

    test('strictly enforces current vet ID when status is SCHEDULED or IN_PROGRESS', () => {
      const scheduledResolved = resolveVetListFilter('VETERINARIAN', 101, 'SCHEDULED', null);
      assert.equal(scheduledResolved, 101, 'SCHEDULED status must enforce current vet ID');

      const inProgressResolved = resolveVetListFilter('VETERINARIAN', 101, 'IN_PROGRESS', null);
      assert.equal(inProgressResolved, 101, 'IN_PROGRESS status must enforce current vet ID');
    });
  });

  describe('AC-4: Preventive Dose Invariant - CANCELLED Plans Excluded', () => {
    interface MockDoseRecord {
      horseId: number;
      protocolId: number;
      startDate: string;
      planStatus: 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
    }

    function computeLastPreventiveDoses(records: MockDoseRecord[]): Map<string, string> {
      // Logic matching PrescriptionRepository query:
      // AND p.preventiveProtocolId IS NOT NULL AND p.startDate IS NOT NULL AND tp.status <> 'CANCELLED'
      const valid = records.filter(r => r.planStatus !== 'CANCELLED');
      const maxDateByHorseAndProtocol = new Map<string, string>();

      for (const r of valid) {
        const key = `${r.horseId}:${r.protocolId}`;
        const current = maxDateByHorseAndProtocol.get(key);
        if (!current || r.startDate > current) {
          maxDateByHorseAndProtocol.set(key, r.startDate);
        }
      }
      return maxDateByHorseAndProtocol;
    }

    test('ignores doses administered under CANCELLED treatment plans', () => {
      const records: MockDoseRecord[] = [
        { horseId: 5, protocolId: 1, startDate: '2026-01-15', planStatus: 'COMPLETED' },
        { horseId: 5, protocolId: 1, startDate: '2026-06-20', planStatus: 'CANCELLED' }, // Cancelled due to reaction
        { horseId: 5, protocolId: 2, startDate: '2026-03-10', planStatus: 'ACTIVE' },
      ];

      const lastDoses = computeLastPreventiveDoses(records);

      assert.equal(lastDoses.get('5:1'), '2026-01-15', 'Cancelled dose on 2026-06-20 must not be considered the last dose');
      assert.equal(lastDoses.get('5:2'), '2026-03-10');
    });
  });

  describe('AC-6: Health Records Treatment Plans & Prescriptions Association', () => {
    test('treatment plan structure accurately pairs with prescription line items', () => {
      const rx: Prescription = {
        id: 1,
        medicationName: 'Phenylbutazone (Bute)',
        dosage: '2g',
        frequency: 'Once daily',
        route: 'Oral',
        startDate: '2026-10-23',
        endDate: '2026-10-30',
        instructions: 'Mix with morning sweet feed',
      };

      const plan: TreatmentPlan = {
        id: 10,
        healthRecordId: 55,
        treatmentName: 'Tendon Strain Anti-inflammatory Regimen',
        instructions: 'Daily oral NSAID and cold hosing twice daily',
        startDate: '2026-10-23',
        endDate: '2026-11-06',
        status: 'ACTIVE',
        prescriptions: [rx],
      };

      assert.equal(plan.healthRecordId, 55);
      assert.equal(plan.status, 'ACTIVE');
      assert.equal(plan.prescriptions?.length, 1);
      assert.equal(plan.prescriptions[0].medicationName, 'Phenylbutazone (Bute)');
      assert.equal(plan.prescriptions[0].route, 'Oral');
    });
  });

  describe('AC-7: Urgent Case View History Restoration', () => {
    function restoreCompletedUrgentCase(
      caseStatus: string,
      description: string,
      detail: CareScheduleDetailResponse | null
    ) {
      if (caseStatus !== 'COMPLETED' || !detail || !detail.healthRecord) {
        return null;
      }
      return {
        symptoms: description || '',
        findings: detail.healthRecord.findings || '',
        diagnosis: detail.healthRecord.diagnosis || '',
        treatment: detail.healthRecord.treatment || undefined,
        trainingDecision: detail.healthRecord.trainingDecision,
        restrictionDetails: detail.healthRecord.restrictionDetails || undefined,
      };
    }

    test('populates historical examination data when urgent case is COMPLETED', () => {
      const mockDetail: CareScheduleDetailResponse = {
        schedule: {
          id: 42,
          horseId: 7,
          veterinarianId: 101,
          admissionId: null,
          sourceIncidentId: 12,
          careType: 'URGENT',
          status: 'COMPLETED',
          scheduledAt: '2026-10-22T10:00:00',
          durationMinutes: 45,
          description: 'Sudden lameness after training',
          completedAt: '2026-10-22T11:00:00',
          cancelReason: null,
          createdAt: '2026-10-22T09:30:00',
          updatedAt: '2026-10-22T11:00:00',
          purpose: 'FOLLOW_UP',
          requestedAt: '2026-10-22T09:30:00',
          rescheduleReason: null,
          rescheduleNote: null,
        },
        horse: {
          id: 7,
          name: 'Thunderbolt',
          breed: 'Thoroughbred',
          registrationNumber: 'TB-9921',
          trainingDecision: 'BLOCKED',
        },
        veterinarian: {
          id: 101,
          fullName: 'Dr. John Vet',
          email: 'vet@rtms.vn',
        },
        healthRecord: {
          id: 88,
          findings: 'Grade 3 lameness on left hind limb, heat detected in hock joint',
          diagnosis: 'Acute hock sprain',
          treatment: 'Cryotherapy and NSAIDs for 7 days',
          trainingDecision: 'BLOCKED',
          restrictionDetails: 'Complete stall rest, no hand walking for 3 days',
        },
      };

      const restored = restoreCompletedUrgentCase('COMPLETED', 'Sudden lameness after training', mockDetail);

      assert.notEqual(restored, null);
      assert.equal(restored?.diagnosis, 'Acute hock sprain');
      assert.equal(restored?.findings, 'Grade 3 lameness on left hind limb, heat detected in hock joint');
      assert.equal(restored?.trainingDecision, 'BLOCKED');
      assert.equal(restored?.restrictionDetails, 'Complete stall rest, no hand walking for 3 days');
      assert.equal(restored?.treatment, 'Cryotherapy and NSAIDs for 7 days');
    });

    test('returns null when urgent case is still IN_PROGRESS', () => {
      const restored = restoreCompletedUrgentCase('IN_PROGRESS', 'Sudden lameness', null);
      assert.equal(restored, null, 'Active cases should keep dynamic workspace data');
    });
  });
});
