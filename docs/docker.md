# Docker Deployment

Local Docker Desktop deployment of the full stack via `docker-compose.yml` at
the repo root. 7 services: 5 app containers + MySQL + Redis.

## Services

| Service | Image | Container name | Internal port | Published port | Healthcheck |
|---|---|---|---|---|---|
| mysql | `mysql:8.0` | `medibridge-mysql-1` | 3306 | - (internal only) | `mysqladmin ping` |
| redis | `redis:7-alpine` | `medibridge-redis-1` | 6379 | - (internal only) | `redis-cli ping` |
| backend | `medibridge-backend` (built) | `medibridge-backend-1` | 8080 | - (internal only) | `curl /api/actuator/health` |
| chat-service | `medibridge-chat-service` (built) | `medibridge-chat-service-1` | 8000 | - (internal only) | `GET /health` |
| notify-service | `medibridge-notify-service` (built) | `medibridge-notify-service-1` | 8000 | - (internal only) | none |
| gateway | `medibridge-gateway` (built) | `medibridge-gateway-1` | 4000 | **4000** | `wget /health` |
| frontend | `medibridge-frontend` (built) | `medibridge-frontend-1` | 80 | **3000** | none |

Only **gateway** (`localhost:4000`) and **frontend** (`localhost:3000`) are
reachable from the host. Everything else talks over the internal `medibridge`
compose network by service name (e.g. backend reaches MySQL at `mysql:3306`).

Startup order is healthcheck-gated: `mysql` → `backend` → `chat-service` /
`notify-service` → `gateway` → `frontend`. If `backend` fails its healthcheck,
every service after it refuses to start.

Secrets live in the gitignored root `.env` (see `.env.example` for the
template) - DB password, JWT secret, the two internal API keys, and
third-party creds (Groq, Twilio, Razorpay, Google OAuth).

## Commands

### Bring the stack up

```bash
# Build images (only rebuilds services whose Dockerfile/context changed) and
# start everything in the background.
docker compose up --build -d
```

```bash
# Start without rebuilding - use when only .env values changed, not code.
docker compose up -d
```

```bash
# Rebuild + restart a single service (e.g. after an application.yml or
# source change). Compose automatically restarts anything that depends on it.
docker compose up --build -d backend
```

### Stop / remove

```bash
# Stop and remove containers + the default network. Volumes (MySQL data) survive.
docker compose down
```

```bash
# Same, but also wipes the MySQL volume - full reset, next boot reseeds from scratch.
docker compose down -v
```

```bash
# Pause without removing containers (state preserved, faster to resume).
docker compose stop
docker compose start
```

### Status

```bash
# One-line status per service: name, image, up/healthy state, ports.
docker compose ps
```

```bash
# Same, but includes stopped/exited containers too (raw docker, not compose-scoped).
docker ps -a
```

```bash
# Live CPU / memory / network per running container.
docker stats
```

```bash
# Full health-probe history for a container stuck as "unhealthy".
docker inspect --format='{{json .State.Health}}' medibridge-backend-1
```

### Logs

```bash
# Follow one service's logs live.
docker compose logs -f backend
```

```bash
# Last 50 lines, no follow - good for a quick check.
docker compose logs --tail 50 gateway
```

```bash
# Every service interleaved, live.
docker compose logs -f
```

```bash
# Grep a running container's logs for errors without leaving Docker's own log driver.
docker logs medibridge-backend-1 2>&1 | grep -iE "error|exception"
```

### Exec into a container

```bash
# Shell into a running container (alpine-based images use sh, not bash).
docker exec -it medibridge-backend-1 sh
```

```bash
# Run a one-off command without a shell, e.g. hit an internal endpoint.
docker exec medibridge-backend-1 curl -sv http://localhost:8080/api/actuator/health
```

### Images

```bash
# List all local images with size and age.
docker images
```

```bash
# Remove one image (must stop/remove any container using it first).
docker rmi medibridge-backend
```

```bash
# Delete every stopped container, dangling image, unused network - frees disk
# space. Does NOT touch volumes or images still in use.
docker system prune
```

```bash
# Force a clean rebuild of one service's image, ignoring the build cache.
docker compose build --no-cache backend
```

### Database access inside the container

```bash
# Open a MySQL shell against the Dockerized DB (password prompt uses DB_PASSWORD from .env).
docker exec -it medibridge-mysql-1 mysql -uroot -p medibridge
```

## Troubleshooting notes

- **`backend` unhealthy / dependents never start**: check
  `docker logs medibridge-backend-1 --tail 80` first - usually a startup
  exception. If the app itself started fine (`Tomcat started on port 8080`)
  but the healthcheck still fails, `docker exec medibridge-backend-1 curl -sv
  http://localhost:8080/api/actuator/health` shows whether the endpoint even
  exists (a custom 404 JSON body means actuator isn't wired in, not a security
  block).
- **Login / any POST from the frontend returns 403 near-instantly**: almost
  always Spring Security's CORS filter rejecting the `Origin` header before
  the request reaches the controller. Backend's allowed origin is
  `CORS_ALLOWED_ORIGINS` (set to `http://localhost:3000` in
  `docker-compose.yml` for the gateway/frontend's origin as Spring sees it).
  A stale hardcoded value here is a repeat-offender bug, not a one-off.
- **Google Sign-In "origin not allowed"**: not a Docker issue - the OAuth
  client's Authorized JavaScript origins list in Google Cloud Console needs
  `http://localhost:3000` added alongside the dev origin.
- **Config file changes (`application.yml`, `.env` defaults baked at build
  time) need `--build`**; only runtime env var changes in `docker-compose.yml`
  need a plain `up -d`.
