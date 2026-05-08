import React, { useState } from 'react';
import { Link } from 'react-router-dom';

export default function ForgotPasswordPage() {
  const [email,   setEmail]   = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error,   setError]   = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      const res = await fetch('/api/password-reset/forgot', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message ?? 'Erreur');
      setSuccess(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-logo">Findem</div>
        <h1 className="auth-title">Mot de passe oublié</h1>
        <p className="auth-subtitle">Entrez votre email pour recevoir un lien de réinitialisation.</p>

        {success ? (
          <div style={{ textAlign: 'center' }}>
            <p style={{ color: '#059669', fontWeight: 600, marginBottom: 16 }}>
              Un lien a été envoyé à {email}.
            </p>
            <Link to="/" style={{ color: '#6366f1', fontSize: 13 }}>Retour à la connexion</Link>
          </div>
        ) : (
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="form-field">
              <label>Email</label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="votre@email.com"
                required
              />
            </div>
            {error && <p className="auth-error">{error}</p>}
            <button type="submit" disabled={loading || !email}>
              {loading ? 'Envoi…' : 'Envoyer le lien'}
            </button>
          </form>
        )}

        <p className="auth-copy">
          <Link to="/">Retour à la connexion</Link>
        </p>
      </section>
    </main>
  );
}
