import { useState, type FormEvent } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { api, ApiError, dateTime, label, money, patch, qs } from '../lib/api';
import { ORDER_STATUSES, type OrderDetail, type OrderStatus, type OrderSummary, type Page } from '../lib/types';
import { useAsync } from '../lib/useAsync';
import { ErrorBanner, Field, PageHeader, Pill } from '../components/ui';

const TONE: Record<OrderStatus, 'green' | 'amber' | 'red' | 'blue' | 'grey'> = {
  PENDING_PAYMENT: 'grey', PAYMENT_FAILED: 'red', PAID: 'blue', PROCESSING: 'blue',
  SHIPPED: 'amber', DELIVERED: 'green', CANCELLED: 'red', RETURNED: 'red',
};

export const StatusPill = ({ status }: { status: OrderStatus }) => <Pill tone={TONE[status]}>{label(status)}</Pill>;

export function OrdersPage() {
  const [params, setParams] = useSearchParams();
  const status = params.get('status') ?? '';
  const q = params.get('q') ?? '';
  const page = Number(params.get('page') ?? 0);
  const [draft, setDraft] = useState(q);
  const { data, loading } = useAsync(
    () => api<Page<OrderSummary>>(`/api/v1/admin/orders${qs({ status, q, page, size: 25 })}`),
    [status, q, page],
  );

  const set = (k: string, v: string) => {
    const next = new URLSearchParams(params);
    v ? next.set(k, v) : next.delete(k);
    if (k !== 'page') next.delete('page');
    setParams(next);
  };

  return (
    <>
      <PageHeader title="Orders" />
      <div className="toolbar">
        <form onSubmit={(e) => { e.preventDefault(); set('q', draft.trim()); }}>
          <input placeholder="Order number or email" value={draft} onChange={(e) => setDraft(e.target.value)} />
        </form>
        <select value={status} onChange={(e) => set('status', e.target.value)}>
          <option value="">All statuses</option>
          {ORDER_STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>
      {loading && <p>Loading…</p>}
      <table className="table">
        <thead><tr><th>Order</th><th>Customer</th><th>Items</th><th>Total</th><th>Status</th><th>Placed</th></tr></thead>
        <tbody>
          {data?.content.map((o) => (
            <tr key={o.orderNumber}>
              <td><Link to={`/orders/${o.orderNumber}`}>{o.orderNumber}</Link></td>
              <td>{o.email} {o.guest && <small className="muted">(guest)</small>}</td>
              <td>{o.itemCount}</td>
              <td>{money(o.grandTotal)}</td>
              <td><StatusPill status={o.status} /></td>
              <td>{dateTime(o.createdAt)}</td>
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

export function OrderDetailPage() {
  const { orderNumber = '' } = useParams();
  const { data: order, setData, error } = useAsync(() => api<OrderDetail>(`/api/v1/admin/orders/${orderNumber}`), [orderNumber]);
  const [next, setNext] = useState<OrderStatus | ''>('');
  const [note, setNote] = useState('');
  const [tracking, setTracking] = useState('');
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string>();

  if (error) return <ErrorBanner error={error.message} />;
  if (!order) return <p>Loading…</p>;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!next) return;
    if (next === 'CANCELLED' && !confirm('Cancel this order? Stock will be returned and any payment refunded.')) return;
    setSaving(true);
    setErr(undefined);
    try {
      setData(await patch<OrderDetail>(`/api/v1/admin/orders/${orderNumber}/status`, { status: next, note: note || undefined, trackingNumber: tracking || undefined }));
      setNext(''); setNote(''); setTracking('');
    } catch (ex) {
      setErr(ex instanceof ApiError ? ex.message : 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  const addr = (a: OrderDetail['shippingAddress']) => (
    <address>{a.fullName}<br />{a.line1}{a.line2 ? `, ${a.line2}` : ''}<br />{a.city}, {a.state} {a.postalCode} {a.country}<br />{a.phone}</address>
  );

  return (
    <>
      <PageHeader title={`Order ${order.orderNumber}`} actions={<StatusPill status={order.status} />} />
      <div className="grid-2">
        <section className="panel">
          <h3>Items</h3>
          <table className="table compact">
            <thead><tr><th>SKU</th><th>Product</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead>
            <tbody>
              {order.items.map((i) => (
                <tr key={i.variantId}><td>{i.sku}</td><td>{i.productName} <small className="muted">{i.color} / {i.size}</small></td><td>{i.quantity}</td><td>{money(i.unitPrice)}</td><td>{money(i.lineTotal)}</td></tr>
              ))}
            </tbody>
          </table>
          <dl className="kv">
            <dt>Subtotal</dt><dd>{money(order.subtotal)}</dd>
            <dt>Discount {order.couponCode && `(${order.couponCode})`}</dt><dd>−{money(order.discountTotal)}</dd>
            <dt>Shipping</dt><dd>{money(order.shippingTotal)}</dd>
            <dt><strong>Total</strong></dt><dd><strong>{money(order.grandTotal)}</strong></dd>
          </dl>
          <h3>Payments</h3>
          {order.payments.map((p, i) => (
            <p key={i}>{label(p.method)} · {p.instrument} · <strong>{p.status}</strong> · {money(p.amount)} {p.providerRef && <small className="muted">ref {p.providerRef}</small>} {p.failureReason && <span className="error">{p.failureReason}</span>}</p>
          ))}
        </section>
        <section className="panel">
          <h3>Customer</h3>
          <p>{order.email} {order.guest ? <Pill tone="grey">Guest</Pill> : <Pill tone="blue">Registered</Pill>}</p>
          <div className="grid-2 tight">
            <div><h4>Ship to</h4>{addr(order.shippingAddress)}</div>
            <div><h4>Bill to</h4>{addr(order.billingAddress)}</div>
          </div>
          {order.trackingNumber && <p>Tracking: <strong>{order.trackingNumber}</strong></p>}

          <h3>Update status</h3>
          {order.allowedNextStatuses.length === 0 ? <p className="muted">This order is in a final state.</p> : (
            <form onSubmit={submit} className="stack">
              <Field label="New status">
                <select value={next} onChange={(e) => setNext(e.target.value as OrderStatus)} required>
                  <option value="">Select…</option>
                  {order.allowedNextStatuses.map((s) => <option key={s} value={s}>{label(s)}</option>)}
                </select>
              </Field>
              {next === 'SHIPPED' && <Field label="Tracking number"><input required value={tracking} onChange={(e) => setTracking(e.target.value)} /></Field>}
              <Field label="Note (optional)"><input value={note} onChange={(e) => setNote(e.target.value)} maxLength={300} /></Field>
              <ErrorBanner error={err} />
              <button className="btn" disabled={saving || !next}>{saving ? 'Saving…' : 'Update'}</button>
            </form>
          )}

          <h3>History</h3>
          <ol className="timeline">
            {order.history.map((h, i) => (
              <li key={i}><strong>{label(h.toStatus)}</strong> · {dateTime(h.changedAt)} · <small>{h.changedBy}</small>{h.note && <div className="muted small">{h.note}</div>}</li>
            ))}
          </ol>
        </section>
      </div>
    </>
  );
}
