# Assessment scoring and benchmark policies

## Decision

- PHQ-9, GAD-7 and WHO-5 are supported public assessments.
- PSS-10 is supported for longitudinal tracking only; MindCare does not assign a clinical severity label.
- DASS-21 is `REVIEW_REQUIRED`: it is hidden from public reads, cannot be submitted, and cannot be published by the admin API.
- Scoring, interpretation and risk are separate concepts. A severity band never creates a risk alert by itself.
- PHQ-9 item 9 is an explicit, versioned risk signal. The risk workflow consumes that signal rather than inferring risk from the total score.
- Existing results remain unchanged. New policy and normalized-score fields are nullable and historical records are not recomputed.

## Deterministic policies

| Instrument | Score | Interpretation |
|---|---|---|
| PHQ-9 | Sum 9 answers, each 0..3 | 0..4 minimal, 5..9 mild, 10..14 moderate, 15..19 moderately severe, 20..27 severe |
| GAD-7 | Sum 7 answers, each 0..3 | 0..4 minimal, 5..9 mild, 10..14 moderate, 15..21 severe |
| WHO-5 | Raw sum 0..25; normalized score = raw x 4 | Below 50: low well-being; otherwise adequate well-being |
| PSS-10 | Reverse items 4, 5, 7 and 8, then sum | `TRACKING_ONLY`; no severity label |

The API persists the scoring-policy key/version and benchmark-policy key/version used for every new result. AI may explain a deterministic result, but it must not calculate or override it.

## Sources

- PHQ-9: Kroenke, Spitzer & Williams (2001), DOI `10.1046/j.1525-1497.2001.016009606.x`.
- GAD-7: Spitzer et al. (2006), DOI `10.1001/archinte.166.10.1092`.
- WHO-5: WHO official 2024 publication, `https://www.who.int/publications/m/item/WHO-UCN-MSD-MHE-2024.01`.
- PSS: Carnegie Mellon University scoring guide, `https://www.cmu.edu/dietrich/psychology/stress-immunity-disease-lab/scales/html/pssscoring.html`.
- DASS: UNSW DASS FAQ. Automated public interpretation remains disabled pending governance review.

## Database

Migration `V6__Assessment_Benchmark_Policies.sql` creates versioned scoring and benchmark policy tables, seeds the approved policies/bands, adds traceability fields to assessment results, and archives any currently published DASS-21 row.
