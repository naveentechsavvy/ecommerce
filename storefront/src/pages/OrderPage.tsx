import { useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { api, post } from '../lib/api';
import { date, money, statusLabel } from '../lib/format';
import type { OrderView } from '../lib/types';
import { useAsync } from '../lib/useAsync';

/** Order confirmation (from checkout state) or a signed-in customer's order detail. */
export function OrderPage() {
  const { orderNumber = '' } = useParams();
  const location = useLocation();
  const auth = useAuth();
  const fromCheckout = (location.state as { order?: OrderView } | null)?.order;

  const { data, error, setData } = useAsync<OrderView | undefined>(
    () => (fromCheckout ? Promise.resolve(fromCheckout)
      : auth.authenticated ? api<OrderView>(`/api/v1/me/orders/${orderNumber}`) : Promise.resolve(undefined)),
    [orderNumber, auth.authenticated],
  );
  const [cancelling, setCancelling] = useState(false);

  if (error) return <div className="container page-pad">Order not found. <Link to="/track">Track an order</Link></div>;
  if (!data) {
    return auth.ready && !auth.authenticated
      ? <div className="container page-pad">Sign in, or use <Link to="/track">Track order</Link> with your email.</div>
      : <div className="container page-pad">Loading…</div>;
  }
  return <OrderDetail order={data} justPlaced={!!fromCheckout} onCancel={auth.authenticated && data.status === 'PAID' ? async () => {
    setCancelling(true);
    try { setData(await post<OrderView>(`/api/v1/me/orders/${data.orderNumber}/cancel`, {})); } finally { setCancelling(false); }
  } : undefined} cancelling={cancelling} />;
}

export function OrderDetail({ order, justPlaced, onCancel, cancelling }: {
  order: OrderView; justPlaced?: boolean; onCancel?: () => void; cancelling?: boolean;
}) {
  const a = order.shippingAddress;
  return (
    <div className="container page-pad order">
      {justPlaced && (
        <div className="success-banner">
          <h1>Thank you! Your order is confirmed.</h1>
          <p>We've sent the details to {order.email}. Save your order number to track it: <strong>{order.orderNumber}</strong></p>
        </div>
      )}
      <div className="order-head">
        <h2>Order {order.orderNumber}</h2>
        <span className={`status status-${order.status.toLowerCase()}`}>{statusLabel(order.status)}</span>
        <span className="muted">Placed {date(order.createdAt)}</span>
      </div>
      {order.trackingNumber && <p>Tracking number: <strong>{order.trackingNumber}</strong></p>}

      <div className="two-col">
        <div>
          <ul className="cart-lines">
            {order.items.map((i) => (
              <li key={i.variantId} className="cart-line">
                {i.imageUrl && <img src={i.imageUrl} alt="" />}
                <div className="cart-line-info">
                  <Link to={`/p/${i.productSlug}`}>{i.productName}</Link>
                  <div className="muted small">{i.color} · {i.size} · Qty {i.quantity}</div>
                </div>
                <div className="cart-line-price">{money(i.lineTotal)}</div>
              </li>
            ))}
          </ul>
          <h3>Timeline</h3>
          <ol className="timeline">
            {order.history.map((h, idx) => (
              <li key={idx}><strong>{statusLabel(h.toStatus)}</strong> · {date(h.changedAt)}{h.note ? ` - ${h.note}` : ''}</li>
            ))}
          </ol>
        </div>
        <aside className="summary">
          <dl className="totals">
            <dt>Subtotal</dt><dd>{money(order.subtotal)}</dd>
            {order.discountTotal > 0 && (<><dt>Discount {order.couponCode && `(${order.couponCode})`}</dt><dd>−{money(order.discountTotal)}</dd></>)}
            <dt>Shipping</dt><dd>{order.shippingTotal === 0 ? 'Free' : money(order.shippingTotal)}</dd>
            <dt className="total">Total</dt><dd className="total">{money(order.grandTotal)}</dd>
          </dl>
          <p className="small">Paid via {order.payments[0]?.instrument ?? statusLabel(order.paymentMethod ?? '')}</p>
          <h4>Shipping to</h4>
          <address>{a.fullName}<br />{a.line1}{a.line2 ? `, ${a.line2}` : ''}<br />{a.city}, {a.state} {a.postalCode}<br />{a.phone}</address>
          {onCancel && <button className="btn ghost wide" onClick={onCancel} disabled={cancelling}>{cancelling ? 'Cancelling…' : 'Cancel order'}</button>}
        </aside>
      </div>
    </div>
  );
}
