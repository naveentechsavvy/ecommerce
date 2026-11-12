import { useState, type FormEvent } from 'react';
import { api, ApiError, dateTime, del, fromLocalInput, label, money, patch, post, put, toLocalInput } from '../lib/api';
import type { Coupon } from '../lib/types';
import { useAsync } from '../lib/useAsync';
import { ErrorBanner, Field, Modal, PageHeader, Pill, num } from '../components/ui';

const STATE_TONE = { LIVE: 'green', SCHEDULED: 'blue', INACTIVE: 'grey', EXPIRED: 'red', EXHAUSTED: 'amber' } as const;

type Form = {
  id?: number; code: string; description: string; discountType: Coupon['discountType']; discountValue: string;
  maxDiscountAmount: string; minCartValue: string; targetUserType: Coupon['targetUserType'];
  startsAt?: string | null; endsAt?: string | null; usageLimitTotal: string; usageLimitPerUser: string; active: boolean;
};

const toForm = (c?: Coupon): Form => ({
  id: c?.id, code: c?.code ?? '', description: c?.description ?? '', discountType: c?.discountType ?? 'PERCENTAGE',
  discountValue: c ? String(c.discountValue) : '', maxDiscountAmount: c?.maxDiscountAmount != null ? String(c.maxDiscountAmount) : '',
  minCartValue: c ? String(c.minCartValue) : '0', targetUserType: c?.targetUserType ?? 'ALL', startsAt: c?.startsAt, endsAt: c?.endsAt,
  usageLimitTotal: c?.usageLimitTotal != null ? String(c.usageLimitTotal) : '', usageLimitPerUser: c?.usageLimitPerUser != null ? String(c.usageLimitPerUser) : '',
  active: c?.active ?? true,
});

const describe = (c: Coupon) =>
  c.discountType === 'PERCENTAGE'
    ? `${c.discountValue}% off${c.maxDiscountAmount ? ` (max ${money(c.maxDiscountAmount)})` : ''}`
    : `${money(c.discountValue)} off`;

export function CouponsPage() {
  const { data, setData } = useAsync(() => api<Coupon[]>('/api/v1/admin/coupons'), []);
  const [form, setForm] = useState<Form>();
  const [err, setErr] = useState<string>();
  const reload = async () => setData(await api<Coupon[]>('/api/v1/admin/coupons'));

  const toggle = async (c: Coupon) => {
    const updated = await patch<Coupon>(`/api/v1/admin/coupons/${c.id}/active`, { active: !c.active });
    setData((d) => d?.map((x) => (x.id === c.id ? updated : x)));
  };

  const remove = async (c: Coupon) => {
    if (!confirm(`Delete ${c.code}?`)) return;
    try {
      await del(`/api/v1/admin/coupons/${c.id}`);
      reload();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : 'Delete failed');
    }
  };

  return (
    <>
      <PageHeader title="Coupons & promotions" actions={<button className="btn" onClick={() => setForm(toForm())}>New coupon</button>} />
      <ErrorBanner error={err} />
      <table className="table">
        <thead><tr><th>Code</th><th>Discount</th><th>Min cart</th><th>Who</th><th>Validity</th><th>Used</th><th>State</th><th /></tr></thead>
        <tbody>
          {data?.map((c) => (
            <tr key={c.id}>
              <td><strong className="mono">{c.code}</strong><div className="muted small">{c.description}</div></td>
              <td>{describe(c)}</td>
              <td>{money(c.minCartValue)}</td>
              <td>{label(c.targetUserType)}</td>
              <td className="small">{c.startsAt || c.endsAt ? <>{dateTime(c.startsAt) || 'now'} → {dateTime(c.endsAt) || 'no end'}</> : 'No limit'}</td>
              <td>{c.timesUsed}{c.usageLimitTotal ? ` / ${c.usageLimitTotal}` : ''}{c.usageLimitPerUser ? <div className="muted small">{c.usageLimitPerUser}/shopper</div> : null}</td>
              <td><Pill tone={STATE_TONE[c.state]}>{label(c.state)}</Pill></td>
              <td className="row-actions">
                <button className="btn small ghost" onClick={() => toggle(c)}>{c.active ? 'Deactivate' : 'Activate'}</button>
                <button className="btn small ghost" onClick={() => setForm(toForm(c))}>Edit</button>
                {c.timesUsed === 0 && <button className="btn small ghost danger" onClick={() => remove(c)}>Delete</button>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {form && <CouponForm initial={form} onClose={() => setForm(undefined)} onSaved={() => { setForm(undefined); reload(); }} />}
    </>
  );
}

function CouponForm({ initial, onClose, onSaved }: { initial: Form; onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState<Form>(initial);
  const [err, setErr] = useState<string>();
  const set = <K extends keyof Form>(k: K, v: Form[K]) => setF({ ...f, [k]: v });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const body = {
      code: f.code, description: f.description || undefined, discountType: f.discountType, discountValue: Number(f.discountValue),
      maxDiscountAmount: num(f.maxDiscountAmount), minCartValue: Number(f.minCartValue || 0), targetUserType: f.targetUserType,
      startsAt: f.startsAt || null, endsAt: f.endsAt || null, usageLimitTotal: num(f.usageLimitTotal),
      usageLimitPerUser: num(f.usageLimitPerUser), active: f.active,
    };
    try {
      f.id ? await put(`/api/v1/admin/coupons/${f.id}`, body) : await post('/api/v1/admin/coupons', body);
      onSaved();
    } catch (ex) {
      setErr(ex instanceof ApiError ? ex.message : 'Save failed');
    }
  };

  return (
    <Modal title={f.id ? `Edit ${f.code}` : 'New coupon'} onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <div className="grid-2 tight">
          <Field label="Code"><input required className="mono" value={f.code} onChange={(e) => set('code', e.target.value.toUpperCase())} pattern="[A-Za-z0-9_-]+" maxLength={40} /></Field>
          <Field label="Who can use it">
            <select value={f.targetUserType} onChange={(e) => set('targetUserType', e.target.value as Form['targetUserType'])}>
              <option value="ALL">Everyone</option><option value="GUEST">Guest checkout only</option><option value="REGISTERED">Signed-in members only</option>
            </select>
          </Field>
        </div>
        <Field label="Description (shown to shoppers)"><input value={f.description} onChange={(e) => set('description', e.target.value)} maxLength={300} /></Field>
        <div className="grid-2 tight">
          <Field label="Discount type">
            <select value={f.discountType} onChange={(e) => set('discountType', e.target.value as Form['discountType'])}>
              <option value="PERCENTAGE">Percentage</option><option value="FIXED_AMOUNT">Fixed amount (₹)</option>
            </select>
          </Field>
          <Field label={f.discountType === 'PERCENTAGE' ? 'Percent off' : 'Amount off (₹)'}>
            <input required type="number" step="0.01" min="0.01" max={f.discountType === 'PERCENTAGE' ? 100 : undefined} value={f.discountValue} onChange={(e) => set('discountValue', e.target.value)} />
          </Field>
          {f.discountType === 'PERCENTAGE' && (
            <Field label="Max discount (₹)" hint="Optional cap"><input type="number" step="0.01" min="0" value={f.maxDiscountAmount} onChange={(e) => set('maxDiscountAmount', e.target.value)} /></Field>
          )}
          <Field label="Minimum cart value (₹)"><input type="number" step="0.01" min="0" value={f.minCartValue} onChange={(e) => set('minCartValue', e.target.value)} /></Field>
          <Field label="Valid from"><input type="datetime-local" value={toLocalInput(f.startsAt)} onChange={(e) => set('startsAt', fromLocalInput(e.target.value))} /></Field>
          <Field label="Valid until"><input type="datetime-local" value={toLocalInput(f.endsAt)} onChange={(e) => set('endsAt', fromLocalInput(e.target.value))} /></Field>
          <Field label="Total usage cap" hint="Blank = unlimited"><input type="number" min="1" value={f.usageLimitTotal} onChange={(e) => set('usageLimitTotal', e.target.value)} /></Field>
          <Field label="Uses per shopper" hint="Blank = unlimited"><input type="number" min="1" value={f.usageLimitPerUser} onChange={(e) => set('usageLimitPerUser', e.target.value)} /></Field>
        </div>
        <label className="check"><input type="checkbox" checked={f.active} onChange={(e) => set('active', e.target.checked)} /> Active</label>
        <ErrorBanner error={err} />
        <button className="btn">Save coupon</button>
      </form>
    </Modal>
  );
}
