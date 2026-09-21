# MindCare data rights

Authenticated users can export a machine-readable JSON copy of their account, wellbeing and AI conversation data. They can also permanently erase this data and their account.

## Deletion order

1. Verify the current password with `auth-service`.
2. Delete user-owned records from `emotion-service`.
3. Delete user-owned conversations from `ai-service`.
4. Permanently delete the auth account, sessions, tokens, notifications and bookmarks through database cascades.
5. Remove the locally stored avatar file after the auth transaction commits.

Emotion and AI deletion endpoints are idempotent. If a network interruption occurs, the client may safely repeat the full workflow. The auth account is deleted last so the user retains authorization to retry earlier steps.

## Endpoints

- `GET /api/auth/me/data-export`
- `POST /api/auth/me/verify-password`
- `DELETE /api/auth/me/permanent`
- `GET /api/v1/privacy/export`
- `DELETE /api/v1/privacy/data`
- `GET /api/ai/privacy/export`
- `DELETE /api/ai/privacy/data`

Exports deliberately omit password hashes, reset tokens, verification codes, hidden AI prompts and detector internals. Permanent deletion cannot be recovered.
