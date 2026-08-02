// Thin HTTP client to the Python chat service, used by chat.routes.js and
// triage.routes.js.
import { env } from '../config/env.js';
import { withCircuitBreaker } from '../utils/circuitBreaker.js';

const TIMEOUT_MS = 15_000;

async function post(path, body) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(new URL(path, env.chatServiceUrl), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // Required: the chat service's require_internal_key rejects
        // anything without this matching its own INTERNAL_API_KEY.
        'X-Internal-Api-Key': env.internalApiKey ?? '',
      },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    if (!res.ok) {
      throw new Error(`chat-service ${path} responded ${res.status}`);
    }
    return res.json();
  } finally {
    clearTimeout(timeout);
  }
}

export const sendChatMessage = withCircuitBreaker('chat-service:message', (payload) =>
  post('/chat/message', payload),
);

export const sendTriageAnswer = withCircuitBreaker('chat-service:triage', (payload) =>
  post('/triage/answer', payload),
);
