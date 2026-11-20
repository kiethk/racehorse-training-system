const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');

const EVIDENCE_DIR = path.resolve(__dirname, '../../../docs/bmad/testing/evidence/super-admin-audit');

test.describe('Super Admin Protection E2E Test Suite', () => {
  test.beforeAll(() => {
    fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
  });

  test('Super Admin (User 1) is protected in Accounts list and Detail Modal from own session', async ({ page }) => {
    // 1. Authenticate as Super Admin (admin@rtms.com)
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.locator('#login-email').waitFor({ state: 'visible' });
    await page.fill('#login-email', 'admin@rtms.com');
    await page.fill('#login-password', 'admin123');
    await page.click('#login-submit');
    await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 15_000 });

    // 2. Navigate to /manager/accounts filtered for admin@rtms.com
    await page.goto('/manager/accounts?search=admin@rtms.com', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);

    // 3. Find row with admin@rtms.com
    const superAdminRow = page.locator('table tbody tr', { hasText: 'admin@rtms.com' });
    await expect(superAdminRow).toBeVisible({ timeout: 10_000 });

    // 4. Verify Protected badge: text is visible, icon exists, and NO background color
    const protectedBadge = superAdminRow.locator('text=Protected').first();
    await expect(protectedBadge).toBeVisible();

    // Check computed background color is transparent or rgba(0, 0, 0, 0)
    const bgColor = await protectedBadge.evaluate((el) => window.getComputedStyle(el).backgroundColor);
    expect(bgColor).toMatch(/rgba\(0,\s*0,\s*0,\s*0\)|transparent/);

    // 5. Verify action button is disabled / protected
    const actionBtn = superAdminRow.locator('td').last().locator('button').last();
    await expect(actionBtn).toBeDisabled();

    // Screenshot Accounts Table
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '01_super_admin_accounts_table.png'), fullPage: true });

    // 6. Click View button for Super Admin
    const viewBtn = superAdminRow.locator('button', { hasText: 'View' });
    await viewBtn.click();

    // 7. Check Modal opens
    const modal = page.locator('[role="dialog"], .fixed');
    await expect(modal.first()).toBeVisible({ timeout: 5_000 });

    // Check Role section displays Protected indicator
    const modalRoleProtected = modal.locator('text=Protected').first();
    await expect(modalRoleProtected).toBeVisible();

    // Check "Edit Profile" button is NOT present in modal
    const editBtn = modal.locator('button', { hasText: 'Edit Profile' });
    await expect(editBtn).toHaveCount(0);

    // Screenshot Modal
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '02_super_admin_detail_modal.png') });

    // Close Modal
    await modal.locator('button', { hasText: 'Close' }).first().click();
    await page.waitForTimeout(500);
  });

  test('Second Manager cannot deactivate or edit Super Admin, and UI locks all controls', async ({ page }) => {
    // 1. Authenticate as Second Manager (admin2@rtms.com)
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.locator('#login-email').waitFor({ state: 'visible' });
    await page.fill('#login-email', 'admin2@rtms.com');
    await page.fill('#login-password', 'admin123');
    await page.click('#login-submit');
    await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 15_000 });

    // 2. Navigate to /manager/accounts filtered for admin@rtms.com
    await page.goto('/manager/accounts?search=admin@rtms.com', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);

    // 3. Find Super Admin row (admin@rtms.com)
    const targetRow = page.locator('table tbody tr', { hasText: 'admin@rtms.com' });
    await expect(targetRow).toBeVisible({ timeout: 10_000 });

    // 4. Verify Protected indicator on Super Admin
    await expect(targetRow.locator('text=Protected').first()).toBeVisible();

    // 5. Verify the Action button is "Protected" and DISABLED
    const protectedActionBtn = targetRow.locator('button', { hasText: 'Protected' });
    await expect(protectedActionBtn).toBeVisible();
    await expect(protectedActionBtn).toBeDisabled();

    // Screenshot from Rogue/Second Admin perspective
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '03_second_manager_cannot_deactivate.png'), fullPage: true });

    // 6. Click View on Super Admin
    await targetRow.locator('button', { hasText: 'View' }).click();

    // 7. Verify modal has no Edit Profile button
    const modal = page.locator('[role="dialog"], .fixed');
    await expect(modal.first()).toBeVisible({ timeout: 5_000 });
    await expect(modal.locator('button', { hasText: 'Edit Profile' })).toHaveCount(0);
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '04_second_manager_cannot_edit_modal.png') });

    await modal.locator('button', { hasText: 'Close' }).first().click();
    await page.waitForTimeout(500);

    // 8. Now check a regular account (e.g. trainer1@rtms.com)
    await page.goto('/manager/accounts?search=trainer1@rtms.com', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    const regularRow = page.locator('table tbody tr', { hasText: 'trainer1@rtms.com' });
    if (await regularRow.count() > 0) {
      // Regular account does NOT have Protected badge
      await expect(regularRow.locator('text=Protected')).toHaveCount(0);
      // Regular account has Deactivate or Activate button ENABLED
      const regularActionBtn = regularRow.locator('td').last().locator('button').last();
      await expect(regularActionBtn).toBeEnabled();
    }
  });

  test('Security Audit Log records blocked unauthorized escalation attempts', async ({ page }) => {
    // 1. Authenticate as admin@rtms.com
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.locator('#login-email').waitFor({ state: 'visible' });
    await page.fill('#login-email', 'admin@rtms.com');
    await page.fill('#login-password', 'admin123');
    await page.click('#login-submit');
    await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 15_000 });

    // 2. Navigate to /manager/audit-log
    await page.goto('/manager/audit-log', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    // 3. Verify audit log contains blocked attempts
    const blockedDeactivate = page.locator('table tbody tr', { hasText: 'Super Admin Deactivate Blocked' });
    const blockedEdit = page.locator('table tbody tr', { hasText: 'Super Admin Edit Blocked' });

    await expect(blockedDeactivate.first()).toBeVisible({ timeout: 10_000 });
    await expect(blockedEdit.first()).toBeVisible({ timeout: 10_000 });

    // Screenshot Audit Log
    await page.screenshot({ path: path.join(EVIDENCE_DIR, '05_audit_log_security_events.png'), fullPage: true });
  });
});
