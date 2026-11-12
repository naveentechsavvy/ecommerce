import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, ApiError, dateTime, post, qs } from '../lib/api';
import type { InventoryRow, Page } from '../lib/types';
import { useAsync } from '../lib/useAsync';
import { ErrorBanner, Field, Modal, PageHeader, Pill, num } from '../components/ui';

const REASONS = ['RESTOCK', 'ADJUSTMENT', 'DAMAGE', 'RETURN'] as const;

export function InventoryPage() {
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const lowStockOnly = params.get('lowStockOnly') === 'true';
  const page = Number(params.get('page') ?? 0);
  const [draft, setDraft] = useState(q);
  const [editing, setEditing] = useState<InventoryRow>();
  const { data, setData, loading } = useAsync(
    () => api<Page<InventoryRow>>(`/api/v1/admin/inventory${qs({ q, lowStockOnly, page, size: 50 })}`),
    [q, lowStockOnly, page],
  );

  const set = (k: string, v: string) => {
    const next = new URLSearchParams(params);
    v ? next.set(k, v) : next.delete(k);
    if (k !== 'page') next.delete('page');
    setParams(next);
  };

  const replaceRow = (row: InventoryRow) =>
    setData((d) => d && { ...d, content: d.content.map((r) => (r.variantId === row.variantId ? row : r)) });

  return (
    <>
      <PageHeader title="Inventory" />
      <div className="toolbar">
        <form onSubmit={(e) => { e.preventDefault(); set('q', draft.trim()); }}>
          <input placeholder="Search SKU or product" value={draft} onChange={(e) => setDraft(e.target.value)} />
        </form>
        <label className="check"><input type="checkbox" checked={lowStockOnly} onChange={(e) => set('lowStockOnly', e.target.checked ? 'true' : '')} /> Low stock only</label>
      </div>
      {loading && <p>Loading…</p>}
      <table className="table">
        <thead><tr><th>SKU</th><th>Product</th><th>Variant</th><th className="num">On hand</th><th className="num">Reserved</th><th className="num">Available</th><th>Updated</th><th /></tr></thead>
        <tbody>
          {data?.content.map((r) => (
            <tr key={r.variantId}>
              <td className="mono">{r.sku}</td>
              <td><Link to={`/products/${r.productId}`}>{r.productName}</Link></td>
              <td>{r.color} / {r.size}</td>
              <td className="num">{r.onHand}</td>
              <td className="num">{r.reserved}</td>
              <td className="num">
                {r.available === 0 ? <Pill tone="red">0</Pill> : r.lowStock ? <Pill tone="amber">{r.available}</Pill> : r.available}
              </td>
              <td className="muted small">{dateTime(r.updatedAt)}</td>
              <td><button className="btn small ghost" onClick={() => setEditing(r)}>Adjust</button></td>
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
      {editing && <AdjustModal row={editing} onClose={() => setEditing(undefined)} onSaved={(r) => { replaceRow(r); setEditing(undefined); }} />}
    </>
  );
}

type StockTarget = { variantId: number; sku: string; onHand: number; reserved: number; lowStockThreshold?: number };

export function AdjustModal({ row, onClose, onSaved }: { row: StockTarget; onClose: () => void; onSaved: (r: InventoryRow) => void }) {
  const [mode, setMode] = useState<'delta' | 'set'>('delta');
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState<(typeof REASONS)[number]>('RESTOCK');
  const [note, setNote] = useState('');
  // blank = leave the stored threshold unchanged
  const [threshold, setThreshold] = useState(row.lowStockThreshold != null ? String(row.lowStockThreshold) : '');
  const [err, setErr] = useState<string>();

  const n = Number(amount || 0);
  const delta = mode === 'delta' ? n : n - row.onHand;
  const after = row.onHand + delta;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setErr(undefined);
    try {
      onSaved(await post<InventoryRow>(`/api/v1/admin/inventory/${row.variantId}/adjustments`, {
        delta, reason: delta === 0 ? 'ADJUSTMENT' : reason, note: note || undefined, lowStockThreshold: num(threshold),
      }));
    } catch (ex) {
      setErr(ex instanceof ApiError ? ex.message : 'Adjustment failed');
    }
  };

  return (
    <Modal title={`Adjust stock · ${row.sku}`} onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <p className="muted">On hand {row.onHand} · reserved {row.reserved} (reserved units belong to open checkouts and can't be removed)</p>
        <div className="seg">
          <button type="button" className={mode === 'delta' ? 'on' : ''} onClick={() => setMode('delta')}>Add / remove</button>
          <button type="button" className={mode === 'set' ? 'on' : ''} onClick={() => setMode('set')}>Set on-hand to</button>
        </div>
        <Field label={mode === 'delta' ? 'Quantity (+/−)' : 'New on-hand quantity'}>
          <input type="number" required value={amount} onChange={(e) => setAmount(e.target.value)} autoFocus />
        </Field>
        <Field label="Reason">
          <select value={reason} onChange={(e) => setReason(e.target.value as (typeof REASONS)[number])}>
            {REASONS.map((r) => <option key={r}>{r}</option>)}
          </select>
        </Field>
        <Field label="Note"><input value={note} onChange={(e) => setNote(e.target.value)} maxLength={300} placeholder="PO number, damage report…" /></Field>
        <Field label="Low-stock alert threshold" hint="Leave blank to keep the current threshold">
          <input type="number" min={0} value={threshold} onChange={(e) => setThreshold(e.target.value)} />
        </Field>
        <p>Result: on hand <strong>{after}</strong> ({delta >= 0 ? '+' : ''}{delta})</p>
        <ErrorBanner error={err} />
        <button className="btn" disabled={after < row.reserved}>Save adjustment</button>
      </form>
    </Modal>
  );
}
