import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

/** Display snapshot kept client-side; prices are always re-quoted by the server. */
export interface CartItem {
  variantId: number;
  quantity: number;
  productName: string;
  productSlug: string;
  size: string;
  color: string;
  price: number;
  imageUrl?: string;
}

interface CartState {
  items: CartItem[];
  count: number;
  couponCode: string;
  add: (item: CartItem) => void;
  setQuantity: (variantId: number, quantity: number) => void;
  remove: (variantId: number) => void;
  clear: () => void;
  setCouponCode: (code: string) => void;
}

const STORAGE_KEY = 'loomco.cart.v1';
const MAX_QTY = 10;
const CartContext = createContext<CartState | null>(null);

function load(): { items: CartItem[]; couponCode: string } {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch {
    /* storage unavailable */
  }
  return { items: [], couponCode: '' };
}

export function CartProvider({ children }: { children: ReactNode }) {
  const initial = useMemo(load, []);
  const [items, setItems] = useState<CartItem[]>(initial.items);
  const [couponCode, setCouponCode] = useState(initial.couponCode);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ items, couponCode }));
    } catch {
      /* ignore */
    }
  }, [items, couponCode]);

  const add = useCallback((item: CartItem) => {
    setItems((prev) => {
      const existing = prev.find((i) => i.variantId === item.variantId);
      if (existing) {
        return prev.map((i) =>
          i.variantId === item.variantId ? { ...i, quantity: Math.min(MAX_QTY, i.quantity + item.quantity) } : i,
        );
      }
      return [...prev, { ...item, quantity: Math.min(MAX_QTY, item.quantity) }];
    });
  }, []);

  const setQuantity = useCallback((variantId: number, quantity: number) => {
    setItems((prev) =>
      quantity <= 0
        ? prev.filter((i) => i.variantId !== variantId)
        : prev.map((i) => (i.variantId === variantId ? { ...i, quantity: Math.min(MAX_QTY, quantity) } : i)),
    );
  }, []);

  const value: CartState = {
    items,
    count: items.reduce((n, i) => n + i.quantity, 0),
    couponCode,
    add,
    setQuantity,
    remove: (variantId) => setItems((prev) => prev.filter((i) => i.variantId !== variantId)),
    clear: () => {
      setItems([]);
      setCouponCode('');
    },
    setCouponCode,
  };
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart(): CartState {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart outside CartProvider');
  return ctx;
}
