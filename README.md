# 🐾 TailSafe — AI-Assisted Pet Insurance Platform

TailSafe is a full-stack pet insurance management platform. Pet owners register their pets, buy insurance policies and submit claims. Uploaded veterinary records and bills are analysed by Google Gemini, which checks eligibility and pulls claim details out of receipts automatically. Administrators get a dashboard for managing plans, reviewing claims and monitoring platform activity.

---

## ✨ Features

### Pet owners
- **AI eligibility check**: upload a vet medical record (PDF). Gemini extracts pet details, medical history and pre-existing conditions, then returns an `ELIGIBLE` / `NOT_ELIGIBLE` decision with a reason.
- **Pet management**: add, edit and remove pets, with a full profile per pet.
- **Plans & policies**: browse and compare insurance plans, subscribe a pet to a plan and cancel active policies.
- **AI-assisted claims**: submit a claim with a receipt. If amounts or details are missing, the AI service extracts them from the bill.
- **Pet health chatbot**: an assistant that answers questions using the context of your pets.
- **Personal dashboard**: stats, active policies, recent claims and an activity feed.

### Administrators
- **Claims review**: approve or reject claims; the plan deductible is applied automatically.
- **Plan management**: create and edit plans, toggle them active or inactive and mark them as popular.
- **User management**: view all users along with their pets and policies.
- **Platform analytics**: total users, active policies, claims over time and approval rates.

---

## 🧱 Tech Stack

| Layer | Technologies |
|---|---|
| **Frontend** | Angular 21 (SSR + Express), NgRx Signals, Tailwind CSS 4, Chart.js / ng2-charts, Lucide icons, TypeScript 5.9 |
| **Backend** | Java 17, Spring Boot 4, Spring Security (JWT in HttpOnly cookie), Spring Data JPA / Hibernate, Bean Validation, Lombok, springdoc-openapi |
| **AI service** | Python 3.10, FastAPI, Google Gen AI SDK (Gemini), Pydantic, pypdf |
| **Database** | MySQL 8.0 |
| **Infrastructure** | Docker, Docker Compose, Nginx (reverse proxy, rate limiting, TLS), Let's Encrypt / Certbot |
| **CI/CD** | GitHub Actions → GitHub Container Registry → SSH deploy to a VM |

### Architecture

```
Browser ──► Nginx :80/:443 ──┬── /api/ ──► Spring Boot backend :8080 ──┬──► MySQL :3306
                             │                                        └──► FastAPI AI service :8001 ──► Gemini API
                             └── /     ──► Angular SSR frontend :4000
```

- **Nginx** is the only public entry point. It handles TLS and rate-limits `/api/` to 5 req/s per IP with a burst of 10.
- **The backend** holds the business logic and authentication, and calls the AI service.
- **The AI service** is internal only and protected by a shared `X-API-Key` header.

---

## ✅ Prerequisites

**Full stack with Docker:**
- Docker 24+ and Docker Compose v2

**Running services individually (local development):**
- Java 17 (the Maven wrapper is included)
- Node.js 20+ and npm
- Python 3.10+
- MySQL 8.0 running locally
- A Google Gemini API key ([get one here](https://aistudio.google.com/apikey))

---

## 🚀 Setup & Run

### 1. Clone and configure

```bash
git clone https://github.com/<your-username>/tailsafe.git
cd tailsafe
cp .env.example .env          # then fill in real values
```

### 2a. Run the full stack with Docker Compose

```bash
docker compose up --build
```

> **TLS note:** `nginx.conf` serves HTTPS and expects certificates under `certbot/conf/live/<domain>/`. Before the first run on a server, replace `tailsafe.example.com` in `nginx.conf` with your domain and run:
>
> ```bash
> chmod +x init-letsencrypt.sh
> DOMAIN=your.domain.com ./init-letsencrypt.sh
> ```
>
> For local-only development, use option 2b instead.

### 2b. Run services individually (local development)

Load the env vars into your shell first, then override the hosts for local use:

```bash
set -a; source .env; set +a
export DB_HOST=localhost AI_SERVICE_URL=http://localhost:8001 APP_FRONTEND_URL=http://localhost:4200
```

**AI service** (port 8001):
```bash
cd tailsafe-ai
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
API_SECRET_KEY=$AI_API_KEY GEMINI_MODEL=$AI_MODEL_NAME uvicorn app.main:app --port 8001 --reload
```

**Backend** (port 8080; creates the schema automatically):
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend** (port 4200):
```bash
cd tailsafe
npm install
npm start
```

> The frontend calls `/api/...` on its own origin. When using `ng serve`, add a dev proxy that forwards `/api` to `http://localhost:8080` (see Roadmap).

### 3. Sign in

When the backend starts, it creates an admin account from `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Register a normal account from the UI to try the pet owner flow. Sample medical records and bills for testing the AI features are in [`docs/`](docs/).

### Tests

```bash
cd backend && ./mvnw test      # Spring context test
cd tailsafe && npm test        # Angular unit tests
```

---

## 🔐 Environment Variables

All variables live in a root `.env` file (see [`.env.example`](.env.example)). Never commit `.env`.

| Variable | Used by | Description |
|---|---|---|
| `ROOT_PASSWORD` | MySQL | Root password for the MySQL container |
| `DB_SCHEMA` | MySQL, backend | Database name |
| `DB_USER` | MySQL, backend | Application database user |
| `DB_PASSWORD` | MySQL, backend | Application database password |
| `GEMINI_API_KEY` | AI service | Google Gemini API key |
| `AI_API_KEY` | backend, AI service | Shared secret for backend ↔ AI calls (passed to the AI service as `API_SECRET_KEY`) |
| `AI_SERVICE_URL` | backend | AI service base URL (`http://ai:8001` in Docker) |
| `AI_MODEL_NAME` | AI service | Gemini model name, e.g. `gemini-2.0-flash` (passed as `GEMINI_MODEL`) |
| `JWT_SECRET` | backend | HMAC signing key for JWTs (≥ 32 characters) |
| `ADMIN_EMAIL` | backend | Email of the admin user seeded at startup |
| `ADMIN_PASSWORD` | backend | Password of the admin user seeded at startup |
| `APP_FRONTEND_URL` | backend, frontend | Public frontend URL (CORS + SSR allowed hosts) |
| `GHCR_OWNER` | Docker Compose | Lowercase GitHub owner for the `ghcr.io/<owner>/tailsafe-*` images |

The standalone AI service also has its own [`tailsafe-ai/.env.example`](tailsafe-ai/.env.example).

### Deployment secrets (GitHub Actions)

`.github/workflows/deploy.yml` builds the images, pushes them to GHCR and deploys over SSH on every push to `main`. It needs these repository secrets: `DO_HOST`, `DO_USERNAME`, `DO_SSH_KEY`, `DO_SSH_PASSPHRASE`, plus every variable in the table above except `GHCR_OWNER`, which the workflow derives itself. The server must have a clone of the repo at `~/tailsafe`.

---

## 📂 Project Structure

```
tailsafe/
├── backend/                         # Spring Boot REST API
│   └── src/main/java/com/org/tailSafe/
│       ├── core/
│       │   ├── config/              # Security, CORS, RestTemplate, admin seeding
│       │   ├── exception/           # Global exception handler + error DTO
│       │   └── security/            # JWT util, auth filter, principal
│       └── domain/
│           ├── auth/                # Register, login, logout, /me
│           ├── user/                # User entity, admin user listing
│           ├── pet/                 # Pet CRUD with ownership checks
│           ├── insurance/           # Insurance plans
│           ├── policy/              # Policy subscription & cancellation
│           ├── claim/               # Claim submission & admin review
│           ├── ai/                  # Calls to the AI service
│           └── dashboard/           # User & admin stats
├── tailsafe/                        # Angular 21 SSR frontend
│   └── src/app/
│       ├── core/                    # models, services, stores, guards, interceptor, constants
│       └── features/                # landing, auth, dashboard (owner), admin
├── tailsafe-ai/                     # FastAPI AI microservice
│   └── app/
│       ├── api/endpoints.py         # health, eligibility, receipt extraction, chat
│       ├── core/                    # settings + API key auth
│       ├── schemas/                 # Pydantic models
│       └── services/                # Gemini prompts, PDF text extraction
├── docs/                            # API reference, business rules, sample PDFs
├── PRD.md                           # Product requirements
├── docker-compose.yml               # mysql, backend, ai, frontend, nginx, certbot
├── nginx.conf                       # Reverse proxy, rate limiting, TLS
├── init-letsencrypt.sh              # First-time certificate setup
└── .github/workflows/deploy.yml     # CI/CD pipeline
```

See [`docs/api_endpoints.md`](docs/api_endpoints.md) for the full API reference and [`docs/business_logic.md`](docs/business_logic.md) for the business rules.

---

## 🗺️ Roadmap

- [ ] Add an Angular dev proxy (`proxy.conf.json`) so `ng serve` works against a local backend
- [ ] Return proper HTTP status codes (404/403/409) via domain-specific exceptions
- [ ] Make AI receipt extraction reliable (read file bytes before going async, retry, track status)
- [ ] Guard claim review against double decisions and approved amounts above the claim amount
- [ ] Expose Swagger UI and health checks through Nginx securely
- [ ] Unit and integration tests for services, controllers and the AI endpoints
- [ ] Email notifications when a claim is approved or rejected
- [ ] Claim document storage and download for admins
- [ ] Policy renewal reminders and automatic expiry
- [ ] Structured logging and request tracing across services

---

## 📄 License

No license has been chosen yet. All rights reserved until one is added.
