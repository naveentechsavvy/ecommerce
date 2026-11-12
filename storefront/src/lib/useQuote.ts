import { useEffect, useState } from 'react';
import { useCart } from '../cart/CartContext';
import { post } from './api';
import type { Quote } from './types';

/** Re-prices the bag on the server whenever items, coupon or email change. */
export function useQuote(email?: string) {
  const { items, couponCode } = useCart();
  const [quote, setQuote] = useState<Quote>();
  const [error, setError] = useState<string>();
  const key = JSON.stringify(items.map((i) => [i.variantId, i.quantity])) + couponCode + (email ?? '');

  useEffect(() => {
    if (items.length === 0) {
      setQuote(undefined);
      return;
    }
    let live = true;
    const t = setTimeout(() => {
      post<Quote>('/api/v1/checkout/quote', {
        items: items.map((i) => ({ variantId: i.variantId, quantity: i.quantity })),
        couponCode: couponCode || undefined,
        email: email && /\S+@\S+\.\S+/.test(email) ? email : undefined,
      })
        .then((q) => live && (setQuote(q), setError(undefined)))
        .catch((e) => live && setError(e.message));
    }, 250);
    return () => {
      live = false;
      clearTimeout(t);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return { quote, error };
}
