import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth/auth';
import { api, money, qs } from '../lib/api';
import type { Category, Page, ProductRow } from '../lib/types';
import { useAsync } from '../lib/useAsync';
import { PageHeader, Pill } from '../components/ui';

export function ProductsPage() {
  const { isAdmin } = useAuth();
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const status = params.get('status') ?? '';
  const categoryId = params.get('categoryId') ?? '';
  const page = Number(params.get('page') ?? 0);
  const [draft, setDraft] = useState(q);
  const categories = useAsync(() => api<Category[]>('/api/v1/admin/categories'), []);
  const { data, loading } = useAsync(
    () => api<Page<ProductRow>>(`/api/v1/admin/products${qs({ q, status, categoryId, page, size: 25 })}`),
    [q, status, categoryId, page],
  );

  const set = (k: string, v: string) => {
    const next = new URLSearchParams(params);
    v ? next.set(k, v) : next.delete(k);
    if (k !== 'page') next.delete('page');
    setParams(next);
  };

  return (
    <>
      <PageHeader title="Products" actions={isAdmin && <Link className="btn" to="/products/new">New product</Link>} />
      <div className="toolbar">
        <form onSubmit={(e) => { e.preventDefault(); set('q', draft.trim()); }}>
          <input placeholder="Search by name" value={draft} onChange={(e) => setDraft(e.target.value)} />
        </form>
        <select value={categoryId} onChange={(e) => set('categoryId', e.target.value)}>
          <option value="">All categories</option>
          {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name} ({c.slug})</option>)}
        </select>
        <select value={status} onChange={(e) => set('status', e.target.value)}>
          <option value="">Any status</option>
          <option>ACTIVE</option><option>DRAFT</option><option>ARCHIVED</option>
        </select>
      </div>
      {loading && <p>Loading…</p>}
      <table className="table">
        <thead><tr><th /><th>Name</th><th>Category</th><th>Price</th><th>SKUs</th><th>Stock</th><th>Status</th></tr></thead>
        <tbody>
          {data?.content.map((p) => (
            <tr key={p.id}>
              <td>{p.imageUrl && <img className="thumb" src={p.imageUrl} alt="" />}</td>
              <td><Link to={`/products/${p.id}`}>{p.name}</Link><div className="muted small mono">{p.slug}</div></td>
              <td>{p.categoryName}</td>
              <td>{money(p.basePrice)}</td>
              <td>{p.variantCount}</td>
              <td>{p.totalAvailable === 0 ? <Pill tone="red">Out of stock</Pill> : p.totalAvailable}</td>
              <td><Pill tone={p.status === 'ACTIVE' ? 'green' : p.status === 'DRAFT' ? 'grey' : 'red'}>{p.status}</Pill></td>
            </tr>
          ))}
        </tbody>
      </table>
      {data && data.totalPages > 1 && (
        <div className="pager">
          <button className="btn ghost" disabled={page === 0} onClick={() => set('page', String(page - 1))}>Previous</button>
          <span>Page {page + 1} / {data.totalPages}</span>
          <button className="btn ghost" disabled={page + 1 >= data.totalPages} onClick={() => set('page', String(page + 1))}>Next</button>
        </div>
      )}
    </>
  );
}
