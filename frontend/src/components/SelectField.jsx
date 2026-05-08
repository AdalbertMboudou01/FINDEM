import React from 'react';

export default function SelectField({ label, value, onChange, options, placeholder, required }) {
  return (
    <label className="field">
      <span>{label}</span>
      <select className="field-select" value={value} onChange={onChange} required={required}>
        {placeholder && <option value="">{placeholder}</option>}
        {options.map(opt => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </label>
  );
}
