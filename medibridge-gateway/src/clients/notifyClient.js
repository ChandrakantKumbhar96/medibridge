// Thin HTTP client to the .NET notify service, used by reports.routes.js.
import { env } from '../config/env.js';
import { withCircuitBreaker } from '../utils/circuitBreaker.js';

const TIMEOUT_MS = 15_000;

async function generate(reportType, rows) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(new URL(`/reports/${reportType}`, env.notifyServiceUrl), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rows }),
      signal: controller.signal,
    });
    if (!res.ok) {
      throw new Error(`notify-service /reports/${reportType} responded ${res.status}`);
    }
    return {
      buffer: Buffer.from(await res.arrayBuffer()),
      contentType: res.headers.get('content-type') ?? 'text/csv',
      contentDisposition: res.headers.get('content-disposition'),
    };
  } finally {
    clearTimeout(timeout);
  }
}

export const generateReport = withCircuitBreaker('notify-service:reports', generate);
