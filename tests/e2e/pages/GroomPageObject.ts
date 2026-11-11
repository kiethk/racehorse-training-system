import { type Page, type Locator, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

export interface AdmissionQueueItem {
  id: number;
  status: string;
  candidateName: string;
  breed: string;
  submittedAt: string;
  arrivalDeadlineAt: string | null;
  quarantineStallCode: string | null;
  ownerName: string;
}

export class GroomPageObject {
  readonly page: Page;
  readonly repoScreenshotsDir: string;
  readonly downloadsEvidenceDir: string;

  constructor(page: Page) {
    this.page = page;
    this.repoScreenshotsDir = path.resolve(__dirname, '../results/screenshots');
    this.downloadsEvidenceDir = 'C:\\Users\\LENOVO\\Downloads\\RTMS_Groom_Automation_Evidence';

    if (!fs.existsSync(this.repoScreenshotsDir)) {
      fs.mkdirSync(this.repoScreenshotsDir, { recursive: true });
    }
    if (!fs.existsSync(this.downloadsEvidenceDir)) {
      fs.mkdirSync(this.downloadsEvidenceDir, { recursive: true });
    }
  }

  /**
   * Captures visual evidence screenshot and saves to both local repo and user Downloads folder.
   */
  async captureEvidence(name: string, locator?: Locator): Promise<string> {
    const filename = `${name}.png`;
    const repoFilePath = path.join(this.repoScreenshotsDir, filename);
    const downloadsFilePath = path.join(this.downloadsEvidenceDir, filename);

    // Wait for animations and fonts to settle
    await this.page.waitForTimeout(300);

    if (locator) {
      await locator.screenshot({ path: repoFilePath });
    } else {
      await this.page.screenshot({ path: repoFilePath, fullPage: true });
    }

    try {
      fs.copyFileSync(repoFilePath, downloadsFilePath);
    } catch (e) {
      console.warn(`Could not copy evidence to downloads: ${(e as Error).message}`);
    }

    return repoFilePath;
  }

  /**
   * Sets up deterministic route mocks for all Groom (M04) features.
   */
  async setupRouteMocks(overrides?: {
    queueItems?: AdmissionQueueItem[];
    admissionDetail55?: any;
    admissionDetail57?: any;
    tasks?: any[];
    incidents?: any[];
    horses?: any[];
    stalls?: any[];
  }) {
    // 1. Auth routes
    await this.page.route('**/api/auth/refresh', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { accessToken: 'mock-groom-token-12345' },
        }),
      });
    });

    await this.page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            userId: 101,
            email: 'groom.minh@rtms.test',
            fullName: 'Minh Groom',
            role: 'GROOM',
          },
        }),
      });
    });

    // 2. Admissions Queue & Filter
    const defaultQueueItems: AdmissionQueueItem[] = overrides?.queueItems ?? [
      {
        id: 55,
        status: 'GROOM_REVIEW',
        candidateName: 'GG DAKAR',
        breed: 'Thoroughbred',
        submittedAt: '2026-11-01T08:00:00Z',
        arrivalDeadlineAt: '2026-11-15T08:00:00Z',
        quarantineStallCode: 'Q-A10',
        ownerName: 'Nguyen Van A',
      },
      {
        id: 56,
        status: 'GROOM_REVIEW',
        candidateName: 'GALATEO',
        breed: 'Arabian',
        submittedAt: '2026-11-02T09:00:00Z',
        arrivalDeadlineAt: '2026-11-16T09:00:00Z',
        quarantineStallCode: 'Q-A11',
        ownerName: 'Tran Thi B',
      },
      {
        id: 57,
        status: 'WAITING_FOR_ARRIVAL',
        candidateName: 'ROYAL RUNNER',
        breed: 'Thoroughbred',
        submittedAt: '2026-10-25T10:00:00Z',
        arrivalDeadlineAt: '2026-11-12T10:00:00Z',
        quarantineStallCode: 'Q-A05',
        ownerName: 'Le Van C',
      },
      {
        id: 58,
        status: 'VET_REVIEW',
        candidateName: 'VELOCITY STAR',
        breed: 'Standardbred',
        submittedAt: '2026-10-20T11:00:00Z',
        arrivalDeadlineAt: '2026-11-05T11:00:00Z',
        quarantineStallCode: 'Q-A02',
        ownerName: 'Pham Thi D',
      },
    ];

    await this.page.route('**/api/admissions/groom/queue*', async (route) => {
      const url = new URL(route.request().url());
      const candidateName = url.searchParams.get('candidateName')?.toUpperCase() ?? '';
      const status = url.searchParams.get('status') ?? '';

      let filtered = [...defaultQueueItems];
      if (candidateName) {
        filtered = filtered.filter((i) => i.candidateName.toUpperCase().includes(candidateName));
      }
      if (status) {
        filtered = filtered.filter((i) => i.status === status);
      }

      const content = filtered.map((item) => ({
        admissionId: item.id,
        status: item.status,
        candidateName: item.candidateName,
        breed: item.breed,
        dateOfBirth: '2020-03-15',
        submittedAt: item.submittedAt,
        quarantineStallId: 10,
        quarantineStallCode: item.quarantineStallCode,
        trainerId: null,
        trainerName: null,
        trainerReviewedAt: null,
      }));

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            content,
            page: 0,
            size: 10,
            totalElements: content.length,
            totalPages: 1,
          },
        }),
      });
    });

    // 3. Admission Detail #55 (GROOM_REVIEW)
    const detail55 = overrides?.admissionDetail55 ?? {
      admissionId: 55,
      ownerId: 201,
      ownerName: 'Nguyen Van A',
      ownerEmail: 'owner.a@rtms.test',
      ownerPhone: '0901234567',
      status: 'GROOM_REVIEW',
      quarantineStallId: null,
      quarantineStallCode: null,
      arrivalDeadlineAt: null,
      arrivedAt: null,
      candidate: {
        id: 55,
        name: 'GG DAKAR',
        breed: 'Thoroughbred',
        sex: 'STALLION',
        dateOfBirth: '2021-03-15',
        registrationNumber: 'REG-5501',
        registryName: 'Australian Stud Book',
        sireName: 'Thunderbolt',
        sireRegistrationNumber: 'SIRE-101',
        damName: 'Desert Rose',
        damRegistrationNumber: 'DAM-202',
        pedigreeNotes: 'Exceptional sprint bloodline with multiple G1 winners.',
      },
      documents: [
        {
          id: 301,
          documentType: 'HORSE_PHOTO',
          fileUrl: '/api/files/dakar.jpg',
          recordDate: '2026-10-30',
          note: 'Front exterior profile photograph',
          uploadedAt: '2026-11-01T08:00:00Z',
          originalFileName: 'dakar_profile.jpg',
        },
        {
          id: 302,
          documentType: 'COGGINS_TEST',
          fileUrl: '/api/files/coggins.pdf',
          recordDate: '2026-10-28',
          note: 'EIA Negative test result certificate',
          uploadedAt: '2026-11-01T08:00:00Z',
          originalFileName: 'coggins_certified.pdf',
        },
      ],
      groomId: null,
      groomName: null,
      groomDecision: null,
      groomFeedback: null,
      groomReviewedAt: null,
      veterinarianId: null,
      vetTrainingDecision: null,
      vetFeedback: null,
      vetReviewedAt: null,
      trainerId: null,
      trainerFeedback: null,
      trainerReviewedAt: null,
      managerId: null,
      managerDecision: null,
      managerFeedback: null,
      managerReviewedAt: null,
      horseId: null,
      submittedAt: '2026-11-01T08:00:00Z',
      availableRegularStalls: [],
      healthRecords: [],
      initialExamSchedule: null,
      initialVetExam: null,
      capacity: {
        availableQuarantineStalls: 4,
        availableRegularStalls: 12,
        occupiedQuarantineStalls: 8,
        admissionCapacityAvailable: true,
        blockingReason: null,
      },
    };

    await this.page.route('**/api/admissions/55/groom-review', async (route) => {
      const body = route.request().postDataJSON?.() ?? {};
      const updated = {
        ...detail55,
        status: body.decision === 'REJECTED' ? 'REJECTED' : 'WAITING_FOR_STALL',
        groomFeedback: body.feedback || 'Health checks and conditions confirmed.',
        groomReviewedAt: new Date().toISOString(),
      };
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: updated }),
      });
    });

    await this.page.route('**/api/admissions/55', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: detail55 }),
      });
    });

    // 4. Admission Detail #57 (WAITING_FOR_ARRIVAL)
    const detail57 = overrides?.admissionDetail57 ?? {
      ...detail55,
      admissionId: 57,
      candidate: {
        ...detail55.candidate,
        id: 57,
        name: 'ROYAL RUNNER',
      },
      status: 'WAITING_FOR_ARRIVAL',
      quarantineStallId: 5,
      quarantineStallCode: 'Q-A05',
      arrivalDeadlineAt: '2026-11-20T12:00:00Z',
    };

    await this.page.route('**/api/admissions/57/arrival-confirmation', async (route) => {
      const updated = {
        ...detail57,
        status: 'VET_REVIEW',
        arrivedAt: new Date().toISOString(),
      };
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: updated }),
      });
    });

    await this.page.route('**/api/admissions/57', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: detail57 }),
      });
    });

    // 5. Non-existent Admission Detail #999999 (404)
    await this.page.route('**/api/admissions/999999', async (route) => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          detail: 'Admission application #999999 not found.',
          status: 404,
        }),
      });
    });

    // 6. Daily Tasks (/groom/tasks)
    const defaultTasks = overrides?.tasks ?? [
      {
        source: 'SOP',
        refId: 101,
        startTime: '05:00:00',
        endTime: '05:30:00',
        horseId: 1,
        horseName: 'GG DAKAR',
        title: 'Morning Feeding',
        note: null,
        status: 'COMPLETED',
        actionable: true,
        completionId: 1,
        completionNote: 'Full ration finished',
      },
      {
        source: 'SOP',
        refId: 102,
        startTime: '05:00:00',
        endTime: '05:30:00',
        horseId: 2,
        horseName: 'GALATEO',
        title: 'Morning Feeding',
        note: null,
        status: 'COMPLETED',
        actionable: true,
        completionId: 1,
        completionNote: 'Full ration finished',
      },
      {
        source: 'SOP',
        refId: 103,
        startTime: '05:30:00',
        endTime: '06:00:00',
        horseId: 1,
        horseName: 'GG DAKAR',
        title: 'Stall Mucking & Fresh Bedding',
        note: null,
        status: 'PENDING',
        actionable: true,
      },
      {
        source: 'PREVENTIVE_CARE',
        refId: 104,
        startTime: '10:00:00',
        endTime: '10:30:00',
        horseId: 2,
        horseName: 'GALATEO',
        title: 'Routine Health Check',
        note: null,
        status: 'PENDING',
        actionable: true,
      },
      {
        source: 'SOP',
        refId: 105,
        startTime: '11:30:00',
        endTime: '12:00:00',
        horseId: 1,
        horseName: 'GG DAKAR',
        title: 'Noon Feeding & Hydration',
        note: null,
        status: 'PENDING',
        actionable: true,
      },
      {
        source: 'SOP',
        refId: 106,
        startTime: '11:30:00',
        endTime: '12:00:00',
        horseId: 2,
        horseName: 'GALATEO',
        title: 'Noon Feeding & Hydration',
        note: null,
        status: 'PENDING',
        actionable: true,
      },
    ];

    await this.page.route('**/api/groom-daily-tasks/today*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: defaultTasks,
        }),
      });
    });

    await this.page.route('**/api/groom-daily-tasks/complete', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { completedCount: 2 },
        }),
      });
    });

    await this.page.route('**/api/groom-daily-tasks/generate-routine*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { generatedCount: 6 },
        }),
      });
    });

    // 7. Incidents (/groom/incidents)
    const defaultIncidents = overrides?.incidents ?? [
      {
        id: 11,
        horseId: 1,
        horseName: 'GG DAKAR',
        title: 'Minor scratch on left foreleg',
        description: 'Superficial skin abrasion noted during morning grooming session.',
        imageUrl: null,
        severity: 'LOW',
        status: 'REPORTED',
        reportedAt: '2026-11-10T08:30:00Z',
        handlerName: null,
        handlerNote: null,
      },
      {
        id: 12,
        horseId: 2,
        horseName: 'GALATEO',
        title: 'Mild cough after exercise',
        description: 'Infrequent dry cough observed during walk cool-down.',
        imageUrl: null,
        severity: 'MEDIUM',
        status: 'IN_REVIEW',
        reportedAt: '2026-11-09T14:15:00Z',
        handlerName: 'Dr. Robert Smith',
        handlerNote: 'Awaiting chest auscultation review.',
      },
      {
        id: 13,
        horseId: 1,
        horseName: 'GG DAKAR',
        title: 'High fever 39.8C and loss of appetite',
        description: 'Horse showed acute lethargy, shivering and refused breakfast. Urgent care exam required.',
        imageUrl: '/api/files/fever_check.jpg',
        severity: 'CRITICAL',
        status: 'REPORTED',
        reportedAt: '2026-11-11T06:00:00Z',
        handlerName: null,
        handlerNote: null,
      },
      {
        id: 14,
        horseId: 2,
        horseName: 'GALATEO',
        title: 'Hoof balance adjustment required',
        description: 'Right hind shoe slightly loosened, farrier attention requested.',
        imageUrl: null,
        severity: 'LOW',
        status: 'RESOLVED',
        reportedAt: '2026-11-05T10:00:00Z',
        handlerName: 'Dr. Emily Watson',
        handlerNote: 'Farrier reshod, gait verified clear.',
      },
    ];

    await this.page.route('**/api/incidents*', async (route) => {
      const req = route.request();
      if (req.method() === 'POST') {
        const body = req.postDataJSON?.() ?? {};
        const newReport = {
          id: 15,
          horseId: body.horseId || 1,
          horseName: body.horseId === 2 ? 'GALATEO' : 'GG DAKAR',
          title: body.title || 'New Incident Report',
          description: body.description || '',
          severity: body.severity || 'MEDIUM',
          status: 'REPORTED',
          imageUrl: null,
          reportedAt: new Date().toISOString(),
        };
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newReport }),
        });
        return;
      }

      // GET /api/incidents
      const url = new URL(req.url());
      const statusParam = url.searchParams.get('status');
      let filtered = [...defaultIncidents];
      if (statusParam && statusParam !== 'ALL') {
        filtered = filtered.filter((r) => r.status === statusParam);
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: filtered }),
      });
    });

    await this.page.route('**/api/incidents/*/image', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { imageUrl: '/api/files/incident_photo.jpg' },
        }),
      });
    });

    // 8. Stable & Barn Map (/groom/stable)
    const defaultAreas = [
      { id: 1, code: 'Q-A', name: 'Quarantine Area A', type: 'QUARANTINE' },
    ];

    const defaultStalls = overrides?.stalls ?? Array.from({ length: 12 }, (_, i) => {
      const stallNumber = i + 1;
      const stallCode = `Q-A${stallNumber.toString().padStart(2, '0')}`;
      // Q-A10 is assigned to current Groom (userId 101) with horse GALATEO
      // Q-A11 is assigned to current Groom (userId 101) RESERVED
      // Q-A12 is assigned to current Groom (userId 101) AVAILABLE
      // Q-A01..Q-A09 belong to other grooms
      const isMine = stallNumber >= 10;
      let status = 'AVAILABLE';
      if (stallNumber === 10) status = 'OCCUPIED';
      else if (stallNumber === 11) status = 'RESERVED';
      else if (stallNumber === 3 || stallNumber === 7) status = 'OCCUPIED';

      return {
        id: stallNumber,
        areaId: 1,
        groomId: isMine ? 101 : 999,
        stallNumber,
        stallCode,
        status,
      };
    });

    const defaultHorses = overrides?.horses ?? [
      {
        id: 1,
        name: 'GG DAKAR',
        currentStallId: null,
        trainingDecision: 'BLOCKED',
        status: 'ACTIVE',
      },
      {
        id: 2,
        name: 'GALATEO',
        currentStallId: 10,
        trainingDecision: 'CLEARED',
        status: 'ACTIVE',
      },
    ];

    await this.page.route('**/api/areas*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: defaultAreas }),
      });
    });

    await this.page.route('**/api/stalls*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: defaultStalls }),
      });
    });

    await this.page.route('**/api/horses*', async (route) => {
      const url = new URL(route.request().url());
      if (url.pathname.includes('/specialty')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: 'SPRINT' }),
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: defaultHorses }),
      });
    });

    // 9. Placeholder static files/images
    await this.page.route('**/api/files/**', async (route) => {
      // 1x1 transparent PNG
      const pngBuffer = Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=',
        'base64'
      );
      await route.fulfill({
        status: 200,
        contentType: 'image/png',
        body: pngBuffer,
      });
    });
  }

  // --- Navigation Helpers ---

  async gotoAdmissions() {
    await this.page.goto('/groom/admissions', { waitUntil: 'domcontentloaded' });
    await this.page.waitForSelector('table', { timeout: 15000 });
  }

  async gotoAdmissionDetail(id: number) {
    await this.page.goto(`/groom/admissions/${id}`, { waitUntil: 'domcontentloaded' });
    await this.page.waitForSelector('main', { timeout: 15000 });
  }

  async gotoTasks() {
    await this.page.goto('/groom/tasks', { waitUntil: 'domcontentloaded' });
    await this.page.waitForSelector('h1', { timeout: 15000 });
  }

  async gotoIncidents() {
    await this.page.goto('/groom/incidents', { waitUntil: 'domcontentloaded' });
    await this.page.waitForSelector('table', { timeout: 15000 });
  }

  async gotoIncidentForm() {
    await this.page.goto('/groom/incidents/new', { waitUntil: 'domcontentloaded' });
    await this.page.waitForSelector('form', { timeout: 15000 });
  }

  async gotoStable() {
    await this.page.goto('/groom/stable', { waitUntil: 'domcontentloaded' });
    await this.page.waitForSelector('main', { timeout: 15000 });
  }
}
