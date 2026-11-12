import DOMPurify from 'dompurify';
import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { BannerCarousel } from '../components/BannerCarousel';
import { useCart } from '../cart/CartContext';
import { api } from '../lib/api';
import { money } from '../lib/format';
import type { Banner, ProductDetail, Variant } from '../lib/types';
import { useAsync } from '../lib/useAsync';

const SIZE_ORDER = ['XXS', 'XS', 'S', 'M', 'L', 'XL', 'XXL'];
const sizeRank = (s: string) => (SIZE_ORDER.includes(s) ? SIZE_ORDER.indexOf(s) : 100 + (parseInt(s, 10) || 0));

export function ProductDetailPage() {
  const { slug = '' } = useParams();
  const { data: p, loading, error } = useAsync(() => api<ProductDetail>(`/api/v1/catalog/products/${slug}`), [slug]);
  const promos = useAsync(() => api<Banner[]>('/api/v1/content/banners?placement=PDP_PROMO'), []);
  const cart = useCart();

  const colors = useMemo(() => {
    const seen = new Map<string, string | undefined>();
    p?.variants.forEach((v) => seen.has(v.color) || seen.set(v.color, v.colorHex));
    return [...seen.entries()];
  }, [p]);

  const [color, setColor] = useState<string>();
  const [size, setSize] = useState<string>();
  const [imageIdx, setImageIdx] = useState(0);
  const [added, setAdded] = useState(false);
  const [showChart, setShowChart] = useState(false);

  useEffect(() => {
    // default to the first colour that has stock
    const firstInStock = p?.variants.find((v) => v.stockStatus !== 'OUT_OF_STOCK');
    setColor(firstInStock?.color ?? colors[0]?.[0]);
    setSize(undefined);
    setImageIdx(0);
  }, [p, colors]);

  if (loading) return <div className="container page-pad">Loading…</div>;
  if (error || !p) return <div className="container page-pad">This product isn't available. <Link to="/">Continue shopping</Link></div>;

  const sizesForColour: Variant[] = p.variants.filter((v) => v.color === color).sort((a, b) => sizeRank(a.size) - sizeRank(b.size));
  const selected = sizesForColour.find((v) => v.size === size);
  const images = p.images.length ? p.images : [{ id: 0, url: '', sortOrder: 0 }];
  const price = selected?.price ?? p.price;

  const addToBag = () => {
    if (!selected) return;
    cart.add({
      variantId: selected.id, quantity: 1, productName: p.name, productSlug: p.slug,
      size: selected.size, color: selected.color, price: selected.price, imageUrl: p.images[0]?.url,
    });
    setAdded(true);
    setTimeout(() => setAdded(false), 2500);
  };

  return (
    <div className="container page-pad">
      <nav className="breadcrumbs" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        {p.breadcrumbs.map((b) => <span key={b.slug}> / <Link to={`/c/${b.slug}`}>{b.name}</Link></span>)}
      </nav>

      <div className="pdp">
        <div className="gallery">
          <div className="gallery-main">{images[imageIdx]?.url && <img src={images[imageIdx].url} alt={images[imageIdx].altText ?? p.name} />}</div>
          <div className="gallery-thumbs">
            {p.images.map((img, i) => (
              <button key={img.id} className={i === imageIdx ? 'active' : ''} onClick={() => setImageIdx(i)} aria-label={`View image ${i + 1}`}>
                <img src={img.url} alt="" />
              </button>
            ))}
          </div>
        </div>

        <div className="pdp-info">
          {p.badge && <span className="marketing-badge static">{p.badge}</span>}
          {p.brand && <div className="brand">{p.brand}</div>}
          <h1>{p.name}</h1>
          <div className="price-row big">
            <span className="price">{money(price)}</span>
            {p.compareAtPrice && <s className="was">{money(p.compareAtPrice)}</s>}
            <span className="muted small">Inclusive of all taxes</span>
          </div>

          <div className="option">
            <div className="option-label">Colour: <strong>{color}</strong></div>
            <div className="swatches big">
              {colors.map(([c, hex]) => (
                <button key={c} className={`swatch${c === color ? ' selected' : ''}`} style={{ background: hex ?? '#ccc' }}
                        onClick={() => { setColor(c); setSize(undefined); }} aria-label={c} aria-pressed={c === color} />
              ))}
            </div>
          </div>

          <div className="option">
            <div className="option-label">
              Size {p.sizeChart && <button className="link-btn" onClick={() => setShowChart((s) => !s)}>Size guide</button>}
            </div>
            <div className="size-filter">
              {sizesForColour.map((v) => (
                <button key={v.id} disabled={v.stockStatus === 'OUT_OF_STOCK'}
                        className={`size-chip${v.size === size ? ' selected' : ''}${v.stockStatus === 'OUT_OF_STOCK' ? ' oos' : ''}`}
                        onClick={() => setSize(v.size)} aria-pressed={v.size === size}>
                  {v.size}
                </button>
              ))}
            </div>
            {selected && (
              <div className={`stock-line ${selected.stockStatus.toLowerCase()}`}>
                {selected.stockStatus === 'LOW_STOCK' ? `Hurry, only ${selected.available} left` : 'In stock - ships in 1-2 days'}
              </div>
            )}
            {!p.inStock && <div className="stock-line out_of_stock">Currently out of stock</div>}
          </div>

          {showChart && p.sizeChart && (
            <div className="size-chart">
              <table>
                <thead>
                  <tr>{Object.keys(p.sizeChart.rows[0] ?? {}).map((k) => <th key={k}>{k.replace(/Cm$/, ' (cm)')}</th>)}</tr>
                </thead>
                <tbody>
                  {p.sizeChart.rows.map((r, i) => (
                    <tr key={i}>{Object.values(r).map((v, j) => <td key={j}>{Array.isArray(v) ? v.join('–') : String(v)}</td>)}</tr>
                  ))}
                </tbody>
              </table>
              {p.sizeChart.fitNotes && <p className="muted small">{p.sizeChart.fitNotes}</p>}
            </div>
          )}

          <button className="btn wide" disabled={!selected} onClick={addToBag}>
            {selected ? (added ? 'Added to bag ✓' : 'Add to bag') : 'Select a size'}
          </button>
          {added && <Link to="/cart" className="view-bag">View bag →</Link>}

          {p.marketingHtml && (
            <div className="marketing" dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(p.marketingHtml) }} />
          )}
          <BannerCarousel banners={promos.data ?? []} variant="strip" />

          <details open>
            <summary>Description</summary>
            <p>{p.description}</p>
          </details>
          {p.material && <details><summary>Material</summary><p>{p.material}</p></details>}
          {p.careInstructions && <details><summary>Care</summary><p>{p.careInstructions}</p></details>}
        </div>
      </div>
    </div>
  );
}
