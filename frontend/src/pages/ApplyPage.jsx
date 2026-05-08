import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Bot, ChevronRight, ChevronLeft, CheckCircle } from 'lucide-react';

async function apiFetch(path, options = {}) {
  const res = await fetch(path, { headers: { 'Content-Type': 'application/json' }, ...options });
  if (!res.ok) {
    const text = await res.text().catch(() => 'Erreur');
    throw new Error(text || 'Erreur serveur');
  }
  const ct = res.headers.get('content-type') ?? '';
  return ct.includes('application/json') ? res.json() : null;
}

const STEPS = { LOADING: 'loading', ERROR: 'error', CLOSED: 'closed', INFO: 'info', QUESTIONS: 'questions', DONE: 'done' };

export default function ApplyPage() {
  const { slug } = useParams();

  const [step,      setStep]      = useState(STEPS.LOADING);
  const [job,       setJob]       = useState(null);
  const [questions, setQuestions] = useState([]);
  const [qIndex,    setQIndex]    = useState(0);
  const [answers,   setAnswers]   = useState({});
  const [error,     setError]     = useState('');
  const [submitting, setSubmitting] = useState(false);

  /* IDs retournés par /apply */
  const [appId,  setAppId]  = useState(null);
  const [candId, setCandId] = useState(null);

  /* Formulaire infos candidat */
  const [info, setInfo] = useState({
    firstName: '', lastName: '', email: '', phone: '',
    school: '', githubUrl: '', portfolioUrl: '', consent: false,
  });
  const [cvFile,     setCvFile]     = useState(null);
  const [infoError,  setInfoError]  = useState('');

  /* Chargement offre + questions */
  useEffect(() => {
    async function init() {
      try {
        const jobData = await apiFetch(`/api/jobs/slug/${slug}`);
        setJob(jobData);
        if (!jobData.isAccepting) { setStep(STEPS.CLOSED); return; }

        const qs = await apiFetch(`/api/jobs/${jobData.jobId}/questions`);
        setQuestions(Array.isArray(qs) ? qs.sort((a, b) => a.orderIndex - b.orderIndex) : []);
        setStep(STEPS.INFO);
      } catch (e) {
        setError(e.message);
        setStep(STEPS.ERROR);
      }
    }
    init();
  }, [slug]);

  /* ── Étape 1 : soumettre les infos candidat ── */
  async function handleInfoSubmit(e) {
    e.preventDefault();
    if (!info.consent) { setInfoError('Vous devez accepter le traitement de vos données.'); return; }
    if (!cvFile)       { setInfoError('Le CV est obligatoire.'); return; }
    setInfoError('');
    setSubmitting(true);
    try {
      const res = await apiFetch('/api/apply', {
        method: 'POST',
        body: JSON.stringify({ ...info, jobId: job.jobId }),
      });
      setAppId(res.applicationId);
      setCandId(res.candidateId);

      /* Upload CV */
      const fd = new FormData();
      fd.append('file', cvFile);
      fd.append('candidateId', res.candidateId);
      fd.append('fileType', 'CV');
      await fetch('/api/files/upload', { method: 'POST', body: fd }).catch(() => {});

      if (questions.length === 0) { setStep(STEPS.DONE); return; }
      setQIndex(0);
      setStep(STEPS.QUESTIONS);
    } catch (e) {
      setInfoError(e.message);
    } finally {
      setSubmitting(false);
    }
  }

  /* ── Étape 2 : envoyer une réponse et avancer ── */
  async function handleNext(e) {
    e.preventDefault();
    const q = questions[qIndex];
    const answer = answers[q.id] ?? '';

    if (q.required && !answer.trim()) {
      setError('Cette question est obligatoire. Veuillez saisir une réponse pour continuer.');
      return;
    }
    setError('');
    setSubmitting(true);
    try {
      await apiFetch('/api/chat-answers/submit', {
        method: 'POST',
        body: JSON.stringify({
          questionKey:   q.id,
          questionText:  q.questionText,
          answer:        answer.trim() || '—',
          applicationId: appId,
          candidateId:   candId,
          required:      q.required ?? false,
        }),
      });
      if (qIndex < questions.length - 1) {
        setQIndex(i => i + 1);
      } else {
        apiFetch(`/api/apply/${appId}/finalize`, { method: 'POST' }).catch(() => {});
        setStep(STEPS.DONE);
      }
    } catch (e) {
      setError(e.message);
    } finally {
      setSubmitting(false);
    }
  }

  function handleBack() {
    setError('');
    setQIndex(i => i - 1);
  }

  /* ── Rendu ── */
  return (
    <div className="apply-page">
      <div className="apply-header">
        <Bot size={22} className="apply-header-icon" />
        <span className="apply-header-title">
          Candidature{job ? ` — ${job.title}` : ''}
        </span>
      </div>

      <div className="apply-card">

        {/* Chargement */}
        {step === STEPS.LOADING && <p className="apply-loading">Chargement de l'offre…</p>}

        {/* Erreur */}
        {step === STEPS.ERROR && (
          <div className="apply-alert error">
            <p>{error || "Cette offre est introuvable."}</p>
          </div>
        )}

        {/* Offre fermée */}
        {step === STEPS.CLOSED && (
          <div className="apply-alert warning">
            <p>Cette offre n'accepte plus de candidatures.</p>
          </div>
        )}

        {/* Étape 1 — Informations */}
        {step === STEPS.INFO && (
          <form onSubmit={handleInfoSubmit}>
            <h2 className="apply-section-title">Vos informations</h2>
            <p className="apply-section-hint">
              Ces informations sont nécessaires pour associer vos réponses à votre candidature.
            </p>

            <div className="apply-row-2">
              <ApplyField label="Prénom *" required value={info.firstName}
                onChange={e => setInfo(p => ({ ...p, firstName: e.target.value }))} />
              <ApplyField label="Nom *" required value={info.lastName}
                onChange={e => setInfo(p => ({ ...p, lastName: e.target.value }))} />
            </div>

            <ApplyField label="Email *" type="email" required value={info.email}
              onChange={e => setInfo(p => ({ ...p, email: e.target.value }))} />

            <div className="apply-row-2">
              <ApplyField label="Téléphone *" type="tel" required value={info.phone}
                onChange={e => setInfo(p => ({ ...p, phone: e.target.value }))} />
              <ApplyField label="École / Formation" value={info.school}
                onChange={e => setInfo(p => ({ ...p, school: e.target.value }))} />
            </div>

            <div className="apply-field">
              <label className="apply-label">CV * <span style={{ fontWeight: 400, color: '#6b7280', fontSize: 11 }}>(PDF, DOC, DOCX — max 5 Mo)</span></label>
              <input
                className="apply-input"
                type="file"
                accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                onChange={e => setCvFile(e.target.files?.[0] ?? null)}
                required
              />
              {cvFile && <span style={{ fontSize: 11, color: '#059669', marginTop: 4, display: 'block' }}>✓ {cvFile.name}</span>}
            </div>

            <div className="apply-row-2">
              <ApplyField label="Lien GitHub" type="url" placeholder="https://github.com/…" value={info.githubUrl}
                onChange={e => setInfo(p => ({ ...p, githubUrl: e.target.value }))} />
              <ApplyField label="Portfolio" type="url" placeholder="https://votre-portfolio.com" value={info.portfolioUrl}
                onChange={e => setInfo(p => ({ ...p, portfolioUrl: e.target.value }))} />
            </div>

            <label className="apply-checkbox">
              <input type="checkbox" checked={info.consent}
                onChange={e => setInfo(p => ({ ...p, consent: e.target.checked }))} />
              J'accepte que mes données soient traitées dans le cadre de ma candidature.&nbsp;*
            </label>

            {infoError && <p className="apply-error">{infoError}</p>}

            <div className="apply-actions">
              <button type="submit" className="apply-btn-primary" disabled={submitting}>
                {submitting ? 'Envoi…' : 'Commencer le questionnaire'} <ChevronRight size={16} />
              </button>
            </div>
          </form>
        )}

        {/* Étape 2 — Questions */}
        {step === STEPS.QUESTIONS && questions[qIndex] && (() => {
          const q   = questions[qIndex];
          const pct = Math.round(((qIndex) / questions.length) * 100);
          return (
            <form onSubmit={handleNext}>
              {/* Barre de progression */}
              <div className="apply-progress-header">
                <span className="apply-progress-label">Question {qIndex + 1} / {questions.length}</span>
                <span className="apply-progress-pct">{pct}%</span>
              </div>
              <div className="apply-progress-bar">
                <div className="apply-progress-fill" style={{ width: `${pct}%` }} />
              </div>

              <p className="apply-question-text">
                {q.questionText} {q.required && <span className="apply-required">*</span>}
              </p>

              {q.answerType === 'yesno' ? (
                <div className="apply-yesno">
                  {['Oui', 'Non'].map(v => (
                    <button
                      key={v}
                      type="button"
                      className={`apply-yesno-btn${answers[q.id] === v ? ' selected' : ''}`}
                      onClick={() => setAnswers(p => ({ ...p, [q.id]: v }))}
                    >
                      {v}
                    </button>
                  ))}
                </div>
              ) : q.answerType === 'scale' ? (
                <div className="apply-scale">
                  {[1, 2, 3, 4, 5].map(n => (
                    <button
                      key={n}
                      type="button"
                      className={`apply-scale-btn${answers[q.id] === String(n) ? ' selected' : ''}`}
                      onClick={() => setAnswers(p => ({ ...p, [q.id]: String(n) }))}
                    >
                      {n}
                    </button>
                  ))}
                  <div className="apply-scale-labels">
                    <span>Débutant</span><span>Expert</span>
                  </div>
                </div>
              ) : (
                <textarea
                  className="apply-textarea"
                  placeholder="Votre réponse…"
                  rows={5}
                  value={answers[q.id] ?? ''}
                  onChange={e => setAnswers(p => ({ ...p, [q.id]: e.target.value }))}
                />
              )}

              {error && <p className="apply-error">{error}</p>}

              <div className="apply-actions apply-actions-split">
                <button type="button" className="apply-btn-back" onClick={handleBack} disabled={qIndex === 0}>
                  <ChevronLeft size={15} /> Précédent
                </button>
                <button type="submit" className="apply-btn-primary" disabled={submitting}>
                  {submitting ? 'Envoi…' : qIndex === questions.length - 1 ? 'Envoyer' : 'Suivant'}
                  {!submitting && <ChevronRight size={15} />}
                </button>
              </div>
            </form>
          );
        })()}

        {/* Étape 3 — Succès */}
        {step === STEPS.DONE && (
          <div className="apply-success">
            <CheckCircle size={28} className="apply-success-icon" />
            <div>
              <p className="apply-success-title">Réponses envoyées avec succès.</p>
              <p className="apply-success-hint">Merci, votre candidature a bien été enregistrée.</p>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}

function ApplyField({ label, type = 'text', placeholder, value, onChange, required }) {
  return (
    <div className="apply-field">
      <label className="apply-label">{label}</label>
      <input
        className="apply-input"
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        required={required}
      />
    </div>
  );
}
