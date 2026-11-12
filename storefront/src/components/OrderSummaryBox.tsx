import { useState, type FormEvent } from 'react';
import { useCart } from '../cart/CartContext';
import { money } from '../lib/format';
import type { Quote } from '../lib/types';

/** Totals + coupon entry, shared by bag and checkout. */
export function OrderSummaryBox({ quote, children }: { quote?: Quote; children?: React.ReactNode }) {
  const { couponCode, setCouponCode } = useCart();
  const [draft, setDraft] = useState(couponCode);

  const apply = (e: FormEvent) => {
    e.preventDefault();
    setCouponCode(draft.trim().toUpperCase());
  };

  const coupon = quote?.coupon;
  return (
    <aside className="summary">
      <h3>Order summary</h3>
      <form className="coupon" onSubmit={apply}>
        <input value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="Coupon code" aria-label="Coupon code" />
        {couponCode ? (
          <button type="button" className="btn ghost" onClick={() => { setCouponCode(''); setDraft(''); }}>Remove</button>
        ) : (
          <button className="btn ghost" disabled={!draft.trim()}>Apply</button>
        )}
      </form>
      {couponCode && coupon && (
        <div className={`coupon-msg ${coupon.applied ? 'ok' : 'bad'}`}>
          {coupon.applied ? `${coupon.code} applied${coupon.message ? ` - ${coupon.message}` : ''}` : coupon.message}
        </div>
      )}
      {quote && (
        <dl className="totals">
          <dt>Subtotal</dt><dd>{money(quote.subtotal)}</dd>
          {quote.discount > 0 && (<><dt>Discount</dt><dd className="ok">−{money(quote.discount)}</dd></>)}
          <dt>Shipping</dt><dd>{quote.shipping === 0 ? 'Free' : money(quote.shipping)}</dd>
          <dt className="total">Total</dt><dd className="total">{money(quote.total)}</dd>
        </dl>
      )}
      <p className="muted small">Prices include GST. Free shipping on orders over ₹999.</p>
      {children}
    </aside>
  );
}
