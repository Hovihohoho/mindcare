"""Internal inference service. Load only a trusted, locally deployed model artifact."""
import logging
import math
import os
import warnings
from contextlib import asynccontextmanager
from datetime import date, timedelta
from pathlib import Path

import joblib
import pandas as pd
import sklearn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, ConfigDict, Field
from sklearn.exceptions import InconsistentVersionWarning

FEATURES = ["TotalSteps", "VeryActiveMinutes", "FairlyActiveMinutes",
            "LightlyActiveMinutes", "SedentaryMinutes", "TotalMinutesAsleep",
            "day_of_week"]
TARGET = "target_sleep_minutes"


class SleepRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    date: date
    totalSteps: int = Field(ge=0, strict=True)
    veryActiveMinutes: int = Field(ge=0, le=1440, strict=True)
    fairlyActiveMinutes: int = Field(ge=0, le=1440, strict=True)
    lightlyActiveMinutes: int = Field(ge=0, le=1440, strict=True)
    sedentaryMinutes: int = Field(ge=0, le=1440, strict=True)
    totalMinutesAsleep: int = Field(ge=1, le=1440, strict=True)


def load_bundle(path):
    with warnings.catch_warnings():
        warnings.simplefilter("error", InconsistentVersionWarning)
        bundle = joblib.load(path)
    if (bundle.get("features") != FEATURES or bundle.get("target") != TARGET
            or bundle.get("sklearn_version") != sklearn.__version__):
        raise ValueError("Model metadata or scikit-learn version mismatch")
    model = bundle["model"]
    if list(model.feature_names_in_) != FEATURES:
        raise ValueError("Model input schema mismatch")
    return model


@asynccontextmanager
async def lifespan(app):
    app.state.model = None
    try:
        app.state.model = load_bundle(os.environ.get(
            "SLEEP_MODEL_PATH", str(Path(__file__).parent / "models/sleep_forecast.joblib")))
    except Exception:
        logging.exception("Sleep model unavailable; install matching dependencies and restart")
    yield


app = FastAPI(title="MindCare Sleep Inference", lifespan=lifespan)


@app.get("/health")
def health():
    if getattr(app.state, "model", None) is None:
        raise HTTPException(503, "Sleep model unavailable")
    return {"status": "ready"}


@app.post("/predict")
def predict(body: SleepRequest):
    try:
        target_date = body.date + timedelta(days=1)
    except OverflowError:
        raise HTTPException(422, "Date outside supported range")
    if getattr(app.state, "model", None) is None:
        raise HTTPException(503, "Sleep model unavailable")
    values = [body.totalSteps, body.veryActiveMinutes, body.fairlyActiveMinutes,
              body.lightlyActiveMinutes, body.sedentaryMinutes,
              body.totalMinutesAsleep, body.date.weekday()]
    try:
        result = float(app.state.model.predict(pd.DataFrame([values], columns=FEATURES))[0])
        if not math.isfinite(result) or not 0 <= result <= 1440:
            raise ValueError("Invalid prediction")
    except Exception:
        logging.exception("Sleep inference failed")
        raise HTTPException(503, "Sleep inference unavailable")
    return {"date": body.date.isoformat(), "targetDate": target_date.isoformat(),
            "predictedSleepMinutes": round(result, 2), "modelType": "RandomForestRegressor"}
