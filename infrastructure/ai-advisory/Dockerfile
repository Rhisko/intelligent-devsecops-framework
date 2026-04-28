FROM python:3.11.14-slim

WORKDIR /app

COPY ai-advisory/requirements.txt /tmp/requirements.txt

RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r /tmp/requirements.txt

RUN useradd -m -u 10001 appuser
USER appuser