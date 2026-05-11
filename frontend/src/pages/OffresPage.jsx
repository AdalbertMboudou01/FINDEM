import React, { useEffect, useRef, useState } from 'react';
import { Plus, MapPin, Calendar, Users, Pencil, Trash2, ChevronRight, Briefcase, Link2, MessageSquare, GripVertical, X, Archive, XCircle, Tag, ExternalLink, Copy, Check, MoreVertical, RotateCcw, Sparkles } from 'lucide-react';
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
  title: '', location: '', alternanceRhythm: '',
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

function JobCard({ job, copied, onEdit, onDelete, onQuickStatus, onQuestions, onCandidatures }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    function close(e) { if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false); }
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  function action(fn) { setMenuOpen(false); fn(); }

  return (
    <div className="job-card">
      {/* ── Header ── */}
      <div className="job-card-top">
        <StatutBadge statut={job.statut} />
        <div className="job-card-menu-wrap" ref={menuRef}>
          <button className="icon-btn" onClick={() => setMenuOpen(v => !v)} title="Actions">
            <MoreVertical size={15} />
          </button>
          {menuOpen && (
            <div className="job-card-menu">
              <button onClick={() => action(onEdit)}><Pencil size={13} /> Modifier</button>
              <button onClick={() => action(onQuestions)}><MessageSquare size={13} /> Questionnaire</button>
              {job.statut !== 'clôturé' && job.statut !== 'archivé' && (
                <button onClick={() => action(() => onQuickStatus(job, 'clôturé'))}><XCircle size={13} /> Fermer l'offre</button>
              )}
              {job.statut !== 'archivé' && (
                <button onClick={() => action(() => onQuickStatus(job, 'archivé'))}><Archive size={13} /> Archiver</button>
              )}
              {(job.statut === 'clôturé' || job.statut === 'archivé') && (
                <button onClick={() => action(() => onQuickStatus(job, 'ouvert'))}><RotateCcw size={13} /> Rouvrir</button>
              )}
              <button className="danger" onClick={() => action(onDelete)}><Trash2 size={13} /> Supprimer</button>
            </div>
          )}
        </div>
      </div>

      {/* ── Body ── */}
      <h3 className="job-card-title">{job.title}</h3>

      <div className="job-card-meta">
        {job.location         && <span><MapPin size={13} strokeWidth={2} /> {job.location}</span>}
        {job.alternanceRhythm && <span><Calendar size={13} strokeWidth={2} /> {job.alternanceRhythm}</span>}
        {job.maxCandidatures  && <span><Users size={13} strokeWidth={2} /> max {job.maxCandidatures}</span>}
        <span className="job-card-date">{timeAgo(job.createdAt)}</span>
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

      {/* ── Footer ── */}
      <div className="job-card-footer">
        <button className="job-card-cta" onClick={onCandidatures}>
          <Users size={14} /> Voir les candidatures <ChevronRight size={14} />
        </button>
      </div>
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
  const [aiGenerating, setAiGenerating] = useState(false);
  const [aiGenerated,  setAiGenerated]  = useState(false);

  /* Questions drawer */
  const [qJob,          setQJob]          = useState(null);
  const [questions,     setQuestions]     = useState([]);
  const [qLoading,      setQLoading]      = useState(false);
  const [newQ,          setNewQ]          = useState(EMPTY_QUESTION);
  const [qSaving,       setQSaving]       = useState(false);
  const [qTab,          setQTab]          = useState('questions');
  const [showAddForm,   setShowAddForm]   = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);

  useEffect(() => { load(); }, []);

  async function load() {
    setLoading(true);
    const data = await api.get('/api/jobs', token).catch(() => []);
    setJobs(Array.isArray(data) ? data : []);
    setLoading(false);
  }

  const filtered = filter === 'all' ? jobs : jobs.filter(j => j.statut === filter);

  /* ── Offre drawer ── */
  function openCreate() { setEditing(null); setForm(EMPTY); setFormError(''); setAiGenerated(false); setShowDrawer(true); }

  function openEdit(job) {
    setEditing(job);
    setForm({
      title: job.title ?? '',
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

  function closeDrawer() { setShowDrawer(false); setFormError(''); setAiGenerated(false); }

  async function handleAiAssist() {
    if (!form.title.trim() || aiGenerating) return;
    setAiGenerating(true);
    setFormError('');
    try {
      const result = await api.post('/api/jobs/ai-assist', {
        title: form.title,
        location: form.location,
        alternanceRhythm: form.alternanceRhythm,
        context: form.serviceEntreprise,
      }, token);
      setForm(f => ({
        ...f,
        contextePoste:      result.contextePoste      || f.contextePoste,
        missionsDetaillees: result.missionsDetaillees  || f.missionsDetaillees,
        technologies:       result.technologies?.length ? result.technologies : f.technologies,
      }));
      setAiGenerated(true);
    } catch (err) {
      setFormError(err.message || "Le service IA est temporairement indisponible.");
    } finally {
      setAiGenerating(false);
    }
  }

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  async function handleSave(e) {
    e.preventDefault();
    if (!form.title.trim() || !form.contextePoste.trim() || !form.missionsDetaillees.trim() || form.technologies.length === 0) {
      setFormError("Titre, contexte de l'offre, missions détaillées et technologies sont obligatoires."); return;
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
          title: form.title,
          location: form.location || null,
          alternanceRhythm: form.alternanceRhythm || null,
          companyId: session.companyId, ownerRecruiterId: session.recruiterId,
          technologies: form.technologies,
          contextePoste: form.contextePoste,
          missionsDetaillees: form.missionsDetaillees,
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
    setQTab('questions');
    setShowAddForm(false);
    setShowSuggestions(false);
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
      setShowAddForm(false);
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
            <JobCard
              key={job.jobId}
              job={job}
              copied={copied}
              onEdit={() => openEdit(job)}
              onDelete={() => handleDelete(job)}
              onQuickStatus={handleQuickStatus}
              onQuestions={() => openQuestions(job)}
              onCandidatures={() => navigate(`/candidatures?jobId=${job.jobId}`)}
            />
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

              {/* ── Bannière IA (création uniquement) ── */}
              {!editing && (
                <div className="ai-assist-banner">
                  <div className="ai-assist-banner-left">
                    <Sparkles size={15} />
                    <span>Remplissez le titre, puis laissez l'IA compléter l'offre</span>
                  </div>
                  <button
                    type="button"
                    className="ai-assist-btn"
                    onClick={handleAiAssist}
                    disabled={!form.title.trim() || aiGenerating}
                  >
                    {aiGenerating
                      ? <><span className="ai-spinner" /> Génération…</>
                      : <><Sparkles size={13} /> Générer</>}
                  </button>
                </div>
              )}

              {aiGenerated && (
                <div className="ai-generated-notice">
                  ✦ Contenu généré par l'IA — vérifiez et ajustez avant de publier
                </div>
              )}

              <div className="form-field">
                <label>Titre de l'offre *</label>
                <input value={form.title} onChange={set('title')} placeholder="Ex: Développeur React Native" required />
              </div>
              <div className="form-field">
                <label>Contexte de l'offre *</label>
                <textarea value={form.contextePoste} onChange={set('contextePoste')} placeholder="Présentez le contexte, l'environnement de travail, la structure…" rows={3} required />
              </div>
              <div className="form-field">
                <label>Missions détaillées *</label>
                <textarea value={form.missionsDetaillees} onChange={set('missionsDetaillees')} placeholder="Listez les missions principales du poste…" rows={4} required />
              </div>
              <div className="form-field">
                <label>Technologies requises *</label>
                <TechTagInput
                  value={form.technologies}
                  onChange={tags => setForm(f => ({ ...f, technologies: tags }))}
                />
              </div>
              <div className="form-row-2">
                <div className="form-field">
                  <label>Localisation</label>
                  <input value={form.location} onChange={set('location')} placeholder="Paris, Lyon…" />
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
                <label>Service / Équipe</label>
                <input value={form.serviceEntreprise} onChange={set('serviceEntreprise')} placeholder="Ex: Équipe produit, R&D…" />
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

            {/* Onglets */}
            <div className="q-tabs">
              <button className={`q-tab${qTab === 'questions' ? ' active' : ''}`} onClick={() => setQTab('questions')}>
                <MessageSquare size={13} /> Questions {questions.length > 0 && <span className="q-tab-count">{questions.length}</span>}
              </button>
              <button className={`q-tab${qTab === 'lien' ? ' active' : ''}`} onClick={() => setQTab('lien')}>
                <Link2 size={13} /> Lien candidat
              </button>
            </div>

            {/* ── Onglet Questions ── */}
            {qTab === 'questions' && (
              <div className="drawer-form" style={{ flex: 1, overflowY: 'auto' }}>
                {qLoading ? (
                  <p style={{ color: '#9ca3af', fontSize: 13 }}>Chargement…</p>
                ) : questions.length === 0 ? (
                  <p className="q-empty">Aucune question configurée. Ajoutez-en une ci-dessous.</p>
                ) : (
                  <div className="q-list">
                    {questions.map((q, i) => (
                      <div key={q.id} className="q-row">
                        <GripVertical size={14} className="q-grip" />
                        <div className="q-body">
                          <span className="q-index">{i + 1}.</span>
                          <span className="q-text">{q.questionText}</span>
                          {q.required && <span className="q-required">Req.</span>}
                        </div>
                        <button className="icon-btn danger" onClick={() => handleDeleteQuestion(q)} title="Supprimer">
                          <X size={13} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {/* Barre d'actions */}
                <div className="q-actions-bar">
                  <button
                    type="button"
                    className="btn-outline"
                    onClick={() => { setShowSuggestions(v => !v); setShowAddForm(false); }}
                  >
                    <Sparkles size={13} /> Suggestions
                  </button>
                  <button
                    type="button"
                    className="btn-primary"
                    onClick={() => { setShowAddForm(v => !v); setShowSuggestions(false); }}
                  >
                    <Plus size={13} /> Ajouter
                  </button>
                </div>

                {/* Suggestions (collapsible) */}
                {showSuggestions && (
                  <div className="q-suggestions-panel">
                    {SUGGESTED_QUESTIONS.map((s, i) => {
                      const added = questions.some(q => q.questionText === s.questionText);
                      return (
                        <button
                          key={i}
                          type="button"
                          className={`q-suggestion-row${added ? ' added' : ''}`}
                          onClick={() => !added && handleAddSuggested(s)}
                          disabled={added || qSaving}
                        >
                          <span className="q-suggestion-icon">{added ? <Check size={12} /> : <Plus size={12} />}</span>
                          <span className="q-suggestion-text">{s.questionText}</span>
                        </button>
                      );
                    })}
                  </div>
                )}

                {/* Formulaire ajout (collapsible) */}
                {showAddForm && (
                  <form className="q-add-form" onSubmit={handleAddQuestion}>
                    <div className="form-field">
                      <input
                        autoFocus
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
                          <option value="scale">Échelle 1–5</option>
                        </select>
                      </div>
                      <div className="form-field" style={{ justifyContent: 'flex-end' }}>
                        <label className="form-checkbox" style={{ marginTop: 22 }}>
                          <input type="checkbox" checked={newQ.required} onChange={e => setNewQ(p => ({ ...p, required: e.target.checked }))} />
                          Obligatoire
                        </label>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
                      <button type="button" className="btn-outline" onClick={() => setShowAddForm(false)}>Annuler</button>
                      <button type="submit" className="btn-primary" disabled={qSaving || !newQ.questionText.trim()}>
                        {qSaving ? 'Ajout…' : 'Ajouter'}
                      </button>
                    </div>
                  </form>
                )}
              </div>
            )}

            {/* ── Onglet Lien ── */}
            {qTab === 'lien' && (
              <div className="drawer-form">
                {qJob.slug ? (
                  <>
                    <p className="q-lien-hint">Partagez ce lien avec vos candidats pour qu'ils postulent directement à cette offre.</p>
                    <div className="q-lien-box">
                      <span className="q-apply-link-url">{applyUrl(qJob.slug)}</span>
                    </div>
                    <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                      <button
                        className={`btn-primary${copied === qJob.jobId ? ' copied' : ''}`}
                        style={{ flex: 1 }}
                        onClick={() => handleCopyLink(qJob)}
                      >
                        {copied === qJob.jobId ? <Check size={14} /> : <Copy size={14} />}
                        {copied === qJob.jobId ? 'Lien copié !' : 'Copier le lien'}
                      </button>
                      <a
                        href={applyUrl(qJob.slug)}
                        target="_blank"
                        rel="noreferrer"
                        className="btn-outline"
                        style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
                      >
                        <ExternalLink size={14} /> Prévisualiser
                      </a>
                    </div>
                  </>
                ) : (
                  <p className="q-empty">Aucun lien disponible pour cette offre.</p>
                )}
              </div>
            )}
          </aside>
        </>
      )}
    </div>
  );
}
