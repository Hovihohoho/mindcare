# LifeSnaps wellness feature contract

Status: implemented as `lifesnaps-features-v1`.

`GET /api/v1/health-metrics/wellness-features` is the canonical producer of the
42 ordered inputs used by the three LifeSnaps ONNX pipelines. The default date
is yesterday in `Asia/Ho_Chi_Minh`; callers may provide an IANA timezone and a
non-future date.

## Health Connect mapping

| LifeSnaps field | MindCare source | Canonical unit / rule |
| --- | --- | --- |
| `hr_mean` | `HEART_RATE` samples | bpm, daily mean |
| `hr_std/min/max/hour_count` | `HEART_RATE` samples | samples collapse to local-hour means first |
| `resting_hr` | `HEART_RATE` with `measurementContext=RESTING` | bpm, daily mean |
| `steps_total` | `STEP_COUNT` | count, daily sum |
| `calories_total` | `TOTAL_CALORIES_BURNED` when synchronized | kcal, daily sum |
| `distance_total` | `DISTANCE` when synchronized | metres, daily sum |
| `active_minutes` | `EXERCISE_SESSION` | session minutes, daily sum |
| `sleep_minutes` | `SLEEP_SESSION` stages, otherwise session duration; fallback `SLEEP_HOURS` | minutes |
| `sleep_duration_minutes` | longest `SLEEP_SESSION` | minutes |
| `sleep_awake_minutes` | awake sleep stages | minutes |
| `sleep_efficiency` | asleep/session duration | percent |

Health Connect currently does not provide a compatible daily value for
`lightly_active_minutes`, `moderately_active_minutes`, `very_active_minutes` or
`sedentary_minutes` in MindCare, so those values remain JSON `null`. Other
unavailable values also remain `null`; no zero is invented.

For the eight LifeSnaps trend sources, the 3-day window contains the feature
date and previous two calendar days with at least two available values. The
7-day window contains the feature date and previous six days with at least
three values. `delta_7d = current - mean_7d`. These rules reproduce the pandas
`rolling("3D")` and `rolling("7D")` feature construction used by
`prepare_lifesnaps.py`.

The response is a model input contract and does not constitute a clinical
assessment. Clients pass its version, names and values unchanged to AI Service.
