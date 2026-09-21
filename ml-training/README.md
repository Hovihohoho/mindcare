# Offline model training

This folder trains MindCare's internal perceived-stress model from PMData. Raw
data remains in `../training/` and is intentionally excluded from Git.

Run after Python 3.11 or newer is installed:

```powershell
cd ml-training
py -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python .\src\train_pmdata.py
```

The script:

1. Streams Fitbit JSON instead of loading the large files fully into memory.
2. Builds prior-day heart-rate, steps, sleep, resting-heart-rate and 3/7-day trend features.
3. Compares logistic regression, random forest and extra trees with
   leave-one-participant-out evaluation.
4. Preserves the paper-defined PMSys stress score from 1 to 5. Values outside
   that documented scale are excluded and counted in the report.
5. Exports the selected pipeline to Joblib and ONNX, then checks that Python
   and ONNX predict the same labels.

Generated files are kept local under `models/` and `reports/`. The current v3
run selected random forest from 1,680 samples across 16 participants. Its
leave-one-participant-out exact-score accuracy is 0.4018, macro F1 is 0.2155,
mean absolute error is 0.7054, and predictions within one point are 0.8970.
Scores 1 and 5 are rare and were not reliably recognized. This is an estimate
of a subjective sports-wellness score, not a clinical diagnosis.

Scientific basis: the PMData paper defines fatigue, sleep quality, soreness,
stress, and mood on a 1-5 scale; 3 is normal, 1-2 are below normal, and 4-5 are
above normal: https://doi.org/10.1145/3339825.3394926

## LifeSnaps preprocessing

Place the extracted LifeSnaps CSV exports under:

```text
../training/lifesnaps/raw/csv_rais_anonymized/
```

Then run:

```powershell
cd ml-training
.\.venv\Scripts\python.exe .\src\prepare_lifesnaps.py
```

The script preserves the raw files and creates local, Git-ignored CSV outputs
under `data/lifesnaps/`. It removes invalid physical values, converts Fitbit
sleep duration from milliseconds to minutes, collapses duplicate hourly keys,
adds 3/7-day history features, creates next-day sleep/steps/resting-heart-rate
targets, and makes deterministic 70/15/15 participant-level splits. A quality
report is written to `reports/lifesnaps-preprocessing.json`.

LifeSnaps source and paper: https://doi.org/10.5281/zenodo.6826682 and
https://doi.org/10.1038/s41597-022-01764-x

After reviewing the preprocessing report, train the three next-day regression
models with:

```powershell
cd ml-training
.\.venv\Scripts\python.exe .\src\train_lifesnaps.py
```

For each target, the training script compares median, current-day and 7-day
naive baselines with ridge regression, random forest and extra trees. It selects
the ML candidate with the lowest validation MAE, refits it on train plus
validation, evaluates the untouched test participants once, exports Joblib and
ONNX artifacts, and verifies Python/ONNX prediction parity. Results are written
to `reports/lifesnaps-metrics.json`; PMData's `metrics.json` is left unchanged.

The current v1 participant-held-out test results are:

| Next-day target | Selected model | MAE | R2 | Within tolerance |
| --- | --- | ---: | ---: | ---: |
| Sleep minutes | Random forest | 62.03 min | 0.2447 | 58.41% within 60 min |
| Steps | Ridge | 3,299 steps | 0.1462 | 42.14% within 2,000 steps |
| Resting heart rate | Random forest | 0.98 bpm | 0.9681 | 99.51% within 5 bpm |

All three models beat the best matching naive baseline by test MAE. Sleep and
steps remain relatively uncertain and should be presented as wellness forecasts,
not clinical conclusions. Resting-heart-rate performance is much stronger, but
still requires validation on MindCare users before production use.
