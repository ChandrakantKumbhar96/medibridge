import express from 'express';
import { Router } from 'express';
import { searchDoctorsBySpecialty } from '../clients/springClient.js';
import { sendTriageAnswer } from '../clients/ragClient.js';
import { requireAuth } from '../middleware/auth.js';
import { rateLimiter } from '../middleware/rateLimit.js';

// Calls the Python chat service for a specialty recommendation, then
// Spring's doctor-search to merge in bookable doctors once a specialty is
// decided (needs_more_info: false, specialty set).
export const triageRoutes = Router();

triageRoutes.post('/api/triage/answer', rateLimiter, requireAuth, express.json(), async (req, res, next) => {
  try {
    const result = await sendTriageAnswer(req.body);

    if (result.specialty && !result.needs_more_info) {
      try {
        result.doctors = await searchDoctorsBySpecialty(result.specialty, req.headers.authorization);
      } catch {
        // A failed doctor lookup shouldn't hide a triage result the patient
        // already has — ship it without the doctors list.
        result.doctors = [];
      }
    }

    res.json(result);
  } catch (err) {
    next(err);
  }
});
