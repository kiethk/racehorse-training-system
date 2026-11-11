import { test, expect } from '@playwright/test';
import { GroomPageObject } from './pages/GroomPageObject';

test.describe('Groom (M04) Automated E2E Test Suite & Visual Verification', () => {
  let pom: GroomPageObject;

  test.beforeEach(async ({ page }) => {
    pom = new GroomPageObject(page);
    await pom.setupRouteMocks();
  });

  // =========================================================================
  // Part 1: Admissions Queue & Approval Workflow (GRM_AUTO_01 .. GRM_AUTO_08)
  // =========================================================================
  test.describe('Part 1: Admissions Queue & Approval Workflow', () => {
    test('GRM_AUTO_01: Verify admissions queue layout, table headers and badges', async ({ page }) => {
      await pom.gotoAdmissions();

      // Check table and key headers
      const table = page.locator('table');
      await expect(table).toBeVisible();
      await expect(table.locator('th', { hasText: 'Horse' })).toBeVisible();
      await expect(table.locator('th', { hasText: 'Status' })).toBeVisible();
      await expect(table.locator('th', { hasText: 'Submitted' })).toBeVisible();

      // Verify candidates rendered
      await expect(page.getByText('GG DAKAR')).toBeVisible();
      await expect(page.getByText('GALATEO')).toBeVisible();

      // Verify status badges inside table
      await expect(table.getByText('Groom review').first()).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_01');
    });

    test('GRM_AUTO_02: Search admission by candidate horse name', async ({ page }) => {
      await pom.gotoAdmissions();

      const searchInput = page.getByPlaceholder('Search horse name');
      await expect(searchInput).toBeVisible();
      await searchInput.fill('GALATEO');

      // Click Apply button
      const applyButton = page.getByRole('button', { name: 'Apply' });
      await applyButton.click();

      // Verify only GALATEO is in the table
      await expect(page.getByText('GALATEO')).toBeVisible();
      await expect(page.getByText('GG DAKAR')).not.toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_02');
    });

    test('GRM_AUTO_03: Filter admissions queue by status', async ({ page }) => {
      await pom.gotoAdmissions();

      // Open status filter select
      const statusSelect = page.locator('select').first();
      await statusSelect.selectOption('VET_REVIEW');

      const applyButton = page.getByRole('button', { name: 'Apply' });
      await applyButton.click();

      // VELOCITY STAR has status VET_REVIEW
      await expect(page.getByText('VELOCITY STAR')).toBeVisible();
      await expect(page.getByText('GG DAKAR')).not.toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_03');
    });

    test('GRM_AUTO_04: View admission application details in GROOM_REVIEW', async ({ page }) => {
      await pom.gotoAdmissionDetail(55);

      // Verify candidate details
      await expect(page.getByRole('heading', { name: 'GG DAKAR' })).toBeVisible();
      await expect(page.getByText('Australian Stud Book')).toBeVisible();
      await expect(page.getByText('Thunderbolt')).toBeVisible();
      await expect(page.getByText('Desert Rose')).toBeVisible();

      // Verify Groom decision card with action buttons
      await expect(page.getByRole('heading', { name: 'Groom decision' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Approve' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Reject' })).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_04');
    });

    test('GRM_AUTO_05: Approve admission with feedback note', async ({ page }) => {
      await pom.gotoAdmissionDetail(55);

      // Click Approve on the sidecard
      await page.getByRole('button', { name: 'Approve' }).click();

      // Verify modal opened
      await expect(page.getByRole('heading', { name: 'Approve admission?' })).toBeVisible();

      // Fill optional feedback
      const feedbackInput = page.getByRole('dialog').locator('textarea');
      await feedbackInput.fill('Quarantine stall condition checked and ready.');

      // Confirm approval
      const confirmApprove = page.getByRole('dialog').getByRole('button', { name: 'Approve' });
      await confirmApprove.click();

      // Verify toast confirmation
      await expect(page.locator('text=Admission approved')).toBeVisible({ timeout: 5000 });

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_05');
    });

    test('GRM_AUTO_06: Validate mandatory reason when rejecting admission', async ({ page }) => {
      await pom.gotoAdmissionDetail(55);

      // Click Reject on the sidecard
      await page.getByRole('button', { name: 'Reject' }).click();

      // Verify modal opened
      await expect(page.getByRole('heading', { name: 'Reject admission?' })).toBeVisible();

      const rejectBtn = page.getByRole('dialog').getByRole('button', { name: 'Reject' });
      // Button must be disabled when reason is empty
      await expect(rejectBtn).toBeDisabled();

      // Focus and blur to trigger touched validation message
      const textarea = page.getByRole('dialog').locator('textarea');
      await textarea.focus();
      await textarea.blur();

      await expect(page.getByText('Enter a reason to reject.')).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_06');
    });

    test('GRM_AUTO_07: Check rejection feedback character counter limit (BVA Max 2000)', async ({ page }) => {
      await pom.gotoAdmissionDetail(55);

      await page.getByRole('button', { name: 'Reject' }).click();
      await expect(page.getByRole('heading', { name: 'Reject admission?' })).toBeVisible();

      const textarea = page.getByRole('dialog').locator('textarea');
      const longText = 'A'.repeat(2500);
      await textarea.fill(longText);

      // maxLength is 2000 so hint shows 2000/2000
      await expect(page.getByText('2000/2000')).toBeVisible();
      const actualValue = await textarea.inputValue();
      expect(actualValue.length).toBe(2000);

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_07');
    });

    test('GRM_AUTO_08: Confirm horse physical arrival for WAITING_FOR_ARRIVAL', async ({ page }) => {
      await pom.gotoAdmissionDetail(57);

      await expect(page.getByRole('heading', { name: 'Confirm horse arrival' })).toBeVisible();
      const markArrivedBtn = page.getByRole('button', { name: 'Mark horse arrived' });
      await expect(markArrivedBtn).toBeVisible();

      await markArrivedBtn.click();

      // Confirm modal opens
      await expect(page.getByRole('heading', { name: 'Confirm the horse has arrived?' })).toBeVisible();
      const confirmBtn = page.getByRole('dialog').getByRole('button', { name: 'Mark arrived' });
      await confirmBtn.click();

      // Verify arrival success toast
      await expect(page.locator('text=Arrival confirmed')).toBeVisible({ timeout: 5000 });

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_08');
    });
  });

  // =========================================================================
  // Part 2: Daily Tasks & Timeline Management (GRM_AUTO_09 .. GRM_AUTO_16)
  // =========================================================================
  test.describe('Part 2: Daily Tasks & Timeline Management', () => {
    test('GRM_AUTO_09: Daily tasks timeline strip layout with scheduled time blocks', async ({ page }) => {
      await pom.gotoTasks();

      // Verify key time blocks
      await expect(page.getByText('05:00')).toBeVisible();
      await expect(page.getByText('Morning Feeding')).toBeVisible();
      await expect(page.getByText('05:30')).toBeVisible();
      await expect(page.getByText('Stall Mucking & Fresh Bedding')).toBeVisible();
      await expect(page.getByText('10:00')).toBeVisible();
      await expect(page.getByText('11:30')).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_09');
    });

    test('GRM_AUTO_10: Task progress bar and header counter calculation', async ({ page }) => {
      await pom.gotoTasks();

      // Verify progress summary
      await expect(page.getByText(/Today's progress: 2\/6 items completed/i)).toBeVisible();
      await expect(page.getByText('33%')).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_10');
    });

    test('GRM_AUTO_11: CompleteTaskDialog multi-horse batch selection', async ({ page }) => {
      await pom.gotoTasks();

      // 11:30 feeding block has 2 pending tasks
      await page.getByRole('button', { name: 'Complete (2)' }).first().click();

      // Verify modal opened with horse checkboxes
      await expect(page.getByRole('heading', { name: 'Complete 2 tasks' })).toBeVisible();
      await expect(page.getByRole('dialog').getByText('GG DAKAR')).toBeVisible();
      await expect(page.getByRole('dialog').getByText('GALATEO')).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_11');
    });

    test('GRM_AUTO_12: CompleteTaskDialog validation when unticking all horses', async ({ page }) => {
      await pom.gotoTasks();

      await page.getByRole('button', { name: 'Complete (2)' }).first().click();

      // Click Untick all button
      const untickBtn = page.getByRole('dialog').getByRole('button', { name: 'Untick all' });
      await untickBtn.click();

      // Submit button should be disabled and show "Mark 0 as completed"
      const submitBtn = page.getByRole('dialog').getByRole('button', { name: 'Mark 0 as completed' });
      await expect(submitBtn).toBeDisabled();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_12');
    });

    test('GRM_AUTO_13: Complete task submission and status transition', async ({ page }) => {
      await pom.gotoTasks();

      await page.getByRole('button', { name: 'Complete (2)' }).first().click();

      // Fill note
      const noteInput = page.getByRole('dialog').locator('textarea');
      await noteInput.fill('Fresh water and electrolyte added to buckets.');

      // Click Mark 2 as completed
      const submitBtn = page.getByRole('dialog').getByRole('button', { name: /Mark \d+ as completed/i });
      await submitBtn.click();

      // Toast confirmation
      await expect(page.locator('text=Tasks completed')).toBeVisible({ timeout: 5000 });

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_13');
    });

    test('GRM_AUTO_14: Upload photo evidence for stall cleaning task (<10MB)', async ({ page }) => {
      await pom.gotoTasks();

      const cleanBlock = page.locator('div', { hasText: '05:30' }).filter({ hasText: 'Stall Mucking' }).first();
      await cleanBlock.getByRole('button', { name: /^Complete$/i }).click();

      // Upload valid image
      const fileInput = page.getByRole('dialog').locator('input[type="file"]');
      await fileInput.setInputFiles({
        name: 'stall_cleaned.jpg',
        mimeType: 'image/jpeg',
        buffer: Buffer.from('data:image/jpeg;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'),
      });

      // Verify photo count updated in hint
      await expect(page.getByText('1/5')).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_14');
    });

    test('GRM_AUTO_15: Reject invalid file format for photo upload (.pdf)', async ({ page }) => {
      await pom.gotoTasks();

      const cleanBlock = page.locator('div', { hasText: '05:30' }).filter({ hasText: 'Stall Mucking' }).first();
      await cleanBlock.getByRole('button', { name: /^Complete$/i }).click();

      // Upload invalid PDF file
      const fileInput = page.getByRole('dialog').locator('input[type="file"]');
      await fileInput.setInputFiles({
        name: 'report.pdf',
        mimeType: 'application/pdf',
        buffer: Buffer.from('%PDF-1.4 simulated pdf document'),
      });

      // Verify error notice banner
      await expect(
        page.getByText('Photos must be JPEG, PNG or WebP images of at most 10 MB each.')
      ).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_15');
    });

    test('GRM_AUTO_16: Submission loading state and idempotency guard', async ({ page }) => {
      await pom.gotoTasks();

      let finishFulfill: () => void;
      const fulfillPromise = new Promise<void>((r) => { finishFulfill = r; });

      await page.route('**/api/groom-daily-tasks/complete', async (route) => {
        await fulfillPromise;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { completedCount: 1 } }),
        });
      });

      const cleanBlock = page.locator('div', { hasText: '05:30' }).filter({ hasText: 'Stall Mucking' }).first();
      await cleanBlock.getByRole('button', { name: /^Complete$/i }).click();

      const submitBtn = page.getByRole('dialog').getByRole('button', { name: /Mark as completed|Mark \d+ as completed/i });
      await submitBtn.click();

      // Verify button is disabled during submit
      await expect(submitBtn).toBeDisabled();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_16');

      finishFulfill!();
      await page.waitForTimeout(300);
    });
  });

  // =========================================================================
  // Part 3: Incident Reporting & Emergency Alerts (GRM_AUTO_17 .. GRM_AUTO_23)
  // =========================================================================
  test.describe('Part 3: Incident Reporting & Emergency Alerts', () => {
    test('GRM_AUTO_17: Incident report form layout and field verification', async ({ page }) => {
      await pom.gotoIncidentForm();

      await expect(page.getByRole('heading', { name: 'Incident Report' })).toBeVisible();
      await expect(page.getByText('Horse involved in the incident')).toBeVisible();
      await expect(page.getByText('Severity')).toBeVisible();
      await expect(page.getByText('Incident title')).toBeVisible();
      await expect(page.getByText('Detailed description')).toBeVisible();
      await expect(page.getByRole('button', { name: 'Submit incident report' })).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_17');
    });

    test('GRM_AUTO_18: Emergency urgent care alert banner on CRITICAL severity', async ({ page }) => {
      await pom.gotoIncidentForm();

      // Click Critical radio card
      const criticalOption = page.locator('label', { hasText: 'Critical' });
      await criticalOption.click();

      // Emergency alert banner should appear
      await expect(page.getByText(/Emergency Urgent Care Alert:/i)).toBeVisible();
      await expect(page.getByText(/pauses training clearance \(BLOCKED\) and triggers a real-time urgent alert/i)).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_18');
    });

    test('GRM_AUTO_19: Validate mandatory fields on incident creation', async ({ page }) => {
      await pom.gotoIncidentForm();

      // Try submitting without title and description
      const submitBtn = page.getByRole('button', { name: 'Submit incident report' });
      await submitBtn.click();

      // HTML5 or custom validation keeps page on form
      await expect(page).toHaveURL(/\/groom\/incidents\/new/);

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_19');
    });

    test('GRM_AUTO_20: Reject image file exceeding 10MB limit', async ({ page }) => {
      await pom.gotoIncidentForm();

      // Create dummy buffer of 11 MB
      const bigBuffer = Buffer.alloc(11 * 1024 * 1024);
      const fileInput = page.locator('input[type="file"]');
      await fileInput.setInputFiles({
        name: 'huge_injury.jpg',
        mimeType: 'image/jpeg',
        buffer: bigBuffer,
      });

      // Verify error banner
      await expect(page.getByText('The image must not exceed 10 MB.')).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_20');
    });

    test('GRM_AUTO_21: Submit valid incident report and redirect', async ({ page }) => {
      await pom.gotoIncidentForm();

      await page.locator('input[type="text"]').fill('Superficial scratch on left fetlock');
      await page.locator('textarea').fill('Minor abrasion noticed after paddock turnout. Cleansed and disinfected.');

      const submitBtn = page.getByRole('button', { name: 'Submit incident report' });
      await submitBtn.click();

      // Should redirect to /groom/incidents
      await expect(page).toHaveURL(/\/groom\/incidents/, { timeout: 10000 });
      await expect(page.getByRole('heading', { name: 'Incident Reports' })).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_21');
    });

    test('GRM_AUTO_22: View incident detail modal with severity and status', async ({ page }) => {
      await pom.gotoIncidents();

      // Find critical report #13
      const row13 = page.locator('tr', { hasText: '#13' });
      await expect(row13).toBeVisible();
      await row13.getByRole('button', { name: 'Details' }).click();

      // Incident modal opens
      const modal = page.getByRole('dialog');
      await expect(modal).toBeVisible();
      await expect(modal.getByText('Critical')).toBeVisible();
      await expect(modal.getByText('GG DAKAR')).toBeVisible();
      await expect(modal.getByText(/Horse showed acute lethargy/i)).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_22');
    });

    test('GRM_AUTO_23: Filter incident list by status chip', async ({ page }) => {
      await pom.gotoIncidents();

      // Click Resolved filter chip (rendered with role="radio")
      const resolvedChip = page.getByRole('radio', { name: 'Resolved' });
      await resolvedChip.click();

      // Verify table displays #14 (Resolved) and hides other statuses
      await expect(page.getByText('#14')).toBeVisible();
      await expect(page.getByText('#13')).not.toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_23');
    });
  });

  // =========================================================================
  // Part 4: Barn & Stable Map Management (GRM_AUTO_24 .. GRM_AUTO_30)
  // =========================================================================
  test.describe('Part 4: Barn & Stable Map Management', () => {
    test('GRM_AUTO_24: Quarantine area 12-stall floor plan grid', async ({ page }) => {
      await pom.gotoStable();

      // Verify Area heading
      await expect(page.getByText('Quarantine Area A')).toBeVisible();

      // Verify 12 stall cells are present
      for (let i = 1; i <= 12; i++) {
        const code = `Q-A${i.toString().padStart(2, '0')}`;
        await expect(page.getByText(code)).toBeVisible();
      }

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_24');
    });

    test('GRM_AUTO_25: Stable legend display and status tones', async ({ page }) => {
      await pom.gotoStable();

      // Verify 4 status labels in legend with first match
      await expect(page.getByText('Occupied', { exact: true }).first()).toBeVisible();
      await expect(page.getByText('Training blocked').first()).toBeVisible();
      await expect(page.getByText('Reserved').first()).toBeVisible();
      await expect(page.getByText('Empty').first()).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_25');
    });

    test('GRM_AUTO_26: Stall ownership isolation for unassigned stalls', async ({ page }) => {
      await pom.gotoStable();

      // Q-A01 is not assigned to current groom -> displays "Not your stall"
      const unassignedStalls = page.locator('text=Not your stall');
      await expect(unassignedStalls.first()).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_26');
    });

    test('GRM_AUTO_27: Stall detail drawer on assigned occupied stall click', async ({ page }) => {
      await pom.gotoStable();

      // Q-A10 is assigned to groom with horse GALATEO
      const stall10 = page.locator('[aria-label*="Q-A10"]').first();
      await stall10.click();

      // Drawer panel opens with horse info and groom name
      await expect(page.getByRole('paragraph').filter({ hasText: 'GALATEO' })).toBeVisible();
      await expect(page.getByRole('main').getByText('Minh Groom')).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_27');
    });

    test('GRM_AUTO_28: Switch view between 2D Plan and 3D Barn canvas', async ({ page }) => {
      await pom.gotoStable();

      // Click 3D barn segmented control (rendered as radio)
      const threeDBtn = page.getByRole('radio', { name: /3D barn/i });
      await threeDBtn.click();

      // Verify 3D view mode triggers
      await expect(page.getByRole('radio', { name: 'Isometric' })).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_28');
    });

    test('GRM_AUTO_29: Responsive layout on tablet viewport (768x1024)', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
      await pom.gotoTasks();

      // Verify tasks checklist displays without breakage
      await expect(page.getByRole('heading', { name: /Today's Task Checklist/i })).toBeVisible();
      await expect(page.getByText(/Today's progress/i)).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_29');
    });

    test('GRM_AUTO_30: EmptyState and 404 handling on non-existent admission ID', async ({ page }) => {
      await pom.gotoAdmissionDetail(999999);

      // Verify EmptyState component
      await expect(
        page.getByText('Unable to load application').or(page.getByText('Application not found'))
      ).toBeVisible();

      // Verify retry or back action
      await expect(
        page.getByRole('button', { name: 'Retry' }).or(page.getByRole('link', { name: /Back/i }))
      ).toBeVisible();

      // Evidence capture
      await pom.captureEvidence('GRM_AUTO_30');
    });
  });
});
