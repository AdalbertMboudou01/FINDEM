import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

export default function Field({ label, type = 'text', placeholder = '', value, onChange, required }) {
  const [visible, setVisible] = useState(false);
  const isPassword = type === 'password';
  const inputType = isPassword ? (visible ? 'text' : 'password') : type;

  return (
    <label className="field">
      <span>{label}</span>
      <input
        type={inputType}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        required={required}
      />
      {isPassword && (
        <button
          type="button"
          className="field-eye"
          onClick={() => setVisible(v => !v)}
          aria-label={visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
        >
          {visible ? <EyeOff size={20} /> : <Eye size={20} />}
        </button>
      )}
    </label>
  );
}
