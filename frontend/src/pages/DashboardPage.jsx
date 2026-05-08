import React, { useEffect, useState } from 'react';
import { Users, CheckSquare, Briefcase, TrendingUp, Clock, ChevronRight, Bell, AlertCircle } from 'lucide-react';
import { api } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

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

const PIPELINE_STAGES = [
  { code: 'nouveau',         label: 'Nouveau',     color: '#2563eb' },
  { code: 'en_etude',        label: 'En étude',    color: '#7c3aed' },
  { code: 'en_attente_avis', label: 'Avis équipe', color: '#d97706' },
  { code: 'entretien',       label: 'Entretien',   color: '#0891b2' },
  { code: 'retenu',          label: 'Retenu',      color: '#059669' },
  { code: 'embauche',        label: 'Embauché',    color: '#047857' },
];

export default function DashboardPage() {
  const { session } = useAuth();
  const token = session?.token;
  const navigate = useNavigate();

  const [apps,    setApps]    = useState([]);
  const [tasks,   setTasks]   = useState([]);
  const [jobs,    setJobs]    = useState([]);
  const [notifs,  setNotifs]  = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      const [appsRes, tasksRes, jobsRes, notifsRes] = await Promise.all([
        api.get('/api/applications', token).catch(() => []),
        api.get('/api/tasks/mine',   token).catch(() => []),
        api.get('/api/jobs',         token).catch(() => []),
        api.get('/api/internal-notifications/me', token).catch(() => null),
      ]);
      setApps(Array.isArray(appsRes)  ? appsRes  : []);
      setTasks(Array.isArray(tasksRes) ? tasksRes : []);
      setJobs(Array.isArray(jobsRes)  ? jobsRes  : []);
      setNotifs(notifsRes?.notifications ?? []);
      setLoading(false);
    }
    load();
  }, [token]);

  const totalApps  = apps.length;
  const todoTasks  = tasks.filter(t => t.status === 'TODO').length;
  const urgentTasks = tasks.filter(t => t.overdue || t.priority === 'URGENT').length;
  const activeJobs = jobs.filter(j => j.statut === 'ouvert').length;
  const retainedApps = apps.filter(a => ['retenu', 'embauche'].includes(a.status?.code)).length;

  /* Pipeline funnel */
  const pipelineCounts = PIPELINE_STAGES.map(s => ({
    ...s,
    count: apps.filter(a => a.status?.code === s.code).length,
  }));
  const maxCount = Math.max(...pipelineCounts.map(s => s.count), 1);

  /* Recent apps */
  const recentApps = [...apps]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 6);

  /* Urgent tasks */
  const urgentTaskList = tasks.filter(t => (t.overdue || t.priority === 'URGENT') && t.status !== 'DONE').slice(0, 4);

  /* Active jobs */
  const activeJobList = jobs.filter(j => j.statut === 'ouvert').slice(0, 4);

  const STATS = [
    { icon: Users,       label: 'Candidatures totales', value: totalApps,   color: '#4f46e5', bg: '#eef2ff', link: '/candidatures' },
    { icon: TrendingUp,  label: 'Retenus / Embauchés',  value: retainedApps, color: '#059669', bg: '#d1fae5', link: '/candidatures' },
    { icon: CheckSquare, label: 'Tâches à faire',        value: todoTasks,   color: '#d97706', bg: '#fef3c7', link: '/taches'       },
    { icon: Briefcase,   label: 'Offres actives',        value: activeJobs,  color: '#0284c7', bg: '#e0f2fe', link: '/offres'       },
  ];

  return (
    <div className="dashboard">
      {/* Stats row */}
      <div className="dashboard-stats">
        {STATS.map(({ icon: Icon, label, value, color, bg, link }) => (
          <div key={label} className="stat-card" style={{ cursor: 'pointer' }} onClick={() => navigate(link)}>
            <div className="stat-icon" style={{ background: bg, color }}>
              <Icon size={22} strokeWidth={1.8} />
            </div>
            <div className="stat-body">
              <span className="stat-value" style={{ color }}>{loading ? '…' : value}</span>
              <span className="stat-label">{label}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Pipeline funnel */}
      <section className="panel" style={{ marginBottom: 24 }}>
        <h3 className="panel-title">Pipeline de recrutement</h3>
        {loading ? <p className="panel-empty">Chargement…</p> : (
          <div className="pipeline-funnel">
            {pipelineCounts.map(stage => (
              <div key={stage.code} className="funnel-stage" onClick={() => navigate(`/candidatures`)}>
                <div className="funnel-bar-wrap">
                  <div
                    className="funnel-bar"
                    style={{
                      height: Math.max(4, (stage.count / maxCount) * 80),
                      background: stage.color,
                    }}
                  />
                </div>
                <span className="funnel-count" style={{ color: stage.color }}>{stage.count}</span>
                <span className="funnel-label">{stage.label}</span>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Panels row */}
      <div className="dashboard-panels">
        {/* Recent applications */}
        <section className="panel">
          <div className="panel-header">
            <h3 className="panel-title" style={{ margin: 0 }}>Candidatures récentes</h3>
            <button className="panel-link-btn" onClick={() => navigate('/candidatures')}>
              Voir tout <ChevronRight size={13} />
            </button>
          </div>
          {loading ? <p className="panel-empty">Chargement…</p>
          : recentApps.length === 0 ? <p className="panel-empty">Aucune candidature.</p>
          : (
            <ul className="app-list">
              {recentApps.map(app => (
                <li key={app.applicationId} className="app-row" style={{ cursor: 'pointer' }} onClick={() => navigate('/candidatures')}>
                  <div className="app-row-avatar">
                    {app.candidate?.firstName?.[0]?.toUpperCase() ?? '?'}
                  </div>
                  <div className="app-row-body">
                    <span className="app-row-name">{app.candidate?.firstName} {app.candidate?.lastName}</span>
                    <span className="app-row-job">{app.job?.title ?? '—'}</span>
                  </div>
                  <div className="app-row-meta">
                    {app.status && (
                      <span style={{ padding: '2px 8px', borderRadius: 6, fontSize: 11, fontWeight: 700, background: '#f1f5f9', color: '#64748b' }}>
                        {app.status.label ?? app.status.code}
                      </span>
                    )}
                    <span className="app-row-time">{timeAgo(app.createdAt)}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Right column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Urgent tasks */}
          <section className="panel">
            <div className="panel-header">
              <h3 className="panel-title" style={{ margin: 0 }}>
                Tâches urgentes
                {urgentTasks > 0 && (
                  <span style={{ marginLeft: 8, background: '#fef2f2', color: '#dc2626', padding: '1px 7px', borderRadius: 20, fontSize: 11, fontWeight: 700 }}>
                    {urgentTasks}
                  </span>
                )}
              </h3>
              <button className="panel-link-btn" onClick={() => navigate('/taches')}>
                Voir tout <ChevronRight size={13} />
              </button>
            </div>
            {loading ? <p className="panel-empty">Chargement…</p>
            : urgentTaskList.length === 0 ? <p className="panel-empty">Aucune tâche urgente.</p>
            : (
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 6 }}>
                {urgentTaskList.map(t => (
                  <li key={t.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0', borderBottom: '1px solid #f1f5f9' }}>
                    {t.overdue ? <AlertCircle size={13} color="#dc2626" /> : <Clock size={13} color="#d97706" />}
                    <span style={{ flex: 1, fontSize: 13, color: '#111827' }}>{t.title}</span>
                    {t.priority === 'URGENT' && (
                      <span style={{ background: '#fef2f2', color: '#dc2626', padding: '1px 6px', borderRadius: 4, fontSize: 10, fontWeight: 700 }}>Urgent</span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>

          {/* Active offers */}
          <section className="panel">
            <div className="panel-header">
              <h3 className="panel-title" style={{ margin: 0 }}>Offres actives</h3>
              <button className="panel-link-btn" onClick={() => navigate('/offres')}>
                Voir tout <ChevronRight size={13} />
              </button>
            </div>
            {loading ? <p className="panel-empty">Chargement…</p>
            : activeJobList.length === 0 ? <p className="panel-empty">Aucune offre ouverte.</p>
            : (
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 6 }}>
                {activeJobList.map(j => (
                  <li key={j.jobId} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0', borderBottom: '1px solid #f1f5f9' }}>
                    <Briefcase size={13} color="#0284c7" />
                    <div style={{ flex: 1 }}>
                      <p style={{ margin: 0, fontSize: 13, fontWeight: 600, color: '#111827' }}>{j.title}</p>
                      {j.location && <p style={{ margin: 0, fontSize: 11, color: '#6b7280' }}>{j.location}</p>}
                    </div>
                    <button className="panel-link-btn" onClick={() => navigate(`/candidatures?jobId=${j.jobId}`)}>
                      <Users size={11} />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {/* Notifications */}
          {notifs.filter(n => !n.read).length > 0 && (
            <section className="panel">
              <div className="panel-header">
                <h3 className="panel-title" style={{ margin: 0 }}>
                  <Bell size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                  Notifications non lues
                </h3>
              </div>
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 4 }}>
                {notifs.filter(n => !n.read).slice(0, 4).map((n, i) => (
                  <li key={n.id ?? i} style={{ fontSize: 12, color: '#374151', padding: '4px 0', borderBottom: '1px solid #f1f5f9' }}>
                    {n.message ?? n.title}
                    <span style={{ marginLeft: 6, fontSize: 11, color: '#9ca3af' }}>{timeAgo(n.createdAt)}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
