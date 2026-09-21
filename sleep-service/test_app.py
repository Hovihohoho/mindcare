import joblib
import pandas as pd
import pytest
import sklearn
from fastapi.testclient import TestClient
from sklearn.ensemble import RandomForestRegressor

from app import app, FEATURES, TARGET, load_bundle

BODY = {"date": "2016-05-06", "totalSteps": 10000,
        "veryActiveMinutes": 20, "fairlyActiveMinutes": 15,
        "lightlyActiveMinutes": 200, "sedentaryMinutes": 700,
        "totalMinutesAsleep": 420}


@pytest.fixture
def client(tmp_path, monkeypatch):
    # Synthetic fixture tests serving only, not accuracy of the Kaggle model.
    frame = pd.DataFrame([[10000, 20, 15, 200, 700, 420, 4]], columns=FEATURES)
    model = RandomForestRegressor(n_estimators=2, random_state=42).fit(frame, [430])
    path = tmp_path / "model.joblib"
    joblib.dump({"model": model, "features": FEATURES, "target": TARGET,
                 "sklearn_version": sklearn.__version__}, path)
    monkeypatch.setenv("SLEEP_MODEL_PATH", str(path))
    with TestClient(app) as test_client:
        yield test_client


def test_prediction_and_calendar_rollover(client):
    response = client.post("/predict", json={**BODY, "date": "2024-02-29"})
    assert response.status_code == 200
    assert response.json()["targetDate"] == "2024-03-01"
    assert response.json()["predictedSleepMinutes"] == 430


def test_feature_order_and_weekday(client):
    class Capture:
        def predict(self, frame):
            assert list(frame.columns) == FEATURES
            assert frame.iloc[0].tolist() == [10000, 20, 15, 200, 700, 420, 4]
            return [421.25]
    app.state.model = Capture()
    assert client.post("/predict", json=BODY).json()["predictedSleepMinutes"] == 421.25


@pytest.mark.parametrize("change", [
    {"totalSteps": -1}, {"totalSteps": True}, {"totalMinutesAsleep": 0},
    {"sedentaryMinutes": 1441}, {"date": "invalid"}, {"day_of_week": 4},
    {"date": "9999-12-31"}, {"totalSteps": 1.2},
])
def test_invalid_input(client, change):
    assert client.post("/predict", json={**BODY, **change}).status_code == 422


def test_missing_field(client):
    assert client.post("/predict", json={"date": "2016-05-06"}).status_code == 422


def test_missing_model(tmp_path, monkeypatch):
    monkeypatch.setenv("SLEEP_MODEL_PATH", str(tmp_path / "missing.joblib"))
    with TestClient(app) as client:
        assert client.get("/health").status_code == 503
        assert client.post("/predict", json=BODY).status_code == 503


def test_invalid_prediction(client):
    class Invalid:
        def predict(self, frame):
            return [float("nan")]
    app.state.model = Invalid()
    assert client.post("/predict", json=BODY).status_code == 503


def test_metadata_rejected(tmp_path):
    path = tmp_path / "wrong.joblib"
    joblib.dump({"features": FEATURES, "target": TARGET, "sklearn_version": "0.0"}, path)
    with pytest.raises(ValueError):
        load_bundle(path)
