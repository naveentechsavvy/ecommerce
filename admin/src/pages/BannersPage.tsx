import { useState, type FormEvent } from 'react';
import { api, ApiError, dateTime, del, fromLocalInput, label, post, put, toLocalInput } from '../lib/api';
import type { BannerAdmin, Placement } from '../lib/types';
import { useAsync } from '../lib/useAsync';
import { ErrorBanner, Field, Modal, PageHeader, Pill } from '../components/ui';

const PLACEMENTS: Placement[] = ['HOME_HERO', 'HOME_STRIP', 'PLP_TOP', 'PDP_PROMO'];
type Draft = Partial<BannerAdmin> & { placement: Placement };

export function BannersPage() {
  const { data, setData } = useAsync(() => api<BannerAdmin[]>('/api/v1/admin/content/banners'), []);
  const [draft, setDraft] = useState<Draft>();
  const reload = async () => setData(await api<BannerAdmin[]>('/api/v1/admin/content/banners'));

  return (
    <>
      <PageHeader title="Banners" actions={<button className="btn" onClick={() => setDraft({ placement: 'HOME_HERO', active: true, sortOrder: 1 })}>New banner</button>} />
      <p className="muted">Banners appear on the storefront when active and inside their schedule window. Storefront caches content for up to 60 seconds.</p>
      {PLACEMENTS.map((pl) => {
        const rows = data?.filter((b) => b.placement === pl) ?? [];
        return (
          <section key={pl} className="panel">
            <h3>{label(pl)}</h3>
            {rows.length === 0 ? <p className="muted">No banners.</p> : (
              <table className="table compact">
                <tbody>
                  {rows.map((b) => (
                    <tr key={b.id}>
                      <td><img className="banner-thumb" src={b.imageUrl} alt="" /></td>
                      <td><strong>{b.title}</strong><div className="muted small">{b.subtitle}</div></td>
                      <td>{b.ctaText && <>{b.ctaText} → <span className="mono">{b.ctaLink}</span></>}</td>
                      <td className="small">{b.startsAt || b.endsAt ? <>{dateTime(b.startsAt) || '…'} – {dateTime(b.endsAt) || '…'}</> : 'Always'}</td>
                      <td>#{b.sortOrder}</td>
                      <td>{b.live ? <Pill tone="green">Live</Pill> : b.active ? <Pill tone="amber">Scheduled/expired</Pill> : <Pill tone="grey">Off</Pill>}</td>
                      <td className="row-actions">
                        <button className="btn small ghost" onClick={() => setDraft(b)}>Edit</button>
                        <button className="btn small ghost danger" onClick={async () => { if (confirm('Delete banner?')) { await del(`/api/v1/admin/content/banners/${b.id}`); reload(); } }}>Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        );
      })}
      {draft && <BannerForm draft={draft} onClose={() => setDraft(undefined)} onSaved={() => { setDraft(undefined); reload(); }} />}
    </>
  );
}

function BannerForm({ draft, onClose, onSaved }: { draft: Draft; onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState<Draft>(draft);
  const [err, setErr] = useState<string>();
  const set = (k: keyof Draft, v: unknown) => setF({ ...f, [k]: v });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const body = { ...f, startsAt: f.startsAt || null, endsAt: f.endsAt || null };
      f.id ? await put(`/api/v1/admin/content/banners/${f.id}`, body) : await post('/api/v1/admin/content/banners', body);
      onSaved();
    } catch (ex) {
      setErr(ex instanceof ApiError ? ex.message : 'Save failed');
    }
  };

  return (
    <Modal title={f.id ? 'Edit banner' : 'New banner'} onClose={onClose}>
      <form onSubmit={submit} className="stack">
        {f.imageUrl && <img className="banner-preview" src={f.imageUrl} alt="" />}
        <Field label="Placement">
          <select value={f.placement} onChange={(e) => set('placement', e.target.value)}>
            {PLACEMENTS.map((p) => <option key={p} value={p}>{label(p)}</option>)}
          </select>
        </Field>
        <Field label="Title"><input required value={f.title ?? ''} onChange={(e) => set('title', e.target.value)} /></Field>
        <Field label="Subtitle"><input value={f.subtitle ?? ''} onChange={(e) => set('subtitle', e.target.value)} /></Field>
        <Field label="Image URL (desktop)"><input required type="url" value={f.imageUrl ?? ''} onChange={(e) => set('imageUrl', e.target.value)} /></Field>
        <Field label="Image URL (mobile, optional)"><input type="url" value={f.mobileImageUrl ?? ''} onChange={(e) => set('mobileImageUrl', e.target.value)} /></Field>
        <div className="grid-2 tight">
          <Field label="Button text"><input value={f.ctaText ?? ''} onChange={(e) => set('ctaText', e.target.value)} /></Field>
          <Field label="Button link" hint="e.g. /c/men"><input value={f.ctaLink ?? ''} onChange={(e) => set('ctaLink', e.target.value)} /></Field>
          <Field label="Starts"><input type="datetime-local" value={toLocalInput(f.startsAt)} onChange={(e) => set('startsAt', fromLocalInput(e.target.value))} /></Field>
          <Field label="Ends"><input type="datetime-local" value={toLocalInput(f.endsAt)} onChange={(e) => set('endsAt', fromLocalInput(e.target.value))} /></Field>
          <Field label="Sort order"><input type="number" value={f.sortOrder ?? 0} onChange={(e) => set('sortOrder', Number(e.target.value))} /></Field>
        </div>
        <label className="check"><input type="checkbox" checked={f.active ?? true} onChange={(e) => set('active', e.target.checked)} /> Active</label>
        <ErrorBanner error={err} />
        <button className="btn">Save</button>
      </form>
    </Modal>
  );
}
