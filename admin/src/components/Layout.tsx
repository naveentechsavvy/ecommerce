import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/auth';

const NAV: { to: string; label: string; adminOnly?: boolean; group: string }[] = [
  { to: '/', label: 'Dashboard', group: 'Overview' },
  { to: '/orders', label: 'Orders', group: 'Operations' },
  { to: '/inventory', label: 'Inventory', group: 'Operations' },
  { to: '/products', label: 'Products', group: 'Catalog' },
  { to: '/categories', label: 'Categories', group: 'Catalog' },
  { to: '/content/home', label: 'Homepage layout', group: 'Content', adminOnly: true },
  { to: '/content/banners', label: 'Banners', group: 'Content', adminOnly: true },
  { to: '/content/navigation', label: 'Menu & navigation', group: 'Content', adminOnly: true },
  { to: '/coupons', label: 'Coupons', group: 'Promotions', adminOnly: true },
];

export function Layout() {
  const auth = useAuth();
  const items = NAV.filter((n) => !n.adminOnly || auth.isAdmin);
  const groups = [...new Set(items.map((i) => i.group))];
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">Loom &amp; Co. <small>Back office</small></div>
        <nav>
          {groups.map((g) => (
            <div key={g} className="nav-group">
              <div className="nav-group-label">{g}</div>
              {items.filter((i) => i.group === g).map((i) => (
                <NavLink key={i.to} to={i.to} end={i.to === '/'}>{i.label}</NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="me">
          <div>{auth.name}</div>
          <small className="muted">{auth.roles.join(', ')}</small>
          <button className="link-btn" onClick={auth.logout}>Sign out</button>
        </div>
      </aside>
      <main className="content"><Outlet /></main>
    </div>
  );
}
