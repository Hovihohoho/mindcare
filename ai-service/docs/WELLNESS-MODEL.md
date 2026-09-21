# LifeSnaps wellness forecast model

## Runtime contract

`POST /api/ai/wellness-forecasts` accepts `lifesnaps-features-v1`: exactly 42
ordered feature names and 42 values. A missing value is JSON `null`; the Java
runtime converts it to IEEE `NaN` so the imputer exported inside each ONNX
pipeline can handle it. JSON `NaN` is deliberately not accepted because it is
not valid JSON.

At startup the runtime checks each metadata file's model version, exact feature
order and ONNX SHA-256 before opening the session. The three artifacts estimate
next-day sleep minutes, steps and resting heart rate. Outputs are bounded to
physical presentation ranges. They are consumer-wearable wellness forecasts,
not diagnoses; sleep and steps are reference-only because their held-out errors
remain substantial.

## Request

```json
{
  "featureVersion": "lifesnaps-features-v1",
  "featureNames": ["hr_mean", "... exact remaining order from metadata ..."],
  "features": [70.0, null, "... exactly 42 values ..."]
}
```

Obtain this body from `GET /api/v1/health-metrics/wellness-features`; clients
must pass `featureVersion`, `featureNames` and `features` without reordering.

## Response

```json
{
  "success": true,
  "message": "Next-day wellness indicators forecast",
  "data": {
    "sleepMinutesNextDay": 410,
    "stepsNextDay": 7200,
    "restingHeartRateNextDay": 68.0,
    "modelVersion": "lifesnaps-v1",
    "clinicalDiagnosis": false,
    "usageNotice": "Consumer-wearable wellness forecast; not a clinical diagnosis. Sleep and steps are estimates only."
  }
}
```

Direct service-to-service orchestration remains deferred until service identity
is defined. For now, an authenticated client calls the feature endpoint and then
passes its contract fields unchanged to the forecast endpoint through Gateway.
