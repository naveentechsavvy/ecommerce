import { type ReactNode } from 'react';
import type { Visibility } from '../lib/types';
import { VISIBILITIES } from '../lib/types';

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <header>
          <h2>{title}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Close">✕</button>
        </header>
        {children}
      </div>
    </div>
  );
}

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small className="muted">{hint}</small>}
    </label>
  );
}

export function VisibilitySelect({ value, onChange }: { value: Visibility; onChange: (v: Visibility) => void }) {
  return (
    <select className={`vis vis-${value.toLowerCase()}`} value={value} onChange={(e) => onChange(e.target.value as Visibility)}>
      {VISIBILITIES.map((v) => <option key={v} value={v}>{v === 'ENABLED' ? 'Enabled' : v === 'DISABLED' ? 'Disabled (greyed)' : 'Hidden'}</option>)}
    </select>
  );
}

export function Pill({ tone, children }: { tone: 'green' | 'amber' | 'red' | 'blue' | 'grey'; children: ReactNode }) {
  return <span className={`pill pill-${tone}`}>{children}</span>;
}

export function ErrorBanner({ error }: { error?: string }) {
  return error ? <div className="error-banner" role="alert">{error}</div> : null;
}

export function PageHeader({ title, actions }: { title: string; actions?: ReactNode }) {
  return (
    <div className="page-header">
      <h1>{title}</h1>
      <div className="actions">{actions}</div>
    </div>
  );
}

/** num("") -> undefined so optional numeric fields serialise as null. */
export const num = (v: string): number | undefined => (v.trim() === '' ? undefined : Number(v));
