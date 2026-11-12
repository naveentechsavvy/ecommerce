-- =====================================================================
-- Apparel Commerce - core schema
-- Conventions: bigserial PKs, timestamptz audit columns, numeric(12,2)
-- money in INR, enums stored as varchar + CHECK (portable, JPA-friendly).
-- =====================================================================

-- ---------------------------------------------------------------- catalog
CREATE TABLE category (
    id            BIGSERIAL PRIMARY KEY,
    parent_id     BIGINT REFERENCES category (id) ON DELETE RESTRICT,
    name          VARCHAR(120) NOT NULL,
    slug          VARCHAR(140) NOT NULL UNIQUE,
    description   TEXT,
    image_url     VARCHAR(500),
    sort_order    INT          NOT NULL DEFAULT 0,
    visibility    VARCHAR(16)  NOT NULL DEFAULT 'ENABLED'
                  CHECK (visibility IN ('ENABLED', 'DISABLED', 'HIDDEN')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_category_parent ON category (parent_id);

CREATE TABLE size_chart (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    -- rows: [{"size":"M","chestCm":[96,101],"waistCm":[81,86],"heightCm":[170,178]}]
    rows        JSONB        NOT NULL,
    fit_notes   TEXT
);

CREATE TABLE product (
    id               BIGSERIAL PRIMARY KEY,
    category_id      BIGINT        NOT NULL REFERENCES category (id),
    size_chart_id    BIGINT REFERENCES size_chart (id),
    name             VARCHAR(200)  NOT NULL,
    slug             VARCHAR(220)  NOT NULL UNIQUE,
    brand            VARCHAR(120),
    description      TEXT,
    material         VARCHAR(200),
    care_instructions TEXT,
    gender           VARCHAR(16) CHECK (gender IN ('MEN', 'WOMEN', 'UNISEX', 'KIDS')),
    base_price       NUMERIC(12, 2) NOT NULL CHECK (base_price >= 0),
    compare_at_price NUMERIC(12, 2) CHECK (compare_at_price >= 0),
    status           VARCHAR(16)   NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    popularity_score INT           NOT NULL DEFAULT 0,
    -- PDP marketing slot managed from admin (badges, promo copy)
    marketing_badge  VARCHAR(60),
    marketing_html   TEXT,
    search_vector    TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(brand, '') || ' ' || coalesce(material, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'C')
    ) STORED,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_product_category ON product (category_id);
CREATE INDEX idx_product_status_created ON product (status, created_at DESC);
CREATE INDEX idx_product_price ON product (base_price);
CREATE INDEX idx_product_search ON product USING GIN (search_vector);

CREATE TABLE product_image (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT       NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    alt_text    VARCHAR(200),
    color       VARCHAR(60),           -- optional: image belongs to a colourway
    sort_order  INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_product_image_product ON product_image (product_id);

CREATE TABLE product_variant (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT        NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    sku             VARCHAR(64)   NOT NULL UNIQUE,
    size            VARCHAR(16)   NOT NULL,
    color           VARCHAR(60)   NOT NULL,
    color_hex       VARCHAR(7),
    price_override  NUMERIC(12, 2) CHECK (price_override >= 0),
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    UNIQUE (product_id, size, color)
);
CREATE INDEX idx_variant_product ON product_variant (product_id);

-- -------------------------------------------------------------- inventory
CREATE TABLE inventory (
    variant_id          BIGINT PRIMARY KEY REFERENCES product_variant (id) ON DELETE CASCADE,
    on_hand             INT         NOT NULL DEFAULT 0,
    reserved            INT         NOT NULL DEFAULT 0,
    low_stock_threshold INT         NOT NULL DEFAULT 5,
    version             BIGINT      NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_inventory_non_negative CHECK (on_hand >= 0 AND reserved >= 0 AND reserved <= on_hand)
);

CREATE TABLE inventory_movement (
    id          BIGSERIAL PRIMARY KEY,
    variant_id  BIGINT      NOT NULL REFERENCES product_variant (id) ON DELETE CASCADE,
    delta       INT         NOT NULL,
    reason      VARCHAR(16) NOT NULL
                CHECK (reason IN ('RESTOCK', 'ADJUSTMENT', 'DAMAGE', 'RESERVE', 'RELEASE', 'SALE', 'RETURN')),
    reference   VARCHAR(80),
    note        VARCHAR(300),
    created_by  VARCHAR(120),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_movement_variant ON inventory_movement (variant_id, created_at DESC);

-- ------------------------------------------------------------ CMS content
CREATE TABLE banner (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(160) NOT NULL,
    subtitle         VARCHAR(300),
    image_url        VARCHAR(500) NOT NULL,
    mobile_image_url VARCHAR(500),
    cta_text         VARCHAR(60),
    cta_link         VARCHAR(300),
    placement        VARCHAR(20)  NOT NULL
                     CHECK (placement IN ('HOME_HERO', 'HOME_STRIP', 'PLP_TOP', 'PDP_PROMO')),
    sort_order       INT          NOT NULL DEFAULT 0,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    starts_at        TIMESTAMPTZ,
    ends_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_banner_placement ON banner (placement, active, sort_order);

CREATE TABLE home_section (
    id           BIGSERIAL PRIMARY KEY,
    section_type VARCHAR(20)  NOT NULL
                 CHECK (section_type IN ('HERO_CAROUSEL', 'PRODUCT_RAIL', 'CATEGORY_GRID', 'BANNER_STRIP', 'RICH_TEXT')),
    title        VARCHAR(160),
    -- type-specific config, e.g. PRODUCT_RAIL: {"categorySlug":"men","sort":"newest","limit":8}
    config       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    sort_order   INT          NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE navigation_item (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT REFERENCES navigation_item (id) ON DELETE CASCADE,
    label       VARCHAR(80)  NOT NULL,
    link_type   VARCHAR(12)  NOT NULL CHECK (link_type IN ('CATEGORY', 'URL', 'NONE')),
    category_id BIGINT REFERENCES category (id) ON DELETE SET NULL,
    url         VARCHAR(300),
    badge_text  VARCHAR(30),
    sort_order  INT          NOT NULL DEFAULT 0,
    visibility  VARCHAR(16)  NOT NULL DEFAULT 'ENABLED'
                CHECK (visibility IN ('ENABLED', 'DISABLED', 'HIDDEN'))
);
CREATE INDEX idx_nav_parent ON navigation_item (parent_id, sort_order);

-- ---------------------------------------------------------------- coupons
CREATE TABLE coupon (
    id                   BIGSERIAL PRIMARY KEY,
    code                 VARCHAR(40)    NOT NULL UNIQUE,   -- stored upper-case
    description          VARCHAR(300),
    discount_type        VARCHAR(16)    NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    discount_value       NUMERIC(12, 2) NOT NULL CHECK (discount_value > 0),
    max_discount_amount  NUMERIC(12, 2),                   -- cap for PERCENTAGE
    min_cart_value       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    target_user_type     VARCHAR(12)    NOT NULL DEFAULT 'ALL'
                         CHECK (target_user_type IN ('ALL', 'GUEST', 'REGISTERED')),
    starts_at            TIMESTAMPTZ,
    ends_at              TIMESTAMPTZ,
    usage_limit_total    INT,                              -- null = unlimited
    usage_limit_per_user INT,                              -- null = unlimited
    times_used           INT            NOT NULL DEFAULT 0,
    active               BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_coupon_pct CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100)
);

-- ----------------------------------------------------------------- orders
CREATE TABLE customer_address (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  VARCHAR(64)  NOT NULL,    -- Keycloak subject (sub)
    label        VARCHAR(40),
    full_name    VARCHAR(120) NOT NULL,
    phone        VARCHAR(20)  NOT NULL,
    line1        VARCHAR(200) NOT NULL,
    line2        VARCHAR(200),
    city         VARCHAR(80)  NOT NULL,
    state        VARCHAR(80)  NOT NULL,
    postal_code  VARCHAR(12)  NOT NULL,
    country      VARCHAR(2)   NOT NULL DEFAULT 'IN',
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_address_customer ON customer_address (customer_id);

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(24)    NOT NULL UNIQUE,
    customer_id     VARCHAR(64),             -- null for guest checkout
    email           VARCHAR(200)   NOT NULL,
    status          VARCHAR(20)    NOT NULL
                    CHECK (status IN ('PENDING_PAYMENT', 'PAYMENT_FAILED', 'PAID', 'PROCESSING',
                                      'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED')),
    currency        VARCHAR(3)     NOT NULL DEFAULT 'INR',
    subtotal        NUMERIC(12, 2) NOT NULL,
    discount_total  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    shipping_total  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tax_total       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    grand_total     NUMERIC(12, 2) NOT NULL,
    coupon_id       BIGINT REFERENCES coupon (id),
    coupon_code     VARCHAR(40),
    payment_method  VARCHAR(16),
    -- shipping address snapshot
    ship_full_name  VARCHAR(120) NOT NULL,
    ship_phone      VARCHAR(20)  NOT NULL,
    ship_line1      VARCHAR(200) NOT NULL,
    ship_line2      VARCHAR(200),
    ship_city       VARCHAR(80)  NOT NULL,
    ship_state      VARCHAR(80)  NOT NULL,
    ship_postal_code VARCHAR(12) NOT NULL,
    ship_country    VARCHAR(2)   NOT NULL,
    -- billing address snapshot
    bill_full_name  VARCHAR(120) NOT NULL,
    bill_phone      VARCHAR(20)  NOT NULL,
    bill_line1      VARCHAR(200) NOT NULL,
    bill_line2      VARCHAR(200),
    bill_city       VARCHAR(80)  NOT NULL,
    bill_state      VARCHAR(80)  NOT NULL,
    bill_postal_code VARCHAR(12) NOT NULL,
    bill_country    VARCHAR(2)   NOT NULL,
    tracking_number VARCHAR(80),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_customer ON orders (customer_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders (status, created_at DESC);
CREATE INDEX idx_orders_email ON orders (lower(email));

CREATE TABLE order_item (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    variant_id    BIGINT         NOT NULL REFERENCES product_variant (id),
    sku           VARCHAR(64)    NOT NULL,
    product_name  VARCHAR(200)   NOT NULL,
    product_slug  VARCHAR(220)   NOT NULL,
    size          VARCHAR(16)    NOT NULL,
    color         VARCHAR(60)    NOT NULL,
    image_url     VARCHAR(500),
    unit_price    NUMERIC(12, 2) NOT NULL,
    quantity      INT            NOT NULL CHECK (quantity > 0),
    line_total    NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_order_item_order ON order_item (order_id);

CREATE TABLE order_status_history (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT      NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    from_status  VARCHAR(20),
    to_status    VARCHAR(20) NOT NULL,
    note         VARCHAR(300),
    changed_by   VARCHAR(120),
    changed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_status_history_order ON order_status_history (order_id, changed_at);

CREATE TABLE payment (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    method        VARCHAR(16)    NOT NULL CHECK (method IN ('CREDIT_CARD', 'DEBIT_CARD', 'WALLET', 'UPI')),
    provider      VARCHAR(40)    NOT NULL,
    provider_ref  VARCHAR(80),
    instrument    VARCHAR(80),             -- masked: "VISA •••• 4242", "name@okbank", "PAYTM"
    status        VARCHAR(12)    NOT NULL CHECK (status IN ('INITIATED', 'SUCCEEDED', 'FAILED', 'REFUNDED')),
    amount        NUMERIC(12, 2) NOT NULL,
    failure_reason VARCHAR(200),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX idx_payment_order ON payment (order_id);

CREATE TABLE coupon_redemption (
    id               BIGSERIAL PRIMARY KEY,
    coupon_id        BIGINT         NOT NULL REFERENCES coupon (id),
    order_id         BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    customer_id      VARCHAR(64),
    email            VARCHAR(200)   NOT NULL,
    discount_amount  NUMERIC(12, 2) NOT NULL,
    redeemed_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX idx_redemption_coupon_customer ON coupon_redemption (coupon_id, customer_id);
CREATE INDEX idx_redemption_coupon_email ON coupon_redemption (coupon_id, lower(email));
