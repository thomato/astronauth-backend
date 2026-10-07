import { ButtonLink } from '../components/Button';
import { Screen } from '../components/Screen';
import { useCopy } from '../copy/useCopy';

export function NotFoundPage() {
  const copy = useCopy();
  return (
    <Screen title={copy.notFound.title}>
      <p className="text-ink-muted mb-6">{copy.notFound.body}</p>
      <ButtonLink to="/register" variant="secondary">
        {copy.notFound.register}
      </ButtonLink>
    </Screen>
  );
}
