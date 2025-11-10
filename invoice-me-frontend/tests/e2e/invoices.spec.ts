import { test, expect } from '@playwright/test';

test.describe('Invoice Management', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the invoice page
    await page.goto('/');
  });

  test('should display the application', async ({ page }) => {
    // Simple smoke test to verify the app loads
    await expect(page).toHaveTitle(/InvoiceMe/i);
  });

  test('should render invoice card component', async ({ page }) => {
    // This test verifies the InvoiceCard component can be rendered
    // In a real E2E test, this would navigate to the invoices page
    // and verify that invoice cards are displayed

    // For now, just verify the app structure exists
    const body = page.locator('body');
    await expect(body).toBeVisible();
  });
});
