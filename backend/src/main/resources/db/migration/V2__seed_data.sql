-- =====================================================================
-- Demo seed data (safe to drop in production: move to a "demo" profile
-- location such as db/demo and add it to spring.flyway.locations only in dev)
-- =====================================================================

INSERT INTO category (id, parent_id, name, slug, description, sort_order, visibility) VALUES
 (1,  NULL, 'Men',          'men',          'Menswear essentials',            1, 'ENABLED'),
 (2,  NULL, 'Women',        'women',        'Womenswear for every day',       2, 'ENABLED'),
 (3,  NULL, 'Kids',         'kids',         'Play-proof kidswear',            3, 'ENABLED'),
 (4,  1,    'T-Shirts',     'men-t-shirts', 'Crew necks, polos and henleys',  1, 'ENABLED'),
 (5,  1,    'Shirts',       'men-shirts',   'Casual and formal shirts',       2, 'ENABLED'),
 (6,  1,    'Jeans',        'men-jeans',    'Slim, straight and relaxed',     3, 'ENABLED'),
 (7,  2,    'Dresses',      'women-dresses','Midi, maxi and mini dresses',    1, 'ENABLED'),
 (8,  2,    'Tops',         'women-tops',   'Tees, blouses and tanks',        2, 'ENABLED'),
 (9,  2,    'Jeans',        'women-jeans',  'High-rise to wide-leg',          3, 'ENABLED'),
 (10, 3,    'T-Shirts',     'kids-t-shirts','Graphic tees for kids',          1, 'ENABLED'),
 (11, NULL, 'Festive Edit', 'festive-edit', 'Launching soon',                 4, 'DISABLED');
SELECT setval('category_id_seq', 20);

INSERT INTO size_chart (id, name, rows, fit_notes) VALUES
 (1, 'Men Tops', '[
   {"size":"S","chestCm":[88,94],"heightCm":[160,170]},
   {"size":"M","chestCm":[95,101],"heightCm":[168,176]},
   {"size":"L","chestCm":[102,108],"heightCm":[174,182]},
   {"size":"XL","chestCm":[109,116],"heightCm":[180,190]}]', 'Regular fit. Size down for a closer fit.'),
 (2, 'Women Tops & Dresses', '[
   {"size":"XS","bustCm":[78,82],"heightCm":[150,160]},
   {"size":"S","bustCm":[83,87],"heightCm":[155,165]},
   {"size":"M","bustCm":[88,93],"heightCm":[160,170]},
   {"size":"L","bustCm":[94,100],"heightCm":[165,175]}]', 'True to size. Dresses are midi length on 165cm.'),
 (3, 'Bottoms (waist)', '[
   {"size":"28","waistCm":[71,74]},
   {"size":"30","waistCm":[75,79]},
   {"size":"32","waistCm":[80,84]},
   {"size":"34","waistCm":[85,89]}]', 'Stretch denim; if between sizes pick the smaller.'),
 (4, 'Kids', '[
   {"size":"4Y","heightCm":[98,104]},
   {"size":"6Y","heightCm":[110,116]},
   {"size":"8Y","heightCm":[122,128]}]', 'Roomy fit with room to grow.');
SELECT setval('size_chart_id_seq', 10);

INSERT INTO product (id, category_id, size_chart_id, name, slug, brand, description, material, care_instructions,
                     gender, base_price, compare_at_price, status, popularity_score, marketing_badge, created_at) VALUES
 (1, 4, 1, 'Essential Crew Tee', 'essential-crew-tee', 'Loom & Thread',
  'Heavyweight 220gsm cotton crew neck tee with a relaxed drape. Built to keep its shape wash after wash.',
  '100% organic cotton', 'Machine wash cold, tumble dry low', 'MEN', 799, 999, 'ACTIVE', 95, 'Bestseller', now() - interval '40 days'),
 (2, 4, 1, 'Pique Polo', 'pique-polo', 'Loom & Thread',
  'Classic two-button polo in breathable pique knit with ribbed collar.',
  '95% cotton, 5% elastane', 'Machine wash cold', 'MEN', 1299, NULL, 'ACTIVE', 70, NULL, now() - interval '20 days'),
 (3, 5, 1, 'Oxford Button-Down Shirt', 'oxford-button-down-shirt', 'Northline',
  'Soft brushed oxford cloth shirt with a button-down collar. Dress it up or down.',
  '100% cotton oxford', 'Machine wash warm, iron medium', 'MEN', 1899, 2299, 'ACTIVE', 80, 'New', now() - interval '3 days'),
 (4, 5, 1, 'Linen Camp Collar Shirt', 'linen-camp-collar-shirt', 'Northline',
  'Breezy linen-blend short-sleeve shirt with a relaxed camp collar for warm days.',
  '55% linen, 45% cotton', 'Hand wash or gentle cycle', 'MEN', 1599, NULL, 'ACTIVE', 60, NULL, now() - interval '10 days'),
 (5, 6, 3, 'Slim Stretch Jeans', 'slim-stretch-jeans', 'Denim Works',
  'Slim fit jeans in comfort stretch denim, mid rise, five-pocket styling.',
  '98% cotton, 2% elastane', 'Wash inside out, cold', 'MEN', 2499, 2999, 'ACTIVE', 88, NULL, now() - interval '60 days'),
 (6, 7, 2, 'Floral Wrap Midi Dress', 'floral-wrap-midi-dress', 'Bloom',
  'Flowing wrap dress with flutter sleeves and an adjustable tie waist, printed with a soft floral.',
  '100% viscose', 'Hand wash cold, line dry', 'WOMEN', 2799, 3499, 'ACTIVE', 92, 'Trending', now() - interval '5 days'),
 (7, 7, 2, 'Ribbed Knit Bodycon Dress', 'ribbed-knit-bodycon-dress', 'Bloom',
  'Figure-skimming ribbed knit dress with a square neckline.',
  '92% polyester, 8% elastane', 'Machine wash cold', 'WOMEN', 1999, NULL, 'ACTIVE', 55, NULL, now() - interval '25 days'),
 (8, 8, 2, 'Relaxed Boxy Tee', 'relaxed-boxy-tee', 'Loom & Thread',
  'Cropped boxy tee in garment-dyed cotton jersey.',
  '100% cotton', 'Machine wash cold', 'WOMEN', 699, NULL, 'ACTIVE', 75, NULL, now() - interval '15 days'),
 (9, 9, 3, 'High-Rise Wide Leg Jeans', 'high-rise-wide-leg-jeans', 'Denim Works',
  'High-rise wide leg jeans in rigid vintage-wash denim with a full-length hem.',
  '100% cotton denim', 'Wash inside out, cold', 'WOMEN', 2699, NULL, 'ACTIVE', 85, 'New', now() - interval '2 days'),
 (10, 10, 4, 'Dino Graphic Tee', 'dino-graphic-tee', 'Little Loom',
  'Super-soft cotton tee with a glow-in-the-dark dinosaur print.',
  '100% cotton', 'Machine wash cold, inside out', 'KIDS', 499, 599, 'ACTIVE', 50, NULL, now() - interval '30 days'),
 (11, 4, 1, 'Merino Henley (Draft)', 'merino-henley', 'Northline',
  'Draft product - not visible on the storefront.', '100% merino wool', 'Hand wash', 'MEN', 3499, NULL, 'DRAFT', 0, NULL, now());
SELECT setval('product_id_seq', 100);

-- Images: deterministic placeholders
INSERT INTO product_image (product_id, url, alt_text, sort_order)
SELECT p.id, 'https://picsum.photos/seed/' || p.slug || '-' || n || '/600/800', p.name || ' view ' || n, n
FROM product p CROSS JOIN generate_series(1, 3) AS n;

-- Variants (product, colour, hex) x sizes
WITH colours(product_id, color, hex) AS (VALUES
   (1, 'Black', '#111111'), (1, 'White', '#F5F5F5'), (1, 'Olive', '#556B2F'),
   (2, 'Navy', '#1F2A44'), (2, 'White', '#F5F5F5'),
   (3, 'Light Blue', '#A7C7E7'), (3, 'White', '#F5F5F5'),
   (4, 'Sand', '#D8C3A5'),
   (5, 'Indigo', '#3F4B7A'), (5, 'Black', '#111111'),
   (6, 'Rose Floral', '#E8A0B4'),
   (7, 'Black', '#111111'), (7, 'Mocha', '#8B6B4E'),
   (8, 'White', '#F5F5F5'), (8, 'Sage', '#9CAF88'),
   (9, 'Vintage Blue', '#6C8EBF'),
   (10, 'Yellow', '#F4D35E'), (10, 'Navy', '#1F2A44'),
   (11, 'Charcoal', '#36454F')),
sizes(chart_id, size, ord) AS (VALUES
   (1, 'S', 1), (1, 'M', 2), (1, 'L', 3), (1, 'XL', 4),
   (2, 'XS', 1), (2, 'S', 2), (2, 'M', 3), (2, 'L', 4),
   (3, '28', 1), (3, '30', 2), (3, '32', 3), (3, '34', 4),
   (4, '4Y', 1), (4, '6Y', 2), (4, '8Y', 3))
INSERT INTO product_variant (product_id, sku, size, color, color_hex)
SELECT c.product_id,
       upper(regexp_replace(p.slug, '[^a-z]', '', 'g'))::varchar(8) || '-' || upper(replace(c.color, ' ', '')) || '-' || s.size,
       s.size, c.color, c.hex
FROM colours c
JOIN product p ON p.id = c.product_id
JOIN sizes s ON s.chart_id = p.size_chart_id
ORDER BY c.product_id, c.color, s.ord;

-- Inventory: pseudo-random stock; some variants deliberately out of stock / low stock
INSERT INTO inventory (variant_id, on_hand, low_stock_threshold)
SELECT v.id,
       CASE WHEN v.id % 11 = 0 THEN 0
            WHEN v.id % 7 = 0 THEN 3
            ELSE 10 + (v.id * 13) % 40 END,
       5
FROM product_variant v;
-- Whole product out of stock for badge demo
UPDATE inventory SET on_hand = 0 WHERE variant_id IN (SELECT id FROM product_variant WHERE product_id = 7);

INSERT INTO inventory_movement (variant_id, delta, reason, note, created_by)
SELECT variant_id, on_hand, 'RESTOCK', 'Initial stock load', 'seed' FROM inventory WHERE on_hand > 0;

-- CMS
INSERT INTO banner (title, subtitle, image_url, cta_text, cta_link, placement, sort_order) VALUES
 ('The Summer Linen Edit', 'Breathable layers for hot days', 'https://picsum.photos/seed/hero-linen/1600/600', 'Shop Men', '/c/men', 'HOME_HERO', 1),
 ('Dresses You''ll Live In', 'New florals just landed', 'https://picsum.photos/seed/hero-dresses/1600/600', 'Shop Dresses', '/c/women-dresses', 'HOME_HERO', 2),
 ('Free shipping over ₹999', 'Use WELCOME10 for 10% off your first order', 'https://picsum.photos/seed/strip-ship/1600/200', NULL, NULL, 'HOME_STRIP', 1),
 ('Denim Days: up to 20% off', NULL, 'https://picsum.photos/seed/plp-denim/1600/300', 'Shop Jeans', '/c/men-jeans', 'PLP_TOP', 1),
 ('Easy 30-day returns', 'Free pickup from your doorstep', 'https://picsum.photos/seed/pdp-returns/800/200', NULL, NULL, 'PDP_PROMO', 1);

INSERT INTO home_section (section_type, title, config, sort_order) VALUES
 ('HERO_CAROUSEL', NULL, '{"placement":"HOME_HERO"}', 1),
 ('CATEGORY_GRID', 'Shop by Category', '{"categorySlugs":["men","women","kids"]}', 2),
 ('PRODUCT_RAIL', 'New Arrivals', '{"sort":"newest","limit":8}', 3),
 ('BANNER_STRIP', NULL, '{"placement":"HOME_STRIP"}', 4),
 ('PRODUCT_RAIL', 'Most Loved', '{"sort":"popularity","limit":8}', 5),
 ('RICH_TEXT', 'Made to Last', '{"html":"<p>Every piece is designed to be worn hundreds of times. Responsibly sourced fabrics, honest pricing.</p>"}', 6);

-- Mega menu: top-level items map to categories; children grouped beneath
INSERT INTO navigation_item (id, parent_id, label, link_type, category_id, url, badge_text, sort_order, visibility) VALUES
 (1, NULL, 'Men',          'CATEGORY', 1,  NULL, NULL,   1, 'ENABLED'),
 (2, NULL, 'Women',        'CATEGORY', 2,  NULL, NULL,   2, 'ENABLED'),
 (3, NULL, 'Kids',         'CATEGORY', 3,  NULL, NULL,   3, 'ENABLED'),
 (4, NULL, 'Festive Edit', 'CATEGORY', 11, NULL, 'Soon', 4, 'DISABLED'),
 (5, NULL, 'Clearance',    'URL',      NULL, '/c/men?sort=price_asc', NULL, 5, 'HIDDEN'),
 (6, 1, 'T-Shirts & Polos', 'CATEGORY', 4, NULL, NULL,  1, 'ENABLED'),
 (7, 1, 'Shirts',           'CATEGORY', 5, NULL, 'New', 2, 'ENABLED'),
 (8, 1, 'Jeans',            'CATEGORY', 6, NULL, NULL,  3, 'ENABLED'),
 (9, 2, 'Dresses',          'CATEGORY', 7, NULL, NULL,  1, 'ENABLED'),
 (10, 2, 'Tops',            'CATEGORY', 8, NULL, NULL,  2, 'ENABLED'),
 (11, 2, 'Jeans',           'CATEGORY', 9, NULL, NULL,  3, 'ENABLED'),
 (12, 3, 'T-Shirts',        'CATEGORY', 10, NULL, NULL, 1, 'ENABLED');
SELECT setval('navigation_item_id_seq', 100);

INSERT INTO coupon (code, description, discount_type, discount_value, max_discount_amount, min_cart_value,
                    target_user_type, starts_at, ends_at, usage_limit_total, usage_limit_per_user) VALUES
 ('WELCOME10', '10% off for new shoppers (max ₹300)', 'PERCENTAGE', 10, 300, 499, 'ALL', now() - interval '1 day', now() + interval '365 days', 10000, 1),
 ('GUEST150',  '₹150 off guest checkout above ₹1499', 'FIXED_AMOUNT', 150, NULL, 1499, 'GUEST', NULL, NULL, NULL, 3),
 ('MEMBER20',  '20% off for signed-in members (max ₹1000)', 'PERCENTAGE', 20, 1000, 1999, 'REGISTERED', NULL, now() + interval '90 days', 500, 2),
 ('EXPIRED5',  'Expired demo coupon', 'PERCENTAGE', 5, NULL, 0, 'ALL', now() - interval '30 days', now() - interval '1 day', NULL, NULL);
