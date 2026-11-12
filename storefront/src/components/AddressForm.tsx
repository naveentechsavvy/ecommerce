import type { Address } from '../lib/types';

export const EMPTY_ADDRESS: Address = {
  fullName: '', phone: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: 'IN',
};

const STATES = [
  'Andhra Pradesh', 'Assam', 'Bihar', 'Delhi', 'Goa', 'Gujarat', 'Haryana', 'Karnataka', 'Kerala',
  'Madhya Pradesh', 'Maharashtra', 'Odisha', 'Punjab', 'Rajasthan', 'Tamil Nadu', 'Telangana',
  'Uttar Pradesh', 'West Bengal',
];

export function AddressForm({ value, onChange, idPrefix }: { value: Address; onChange: (a: Address) => void; idPrefix: string }) {
  const set = (k: keyof Address) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => onChange({ ...value, [k]: e.target.value });
  const field = (k: keyof Address, label: string, props: React.InputHTMLAttributes<HTMLInputElement> = {}) => (
    <label className="field" htmlFor={`${idPrefix}-${k}`}>
      <span>{label}</span>
      <input id={`${idPrefix}-${k}`} value={value[k] ?? ''} onChange={set(k)} {...props} />
    </label>
  );
  return (
    <div className="form-grid">
      {field('fullName', 'Full name', { required: true, autoComplete: 'name' })}
      {field('phone', 'Phone', { required: true, autoComplete: 'tel', pattern: '[0-9+ -]{8,20}', inputMode: 'tel' })}
      {field('line1', 'Address line 1', { required: true, autoComplete: 'address-line1' })}
      {field('line2', 'Address line 2 (optional)', { autoComplete: 'address-line2' })}
      {field('city', 'City', { required: true, autoComplete: 'address-level2' })}
      <label className="field" htmlFor={`${idPrefix}-state`}>
        <span>State</span>
        <select id={`${idPrefix}-state`} value={value.state} onChange={set('state')} required>
          <option value="">Select…</option>
          {STATES.map((s) => <option key={s}>{s}</option>)}
        </select>
      </label>
      {field('postalCode', 'PIN code', { required: true, autoComplete: 'postal-code', pattern: '[0-9]{6}', inputMode: 'numeric' })}
    </div>
  );
}
