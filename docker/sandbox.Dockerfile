# Optional minimal image for the docker sandbox backend.
# Build: docker build -f docker/sandbox.Dockerfile -t pyjudge-sandbox .
# Then set sandbox.backend=docker and sandbox.docker.image=pyjudge-sandbox in application.properties
# (or the PYJUDGE_SANDBOX_BACKEND / PYJUDGE_SANDBOX_DOCKER_IMAGE environment variables).
FROM python:3.11-slim
RUN useradd --uid 65534 --no-create-home --shell /usr/sbin/nologin sandbox || true
USER 65534
WORKDIR /code
