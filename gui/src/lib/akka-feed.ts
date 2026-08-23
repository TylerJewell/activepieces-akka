/**
 * Where this interface gets its data in the port.
 *
 * The application is activepieces' own, unchanged apart from this file and the two views that
 * import it (RENDERING.md R3). Two things are different about the port's side of the wire and
 * both are here:
 *
 *  - **A run arrives on an open connection, not by asking again.** The original re-fetches a run
 *    every fifteen seconds while it is unfinished. R1 requires a subscription instead, so both
 *    views open an `EventSource` and never issue a repeat request. The first frame is the run as
 *    it stands, which is what makes the reconnect path work without a second question: a client
 *    that dropped is told the current state rather than the changes it missed.
 *  - **There is no sign-in.** Authentication is outside the behavioural slice, so the interface is
 *    handed a signed-in session at boot rather than asked to obtain one.
 */
import { useEffect, useState } from 'react';

import { ApStorage } from './ap-browser-storage';

const PROJECT_ID = 'port-project';
const USER_ID = 'port-user';
const PLATFORM_ID = 'port-platform';

function base64Url(value: object): string {
  return btoa(JSON.stringify(value))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

/**
 * A session the interface can read. The claims are what `authentication-session` decodes; nothing
 * here is presented to the port's own endpoints as proof of anything, because they ask for none.
 */
export function seedSession(): void {
  const storage = ApStorage.getInstance();
  if (storage.getItem('token')) {
    return;
  }
  const claims = {
    id: USER_ID,
    type: 'USER',
    projectId: PROJECT_ID,
    platform: { id: PLATFORM_ID },
    tokenVersion: 'port',
    exp: Math.floor(Date.now() / 1000) + 60 * 60 * 24 * 365,
  };
  const token = [
    base64Url({ alg: 'none', typ: 'JWT' }),
    base64Url(claims),
    'port',
  ].join('.');
  storage.setItem('token', token);
  storage.setItem('projectId', PROJECT_ID);
}

/**
 * Subscribe to one endpoint and hand back the last frame it sent. `undefined` until the first
 * frame arrives, which is the loading state the callers already had.
 */
function useStream<T>(path: string | null): T | undefined {
  const [value, setValue] = useState<T | undefined>(undefined);
  useEffect(() => {
    if (path === null) {
      return;
    }
    setValue(undefined);
    const source = new EventSource(path);
    source.onmessage = (event) => setValue(JSON.parse(event.data) as T);
    return () => source.close();
  }, [path]);
  return value;
}

export function useRunStream<T>(runId: string | undefined): T | undefined {
  return useStream<T>(runId === undefined ? null : `/api/v1/flow-runs/${runId}/stream`);
}

export function useRunListStream<T>(): T | undefined {
  return useStream<T>('/api/v1/flow-runs/stream');
}
