import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useCart } from '../cart/CartContext';
import { AddressForm, EMPTY_ADDRESS } from '../components/AddressForm';
import { OrderSummaryBox } from '../components/OrderSummaryBox';
import { api, ApiError, post } from '../lib/api';
import { money } from '../lib/format';
import type { Address, OrderView, PaymentMethod, SavedAddress } from '../lib/types';
import { useQuote } from '../lib/useQuote';

const WALLETS = ['PAYTM', 'PHONEPE', 'AMAZONPAY', 'MOBIKWIK'];

/**
 * Stand-in for the PSP's hosted card fields. In production the card number never
 * touches our JS bundle or backend: the PSP SDK returns a token and we send only that.
 */
function mockTokenize(cardNumber: string): string | null {
  const digits = cardNumber.replace(/\D/g, '');
  if (digits.length < 12 || digits.length > 19) return null;
  const brand = digits.startsWith('4') ? 'visa' : digits.startsWith('5') ? 'mastercard' : digits.startsWith('6') ? 'rupay' : digits.startsWith('3') ? 'amex' : 'card';
  return `tok_${brand}_${digits.slice(-4)}`;
}

export function CheckoutPage() {
  const auth = useAuth();
  const cart = useCart();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const { quote } = useQuote(auth.authenticated ? undefined : email);
  const [saved, setSaved] = useState<SavedAddress[]>([]);
  const [shipping, setShipping] = useState<Address>(EMPTY_ADDRESS);
  const [billingSame, setBillingSame] = useState(true);
  const [billing, setBilling] = useState<Address>(EMPTY_ADDRESS);
  const [method, setMethod] = useState<PaymentMethod>('UPI');
  const [card, setCard] = useState({ number: '', name: '', expiry: '', cvv: '' });
  const [upiId, setUpiId] = useState('');
  const [wallet, setWallet] = useState(WALLETS[0]);
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState<string>();
  // One key per payment attempt: a double-click reuses it, a retry after a decline gets a new one.
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID());

  useEffect(() => {
    if (!auth.authenticated) return;
    api<SavedAddress[]>('/api/v1/me/addresses').then((list) => {
      setSaved(list);
      const def = list.find((a) => a.isDefault) ?? list[0];
      if (def) setShipping(stripId(def));
    }).catch(() => undefined);
  }, [auth.authenticated]);

  if (cart.items.length === 0) {
    return <div className="container page-pad">Your bag is empty. <Link to="/">Continue shopping</Link></div>;
  }

  const placeOrder = async (e: FormEvent) => {
    e.preventDefault();
    setError(undefined);
    let payment: Record<string, string>;
    if (method === 'CREDIT_CARD' || method === 'DEBIT_CARD') {
      const token = mockTokenize(card.number);
      if (!token) return setError('Please enter a valid card number.');
      if (!/^\d{2}\/\d{2}$/.test(card.expiry) || !/^\d{3,4}$/.test(card.cvv)) return setError('Please check the expiry (MM/YY) and CVV.');
      payment = { method, token };
    } else if (method === 'UPI') {
      payment = { method, upiId: upiId.trim() };
    } else {
      payment = { method, walletProvider: wallet };
    }

    setPlacing(true);
    try {
      const order = await post<OrderView>('/api/v1/checkout/orders', {
        items: cart.items.map((i) => ({ variantId: i.variantId, quantity: i.quantity })),
        couponCode: cart.couponCode || undefined,
        email: auth.authenticated ? undefined : email,
        shippingAddress: shipping,
        billingAddress: billingSame ? undefined : billing,
        billingSameAsShipping: billingSame,
        payment,
        idempotencyKey,
      });
      if (order.status === 'PAYMENT_FAILED') {
        const reason = order.payments[0]?.failureReason ?? 'Payment was declined';
        setError(`${reason}. Your bag is saved - try another payment method.`);
        setIdempotencyKey(crypto.randomUUID());
        return;
      }
      cart.clear();
      navigate(`/order/${order.orderNumber}`, { state: { order } });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not place the order. Please try again.');
    } finally {
      setPlacing(false);
    }
  };

  return (
    <form className="container page-pad checkout" onSubmit={placeOrder}>
      <h1>Checkout</h1>
      <div className="two-col">
        <div className="checkout-steps">
          <section className="card">
            <h2>1. Contact</h2>
            {auth.authenticated ? (
              <p>Signed in as <strong>{auth.email}</strong></p>
            ) : (
              <>
                <label className="field">
                  <span>Email for order updates</span>
                  <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
                </label>
                <p className="muted small">
                  Checking out as a guest. <button type="button" className="link-btn" onClick={auth.login}>Sign in</button> for
                  saved addresses and member coupons.
                </p>
              </>
            )}
          </section>

          <section className="card">
            <h2>2. Shipping address</h2>
            {saved.length > 0 && (
              <div className="saved-addresses">
                {saved.map((a) => (
                  <label key={a.id} className="radio-card">
                    <input type="radio" name="saved" onChange={() => setShipping(stripId(a))} defaultChecked={a.isDefault} />
                    <span><strong>{a.label ?? a.fullName}</strong> · {a.line1}, {a.city} {a.postalCode}</span>
                  </label>
                ))}
              </div>
            )}
            <AddressForm idPrefix="ship" value={shipping} onChange={setShipping} />
            <label className="check">
              <input type="checkbox" checked={billingSame} onChange={(e) => setBillingSame(e.target.checked)} />
              Billing address same as shipping
            </label>
            {!billingSame && (
              <>
                <h3>Billing address</h3>
                <AddressForm idPrefix="bill" value={billing} onChange={setBilling} />
              </>
            )}
          </section>

          <section className="card">
            <h2>3. Payment</h2>
            <div className="pay-methods" role="radiogroup">
              {([['UPI', 'UPI'], ['CREDIT_CARD', 'Credit card'], ['DEBIT_CARD', 'Debit card'], ['WALLET', 'Wallet']] as const).map(([m, label]) => (
                <label key={m} className={`radio-card${method === m ? ' selected' : ''}`}>
                  <input type="radio" name="method" checked={method === m} onChange={() => setMethod(m)} /> {label}
                </label>
              ))}
            </div>

            {method === 'UPI' && (
              <label className="field">
                <span>UPI ID</span>
                <input required value={upiId} onChange={(e) => setUpiId(e.target.value)} placeholder="name@okbank" pattern="[a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,}" />
              </label>
            )}
            {(method === 'CREDIT_CARD' || method === 'DEBIT_CARD') && (
              <div className="form-grid">
                <label className="field"><span>Card number</span>
                  <input required inputMode="numeric" autoComplete="cc-number" value={card.number}
                         onChange={(e) => setCard({ ...card, number: e.target.value })} placeholder="4242 4242 4242 4242" /></label>
                <label className="field"><span>Name on card</span>
                  <input required autoComplete="cc-name" value={card.name} onChange={(e) => setCard({ ...card, name: e.target.value })} /></label>
                <label className="field"><span>Expiry (MM/YY)</span>
                  <input required autoComplete="cc-exp" value={card.expiry} onChange={(e) => setCard({ ...card, expiry: e.target.value })} placeholder="12/28" /></label>
                <label className="field"><span>CVV</span>
                  <input required inputMode="numeric" autoComplete="cc-csc" type="password" value={card.cvv} onChange={(e) => setCard({ ...card, cvv: e.target.value })} /></label>
              </div>
            )}
            {method === 'WALLET' && (
              <label className="field"><span>Wallet</span>
                <select value={wallet} onChange={(e) => setWallet(e.target.value)}>
                  {WALLETS.map((w) => <option key={w}>{w}</option>)}
                </select>
              </label>
            )}
            <p className="muted small">
              Demo payments: any card succeeds except one ending in 0002; UPI <code>fail@upi</code> is declined.
            </p>
          </section>
        </div>

        <OrderSummaryBox quote={quote}>
          {quote && (
            <ul className="mini-lines">
              {quote.lines.map((l) => <li key={l.variantId}>{l.quantity} × {l.productName} ({l.size}, {l.color}) <span>{money(l.lineTotal)}</span></li>)}
            </ul>
          )}
          {error && <div className="error" role="alert">{error}</div>}
          <button className="btn wide" disabled={placing || !quote || quote.issues.length > 0}>
            {placing ? 'Placing order…' : `Pay ${quote ? money(quote.total) : ''}`}
          </button>
        </OrderSummaryBox>
      </div>
    </form>
  );
}

function stripId(a: SavedAddress): Address {
  const { fullName, phone, line1, line2, city, state, postalCode, country } = a;
  return { fullName, phone, line1, line2, city, state, postalCode, country };
}
