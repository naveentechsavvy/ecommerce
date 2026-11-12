# Developer Setup

How to run the project locally after cloning the repo.

## 1. Required versions

| Tool | Version | Check with |
|---|---|---|
| **Java (JDK)** | **21** (Eclipse Temurin recommended) | `java -version` |
| **Maven** | 3.9.9. No install needed: use the included `mvnw` wrapper | `./mvnw -v` |
| **Node.js** | **22 LTS** (20.19+ also works; see `.nvmrc`) | `node -v` |
| **npm** | 10+ (bundled with Node) | `npm -v` |
| **Docker Desktop** | Any recent version with Compose v2 | `docker compose version` |
| **Git** | Any recent version | `git --version` |

Docker pulls these automatically:

| Service | Image |
|---|---|
| PostgreSQL | `postgres:16.4-alpine` |
| Keycloak | `quay.io/keycloak/keycloak:26.0.7` |

Key library versions, for reference: Spring Boot 3.5.6, Anthropic Java SDK 3.3.0, React 18.3.1, Vite 5.4.11, TypeScript 5.6.3, keycloak-js 26.0.7.

### Install on Windows (winget)

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install OpenJS.NodeJS.LTS
winget install Docker.DockerDesktop
winget install Git.Git
```

On macOS, use `brew install --cask temurin@21 docker` and `brew install node@22 git`.

Restart your terminal after installing, and open Docker Desktop once so the Docker engine starts.

## 2. Ports used

These ports must be free:

| Port | Service |
|---|---|
| 5432 | PostgreSQL |
| 8180 | Keycloak |
| 8080 | Backend API |
| 5173 | Storefront |
| 5174 | Admin portal |

## 3. First-time setup

```bash
git clone https://github.com/naveentechsavvy/ecommerce.git
cd ecommerce/apparel-commerce
```

Each step below runs in its own terminal, starting from `ecommerce/apparel-commerce`.

### Step 1: Start the database and Keycloak

```bash
cd infra
docker compose up -d
```

Wait about 30 seconds. Keycloak is ready when http://localhost:8180 opens.

### Step 2: Start the backend API

Windows (PowerShell):
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

macOS / Linux:
```bash
cd backend
./mvnw spring-boot:run
```

It's ready when you see `Started CommerceApplication`. The first run creates the tables and loads demo data. Check it at http://localhost:8080/swagger-ui.html.

**Optional:** to enable the AI chatbot, set your key before starting the backend. Without it, the chatbot falls back to keyword search.
```powershell
$env:ANTHROPIC_API_KEY = "sk-ant-..."       # PowerShell
```
```bash
export ANTHROPIC_API_KEY=sk-ant-...         # macOS / Linux
```

### Step 3: Start the storefront

```bash
cd storefront
npm ci
npm run dev
```

Open http://localhost:5173.

### Step 4: Start the admin portal

```bash
cd admin
npm ci
npm run dev
```

Open http://localhost:5174.

Use `npm ci` (not `npm install`) so you get the exact package versions from `package-lock.json`.

## 4. Login accounts

| Username | Password | Role | Use for |
|---|---|---|---|
| `admin@loomandco.example` | `admin123` | ADMIN | Admin portal: full access |
| `ops@loomandco.example` | `ops123` | OPERATIONS | Admin portal: orders and inventory |
| `shopper@example.com` | `shopper123` | CUSTOMER | Storefront |
| `admin` | `admin` | Keycloak console | http://localhost:8180 |

Test coupons: `WELCOME10`, `GUEST150` (guest only), `MEMBER20` (signed-in only).

Test payments: UPI `test@okbank` succeeds and `fail@upi` is declined. Any card number succeeds except one ending in `0002`.

## 5. Daily commands

| Task | Command |
|---|---|
| Start database and Keycloak | `cd infra` then `docker compose up -d` |
| Stop them (data is kept) | `cd infra` then `docker compose stop` |
| Reset database to demo data | `cd infra` then `docker compose down -v`, then `docker compose up -d` |
| Run backend tests | `cd backend` then `./mvnw test` |
| Build frontends | `npm run build` (inside `storefront` or `admin`) |
| Run everything in Docker | `cd infra` then `docker compose --profile full up -d --build` |

## 6. Rules for working on the code

- **Database changes:** add a new file such as `backend/src/main/resources/db/migration/V3__your_change.sql`. Never edit `V1` or `V2`; Flyway will refuse to start if you do.
- **Secrets:** never commit API keys or `.env` files. They are already in `.gitignore`.
- **Before pushing:** run `./mvnw test` in `backend`, and `npm run build` in both `storefront` and `admin`.

## 7. Troubleshooting

| Problem | Fix |
|---|---|
| `invalid target release: 21` | You're on an older JDK. Install JDK 21 and check that `java -version` shows 21. |
| `port 5432 is already allocated` | A local PostgreSQL is running. Stop it, or change the mapping to `"5433:5432"` in `infra/docker-compose.yml` and set `DB_URL=jdbc:postgresql://localhost:5433/commerce`. |
| `./mvnw: bad interpreter` or `permission denied` | Run `chmod +x mvnw`. If that doesn't help, re-clone so the line endings are fixed. |
| `Flyway ... checksum mismatch` | A migration file was edited. Revert it, or reset the database with `docker compose down -v`. |
| Admin shows "Could not reach the identity server" | Keycloak is still starting. Wait 30 seconds and refresh. |
| Logged in, but the API returns 401 | Open the apps at `localhost`, not `127.0.0.1`. |
| `npm ci` fails with an engine error | Use Node 22 (`nvm use` reads `.nvmrc`). |
| Chat says "AI assistant is offline" | `ANTHROPIC_API_KEY` wasn't set in the terminal that started the backend. |
