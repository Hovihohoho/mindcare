# MindCare Web App Architecture

## Design inventory

The implementation maps the 26 user frames and 6 expert frames from the supplied
Figma files into route-level feature pages. Repeated navigation, footer, form,
card, status and profile patterns are implemented once and composed by features.

## Feature map

- User: `auth`, `home`, `assessment`, `emotion`, `expert-directory`, `booking`,
  `payment`, `ai-chat`, `bookmark`, `profile`, `settings`.
- Expert: `expert-dashboard`, `expert-calendar`, `expert-profile`,
  `client-record`, `expert-registration`.

## Import boundaries

1. A feature owns its `api`, `components`, `constants`, `hooks`, `pages`,
   `services`, `types` and `utils`.
2. Routes import features only through their public `index.ts`.
3. Cross-feature imports use only the destination feature's public `index.ts`.
4. Components shared by multiple features live in `src/shared/components`.
5. Layouts compose shared navigation and route outlets; they do not own domain UI.

## Dependency direction

```text
app/router
  -> layouts
  -> feature public APIs
       -> shared UI / API / lib
       -> feature-private modules
```

`booking` uses the booking-service contract and expert identifier. `payment`
receives checkout context from booking. `client-record` reuses only the public
emotion trend visualization. No feature imports another feature's private file.

## API integration

- Base URL: `VITE_API_URL` (defaults to `http://localhost:8079`).
- Auth: `/api/auth/**`.
- AI: `/api/ai/**`.
- Booking: `/api/v1/bookings`, `/api/v1/experts/**`.
- Emotion and assessment: `/api/v1/emotion-*`, `/api/v1/assessments/**`.
- `VITE_ENABLE_API_MOCK_FALLBACK=true` keeps booking/emotion screens usable until
  the gateway routes `/api/v1/**` to the corresponding services.
