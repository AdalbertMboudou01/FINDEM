import React, { useEffect, useState } from 'react';
import { Plus, MapPin, Calendar, Users, Pencil, Trash2, ChevronRight, Briefcase, Link2, MessageSquare, GripVertical, X, Archive, XCircle, Tag, ExternalLink, Copy, Check } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { useAuth } from '../context/AuthContext';

const STATUT = {
  ouvert:    { label: 'Ouvert',    bg: '#d1fae5', color: '#059669' },
  pause:     { label: 'En pause',  bg: '#fef3c7', color: '#d97706' },
  'clôturé': { label: 'Clôturé',  bg: '#fee2e2', color: '#dc2626' },
  archivé:   { label: 'Archivé',  bg: '#f1f5f9', color: '#64748b' },
};

function StatutBadge({ statut }) {
  const s = STATUT[statut] ?? { label: statut ?? '—', bg: '#f1f5f9', color: '#64748b' };
  return (
    <span style={{ background: s.bg, color: s.color, padding: '3px 10px', borderRadius: 20, fontSize: 12, fontWeight: 600 }}>
      {s.label}
    </span>
  );
}

function timeAgo(date) {
  if (!date) return '';
  const days = Math.floor((Date.now() - new Date(date)) / 86400000);
  if (days === 0) return "aujourd'hui";
  if (days === 1) return 'hier';
  return `il y a ${days}j`;
}

function applyUrl(slug) {
  return `${window.location.origin}/#/apply/${slug}`;
}

const EMPTY = {
  title: '', description: '', location: '', alternanceRhythm: '',
  statut: 'ouvert', maxCandidatures: '', autoClose: true,
  technologies: [], contextePoste: '', missionsDetaillees: '', serviceEntreprise: '',
};
const FILTERS = [['all', 'Toutes'], ['ouvert', 'Ouvertes'], ['pause', 'En pause'], ['clôturé', 'Clôturées'], ['archivé', 'Archivées']];
const EMPTY_QUESTION = { questionText: '', answerType: 'open', required: true };

const SUGGESTED_QUESTIONS = [
  { questionText: "Pourquoi cette alternance vous intéresse-t-elle ?",              answerType: 'open',  required: true  },
  { questionText: "Parlez-moi de votre formation actuelle.",                        answerType: 'open',  required: true  },
  { questionText: "Quelles compétences techniques apportez-vous à ce poste ?",      answerType: 'open',  required: true  },
  { questionText: "Décrivez un projet ou une expérience dont vous êtes fier(e).",   answerType: 'open',  required: false },
  { questionText: "Qu'est-ce qui vous attire dans notre entreprise ?",              answerType: 'open',  required: true  },
  { questionText: "Comment gérez-vous les situations de stress ou d'urgence ?",     answerType: 'open',  required: false },
  { questionText: "Quelle est votre disponibilité pour démarrer l'alternance ?",    answerType: 'open',  required: true  },
  { questionText: "Avez-vous déjà une expérience en entreprise ?",                  answerType: 'yesno', required: true  },
  { questionText: "Êtes-vous disponible pour travailler en présentiel ?",           answerType: 'yesno', required: true  },
  { questionText: "Avez-vous le permis de conduire ?",                              answerType: 'yesno', required: false },
  { questionText: "Évaluez votre niveau sur les technologies du poste (1 = débutant, 5 = expert).", answerType: 'scale', required: true },
  { questionText: "Évaluez votre aisance à travailler en équipe.",                  answerType: 'scale', required: false },
];

function TechTagInput({ value = [], onChange }) {
  const [input, setInput] = useState('');

  function addTag(raw) {
    const tag = raw.trim();
    if (!tag || value.includes(tag)) { setInput(''); return; }
    onChange([...value, tag]);
    setInput('');
  }

  function handleKey(e) {
    if (e.key === 'Enter' || e.key === ',') { e.preventDefault(); addTag(input); }
    if (e.key === 'Backspace' && !input) onChange(value.slice(0, -1));
  }

  return (
    <div className="tech-tag-input">
      {value.map((t, i) => (
        <span key={i} className="tech-tag-chip">
          {t}
          <button type="button" onClick={() => onChange(value.filter((_, j) => j !== i))}><X size={10} /></button>
        </span>
      ))}
      <input
        value={input}
        onChange={e => setInput(e.target.value)}
        onKeyDown={handleKey}
        onBlur={() => addTag(input)}
        placeholder={value.length === 0 ? 'React, Node.js, Python… (Entrée pour valider)' : ''}
        className="tech-tag-input-field"
      />
    </div>
  );
}

export default function OffresPage() {
  const { session } = useAuth();
  const navigate    = useNavigate();
  const token       = session?.token;

  const [jobs,        setJobs]        = useState([]);
  const [loading,     setLoading]     = useState(true);
  const [filter,      setFilter]      = useState('all');
  const [showDrawer,  setShowDrawer]  = useState(false);
  const [editing,     setEditing]     = useState(null);
  const [form,        setForm]        = useState(EMPTY);
  const [saving,      setSaving]      = useState(false);
  const [formError,   setFormError]   = useState('');
  const [copied,      setCopied]      = useState(null);

  /* Questions drawer */
  const [qJob,        setQJob]        = useState(null);
  const [questions,   setQuestions]   = useState([]);
  const [qLoading,    setQLoading]    = useState(false);
  const [newQ,        setNewQ]        = useState(EMPTY_QUESTION);
  const [qSaving,     setQSaving]     = useState(false);

  useEffect(() => { load(); }, []);

  async function load() {
    setLoading(true);
    const data = await api.get('/api/jobs', token).catch(() => []);
    setJobs(Array.isArray(data) ? data : []);
    setLoading(false);
  }

  const filtered = filter === 'all' ? jobs : jobs.filter(j => j.statut === filter);

  /* ── Offre drawer ── */
  function openCreate() { setEditing(null); setForm(EMPTY); setFormError(''); setShowDrawer(true); }

  function openEdit(job) {
    setEditing(job);
    setForm({
      title: job.title ?? '', description: job.description ?? '',
      location: job.location ?? '', alternanceRhythm: job.alternanceRhythm ?? '',
      statut: job.statut ?? 'ouvert',
      maxCandidatures: job.maxCandidatures ?? '', autoClose: job.autoClose ?? true,
      technologies: job.technologies ?? [],
      contextePoste: job.contextePoste ?? '',
      missionsDetaillees: job.missionsDetaillees ?? '',
      serviceEntreprise: job.serviceEntreprise ?? '',
    });
    setFormError(''); setShowDrawer(true);
  }

  async function handleQuickStatus(job, newStatut) {
    try {
      const updated = await api.put(`/api/jobs/${job.jobId}`, { ...job, statut: newStatut }, token);
      setJobs(p => p.map(j => j.jobId === job.jobId ? updated : j));
    } catch { /* silent */ }
  }

  function closeDrawer() { setShowDrawer(false); setFormError(''); }

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  async function handleSave(e) {
    e.preventDefault();
    if (!form.title || !form.description || !form.location) {
      setFormError('Titre, description et localisation sont obligatoires.'); return;
    }
    setSaving(true); setFormError('');
    try {
      if (editing) {
        const updated = await api.put(`/api/jobs/${editing.jobId}`, {
          ...editing, ...form,
          maxCandidatures: form.maxCandidatures ? Number(form.maxCandidatures) : null,
        }, token);
        setJobs(p => p.map(j => j.jobId === editing.jobId ? updated : j));
      } else {
        const created = await api.post('/api/jobs', {
          title: form.title, description: form.description, location: form.location,
          alternanceRhythm: form.alternanceRhythm || null,
          companyId: session.companyId, ownerRecruiterId: session.recruiterId,
          technologies: form.technologies ?? [],
          contextePoste: form.contextePoste || null,
          missionsDetaillees: form.missionsDetaillees || null,
          serviceEntreprise: form.serviceEntreprise || null,
          maxCandidatures: form.maxCandidatures ? Number(form.maxCandidatures) : null,
          autoClose: form.autoClose,
        }, token);
        setJobs(p => [created, ...p]);
      }
      closeDrawer();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(job) {
    if (!confirm(`Supprimer l'offre "${job.title}" ?`)) return;
    await api.del(`/api/jobs/${job.jobId}`, token).catch(() => {});
    setJobs(p => p.filter(j => j.jobId !== job.jobId));
  }

  /* ── Copy link ── */
  function handleCopyLink(job) {
    if (!job.slug) return;
    navigator.clipboard.writeText(applyUrl(job.slug)).then(() => {
      setCopied(job.jobId);
      setTimeout(() => setCopied(null), 2000);
    });
  }

  /* ── Questions drawer ── */
  async function openQuestions(job) {
    setQJob(job);
    setQLoading(true);
    setNewQ(EMPTY_QUESTION);
    const data = await api.get(`/api/jobs/${job.jobId}/questions`, token).catch(() => []);
    setQuestions(Array.isArray(data) ? data.sort((a, b) => a.orderIndex - b.orderIndex) : []);
    setQLoading(false);
  }

  function closeQuestions() { setQJob(null); setQuestions([]); }

  async function handleAddQuestion(e) {
    e.preventDefault();
    if (!newQ.questionText.trim()) return;
    setQSaving(true);
    try {
      const created = await api.post(`/api/jobs/${qJob.jobId}/questions`, {
        questionText: newQ.questionText.trim(),
        answerType: newQ.answerType,
        required: newQ.required,
        orderIndex: questions.length,
      }, token);
      setQuestions(p => [...p, created]);
      setNewQ(EMPTY_QUESTION);
    } catch { /* silent */ } finally {
      setQSaving(false);
    }
  }

  async function handleDeleteQuestion(q) {
    await api.del(`/api/jobs/${qJob.jobId}/questions/${q.id}`, token).catch(() => {});
    setQuestions(p => p.filter(x => x.id !== q.id));
  }

  async function handleAddSuggested(suggestion) {
    const already = questions.some(q => q.questionText === suggestion.questionText);
    if (already) return;
    setQSaving(true);
    try {
      const created = await api.post(`/api/jobs/${qJob.jobId}/questions`, {
        ...suggestion,
        orderIndex: questions.length,
      }, token);
      setQuestions(p => [...p, created]);
    } catch { /* silent */ } finally {
      setQSaving(false);
    }
  }

  return (
    <div className="offres-page">
      {/* ── Toolbar ── */}
      <div className="page-toolbar">
        <div className="filter-tabs">
          {FILTERS.map(([val, lbl]) => (
            <button key={val} className={`filter-tab${filter === val ? ' active' : ''}`} onClick={() => setFilter(val)}>
              {lbl}
              {val !== 'all' && <span className="filter-tab-count">{jobs.filter(j => j.statut === val).length}</span>}
            </button>
          ))}
        </div>
        <button className="btn-primary" onClick={openCreate}>
          <Plus size={16} strokeWidth={2.5} /> Nouvelle offre
        </button>
      </div>

      {/* ── Content ── */}
      {loading ? (
        <p className="panel-empty" style={{ marginTop: 32 }}>Chargement…</p>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon"><Briefcase size={32} strokeWidth={1.5} /></div>
          <p className="empty-state-title">
            {filter === 'all' ? 'Aucune offre créée' : 'Aucune offre avec ce statut'}
          </p>
          <p className="empty-state-hint">
            {filter === 'all'
              ? "Cliquez sur \"+ Nouvelle offre\" pour publier votre première offre d'alternance."
              : 'Modifiez le filtre ou créez une nouvelle offre.'}
          </p>
        </div>
      ) : (
        <div className="job-grid">
          {filtered.map(job => (
            <div key={job.jobId} className="job-card">
              <div className="job-card-top">
                <StatutBadge statut={job.statut} />
                <div className="job-card-actions">
                  <button className="icon-btn" onClick={() => openEdit(job)} title="Modifier"><Pencil size={15} /></button>
                  <button className="icon-btn danger" onClick={() => handleDelete(job)} title="Supprimer"><Trash2 size={15} /></button>
                </div>
              </div>

              <h3 className="job-card-title">{job.title}</h3>

              <div className="job-card-meta">
                {job.location         && <span><MapPin size={13} strokeWidth={2} /> {job.location}</span>}
                {job.alternanceRhythm && <span><Calendar size={13} strokeWidth={2} /> {job.alternanceRhythm}</span>}
                {job.maxCandidatures  && <span><Users size={13} strokeWidth={2} /> max {job.maxCandidatures}</span>}
              </div>

              {job.technologies?.length > 0 && (
                <div className="job-card-techs">
                  <Tag size={11} style={{ color: '#6b7280', flexShrink: 0 }} />
                  {job.technologies.slice(0, 4).map((t, i) => (
                    <span key={i} className="job-tech-tag">{t}</span>
                  ))}
                  {job.technologies.length > 4 && <span className="job-tech-more">+{job.technologies.length - 4}</span>}
                </div>
              )}

              {job.description && (
                <p className="job-card-desc">
                  {job.description.length > 110 ? job.description.slice(0, 110) + '…' : job.description}
                </p>
              )}


              <div className="job-card-quick-actions">
                {job.statut !== 'clôturé' && job.statut !== 'archivé' && (
                  <button className="job-quick-btn" onClick={() => handleQuickStatus(job, 'clôturé')} title="Fermer l'offre">
                    <XCircle size={12} /> Fermer
                  </button>
                )}
                {job.statut !== 'archivé' && (
                  <button className="job-quick-btn" onClick={() => handleQuickStatus(job, 'archivé')} title="Archiver">
                    <Archive size={12} /> Archiver
                  </button>
                )}
                {(job.statut === 'clôturé' || job.statut === 'archivé') && (
                  <button className="job-quick-btn green" onClick={() => handleQuickStatus(job, 'ouvert')} title="Réouvrir">
                    <Plus size={12} /> Rouvrir
                  </button>
                )}
              </div>

              <div className="job-card-footer">
                <span className="job-card-date">{timeAgo(job.createdAt)}</span>
                <div style={{ display: 'flex', gap: 6 }}>
                  <button className="btn-ghost" onClick={() => openQuestions(job)} title="Configurer le questionnaire candidat">
                    <MessageSquare size={13} /> Questionnaire
                  </button>
                  <button className="btn-ghost" onClick={() => navigate(`/candidatures?jobId=${job.jobId}`)}>
                    Candidatures <ChevronRight size={14} />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ── Drawer offre ── */}
      {showDrawer && (
        <>
          <div className="drawer-overlay" onClick={closeDrawer} />
          <aside className="drawer">
            <div className="drawer-header">
              <h3>{editing ? "Modifier l'offre" : 'Nouvelle offre'}</h3>
              <button className="icon-btn" onClick={closeDrawer}>✕</button>
            </div>
            <form className="drawer-form" onSubmit={handleSave}>
              <div className="form-field">
                <label>Titre *</label>
                <input value={form.title} onChange={set('title')} placeholder="Ex: Développeur React" required />
              </div>
              <div className="form-field">
                <label>Description *</label>
                <textarea value={form.description} onChange={set('description')} placeholder="Décrivez le poste, les missions…" rows={4} required />
              </div>
              <div className="form-row-2">
                <div className="form-field">
                  <label>Localisation *</label>
                  <input value={form.location} onChange={set('location')} placeholder="Paris, Lyon…" required />
                </div>
                <div className="form-field">
                  <label>Durée</label>
                  <select value={form.alternanceRhythm} onChange={set('alternanceRhythm')}>
                    <option value="">— Sélectionner —</option>
                    <option value="12 mois">12 mois</option>
                    <option value="24 mois">24 mois</option>
                    <option value="36 mois">36 mois</option>
                  </select>
                </div>
              </div>
              <div className="form-row-2">
                <div className="form-field">
                  <label>Statut</label>
                  <select value={form.statut} onChange={set('statut')}>
                    <option value="ouvert">Ouvert</option>
                    <option value="pause">En pause</option>
                    <option value="clôturé">Clôturé</option>
                  </select>
                </div>
                <div className="form-field">
                  <label>Max candidatures</label>
                  <input type="number" value={form.maxCandidatures} onChange={set('maxCandidatures')} placeholder="50" min={1} />
                </div>
              </div>
              <label className="form-checkbox">
                <input type="checkbox" checked={form.autoClose} onChange={set('autoClose')} />
                Fermer automatiquement quand le maximum est atteint
              </label>

              <div className="form-field">
                <label>Technologies requises</label>
                <TechTagInput
                  value={form.technologies}
                  onChange={tags => setForm(f => ({ ...f, technologies: tags }))}
                />
              </div>

              <div className="form-field">
                <label>Service / Équipe</label>
                <input value={form.serviceEntreprise} onChange={set('serviceEntreprise')} placeholder="Ex: Équipe produit, R&D…" />
              </div>
              <div className="form-field">
                <label>Contexte du poste</label>
                <textarea value={form.contextePoste} onChange={set('contextePoste')} placeholder="Présentez le contexte, l'environnement de travail…" rows={3} />
              </div>
              <div className="form-field">
                <label>Missions détaillées</label>
                <textarea value={form.missionsDetaillees} onChange={set('missionsDetaillees')} placeholder="Listez les missions principales du poste…" rows={4} />
              </div>

              {formError && <p className="auth-error">{formError}</p>}
              <div className="drawer-footer">
                <button type="button" className="btn-outline" onClick={closeDrawer}>Annuler</button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? 'Enregistrement…' : editing ? 'Mettre à jour' : "Créer l'offre"}
                </button>
              </div>
            </form>
          </aside>
        </>
      )}

      {/* ── Drawer questions chatbot ── */}
      {qJob && (
        <>
          <div className="drawer-overlay" onClick={closeQuestions} />
          <aside className="drawer">
            <div className="drawer-header">
              <div>
                <h3>Questionnaire candidat</h3>
                <p style={{ margin: 0, fontSize: 12, color: '#6b7280' }}>{qJob.title}</p>
              </div>
              <button className="icon-btn" onClick={closeQuestions}>✕</button>
            </div>

            {/* Lien de candidature */}
            {qJob.slug && (
              <div className="q-apply-link">
                <div className="q-apply-link-label">
                  <Link2 size={13} />
                  Lien à envoyer aux candidats
                </div>
                <div className="q-apply-link-row">
                  <span className="q-apply-link-url">{applyUrl(qJob.slug)}</span>
                  <button
                    className={`q-apply-copy-btn${copied === qJob.jobId ? ' copied' : ''}`}
                    onClick={() => handleCopyLink(qJob)}
                    title="Copier le lien"
                  >
                    {copied === qJob.jobId ? <Check size={13} /> : <Copy size={13} />}
                    {copied === qJob.jobId ? 'Copié !' : 'Copier'}
                  </button>
                  <a
                    href={applyUrl(qJob.slug)}
                    target="_blank"
                    rel="noreferrer"
                    className="q-apply-preview-btn"
                    title="Prévisualiser le formulaire"
                  >
                    <ExternalLink size={13} /> Prévisualiser
                  </a>
                </div>
              </div>
            )}

            <div className="drawer-form" style={{ flex: 1, overflowY: 'auto' }}>
              {qLoading ? (
                <p style={{ color: '#9ca3af', fontSize: 13 }}>Chargement…</p>
              ) : questions.length === 0 ? (
                <p style={{ color: '#9ca3af', fontSize: 13 }}>Aucune question configurée.</p>
              ) : (
                <div className="q-list">
                  {questions.map((q, i) => (
                    <div key={q.id} className="q-row">
                      <GripVertical size={14} className="q-grip" />
                      <div className="q-body">
                        <span className="q-index">{i + 1}.</span>
                        <span className="q-text">{q.questionText}</span>
                        {q.required && <span className="q-required">Obligatoire</span>}
                      </div>
                      <button className="icon-btn danger" onClick={() => handleDeleteQuestion(q)} title="Supprimer">
                        <X size={13} />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              {/* Questions suggérées */}
              <div className="q-suggestions">
                <p className="q-add-label">Questions suggérées</p>
                <div className="q-chips">
                  {SUGGESTED_QUESTIONS.map((s, i) => {
                    const added = questions.some(q => q.questionText === s.questionText);
                    return (
                      <button
                        key={i}
                        type="button"
                        className={`q-chip${added ? ' q-chip-added' : ''}`}
                        onClick={() => !added && handleAddSuggested(s)}
                        disabled={added || qSaving}
                        title={s.questionText}
                      >
                        {added ? '✓ ' : '+ '}{s.questionText.length > 48 ? s.questionText.slice(0, 48) + '…' : s.questionText}
                      </button>
                    );
                  })}
                </div>
              </div>

              <form className="q-add-form" onSubmit={handleAddQuestion}>
                <p className="q-add-label">Ajouter une question</p>
                <div className="form-field">
                  <input
                    value={newQ.questionText}
                    onChange={e => setNewQ(p => ({ ...p, questionText: e.target.value }))}
                    placeholder="Ex: Pourquoi cette alternance vous intéresse-t-elle ?"
                    required
                  />
                </div>
                <div className="form-row-2">
                  <div className="form-field">
                    <label>Type</label>
                    <select value={newQ.answerType} onChange={e => setNewQ(p => ({ ...p, answerType: e.target.value }))}>
                      <option value="open">Réponse libre</option>
                      <option value="yesno">Oui / Non</option>
                      <option value="scale">Échelle 1-5</option>
                    </select>
                  </div>
                  <div className="form-field" style={{ justifyContent: 'flex-end' }}>
                    <label className="form-checkbox" style={{ marginTop: 22 }}>
                      <input type="checkbox" checked={newQ.required} onChange={e => setNewQ(p => ({ ...p, required: e.target.checked }))} />
                      Obligatoire
                    </label>
                  </div>
                </div>
                <div className="drawer-footer" style={{ padding: 0, marginTop: 8 }}>
                  <button type="submit" className="btn-primary" disabled={qSaving || !newQ.questionText.trim()}>
                    {qSaving ? 'Ajout…' : '+ Ajouter'}
                  </button>
                </div>
              </form>
            </div>
          </aside>
        </>
      )}
    </div>
  );
}
