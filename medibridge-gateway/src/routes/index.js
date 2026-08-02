import { Router } from 'express';
import { healthRoutes } from './health.routes.js';
import { chatRoutes } from './chat.routes.js';
import { triageRoutes } from './triage.routes.js';
import { reportsRoutes } from './reports.routes.js';
import { proxyRoutes } from './proxy.routes.js';

// chat/triage/reports must be registered before proxyRoutes' catch-all /api
// mount, or the proxy would swallow their requests and forward them to
// Spring, which has no such routes.
export const routes = Router();

routes.use(healthRoutes);
routes.use(chatRoutes);
routes.use(triageRoutes);
routes.use(reportsRoutes);
routes.use(proxyRoutes);
