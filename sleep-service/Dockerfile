FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
ARG SKLEARN_VERSION
RUN test -n "$SKLEARN_VERSION" && pip install --no-cache-dir -r requirements.txt "scikit-learn==$SKLEARN_VERSION"
COPY app.py .
RUN useradd --uid 10001 --create-home inference
USER inference
EXPOSE 8085
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8085"]
