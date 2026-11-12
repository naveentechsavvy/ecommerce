import { useState, type FormEvent } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useCart } from '../cart/CartContext';
import { api } from '../lib/api';
import type { NavNode } from '../lib/types';
import { useAsync } from '../lib/useAsync';

/** Mega menu driven entirely by /content/navigation (admin-controlled visibility). */
export function Header() {
  const { data: nav } = useAsync(() => api<NavNode[]>('/api/v1/content/navigation'), []);
  const { count } = useCart();
  const auth = useAuth();
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [open, setOpen] = useState<number | null>(null);

  const search = (e: FormEvent) => {
    e.preventDefault();
    if (q.trim()) navigate(`/search?q=${encodeURIComponent(q.trim())}`);
  };

  return (
    <header className="site-header">
      <div className="header-bar container">
        <Link to="/" className="logo">Loom &amp; Co.</Link>

        <nav className="mega-nav" aria-label="Main" onMouseLeave={() => setOpen(null)}>
          {nav?.map((item) => (
            <div
              key={item.id}
              className={`mega-item${item.disabled ? ' is-disabled' : ''}`}
              onMouseEnter={() => setOpen(item.id)}
              onFocus={() => setOpen(item.id)}
            >
              {item.href && !item.disabled ? (
                <NavLink to={item.href} className="mega-link" onClick={() => setOpen(null)}>
                  {item.label}
                </NavLink>
              ) : (
                <span className="mega-link" aria-disabled={item.disabled}>{item.label}</span>
              )}
              {item.badge && <span className="nav-badge">{item.badge}</span>}

              {open === item.id && item.children.length > 0 && !item.disabled && (
                <div className="mega-panel" role="menu">
                  <div className="mega-panel-inner container">
                    {item.children.map((child) =>
                      child.href && !child.disabled ? (
                        <Link key={child.id} to={child.href} className="mega-child" role="menuitem" onClick={() => setOpen(null)}>
                          {child.label}
                          {child.badge && <span className="nav-badge">{child.badge}</span>}
                        </Link>
                      ) : (
                        <span key={child.id} className="mega-child is-disabled">{child.label} (coming soon)</span>
                      ),
                    )}
                    {item.href && (
                      <Link to={item.href} className="mega-child mega-all" onClick={() => setOpen(null)}>
                        Shop all {item.label} →
                      </Link>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}
        </nav>

        <form className="search" onSubmit={search} role="search">
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search products" aria-label="Search products" />
        </form>

        <div className="header-actions">
          {auth.ready && (auth.authenticated ? (
            <>
              <Link to="/account/orders">My orders</Link>
              <button className="link-btn" onClick={auth.logout}>Sign out</button>
            </>
          ) : (
            <>
              <Link to="/track">Track order</Link>
              <button className="link-btn" onClick={auth.login}>Sign in</button>
            </>
          ))}
          <Link to="/cart" className="cart-link" aria-label={`Bag, ${count} items`}>
            Bag <span className="cart-count">{count}</span>
          </Link>
        </div>
      </div>
    </header>
  );
}
