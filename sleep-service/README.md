# MindCare sleep prediction

Public API: `POST http://localhost:8079/api/ai/sleep/predict` with
`Authorization: Bearer <access token>`. The gateway routes to the authenticated
Spring AI service, which calls this internal Python service on port 8085.
No database migration is needed; requests and predictions are not persisted.

## Install the trained artifact

The supplied Kaggle artifact records scikit-learn **1.6.1**. Install
`requirements-model.txt` for this artifact; update that pin when replacing it
with a model trained using another version. From the repository root, run
`./sleep-service/start.ps1` to start the prepared local environment.

Copy your trusted Kaggle `sleep_forecast.joblib` to `sleep-service/models/`.
Do not load joblib files from untrusted sources: deserialization executes code.
The artifact is ignored by Git and is never accepted through an upload endpoint.

In the original Kaggle notebook run `print(sklearn.__version__)`. Use that exact
version below. The service rejects mismatched versions, feature schemas and targets.
For reproducible deployment retain `pip freeze` from training as well; Python,
NumPy, pandas and joblib versions should match the training environment.

### Docker (PowerShell, from repository root)

```powershell
$env:SKLEARN_VERSION = '<version printed in Kaggle>'
docker compose -f sleep-service/compose.yml up --build -d
Invoke-RestMethod http://127.0.0.1:8085/health
```

### Local Python (alternative, Python 3.11)

```powershell
cd sleep-service
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt 'scikit-learn==<Kaggle version>'
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 8085
```

Start/restart the existing API Gateway and AI service normally. The default
inference URL is `http://127.0.0.1:8085`; override `SLEEP_INFERENCE_URL` for a private
container network. Keep port 8085 private. Restart inference after replacing a model.
Missing, incompatible or broken models produce HTTP 503, never fabricated predictions.

## Request

```json
{
  "date": "2016-05-06",
  "totalSteps": 10000,
  "veryActiveMinutes": 20,
  "fairlyActiveMinutes": 15,
  "lightlyActiveMinutes": 200,
  "sedentaryMinutes": 700,
  "totalMinutesAsleep": 420
}
```

`date` is the user's local calendar date D, with finalized daily measurements
available at prediction time. Output `targetDate` is D+1. Minutes are integer daily
totals, steps are a nonnegative integer, and sleep must be present (1–1440).
Never replace missing sleep with zero. Day of week is derived server-side with
Monday=0, matching training. The seven feature names and their order exactly
match the Kaggle notebook. Do not send a user ID or manually computed weekday.

The existing `ApiResponse` envelope contains `date`, `targetDate`,
`predictedSleepMinutes`, and `modelType` in `data`. Predictions are minutes, not a
diagnosis or sleep quality score. Invalid input is rejected; unavailable inference
returns 503 through Spring. The Kaggle test MAE of 71.08 minutes is an aggregate
evaluation result, not an individual confidence interval.

Example call after setting your access token locally:

```powershell
$body = @{
  date = '2016-05-06'; totalSteps = 10000; veryActiveMinutes = 20
  fairlyActiveMinutes = 15; lightlyActiveMinutes = 200
  sedentaryMinutes = 700; totalMinutesAsleep = 420
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8079/api/ai/sleep/predict' -Headers @{Authorization = "Bearer $env:MINDCARE_ACCESS_TOKEN"} -ContentType 'application/json' -Body $body
```

Tests use a synthetic forest to verify serving behavior, not the Kaggle model's accuracy.
