# MindCare Web App

React 19 + Vite + TypeScript + Tailwind CSS application for MindCare users and
experts. The source is organized by feature and follows public barrel import
boundaries.

## Run locally

```bash
cp .env.example .env
npm install
npm run dev
```

The default gateway URL is `http://localhost:8079`.

## Quality checks

```bash
npm run lint
npm run typecheck
npm run build
```

## Main routes

- User: `/`, `/assessments`, `/emotion`, `/experts`, `/ai-chat`, `/profile`
- Expert: `/expert`, `/expert/calendar`, `/expert/profile`
- Expert onboarding: `/expert/register`

See [ARCHITECTURE.md](./ARCHITECTURE.md) for feature boundaries and API notes.
