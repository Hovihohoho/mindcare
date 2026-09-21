# Offline stress feature contract

Status: accepted

## Decision

`emotion-service` is the canonical producer of health features for the offline
PMData stress model. Clients request the feature vector from
`GET /api/v1/health-metrics/stress-features`, then pass both `featureVersion`
and `features` unchanged to `POST /api/ai/stress-predictions`.

The current feature contract is `pmdata-features-v2` with 25 ordered values. Missing
Health Connect values remain JSON `null`; model-side median imputation handles
them. Model v3 predicts the paper-defined PMData/PMSys subjective stress score
from 1 to 5: 3 is normal, 1-2 are below normal, and 4-5 are above normal. It
must not be described as a clinical diagnosis.

## Rationale

Keeping aggregation in one server component prevents Android, iOS, and future
clients from implementing different date windows, sleep calculations, feature
ordering, or missing-value behavior. Version validation in `ai-service`
prevents a vector produced by an incompatible formula from reaching the model.

## Follow-up

Direct service-to-service orchestration remains deferred until the repository's
service identity and authorization decision is resolved. Until then, the
authenticated client performs the two API calls.
