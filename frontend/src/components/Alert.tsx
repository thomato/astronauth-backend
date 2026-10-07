import type { ReactNode } from 'react';

interface AlertProps {
  tone: 'danger' | 'success';
  children: ReactNode;
}

const tones = {
  danger: 'border-danger/40 bg-danger-surface text-danger',
  success: 'border-success/40 bg-success-surface text-success',
};

export function Alert({ tone, children }: AlertProps) {
  return (
    <p role="alert" className={`mb-5 rounded-lg border px-3 py-2 text-sm ${tones[tone]}`}>
      {children}
    </p>
  );
}
