import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/auth';
import { Layout } from './components/Layout';
import { BannersPage } from './pages/BannersPage';
import { CategoriesPage } from './pages/CategoriesPage';
import { CouponsPage } from './pages/CouponsPage';
import { DashboardPage } from './pages/DashboardPage';
import { HomeLayoutPage } from './pages/HomeLayoutPage';
import { InventoryPage } from './pages/InventoryPage';
import { NavigationPage } from './pages/NavigationPage';
import { OrderDetailPage, OrdersPage } from './pages/OrdersPage';
import { ProductEditorPage } from './pages/ProductEditorPage';
import { ProductsPage } from './pages/ProductsPage';

export default function App() {
  const { isAdmin } = useAuth();
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route path="orders" element={<OrdersPage />} />
        <Route path="orders/:orderNumber" element={<OrderDetailPage />} />
        <Route path="inventory" element={<InventoryPage />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="products/:id" element={<ProductEditorPage />} />
        <Route path="categories" element={<CategoriesPage />} />
        {isAdmin && (
          <>
            <Route path="content/home" element={<HomeLayoutPage />} />
            <Route path="content/banners" element={<BannersPage />} />
            <Route path="content/navigation" element={<NavigationPage />} />
            <Route path="coupons" element={<CouponsPage />} />
          </>
        )}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
