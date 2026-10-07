import { useId, useState, type InputHTMLAttributes } from 'react';
import { useCopy } from '../copy/useCopy';

interface FieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange' | 'value'> {
  label: string;
  value: string;
  onChange: (value: string) => void;
  hint?: string;
  errors?: string[];
}

const input =
  'block w-full rounded-lg border bg-card px-3 py-2.5 text-base text-ink placeholder:text-ink-muted ' +
  'focus:outline-2 focus:outline-offset-1 focus:outline-focus sm:text-sm';

/** A labelled input whose hint and errors are announced with it. */
export function TextField({ label, value, onChange, hint, errors = [], type = 'text', ...props }: FieldProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const invalid = errors.length > 0;
  const describedBy = [invalid ? errorId : undefined, hint ? hintId : undefined].filter(Boolean).join(' ');

  return (
    <div className="mb-5">
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={invalid || undefined}
        aria-describedby={describedBy || undefined}
        className={`${input} ${invalid ? 'border-danger' : 'border-line'}`}
        {...props}
      />
      {invalid && (
        <ul id={errorId} className="text-danger mt-1.5 text-sm">
          {errors.map((error) => (
            <li key={error}>{error}</li>
          ))}
        </ul>
      )}
      {hint && (
        <p id={hintId} className="text-ink-muted mt-1.5 text-sm">
          {hint}
        </p>
      )}
    </div>
  );
}

/** Instead of a confirmation field: showing the password catches typos while they are being made. */
export function PasswordField(props: Omit<FieldProps, 'type'>) {
  const copy = useCopy();
  const [visible, setVisible] = useState(false);

  return (
    <div className="relative">
      <TextField {...props} type={visible ? 'text' : 'password'} spellCheck={false} autoCapitalize="off" />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        aria-pressed={visible}
        className="text-accent hover:text-accent-strong focus-visible:outline-focus absolute top-0 right-0 text-sm font-medium focus-visible:outline-2"
      >
        {visible ? copy.common.hidePassword : copy.common.showPassword}
      </button>
    </div>
  );
}
