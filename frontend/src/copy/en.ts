import type { RegistrationViolationCode } from '../graphql/generated/graphql';

/**
 * Every word the pages show. Components read it only through useCopy(), so a translation is one more file
 * of type Copy. The server sends codes and numbers, never text.
 */
export const en = {
  productName: 'Astronauth',
  common: {
    showPassword: 'Show password',
    hidePassword: 'Hide password',
    somethingWentWrong: 'Something went wrong. Check your connection and try again.',
    tryAgainIn: (seconds: number) => {
      const minutes = Math.ceil(seconds / 60);
      return `Too many attempts. Try again in ${minutes === 1 ? '1 minute' : `${minutes} minutes`}.`;
    },
    required: {
      email: 'Enter your email address.',
      password: 'Enter your password.',
    },
  },
  register: {
    title: 'Create your account',
    emailLabel: 'Email address',
    passwordLabel: 'Password',
    passwordHint: 'Use a long password. A few random words work well.',
    submit: 'Create account',
    submitting: 'Creating account…',
    violations: {
      EMAIL_INVALID: () => 'Enter an email address like name@example.com.',
      EMAIL_TOO_LONG: (limit) => `An email address can be at most ${limit} characters.`,
      PASSWORD_TOO_SHORT: (limit) => `Use at least ${limit} characters.`,
      PASSWORD_TOO_LONG: (limit) => `Use at most ${limit} characters.`,
    } satisfies Record<RegistrationViolationCode, (limit: number | null) => string>,
  },
  checkInbox: {
    title: 'Check your inbox',
    sentTo: 'We’ve sent a link to',
    nextStep: 'Open it to verify your email address. It works for 24 hours.',
    spam: 'If you can’t find it, check your spam folder.',
    differentAddress: 'Use a different address',
  },
  verifyEmail: {
    checking: 'Checking your link…',
    title: 'Verify your email address',
    verifying: 'Verify',
    forAddress: 'You’re verifying',
    passwordLabel: 'Password',
    passwordHint: 'Enter the password you chose when you registered.',
    submit: 'Verify email address',
    submitting: 'Verifying…',
    wrongPassword: (attemptsLeft: number) =>
      `That’s not the password you registered with. ${
        attemptsLeft === 1 ? '1 attempt left.' : `${attemptsLeft} attempts left.`
      }`,
    verifiedTitle: 'Your email address is verified',
    verifiedBody: (email: string) => `${email} is verified. You can close this page.`,
    deadTitle: 'This link no longer works',
    deadBody: 'Links work once and expire after 24 hours. Register again to get a new one.',
    exhaustedTitle: 'Too many wrong passwords',
    exhaustedBody:
      'This link can’t be used anymore. If you don’t remember the password, register again with a new one: you’ll get a new link for it.',
    registerAgain: 'Register again',
  },
  notFound: {
    title: 'Page not found',
    body: 'There’s nothing at this address.',
    register: 'Create an account',
  },
};

export type Copy = typeof en;
