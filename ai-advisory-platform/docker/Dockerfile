FROM python:3.11.14-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /opt/ai-advisory-platform

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt /tmp/requirements.txt

RUN pip install --upgrade pip && \
    pip install -r /tmp/requirements.txt

COPY app ./app
COPY configs ./configs
COPY prompts ./prompts
COPY logo.png ./logo.png
COPY .env ./.env

RUN useradd -m -u 10001 appuser && \
    chown -R appuser:appuser /opt/ai-advisory-platform

USER appuser
