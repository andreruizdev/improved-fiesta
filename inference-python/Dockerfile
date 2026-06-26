FROM python:3.11-slim
RUN adduser --disabled-password --gecos '' appuser
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends build-essential librdkafka-dev && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app/ ./app/
USER appuser
CMD ["python", "-c", "import asyncio; from app.worker import start_event_consumer; asyncio.run(start_event_consumer())"]
