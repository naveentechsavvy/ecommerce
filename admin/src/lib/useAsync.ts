import { useEffect, useState, type DependencyList } from 'react';

/** Minimal data-fetch hook; ignores responses from stale requests. */
export function useAsync<T>(fn: () => Promise<T>, deps: DependencyList) {
  const [data, setData] = useState<T | undefined>();
  const [error, setError] = useState<Error | undefined>();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let live = true;
    setLoading(true);
    setError(undefined);
    fn()
      .then((d) => live && setData(d))
      .catch((e) => live && setError(e))
      .finally(() => live && setLoading(false));
    return () => {
      live = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, error, loading, setData };
}
