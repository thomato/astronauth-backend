import { expect, test, type APIRequestContext } from '@playwright/test';

const MAILPIT = 'http://localhost:8025';

/** Reads the Verification link from the email Mailpit caught, as the person would from their inbox. */
async function verificationLinkSentTo(request: APIRequestContext, email: string): Promise<string> {
  let link = '';
  await expect
    .poll(
      async () => {
        const search = await request.get(`${MAILPIT}/api/v1/search`, { params: { query: `to:"${email}"` } });
        const [latest] = (await search.json()).messages as { ID: string }[];
        if (!latest) return '';
        const message = await (await request.get(`${MAILPIT}/api/v1/message/${latest.ID}`)).json();
        link = /http\S+\/verify-email\?token=\S+/.exec(message.Text as string)?.[0] ?? '';
        return link;
      },
      { timeout: 15_000 },
    )
    .not.toBe('');
  return link;
}

test('a person registers, verifies, lands signed in, signs out and signs back in', async ({ page, request }) => {
  const email = `e2e-${Date.now()}@example.com`;
  const password = 'correct horse battery staple';

  await page.goto('/register');
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Create account' }).click();

  await expect(page.getByRole('heading', { name: 'Check your inbox' })).toBeVisible();
  await expect(page.getByText(email)).toBeVisible();

  await page.goto(await verificationLinkSentTo(request, email));

  await expect(page.getByText(email)).toBeVisible();
  await expect(page).toHaveURL(/\/verify-email$/);
  await page.getByLabel('Password').fill('not the password');
  await page.getByRole('button', { name: 'Verify email address' }).click();
  await expect(page.getByRole('alert')).toContainText('4 attempts left.');

  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Verify email address' }).click();

  // Verification proved the address and the Credential, so it signs the person in (ADR 0006)
  await expect(page).toHaveURL(/\/account$/);
  await expect(page.getByRole('heading', { name: 'You’re signed in' })).toBeVisible();
  await expect(page.getByText(email)).toBeVisible();

  await page.getByRole('button', { name: 'Sign out' }).click();
  await expect(page).toHaveURL(/\/sign-in$/);

  // The Account page is guarded by Spring, not by the app, so asking for it again bounces to sign-in
  await page.goto('/account');
  await expect(page).toHaveURL(/\/sign-in$/);

  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password').fill('not the password');
  await page.getByRole('button', { name: 'Sign in' }).click();
  // Says nothing about whether the address has an Account (ADR 0003)
  await expect(page.getByRole('alert')).toContainText('don’t go together');

  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/account$/);
  await expect(page.getByText(email)).toBeVisible();
});

test('an unverified Account is sent back to register for a fresh link', async ({ page }) => {
  const email = `e2e-unverified-${Date.now()}@example.com`;
  const password = 'correct horse battery staple';

  await page.goto('/register');
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.getByRole('heading', { name: 'Check your inbox' })).toBeVisible();

  await page.goto('/sign-in');
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page.getByRole('heading', { name: 'Verify your email address first' })).toBeVisible();
  await page.getByRole('link', { name: 'Send a new link' }).click();
  await expect(page.getByLabel('Email address')).toHaveValue(email);
});

test('broken rules are shown at their fields', async ({ page }) => {
  await page.goto('/register');
  await page.getByLabel('Email address').fill('not-an-address');
  await page.getByLabel('Password').fill('short');
  await page.getByRole('button', { name: 'Create account' }).click();

  await expect(page.getByText('Enter an email address like name@example.com.')).toBeVisible();
  await expect(page.getByText('Use at least 12 characters.')).toBeVisible();
});
