# Frontend consolidation baseline

All frontend-related source now lives under `frontend/`.

## Layout

- `src/`: canonical React + TypeScript application.
- `public/`: canonical runtime assets.
- `legacy-app/`: previous JavaScript application retained for feature comparison,
  especially the Admin area and API integrations not yet ported.
- `design-reference/figmatocode/`: generated Figma reference screens.
- `design-reference/logo/`: original logo reference.

Only the package at `frontend/package.json` is started by `run-all.ps1`.
`legacy-app` and `design-reference` are not independent runtime applications.

## Verified baseline

- `npm run typecheck`: passes.
- `npm run lint`: passes.
- `npm run build`: passes.
- Gateway base URL: `http://localhost:8079`.

## Consolidation review result

The contract gaps found during consolidation have been addressed:

1. Auth roles, registration, verification, session storage and route guards now
   match the Auth Service.
2. Assessment and emotion journal DTOs and submissions match the Emotion Service.
3. Booking includes idempotency and uses real schedules and checkout responses.
4. Expert schedules, booking history, dashboard and account identity use live APIs.
5. Admin user and AI-document routes have been ported to the TypeScript app.
6. Runtime mock data and API mock fallback have been removed.

Backend work is still required before bookmark, expert application, expert-profile
editing and consent-based client records can be enabled. Their screens now show an
explicit unavailable state rather than fabricated or silently discarded data.

Dependency overrides reduced the audit findings from seven to two. The remaining
findings are in React Router's optional RSC/server-action behavior; this SPA does
not use those modes. No forced major downgrade was applied.
