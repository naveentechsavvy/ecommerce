# Loom & Co. — B2C Apparel Commerce

A full-stack apparel store:

- **Customer storefront** (React): CMS-driven homepage, mega menu, PLP with filters and sorting, PDP with size and colour variants, coupons, guest and member checkout (Card, Debit, UPI, Wallet via a mock gateway), order tracking, and an AI shopping assistant.
- **Admin back office** (React): catalog, SKUs and images, real-time inventory, banners, homepage layout, menu visibility, coupons, and order fulfilment.
- **Spring Boot API** (Java 21) backed by **PostgreSQL**, with **Keycloak** for identity.

**New to the project? Start with [docs/SETUP.md](docs/SETUP.md)**: required versions and step-by-step setup.

Architecture, ER diagram, API contracts, chatbot design, and the 4-phase roadmap are in **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

```
apparel-commerce/
├── backend/      Spring Boot API (catalog, inventory, content, coupon, order, payment, customer, chat)
├── storefront/   Customer SPA            → http://localhost:5173
├── admin/        Back-office SPA         → http://localhost:5174
├── infra/        docker-compose, Keycloak realm, Postgres init
└── docs/         Architecture & roadmap
```

## Run locally

Prerequisites: Docker, JDK 21, Maven 3.9+, Node 20+.

```bash
# 1. Postgres (:5432) + Keycloak (:8180), realm auto-imported
cd infra && docker compose up -d

# 2. API on :8080 (Flyway creates the schema and demo data on first start)
cd ../backend
export ANTHROPIC_API_KEY=sk-ant-...      # optional; without it the assistant runs in keyword mode
mvn spring-boot:run

# 3. Storefront on :5173
cd ../storefront && npm install && npm run dev

# 4. Admin portal on :5174
cd ../admin && npm install && npm run dev
```

On JDK 17, build with `mvn -Djava.version=17 ...`. The code uses no 21-only APIs, but production should run on 21.

To run the API in Docker as well: `docker compose --profile full up -d --build`.

### Demo accounts (Keycloak realm `apparel`)

| User | Password | Role | Use in |
|---|---|---|---|
| `admin@loomandco.example` | `admin123` | ADMIN | Admin portal (everything) |
| `ops@loomandco.example` | `ops123` | OPERATIONS | Admin portal (orders, inventory, read-only catalog) |
| `shopper@example.com` | `shopper123` | CUSTOMER | Storefront (or self-register) |

Keycloak console: http://localhost:8180 (`admin` / `admin`).

### Things to try

- **Coupons:** `WELCOME10` (10%, max ₹300), `GUEST150` (guests only, cart ≥ ₹1,499), `MEMBER20` (signed-in only, cart ≥ ₹1,999), `EXPIRED5` (shows the expiry message).
- **Payments (mock):** any card number succeeds except one ending in `0002`. UPI `anything@okbank` succeeds and `fail@upi` is declined. Every wallet succeeds.
- **Stock badges:** the *Ribbed Knit Bodycon Dress* is fully out of stock, and several SKUs are low.
- **Menu visibility:** "Festive Edit" is DISABLED (greyed out) and "Clearance" is HIDDEN. Toggle them under Admin → Menu & navigation.
- **Assistant:** "linen shirts under ₹2000", "what size is the wrap dress for 165cm, bust 90?", "where's order AC…?"

## Tests

```bash
cd backend && mvn test          # coupon rule unit tests
cd storefront && npm run build  # type-check + production build
cd admin && npm run build
```

## Security notes

- Card numbers never reach the API. The storefront's `mockTokenize` stands in for a PSP's hosted fields, which return a token.
- Prices, discounts, stock, and shipping are always recomputed on the server. Client cart values are only for display.
- See section 6 of the architecture doc for the production hardening checklist.







