"""Exploratory SSAQS stress regression, with participant-disjoint evaluation.

Estimate a day's self-reported stress at the scheduled evening questionnaire.
This is not a next-day forecast, PSS-10 score, or diagnostic classifier.
"""
import hashlib
import json
from pathlib import Path
import platform

import joblib
import numpy as np
import pandas as pd
import sklearn
from sklearn.base import clone
from sklearn.dummy import DummyRegressor
from sklearn.ensemble import RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import GroupKFold, GroupShuffleSplit
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

ROOT = Path(__file__).resolve().parent
SEED = 20260916
STEP_FEATURES = ['steps_24h', 'steps_recent_6h', 'steps_previous_6h', 'steps_older_12h']
FEATURE_SETS = {'steps_only': STEP_FEATURES,
                'steps_and_deep_sleep': STEP_FEATURES + ['deep_sleep_minutes']}


def features_before(steps, sleep, cutoff):
    """Strictly preceding windows; missing steps are not treated as zero activity.

    Minute timestamps are conservatively treated as interval starts, so a minute
    must finish by cutoff. Sleep timestamps are assumed to denote completed sleep.
    """
    end = steps['timestamp'] + pd.Timedelta(minutes=1)
    window = steps[(steps['timestamp'] >= cutoff - pd.Timedelta(hours=24)) & (end <= cutoff)]
    if window.empty:
        return None
    recent = window['timestamp'] >= cutoff - pd.Timedelta(hours=6)
    middle = ((window['timestamp'] >= cutoff - pd.Timedelta(hours=12)) & ~recent)
    previous_sleep = sleep[(sleep['timestamp'] <= cutoff)
                           & (sleep['timestamp'] > cutoff - pd.Timedelta(hours=36))]
    return {
        'steps_24h': float(window.steps.sum()),
        'steps_recent_6h': float(window.loc[recent, 'steps'].sum()),
        'steps_previous_6h': float(window.loc[middle, 'steps'].sum()),
        'steps_older_12h': float(window.loc[~(recent | middle), 'steps'].sum()),
        'deep_sleep_minutes': (float(previous_sleep.iloc[-1].deep_sleep_in_minutes)
                               if not previous_sleep.empty else np.nan),
    }


def read_sensor(path, value, upper):
    if not path.exists():
        return pd.DataFrame({'timestamp': pd.Series([], dtype='datetime64[ns, UTC]'),
                             value: pd.Series([], dtype=float)})
    frame = pd.read_csv(path)
    frame['timestamp'] = pd.to_datetime(frame.timestamp, utc=True, errors='coerce')
    frame[value] = pd.to_numeric(frame[value], errors='coerce')
    frame = frame.dropna(subset=['timestamp', value])
    frame = frame[frame[value].between(0, upper)].drop_duplicates()
    # Reject ambiguous duplicated timestamps instead of summing overlapping exports.
    if frame.timestamp.duplicated().any():
        raise ValueError(f'Conflicting timestamps: {path}')
    return frame.sort_values('timestamp')


def build_dataset(directory):
    rows, audit = [], {'questionnaires': 0, 'invalid_or_late_labels': 0,
                      'no_steps_in_window': 0, 'duplicate_questionnaires': 0}
    for folder in sorted(directory.iterdir(), key=lambda p: p.name):
        if not folder.is_dir() or not (folder / 'daily_questions.csv').exists():
            continue
        steps = read_sensor(folder / 'steps.csv', 'steps', 1000)
        sleep = read_sensor(folder / 'sleep.csv', 'deep_sleep_in_minutes', 1440)
        labels = pd.read_csv(folder / 'daily_questions.csv')
        audit['questionnaires'] += len(labels)
        duplicated = labels.duplicated('timeStampScheduled', keep=False)
        audit['duplicate_questionnaires'] += int(duplicated.sum())
        for row in labels.loc[~duplicated].itertuples(index=False):
            stress = pd.to_numeric(row.stress, errors='coerce')
            cutoff = pd.to_datetime(row.timeStampScheduled, unit='s', utc=True, errors='coerce')
            answered = pd.to_datetime(row.timeStampStart, unit='s', utc=True, errors='coerce')
            if (not np.isfinite(stress) or not 0 <= stress <= 100 or pd.isna(cutoff)
                    or pd.isna(answered) or not cutoff <= answered <= cutoff + pd.Timedelta(hours=6)):
                audit['invalid_or_late_labels'] += 1
                continue
            features = features_before(steps, sleep, cutoff)
            if features is None:
                audit['no_steps_in_window'] += 1
                continue
            rows.append({'subject': folder.name, 'cutoff': cutoff.isoformat(),
                         'stress': float(stress), **features})
    frame = pd.DataFrame(rows)
    if frame.empty or frame.subject.nunique() < 10:
        raise ValueError('Insufficient participants for this benchmark protocol')
    return frame, audit


def models():
    return {
        'median_baseline': DummyRegressor(strategy='median'),
        'mean_baseline': DummyRegressor(strategy='mean'),
        'ridge': make_pipeline(SimpleImputer(strategy='median', add_indicator=True),
                               StandardScaler(), Ridge(alpha=100)),
        'random_forest': make_pipeline(SimpleImputer(strategy='median', add_indicator=True),
                                       RandomForestRegressor(n_estimators=150, max_depth=5,
                                                             min_samples_leaf=20,
                                                             random_state=SEED, n_jobs=2)),
    }


def metrics(y, predicted, groups):
    errors = pd.DataFrame({'subject': np.asarray(groups), 'ae': np.abs(y - predicted)})
    return {'mae': float(mean_absolute_error(y, predicted)),
            'participant_macro_mae': float(errors.groupby('subject').ae.mean().mean()),
            'rmse': float(np.sqrt(mean_squared_error(y, predicted))),
            'r2': float(r2_score(y, predicted))}


def main():
    data, audit = build_dataset(ROOT / 'data' / 'ssaqs')
    out = ROOT / 'artifacts'
    out.mkdir(exist_ok=True)
    data.to_csv(out / 'features.csv', index=False)
    dev_ids, test_ids = next(GroupShuffleSplit(n_splits=1, test_size=.25, random_state=SEED)
                             .split(data, groups=data.subject))
    dev, test = data.iloc[dev_ids].reset_index(drop=True), data.iloc[test_ids].reset_index(drop=True)
    assert set(dev.subject).isdisjoint(test.subject)
    folds = list(GroupKFold(n_splits=5).split(dev, groups=dev.subject))
    cv_results = []
    for feature_set, features in FEATURE_SETS.items():
        for name, model in models().items():
            predicted = np.zeros(len(dev))
            for fit, validation in folds:
                assert set(dev.iloc[fit].subject).isdisjoint(dev.iloc[validation].subject)
                estimator = clone(model).fit(dev.iloc[fit][features], dev.iloc[fit].stress)
                predicted[validation] = np.clip(estimator.predict(dev.iloc[validation][features]), 0, 100)
            cv_results.append({'feature_set': feature_set, 'model': name,
                               **metrics(dev.stress.to_numpy(), predicted, dev.subject)})
    # All selection uses development data; held-out people are evaluated only below.
    candidates = [r for r in cv_results if r['model'] not in ('median_baseline', 'mean_baseline')]
    selected = min(candidates, key=lambda r: r['participant_macro_mae'])
    features = FEATURE_SETS[selected['feature_set']]
    fitted = clone(models()[selected['model']]).fit(dev[features], dev.stress)
    prediction = np.clip(fitted.predict(test[features]), 0, 100)
    baseline_predictions = {
        name: model.fit(dev[features], dev.stress).predict(test[features])
        for name, model in models().items() if name.endswith('baseline')
    }
    evaluation = {name: metrics(test.stress.to_numpy(), p, test.subject)
                  for name, p in baseline_predictions.items()}
    evaluation['selected_candidate'] = metrics(test.stress.to_numpy(), prediction, test.subject)
    paired = pd.DataFrame({'subject': test.subject,
                           'delta': np.abs(test.stress.to_numpy() - prediction)
                                    - np.abs(test.stress.to_numpy() - baseline_predictions['median_baseline'])})
    per_person = paired.groupby('subject').delta.mean()
    rng = np.random.default_rng(SEED)
    bootstrap = rng.choice(per_person.to_numpy(), (10000, len(per_person)), replace=True).mean(axis=1)
    ci = np.quantile(bootstrap, [.025, .975]).tolist()
    # Exploratory improvement is necessary but not sufficient for app deployment.
    improves = ci[1] < 0
    predictions = test[['subject', 'cutoff', 'stress']].copy()
    predictions['predicted_stress'] = prediction
    for name, values in baseline_predictions.items():
        predictions[name] = values
    predictions.to_csv(out / 'heldout_predictions.csv', index=False)
    report = {
        'seed': SEED, 'target': 'same-day self-reported stress 0..100 at evening prompt',
        'dataset': 'https://zenodo.org/records/18706837',
        'archive_sha256': hashlib.sha256((ROOT / 'data' / 'ssaqs.zip').read_bytes()).hexdigest(),
        'environment': {'python': platform.python_version(), 'numpy': np.__version__,
                        'pandas': pd.__version__, 'sklearn': sklearn.__version__, 'joblib': joblib.__version__},
        'audit': audit, 'rows': len(data), 'participants': int(data.subject.nunique()),
        'deep_sleep_missing_fraction': float(data.deep_sleep_minutes.isna().mean()),
        'development_rows': len(dev), 'test_rows': len(test),
        'development_participants': sorted(dev.subject.unique().tolist()),
        'test_participants': sorted(test.subject.unique().tolist()),
        'selection_metric': '5-fold development participant-macro MAE',
        'development_cv': cv_results, 'selected': selected, 'features': features,
        'heldout': evaluation,
        'candidate_minus_median_participant_mae': float(per_person.mean()),
        'participant_bootstrap_95_percent_interval': ci,
        'exploratory_improvement_over_median': improves,
        'deployment_status': 'RESEARCH_ONLY_NOT_VALIDATED_FOR_MINDCARE',
    }
    (out / 'benchmark.json').write_text(json.dumps(report, indent=2, allow_nan=False), encoding='utf-8')
    joblib.dump({'pipeline': fitted, 'features': features, 'report': report}, out / 'stress_research.joblib')
    print(json.dumps({k: report[k] for k in ('rows', 'participants', 'audit', 'selected', 'heldout',
                       'participant_bootstrap_95_percent_interval', 'deployment_status')}, indent=2))


if __name__ == '__main__':
    main()
