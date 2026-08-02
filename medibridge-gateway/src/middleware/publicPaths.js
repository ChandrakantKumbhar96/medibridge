// Mirrors SecurityConfig.java's permitAll() list so the gateway doesn't
// reject requests Spring would happily serve unauthenticated. Keep in sync
// with SecurityConfig.java's requestMatchers(...).permitAll() block —
// currently: /auth/**, GET /specialties, GET /specializations, GET /policies.
const PUBLIC_GET_PREFIXES = ['/api/specialties', '/api/specializations', '/api/policies'];

export function isPublicPath(req) {
  if (req.method === 'OPTIONS') return true;

  const path = req.originalUrl.split('?')[0];
  if (path.startsWith('/api/auth/')) return true;
  if (req.method === 'GET' && PUBLIC_GET_PREFIXES.some((prefix) => path.startsWith(prefix))) {
    return true;
  }
  return false;
}
