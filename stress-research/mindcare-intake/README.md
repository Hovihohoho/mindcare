# Next MindCare stress experiment

This is an offline intake contract and validator, not an enabled collection feature.
No app prediction or automatic data export is added. The existing SSAQS result
remains `RESEARCH_ONLY_NOT_VALIDATED_FOR_MINDCARE` and its holdout remains consumed.

## Target and data agreement

The proposed target is the existing same-day MindCare `stressLevel` integer 1–5.
Do not convert SSAQS 0–100 labels into this scale or call it a PSS-10 score.
`protocol.json` freezes the initial feature set, split seed and primary metric.
Review the protocol before collecting real data; a schema-valid export is not an
approved research study or a validated model.

Before collection, implement an explicit opt-in screen explaining the research
purpose, which signals/check-ins are used, retention, access and withdrawal.
Record the consent version and timestamp independently from ordinary health sync
permission. A user who declines must retain ordinary app access. Withdrawal must
exclude all of that participant's rows from future research exports and training,
and follow the agreed deletion procedure for existing datasets/artifacts. This
validator checks supplied consent fields; it cannot establish that consent was
actually obtained. Consent collection/UI remains to be implemented once the
research procedure and wording are approved.

Use a random research participant UUID. Keep its account mapping separately with
restricted access. Never include names, emails, diary text or assessment answers.
Store private CSVs under the git-ignored `stress-research/data/` directory.

## CSV contract

Copy `template.csv`; it deliberately contains no invented patient observations.
All times require a UTC offset. `cutoff` is the scheduled check-in prompt, and
`answered_at` must be within the following six hours. The feature window is the
preceding 24 hours ending at cutoff. `features_available_at` is the latest source
availability time among included records, not the time a retrospective export was
written; source records arriving after cutoff are excluded.

Record `steps_24h`, independently measured `step_coverage_minutes` (1–1440), and
device/source provenance. Zero steps is valid only with observed coverage; missing
steps must remain missing and are rejected for this protocol. Do not infer wearing
time from the existence of a daily step total. If the current device integration
cannot establish coverage or availability times, collect that provenance first.
Heart-rate and sleep features are not included in this experiment.

Consent must precede the observation window. Nonempty `revoked_at`, missing/late
data, duplicated participant-cutoff pairs and wrong scales fail validation. Never
fill future or missing data just to make a row pass.

```powershell
python stress-research/validate_mindcare_intake.py stress-research/data/mindcare.csv --report stress-research/artifacts/intake-report.json
python -m unittest discover -s stress-research -p test_mindcare_intake.py
```

The report contains counts and row-level structural errors, no participant values.
Its `ready_for_protocol_review` flag does not indicate that there are enough people
or that the cohort represents the intended population.

## Frozen evaluation plan

1. Audit coverage, missingness, device sources and number of independent people.
   Agree the required sample size with the study owner before training.
2. Reserve 25% of new participants, seed 20260920. Keep every person's records on
   one side; fit imputation and scaling only inside development folds.
3. Pre-register a separate later-time cohort and its date boundary before tuning.
   Do not fabricate a boundary while no actual collection dates exist.
4. Compare mean/median baselines with candidate models using five participant-group
   folds; select by participant-macro MAE on the 1–5 scale.
5. Evaluate the selected model once on reserved people and once on the later cohort.
   Report error by coverage/device, and participant-bootstrap uncertainty.
6. Do not promote unless it improves on the preselected baseline and data governance,
   target compatibility and subgroup limitations have been reviewed. New data is
   required; the old SSAQS holdout cannot become an untouched test set again.
