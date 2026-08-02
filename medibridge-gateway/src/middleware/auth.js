// Verifies the Spring-issued JWT (HS384 — do not hardcode HS256) and rejects
// missing/invalid tokens before the proxy forwards to Spring. Must NOT
// replace the Authorization header or inject a trusted X-User-Id — the
// original header is forwarded to Spring untouched so ownership/role checks
// stay authoritative there. This is a cheap early reject, not a replacement
// for Spring's own SecurityConfig checks.
import jwt from 'jsonwebtoken';
import { env } from '../config/env.js';
import { isPublicPath } from './publicPaths.js';

export function requireAuth(req, res, next) {
  if (isPublicPath(req)) return next();

  const header = req.headers.authorization;
  if (!header?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing bearer token' });
  }
  try {
    req.user = jwt.verify(header.slice(7), env.jwtSecret);
    next();
  } catch {
    res.status(401).json({ error: 'Invalid or expired token' });
  }
}
