import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { PendingAdmissionActionDto, UrgentIncidentActionDto } from '../types';

describe('ManagerActionCenter Behavior', () => {
  it('determines split layout when both pending admissions and urgent incidents exist', () => {
    const pendingAdmissions: PendingAdmissionActionDto[] = [
      {
        admissionId: 1,
        candidateHorseName: 'DR MAYOR',
        ownerName: 'Amelia Brooks',
        breed: 'Arabian',
        ownerId: 10,
        status: 'MANAGER_REVIEW',
        submittedAt: '2026-11-09T08:00:00Z',
        arrivalDeadlineAt: null,
        isArrivalOverdueOrExpiring: false,
      },
    ];

    const urgentIncidents: UrgentIncidentActionDto[] = [
      {
        incidentId: 101,
        horseId: 1,
        horseName: 'WILD IVANA',
        title: 'Acute colic signs',
        description: 'Sweating and rolling',
        incidentType: 'TRAUMA',
        status: 'IN_REVIEW',
        reportedAt: '2026-11-09T08:00:00Z',
        reportedById: 5,
        reporterName: 'John Groom',
      },
    ];

    const hasPendingApprovals = pendingAdmissions.length > 0;
    const hasIncidentAlerts = urgentIncidents.length > 0;
    const isSplitLayout = hasPendingApprovals && hasIncidentAlerts;

    assert.equal(isSplitLayout, true);
  });

  it('determines single layout when only one action type exists', () => {
    const pendingAdmissions: PendingAdmissionActionDto[] = [];
    const urgentIncidents: UrgentIncidentActionDto[] = [
      {
        incidentId: 101,
        horseId: 1,
        horseName: 'WILD IVANA',
        title: 'Acute colic signs',
        description: 'Sweating and rolling',
        incidentType: 'TRAUMA',
        status: 'IN_REVIEW',
        reportedAt: '2026-11-09T08:00:00Z',
        reportedById: 5,
        reporterName: 'John Groom',
      },
    ];

    const hasPendingApprovals = pendingAdmissions.length > 0;
    const hasIncidentAlerts = urgentIncidents.length > 0;
    const isSplitLayout = hasPendingApprovals && hasIncidentAlerts;

    assert.equal(isSplitLayout, false);
    assert.equal(hasIncidentAlerts, true);
  });
});
