export type Visibility = 'ENABLED' | 'DISABLED' | 'HIDDEN';
export const VISIBILITIES: Visibility[] = ['ENABLED', 'DISABLED', 'HIDDEN'];

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Category {
  id: number;
  parentId?: number;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  sortOrder: number;
  visibility: Visibility;
}

export type ProductStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
export type Gender = 'MEN' | 'WOMEN' | 'UNISEX' | 'KIDS';

export interface ProductRow {
  id: number;
  name: string;
  slug: string;
  categoryName: string;
  basePrice: number;
  status: ProductStatus;
  variantCount: number;
  totalAvailable: number;
  imageUrl?: string;
  updatedAt: string;
}

export interface VariantAdmin {
  id: number;
  sku: string;
  size: string;
  color: string;
  colorHex?: string;
  priceOverride?: number;
  active: boolean;
  onHand: number;
  reserved: number;
  available: number;
}

export interface ProductAdmin {
  id: number;
  categoryId: number;
  categoryName: string;
  sizeChartId?: number;
  name: string;
  slug: string;
  brand?: string;
  description?: string;
  material?: string;
  careInstructions?: string;
  gender?: Gender;
  basePrice: number;
  compareAtPrice?: number;
  status: ProductStatus;
  popularityScore: number;
  marketingBadge?: string;
  marketingHtml?: string;
  images: { id: number; url: string; altText?: string; color?: string; sortOrder: number }[];
  variants: VariantAdmin[];
  totalAvailable: number;
}

export interface InventoryRow {
  variantId: number;
  sku: string;
  productId: number;
  productName: string;
  size: string;
  color: string;
  onHand: number;
  reserved: number;
  available: number;
  lowStockThreshold: number;
  lowStock: boolean;
  updatedAt: string;
}

export type Placement = 'HOME_HERO' | 'HOME_STRIP' | 'PLP_TOP' | 'PDP_PROMO';

export interface BannerAdmin {
  id: number;
  title: string;
  subtitle?: string;
  imageUrl: string;
  mobileImageUrl?: string;
  ctaText?: string;
  ctaLink?: string;
  placement: Placement;
  sortOrder: number;
  active: boolean;
  startsAt?: string | null;
  endsAt?: string | null;
  live: boolean;
}

export type SectionType = 'HERO_CAROUSEL' | 'PRODUCT_RAIL' | 'CATEGORY_GRID' | 'BANNER_STRIP' | 'RICH_TEXT';

export interface HomeSectionAdmin {
  id: number;
  sectionType: SectionType;
  title?: string;
  config: Record<string, unknown>;
  sortOrder: number;
  active: boolean;
}

export interface NavItemAdmin {
  id: number;
  parentId?: number;
  label: string;
  linkType: 'CATEGORY' | 'URL' | 'NONE';
  categoryId?: number;
  categoryName?: string;
  url?: string;
  badgeText?: string;
  sortOrder: number;
  visibility: Visibility;
}

export interface Coupon {
  id: number;
  code: string;
  description?: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  maxDiscountAmount?: number;
  minCartValue: number;
  targetUserType: 'ALL' | 'GUEST' | 'REGISTERED';
  startsAt?: string | null;
  endsAt?: string | null;
  usageLimitTotal?: number;
  usageLimitPerUser?: number;
  timesUsed: number;
  active: boolean;
  state: 'LIVE' | 'INACTIVE' | 'SCHEDULED' | 'EXPIRED' | 'EXHAUSTED';
}

export type OrderStatus =
  | 'PENDING_PAYMENT' | 'PAYMENT_FAILED' | 'PAID' | 'PROCESSING'
  | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'RETURNED';
export const ORDER_STATUSES: OrderStatus[] = ['PENDING_PAYMENT', 'PAYMENT_FAILED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED'];

export interface OrderSummary {
  orderNumber: string;
  status: OrderStatus;
  email: string;
  guest: boolean;
  itemCount: number;
  grandTotal: number;
  createdAt: string;
}

export interface Address {
  fullName: string; phone: string; line1: string; line2?: string; city: string; state: string; postalCode: string; country: string;
}

export interface OrderDetail extends OrderSummary {
  items: { variantId: number; sku: string; productName: string; size: string; color: string; unitPrice: number; quantity: number; lineTotal: number }[];
  subtotal: number;
  discountTotal: number;
  shippingTotal: number;
  couponCode?: string;
  payments: { method: string; provider: string; providerRef?: string; instrument?: string; status: string; amount: number; failureReason?: string; createdAt: string }[];
  shippingAddress: Address;
  billingAddress: Address;
  trackingNumber?: string;
  history: { fromStatus?: OrderStatus; toStatus: OrderStatus; note?: string; changedBy?: string; changedAt: string }[];
  allowedNextStatuses: OrderStatus[];
}

export interface Dashboard {
  ordersToday: number;
  revenueToday: number;
  awaitingFulfilment: number;
  lowStockSkus: number;
  recentOrders: OrderSummary[];
}
