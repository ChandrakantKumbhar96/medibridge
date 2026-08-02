// Wraps calls to the Python chat service (and later the .NET notify
// service) so a slow/down upstream degrades gracefully instead of hanging
// gateway requests. Not used for springClient — Spring is the primary
// backend; an open circuit there would mean the whole gateway is down too.
const FAILURE_THRESHOLD = 5;
const COOLDOWN_MS = 30_000;

export function withCircuitBreaker(name, fn) {
  let failures = 0;
  let openedAt = null;

  return async (...args) => {
    if (openedAt && Date.now() - openedAt < COOLDOWN_MS) {
      throw new Error(`${name} circuit open`);
    }

    try {
      const result = await fn(...args);
      failures = 0;
      openedAt = null;
      return result;
    } catch (err) {
      failures += 1;
      if (failures >= FAILURE_THRESHOLD) {
        openedAt = Date.now();
      }
      throw err;
    }
  };
}
