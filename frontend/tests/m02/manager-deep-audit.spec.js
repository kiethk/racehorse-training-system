const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');

const EVIDENCE_DIR = path.resolve(__dirname, '../../../docs/bmad/testing/evidence/manager-audit');

test.describe('Manager Deep Audit & Council Readiness Sweep', () => {
  test.beforeAll(() => {
    fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
  });

  test('Comprehensive Manager E2E Sweep across all 8 modules and modals', async ({ page }) => {
    const consoleErrors = [];
    const pageErrors = [];

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        const text = msg.text();
        if (!text.includes('favicon.ico') && !text.includes('chrome-extension')) {
          consoleErrors.push(text);
        }
      }
    });

    page.on('pageerror', (err) => {
      pageErrors.push(err.message);
    });

    // 1. Authenticate as CLUB_MANAGER
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.locator('#login-email').waitFor({ state: 'visible' });
    await page.fill('#login-email', 'admin@rtms.com');
    await page.fill('#login-password', 'admin123');
    await page.click('#login-submit');
    await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 15_000 });
    await page.waitForLoadState('domcontentloaded');

    // 2. Dashboard (/manager)
    await page.goto('/manager', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    await expect(page.locator('text=Fleet Readiness').first()).toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '01_manager_dashboard.png'), fullPage: true });

    // 3. Analytics (/manager/analytics)
    await page.goto('/manager/analytics', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '02_manager_analytics.png'), fullPage: true });

    // 4. Admissions (/manager/admissions)
    await page.goto('/manager/admissions', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '03_manager_admissions.png'), fullPage: true });

    // 5. Horses (/manager/horses)
    await page.goto('/manager/horses', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '04_manager_horses.png'), fullPage: true });

    // Click first horse if present to test profile view
    const firstHorseLink = page.locator('table tbody tr a, [data-testid="horse-card"], a[href^="/manager/horses/"]').first();
    if (await firstHorseLink.isVisible()) {
      await firstHorseLink.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ path: path.join(EVIDENCE_DIR, '04b_manager_horse_detail.png'), fullPage: true });
    }

    // 6. Stable Management (/manager/stable)
    await page.goto('/manager/stable', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '05_manager_stable.png'), fullPage: true });

    // 7. Accounts Management (/manager/accounts)
    await page.goto('/manager/accounts', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '06_manager_accounts.png'), fullPage: true });

    // 8. Access Control (/manager/access-control)
    await page.goto('/manager/access-control', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '07_manager_access_control.png'), fullPage: true });

    // 9. Audit Log (/manager/audit-log) with Result Status & Date Filters
    await page.goto('/manager/audit-log', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    await expect(page.locator('text=Audit Log').first()).toBeVisible({ timeout: 10_000 });

    // Test status filter dropdown if present
    const statusSelect = page.locator('select, [data-testid="status-filter"], button:has-text("Status"), button:has-text("All statuses")').first();
    if (await statusSelect.isVisible()) {
      await statusSelect.click();
      await page.waitForTimeout(300);
    }
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '08_manager_audit_log.png'), fullPage: true });

    // 10. Profile (/profile)
    await page.goto('/profile', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '09_manager_profile.png'), fullPage: true });

    // Final Assertion: Zero fatal page errors or uncaught runtime exceptions
    expect(pageErrors).toEqual([]);
  });
});
