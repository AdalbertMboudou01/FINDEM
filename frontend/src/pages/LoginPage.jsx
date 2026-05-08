import React, { useState } from 'react';
import { Link, useNavigate, Navigate } from 'react-router-dom';
import Field from '../components/Field';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="login-title">
        <div className="brand-name">Findem</div>
        <h1 id="login-title">Welcome back</h1>

        <form className="auth-form" onSubmit={handleSubmit}>
          <Field
            label="Email"
            type="email"
            placeholder="e.g. howard.thurman@gmail.com"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
          <Field
            label="Mot de passe"
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            required
          />
          {error && <p className="auth-error">{error}</p>}
          <button type="submit" disabled={loading}>
            {loading ? 'Connexion…' : 'Se connecter'}
          </button>
        </form>

        <p className="auth-copy" style={{ textAlign: 'center', marginTop: 8 }}>
          <Link to="/forgot-password" style={{ color: '#6366f1', fontSize: 13 }}>Mot de passe oublié ?</Link>
        </p>

        <p className="auth-copy">
          Pas de compte ? <Link to="/signup">Créer un espace</Link>
        </p>
      </section>
    </main>
  );
}
