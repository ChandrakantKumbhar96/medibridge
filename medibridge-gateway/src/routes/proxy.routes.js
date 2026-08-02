import { Router } from 'express';
import { createUpstreamProxy } from '../proxy/createProxy.js';
import { upstreams } from '../config/upstreams.js';
import { rateLimiter } from '../middleware/rateLimit.js';
import { requireAuth } from '../middleware/auth.js';

// Phase 2: rate-limited and JWT-gated ahead of the proxy (requireAuth skips
// the paths SecurityConfig.java marks permitAll — see publicPaths.js).
// Still pure passthrough otherwise: no rewriting, and the original
// Authorization header reaches Spring untouched, so Spring stays the
// authority on ownership/role. Everything under /api that isn't
// /api/chat or /api/reports lands here.
export const proxyRoutes = Router();

proxyRoutes.use(
  '/api',
  rateLimiter,
  requireAuth,
  createUpstreamProxy(upstreams.spring.target, { pathPrefix: upstreams.spring.pathPrefix }),
);
