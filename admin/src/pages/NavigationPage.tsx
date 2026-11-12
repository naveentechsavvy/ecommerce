import { useState, type FormEvent } from 'react';
import { api, ApiError, del, patch, post, put } from '../lib/api';
import type { Category, NavItemAdmin, Visibility } from '../lib/types';
import { useAsync } from '../lib/useAsync';
import { ErrorBanner, Field, Modal, PageHeader, VisibilitySelect } from '../components/ui';

type Draft = Partial<NavItemAdmin> & { linkType: NavItemAdmin['linkType'] };

/** Two-level mega menu editor with per-item Enable / Disable / Hide. */
export function NavigationPage() {
  const { data, setData } = useAsync(() => api<NavItemAdmin[]>('/api/v1/admin/content/navigation'), []);
  const categories = useAsync(() => api<Category[]>('/api/v1/admin/categories'), []);
  const [draft, setDraft] = useState<Draft>();
  const [err, setErr] = useState<string>();
  const reload = async () => setData(await api<NavItemAdmin[]>('/api/v1/admin/content/navigation'));

  const setVisibility = async (n: NavItemAdmin, visibility: Visibility) => {
    try {
      const updated = await patch<NavItemAdmin>(`/api/v1/admin/content/navigation/${n.id}/visibility`, { visibility });
      setData((d) => d?.map((x) => (x.id === n.id ? updated : x)));
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'Update failed');
    }
  };

  const top = data?.filter((n) => !n.parentId) ?? [];
  const row = (n: NavItemAdmin, child = false) => (
    <tr key={n.id} className={n.visibility === 'HIDDEN' ? 'inactive' : ''}>
      <td style={{ paddingLeft: child ? 36 : 12 }}>{child ? <>↳ {n.label}</> : <strong>{n.label}</strong>}</td>
      <td>{n.linkType === 'CATEGORY' ? `Category: ${n.categoryName}` : n.linkType === 'URL' ? n.url : '—'}</td>
      <td>{n.badgeText}</td>
      <td>{n.sortOrder}</td>
      <td><VisibilitySelect value={n.visibility} onChange={(v) => setVisibility(n, v)} /></td>
      <td className="row-actions">
        {!child && <button className="btn small ghost" onClick={() => setDraft({ parentId: n.id, linkType: 'CATEGORY', visibility: 'ENABLED', sortOrder: 0 })}>Add child</button>}
        <button className="btn small ghost" onClick={() => setDraft(n)}>Edit</button>
        <button className="btn small ghost danger" onClick={async () => { if (confirm(`Delete “${n.label}”${child ? '' : ' and its children'}?`)) { await del(`/api/v1/admin/content/navigation/${n.id}`); reload(); } }}>Delete</button>
      </td>
    </tr>
  );

  return (
    <>
      <PageHeader title="Menu & navigation" actions={<button className="btn" onClick={() => setDraft({ linkType: 'CATEGORY', visibility: 'ENABLED', sortOrder: top.length + 1 })}>Add top-level item</button>} />
      <p className="muted">Top-level items are the header tabs; children appear in the mega-menu dropdown. A link to a hidden category is hidden automatically.</p>
      <ErrorBanner error={err} />
      <table className="table">
        <thead><tr><th>Label</th><th>Link</th><th>Badge</th><th>Order</th><th>Visibility</th><th /></tr></thead>
        <tbody>
          {top.flatMap((n) => [row(n), ...(data ?? []).filter((c) => c.parentId === n.id).map((c) => row(c, true))])}
        </tbody>
      </table>
      {draft && <NavForm draft={draft} top={top} categories={categories.data ?? []} onClose={() => setDraft(undefined)} onSaved={() => { setDraft(undefined); reload(); }} />}
    </>
  );
}

function NavForm({ draft, top, categories, onClose, onSaved }: { draft: Draft; top: NavItemAdmin[]; categories: Category[]; onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState<Draft>(draft);
  const [err, setErr] = useState<string>();
  const set = (k: keyof Draft, v: unknown) => setF({ ...f, [k]: v });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      f.id ? await put(`/api/v1/admin/content/navigation/${f.id}`, f) : await post('/api/v1/admin/content/navigation', f);
      onSaved();
    } catch (ex) {
      setErr(ex instanceof ApiError ? ex.message : 'Save failed');
    }
  };

  return (
    <Modal title={f.id ? 'Edit menu item' : 'New menu item'} onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <Field label="Label"><input required value={f.label ?? ''} onChange={(e) => set('label', e.target.value)} maxLength={80} /></Field>
        <Field label="Parent">
          <select value={f.parentId ?? ''} onChange={(e) => set('parentId', e.target.value ? Number(e.target.value) : undefined)}>
            <option value="">— Top level —</option>
            {top.filter((t) => t.id !== f.id).map((t) => <option key={t.id} value={t.id}>{t.label}</option>)}
          </select>
        </Field>
        <Field label="Link type">
          <select value={f.linkType} onChange={(e) => set('linkType', e.target.value)}>
            <option value="CATEGORY">Category</option><option value="URL">URL</option><option value="NONE">No link (heading)</option>
          </select>
        </Field>
        {f.linkType === 'CATEGORY' && (
          <Field label="Category">
            <select required value={f.categoryId ?? ''} onChange={(e) => set('categoryId', Number(e.target.value))}>
              <option value="">Select…</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name} ({c.slug})</option>)}
            </select>
          </Field>
        )}
        {f.linkType === 'URL' && <Field label="URL" hint="Relative (/c/men?sort=price_asc) or absolute"><input required value={f.url ?? ''} onChange={(e) => set('url', e.target.value)} /></Field>}
        <div className="grid-2 tight">
          <Field label="Badge"><input value={f.badgeText ?? ''} onChange={(e) => set('badgeText', e.target.value)} maxLength={30} placeholder="New" /></Field>
          <Field label="Sort order"><input type="number" value={f.sortOrder ?? 0} onChange={(e) => set('sortOrder', Number(e.target.value))} /></Field>
        </div>
        <Field label="Visibility"><VisibilitySelect value={f.visibility ?? 'ENABLED'} onChange={(v) => set('visibility', v)} /></Field>
        <ErrorBanner error={err} />
        <button className="btn">Save</button>
      </form>
    </Modal>
  );
}
