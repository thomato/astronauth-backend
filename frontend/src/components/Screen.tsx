import { useEffect, useRef, type ReactNode } from 'react';
import { useCopy } from '../copy/useCopy';

interface ScreenProps {
  title: string;
  children: ReactNode;
}

/**
 * One step of a flow. Its heading takes focus when it appears, so screen reader users hear that the step
 * changed even though the URL did not.
 */
export function Screen({ title, children }: ScreenProps) {
  const copy = useCopy();
  const heading = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    heading.current?.focus();
  }, [title]);

  return (
    <main className="flex min-h-dvh flex-col items-center px-4 py-10 sm:justify-center sm:py-16">
      <p className="text-ink-muted mb-6 flex items-center gap-2 text-sm font-semibold tracking-wide">
        <img src="/favicon.svg" alt="" className="size-6" />
        {copy.productName}
      </p>
      <section className="border-line bg-card w-full max-w-sm rounded-2xl border p-6 shadow-sm sm:p-8">
        <h1 ref={heading} tabIndex={-1} className="mb-6 text-2xl font-semibold tracking-tight outline-none">
          {title}
        </h1>
        {children}
      </section>
    </main>
  );
}
