import { useState, type FormEvent } from 'react';
import { api, ApiError, del, label, post, put } from '../lib/api';
import type { HomeSectionAdmin, SectionType } from '../lib/types';
import { useAsync } from '../lib/useAsync';
import { ErrorBanner, Field, Modal, PageHeader, Pill } from '../components/ui';

const TYPES: SectionType[] = ['HERO_CAROUSEL', 'CATEGORY_GRID', 'PRODUCT_RAIL', 'BANNER_STRIP', 'RICH_TEXT'];

const CONFIG_TEMPLATES: Record<SectionType, Record<string, unknown>> = {
  HERO_CAROUSEL: { placement: 'HOME_HERO' },
  BANNER_STRIP: { placement: 'HOME_STRIP' },
  PRODUCT_RAIL: { categorySlug: 'women', sort: 'newest', limit: 8 },
  CATEGORY_GRID: { categorySlugs: ['men', 'women', 'kids'] },
  RICH_TEXT: { html: '<p>Your story here.</p>' },
};

const HELP: Record<SectionType, string> = {
  HERO_CAROUSEL: 'Rotating banners from a placement (manage them under Banners).',
  BANNER_STRIP: 'A single-row promotional banner from a placement.',
  PRODUCT_RAIL: 'Horizontal product carousel. sort: newest | popularity | price_asc | price_desc. categorySlug optional.',
  CATEGORY_GRID: 'Tiles linking to top-level categories.',
  RICH_TEXT: 'Free-form HTML (sanitised on the storefront).',
};

/** Homepage builder: ordered, toggleable sections whose config is JSON. */
export function HomeLayoutPage() {
  const { data, setData } = useAsync(() => api<HomeSectionAdmin[]>('/api/v1/admin/content/home-sections'), []);
  const [draft, setDraft] = useState<Partial<HomeSectionAdmin>>();
  const [err, setErr] = useState<string>();
  const reload = async () => setData(await api<HomeSectionAdmin[]>('/api/v1/admin/content/home-sections'));

  const move = async (index: number, dir: -1 | 1) => {
    if (!data) return;
    const ids = data.map((s) => s.id);
    [ids[index], ids[index + dir]] = [ids[index + dir], ids[index]];
    setData(await put<HomeSectionAdmin[]>('/api/v1/admin/content/home-sections/order', { orderedIds: ids }));
  };

  const toggle = async (s: HomeSectionAdmin) => {
    try {
      await put(`/api/v1/admin/content/home-sections/${s.id}`, { ...s, active: !s.active });
      reload();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'Update failed');
    }
  };

  return (
    <>
      <PageHeader title="Homepage layout" actions={<button className="btn" onClick={() => setDraft({ sectionType: 'PRODUCT_RAIL', config: CONFIG_TEMPLATES.PRODUCT_RAIL, active: true })}>Add section</button>} />
      <ErrorBanner error={err} />
      <ol className="section-list">
        {data?.map((s, i) => (
          <li key={s.id} className={`panel section-row${s.active ? '' : ' inactive'}`}>
            <div className="reorder">
              <button className="icon-btn" disabled={i === 0} onClick={() => move(i, -1)} aria-label="Move up">↑</button>
              <button className="icon-btn" disabled={i === data.length - 1} onClick={() => move(i, 1)} aria-label="Move down">↓</button>
            </div>
            <div className="grow">
              <strong>{s.title || label(s.sectionType)}</strong> <Pill tone="blue">{label(s.sectionType)}</Pill>
              <div className="mono small muted">{JSON.stringify(s.config)}</div>
            </div>
            <label className="check"><input type="checkbox" checked={s.active} onChange={() => toggle(s)} /> Visible</label>
            <button className="btn small ghost" onClick={() => setDraft(s)}>Edit</button>
            <button className="btn small ghost danger" onClick={async () => { if (confirm('Remove section?')) { await del(`/api/v1/admin/content/home-sections/${s.id}`); reload(); } }}>Remove</button>
          </li>
        ))}
      </ol>
      {draft && <SectionForm draft={draft} nextOrder={(data?.length ?? 0) + 1} onClose={() => setDraft(undefined)} onSaved={() => { setDraft(undefined); reload(); }} />}
    </>
  );
}

function SectionForm({ draft, nextOrder, onClose, onSaved }: { draft: Partial<HomeSectionAdmin>; nextOrder: number; onClose: () => void; onSaved: () => void }) {
  const [type, setType] = useState<SectionType>(draft.sectionType ?? 'PRODUCT_RAIL');
  const [title, setTitle] = useState(draft.title ?? '');
  const [config, setConfig] = useState(JSON.stringify(draft.config ?? CONFIG_TEMPLATES[type], null, 2));
  const [err, setErr] = useState<string>();

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    let parsed: unknown;
    try {
      parsed = JSON.parse(config);
    } catch {
      return setErr('Config must be valid JSON');
    }
    const body = { sectionType: type, title: title || null, config: parsed, sortOrder: draft.sortOrder ?? nextOrder, active: draft.active ?? true };
    try {
      draft.id ? await put(`/api/v1/admin/content/home-sections/${draft.id}`, body) : await post('/api/v1/admin/content/home-sections', body);
      onSaved();
    } catch (ex) {
      setErr(ex instanceof ApiError ? ex.message : 'Save failed');
    }
  };

  return (
    <Modal title={draft.id ? 'Edit section' : 'Add section'} onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <Field label="Type" hint={HELP[type]}>
          <select value={type} onChange={(e) => { const t = e.target.value as SectionType; setType(t); if (!draft.id) setConfig(JSON.stringify(CONFIG_TEMPLATES[t], null, 2)); }}>
            {TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
          </select>
        </Field>
        <Field label="Heading (optional)"><input value={title} onChange={(e) => setTitle(e.target.value)} /></Field>
        <Field label="Config (JSON)"><textarea className="mono" rows={8} value={config} onChange={(e) => setConfig(e.target.value)} /></Field>
        <ErrorBanner error={err} />
        <button className="btn">Save</button>
      </form>
    </Modal>
  );
}
