import express from 'express';
import { Router } from 'express';
import { sendChatMessage } from '../clients/ragClient.js';
import { requireAuth } from '../middleware/auth.js';
import { rateLimiter } from '../middleware/rateLimit.js';

// Forwards to the Python chat service. Needs its own body parser — this
// isn't on the streaming proxy path, so it's safe here (see
// proxy/streaming.js for why the passthrough routes must stay parser-free).
export const chatRoutes = Router();

chatRoutes.post('/api/chat/message', rateLimiter, requireAuth, express.json(), async (req, res, next) => {
  try {
    // req.user comes from the verified JWT (see auth.js), never from the
    // request body - the chat-service must not be able to be told who's asking.
    const payload = { ...req.body, user_id: req.user.sub, role: req.user.type };
    res.json(await sendChatMessage(payload));
  } catch (err) {
    next(err);
  }
});
