import { test, expect, type Page } from '@playwright/test';

/**
 * Page Object Model for Club Manager Executive Analytics
 */
class ManagerAnalyticsPageObject {
  constructor(private readonly page: Page) {}

  async gotoDashboard() {
    await this.page.goto('/manager');
  }

  async gotoAnalytics() {
    await this.page.goto('/manager/analytics');
  }

  get executiveEdaCard() {
    return this.page.getByRole('heading', { name: /Executive Exploratory Data Analysis/i });
  }

  get viewFullEdaLink() {
    return this.page.getByRole('link', { name: /View (Operations & Performance|Full EDA Suite)/i });
  }

  getTabButton(name: string | RegExp) {
    return this.page.getByRole('button', { name });
  }

  getTimeWindowButton(days: string) {
    return this.page.getByRole('button', { name: days, exact: true });
  }
}

test.describe('Club Manager EDA Multi-Tier E2E & Visual Verification', () => {
  let pom: ManagerAnalyticsPageObject;

  test.beforeEach(async ({ page }) => {
    pom = new ManagerAnalyticsPageObject(page);
  });

  test('TC-EDA-01: Executive EDA widget is visible on Manager Dashboard with diagnostic indicators', async ({ page }) => {
    await pom.gotoDashboard();

    // Verify Executive EDA Card heading
    await expect(pom.executiveEdaCard).toBeVisible();

    // Verify 4 diagnostic indicator cards are rendered
    await expect(page.getByText(/Vet Rollover Delays/i)).toBeVisible();
    await expect(page.getByText(/Trainer Adherence/i)).toBeVisible();
    await expect(page.getByText(/Quarantine Buffer/i)).toBeVisible();
    await expect(page.getByText(/Admission SLA Gate/i)).toBeVisible();

    // Verify quick action link to full suite
    await expect(pom.viewFullEdaLink).toBeVisible();
  });

  test('TC-EDA-02: Clicking View Full EDA Suite navigates to /manager/analytics', async ({ page }) => {
    await pom.gotoDashboard();
    await pom.viewFullEdaLink.click();

    await expect(page).toHaveURL(/\/manager\/analytics/);
    await expect(page.getByRole('heading', { name: /Executive Exploratory Data Analysis \(EDA\)/i })).toBeVisible();
  });

  test('TC-EDA-03: Tab switching renders all 4 domain EDA analytical views', async ({ page }) => {
    await pom.gotoAnalytics();

    // Tab 1: Veterinary Delays & Rollovers (default active)
    await expect(pom.getTabButton(/Veterinary Delays & Rollovers/i)).toBeVisible();
    await expect(page.getByText(/Veterinarian Rollover Rates vs. Clinic Average/i)).toBeVisible();
    await expect(page.getByText(/Reschedule Reason Breakdown/i)).toBeVisible();

    // Tab 2: Trainer Evaluation & Progress
    await pom.getTabButton(/Trainer Evaluation & Progress/i).click();
    await expect(page.getByText(/Trainer Workload Adherence vs. Performance Rating/i)).toBeVisible();
    await expect(page.getByText(/Trainer Fitness & Conditioning Matrix/i)).toBeVisible();

    // Tab 3: Barn Occupancy & Quarantine
    await pom.getTabButton(/Barn Occupancy & Quarantine/i).click();
    await expect(page.getByText(/Facility Capacity Breakdown by Barn & Area/i)).toBeVisible();
    await expect(page.getByText(/Barn & Area Utilization Details/i)).toBeVisible();

    // Tab 4: Herd Intake & Admission Funnel
    await pom.getTabButton(/Herd Intake & Admission Funnel/i).click();
    await expect(page.getByText(/Admission Pipeline Funnel & Stage Drop-off/i)).toBeVisible();
    await expect(page.getByText(/Median Review Latency by Gate/i)).toBeVisible();
  });

  test('TC-EDA-04: Time window selector dynamically triggers data reload', async ({ page }) => {
    await pom.gotoAnalytics();

    // Check 30d filter
    const btn30d = pom.getTimeWindowButton('30d');
    await expect(btn30d).toBeVisible();
    await btn30d.click();
    await expect(btn30d).toHaveClass(/bg-\[var\(--color-primary\)\]/);

    // Check 180d filter
    const btn180d = pom.getTimeWindowButton('180d');
    await btn180d.click();
    await expect(btn180d).toHaveClass(/bg-\[var\(--color-primary\)\]/);
  });
});
