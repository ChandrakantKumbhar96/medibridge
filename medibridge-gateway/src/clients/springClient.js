// Used by triage.routes.js to call Spring's doctor-search after the chat
// service returns a specialty recommendation. Forwards the caller's own
// Authorization header — never mints or substitutes a trusted one, so
// ownership/role checks stay authoritative in Spring.
import { env } from '../config/env.js';

export async function searchDoctorsBySpecialty(specialization, authorizationHeader) {
  const url = new URL('/api/doctors', env.springApiUrl);
  url.searchParams.set('specialization', specialization);

  const res = await fetch(url, {
    headers: authorizationHeader ? { Authorization: authorizationHeader } : {},
  });

  if (!res.ok) {
    throw new Error(`Spring /doctors responded ${res.status}`);
  }
  return res.json();
}
