# Offline stress model

Train the PMData model from the repository root as described in
`../../ml-training/README.md`, then enable local inference:

```powershell
$env:STRESS_MODEL_ENABLED='true'
$env:STRESS_MODEL_PATH='..\ml-training\models\stress-classifier-v3.onnx'
mvn.cmd spring-boot:run
```

`POST /api/ai/stress-predictions` requires `ROLE_USER`, requires
`featureVersion: pmdata-features-v2`, and accepts exactly 25 values in the
order below. Use JSON `null` when Health Connect did not provide a value; the
exported pipeline performs the same median imputation used while training. At
least one of the first 10 current-day values is required.

The canonical server-side producer is
`GET /api/v1/health-metrics/stress-features` in `emotion-service`. Its
`features` array can be passed unchanged to this prediction endpoint when
`featureVersion` is `pmdata-features-v2`.

1. `hr_mean`
2. `hr_std`
3. `hr_count`
4. `steps_total`
5. `sleep_minutes`
6. `sleep_efficiency`
7. `sleep_deep_minutes`
8. `sleep_rem_minutes`
9. `sleep_awake_minutes`
10. `resting_hr`
11. `hr_mean_mean_3d`
12. `hr_mean_mean_7d`
13. `hr_mean_delta_7d`
14. `steps_total_mean_3d`
15. `steps_total_mean_7d`
16. `steps_total_delta_7d`
17. `sleep_minutes_mean_3d`
18. `sleep_minutes_mean_7d`
19. `sleep_minutes_delta_7d`
20. `sleep_efficiency_mean_3d`
21. `sleep_efficiency_mean_7d`
22. `sleep_efficiency_delta_7d`
23. `resting_hr_mean_3d`
24. `resting_hr_mean_7d`
25. `resting_hr_delta_7d`

Example body:

```json
{
  "featureVersion": "pmdata-features-v2",
  "features": [72.5, 8.1, 620, 8400, 430, 92, 75, 96, 34, 64, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]
}
```

The response preserves the PMData/PMSys stress scale:

- `stressScore`: integer from 1 to 5.
- `relativeLevel`: `BELOW_NORMAL` for 1-2, `NORMAL` for 3, and
  `ABOVE_NORMAL` for 4-5.
- `scaleMinimum` and `scaleMaximum`: 1 and 5.
- `confidence`: probability assigned to the predicted score.

Example response data:

```json
{
  "stressScore": 4,
  "relativeLevel": "ABOVE_NORMAL",
  "scaleMinimum": 1,
  "scaleMaximum": 5,
  "confidence": 0.52,
  "modelVersion": "stress-classifier-v3",
  "usageNotice": "Estimate of the PMData self-reported wellness score; not a clinical diagnosis."
}
```

The PMData paper defines 3 as normal, 1-2 as below normal, and 4-5 as above
normal. The score is a subjective sports-wellness measure and must not be
presented as a medical diagnosis. Source:
https://doi.org/10.1145/3339825.3394926
