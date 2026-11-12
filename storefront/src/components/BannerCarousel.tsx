import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { Banner } from '../lib/types';

export function BannerCarousel({ banners, variant = 'hero' }: { banners: Banner[]; variant?: 'hero' | 'strip' }) {
  const [index, setIndex] = useState(0);

  useEffect(() => {
    if (banners.length < 2) return;
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (reduce) return;
    const t = setInterval(() => setIndex((i) => (i + 1) % banners.length), 6000);
    return () => clearInterval(t);
  }, [banners.length]);

  if (banners.length === 0) return null;
  const b = banners[index % banners.length];

  return (
    <section className={`banner banner-${variant}`} aria-roledescription="carousel">
      <picture>
        {b.mobileImageUrl && <source media="(max-width: 640px)" srcSet={b.mobileImageUrl} />}
        <img src={b.imageUrl} alt="" />
      </picture>
      <div className="banner-copy">
        <h2>{b.title}</h2>
        {b.subtitle && <p>{b.subtitle}</p>}
        {b.ctaText && b.ctaLink && <Link to={b.ctaLink} className="btn">{b.ctaText}</Link>}
      </div>
      {banners.length > 1 && (
        <div className="banner-dots">
          {banners.map((x, i) => (
            <button key={x.id} aria-label={`Show banner ${i + 1}`} aria-current={i === index}
                    className={i === index ? 'active' : ''} onClick={() => setIndex(i)} />
          ))}
        </div>
      )}
    </section>
  );
}
