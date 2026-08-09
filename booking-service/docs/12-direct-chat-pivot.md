# Direct expert chat pivot

## Decision

From 2026-08-09, the public product flow keeps direct chat between a user and an
eligible expert. Scheduling, booking, payment, review and booking-gated client
records are removed from the web application and from public Gateway routing.

## API

- `POST /api/v1/conversations` with `{ "expertUserId": "<uuid>" }` starts or
  returns the single active conversation for that user/expert pair.
- `GET /api/v1/conversations` lists conversations for the authenticated actor.
- `GET /api/v1/conversations/{id}` reads participant-safe conversation metadata.
- Message history, send and mark-read endpoints retain their existing paths.

Identity always comes from the verified security context. Only a user may start a
conversation, and the target expert must be eligible according to Auth Service.

## Migration and rollback

Migration V3 backfills `user_id` and `expert_user_id` from historical bookings,
makes `booking_id` nullable, and changes uniqueness to the active participant
pair. It deliberately retains the legacy booking reference and tables so existing
chat history is not destroyed and rollback remains possible. Legacy booking and
payment controllers are no longer exposed by the API Gateway.
