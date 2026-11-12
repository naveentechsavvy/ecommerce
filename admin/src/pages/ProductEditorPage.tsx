import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../auth/auth';
import { api, ApiError, del, money, post, put } from '../lib/api';
import type { Category, Gender, ProductAdmin, ProductStatus, VariantAdmin } from '../lib/types';
import { useAsync } from '../lib/useAsync';
import { ErrorBanner, Field, Modal, PageHeader, Pill, num } from '../components/ui';
import { AdjustModal } from './InventoryPage';

interface ProductForm {
  categoryId: string; sizeChartId: string; name: string; slug: string; brand: string; description: string;
  material: string; careInstructions: string; gender: Gender | ''; basePrice: string; compareAtPrice: string;
  status: ProductStatus; popularityScore: string; marketingBadge: string; marketingHtml: string;
}

const toForm = (p?: ProductAdmin): ProductForm => ({
  categoryId: p ? String(p.categoryId) : '', sizeChartId: p?.sizeChartId ? String(p.sizeChartId) : '',
  name: p?.name ?? '', slug: p?.slug ?? '', brand: p?.brand ?? '', description: p?.description ?? '',
  material: p?.material ?? '', careInstructions: p?.careInstructions ?? '', gender: p?.gender ?? '',
  basePrice: p ? String(p.basePrice) : '', compareAtPrice: p?.compareAtPrice ? String(p.compareAtPrice) : '',
  status: p?.status ?? 'DRAFT', popularityScore: String(p?.popularityScore ?? 0),
  marketingBadge: p?.marketingBadge ?? '', marketingHtml: p?.marketingHtml ?? '',
});

export function ProductEditorPage() {
  const { id } = useParams();
  const isNew = id === 'new';
  const { isAdmin } = useAuth();
  const navigate = useNavigate();
  const categories = useAsync(() => api<Category[]>('/api/v1/admin/categories'), []);
  const sizeCharts = useAsync(() => api<{ id: number; name: string }[]>('/api/v1/admin/size-charts'), []);
  const { data: product, setData: setProduct, error } = useAsync(
    () => (isNew ? Promise.resolve(undefined) : api<ProductAdmin>(`/api/v1/admin/products/${id}`)), [id]);

  const [form, setForm] = useState<ProductForm>(toForm());
  const [err, setErr] = useState<string>();
  const [saved, setSaved] = useState(false);
  const [variantModal, setVariantModal] = useState<VariantAdmin | 'new'>();
  const [stockFor, setStockFor] = useState<VariantAdmin>();
  const [imageUrl, setImageUrl] = useState('');

  useEffect(() => setForm(toForm(product)), [product]);

  if (error) return <ErrorBanner error={error.message} />;
  const readOnly = !isAdmin;
  const set = (k: keyof ProductForm) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm({ ...form, [k]: e.target.value });

  const save = async (e: FormEvent) => {
    e.preventDefault();
    setErr(undefined);
    const body = {
      ...form, categoryId: Number(form.categoryId), sizeChartId: num(form.sizeChartId), gender: form.gender || undefined,
      basePrice: Number(form.basePrice), compareAtPrice: num(form.compareAtPrice), popularityScore: Number(form.popularityScore || 0),
      slug: form.slug || undefined,
    };
    try {
      const p = isNew ? await post<ProductAdmin>('/api/v1/admin/products', body) : await put<ProductAdmin>(`/api/v1/admin/products/${id}`, body);
      if (isNew) navigate(`/products/${p.id}`, { replace: true });
      else { setProduct(p); setSaved(true); setTimeout(() => setSaved(false), 2000); }
    } catch (ex) {
      setErr(ex instanceof ApiError ? ex.message : 'Save failed');
    }
  };

  const archive = async () => {
    if (!confirm('Archive this product? It will disappear from the storefront but stay in order history.')) return;
    await del(`/api/v1/admin/products/${id}`);
    setProduct(await api<ProductAdmin>(`/api/v1/admin/products/${id}`));
  };

  const addImage = async (e: FormEvent) => {
    e.preventDefault();
    if (!imageUrl.trim()) return;
    setProduct(await post<ProductAdmin>(`/api/v1/admin/products/${id}/images`, { url: imageUrl.trim() }));
    setImageUrl('');
  };

  return (
    <>
      <PageHeader title={isNew ? 'New product' : product?.name ?? 'Product'}
                  actions={!isNew && product && <Pill tone={product.status === 'ACTIVE' ? 'green' : 'grey'}>{product.status}</Pill>} />
      <form onSubmit={save} className="panel">
        <fieldset disabled={readOnly} className="form-grid-3">
          <Field label="Name"><input required value={form.name} onChange={set('name')} /></Field>
          <Field label="Slug" hint="Blank = generated"><input value={form.slug} onChange={set('slug')} /></Field>
          <Field label="Brand"><input value={form.brand} onChange={set('brand')} /></Field>
          <Field label="Category">
            <select required value={form.categoryId} onChange={set('categoryId')}>
              <option value="">Select…</option>
              {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name} ({c.slug})</option>)}
            </select>
          </Field>
          <Field label="Size chart">
            <select value={form.sizeChartId} onChange={set('sizeChartId')}>
              <option value="">None</option>
              {sizeCharts.data?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <Field label="Gender">
            <select value={form.gender} onChange={set('gender')}>
              <option value="">—</option><option>MEN</option><option>WOMEN</option><option>UNISEX</option><option>KIDS</option>
            </select>
          </Field>
          <Field label="Price (₹)"><input required type="number" step="0.01" min="0" value={form.basePrice} onChange={set('basePrice')} /></Field>
          <Field label="Compare-at price (₹)" hint="Shown struck-through; must exceed price"><input type="number" step="0.01" min="0" value={form.compareAtPrice} onChange={set('compareAtPrice')} /></Field>
          <Field label="Status">
            <select value={form.status} onChange={set('status')}><option>DRAFT</option><option>ACTIVE</option><option>ARCHIVED</option></select>
          </Field>
          <Field label="Material"><input value={form.material} onChange={set('material')} /></Field>
          <Field label="Care instructions"><input value={form.careInstructions} onChange={set('careInstructions')} /></Field>
          <Field label="Popularity score" hint="Drives 'Popularity' sort"><input type="number" value={form.popularityScore} onChange={set('popularityScore')} /></Field>
          <div className="span-3"><Field label="Description"><textarea rows={3} value={form.description} onChange={set('description')} /></Field></div>
          <Field label="Marketing badge" hint="e.g. New, Bestseller"><input value={form.marketingBadge} onChange={set('marketingBadge')} maxLength={60} /></Field>
          <div className="span-2"><Field label="PDP marketing content (HTML, sanitised on render)"><textarea rows={2} value={form.marketingHtml} onChange={set('marketingHtml')} /></Field></div>
        </fieldset>
        <ErrorBanner error={err} />
        {!readOnly && (
          <div className="row-actions">
            <button className="btn">{isNew ? 'Create product' : saved ? 'Saved ✓' : 'Save changes'}</button>
            {!isNew && product?.status !== 'ARCHIVED' && <button type="button" className="btn ghost danger" onClick={archive}>Archive</button>}
          </div>
        )}
      </form>

      {!isNew && product && (
        <>
          <section className="panel">
            <div className="page-header"><h2>Variants (SKUs) &amp; stock</h2>
              {!readOnly && <button className="btn small" onClick={() => setVariantModal('new')}>Add variant</button>}</div>
            <table className="table compact">
              <thead><tr><th>SKU</th><th>Colour</th><th>Size</th><th>Price</th><th className="num">On hand</th><th className="num">Reserved</th><th className="num">Available</th><th>Active</th><th /></tr></thead>
              <tbody>
                {product.variants.map((v) => (
                  <tr key={v.id}>
                    <td className="mono">{v.sku}</td>
                    <td><span className="dot" style={{ background: v.colorHex ?? '#ccc' }} /> {v.color}</td>
                    <td>{v.size}</td>
                    <td>{money(v.priceOverride ?? product.basePrice)}{v.priceOverride != null && <small className="muted"> (override)</small>}</td>
                    <td className="num">{v.onHand}</td>
                    <td className="num">{v.reserved}</td>
                    <td className="num">{v.available === 0 ? <Pill tone="red">0</Pill> : v.available}</td>
                    <td>{v.active ? 'Yes' : 'No'}</td>
                    <td className="row-actions">
                      <button className="btn small ghost" onClick={() => setStockFor(v)}>Stock</button>
                      {!readOnly && <button className="btn small ghost" onClick={() => setVariantModal(v)}>Edit</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>

          <section className="panel">
            <h2>Images</h2>
            <div className="image-grid">
              {product.images.map((img) => (
                <figure key={img.id}>
                  <img src={img.url} alt={img.altText ?? ''} />
                  {!readOnly && <button className="btn small ghost danger" onClick={async () => setProduct(await del<ProductAdmin>(`/api/v1/admin/products/${id}/images/${img.id}`))}>Remove</button>}
                </figure>
              ))}
            </div>
            {!readOnly && (
              <form onSubmit={addImage} className="toolbar">
                <input type="url" placeholder="https://cdn.example.com/image.jpg" value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} />
                <button className="btn small">Add image</button>
              </form>
            )}
            <p className="muted small">Production: upload to object storage (S3/GCS) via a pre-signed URL and store the CDN URL here.</p>
          </section>
        </>
      )}

      {variantModal && product && (
        <VariantForm productId={product.id} variant={variantModal === 'new' ? undefined : variantModal}
                     onClose={() => setVariantModal(undefined)} onSaved={(p) => { setProduct(p); setVariantModal(undefined); }} />
      )}
      {stockFor && (
        <AdjustModal row={{ variantId: stockFor.id, sku: stockFor.sku, onHand: stockFor.onHand, reserved: stockFor.reserved }}
                     onClose={() => setStockFor(undefined)}
                     onSaved={async () => { setStockFor(undefined); setProduct(await api<ProductAdmin>(`/api/v1/admin/products/${id}`)); }} />
      )}
    </>
  );
}

function VariantForm({ productId, variant, onClose, onSaved }: { productId: number; variant?: VariantAdmin; onClose: () => void; onSaved: (p: ProductAdmin) => void }) {
  const [f, setF] = useState({
    sku: variant?.sku ?? '', size: variant?.size ?? '', color: variant?.color ?? '', colorHex: variant?.colorHex ?? '#000000',
    priceOverride: variant?.priceOverride != null ? String(variant.priceOverride) : '', active: variant?.active ?? true, initialStock: '0',
  });
  const [err, setErr] = useState<string>();
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const body = { ...f, priceOverride: num(f.priceOverride), initialStock: Number(f.initialStock || 0) };
    try {
      onSaved(variant
        ? await put<ProductAdmin>(`/api/v1/admin/products/${productId}/variants/${variant.id}`, body)
        : await post<ProductAdmin>(`/api/v1/admin/products/${productId}/variants`, body));
    } catch (ex) {
      setErr(ex instanceof ApiError ? ex.message : 'Save failed');
    }
  };
  return (
    <Modal title={variant ? `Edit ${variant.sku}` : 'Add variant'} onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <Field label="SKU"><input required value={f.sku} onChange={(e) => setF({ ...f, sku: e.target.value })} /></Field>
        <div className="grid-2 tight">
          <Field label="Size"><input required value={f.size} onChange={(e) => setF({ ...f, size: e.target.value })} placeholder="M or 32" /></Field>
          <Field label="Colour"><input required value={f.color} onChange={(e) => setF({ ...f, color: e.target.value })} /></Field>
          <Field label="Swatch"><input type="color" value={f.colorHex} onChange={(e) => setF({ ...f, colorHex: e.target.value })} /></Field>
          <Field label="Price override (₹)" hint="Blank = product price"><input type="number" step="0.01" value={f.priceOverride} onChange={(e) => setF({ ...f, priceOverride: e.target.value })} /></Field>
        </div>
        {!variant && <Field label="Initial stock"><input type="number" min={0} value={f.initialStock} onChange={(e) => setF({ ...f, initialStock: e.target.value })} /></Field>}
        <label className="check"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} /> Active (sellable)</label>
        <ErrorBanner error={err} />
        <button className="btn">Save</button>
      </form>
    </Modal>
  );
}
