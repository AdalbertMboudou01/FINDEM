import React, { useEffect, useState, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Search, Github, Globe, FileText, Send, Mail, Phone,
  MapPin, GraduationCap, X, Sparkles, AlertTriangle,
  CheckCircle, TrendingUp, RefreshCw, Filter, ChevronRight,
  ThumbsUp, ThumbsDown, Minus, Star, Award, ArrowRight,
  Clock, MessageCircle, UserCheck, Calendar, Zap, Download, MailCheck,
} from 'lucide-react';
import { api } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import { decodeToken } from '../lib/auth';
import { createStompClient } from '../lib/websocket';

/* ── Helpers ─────────────────────────────────────────────── */
function timeAgo(date) {
  if (!date) return '';
  const diff = Date.now() - new Date(date).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "à l'instant";
  if (mins < 60) return `il y a ${mins}m`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `il y a ${hrs}h`;
  return `il y a ${Math.floor(hrs / 24)}j`;
}

function initials(app) {
  const f = app?.candidate?.firstName?.[0] ?? '';
  const l = app?.candidate?.lastName?.[0] ?? '';
  return (f + l).toUpperCase() || '?';
}

function renderMarkdown(text) {
  if (!text) return null;
  return text.split('\n').map((line, i) => {
    const html = line
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2" target="_blank" rel="noreferrer">$1</a>');
    return <span key={i}><span dangerouslySetInnerHTML={{ __html: html || '&nbsp;' }} />{i < text.split('\n').length - 1 && <br />}</span>;
  });
}

/* ── Status config ───────────────────────────────────────── */
const PIPELINE_STAGES = [
  { code: 'nouveau',          label: 'Nouveau'     },
  { code: 'en_etude',         label: 'En étude'    },
  { code: 'en_attente_avis',  label: 'Avis équipe' },
  { code: 'entretien',        label: 'Entretien'   },
  { code: 'retenu',           label: 'Retenu'      },
  { code: 'embauche',         label: 'Embauché'    },
];

const STATUS_COLOR = {
  nouveau:           { bg: '#eff6ff', color: '#2563eb' },
  en_etude:          { bg: '#f0fdf4', color: '#16a34a' },
  en_attente_avis:   { bg: '#fefce8', color: '#ca8a04' },
  entretien:         { bg: '#faf5ff', color: '#7c3aed' },
  retenu:            { bg: '#d1fae5', color: '#059669' },
  embauche:          { bg: '#ecfdf5', color: '#047857' },
  non_retenu:        { bg: '#fee2e2', color: '#dc2626' },
  vivier:            { bg: '#f3e8ff', color: '#9333ea' },
  en_attente:        { bg: '#fef3c7', color: '#d97706' },
  a_revoir_manuellement: { bg: '#ede9fe', color: '#7c3aed' },
  retenu_entretien:  { bg: '#d1fae5', color: '#059669' },
};

function StatusBadge({ code, label }) {
  const s = STATUS_COLOR[code] ?? { bg: '#f1f5f9', color: '#6b7280' };
  return (
    <span style={{ background: s.bg, color: s.color, padding: '2px 8px', borderRadius: 6, fontSize: 11, fontWeight: 700, whiteSpace: 'nowrap' }}>
      {label ?? code ?? '—'}
    </span>
  );
}

const FILTER_BADGE = {
  PRIORITY: { label: 'Prioritaire', bg: '#d1fae5', color: '#059669' },
  REVIEW:   { label: 'À examiner',  bg: '#fef3c7', color: '#d97706' },
  REJECT:   { label: 'Refusé auto', bg: '#fee2e2', color: '#dc2626' },
};

function ViewChip({ label, active, onClick }) {
  return (
    <button
      className={`cand-view-chip${active ? ' active' : ''}`}
      onClick={onClick}
    >
      {label}
    </button>
  );
}

const TABS = ['Profil', 'Analyse', 'Décision', 'Entretiens', 'Commentaires', 'Historique'];

/* ── Main page ───────────────────────────────────────────── */
export default function CandidaturesPage() {
  const { session } = useAuth();
  const token = session?.token;
  const [searchParams] = useSearchParams();

  const [apps,              setApps]              = useState([]);
  const [jobs,              setJobs]              = useState([]);
  const [loading,           setLoading]           = useState(true);
  const [jobFilter,         setJobFilter]         = useState(searchParams.get('jobId') ?? '');
  const [search,            setSearch]            = useState('');
  const [selectedId,        setSelectedId]        = useState(null);
  const [dossier,           setDossier]           = useState(null);
  const [comments,          setComments]          = useState([]);
  const [analysis,          setAnalysis]          = useState(null);
  const [analysisLoading,   setAnalysisLoading]   = useState(false);
  const [activeTab,         setActiveTab]         = useState('Profil');
  const [newComment,        setNewComment]        = useState('');
  const [sending,           setSending]           = useState(false);
  const [allowedTransitions,setAllowedTransitions]= useState([]);
  const [patchingStatus,    setPatchingStatus]    = useState(false);
  const [filterResults,     setFilterResults]     = useState(null);
  const [filtering,         setFiltering]         = useState(false);
  const [cvFile,            setCvFile]            = useState(null);
  const [completenessMap,   setCompletenessMap]   = useState({});
  const [activeView,        setActiveView]        = useState('all');
  const commentsEndRef = useRef(null);

  /* WebSocket — mises à jour de statut en temps réel */
  useEffect(() => {
    if (!session?.token) return;
    const claims    = decodeToken(session.token);
    const companyId = claims?.companyId;
    if (!companyId) return;

    const client = createStompClient({
      token: session.token,
      companyId,
      onApplicationUpdate: ({ applicationId, statusCode, statusLabel }) => {
        setApps(prev => prev.map(a =>
          a.applicationId === applicationId
            ? { ...a, status: { ...a.status, code: statusCode, label: statusLabel } }
            : a
        ));
        setDossier(prev =>
          prev?.applicationId === applicationId
            ? { ...prev, status: { ...prev.status, code: statusCode, label: statusLabel } }
            : prev
        );
      },
    });
    client.activate();
    return () => client.deactivate();
  }, [session?.token]);

  /* Load list + jobs */
  useEffect(() => {
    async function init() {
      const [appsRes, jobsRes] = await Promise.all([
        api.get('/api/applications', token).catch(() => []),
        api.get('/api/jobs',         token).catch(() => []),
      ]);
      const appsArr = Array.isArray(appsRes) ? appsRes : [];
      setApps(appsArr);
      setJobs(Array.isArray(jobsRes) ? jobsRes : []);
      setLoading(false);
      if (appsArr.length > 0) {
        const ids = appsArr.map(a => a.applicationId);
        api.post('/api/chat-answers/summary/batch', ids, token)
          .then(batchData => {
            if (batchData && typeof batchData === 'object') {
              const map = {};
              Object.entries(batchData).forEach(([id, s]) => {
                if (s?.completenessScore != null) map[id] = s.completenessScore;
              });
              setCompletenessMap(map);
            }
          })
          .catch(() => {});
      }
    }
    init();
  }, [token]);

  /* Load dossier when selection changes */
  useEffect(() => {
    if (!selectedId) { setDossier(null); setComments([]); setAnalysis(null); setAllowedTransitions([]); setCvFile(null); return; }
    setDossier(null); setComments([]); setAnalysis(null); setAllowedTransitions([]); setCvFile(null);
    Promise.all([
      api.get(`/api/applications/${selectedId}`,                      token).catch(() => null),
      api.get(`/api/applications/${selectedId}/comments`,             token).catch(() => []),
      api.get(`/api/applications/${selectedId}/allowed-transitions`,  token).catch(() => []),
    ]).then(([app, cmts, transitions]) => {
      const transArr = Array.isArray(transitions) ? transitions : [];
      setComments(Array.isArray(cmts) ? cmts : []);

      /* Auto nouveau → en_etude quand le recruteur ouvre le dossier */
      if (app?.status?.code === 'nouveau' && transArr.includes('en_etude')) {
        api.patch(`/api/applications/${selectedId}/status`, { statusCode: 'en_etude' }, token)
          .then(updated => {
            setDossier(updated);
            setApps(prev => prev.map(a => a.applicationId === selectedId ? { ...a, status: updated.status } : a));
            api.get(`/api/applications/${selectedId}/allowed-transitions`, token)
              .then(t => setAllowedTransitions(Array.isArray(t) ? t : [])).catch(() => {});
          })
          .catch(() => { setDossier(app); setAllowedTransitions(transArr); });
      } else {
        setDossier(app);
        setAllowedTransitions(transArr);
      }

      if (app?.candidate?.candidateId) {
        api.get(`/api/files/candidate/${app.candidate.candidateId}/cv`, token).catch(() => null).then(cv => setCvFile(cv));
      }
    });
  }, [selectedId, token]);

  /* Load analysis when switching to that tab */
  useEffect(() => {
    if (activeTab !== 'Analyse' || !selectedId || analysis) return;
    setAnalysisLoading(true);
    api.get(`/api/chat-answers/summary/${selectedId}`, token)
      .then(data => { setAnalysis(data); setAnalysisLoading(false); })
      .catch(() => { setAnalysis(null); setAnalysisLoading(false); });
  }, [activeTab, selectedId, token, analysis]);

  useEffect(() => {
    commentsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [comments]);

  /* Reset filter when job changes */
  useEffect(() => { setFilterResults(null); }, [jobFilter]);

  const filtered = apps
    .filter(a => activeView === 'vivier' ? a.status?.code === 'vivier' : true)
    .filter(a => !jobFilter || a.job?.jobId === jobFilter)
    .filter(a => {
      if (!search) return true;
      const name = `${a.candidate?.firstName ?? ''} ${a.candidate?.lastName ?? ''}`.toLowerCase();
      return name.includes(search.toLowerCase());
    })
    .sort((a, b) => {
      if (!filterResults) return 0;
      const order = { PRIORITY: 0, REVIEW: 1, REJECT: 2 };
      return (order[filterResults[a.applicationId]] ?? 1) - (order[filterResults[b.applicationId]] ?? 1);
    });

  async function handleSendComment(e) {
    e.preventDefault();
    if (!newComment.trim() || !selectedId) return;
    setSending(true);
    try {
      const created = await api.post(
        `/api/applications/${selectedId}/comments`,
        { body: newComment.trim(), visibility: 'INTERNAL' },
        token
      );
      setComments(p => [...p, created]);
      setNewComment('');
    } catch { /* silent */ } finally {
      setSending(false);
    }
  }

  async function handlePatchStatus(statusCode) {
    if (!selectedId || patchingStatus) return;
    setPatchingStatus(true);
    try {
      await api.patch(`/api/applications/${selectedId}/status`, { statusCode }, token);
      const [updatedApp, newTransitions] = await Promise.all([
        api.get(`/api/applications/${selectedId}`, token),
        api.get(`/api/applications/${selectedId}/allowed-transitions`, token),
      ]);
      setDossier(updatedApp);
      setAllowedTransitions(Array.isArray(newTransitions) ? newTransitions : []);
      setApps(prev => prev.map(a => a.applicationId === selectedId ? { ...a, status: updatedApp.status } : a));
    } catch { /* silent */ } finally {
      setPatchingStatus(false);
    }
  }

  async function handleFilter() {
    if (!jobFilter || filtering) return;
    setFiltering(true);
    try {
      const results = await api.post(`/api/filtering/applications/${jobFilter}`, {}, token);
      if (Array.isArray(results)) {
        const map = {};
        results.forEach(r => { if (r.applicationId) map[r.applicationId] = r.recommendedAction; });
        setFilterResults(map);
      }
    } catch { /* silent */ } finally {
      setFiltering(false);
    }
  }

  const cand = dossier?.candidate;
  const candName = cand ? `${cand.firstName ?? ''} ${cand.lastName ?? ''}`.trim() : '—';

  return (
    <div className="cand-shell">
      {/* ════ LIST PANEL ══════════════════════════════════════ */}
      <div className="cand-list-panel">
        <div className="cand-search-bar">
          <Search size={15} className="cand-search-icon" />
          <input
            className="cand-search-input"
            placeholder="Rechercher un candidat…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
          {search && <button className="cand-search-clear" onClick={() => setSearch('')}><X size={13} /></button>}
        </div>

        <div className="cand-job-filter">
          <select value={jobFilter} onChange={e => setJobFilter(e.target.value)}>
            <option value="">Toutes les offres</option>
            {jobs.map(j => <option key={j.jobId} value={j.jobId}>{j.title}</option>)}
          </select>
        </div>

        <div className="cand-views-bar">
          <ViewChip label="Toutes"      active={activeView === 'all'}    onClick={() => { setActiveView('all');    setJobFilter(''); }} />
          <ViewChip label="Vivier"      active={activeView === 'vivier'} onClick={() => { setActiveView('vivier'); setJobFilter(''); }} />
          <ViewChip label="Entretiens"  active={activeView === 'interview'} onClick={() => {
            setActiveView('interview');
            api.get('/api/applications?view=interview', token).catch(() => []).then(d => { if (Array.isArray(d)) setApps(d); });
          }} />
          <ViewChip label="Urgentes"    active={activeView === 'urgent'}  onClick={() => {
            setActiveView('urgent');
            api.get('/api/applications?view=urgent', token).catch(() => []).then(d => { if (Array.isArray(d)) setApps(d); });
          }} />
          <ViewChip label="Récentes"    active={activeView === 'recent'}  onClick={() => {
            setActiveView('recent');
            api.get('/api/applications?view=recent', token).catch(() => []).then(d => { if (Array.isArray(d)) setApps(d); });
          }} />
        </div>

        {jobFilter && (
          <div className="cand-filter-bar">
            <button
              className={`cand-filter-btn${filterResults ? ' active' : ''}`}
              onClick={filterResults ? () => setFilterResults(null) : handleFilter}
              disabled={filtering}
            >
              <Filter size={12} />
              {filtering ? 'Filtrage…' : filterResults ? 'Annuler le filtre' : 'Filtrer automatiquement'}
            </button>
            {filterResults && (
              <span className="cand-filter-hint">Trié par pertinence</span>
            )}
          </div>
        )}

        <div className="cand-list-count">{filtered.length} candidature{filtered.length !== 1 ? 's' : ''}</div>

        <div className="cand-list">
          {loading ? (
            <p className="cand-list-empty">Chargement…</p>
          ) : filtered.length === 0 ? (
            <p className="cand-list-empty">Aucune candidature.</p>
          ) : filtered.map(app => {
            const filterAction = filterResults?.[app.applicationId];
            const fBadge = filterAction ? FILTER_BADGE[filterAction] : null;
            return (
              <div
                key={app.applicationId}
                className={`cand-item${selectedId === app.applicationId ? ' active' : ''}`}
                onClick={() => { setSelectedId(app.applicationId); setActiveTab('Profil'); setAnalysis(null); }}
              >
                <div className="cand-item-avatar">{initials(app)}</div>
                <div className="cand-item-body">
                  <span className="cand-item-name">
                    {app.candidate?.firstName} {app.candidate?.lastName}
                  </span>
                  <span className="cand-item-job">{app.job?.title ?? '—'}</span>
                  {fBadge && (
                    <span style={{ background: fBadge.bg, color: fBadge.color, fontSize: 10, fontWeight: 700, padding: '1px 6px', borderRadius: 4, marginTop: 2, display: 'inline-block' }}>
                      {fBadge.label}
                    </span>
                  )}
                </div>
                <div className="cand-item-side">
                  <StatusBadge code={app.status?.code} label={app.status?.label} />
                  <span className="cand-item-date">{timeAgo(app.createdAt)}</span>
                  {completenessMap[app.applicationId] != null && (
                    <div className="cand-item-progress" title={`Questionnaire: ${Math.round(completenessMap[app.applicationId] * 100)}%`}>
                      <div className="cand-item-progress-bar" style={{ width: `${Math.round(completenessMap[app.applicationId] * 100)}%` }} />
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* ════ DOSSIER PANEL ═══════════════════════════════════ */}
      <div className="cand-dossier">
        {!selectedId ? (
          <div className="cand-dossier-empty">
            <Search size={36} strokeWidth={1.2} />
            <p>Sélectionnez une candidature pour voir le dossier</p>
          </div>
        ) : !dossier ? (
          <div className="cand-dossier-empty"><p>Chargement…</p></div>
        ) : (
          <>
            <div className="cand-dossier-header">
              <div className="cand-dossier-identity">
                <div className="cand-dossier-avatar">{initials(dossier)}</div>
                <div>
                  <h2 className="cand-dossier-name">{candName}</h2>
                  <span className="cand-dossier-job">{dossier.job?.title ?? '—'}</span>
                </div>
              </div>
              <StatusBadge code={dossier.status?.code} label={dossier.status?.label} />
            </div>

            {/* ── Status pipeline ───────────────────────────── */}
            <StatusPipeline
              currentCode={dossier.status?.code}
              allowedTransitions={allowedTransitions}
              onTransition={handlePatchStatus}
              patching={patchingStatus}
            />

            <div className="cand-tabs">
              {TABS.map(tab => (
                <button
                  key={tab}
                  className={`cand-tab${activeTab === tab ? ' active' : ''}`}
                  onClick={() => setActiveTab(tab)}
                >
                  {tab === 'Décision'    && <Award    size={13} style={{ marginRight: 4 }} />}
                  {tab === 'Entretiens'  && <Calendar size={13} style={{ marginRight: 4 }} />}
                  {tab === 'Historique'  && <Clock    size={13} style={{ marginRight: 4 }} />}
                  {tab}
                </button>
              ))}
            </div>

            <div className="cand-tab-content">
              {activeTab === 'Profil'       && <ProfilTab candidate={cand} cvFile={cvFile} />}
              {activeTab === 'Analyse'   && (
                <AnalyseTab
                  analysis={analysis}
                  loading={analysisLoading}
                  applicationId={selectedId}
                  token={token}
                  onLoaded={setAnalysis}
                  onRefreshComments={() =>
                    api.get(`/api/applications/${selectedId}/comments`, token)
                      .then(cmts => setComments(Array.isArray(cmts) ? cmts : []))
                      .catch(() => {})
                  }
                />
              )}
              {activeTab === 'Décision'     && (
                <DecisionTab
                  applicationId={selectedId}
                  token={token}
                  currentStatusCode={dossier.status?.code}
                  onStatusChange={handlePatchStatus}
                />
              )}
              {activeTab === 'Commentaires' && (
                <CommentsTab
                  comments={comments}
                  newComment={newComment}
                  onNewComment={setNewComment}
                  onSend={handleSendComment}
                  sending={sending}
                  endRef={commentsEndRef}
                />
              )}
              {activeTab === 'Entretiens' && (
                <EntretiensTab
                  applicationId={selectedId}
                  token={token}
                  currentStatusCode={dossier.status?.code}
                  onStatusChange={handlePatchStatus}
                />
              )}
              {activeTab === 'Historique' && (
                <HistoriqueTab applicationId={selectedId} token={token} />
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

/* ── Status Pipeline ─────────────────────────────────────── */
function StatusPipeline({ currentCode, allowedTransitions, onTransition, patching }) {
  const [pending, setPending] = useState(null); // action en attente de confirmation

  const currentIdx = PIPELINE_STAGES.findIndex(s => s.code === currentCode);
  const canHire    = currentCode === 'retenu' && allowedTransitions.includes('embauche');
  const canReject  = allowedTransitions.includes('non_retenu') && currentCode !== 'non_retenu';
  const canVivier  = allowedTransitions.includes('vivier')     && currentCode !== 'vivier';
  /* Boutons de récupération : retour possible depuis non_retenu ou vivier */
  const canRecover  = allowedTransitions.includes('en_etude')  && ['non_retenu', 'vivier'].includes(currentCode);
  const hasActions  = canHire || canReject || canVivier || canRecover;

  function requestRiskyAction(code) { setPending(code); }
  function confirmAction()           { if (pending) { onTransition(pending); setPending(null); } }
  function cancelAction()            { setPending(null); }

  return (
    <div className="pipeline-wrapper">
      {/* Barre de progression — purement visuelle */}
      <div className="pipeline-track">
        {PIPELINE_STAGES.map((stage, idx) => {
          const isPast    = idx < currentIdx;
          const isCurrent = stage.code === currentCode;
          return (
            <React.Fragment key={stage.code}>
              <div className={`pipeline-step${isCurrent ? ' current' : ''}${isPast ? ' past' : ''}`}>
                <div className="pipeline-dot">
                  {isPast || isCurrent ? <CheckCircle size={10} /> : <span>{idx + 1}</span>}
                </div>
                <span className="pipeline-label">{stage.label}</span>
              </div>
              {idx < PIPELINE_STAGES.length - 1 && (
                <div className={`pipeline-connector${idx < currentIdx ? ' filled' : ''}`} />
              )}
            </React.Fragment>
          );
        })}
      </div>

      {hasActions && (
        <div className="pipeline-actions">
          {/* Confirmation inline */}
          {pending && (
            <div className="pipeline-confirm">
              <span className="pipeline-confirm-label">
                {pending === 'non_retenu' ? 'Marquer comme non retenu ?' : 'Ajouter au vivier ?'}
              </span>
              <button className="pipeline-confirm-yes" onClick={confirmAction} disabled={patching}>
                <CheckCircle size={12} /> Confirmer
              </button>
              <button className="pipeline-confirm-no" onClick={cancelAction}>
                <X size={12} /> Annuler
              </button>
            </div>
          )}

          {!pending && (
            <>
              {canHire && (
                <button className="pipeline-action-btn advance" onClick={() => !patching && onTransition('embauche')} disabled={patching}>
                  <CheckCircle size={12} /> Confirmer l'embauche
                </button>
              )}
              {canRecover && (
                <button className="pipeline-action-btn recover" onClick={() => !patching && onTransition('en_etude')} disabled={patching}>
                  <ArrowRight size={12} /> Remettre en étude
                </button>
              )}
              {canReject && (
                <button className="pipeline-action-btn reject" onClick={() => requestRiskyAction('non_retenu')} disabled={patching}>
                  <X size={12} /> Non retenu
                </button>
              )}
              {canVivier && (
                <button className="pipeline-action-btn vivier" onClick={() => requestRiskyAction('vivier')} disabled={patching}>
                  <Star size={12} /> Ajouter au vivier
                </button>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}

/* ── Profil tab ──────────────────────────────────────────── */
function ProfilTab({ candidate: c, cvFile }) {
  if (!c) return <p className="tab-empty">Profil indisponible.</p>;

  const rows = [
    { icon: Mail,          value: c.email,    label: 'Email'       },
    { icon: Phone,         value: c.phone,    label: 'Téléphone'   },
    { icon: MapPin,        value: c.location, label: 'Localisation'},
    { icon: GraduationCap, value: c.school,   label: 'École'       },
  ].filter(r => r.value);

  return (
    <div className="profil-tab">
      <div className="profil-hero">
        <div className="profil-avatar">
          {(c.firstName?.[0] ?? '').toUpperCase()}{(c.lastName?.[0] ?? '').toUpperCase()}
        </div>
        <div>
          <h3 className="profil-name">{c.firstName} {c.lastName}</h3>
          {c.school && <span className="profil-school">{c.school}</span>}
        </div>
      </div>

      <div className="profil-info">
        {rows.map(({ icon: Icon, value, label }) => (
          <div key={label} className="profil-row">
            <Icon size={15} strokeWidth={1.8} className="profil-row-icon" />
            <div>
              <span className="profil-row-label">{label}</span>
              <span className="profil-row-value">{value}</span>
            </div>
          </div>
        ))}
      </div>

      {(c.githubUrl || c.portfolioUrl || c.cvPath || cvFile) && (
        <div className="profil-links">
          {c.githubUrl && (
            <a href={c.githubUrl} target="_blank" rel="noreferrer" className="profil-link">
              <Github size={15} /> GitHub
            </a>
          )}
          {c.portfolioUrl && (
            <a href={c.portfolioUrl} target="_blank" rel="noreferrer" className="profil-link">
              <Globe size={15} /> Portfolio
            </a>
          )}
          {cvFile ? (
            <a href={`/api/files/candidate/${c.candidateId}/cv/content`} target="_blank" rel="noreferrer" className="profil-link profil-link-cv">
              <Download size={15} /> {cvFile.originalFileName ?? 'CV.pdf'}
            </a>
          ) : c.cvPath ? (
            <a href={`/api/upload/${c.cvPath}`} target="_blank" rel="noreferrer" className="profil-link profil-link-cv">
              <FileText size={15} /> CV
            </a>
          ) : null}
        </div>
      )}
    </div>
  );
}

/* ── Analyse IA tab ──────────────────────────────────────── */
const LEVEL_CONFIG = {
  HIGH:   { label: 'Élevée',      color: '#059669', bg: '#d1fae5' },
  MEDIUM: { label: 'Moyenne',     color: '#d97706', bg: '#fef3c7' },
  LOW:    { label: 'Faible',      color: '#dc2626', bg: '#fee2e2' },
  STRONG: { label: 'Solide',      color: '#059669', bg: '#d1fae5' },
  WEAK:   { label: 'Insuffisant', color: '#dc2626', bg: '#fee2e2' },
};

const ACTION_CONFIG = {
  PRIORITY: { label: 'Candidature prioritaire', color: '#059669', bg: '#d1fae5', icon: CheckCircle },
  REVIEW:   { label: 'À examiner',              color: '#d97706', bg: '#fef3c7', icon: TrendingUp  },
  REJECT:   { label: 'Ne correspond pas',       color: '#dc2626', bg: '#fee2e2', icon: X           },
};

function LevelBadge({ value }) {
  const cfg = LEVEL_CONFIG[value] ?? { label: value ?? '—', color: '#6b7280', bg: '#f1f5f9' };
  return (
    <span className="ai-badge" style={{ color: cfg.color, background: cfg.bg }}>{cfg.label}</span>
  );
}

function AnalyseTab({ analysis, loading, applicationId, token, onLoaded, onRefreshComments }) {
  const [triggering,      setTriggering]      = useState(false);
  const [feedbackMap,     setFeedbackMap]     = useState({});
  const [feedbackLoading, setFeedbackLoading] = useState({});

  useEffect(() => {
    setFeedbackMap({});
    if (!applicationId) return;
    api.get(`/api/chat-answers/feedback/${applicationId}/latest`, token)
      .then(data => {
        if (Array.isArray(data)) {
          const map = {};
          data.forEach(f => {
            const key = `${f.dimension ?? 'general'}::${f.finding ?? ''}`;
            map[key] = f.decision;
          });
          setFeedbackMap(map);
        }
      })
      .catch(() => {});
  }, [applicationId, token]);

  async function submitFeedback(fact, decision) {
    const key = `${fact.dimension ?? 'general'}::${fact.finding ?? ''}`;
    setFeedbackLoading(prev => ({ ...prev, [key]: true }));
    try {
      await api.post(`/api/chat-answers/feedback/${applicationId}`, {
        dimension: fact.dimension,
        finding:   fact.finding,
        evidence:  fact.evidence,
        decision,
      }, token);
      setFeedbackMap(prev => ({ ...prev, [key]: decision }));
    } catch { /* silent */ } finally {
      setFeedbackLoading(prev => ({ ...prev, [key]: false }));
    }
  }

  async function triggerAnalysis() {
    setTriggering(true);
    try {
      const data = await api.get(`/api/chat-answers/summary/${applicationId}`, token);
      onLoaded(data);
      onRefreshComments?.();
    } catch { /* silent */ } finally {
      setTriggering(false);
    }
  }

  if (loading) return (
    <div className="ai-tab-loading">
      <RefreshCw size={24} className="ai-tab-loading-icon spin" />
      <p>Analyse en cours…</p>
    </div>
  );

  if (!analysis || (!analysis.motivationLevel && !analysis.technicalLevel)) return (
    <div className="ai-tab-empty">
      <Sparkles size={28} />
      <p>Aucune analyse disponible pour cette candidature.</p>
      <p className="ai-tab-empty-hint">Le candidat doit d'abord compléter le questionnaire.</p>
      <button className="ai-trigger-btn" onClick={triggerAnalysis} disabled={triggering}>
        {triggering ? 'Analyse en cours…' : "Lancer l'analyse"} <ChevronRight size={14} />
      </button>
    </div>
  );

  const actionCfg = ACTION_CONFIG[analysis.recommendedAction] ?? null;

  return (
    <div className="ai-tab">
      <div className="ai-refresh-bar">
        <button className="ai-refresh-btn" onClick={triggerAnalysis} disabled={triggering}>
          <RefreshCw size={13} className={triggering ? 'spin' : ''} />
          {triggering ? 'Rafraîchissement…' : "Rafraîchir l'analyse"}
        </button>
      </div>

      {actionCfg && (
        <div className="ai-action-banner" style={{ background: actionCfg.bg, borderColor: actionCfg.color }}>
          <actionCfg.icon size={18} style={{ color: actionCfg.color, flexShrink: 0 }} />
          <div>
            <p className="ai-action-label" style={{ color: actionCfg.color }}>{actionCfg.label}</p>
            {analysis.recruiterGuidance && <p className="ai-action-guidance">{analysis.recruiterGuidance}</p>}
          </div>
        </div>
      )}

      <div className="ai-scores-grid">
        <ScoreCard title="Motivation"       value={<LevelBadge value={analysis.motivationLevel} />} />
        <ScoreCard title="Niveau technique" value={<LevelBadge value={analysis.technicalLevel} />} />
        <ScoreCard title="Complétude" value={
          <div className="ai-completeness">
            <div className="ai-completeness-bar">
              <div className="ai-completeness-fill" style={{ width: `${Math.round((analysis.completenessScore ?? 0) * 100)}%` }} />
            </div>
            <span>{Math.round((analysis.completenessScore ?? 0) * 100)}%</span>
          </div>
        } />
        <ScoreCard title="Disponibilité" value={
          <span className="ai-badge" style={{ color: '#0ea5e9', background: '#e0f2fe' }}>
            {analysis.hasClearAvailability ? 'Précisée' : 'Non précisée'}
          </span>
        } />
      </div>

      {analysis.motivationSummary && analysis.motivationSummary !== 'Motivation non fournie.' && (
        <div className="ai-section">
          <p className="ai-section-title">Résumé motivation</p>
          <p className="ai-section-text">{analysis.motivationSummary}</p>
        </div>
      )}

      {(analysis.motivationAssessment || analysis.projectAssessment || analysis.availabilityAssessment || analysis.locationAssessment) && (
        <div className="ai-section">
          <p className="ai-section-title">Évaluations qualitatives</p>
          {[
            { label: 'Motivation',    value: analysis.motivationAssessment    },
            { label: 'Projets',       value: analysis.projectAssessment       },
            { label: 'Disponibilité', value: analysis.availabilityAssessment  },
            { label: 'Localisation',  value: analysis.locationAssessment      },
          ].filter(r => r.value).map(r => (
            <div key={r.label} className="ai-assessment-row">
              <span className="ai-assessment-label">{r.label}</span>
              <p className="ai-assessment-text">{r.value}</p>
            </div>
          ))}
        </div>
      )}

      {(analysis.githubSummary || analysis.githubAssessment) && (
        <div className="ai-section">
          <p className="ai-section-title" style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
            <Github size={13} /> Profil GitHub / Portfolio
          </p>
          <div className="ai-github-block">
            {analysis.githubSummary    && <p className="ai-section-text" style={{ marginBottom: 4 }}>{analysis.githubSummary}</p>}
            {analysis.githubAssessment && <p className="ai-github-assessment">{analysis.githubAssessment}</p>}
          </div>
        </div>
      )}

      {analysis.strengths?.length > 0 && (
        <div className="ai-section">
          <p className="ai-section-title">Points forts</p>
          {analysis.strengths.map((s, i) => (
            <div key={i} className="ai-list-item ai-list-item-green">
              <CheckCircle size={13} /> <span>{s}</span>
            </div>
          ))}
        </div>
      )}

      {analysis.pointsToConfirm?.length > 0 && (
        <div className="ai-section">
          <p className="ai-section-title">À vérifier</p>
          {analysis.pointsToConfirm.map((p, i) => (
            <div key={i} className="ai-list-item ai-list-item-orange">
              <AlertTriangle size={13} /> <span>{p}</span>
            </div>
          ))}
        </div>
      )}

      {analysis.inconsistencies?.length > 0 && (
        <div className="ai-section">
          <p className="ai-section-title">⚠ Incohérences détectées</p>
          {analysis.inconsistencies.map((inc, i) => (
            <div key={i} className="ai-list-item ai-list-item-red">
              <X size={13} /> <span>{inc}</span>
            </div>
          ))}
        </div>
      )}

      {analysis.followUpQuestions?.length > 0 && (
        <div className="ai-section">
          <p className="ai-section-title">Questions à poser en entretien</p>
          {analysis.followUpQuestions.map((q, i) => (
            <div key={i} className="ai-followup-item">
              <span className="ai-followup-num">{i + 1}</span>
              <span>{q}</span>
            </div>
          ))}
        </div>
      )}

      {analysis.technicalSkills?.length > 0 && (
        <div className="ai-section">
          <p className="ai-section-title">Compétences détectées</p>
          <div className="ai-skills">
            {analysis.technicalSkills.map((s, i) => <span key={i} className="ai-skill-tag">{s}</span>)}
          </div>
        </div>
      )}

      {analysis.analysisSchema?.facts?.length > 0 && (
        <div className="ai-section">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
            <p className="ai-section-title" style={{ margin: 0 }}>Constats</p>
            {analysis.analysisReviewCoverage?.totalFacts > 0 && (
              <div className="ai-review-coverage">
                <span className="ai-review-coverage-label">
                  {Object.keys(feedbackMap).length}/{analysis.analysisReviewCoverage.totalFacts}
                </span>
                <div className="ai-review-coverage-bar">
                  <div
                    className="ai-review-coverage-fill"
                    style={{ width: `${Math.round((analysis.analysisReviewCoverage.completionRate ?? 0) * 100)}%` }}
                  />
                </div>
              </div>
            )}
          </div>
          {analysis.analysisSchema.facts.map((fact, i) => {
            const key = `${fact.dimension ?? 'general'}::${fact.finding ?? ''}`;
            const cur = feedbackMap[key];
            const busy = feedbackLoading[key];
            return (
              <div key={i} className="ai-fact-item">
                <div className="ai-fact-body">
                  {fact.dimension && <div className="ai-fact-dimension">{fact.dimension}</div>}
                  <div className="ai-fact-finding">{fact.finding}</div>
                  {fact.evidence && <div className="ai-fact-evidence">{fact.evidence}</div>}
                  {fact.confidence > 0 && (
                    <div className="ai-fact-confidence">Confiance : {Math.round(fact.confidence * 100)}%</div>
                  )}
                </div>
                <div className="ai-fact-feedback">
                  <button
                    className={`ai-fact-feedback-btn${cur === 'CONFIRMED' ? ' confirmed' : ''}`}
                    onClick={() => !busy && submitFeedback(fact, 'CONFIRMED')}
                    title="Confirmer ce constat"
                    disabled={busy}
                  >👍</button>
                  <button
                    className={`ai-fact-feedback-btn${cur === 'REJECTED' ? ' rejected' : ''}`}
                    onClick={() => !busy && submitFeedback(fact, 'REJECTED')}
                    title="Contester ce constat"
                    disabled={busy}
                  >👎</button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function ScoreCard({ title, value }) {
  return (
    <div className="ai-score-card">
      <p className="ai-score-title">{title}</p>
      <div className="ai-score-value">{value}</div>
    </div>
  );
}

/* ── Decision tab ────────────────────────────────────────── */
const SENTIMENT_CFG = {
  FAVORABLE:   { label: 'Favorable',   color: '#059669', bg: '#d1fae5', icon: ThumbsUp   },
  NEUTRE:      { label: 'Neutre',      color: '#d97706', bg: '#fef3c7', icon: Minus       },
  DÉFAVORABLE: { label: 'Défavorable', color: '#dc2626', bg: '#fee2e2', icon: ThumbsDown  },
  DEFAVORABLE: { label: 'Défavorable', color: '#dc2626', bg: '#fee2e2', icon: ThumbsDown  },
};

function DecisionTab({ applicationId, token, currentStatusCode, onStatusChange }) {
  const [decision,   setDecision]   = useState(null);
  const [loadingDec, setLoadingDec] = useState(true);
  const [sentiment,  setSentiment]  = useState('FAVORABLE');
  const [comment,    setComment]    = useState('');
  const [confidence, setConfidence] = useState(3);
  const [submitting, setSubmitting] = useState(false);
  const [finalStatus,  setFinalStatus]  = useState('RETENU');
  const [rationale,    setRationale]    = useState('');
  const [submittingFinal, setSubmittingFinal] = useState(false);
  const [emailSending, setEmailSending] = useState(false);
  const [emailSent,    setEmailSent]    = useState(null);

  useEffect(() => {
    setLoadingDec(true);
    api.get(`/api/applications/${applicationId}/decision`, token)
      .then(d => { setDecision(d); setLoadingDec(false); })
      .catch(() => setLoadingDec(false));
  }, [applicationId, token]);

  async function handleAddInput(e) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const saved = await api.post(`/api/applications/${applicationId}/decision-inputs`, {
        sentiment, comment, confidence,
      }, token);
      setDecision(prev => ({
        ...prev,
        inputs: [...(prev?.inputs ?? []), saved],
        blockingReason: null,
      }));
      setComment('');
      /* Auto-avance vers en_attente_avis si le dossier n'y est pas encore */
      if (['nouveau', 'en_etude'].includes(currentStatusCode)) {
        onStatusChange?.('en_attente_avis');
      }
    } catch { /* silent */ } finally {
      setSubmitting(false);
    }
  }

  async function handleFinalDecision(e) {
    e.preventDefault();
    setSubmittingFinal(true);
    try {
      const saved = await api.post(`/api/applications/${applicationId}/decision`, {
        finalStatus, rationale,
      }, token);
      setDecision(saved);
      if (finalStatus === 'RETENU') onStatusChange?.('retenu');
      else if (finalStatus === 'REFUSÉ') onStatusChange?.('non_retenu');
    } catch { /* silent */ } finally {
      setSubmittingFinal(false);
    }
  }

  async function handleSendEmail() {
    if (!decision?.finalStatus || emailSending) return;
    setEmailSending(true);
    try {
      const endpoint = decision.finalStatus === 'RETENU'
        ? `/api/notifications/candidate/retention/${applicationId}`
        : `/api/notifications/candidate/rejection/${applicationId}`;
      const result = await api.post(endpoint, {}, token);
      setEmailSent(result?.success ? 'sent' : 'error');
    } catch { setEmailSent('error'); } finally {
      setEmailSending(false);
    }
  }

  if (loadingDec) return <div className="tab-empty">Chargement…</div>;

  const inputs = decision?.inputs ?? [];
  const hasFinal = !!decision?.id;

  return (
    <div className="decision-tab">
      {/* ── Avis de l'équipe ── */}
      <div className="decision-section">
        <h3 className="decision-section-title">Avis de l'équipe</h3>

        {inputs.length === 0 ? (
          <p className="tab-empty" style={{ padding: '12px 0', textAlign: 'left' }}>
            Aucun avis pour l'instant.
          </p>
        ) : (
          <div className="decision-inputs-list">
            {inputs.map((inp, i) => {
              const cfg = SENTIMENT_CFG[inp.sentiment] ?? { label: inp.sentiment, color: '#6b7280', bg: '#f1f5f9', icon: Minus };
              const Icon = cfg.icon;
              return (
                <div key={inp.id ?? i} className="decision-input-card">
                  <div className="decision-input-header">
                    <span className="decision-input-badge" style={{ background: cfg.bg, color: cfg.color }}>
                      <Icon size={11} /> {cfg.label}
                    </span>
                    <div className="decision-confidence">
                      {[1,2,3,4,5].map(n => (
                        <span key={n} style={{ color: n <= (inp.confidence ?? 3) ? '#f59e0b' : '#d1d5db', fontSize: 12 }}>★</span>
                      ))}
                    </div>
                    <span className="decision-input-date">{timeAgo(inp.createdAt)}</span>
                  </div>
                  {inp.comment && <p className="decision-input-comment">{inp.comment}</p>}
                </div>
              );
            })}
          </div>
        )}

        {/* Add opinion form */}
        {!hasFinal && (
          <form className="decision-add-form" onSubmit={handleAddInput}>
            <div className="decision-sentiment-row">
              {['FAVORABLE', 'NEUTRE', 'DÉFAVORABLE'].map(s => {
                const cfg = SENTIMENT_CFG[s];
                const Icon = cfg.icon;
                return (
                  <button
                    type="button"
                    key={s}
                    className={`decision-sentiment-btn${sentiment === s ? ' selected' : ''}`}
                    style={sentiment === s ? { borderColor: cfg.color, background: cfg.bg, color: cfg.color } : {}}
                    onClick={() => setSentiment(s)}
                  >
                    <Icon size={13} /> {cfg.label}
                  </button>
                );
              })}
            </div>
            <div className="decision-confidence-row">
              <span className="decision-conf-label">Confiance</span>
              {[1,2,3,4,5].map(n => (
                <button
                  type="button"
                  key={n}
                  className="decision-star-btn"
                  onClick={() => setConfidence(n)}
                  style={{ color: n <= confidence ? '#f59e0b' : '#d1d5db' }}
                >★</button>
              ))}
            </div>
            <textarea
              className="decision-comment-input"
              placeholder="Commentaire (optionnel)…"
              value={comment}
              onChange={e => setComment(e.target.value)}
              rows={2}
            />
            <button type="submit" className="decision-submit-btn" disabled={submitting}>
              {submitting ? 'Envoi…' : 'Ajouter mon avis'} <ArrowRight size={13} />
            </button>
          </form>
        )}
      </div>

      {/* ── Décision finale ── */}
      {!hasFinal && inputs.length > 0 && (
        <div className="decision-section">
          <h3 className="decision-section-title">Décision finale</h3>
          <form className="decision-final-form" onSubmit={handleFinalDecision}>
            <div className="decision-final-btns">
              <button
                type="button"
                className={`decision-final-btn${finalStatus === 'RETENU' ? ' selected-green' : ''}`}
                onClick={() => setFinalStatus('RETENU')}
              >
                <CheckCircle size={14} /> Retenu
              </button>
              <button
                type="button"
                className={`decision-final-btn${finalStatus === 'REFUSÉ' ? ' selected-red' : ''}`}
                onClick={() => setFinalStatus('REFUSÉ')}
              >
                <X size={14} /> Refusé
              </button>
            </div>
            <textarea
              className="decision-comment-input"
              placeholder="Justification de la décision…"
              value={rationale}
              onChange={e => setRationale(e.target.value)}
              rows={3}
              required
            />
            <button type="submit" className="decision-submit-btn primary" disabled={submittingFinal || !rationale.trim()}>
              {submittingFinal ? 'Enregistrement…' : 'Valider la décision'} <ArrowRight size={13} />
            </button>
          </form>
        </div>
      )}

      {/* ── Décision enregistrée ── */}
      {hasFinal && (
        <div className="decision-section">
          <h3 className="decision-section-title">Décision enregistrée</h3>
          <div className="decision-final-result">
            <span
              className="decision-final-verdict"
              style={{
                background: decision.finalStatus === 'RETENU' ? '#d1fae5' : '#fee2e2',
                color:      decision.finalStatus === 'RETENU' ? '#059669' : '#dc2626',
              }}
            >
              {decision.finalStatus === 'RETENU' ? <CheckCircle size={14} /> : <X size={14} />}
              {decision.finalStatus}
            </span>
            <span className="decision-final-date">{timeAgo(decision.decidedAt)}</span>
          </div>
          {decision.rationale && <p className="decision-rationale">{decision.rationale}</p>}

          {/* Email candidat */}
          <div className="decision-email-section">
            <button
              className={`decision-email-btn${emailSent === 'sent' ? ' sent' : ''}`}
              onClick={handleSendEmail}
              disabled={emailSending || emailSent === 'sent'}
            >
              {emailSent === 'sent' ? (
                <><MailCheck size={14} /> Email envoyé !</>
              ) : emailSending ? (
                <>Envoi en cours…</>
              ) : (
                <><Mail size={14} /> Notifier le candidat par email</>
              )}
            </button>
            {emailSent === 'error' && <span className="decision-email-error">Échec de l'envoi.</span>}
          </div>

          {decision.aiReview && (
            <div className="decision-ai-review">
              <div className="decision-ai-review-header">
                <Sparkles size={14} style={{ color: '#6366f1' }} />
                <span>Évaluation automatique</span>
              </div>
              <div className="decision-ai-review-body">{renderMarkdown(decision.aiReview)}</div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/* ── Entretiens tab ──────────────────────────────────────── */
const INTERVIEW_STATUS = {
  SCHEDULED:   { label: 'Planifié',  bg: '#eff6ff', color: '#2563eb' },
  COMPLETED:   { label: 'Terminé',   bg: '#d1fae5', color: '#059669' },
  CANCELLED:   { label: 'Annulé',    bg: '#fee2e2', color: '#dc2626' },
  RESCHEDULED: { label: 'Reporté',   bg: '#fef3c7', color: '#d97706' },
};

function EntretiensTab({ applicationId, token, currentStatusCode, onStatusChange }) {
  const [interviews, setInterviews] = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [showForm,   setShowForm]   = useState(false);
  const [saving,     setSaving]     = useState(false);
  const [form, setForm] = useState({
    title: 'Entretien téléphonique', scheduledAt: '', durationMinutes: 30,
    location: '', meetingUrl: '', notes: '',
  });

  useEffect(() => {
    setLoading(true);
    api.get(`/api/applications/${applicationId}/interviews`, token)
      .then(d => { setInterviews(Array.isArray(d) ? d : []); setLoading(false); })
      .catch(() => setLoading(false));
  }, [applicationId, token]);

  async function handleSchedule(e) {
    e.preventDefault();
    setSaving(true);
    try {
      const saved = await api.post(`/api/applications/${applicationId}/interviews`, {
        ...form,
        durationMinutes: Number(form.durationMinutes),
      }, token);
      setInterviews(p => [saved, ...p]);
      setShowForm(false);
      setForm({ title: 'Entretien téléphonique', scheduledAt: '', durationMinutes: 30, location: '', meetingUrl: '', notes: '' });
      /* Auto-avance vers entretien si le dossier n'y est pas encore */
      if (['nouveau', 'en_etude', 'en_attente_avis'].includes(currentStatusCode)) {
        onStatusChange?.('entretien');
      }
    } catch { /* silent */ } finally {
      setSaving(false);
    }
  }

  async function handleUpdateStatus(interviewId, status) {
    try {
      const updated = await api.patch(`/api/applications/${applicationId}/interviews/${interviewId}/status`, { status }, token);
      setInterviews(p => p.map(i => i.interviewId === interviewId ? updated : i));
    } catch { /* silent */ }
  }

  if (loading) return <div className="tab-empty">Chargement…</div>;

  return (
    <div className="entretiens-tab">
      <div className="entretiens-header">
        <button className="decision-submit-btn primary" onClick={() => setShowForm(v => !v)}>
          <Calendar size={13} /> {showForm ? 'Annuler' : 'Planifier un entretien'}
        </button>
      </div>

      {showForm && (
        <form className="entretien-form" onSubmit={handleSchedule}>
          <div className="form-field">
            <label>Titre</label>
            <input value={form.title} onChange={e => setForm(f => ({...f, title: e.target.value}))} required />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <div className="form-field">
              <label>Date & heure *</label>
              <input type="datetime-local" value={form.scheduledAt} onChange={e => setForm(f => ({...f, scheduledAt: e.target.value}))} required />
            </div>
            <div className="form-field">
              <label>Durée (min)</label>
              <input type="number" value={form.durationMinutes} onChange={e => setForm(f => ({...f, durationMinutes: e.target.value}))} min={15} step={15} />
            </div>
          </div>
          <div className="form-field">
            <label>Lieu</label>
            <input value={form.location} onChange={e => setForm(f => ({...f, location: e.target.value}))} placeholder="Locaux, visioconférence…" />
          </div>
          <div className="form-field">
            <label>Lien visio</label>
            <input value={form.meetingUrl} onChange={e => setForm(f => ({...f, meetingUrl: e.target.value}))} placeholder="https://meet.google.com/…" />
          </div>
          <div className="form-field">
            <label>Notes</label>
            <textarea value={form.notes} onChange={e => setForm(f => ({...f, notes: e.target.value}))} rows={2} placeholder="Points à aborder, préparation…" />
          </div>
          <button type="submit" className="decision-submit-btn primary" disabled={saving}>
            {saving ? 'Enregistrement…' : 'Planifier'} <ArrowRight size={13} />
          </button>
        </form>
      )}

      {interviews.length === 0 && !showForm ? (
        <p className="tab-empty" style={{ paddingTop: 12 }}>Aucun entretien planifié.</p>
      ) : (
        <div className="entretiens-list">
          {interviews.map(iv => {
            const sc = INTERVIEW_STATUS[iv.status] ?? { label: iv.status, bg: '#f1f5f9', color: '#64748b' };
            return (
              <div key={iv.interviewId} className="entretien-card">
                <div className="entretien-card-header">
                  <span className="entretien-title">{iv.title}</span>
                  <span style={{ background: sc.bg, color: sc.color, padding: '2px 8px', borderRadius: 20, fontSize: 11, fontWeight: 700 }}>{sc.label}</span>
                </div>
                {iv.scheduledAt && (
                  <div className="entretien-meta">
                    <Calendar size={12} />
                    <span>{new Date(iv.scheduledAt).toLocaleString('fr-FR', { dateStyle: 'medium', timeStyle: 'short' })}</span>
                    {iv.durationMinutes && <span>· {iv.durationMinutes} min</span>}
                  </div>
                )}
                {iv.location && <p className="entretien-detail">{iv.location}</p>}
                {iv.meetingUrl && (
                  <a href={iv.meetingUrl} target="_blank" rel="noreferrer" className="entretien-link">
                    Rejoindre <ChevronRight size={12} />
                  </a>
                )}
                {iv.status === 'SCHEDULED' && (
                  <div className="entretien-actions">
                    <button className="job-quick-btn green" onClick={() => handleUpdateStatus(iv.interviewId, 'COMPLETED')}>
                      <CheckCircle size={11} /> Terminé
                    </button>
                    <button className="job-quick-btn" onClick={() => handleUpdateStatus(iv.interviewId, 'CANCELLED')}>
                      <X size={11} /> Annuler
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

/* ── Historique tab ──────────────────────────────────────── */
const EVENT_CONFIG = {
  STATUS_CHANGED:          { icon: UserCheck,      color: '#6366f1', label: 'Statut modifié'       },
  COMMENT_ADDED:           { icon: MessageCircle,  color: '#0ea5e9', label: 'Commentaire ajouté'   },
  DECISION_RECORDED:       { icon: Award,          color: '#059669', label: 'Décision enregistrée' },
  INTERVIEW_SCHEDULED:     { icon: Calendar,       color: '#7c3aed', label: 'Entretien planifié'   },
  DOCUMENT_ADDED:          { icon: FileText,       color: '#d97706', label: 'Document ajouté'      },
  AI_ANALYSIS_DONE:        { icon: Sparkles,        color: '#6366f1', label: 'Analyse effectuée'    },
  CHATBOT_COMPLETED:       { icon: Zap,            color: '#f59e0b', label: 'Questionnaire terminé'},
  TASK_CREATED:            { icon: CheckCircle,    color: '#10b981', label: 'Tâche créée'          },
  TASK_DONE:               { icon: CheckCircle,    color: '#059669', label: 'Tâche terminée'       },
  ANSWER_SUBMITTED:        { icon: Send,           color: '#3b82f6', label: 'Réponse soumise'      },
  BATCH_ANSWERS_SUBMITTED: { icon: Send,           color: '#3b82f6', label: 'Réponses soumises'    },
};

function formatEventDetail(event) {
  const p = event.payload ?? {};
  switch (event.eventType) {
    case 'STATUS_CHANGED':
      return p.from ? `${p.from} → ${p.to}` : `→ ${p.to ?? p.label ?? ''}`;
    case 'DECISION_RECORDED':
      return p.type === 'FINAL' ? `Décision finale : ${p.finalStatus ?? ''}` : `Avis (${p.sentiment ?? p.type ?? ''})`;
    case 'COMMENT_ADDED':
      return p.preview ? `"${p.preview}"` : null;
    default:
      return null;
  }
}

function HistoriqueTab({ applicationId, token }) {
  const [events,  setEvents]  = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    api.get(`/api/applications/${applicationId}/activity`, token)
      .then(data => { setEvents(Array.isArray(data) ? data : []); setLoading(false); })
      .catch(() => setLoading(false));
  }, [applicationId, token]);

  if (loading) return <div className="tab-empty">Chargement…</div>;

  if (events.length === 0) return (
    <div className="tab-empty">
      <Clock size={24} strokeWidth={1.2} style={{ margin: '0 auto 8px', display: 'block', color: '#d1d5db' }} />
      Aucun événement pour l'instant.
    </div>
  );

  return (
    <div className="historique-tab">
      {events.map((ev, i) => {
        const cfg = EVENT_CONFIG[ev.eventType] ?? { icon: Clock, color: '#6b7280', label: ev.eventType };
        const Icon = cfg.icon;
        const detail = formatEventDetail(ev);
        return (
          <div key={ev.id ?? i} className="historique-item">
            <div className="historique-icon" style={{ background: cfg.color + '18', color: cfg.color }}>
              <Icon size={13} />
            </div>
            <div className="historique-body">
              <span className="historique-label">{cfg.label}</span>
              {detail && <span className="historique-detail">{detail}</span>}
            </div>
            <span className="historique-date">{timeAgo(ev.createdAt)}</span>
          </div>
        );
      })}
    </div>
  );
}

/* ── Comments tab ────────────────────────────────────────── */
const AI_AUTHOR_IDS = new Set([
  '00000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000002',
  '00000000-0000-0000-0000-000000000003',
]);
const AI_PERSONA_NAMES = {
  '00000000-0000-0000-0000-000000000001': 'Findem',
  '00000000-0000-0000-0000-000000000002': 'Findem',
  '00000000-0000-0000-0000-000000000003': 'Findem',
};
const AI_PERSONA_COLORS = {
  '00000000-0000-0000-0000-000000000001': '#6366f1',
  '00000000-0000-0000-0000-000000000002': '#0ea5e9',
  '00000000-0000-0000-0000-000000000003': '#10b981',
};

function CommentsTab({ comments, newComment, onNewComment, onSend, sending, endRef }) {
  return (
    <div className="comments-tab">
      <div className="comments-list">
        {comments.length === 0 ? (
          <p className="tab-empty">Aucun commentaire.</p>
        ) : comments.map((c, i) => {
          const isAI = AI_AUTHOR_IDS.has(c.authorId) || c.authorType === 'AI_SYSTEM';
          const personaName = AI_PERSONA_NAMES[c.authorId];
          const personaColor = AI_PERSONA_COLORS[c.authorId] ?? '#6366f1';
          const authorLabel = personaName ?? c.authorName ?? c.authorType ?? 'Recruteur';
          return (
            <div key={c.commentId ?? i} className={`comment-item${isAI ? ' comment-item-ai' : ''}`}>
              <div
                className="comment-avatar"
                style={isAI ? { background: personaColor } : {}}
              >
                {authorLabel[0].toUpperCase()}
              </div>
              <div className="comment-body">
                <div className="comment-meta">
                  <span className="comment-author" style={isAI ? { color: personaColor } : {}}>
                    {authorLabel}
                  </span>
                  <span className="comment-time">{timeAgo(c.createdAt)}</span>
                </div>
                <div className="comment-text">{renderMarkdown(c.body)}</div>
              </div>
            </div>
          );
        })}
        <div ref={endRef} />
      </div>

      <form className="comment-form" onSubmit={onSend}>
        <input
          className="comment-input"
          placeholder="Ajouter un commentaire interne…"
          value={newComment}
          onChange={e => onNewComment(e.target.value)}
          disabled={sending}
        />
        <button type="submit" className="comment-send" disabled={sending || !newComment.trim()}>
          <Send size={16} />
        </button>
      </form>
    </div>
  );
}
