import { useState, type FormEvent } from 'react';
import { useAuth } from '../auth/auth';
import { api, ApiError, del, patch, post, put } from '../lib/api';
import type { Category, Visibility } from '../lib/types';
import { useAsync } from '../lib/useAsync';
import { ErrorBanner, Field, Modal, PageHeader, VisibilitySelect } from '../components/ui';

type Draft = Omit<Category, 'id'> & { id?: number };
const EMPTY: Draft = { name: '', slug: '', description: '', imageUrl: '', sortOrder: 0, visibility: 'ENABLED' };

/** Flattened tree with depth so we can indent children. */
function flatten(all: Category[], parentId?: number, depth = 0): (Category & { depth: number })[] {
  return all
    .filter((c) => (c.parentId ?? undefined) === parentId)
    .sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name))
    .flatMap((c) => [{ ...c, depth }, ...flatten(all, c.id, depth + 1)]);
}

export function CategoriesPage() {
  const { isAdmin } = useAuth();
  const { data, setData, error } = useAsync(() => api<Category[]>('/api/v1/admin/categories'), []);
  const [draft, setDraft] = useState<Draft>();
  const [err, setErr] = useState<string>();

  const reload = async () => setData(await api<Category[]>('/api/v1/admin/categories'));

  const setVisibility = async (c: Category, visibility: Visibility) => {
    setErr(undefined);
    try {
      const updated = await patch<Category>(`/api/v1/admin/categories/${c.id}/visibility`, { visibility });
      setData((d) => d?.map((x) => (x.id === c.id ? updated : x)));
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'Update failed');
    }
  };

  const remove = async (c: Category) => {
    if (!confirm(`Delete category “${c.name}”?`)) return;
    try {
      await del(`/api/v1/admin/categories/${c.id}`);
      await reload();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'Delete failed');
    }
  };

  if (error) return <ErrorBanner error={error.message} />;
  const rows = data ? flatten(data) : [];

  return (
    <>
      <PageHeader title="Categories" actions={isAdmin && <button className="btn" onClick={() => setDraft({ ...EMPTY })}>New category</button>} />
      <p className="muted">Visibility controls the storefront: <b>Enabled</b> shows it, <b>Disabled</b> shows it greyed out (“coming soon”), <b>Hidden</b> removes it from menus and listings.</p>
      <ErrorBanner error={err} />
      <table className="table">
        <thead><tr><th>Name</th><th>Slug</th><th>Order</th><th>Visibility</th><th /></tr></thead>
        <tbody>
          {rows.map((c) => (
            <tr key={c.id}>
              <td style={{ paddingLeft: 12 + c.depth * 24 }}>{c.depth > 0 && '↳ '}{c.name}</td>
              <td className="mono">{c.slug}</td>
              <td>{c.sortOrder}</td>
              <td>{isAdmin ? <VisibilitySelect value={c.visibility} onChange={(v) => setVisibility(c, v)} /> : c.visibility}</td>
              <td className="row-actions">
                {isAdmin && <>
                  <button className="btn small ghost" onClick={() => setDraft({ ...c })}>Edit</button>
                  <button className="btn small ghost danger" onClick={() => remove(c)}>Delete</button>
                </>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {draft && data && (
        <CategoryForm draft={draft} all={data} onClose={() => setDraft(undefined)} onSaved={async () => { setDraft(undefined); await reload(); }} />
      )}
    </>
  );
}

function CategoryForm({ draft, all, onClose, onSaved }: { draft: Draft; all: Category[]; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState<Draft>(draft);
  const [err, setErr] = useState<string>();
  const set = <K extends keyof Draft>(k: K, v: Draft[K]) => setForm({ ...form, [k]: v });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setErr(undefined);
    const body = { ...form, slug: form.slug || undefined, parentId: form.parentId || undefined };
    try {
      form.id ? await put(`/api/v1/admin/categories/${form.id}`, body) : await post('/api/v1/admin/categories', body);
      onSaved();
    } catch (ex) {
      setErr(ex instanceof ApiError ? ex.message : 'Save failed');
    }
  };

  return (
    <Modal title={form.id ? 'Edit category' : 'New category'} onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <Field label="Name"><input required value={form.name} onChange={(e) => set('name', e.target.value)} /></Field>
        <Field label="Slug" hint="Leave blank to generate from the name"><input value={form.slug} onChange={(e) => set('slug', e.target.value)} /></Field>
        <Field label="Parent">
          <select value={form.parentId ?? ''} onChange={(e) => set('parentId', e.target.value ? Number(e.target.value) : undefined)}>
            <option value="">— Top level —</option>
            {all.filter((c) => c.id !== form.id).map((c) => <option key={c.id} value={c.id}>{c.name} ({c.slug})</option>)}
          </select>
        </Field>
        <Field label="Description"><input value={form.description ?? ''} onChange={(e) => set('description', e.target.value)} /></Field>
        <Field label="Image URL"><input value={form.imageUrl ?? ''} onChange={(e) => set('imageUrl', e.target.value)} /></Field>
        <div className="grid-2 tight">
          <Field label="Sort order"><input type="number" value={form.sortOrder} onChange={(e) => set('sortOrder', Number(e.target.value))} /></Field>
          <Field label="Visibility"><VisibilitySelect value={form.visibility} onChange={(v) => set('visibility', v)} /></Field>
        </div>
        <ErrorBanner error={err} />
        <button className="btn">Save</button>
      </form>
    </Modal>
  );
}
