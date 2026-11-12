import DOMPurify from 'dompurify';
import { Link } from 'react-router-dom';
import { BannerCarousel } from '../components/BannerCarousel';
import { ProductCard } from '../components/ProductCard';
import { api } from '../lib/api';
import type { HomeSection } from '../lib/types';
import { useAsync } from '../lib/useAsync';

/** Homepage layout is data: the admin CMS decides which sections render, in which order. */
export function HomePage() {
  const { data, loading, error } = useAsync(() => api<{ sections: HomeSection[] }>('/api/v1/content/home'), []);

  if (loading) return <div className="container page-pad">Loading…</div>;
  if (error) return <div className="container page-pad error">Could not load the homepage. {error.message}</div>;

  return (
    <div className="home">
      {data?.sections.map((s) => {
        switch (s.type) {
          case 'HERO_CAROUSEL':
            return <BannerCarousel key={s.id} banners={s.banners} />;
          case 'BANNER_STRIP':
            return <BannerCarousel key={s.id} banners={s.banners} variant="strip" />;
          case 'PRODUCT_RAIL':
            return s.products.length === 0 ? null : (
              <section key={s.id} className="container section">
                {s.title && <h2>{s.title}</h2>}
                <div className="rail">{s.products.map((p) => <ProductCard key={p.id} p={p} />)}</div>
              </section>
            );
          case 'CATEGORY_GRID':
            return (
              <section key={s.id} className="container section">
                {s.title && <h2>{s.title}</h2>}
                <div className="category-grid">
                  {s.categories.map((c) => (
                    <Link key={c.id} to={`/c/${c.slug}`} className="category-tile">
                      <img src={c.imageUrl ?? `https://picsum.photos/seed/cat-${c.slug}/600/400`} alt="" loading="lazy" />
                      <span>{c.name}</span>
                    </Link>
                  ))}
                </div>
              </section>
            );
          case 'RICH_TEXT':
            return (
              <section key={s.id} className="container section rich-text">
                {s.title && <h2>{s.title}</h2>}
                <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(String(s.config.html ?? '')) }} />
              </section>
            );
          default:
            return null;
        }
      })}
    </div>
  );
}
