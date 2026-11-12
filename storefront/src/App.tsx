import { Link, Route, Routes } from 'react-router-dom';
import { ChatWidget } from './components/ChatWidget';
import { Header } from './components/Header';
import { AccountOrdersPage } from './pages/AccountOrdersPage';
import { CartPage } from './pages/CartPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { HomePage } from './pages/HomePage';
import { OrderPage } from './pages/OrderPage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { ProductListPage } from './pages/ProductListPage';
import { TrackOrderPage } from './pages/TrackOrderPage';

export default function App() {
  return (
    <>
      <Header />
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/c/:slug" element={<ProductListPage />} />
          <Route path="/search" element={<ProductListPage />} />
          <Route path="/p/:slug" element={<ProductDetailPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/order/:orderNumber" element={<OrderPage />} />
          <Route path="/account/orders" element={<AccountOrdersPage />} />
          <Route path="/track" element={<TrackOrderPage />} />
          <Route path="*" element={<div className="container page-pad">Page not found. <Link to="/">Go home</Link></div>} />
        </Routes>
      </main>
      <footer className="site-footer">
        <div className="container">
          <span>© Loom &amp; Co.</span>
          <span>Free shipping over ₹999 · 30-day returns · Prices include GST</span>
        </div>
      </footer>
      <ChatWidget />
    </>
  );
}
