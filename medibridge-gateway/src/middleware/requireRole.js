// Phase 5 — not yet wired. Spring already enforces role restriction on its
// own /admin, /doctor, /patient prefixes (SecurityConfig.java), so wiring
// this against proxy.routes.js today would just duplicate that check and
// risk drifting out of sync. This exists for the chat/triage/notify routes
// (Phase 5), which have no Spring-side role check of their own. Gate on
// presence of a role claim only; ownership (which record) is never decided
// here, that's Spring's job.
export function requireRole(...roles) {
  return (req, res, next) => {
    if (!roles.includes(req.user?.role)) {
      return res.status(403).json({ error: 'Forbidden' });
    }
    next();
  };
}
