import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { api } from '../lib/api';
import { date, money, statusLabel } from '../lib/format';
import type { OrderSummary, Page } from '../lib/types';
import { useAsync } from '../lib/useAsync';

export function AccountOrdersPage() {
  const auth = useAuth();
  const { data, loading } = useAsync(
    () => (auth.authenticated ? api<Page<OrderSummary>>('/api/v1/me/orders?size=50') : Promise.resolve(undefined)),
    [auth.authenticated],
  );

  if (auth.ready && !auth.authenticated) {
    return (
      <div className="container page-pad">
        <h1>My orders</h1>
        <p>Please <button className="link-btn" onClick={auth.login}>sign in</button> to see your orders.</p>
      </div>
    );
  }

  return (
    <div className="container page-pad">
      <h1>My orders{auth.name ? ` · ${auth.name}` : ''}</h1>
      {loading && <p>Loading…</p>}
      {data && data.content.length === 0 && <p>No orders yet. <Link to="/">Start shopping</Link></p>}
      <table className="table">
        <tbody>
          {data?.content.map((o) => (
            <tr key={o.orderNumber}>
              <td><Link to={`/order/${o.orderNumber}`}>{o.orderNumber}</Link></td>
              <td>{date(o.createdAt)}</td>
              <td>{o.itemCount} item(s)</td>
              <td>{money(o.grandTotal)}</td>
              <td><span className={`status status-${o.status.toLowerCase()}`}>{statusLabel(o.status)}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
