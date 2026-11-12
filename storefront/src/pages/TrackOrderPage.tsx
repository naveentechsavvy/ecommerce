import { useState, type FormEvent } from 'react';
import { api, ApiError, qs } from '../lib/api';
import type { OrderView } from '../lib/types';
import { OrderDetail } from './OrderPage';

/** Guest order lookup by order number + checkout email. */
export function TrackOrderPage() {
  const [orderNumber, setOrderNumber] = useState('');
  const [email, setEmail] = useState('');
  const [order, setOrder] = useState<OrderView>();
  const [error, setError] = useState<string>();

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(undefined);
    try {
      setOrder(await api<OrderView>(`/api/v1/orders/track${qs({ orderNumber: orderNumber.trim(), email: email.trim() })}`));
    } catch (err) {
      setOrder(undefined);
      setError(err instanceof ApiError && err.status === 404 ? 'No order matches those details.' : 'Something went wrong. Please try again.');
    }
  };

  if (order) return <OrderDetail order={order} />;

  return (
    <div className="container page-pad narrow">
      <h1>Track your order</h1>
      <form onSubmit={submit} className="stack">
        <label className="field"><span>Order number</span>
          <input required value={orderNumber} onChange={(e) => setOrderNumber(e.target.value)} placeholder="AC250101-ABC123" /></label>
        <label className="field"><span>Email used at checkout</span>
          <input required type="email" value={email} onChange={(e) => setEmail(e.target.value)} /></label>
        {error && <div className="error">{error}</div>}
        <button className="btn">Track</button>
      </form>
    </div>
  );
}
