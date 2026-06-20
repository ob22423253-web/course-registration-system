FROM python:3.12-slim
WORKDIR /app

# Faster, smaller installs and no .pyc clutter inside the layer.
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# Copy requirements first so pip layer caches independently of code.
COPY requirements.txt ./
RUN pip install -r requirements.txt

COPY app ./app

# Non-root user — Render runs containers as root by default; we don't want that.
RUN useradd -r app && chown -R app:app /app
USER app

EXPOSE 8000

# Render injects PORT; default to 8000 for local docker run.
# Use sh -c so ${PORT:-8000} actually expands.
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
