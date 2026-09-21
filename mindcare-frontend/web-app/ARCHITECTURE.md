# MindCare web architecture

The React application is organized by feature. User-facing capabilities include
authentication, assessments, emotion journals, Health Connect data, AI chat,
bookmarks, profile, settings, and notifications. Health Connect data is read-only
on web; native permissions and synchronization remain owned by the Android app.
Administrative capabilities include user management, assessment content, AI
knowledge documents, notifications, and audit logs.

All HTTP and WebSocket traffic goes through the API Gateway configured by
`VITE_API_URL` (default `http://localhost:8079`). The application supports only
`ROLE_USER` and `ROLE_ADMIN`.

The former expert, consultation, booking, and payment capabilities have been
removed and must not be referenced by new UI or API code.
