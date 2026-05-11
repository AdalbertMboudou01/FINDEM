import React, { useEffect, useState, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Search, Github, Globe, FileText, Send, Mail, Phone,
  MapPin, GraduationCap, X, Sparkles, AlertTriangle,
  CheckCircle, TrendingUp, RefreshCw, Filter, ChevronRight,
  ThumbsUp, ThumbsDown, Minus, Star, Award, ArrowRight,
  Clock, MessageCircle, UserCheck, Calendar, Zap, Download, MailCheck,
  MoreVertical, Check, Lock, LayoutGrid, List,
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

const RECOMMENDATION_BADGE = {
  PRIORITY: { label: 'À prioriser',    bg: '#d1fae5', color: '#059669', dot: '#059669' },
  REVIEW:   { label: 'À examiner',     bg: '#fef3c7', color: '#d97706', dot: '#d97706' },
  REJECT:   { label: 'Non recommandé', bg: '#fee2e2', color: '#dc2626', dot: '#dc2626' },
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

const TABS = ['Profil', 'Analyse', 'Avis', 'Entretiens'];

/* ── Main page ───────────────────────────────────────────── */
export default function CandidaturesPage() {
  const { session } = useAuth();
  const token = session?.token;
  const currentUserRole = token ? (decodeToken(token)?.role ?? '') : '';
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
  const [summaryMap,        setSummaryMap]        = useState({});
  const [activeView,        setActiveView]        = useState('all');
  const [showDecisionModal, setShowDecisionModal] = useState(false);
  const [selectedIds,       setSelectedIds]       = useState(new Set());
  const [bulkActing,        setBulkActing]        = useState(false);
  const [viewMode,          setViewMode]          = useState('kanban');
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
                if (s) map[id] = {
                  completenessScore:  s.completenessScore ?? null,
                  recommendedAction:  s.recommendedAction ?? null,
                  jobMatchLevel:      s.jobMatchLevel ?? null,
                  matchedTechs:       s.matchedJobTechnologies ?? [],
                  missingTechs:       s.missingJobTechnologies ?? [],
                };
              });
              setSummaryMap(map);
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

      setDossier(app);
      setAllowedTransitions(transArr);

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

  async function handleBulkAction(statusCode) {
    if (selectedIds.size === 0 || bulkActing) return;
    setBulkActing(true);
    try {
      await Promise.all(
        [...selectedIds].map(id =>
          api.patch(`/api/applications/${id}/status`, { statusCode }, token).catch(() => null)
        )
      );
      setApps(prev => prev.map(a =>
        selectedIds.has(a.applicationId)
          ? { ...a, status: { code: statusCode, label: statusCode } }
          : a
      ));
      setSelectedIds(new Set());
    } finally {
      setBulkActing(false);
    }
  }

  function toggleSelectId(id, e) {
    e.stopPropagation();
    setSelectedIds(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
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

  function openApp(id) { setSelectedId(id); setActiveTab('Profil'); setAnalysis(null); }

  return (
    <div className={`cand-shell${viewMode === 'kanban' ? ' kanban-mode' : ''}`}>
      {/* ════ KANBAN VIEW ═════════════════════════════════════ */}
      {viewMode === 'kanban' && (
        <KanbanView
          apps={apps}
          summaryMap={summaryMap}
          selectedId={selectedId}
          onSelect={openApp}
          jobFilter={jobFilter}
          setJobFilter={setJobFilter}
          jobs={jobs}
          search={search}
          setSearch={setSearch}
          onViewMode={setViewMode}
          loading={loading}
        />
      )}

      {/* ════ LIST PANEL ══════════════════════════════════════ */}
      {viewMode === 'list' && (
      <div className="cand-list-panel">
        <div className="cand-panel-header">
          <div className="cand-search-bar-inline">
            <Search size={14} className="cand-search-icon-inline" />
            <input
              className="cand-search-input"
              placeholder="Rechercher un candidat…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
            {search && <button className="cand-search-clear" onClick={() => setSearch('')}><X size={13} /></button>}
          </div>
          <div className="cand-view-mode-toggle">
            <button className="cand-toggle-btn active" title="Vue liste"><List size={13} /></button>
            <button className="cand-toggle-btn" title="Vue kanban" onClick={() => setViewMode('kanban')}><LayoutGrid size={13} /></button>
          </div>
        </div>

        <div className="cand-panel-subheader">
          <select className="cand-job-select-inline" value={jobFilter} onChange={e => setJobFilter(e.target.value)}>
            <option value="">Toutes les offres</option>
            {jobs.map(j => <option key={j.jobId} value={j.jobId}>{j.title}</option>)}
          </select>
          {jobFilter && (
            <button
              className={`cand-filter-btn-sm${filterResults ? ' active' : ''}`}
              onClick={filterResults ? () => setFilterResults(null) : handleFilter}
              disabled={filtering}
              title={filtering ? 'Filtrage…' : filterResults ? 'Annuler le filtre IA' : 'Trier par pertinence IA'}
            >
              <Filter size={12} />
              {filtering ? '…' : filterResults ? 'Reset' : 'Trier'}
            </button>
          )}
        </div>

        <div className="cand-list-count">{filtered.length} candidature{filtered.length !== 1 ? 's' : ''}</div>

        <div className="cand-list">
          {loading ? (
            <p className="cand-list-empty">Chargement…</p>
          ) : filtered.length === 0 ? (
            <p className="cand-list-empty">Aucune candidature.</p>
          ) : filtered.map(app => {
            const filterAction = filterResults?.[app.applicationId];
            const fBadge       = filterAction ? FILTER_BADGE[filterAction] : null;
            const summary      = summaryMap[app.applicationId];
            const recBadge     = summary?.recommendedAction ? RECOMMENDATION_BADGE[summary.recommendedAction] : null;
            return (
              <div
                key={app.applicationId}
                className={`cand-item${selectedId === app.applicationId ? ' active' : ''}`}
                onClick={() => openApp(app.applicationId)}
              >
                <div className="cand-item-avatar">{initials(app)}</div>
                <div className="cand-item-body">
                  <span className="cand-item-name">
                    {app.candidate?.firstName} {app.candidate?.lastName}
                  </span>
                  <span className="cand-item-job">{app.job?.title ?? '—'}</span>
                  {recBadge && !fBadge && (
                    <span className="cand-item-rec-badge" style={{ background: recBadge.bg, color: recBadge.color }}>
                      <span className="rec-dot" style={{ background: recBadge.dot }} />
                      {recBadge.label}
                    </span>
                  )}
                  {fBadge && (
                    <span style={{ background: fBadge.bg, color: fBadge.color, fontSize: 10, fontWeight: 700, padding: '1px 6px', borderRadius: 4, marginTop: 2, display: 'inline-block' }}>
                      {fBadge.label}
                    </span>
                  )}
                </div>
                <div className="cand-item-side">
                  <StatusBadge code={app.status?.code} label={app.status?.label} />
                  <span className="cand-item-date">{timeAgo(app.createdAt)}</span>
                  {summary?.completenessScore != null && (
                    <div className="cand-item-progress" title={`Questionnaire: ${Math.round(summary.completenessScore * 100)}%`}>
                      <div className="cand-item-progress-bar" style={{ width: `${Math.round(summary.completenessScore * 100)}%` }} />
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
      )}

      {/* ════ DOSSIER PANEL ═══════════════════════════════════ */}
      {viewMode === 'kanban' && selectedId && (
        <div className="kanban-drawer-overlay" onClick={() => setSelectedId(null)} />
      )}
      <div className={`cand-dossier${viewMode === 'kanban' && !selectedId ? ' cand-dossier-hidden' : ''}`}>
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
              <div className="cand-header-right">
                <StatusBadge code={dossier.status?.code} label={dossier.status?.label} />
                <button className="decision-finale-header-btn" onClick={() => setShowDecisionModal(true)}>
                  <Award size={14} /> Décision finale
                </button>
                <DossierMenu
                  allowedTransitions={allowedTransitions}
                  currentCode={dossier.status?.code}
                  onTransition={handlePatchStatus}
                  patching={patchingStatus}
                />
                {viewMode === 'kanban' && (
                  <button className="icon-btn" onClick={() => setSelectedId(null)} title="Fermer">
                    <X size={16} />
                  </button>
                )}
              </div>
            </div>

            <CompactPipeline
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
                  {tab}
                </button>
              ))}
            </div>

            <div className="cand-tab-content">
              {activeTab === 'Profil'  && <ProfilTab candidate={cand} cvFile={cvFile} />}
              {activeTab === 'Analyse' && (
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
              {activeTab === 'Avis' && (
                <DecisionTab
                  applicationId={selectedId}
                  token={token}
                  currentStatusCode={dossier.status?.code}
                  onStatusChange={handlePatchStatus}
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
            </div>

            {showDecisionModal && (
              <DecisionFinaleModal
                applicationId={selectedId}
                token={token}
                currentUserRole={currentUserRole}
                onClose={() => setShowDecisionModal(false)}
                onStatusChange={handlePatchStatus}
              />
            )}
          </>
        )}
      </div>
    </div>
  );
}

/* ── Compact Pipeline ────────────────────────────────────── */
/* ── Kanban view ─────────────────────────────────────────── */
const KANBAN_COLS = [
  { key: 'PRIORITY', label: 'Pertinent',     color: '#4854e8', bg: '#eef0fd', border: '#c7caf8' },
  { key: 'REVIEW',   label: 'À étudier',     color: '#64748b', bg: '#f1f5f9', border: '#cbd5e1' },
  { key: 'REJECT',   label: 'Non pertinent', color: '#94a3b8', bg: '#f8fafc', border: '#e2e8f0' },
];

function kanbanColOf(app, summaryMap) {
  const ra = summaryMap[app.applicationId]?.recommendedAction;
  if (ra === 'PRIORITY') return 'PRIORITY';
  if (ra === 'REJECT')   return 'REJECT';
  return 'REVIEW';
}

function KanbanCard({ app, summary, active, onClick }) {
  return (
    <div className={`kanban-card${active ? ' active' : ''}`} onClick={onClick}>
      <div className="kanban-card-top">
        <div className="kanban-card-avatar">{initials(app)}</div>
        <div className="kanban-card-info">
          <span className="kanban-card-name">{app.candidate?.firstName} {app.candidate?.lastName}</span>
          <span className="kanban-card-job">{app.job?.title ?? '—'}</span>
        </div>
      </div>
      <div className="kanban-card-bottom">
        <StatusBadge code={app.status?.code} label={app.status?.label} />
        <span className="kanban-card-date">{timeAgo(app.createdAt)}</span>
      </div>
      {summary?.completenessScore != null && (
        <div className="cand-item-progress" style={{ margin: '6px 0 0' }}>
          <div className="cand-item-progress-bar" style={{ width: `${Math.round(summary.completenessScore * 100)}%` }} />
        </div>
      )}
    </div>
  );
}

function KanbanView({ apps, summaryMap, selectedId, onSelect, jobFilter, setJobFilter, jobs, search, setSearch, onViewMode, loading }) {
  const filtered = apps
    .filter(a => !jobFilter || a.job?.jobId === jobFilter)
    .filter(a => {
      if (!search) return true;
      const name = `${a.candidate?.firstName ?? ''} ${a.candidate?.lastName ?? ''}`.toLowerCase();
      return name.includes(search.toLowerCase());
    });

  return (
    <div className={`kanban-area${selectedId ? ' has-dossier' : ''}`}>
      <div className="kanban-topbar">
        <div className="kanban-topbar-left">
          <div style={{ position: 'relative', flex: 1, maxWidth: 260 }}>
            <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: '#9ca3af', pointerEvents: 'none' }} />
            <input
              className="cand-search-input"
              style={{ paddingLeft: 32 }}
              placeholder="Rechercher un candidat…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <select className="kanban-job-select" value={jobFilter} onChange={e => setJobFilter(e.target.value)}>
            <option value="">Toutes les offres</option>
            {jobs.map(j => <option key={j.jobId} value={j.jobId}>{j.title}</option>)}
          </select>
          <span className="kanban-count">{filtered.length} candidature{filtered.length !== 1 ? 's' : ''}</span>
        </div>
        <div className="cand-view-mode-toggle">
          <button className="cand-toggle-btn" title="Vue liste" onClick={() => onViewMode('list')}><List size={13} /></button>
          <button className="cand-toggle-btn active" title="Vue kanban"><LayoutGrid size={13} /></button>
        </div>
      </div>

      <div className="kanban-cols">
        {KANBAN_COLS.map(col => {
          const colApps = filtered.filter(a => kanbanColOf(a, summaryMap) === col.key);
          return (
            <div key={col.key} className="kanban-col" style={{ borderTop: `3px solid ${col.color}` }}>
              <div className="kanban-col-header">
                <span className="kanban-col-title" style={{ color: col.color }}>{col.label}</span>
                <span className="kanban-col-count" style={{ background: col.bg, color: col.color, border: `1px solid ${col.border}` }}>{colApps.length}</span>
              </div>
              <div className="kanban-col-body">
                {loading ? (
                  <p className="kanban-empty">Chargement…</p>
                ) : colApps.length === 0 ? (
                  <p className="kanban-empty">Aucun candidat</p>
                ) : colApps.map(app => (
                  <KanbanCard
                    key={app.applicationId}
                    app={app}
                    summary={summaryMap[app.applicationId]}
                    active={selectedId === app.applicationId}
                    onClick={() => onSelect(app.applicationId)}
                  />
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function CompactPipeline({ currentCode, allowedTransitions = [], onTransition, patching }) {
  const currentIdx    = PIPELINE_STAGES.findIndex(s => s.code === currentCode);
  const isOffPipeline = currentIdx < 0;

  if (isOffPipeline) {
    const offLabel = currentCode === 'non_retenu' ? 'Non retenu'
      : currentCode === 'vivier'   ? 'Dans le vivier'
      : currentCode === 'embauche' ? 'Embauché'
      : currentCode ?? '—';
    const offColor = currentCode === 'non_retenu' ? '#dc2626'
      : currentCode === 'embauche' ? '#059669'
      : '#9333ea';
    const canReopen = allowedTransitions.includes('en_etude');
    return (
      <div className="pipeline-stepper off-pipeline">
        <span className="pipeline-off-badge" style={{ background: offColor + '18', color: offColor, border: `1px solid ${offColor}40` }}>
          {offLabel}
        </span>
        {canReopen && (
          <button className="pipeline-reopen-btn" onClick={() => onTransition('en_etude')} disabled={patching}>
            Remettre en étude
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="pipeline-stepper">
      {PIPELINE_STAGES.map((stage, idx) => {
        const isPast      = idx < currentIdx;
        const isCurrent   = idx === currentIdx;
        const isClickable = !isCurrent && !patching && allowedTransitions.includes(stage.code);
        return (
          <React.Fragment key={stage.code}>
            <div
              className={`pipeline-step${isPast ? ' past' : ''}${isCurrent ? ' current' : ''}${isClickable ? ' clickable' : ''}`}
              onClick={isClickable ? () => onTransition(stage.code) : undefined}
              title={isClickable ? `Passer en « ${stage.label} »` : undefined}
            >
              <div className="pipeline-step-dot">
                {isPast && <Check size={9} />}
                {isCurrent && <div className="pipeline-step-dot-inner" />}
              </div>
              <span className="pipeline-step-label">{stage.label}</span>
            </div>
            {idx < PIPELINE_STAGES.length - 1 && (
              <div className={`pipeline-connector${isPast ? ' filled' : ''}`} />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}

/* ── Dossier action menu (⋮) ─────────────────────────────── */
function DossierMenu({ allowedTransitions, currentCode, onTransition, patching }) {
  const [open,       setOpen]       = useState(false);
  const [confirming, setConfirming] = useState(null);
  const ref = useRef(null);

  useEffect(() => {
    function close(e) {
      if (ref.current && !ref.current.contains(e.target)) { setOpen(false); setConfirming(null); }
    }
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const PIPELINE_LABELS = {
    en_etude:         'En étude',
    en_attente_avis:  'Avis équipe',
    entretien:        'Entretien',
    retenu:           'Retenu',
    embauche:         'Embauché',
    non_retenu:       'Non retenu',
    vivier:           'Dans le vivier',
    en_deliberation:  'En délibération',
    second_vote:      'Vote élargi',
  };

  const CONFIRM_CODES = new Set(['non_retenu', 'vivier']);
  const PIPELINE_ORDER = ['en_etude', 'en_attente_avis', 'entretien', 'retenu'];

  const pipelineMoves = allowedTransitions.filter(c => PIPELINE_ORDER.includes(c));
  const terminalMoves = allowedTransitions.filter(c => c === 'non_retenu' || c === 'vivier');
  const recoveryMove  = allowedTransitions.includes('en_etude') && ['non_retenu', 'vivier'].includes(currentCode);

  const hasAny = pipelineMoves.length > 0 || terminalMoves.length > 0 || recoveryMove;
  if (!hasAny) return null;

  function trigger(code) {
    if (CONFIRM_CODES.has(code)) { setConfirming(code); }
    else { onTransition(code); setOpen(false); }
  }

  function confirm() {
    if (confirming) { onTransition(confirming); setOpen(false); setConfirming(null); }
  }

  return (
    <div className="dossier-menu-wrap" ref={ref}>
      <button className="dossier-menu-btn" onClick={() => { setOpen(o => !o); setConfirming(null); }} title="Modifier le statut">
        <MoreVertical size={16} />
      </button>
      {open && (
        <div className="dossier-menu">
          {confirming ? (
            <div className="dossier-menu-confirm">
              <p>Passer en « {PIPELINE_LABELS[confirming]} » ?</p>
              <div className="dossier-menu-confirm-btns">
                <button className="dossier-confirm-yes" onClick={confirm} disabled={patching}>Confirmer</button>
                <button className="dossier-confirm-no"  onClick={() => setConfirming(null)}>Annuler</button>
              </div>
            </div>
          ) : (
            <>
              {(pipelineMoves.length > 0 || recoveryMove) && (
                <>
                  <div className="dossier-menu-section-label">Corriger le statut</div>
                  {recoveryMove && (
                    <button onClick={() => trigger('en_etude')} disabled={patching}>
                      <ArrowRight size={13} /> Remettre en étude
                    </button>
                  )}
                  {pipelineMoves.map(code => (
                    <button key={code} onClick={() => trigger(code)} disabled={patching}>
                      <ArrowRight size={13} /> {PIPELINE_LABELS[code]}
                    </button>
                  ))}
                </>
              )}
              {terminalMoves.length > 0 && (
                <>
                  <div className="dossier-menu-section-label">Actions</div>
                  {terminalMoves.map(code => (
                    <button key={code} className={code === 'non_retenu' ? 'danger' : ''}
                      onClick={() => trigger(code)} disabled={patching}>
                      {code === 'non_retenu' ? <X size={13} /> : <Star size={13} />} {PIPELINE_LABELS[code]}
                    </button>
                  ))}
                </>
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
  const { session } = useAuth();
  const [pdfUrl,     setPdfUrl]     = useState(null);
  const [pdfLoading, setPdfLoading] = useState(false);

  useEffect(() => {
    if (!cvFile || !c?.candidateId) { setPdfUrl(null); return; }
    let objectUrl = null;
    setPdfLoading(true);
    fetch(`/api/files/candidate/${c.candidateId}/cv/content`, {
      headers: { Authorization: `Bearer ${session?.token}` },
    })
      .then(r => r.ok ? r.blob() : null)
      .then(blob => {
        if (blob) { objectUrl = URL.createObjectURL(blob); setPdfUrl(objectUrl); }
        setPdfLoading(false);
      })
      .catch(() => setPdfLoading(false));
    return () => { if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [cvFile, c?.candidateId, session?.token]);

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

      {(c.githubUrl || c.portfolioUrl) && (
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
        </div>
      )}

      {/* ── CV viewer ── */}
      {(cvFile || c.cvPath) && (
        <div className="profil-cv-section">
          <div className="profil-cv-header">
            <FileText size={14} />
            <span>{cvFile?.originalFileName ?? 'CV'}</span>
            {pdfUrl && (
              <a href={pdfUrl} download={cvFile?.originalFileName ?? 'cv.pdf'} className="profil-cv-download">
                <Download size={13} /> Télécharger
              </a>
            )}
          </div>
          {pdfLoading && <p className="profil-cv-loading">Chargement du CV…</p>}
          {pdfUrl && (
            <iframe
              src={pdfUrl}
              className="profil-cv-iframe"
              title="CV du candidat"
            />
          )}
          {!pdfUrl && !pdfLoading && c.cvPath && (
            <p className="profil-cv-loading">
              <a href={`/api/upload/${c.cvPath}`} target="_blank" rel="noreferrer" className="profil-link">
                <ExternalLink size={13} /> Ouvrir le CV
              </a>
            </p>
          )}
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

function LevelBadge({ value, map }) {
  const cfg = LEVEL_CONFIG[value] ?? { label: value ?? '—', color: '#6b7280', bg: '#f1f5f9' };
  const label = map?.[value] ?? cfg.label;
  return (
    <span className="ai-badge" style={{ color: cfg.color, background: cfg.bg }}>{label}</span>
  );
}

function AnalyseTab({ analysis, loading, applicationId, token, onLoaded, onRefreshComments }) {
  const [triggering,      setTriggering]      = useState(false);
  const [feedbackMap,     setFeedbackMap]     = useState({});
  const [feedbackLoading, setFeedbackLoading] = useState({});
  const autoTriggeredRef = useRef(false);

  useEffect(() => {
    setFeedbackMap({});
    autoTriggeredRef.current = false;
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

  const isEmpty = !analysis || (!analysis.motivationLevel && !analysis.technicalLevel);

  useEffect(() => {
    if (isEmpty && !loading && !triggering && applicationId && !autoTriggeredRef.current) {
      autoTriggeredRef.current = true;
      triggerAnalysis();
    }
  }, [isEmpty, loading, applicationId]);

  if (loading || (triggering && isEmpty)) return (
    <div className="ai-tab-loading">
      <RefreshCw size={24} className="ai-tab-loading-icon spin" />
      <p>Analyse en cours…</p>
    </div>
  );

  if (isEmpty) return (
    <div className="ai-tab-empty">
      <Sparkles size={28} />
      <p>Aucune analyse disponible pour cette candidature.</p>
      <p className="ai-tab-empty-hint">Le candidat doit d'abord compléter le questionnaire.</p>
    </div>
  );

  const actionCfg = ACTION_CONFIG[analysis.recommendedAction] ?? null;
  const isLlmPrimary = analysis.analysisSchemaVersion?.includes('llm-primary');

  return (
    <div className="ai-tab">
      <div className="ai-refresh-bar">
        <button className="ai-refresh-btn" onClick={triggerAnalysis} disabled={triggering}>
          <RefreshCw size={13} className={triggering ? 'spin' : ''} />
          {triggering ? 'Rafraîchissement…' : "Rafraîchir l'analyse"}
        </button>
        {isLlmPrimary
          ? <span className="ai-mode-badge ai-mode-llm"><Sparkles size={10} /> Analyse IA</span>
          : <span className="ai-mode-badge ai-mode-fallback">Mode dégradé</span>
        }
      </div>

      {/* ── Bannière recommandation ── */}
      {actionCfg && (
        <div className="ai-action-banner" style={{ background: actionCfg.bg, borderColor: actionCfg.color }}>
          <actionCfg.icon size={18} style={{ color: actionCfg.color, flexShrink: 0 }} />
          <div>
            <p className="ai-action-label" style={{ color: actionCfg.color }}>{actionCfg.label}</p>
            {analysis.recommendationReasoning
              ? <p className="ai-action-guidance">{analysis.recommendationReasoning}</p>
              : analysis.recruiterGuidance && <p className="ai-action-guidance">{analysis.recruiterGuidance}</p>
            }
          </div>
        </div>
      )}

      {/* ── Synthèse globale ── */}
      {analysis.overallAssessment && (
        <div className="ai-synthesis-block">
          <div className="ai-synthesis-header">
            <Sparkles size={14} />
            <span>Synthèse</span>
          </div>
          <p className="ai-synthesis-text">{analysis.overallAssessment}</p>
          {analysis.recruiterGuidance && isLlmPrimary && (
            <p className="ai-synthesis-guidance">{analysis.recruiterGuidance}</p>
          )}
        </div>
      )}

      {/* ── Scores rapides ── */}
      <div className="ai-scores-grid">
        <ScoreCard title="Complétude" value={
          <div className="ai-completeness">
            <div className="ai-completeness-bar">
              <div className="ai-completeness-fill" style={{ width: `${Math.round((analysis.completenessScore ?? 0) * 100)}%` }} />
            </div>
            <span>{Math.round((analysis.completenessScore ?? 0) * 100)}%</span>
          </div>
        } />
        <ScoreCard title="Adéquation poste" value={<LevelBadge value={analysis.jobMatchLevel} />} />
        <ScoreCard title="Expérience"        value={<LevelBadge value={analysis.experienceLevel} map={{ SENIOR: 'Sénior', INTERMEDIATE: 'Intermédiaire', JUNIOR: 'Junior' }} />} />
        <ScoreCard title="Disponibilité" value={
          <span className="ai-badge" style={{ color: '#0ea5e9', background: '#e0f2fe' }}>
            {analysis.hasClearAvailability ? 'Précisée' : 'Non précisée'}
          </span>
        } />
      </div>

      {/* ── Analyse détaillée par dimension ── */}
      <div className="ai-section">
        <p className="ai-section-title">Analyse détaillée</p>
        <div className="ai-dimensions">
          {[
            { key: 'motivation',   label: 'Motivation',           badge: <LevelBadge value={analysis.motivationLevel} />,   reasoning: analysis.motivationReasoning   || analysis.motivationAssessment },
            { key: 'technique',    label: 'Profil technique',      badge: <LevelBadge value={analysis.technicalLevel} />,    reasoning: analysis.technicalReasoning    },
            { key: 'experience',   label: 'Expérience',            badge: <LevelBadge value={analysis.experienceLevel} map={{ SENIOR: 'Sénior', INTERMEDIATE: 'Intermédiaire', JUNIOR: 'Junior' }} />, reasoning: analysis.experienceReasoning },
            { key: 'jobmatch',     label: 'Adéquation au poste',   badge: <LevelBadge value={analysis.jobMatchLevel} />,     reasoning: analysis.jobMatchReasoning     },
            { key: 'availability', label: 'Disponibilité',         badge: null, reasoning: analysis.availabilityReasoning || analysis.availabilityAssessment },
            { key: 'location',     label: 'Localisation',          badge: null, reasoning: analysis.locationReasoning     || analysis.locationAssessment     },
          ].filter(d => d.reasoning).map(d => (
            <div key={d.key} className="ai-dimension-row">
              <div className="ai-dimension-header">
                <span className="ai-dimension-label">{d.label}</span>
                {d.badge}
              </div>
              <p className="ai-dimension-reasoning">{d.reasoning}</p>
            </div>
          ))}
        </div>
      </div>

      {/* ── Lacunes vs correspondances ── */}
      {(analysis.matchedJobTechnologies?.length > 0 || analysis.missingJobTechnologies?.length > 0) && (
        <div className="ai-section">
          <p className="ai-section-title">Correspondance avec le poste</p>
          {analysis.matchedJobTechnologies?.length > 0 && (
            <div className="ai-match-group">
              <span className="ai-match-label ai-match-ok">Couverts</span>
              <div className="ai-skills">
                {analysis.matchedJobTechnologies.map((s, i) => <span key={i} className="ai-skill-tag ai-skill-match">{s}</span>)}
              </div>
            </div>
          )}
          {analysis.missingJobTechnologies?.length > 0 && (
            <div className="ai-match-group">
              <span className="ai-match-label ai-match-miss">Manquants</span>
              <div className="ai-skills">
                {analysis.missingJobTechnologies.map((s, i) => <span key={i} className="ai-skill-tag ai-skill-gap">{s}</span>)}
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── GitHub / Portfolio ── */}
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

      {/* ── Points forts ── */}
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

      {/* ── Points d'attention ── */}
      {analysis.pointsToConfirm?.length > 0 && (
        <div className="ai-section">
          <p className="ai-section-title">Points d'attention</p>
          {analysis.pointsToConfirm.map((p, i) => (
            <div key={i} className="ai-list-item ai-list-item-orange">
              <AlertTriangle size={13} /> <span>{p}</span>
            </div>
          ))}
        </div>
      )}

      {/* ── Incohérences ── */}
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

      {/* ── Questions entretien ── */}
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

      {/* ── Compétences détectées ── */}
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
  FAVORABLE:  { label: 'Favorable',   color: '#059669', bg: '#d1fae5', icon: ThumbsUp  },
  RESERVE:    { label: 'Réservé',     color: '#d97706', bg: '#fef3c7', icon: Minus      },
  DEFAVORABLE:{ label: 'Défavorable', color: '#dc2626', bg: '#fee2e2', icon: ThumbsDown },
};

function DecisionTab({ applicationId, token, currentStatusCode, onStatusChange }) {
  const [decision,   setDecision]   = useState(null);
  const [loadingDec, setLoadingDec] = useState(true);
  const [sentiment,  setSentiment]  = useState('FAVORABLE');
  const [comment,    setComment]    = useState('');
  const [confidence, setConfidence] = useState(3);
  const [submitting,  setSubmitting]  = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [editingId,   setEditingId]   = useState(null);

  useEffect(() => {
    setSubmitError('');
    setEditingId(null);
    setLoadingDec(true);
    api.get(`/api/applications/${applicationId}/decision`, token)
      .then(d => { setDecision(d); setLoadingDec(false); })
      .catch(() => setLoadingDec(false));
  }, [applicationId, token]);

  const ownInput = (decision?.inputs ?? []).find(i => i.own);

  function startEdit(inp) {
    setSentiment(inp.sentiment);
    setComment(inp.comment ?? '');
    setConfidence(inp.confidence ?? 3);
    setEditingId(inp.id);
    setSubmitError('');
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setSubmitError('');
    try {
      if (editingId) {
        const saved = await api.patch(
          `/api/applications/${applicationId}/decision-inputs/${editingId}`,
          { sentiment, comment, confidence }, token
        );
        setDecision(prev => ({ ...prev, inputs: prev.inputs.map(i => i.id === editingId ? saved : i) }));
        setEditingId(null);
      } else {
        const saved = await api.post(`/api/applications/${applicationId}/decision-inputs`,
          { sentiment, comment, confidence }, token);
        setDecision(prev => ({ ...prev, inputs: [...(prev?.inputs ?? []), saved] }));
        setComment('');
        if (['nouveau', 'en_etude'].includes(currentStatusCode)) onStatusChange?.('en_attente_avis');
      }
    } catch (err) {
      setSubmitError(err.message ?? 'Une erreur est survenue.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loadingDec) return <div className="tab-empty">Chargement…</div>;

  const inputs   = decision?.inputs ?? [];
  const hasFinal = !!decision?.id;
  const showForm = !hasFinal && (!ownInput || editingId !== null);

  function AvisForm() {
    return (
      <form className="decision-add-form" onSubmit={handleSubmit}>
        <div className="decision-sentiment-row">
          {['FAVORABLE', 'RESERVE', 'DEFAVORABLE'].map(s => {
            const cfg = SENTIMENT_CFG[s];
            const Icon = cfg.icon;
            return (
              <button type="button" key={s}
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
            <button type="button" key={n} className="decision-star-btn"
              onClick={() => setConfidence(n)}
              style={{ color: n <= confidence ? '#f59e0b' : '#d1d5db' }}
            >★</button>
          ))}
        </div>
        <textarea className="decision-comment-input" placeholder="Commentaire (optionnel)…"
          value={comment} onChange={e => setComment(e.target.value)} rows={2} />
        {submitError && <p className="avis-error">{submitError}</p>}
        <div style={{ display: 'flex', gap: 8 }}>
          {editingId && (
            <button type="button" className="btn-outline" style={{ fontSize: 13 }}
              onClick={() => { setEditingId(null); setSubmitError(''); }}>
              Annuler
            </button>
          )}
          <button type="submit" className="decision-submit-btn" disabled={submitting}>
            {submitting ? 'Envoi…' : editingId ? 'Mettre à jour' : 'Ajouter mon avis'} <ArrowRight size={13} />
          </button>
        </div>
      </form>
    );
  }

  return (
    <div className="decision-tab">
      <div className="decision-section">
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
                <div key={inp.id ?? i} className={`decision-input-card${inp.own ? ' own' : ''}`}>
                  <div className="decision-input-header">
                    <span className="decision-input-badge" style={{ background: cfg.bg, color: cfg.color }}>
                      <Icon size={11} /> {cfg.label}
                    </span>
                    <div className="decision-confidence">
                      {[1,2,3,4,5].map(n => (
                        <span key={n} style={{ color: n <= (inp.confidence ?? 3) ? '#f59e0b' : '#d1d5db', fontSize: 12 }}>★</span>
                      ))}
                    </div>
                    <span className="decision-input-author">{inp.authorName ?? '—'}</span>
                    <span className="decision-input-date">{timeAgo(inp.createdAt)}</span>
                    {inp.own && !hasFinal && editingId !== inp.id && (
                      <button className="avis-edit-btn" onClick={() => startEdit(inp)} title="Modifier mon avis">
                        Modifier
                      </button>
                    )}
                  </div>
                  {inp.comment && <p className="decision-input-comment">{inp.comment}</p>}
                  {editingId === inp.id && <AvisForm />}
                </div>
              );
            })}
          </div>
        )}

        {showForm && editingId === null && <AvisForm />}

        {hasFinal && (
          <p className="avis-locked-notice">
            <CheckCircle size={13} /> Décision finale enregistrée — les avis sont verrouillés.
          </p>
        )}
      </div>
    </div>
  );
}

/* ── Décision finale modal ───────────────────────────────── */
function DecisionFinaleModal({ applicationId, token, currentUserRole, onClose, onStatusChange }) {
  const [decision,     setDecision]     = useState(null);
  const [loading,      setLoading]      = useState(true);
  const [finalStatus,  setFinalStatus]  = useState('retenu');
  const [rationale,    setRationale]    = useState('');
  const [submitting,   setSubmitting]   = useState(false);
  const [submitError,  setSubmitError]  = useState(null);
  const [emailSending, setEmailSending] = useState(false);
  const [emailSent,    setEmailSent]    = useState(null);

  const canDecide = (currentUserRole ?? '').toUpperCase() === 'MANAGER';

  useEffect(() => {
    api.get(`/api/applications/${applicationId}/decision`, token)
      .then(d => { setDecision(d); setLoading(false); })
      .catch(() => setLoading(false));
  }, [applicationId, token]);

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setSubmitError(null);
    try {
      const saved = await api.post(`/api/applications/${applicationId}/decision`, { finalStatus, rationale }, token);
      setDecision(saved);
      if (finalStatus === 'retenu') onStatusChange?.('retenu');
      else if (finalStatus === 'non_retenu') onStatusChange?.('non_retenu');
    } catch (err) {
      setSubmitError(err?.message ?? 'Erreur lors de l\'enregistrement.');
    } finally {
      setSubmitting(false);
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

  const hasFinal = !!decision?.id;

  return (
    <>
      <div className="modal-overlay" onClick={onClose} />
      <div className="modal-panel">
        <div className="modal-header">
          <div className="modal-header-left">
            <Award size={18} style={{ color: '#6366f1' }} />
            <h3>Décision finale</h3>
          </div>
          <button className="icon-btn" onClick={onClose}><X size={18} /></button>
        </div>

        {loading ? (
          <div className="tab-empty">Chargement…</div>
        ) : hasFinal ? (
          <div className="modal-body">
            <div className="decision-final-result">
              <span className="decision-final-verdict"
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
            {decision.aiReview && (
              <div className="decision-ai-review">
                <div className="decision-ai-review-header">
                  <Sparkles size={14} style={{ color: '#6366f1' }} /> <span>Évaluation automatique</span>
                </div>
                <div className="decision-ai-review-body">{renderMarkdown(decision.aiReview)}</div>
              </div>
            )}
            <div className="decision-email-section">
              <button className={`decision-email-btn${emailSent === 'sent' ? ' sent' : ''}`}
                onClick={handleSendEmail} disabled={emailSending || emailSent === 'sent'}>
                {emailSent === 'sent' ? <><MailCheck size={14} /> Email envoyé !</>
                  : emailSending ? <>Envoi en cours…</>
                  : <><Mail size={14} /> Notifier le candidat par email</>}
              </button>
              {emailSent === 'error' && <span className="decision-email-error">Échec de l'envoi.</span>}
            </div>
          </div>
        ) : !canDecide ? (
          <div className="modal-body">
            <p className="avis-locked-notice" style={{ justifyContent: 'center', textAlign: 'center' }}>
              <Lock size={13} />
              Seul le manager peut enregistrer la décision finale.
            </p>
            <div className="modal-footer">
              <button type="button" className="btn-outline" onClick={onClose}>Fermer</button>
            </div>
          </div>
        ) : (
          <form className="modal-body" onSubmit={handleSubmit}>
            <p className="modal-intro">Cette action est définitive et visible par toute l'équipe.</p>
            <div className="decision-final-btns">
              <button type="button"
                className={`decision-final-btn${finalStatus === 'retenu' ? ' selected-green' : ''}`}
                onClick={() => setFinalStatus('retenu')}
              >
                <CheckCircle size={14} /> Retenu
              </button>
              <button type="button"
                className={`decision-final-btn${finalStatus === 'non_retenu' ? ' selected-red' : ''}`}
                onClick={() => setFinalStatus('non_retenu')}
              >
                <X size={14} /> Non retenu
              </button>
            </div>
            <textarea className="decision-comment-input"
              placeholder="Justification de la décision…"
              value={rationale} onChange={e => setRationale(e.target.value)}
              rows={4} required
            />
            {submitError && <p style={{ color: '#dc2626', fontSize: 12, margin: '4px 0 0' }}>{submitError}</p>}
            <div className="modal-footer">
              <button type="button" className="btn-outline" onClick={onClose}>Annuler</button>
              <button type="submit" className="btn-primary" disabled={submitting || !rationale.trim()}>
                {submitting ? 'Enregistrement…' : 'Valider la décision'}
              </button>
            </div>
          </form>
        )}
      </div>
    </>
  );
}

/* ── Vote collectif panel ────────────────────────────────── */
function CollectiveVotePanel({ applicationId, token, currentUserRole }) {
  const [voteStatus, setVoteStatus] = useState(null);
  const [loading,    setLoading]    = useState(true);
  const [casting,    setCasting]    = useState(false);
  const [error,      setError]      = useState(null);

  useEffect(() => {
    setLoading(true);
    api.get(`/api/applications/${applicationId}/collective-vote`, token)
      .then(d => { setVoteStatus(d); setLoading(false); })
      .catch(() => setLoading(false));
  }, [applicationId, token]);

  async function handleVote(choice) {
    if (casting) return;
    setCasting(true);
    setError(null);
    try {
      const updated = await api.post(
        `/api/applications/${applicationId}/collective-vote/${voteStatus.voteId}/ballot`,
        { choice }, token
      );
      setVoteStatus(v => ({ ...v, ...updated, currentUserHasVoted: true }));
    } catch (err) {
      setError(err?.message ?? 'Erreur lors du vote.');
    } finally {
      setCasting(false);
    }
  }

  if (loading) return null;
  if (!voteStatus?.hasVote || voteStatus.status === 'CLOSED') return null;

  const { round, closesAt, approves, rejects, totalVoted, eligibleCount,
          currentUserHasVoted, currentUserIsEligible } = voteStatus;

  const hoursLeft = Math.max(0, Math.round((new Date(closesAt) - Date.now()) / 3600000));
  const pct = eligibleCount > 0 ? Math.round((totalVoted / eligibleCount) * 100) : 0;

  return (
    <div className="collective-vote-panel">
      <div className="cvp-header">
        <div className="cvp-title">
          <span className="cvp-badge">{round === 1 ? 'Vote équipe' : 'Vote élargi'}</span>
          <span className="cvp-subtitle">
            {round === 1 ? 'Désaccord détecté — l\'équipe assignée est consultée' : 'Égalité — toute l\'entreprise vote'}
          </span>
        </div>
        <span className="cvp-timer"><Clock size={11} /> {hoursLeft}h restantes</span>
      </div>

      <div className="cvp-progress-row">
        <div className="cvp-progress-bar-wrap">
          <div className="cvp-progress-bar" style={{ width: `${pct}%` }} />
        </div>
        <span className="cvp-progress-label">{totalVoted}/{eligibleCount} votes</span>
      </div>

      <div className="cvp-counts">
        <span className="cvp-count approve">{approves} Pour</span>
        <span className="cvp-count reject">{rejects} Contre</span>
      </div>

      {currentUserIsEligible && !currentUserHasVoted && (
        <div className="cvp-actions">
          <p className="cvp-prompt">Votre vote est attendu :</p>
          <div className="cvp-btns">
            <button className="cvp-btn approve" onClick={() => handleVote('APPROVE')} disabled={casting}>
              <CheckCircle size={13} /> Pour
            </button>
            <button className="cvp-btn reject" onClick={() => handleVote('REJECT')} disabled={casting}>
              <X size={13} /> Contre
            </button>
          </div>
          {error && <p style={{ color: '#dc2626', fontSize: 11, marginTop: 4 }}>{error}</p>}
        </div>
      )}

      {currentUserHasVoted && (
        <p className="cvp-voted-notice"><CheckCircle size={11} /> Votre vote a été enregistré anonymement.</p>
      )}

      {!currentUserIsEligible && (
        <p className="cvp-voted-notice" style={{ color: '#6b7280' }}>
          Vous n'êtes pas assigné à cette offre{round === 2 ? '' : ' (vote round 1 réservé aux membres assignés)'}.
        </p>
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
  const [interviews,    setInterviews]    = useState([]);
  const [loading,       setLoading]       = useState(true);
  const [showForm,      setShowForm]      = useState(false);
  const [saving,        setSaving]        = useState(false);
  const [completingId,  setCompletingId]  = useState(null); // interview en cours de clôture
  const [completNotes,  setCompletNotes]  = useState('');
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
      if (['nouveau', 'en_etude', 'en_attente_avis'].includes(currentStatusCode)) {
        onStatusChange?.('entretien');
      }
    } catch { /* silent */ } finally {
      setSaving(false);
    }
  }

  async function handleUpdateStatus(interviewId, status, notes) {
    try {
      const body = { status };
      if (notes) body.notes = notes;
      const updated = await api.patch(`/api/applications/${applicationId}/interviews/${interviewId}/status`, body, token);
      setInterviews(p => p.map(i => i.interviewId === interviewId ? updated : i));
    } catch { /* silent */ }
  }

  async function handleComplete(e) {
    e.preventDefault();
    await handleUpdateStatus(completingId, 'COMPLETED', completNotes);
    setCompletingId(null);
    setCompletNotes('');
  }

  if (loading) return <div className="tab-empty">Chargement…</div>;

  const isTerminal = ['non_retenu', 'vivier', 'embauche'].includes(currentStatusCode);

  return (
    <div className="entretiens-tab">
      {isTerminal ? (
        <p className="avis-locked-notice" style={{ marginBottom: 12 }}>
          <CheckCircle size={13} />
          {currentStatusCode === 'non_retenu' && 'Ce candidat est marqué non retenu — la planification d\'entretien est désactivée.'}
          {currentStatusCode === 'vivier' && 'Ce candidat est dans le vivier — rouvrez le dossier pour planifier un entretien.'}
          {currentStatusCode === 'embauche' && 'Ce candidat a été embauché.'}
        </p>
      ) : (
        <div className="entretiens-header">
          <button className="decision-submit-btn primary" onClick={() => setShowForm(v => !v)}>
            <Calendar size={13} /> {showForm ? 'Annuler' : 'Planifier un entretien'}
          </button>
        </div>
      )}

      {showForm && !isTerminal && (
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
            <label>Notes préparatoires</label>
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
                {iv.notes && iv.status !== 'SCHEDULED' && (
                  <div className="entretien-notes">
                    <FileText size={11} style={{ flexShrink: 0, marginTop: 1 }} />
                    <span>{iv.notes}</span>
                  </div>
                )}
                {iv.status === 'SCHEDULED' && completingId !== iv.interviewId && (
                  <div className="entretien-actions">
                    <button className="job-quick-btn green" onClick={() => { setCompletingId(iv.interviewId); setCompletNotes(''); }}>
                      <CheckCircle size={11} /> Terminer
                    </button>
                    <button className="job-quick-btn" onClick={() => handleUpdateStatus(iv.interviewId, 'CANCELLED')}>
                      <X size={11} /> Annuler
                    </button>
                  </div>
                )}
                {iv.status === 'SCHEDULED' && completingId === iv.interviewId && (
                  <form className="entretien-complete-form" onSubmit={handleComplete}>
                    <textarea
                      value={completNotes}
                      onChange={e => setCompletNotes(e.target.value)}
                      rows={3}
                      placeholder="Notes post-entretien (impressions, points clés, suite…)"
                      autoFocus
                    />
                    <div className="entretien-actions">
                      <button type="submit" className="job-quick-btn green"><CheckCircle size={11} /> Confirmer</button>
                      <button type="button" className="job-quick-btn" onClick={() => setCompletingId(null)}><X size={11} /> Annuler</button>
                    </div>
                  </form>
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
  INTERVIEW_COMPLETED:     { icon: CheckCircle,    color: '#059669', label: 'Entretien terminé'    },
  INTERVIEW_CANCELLED:     { icon: X,              color: '#dc2626', label: 'Entretien annulé'     },
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
    case 'INTERVIEW_SCHEDULED':
      return p.title ? `${p.title}${p.scheduledAt ? ' — ' + new Date(p.scheduledAt).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : ''}` : null;
    case 'INTERVIEW_COMPLETED':
      return p.notes ? `Notes : ${p.notes.slice(0, 80)}${p.notes.length > 80 ? '…' : ''}` : null;
    case 'INTERVIEW_CANCELLED':
      return p.title ?? null;
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

