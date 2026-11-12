import { Link } from 'react-router-dom';
import { money } from '../lib/format';
import type { ProductCard as Card } from '../lib/types';

export function StockBadge({ inStock, lowStock }: { inStock: boolean; lowStock?: boolean }) {
  if (!inStock) return <span className="stock-badge out">Out of stock</span>;
  if (lowStock) return <span className="stock-badge low">Few left</span>;
  return <span className="stock-badge in">In stock</span>;
}

export function ProductCard({ p }: { p: Card }) {
  const off = p.compareAtPrice ? Math.round((1 - p.price / p.compareAtPrice) * 100) : 0;
  return (
    <Link to={`/p/${p.slug}`} className={`product-card${p.inStock ? '' : ' is-oos'}`}>
      <div className="product-media">
        {p.imageUrl && <img src={p.imageUrl} alt={p.name} loading="lazy" />}
        {p.badge && <span className="marketing-badge">{p.badge}</span>}
      </div>
      <div className="product-info">
        {p.brand && <div className="brand">{p.brand}</div>}
        <div className="name">{p.name}</div>
        <div className="price-row">
          <span className="price">{money(p.price)}</span>
          {p.compareAtPrice && <s className="was">{money(p.compareAtPrice)}</s>}
          {off > 0 && <span className="off">{off}% off</span>}
        </div>
        <div className="card-foot">
          <span className="swatches" aria-label={`Colours: ${p.colors.join(', ')}`}>
            {p.colorHexes.slice(0, 5).map((hex, i) => (
              <span key={i} className="swatch" style={{ background: hex ?? '#ccc' }} title={p.colors[i]} />
            ))}
          </span>
          <StockBadge inStock={p.inStock} lowStock={p.lowStock} />
        </div>
      </div>
    </Link>
  );
}
