# Stress prediction research

Next MindCare experiment: [intake contract, consent requirements and frozen evaluation plan](mindcare-intake/README.md).

Initial benchmark completed on 2026-09-16. **The trained candidate did not beat
the median baseline on MAE. Do not connect this artifact to the MindCare app.**

This directory is an offline experiment, separate from the sleep inference service.
No public API, mobile behavior, questionnaire scoring, or database schema is changed.

## Data and target

Use [SSAQS v1](https://zenodo.org/records/18706837), described in the authors'
[Scientific Data paper](https://www.nature.com/articles/s41597-026-07085-7).
The target is the participant's daily self-reported stress on a 0–100 scale.
It is not a PSS-10 score, diagnostic category, Fitbit stress score, or next-day prediction.

The download is verified against the publisher's MD5 and its SHA-256 is recorded
in the result. Raw data, row-level predictions and model binaries are git-ignored.
Keep the authors' README with the data. Check the dataset's reuse terms before
redistributing it or shipping a derived model; the landing page's parsed license
metadata does not clearly identify a reuse license. Article licensing alone does
not establish the dataset license.

## Experiment

- Compare median/mean dummy regressors with Ridge and Random Forest.
- Compare step-count features alone with step counts plus the latest deep-sleep duration.
- Exclude anxiety answers, Fitbit stress/sleep scores, HRV, oxygen, subject ID,
  university and course from model inputs.
- Step features cover the 24 hours before the scheduled questionnaire, subdivided
  into recent 6 hours, preceding 6 hours and older 12 hours. Minute records must
  finish by the cutoff. The overall 24-hour total is also included.
- Require a valid label and an answer starting within six hours after the scheduled
  prompt. Reject duplicate prompts. Drop rows with no step observations; retain
  missing sleep as missing. Imputation and scaling are fitted inside training folds.
- Use only sleep records timestamped before the cutoff and within the preceding
  36 hours. Never read subsequent sleep into an earlier prediction.
- Hold out 25% of participants using seed `20260916`. Use five participant-disjoint
  development folds to choose the feature set and candidate by participant-macro MAE.
- Evaluate the selected candidate and both baselines once on held-out participants.
  Each participant contributes equally to macro MAE, irrespective of their row count.
  The candidate is compared even when the development baseline is better, to document
  a negative result; this does not constitute model promotion.
- Calculate an exploratory paired bootstrap interval by resampling held-out participants,
  not individual correlated daily observations.

## Results

The actual run produced 2,237 eligible questionnaire observations from 32 participants.
Development: 1,618 observations from 24 people. Holdout: 619 observations from 8 people.
Of 3,133 questionnaire records, 24 were invalid/late and 872 lacked preceding step data.
Deep-sleep duration was missing in 30.26% of eligible observations.

Ridge using only step features was selected on development data. Its development
macro MAE was 22.57, versus 22.24 for the median baseline.

| Held-out estimator | MAE (0–100 points) | Participant-macro MAE | RMSE | R² |
|---|---:|---:|---:|---:|
| Training-label median | **16.78** | **15.78** | 23.08 | -0.015 |
| Training-label mean | 18.36 | 17.33 | 23.15 | -0.021 |
| Selected Ridge candidate | 18.00 | 16.97 | **22.66** | 0.021 |

The candidate-minus-median macro MAE difference was +1.20 points (worse), with an
exploratory 95% participant-bootstrap interval of [-0.51, 2.81]. The RMSE improvement
does not overturn the preselected MAE criterion. This is an initial benchmark result,
not a published state-of-the-art comparison or evidence that all stress models fail.

Full metrics, environment versions and partition membership: [benchmark.json](benchmark.json).
The local candidate artifact is `artifacts/stress_research.joblib`; it is fitted only
on development participants and marked `RESEARCH_ONLY_NOT_VALIDATED_FOR_MINDCARE`.

## Limits and app compatibility

- SSAQS has no ordinary heart-rate series in its provided files. This experiment
  therefore does not evaluate MindCare's heart-rate features.
- Steps map conceptually to the app's step records, but source devices, aggregation
  intervals and coverage can differ. A sparse step export cannot distinguish every
  unworn interval from inactivity. Partial windows may undercount activity; sums over
  subwindows without rows are zero, not proof of zero physical activity.
- The deep-sleep variant would require reliable sleep-stage records and matching
  aggregation in MindCare; total sleep duration cannot substitute for deep sleep.
- Sensor timestamps ending in `Z` are interpreted as UTC, as encoded. Sleep timestamps
  are assumed to describe completed sleep. Confirm these semantics with the data
  producer before making deployment claims. The selected steps-only candidate does
  not depend on sleep timestamp semantics.
- Different devices and populations, missing measurements and only eight held-out
  people limit generalization. The interval is exploratory and is not an individual
  prediction confidence interval.
- The held-out set is now consumed. Do not repeatedly tune against it and continue
  calling it an untouched test set.

Next experiment: collect opt-in, timestamped health signals paired with MindCare's
own daily stress check-ins, establish the intended target and scale, and evaluate on
new participants and later time periods. MindCare's existing 1–5 check-in must not be
silently treated as equivalent to SSAQS's 0–100 target.

## Reproduce

From the repository root, with Python 3.11:

```powershell
python -m venv stress-research/.venv
./stress-research/.venv/Scripts/python.exe -m pip install -r stress-research/requirements.txt
./stress-research/.venv/Scripts/python.exe stress-research/fetch_dataset.py
./stress-research/.venv/Scripts/python.exe -m unittest discover -s stress-research -p test_protocol.py
./stress-research/.venv/Scripts/python.exe stress-research/train.py
```

The initial run used the already installed Python environment under `sleep-service/.venv`
without modifying it. Exact imported versions are recorded in the benchmark JSON.
New runs write `artifacts/benchmark.json`; the root benchmark JSON is the recorded
initial result. The unit tests exercise temporal cutoffs and missing-data behavior;
their synthetic fixtures are never used to train or report model quality.
