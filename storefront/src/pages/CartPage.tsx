import { Link, useNavigate } from 'react-router-dom';
import { OrderSummaryBox } from '../components/OrderSummaryBox';
import { useCart } from '../cart/CartContext';
import { money } from '../lib/format';
import { useQuote } from '../lib/useQuote';

export function CartPage() {
  const cart = useCart();
  const { quote, error } = useQuote();
  const navigate = useNavigate();

  if (cart.items.length === 0) {
    return (
      <div className="container page-pad empty">
        <h1>Your bag is empty</h1>
        <Link to="/" className="btn">Start shopping</Link>
      </div>
    );
  }

  const lineFor = (variantId: number) => quote?.lines.find((l) => l.variantId === variantId);

  return (
    <div className="container page-pad">
      <h1>Bag ({cart.count})</h1>
      {error && <div className="error">{error}</div>}
      {quote && quote.issues.length > 0 && (
        <div className="notice">Some items need attention before checkout: {quote.issues.join(' ')}</div>
      )}
      <div className="two-col">
        <ul className="cart-lines">
          {cart.items.map((item) => {
            const line = lineFor(item.variantId);
            return (
              <li key={item.variantId} className="cart-line">
                {item.imageUrl && <img src={item.imageUrl} alt="" />}
                <div className="cart-line-info">
                  <Link to={`/p/${item.productSlug}`}>{item.productName}</Link>
                  <div className="muted small">{item.color} · {item.size}</div>
                  {line?.issue && <div className="error small">{line.issue}</div>}
                  <div className="qty">
                    <button onClick={() => cart.setQuantity(item.variantId, item.quantity - 1)} aria-label="Decrease">−</button>
                    <span>{item.quantity}</span>
                    <button onClick={() => cart.setQuantity(item.variantId, item.quantity + 1)} aria-label="Increase"
                            disabled={item.quantity >= 10 || (line != null && item.quantity >= line.available)}>+</button>
                    <button className="link-btn" onClick={() => cart.remove(item.variantId)}>Remove</button>
                  </div>
                </div>
                <div className="cart-line-price">{money(line?.lineTotal ?? item.price * item.quantity)}</div>
              </li>
            );
          })}
        </ul>
        <OrderSummaryBox quote={quote}>
          <button className="btn wide" disabled={!quote || quote.issues.length > 0} onClick={() => navigate('/checkout')}>
            Checkout
          </button>
        </OrderSummaryBox>
      </div>
    </div>
  );
}
