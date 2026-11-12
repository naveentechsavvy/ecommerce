import { Link, useParams, useSearchParams } from 'react-router-dom';
import { BannerCarousel } from '../components/BannerCarousel';
import { ProductCard } from '../components/ProductCard';
import { api, qs } from '../lib/api';
import type { Banner, CategoryNode, Facets, Page, ProductCard as Card } from '../lib/types';
import { useAsync } from '../lib/useAsync';

const SORTS = [
  { value: 'newest', label: 'Newest' },
  { value: 'popularity', label: 'Popularity' },
  { value: 'price_asc', label: 'Price: Low to High' },
  { value: 'price_desc', label: 'Price: High to Low' },
];

/** PLP for /c/:slug and /search?q=. All filter state lives in the URL so pages are shareable. */
export function ProductListPage() {
  const { slug } = useParams();
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? undefined;
  const sort = params.get('sort') ?? 'newest';
  const sizes = params.getAll('size');
  const colors = params.getAll('color');
  const inStock = params.get('inStock') === 'true';
  const maxPrice = params.get('maxPrice') ?? undefined;
  const page = Number(params.get('page') ?? 0);

  const category = useAsync(
    () => (slug ? api<CategoryNode>(`/api/v1/catalog/categories/${slug}`) : Promise.resolve(undefined)),
    [slug],
  );
  const facets = useAsync(() => api<Facets>(`/api/v1/catalog/facets${qs({ category: slug })}`), [slug]);
  const banners = useAsync(() => api<Banner[]>('/api/v1/content/banners?placement=PLP_TOP'), []);
  const products = useAsync(
    () => api<Page<Card>>(`/api/v1/catalog/products${qs({ category: slug, q, sort, size: sizes, color: colors, inStock: inStock || undefined, maxPrice, page, pageSize: 24 })}`),
    [slug, params.toString()],
  );

  const update = (mut: (p: URLSearchParams) => void) => {
    const next = new URLSearchParams(params);
    mut(next);
    next.delete('page');
    setParams(next);
  };
  const toggle = (key: string, value: string) =>
    update((p) => {
      const values = p.getAll(key);
      p.delete(key);
      (values.includes(value) ? values.filter((v) => v !== value) : [...values, value]).forEach((v) => p.append(key, v));
    });

  if (category.error) return <div className="container page-pad">This category isn't available.</div>;

  const title = q ? `Results for “${q}”` : category.data?.name ?? '';

  return (
    <div className="container page-pad">
      {!q && <BannerCarousel banners={banners.data ?? []} variant="strip" />}
      <div className="plp-head">
        <h1>{title}</h1>
        {products.data && <span className="muted">{products.data.totalElements} items</span>}
        <label className="sort">
          Sort by{' '}
          <select value={sort} onChange={(e) => update((p) => p.set('sort', e.target.value))}>
            {SORTS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        </label>
      </div>

      {category.data && category.data.children.length > 0 && (
        <div className="chips">
          {category.data.children.filter((c) => c.visibility === 'ENABLED').map((c) => (
            <Link key={c.id} to={`/c/${c.slug}`} className="chip">{c.name}</Link>
          ))}
        </div>
      )}

      <div className="plp">
        <aside className="filters" aria-label="Filters">
          <label className="check">
            <input type="checkbox" checked={inStock}
                   onChange={(e) => update((p) => (e.target.checked ? p.set('inStock', 'true') : p.delete('inStock')))} />
            In stock only
          </label>
          <h4>Max price</h4>
          <select value={maxPrice ?? ''} onChange={(e) => update((p) => (e.target.value ? p.set('maxPrice', e.target.value) : p.delete('maxPrice')))}>
            <option value="">Any</option>
            {[999, 1499, 1999, 2999].map((v) => <option key={v} value={v}>Under ₹{v}</option>)}
          </select>
          {facets.data && facets.data.sizes.length > 0 && (
            <>
              <h4>Size</h4>
              <div className="size-filter">
                {facets.data.sizes.map((s) => (
                  <button key={s} className={`size-chip${sizes.includes(s) ? ' selected' : ''}`} onClick={() => toggle('size', s)}>{s}</button>
                ))}
              </div>
            </>
          )}
          {facets.data && facets.data.colors.length > 0 && (
            <>
              <h4>Colour</h4>
              {facets.data.colors.map((c) => (
                <label key={c} className="check">
                  <input type="checkbox" checked={colors.includes(c)} onChange={() => toggle('color', c)} /> {c}
                </label>
              ))}
            </>
          )}
          {(sizes.length > 0 || colors.length > 0 || inStock || maxPrice) && (
            <button className="link-btn" onClick={() => update((p) => ['size', 'color', 'inStock', 'maxPrice'].forEach((k) => p.delete(k)))}>
              Clear filters
            </button>
          )}
        </aside>

        <section className="grid-wrap">
          {products.loading && <div>Loading…</div>}
          {products.data && products.data.content.length === 0 && <div className="empty">No products match these filters.</div>}
          <div className="grid">{products.data?.content.map((p) => <ProductCard key={p.id} p={p} />)}</div>
          {products.data && products.data.totalPages > 1 && (
            <div className="pager">
              <button className="btn ghost" disabled={page === 0} onClick={() => setParams((p) => { p.set('page', String(page - 1)); return p; })}>Previous</button>
              <span>Page {page + 1} of {products.data.totalPages}</span>
              <button className="btn ghost" disabled={page + 1 >= products.data.totalPages} onClick={() => setParams((p) => { p.set('page', String(page + 1)); return p; })}>Next</button>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
