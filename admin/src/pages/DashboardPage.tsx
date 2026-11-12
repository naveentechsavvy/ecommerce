import { Link } from 'react-router-dom';
import { api, dateTime, money } from '../lib/api';
import type { Dashboard } from '../lib/types';
import { useAsync } from '../lib/useAsync';
import { PageHeader } from '../components/ui';
import { StatusPill } from './OrdersPage';

export function DashboardPage() {
  const { data, error } = useAsync(() => api<Dashboard>('/api/v1/admin/dashboard'), []);
  if (error) return <div className="error-banner">{error.message}</div>;
  if (!data) return <p>Loading…</p>;
  return (
    <>
      <PageHeader title="Dashboard" />
      <div className="stats">
        <div className="stat"><span className="stat-label">Orders today</span><span className="stat-value">{data.ordersToday}</span></div>
        <div className="stat"><span className="stat-label">Revenue today</span><span className="stat-value">{money(data.revenueToday)}</span></div>
        <Link to="/orders?status=PAID" className="stat"><span className="stat-label">Awaiting fulfilment</span><span className="stat-value">{data.awaitingFulfilment}</span></Link>
        <Link to="/inventory?lowStockOnly=true" className={`stat${data.lowStockSkus > 0 ? ' warn' : ''}`}><span className="stat-label">Low-stock SKUs</span><span className="stat-value">{data.lowStockSkus}</span></Link>
      </div>
      <h2>Recent orders</h2>
      <table className="table">
        <thead><tr><th>Order</th><th>Customer</th><th>Items</th><th>Total</th><th>Status</th><th>Placed</th></tr></thead>
        <tbody>
          {data.recentOrders.map((o) => (
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
      {data.recentOrders.length === 0 && <p className="muted">No orders yet. Place one from the storefront to see it here.</p>}
    </>
  );
}
