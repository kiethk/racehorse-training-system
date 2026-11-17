const { test, expect } = require('@playwright/test');

const ROLES = [
  {
    role: 'CLUB_MANAGER',
    email: 'admin@rtms.com',
    expectedColorHex: '#6657c7',
    routes: [
      '/manager',
      '/manager/analytics',
      '/manager/admissions',
      '/manager/horses',
      '/manager/stable',
      '/manager/accounts',
      '/manager/access-control',
      '/manager/audit-log',
    ],
  },
  {
    role: 'HEAD_TRAINER',
    email: 'trainer1@rtms.com',
    expectedColorHex: '#2563eb',
    routes: [
      '/trainer',
      '/trainer/groups',
      '/trainer/courses',
      '/trainer/course-runs',
      '/trainer/schedule',
      '/trainer/incidents',
      '/trainer/admissions',
      '/trainer/stable',
      '/trainer/horses',
    ],
  },
  {
    role: 'VETERINARIAN',
    email: 'vet1@rtms.com',
    expectedColorHex: '#0d9488',
    routes: [
      '/veterinarian',
      '/veterinarian/urgent',
      '/veterinarian/schedule',
      '/veterinarian/admissions',
      '/veterinarian/health',
      '/veterinarian/routine',
      '/veterinarian/preventive-care',
    ],
  },
  {
    role: 'GROOM',
    email: 'groom1@rtms.com',
    expectedColorHex: '#d97706',
    routes: [
      '/groom',
      '/groom/tasks',
      '/groom/schedule',
      '/groom/incidents',
      '/groom/admissions',
      '/groom/horse-edit-requests',
      '/groom/stable',
    ],
  },
  {
    role: 'HORSE_OWNER',
    email: 'owner@rtms.com',
    expectedColorHex: '#059669',
    routes: [
      '/owner',
      '/owner/horses',
      '/owner/schedule',
      '/owner/admissions',
    ],
  },
];

for (const config of ROLES) {
  test.describe(`Role Theme Sweep: ${config.role}`, () => {
    test(`Verify theme colors, zero runtime errors, and no layout overflow across all routes`, async ({
      page,
    }) => {
      const consoleErrors = [];
      const pageErrors = [];

      page.on('console', (msg) => {
        if (msg.type() === 'error') {
          const text = msg.text();
          // Filter out benign favicon or noise if any
          if (!text.includes('favicon.ico')) {
            consoleErrors.push(text);
          }
        }
      });

      page.on('pageerror', (err) => {
        pageErrors.push(err.message);
      });

      // Login
      await page.goto('/login', { waitUntil: 'domcontentloaded' });
      await page.locator('#login-email').waitFor({ state: 'visible' });
      await page.fill('#login-email', config.email);
      await page.fill('#login-password', 'admin123');
      await page.click('#login-submit');
      await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 15_000 });
      await page.waitForLoadState('domcontentloaded');

      // Verify each route
      for (const route of config.routes) {
        await page.goto(route, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(500);

        // 1. Verify AppShell has data-role attribute
        const appShellRole = await page.getAttribute('div[data-role]', 'data-role');
        expect(appShellRole).toBe(config.role);

        // 2. Verify computed CSS variable --color-primary
        const primaryColor = await page.evaluate(() => {
          const el = document.querySelector('div[data-role]');
          if (!el) return null;
          return getComputedStyle(el).getPropertyValue('--color-primary').trim();
        });
        expect(primaryColor.toLowerCase()).toBe(
          config.role === 'CLUB_MANAGER' ? '#6657c7' :
          config.role === 'HEAD_TRAINER' ? '#2563eb' :
          config.role === 'VETERINARIAN' ? '#0d9488' :
          config.role === 'GROOM' ? '#d97706' : '#059669'
        );

        // 3. Verify horizontal layout: no unwanted horizontal overflow
        const hasHorizontalScroll = await page.evaluate(() => {
          return document.documentElement.scrollWidth > window.innerWidth + 2;
        });
        expect(hasHorizontalScroll).toBe(false);
      }

      // 4. Assert zero fatal page errors across all visited pages
      expect(pageErrors).toEqual([]);
    });
  });
}
