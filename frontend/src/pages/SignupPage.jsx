import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Field from '../components/Field';
import SelectField from '../components/SelectField';
import { useAuth } from '../context/AuthContext';

const SECTORS = [
  { value: 'Tech', label: 'Tech / Numérique' },
  { value: 'Finance', label: 'Finance / Banque' },
  { value: 'Santé', label: 'Santé / Médical' },
  { value: 'Commerce', label: 'Commerce / Retail' },
  { value: 'Industrie', label: 'Industrie / Manufacturing' },
  { value: 'Éducation', label: 'Éducation / Formation' },
  { value: 'Services', label: 'Services aux entreprises' },
  { value: 'Autre', label: 'Autre' },
];

const SIZES = [
  { value: '1-10',    label: '1 – 10 employés' },
  { value: '11-50',   label: '11 – 50 employés' },
  { value: '51-200',  label: '51 – 200 employés' },
  { value: '201-500', label: '201 – 500 employés' },
  { value: '500+',    label: 'Plus de 500 employés' },
];

const EMPTY = {
  firstName: '', lastName: '', email: '', password: '', confirmPassword: '',
  companyName: '', sector: '', size: '', website: '',
};

export default function SignupPage() {
  const { register, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [step, setStep]     = useState(1);
  const [form, setForm]     = useState(EMPTY);
  const [error, setError]   = useState('');
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    navigate('/dashboard', { replace: true });
    return null;
  }

  const set = key => e => setForm(f => ({ ...f, [key]: e.target.value }));

  function validateStep1() {
    if (!form.firstName || !form.lastName || !form.email || !form.password || !form.confirmPassword)
      return 'Tous les champs sont obligatoires.';
    if (form.password.length < 8)
      return 'Le mot de passe doit contenir au moins 8 caractères.';
    if (form.password !== form.confirmPassword)
      return 'Les mots de passe ne correspondent pas.';
    return null;
  }

  function handleNext(e) {
    e.preventDefault();
    const err = validateStep1();
    if (err) { setError(err); return; }
    setError('');
    setStep(2);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!form.companyName || !form.sector || !form.size) {
      setError('Veuillez remplir tous les champs obligatoires.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await register({
        fullName:        `${form.firstName} ${form.lastName}`,
        email:           form.email,
        password:        form.password,
        confirmPassword: form.confirmPassword,
        companyName:     form.companyName,
        sector:          form.sector,
        size:            form.size,
        website:         form.website || undefined,
      });
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="signup-title">
        <div className="brand-name">Findem</div>
        <h1 id="signup-title">Créer un espace</h1>

        <div className="auth-steps">
          <span className={step === 1 ? 'active' : 'done'}>1</span>
          <div className="auth-steps-line" />
          <span className={step === 2 ? 'active' : ''}>2</span>
        </div>
        <p className="auth-step-label">
          {step === 1 ? 'Votre compte' : 'Votre entreprise'}
        </p>

        {step === 1 ? (
          <form className="auth-form" onSubmit={handleNext}>
            <div className="name-row">
              <Field label="Prénom"  placeholder="Howard" value={form.firstName} onChange={set('firstName')} required />
              <Field label="Nom"     placeholder="Thurman" value={form.lastName}  onChange={set('lastName')}  required />
            </div>
            <Field label="Email"           type="email"    placeholder="howard@exemple.com" value={form.email}            onChange={set('email')}           required />
            <Field label="Mot de passe"    type="password" value={form.password}            onChange={set('password')}    required />
            <Field label="Confirmer"       type="password" value={form.confirmPassword}      onChange={set('confirmPassword')} required />
            {error && <p className="auth-error">{error}</p>}
            <button type="submit">Suivant →</button>
          </form>
        ) : (
          <form className="auth-form" onSubmit={handleSubmit}>
            <Field
              label="Nom de l'entreprise"
              placeholder="Acme Corp"
              value={form.companyName}
              onChange={set('companyName')}
              required
            />
            <SelectField
              label="Secteur"
              options={SECTORS}
              placeholder="Choisir un secteur"
              value={form.sector}
              onChange={set('sector')}
              required
            />
            <SelectField
              label="Taille"
              options={SIZES}
              placeholder="Choisir une taille"
              value={form.size}
              onChange={set('size')}
              required
            />
            <Field
              label="Site web (optionnel)"
              type="url"
              placeholder="https://acme.com"
              value={form.website}
              onChange={set('website')}
            />
            {error && <p className="auth-error">{error}</p>}
            <div className="auth-form-actions">
              <button type="button" className="btn-secondary" onClick={() => { setError(''); setStep(1); }}>
                ← Retour
              </button>
              <button type="submit" disabled={loading}>
                {loading ? 'Création…' : 'Créer l\'espace'}
              </button>
            </div>
          </form>
        )}

        <p className="auth-copy">
          Déjà un compte ? <Link to="/">Se connecter</Link>
        </p>
      </section>
    </main>
  );
}
