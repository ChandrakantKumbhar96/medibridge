import express from 'express';
import { Router } from 'express';
import { generateReport } from '../clients/notifyClient.js';
import { requireAuth } from '../middleware/auth.js';
import { rateLimiter } from '../middleware/rateLimit.js';

// Forwards to the .NET notify service's CSV export. Needs its own body
// parser, same reason as chat.routes.js: this isn't on the streaming proxy
// path (see proxy/streaming.js).
export const reportsRoutes = Router();

reportsRoutes.post('/api/reports/:reportType', rateLimiter, requireAuth, express.json(), async (req, res, next) => {
  try {
    const { buffer, contentType, contentDisposition } = await generateReport(req.params.reportType, req.body.rows);
    res.set('Content-Type', contentType);
    if (contentDisposition) res.set('Content-Disposition', contentDisposition);
    res.send(buffer);
  } catch (err) {
    next(err);
  }
});
