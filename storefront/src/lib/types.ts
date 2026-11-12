// Mirrors the backend DTOs (com.apparel.commerce.*Dtos)

export type Visibility = 'ENABLED' | 'DISABLED' | 'HIDDEN';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CategoryNode {
  id: number;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  visibility: Visibility;
  children: CategoryNode[];
}

export interface ProductCard {
  id: number;
  name: string;
  slug: string;
  brand?: string;
  price: number;
  compareAtPrice?: number;
  imageUrl?: string;
  badge?: string;
  inStock: boolean;
  lowStock: boolean;
  colors: string[];
  colorHexes: (string | null)[];
}

export type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';

export interface Variant {
  id: number;
  sku: string;
  size: string;
  color: string;
  colorHex?: string;
  price: number;
  available: number;
  stockStatus: StockStatus;
}

export interface ProductDetail {
  id: number;
  name: string;
  slug: string;
  brand?: string;
  description?: string;
  material?: string;
  careInstructions?: string;
  price: number;
  compareAtPrice?: number;
  badge?: string;
  marketingHtml?: string;
  inStock: boolean;
  images: { id: number; url: string; altText?: string; color?: string; sortOrder: number }[];
  variants: Variant[];
  sizeChart?: { name: string; rows: Record<string, unknown>[]; fitNotes?: string };
  breadcrumbs: { name: string; slug: string }[];
}

export interface Facets {
  sizes: string[];
  colors: string[];
}

export interface Banner {
  id: number;
  title: string;
  subtitle?: string;
  imageUrl: string;
  mobileImageUrl?: string;
  ctaText?: string;
  ctaLink?: string;
}

export type SectionType = 'HERO_CAROUSEL' | 'PRODUCT_RAIL' | 'CATEGORY_GRID' | 'BANNER_STRIP' | 'RICH_TEXT';

export interface HomeSection {
  id: number;
  type: SectionType;
  title?: string;
  config: Record<string, unknown>;
  banners: Banner[];
  products: ProductCard[];
  categories: CategoryNode[];
}

export interface NavNode {
  id: number;
  label: string;
  href?: string;
  disabled: boolean;
  badge?: string;
  children: NavNode[];
}

export interface CartLine {
  variantId: number;
  quantity: number;
}

export interface CouponEvaluation {
  code?: string;
  applied: boolean;
  discount: number;
  reasonCode?: string;
  message?: string;
}

export interface QuoteLine {
  variantId: number;
  sku: string;
  productName: string;
  productSlug: string;
  size: string;
  color: string;
  imageUrl?: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  available: number;
  issue?: string;
}

export interface Quote {
  lines: QuoteLine[];
  subtotal: number;
  discount: number;
  shipping: number;
  tax: number;
  total: number;
  currency: string;
  coupon: CouponEvaluation;
  issues: string[];
}

export interface Address {
  fullName: string;
  phone: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface SavedAddress extends Address {
  id: number;
  label?: string;
  isDefault: boolean;
}

export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'WALLET' | 'UPI';

export type OrderStatus =
  | 'PENDING_PAYMENT' | 'PAYMENT_FAILED' | 'PAID' | 'PROCESSING'
  | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'RETURNED';

export interface OrderView {
  orderNumber: string;
  status: OrderStatus;
  email: string;
  guest: boolean;
  items: {
    variantId: number; sku: string; productName: string; productSlug: string; size: string; color: string;
    imageUrl?: string; unitPrice: number; quantity: number; lineTotal: number;
  }[];
  subtotal: number;
  discountTotal: number;
  shippingTotal: number;
  taxTotal: number;
  grandTotal: number;
  currency: string;
  couponCode?: string;
  paymentMethod?: PaymentMethod;
  payments: { method: PaymentMethod; instrument?: string; status: string; failureReason?: string }[];
  shippingAddress: Address;
  billingAddress: Address;
  trackingNumber?: string;
  history: { fromStatus?: OrderStatus; toStatus: OrderStatus; note?: string; changedAt: string }[];
  createdAt: string;
}

export interface OrderSummary {
  orderNumber: string;
  status: OrderStatus;
  itemCount: number;
  grandTotal: number;
  createdAt: string;
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatResponse {
  reply: string;
  products: ProductCard[];
  mode: 'ai' | 'offline';
}
