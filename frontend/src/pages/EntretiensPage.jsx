import React, { useEffect, useState } from 'react';
import { Calendar, MapPin, Link, Clock, User, Briefcase, Search, X, Video } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { useAuth } from '../context/AuthContext';

const STATUS_MAP = {
  SCHEDULED:   { label: 'Planifié',   bg: '#dbeafe', color: '#1d4ed8' },
  COMPLETED:   { label: 'Terminé',    bg: '#d1fae5', color: '#059669' },
  CANCELLED:   { label: 'Annulé',     bg: '#fee2e2', color: '#dc2626' },
  RESCHEDULED: { label: 'Reporté',    bg: '#fef3c7', color: '#d97706' },
};

function fmtDate(dt) {
  if (!dt) return '—';
  const d = new Date(dt);
  return d.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
}

function fmtTime(dt) {
  if (!dt) return '';
  const d = new Date(dt);
  return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

function StatusBadge({ status }) {
  const s = STATUS_MAP[status] ?? { label: status ?? '—', bg: '#f1f5f9', color: '#64748b' };
  return (
    <span style={{ background: s.bg, color: s.color, padding: '3px 10px', borderRadius: 20, fontSize: 12, fontWeight: 600 }}>
      {s.label}
    </span>
  );
}

const FILTERS = ['Tous', 'Planifié', 'Terminé', 'Annulé'];
const FILTER_CODES = { 'Tous': null, 'Planifié': 'SCHEDULED', 'Terminé': 'COMPLETED', 'Annulé': 'CANCELLED' };

export default function EntretiensPage() {
  const { session } = useAuth();
  const token = session?.token;
  const navigate = useNavigate();

  const [interviews, setInterviews] = useState([]);
  const [loading, setLoading]       = useState(true);
  const [search, setSearch]         = useState('');
  const [filter, setFilter]         = useState('Tous');

  useEffect(() => {
    api.get('/api/interviews', token)
      .then(data => { setInterviews(Array.isArray(data) ? data : []); setLoading(false); })
      .catch(() => setLoading(false));
  }, [token]);

  const filtered = interviews.filter(i => {
    const code = FILTER_CODES[filter];
    if (code && i.status !== code) return false;
    if (!search) return true;
    const hay = [
      i.title ?? '',
      i.candidate?.firstName ?? '',
      i.candidate?.lastName ?? '',
      i.job?.title ?? '',
      i.location ?? '',
    ].join(' ').toLowerCase();
    return hay.includes(search.toLowerCase());
  });

  const scheduled  = interviews.filter(i => i.status === 'SCHEDULED').length;
  const completed  = interviews.filter(i => i.status === 'COMPLETED').length;

  return (
    <div className="offres-page">
      <div className="page-toolbar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Calendar size={18} style={{ color: '#2563eb' }} />
          <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: '#111827' }}>Entretiens</h2>
          {!loading && (
            <span style={{ background: '#dbeafe', color: '#1d4ed8', borderRadius: 20, padding: '2px 10px', fontSize: 12, fontWeight: 700 }}>
              {scheduled} planifié{scheduled !== 1 ? 's' : ''}
            </span>
          )}
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <div className="cand-search-bar" style={{ maxWidth: 240 }}>
            <Search size={14} className="cand-search-icon" />
            <input
              className="cand-search-input"
              placeholder="Rechercher…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
            {search && <button className="cand-search-clear" onClick={() => setSearch('')}><X size={13} /></button>}
          </div>
          <div style={{ display: 'flex', gap: 4 }}>
            {FILTERS.map(f => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                style={{
                  padding: '5px 12px', borderRadius: 20, fontSize: 12, fontWeight: 600, border: 'none', cursor: 'pointer',
                  background: filter === f ? '#2563eb' : '#f1f5f9',
                  color: filter === f ? '#fff' : '#6b7280',
                }}
              >
                {f}
              </button>
            ))}
          </div>
        </div>
      </div>

      {loading ? (
        <p className="panel-empty" style={{ marginTop: 32 }}>Chargement…</p>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon"><Calendar size={32} strokeWidth={1.5} style={{ color: '#93c5fd' }} /></div>
          <p className="empty-state-title">Aucun entretien</p>
          <p className="empty-state-hint">
            {search || filter !== 'Tous' ? 'Aucun entretien ne correspond aux filtres.' : 'Les entretiens planifiés depuis les dossiers de candidature apparaissent ici.'}
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 4 }}>
          {filtered.map(interview => {
            const c    = interview.candidate ?? {};
            const job  = interview.job ?? {};
            const inits = ((c.firstName?.[0] ?? '') + (c.lastName?.[0] ?? '')).toUpperCase() || '?';
            return (
              <div key={interview.interviewId} className="entretien-card">
                <div className="entretien-card-left">
                  <div className="entretien-date-block">
                    <span className="entretien-day">
                      {interview.scheduledAt ? new Date(interview.scheduledAt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' }) : '—'}
                    </span>
                    <span className="entretien-time">
                      {interview.scheduledAt ? fmtTime(interview.scheduledAt) : ''}
                    </span>
                  </div>
                </div>

                <div className="entretien-card-body">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
                    <div className="vivier-avatar" style={{ width: 32, height: 32, fontSize: 12, flexShrink: 0 }}>{inits}</div>
                    <div>
                      <span style={{ fontWeight: 700, fontSize: 14, color: '#111827' }}>
                        {c.firstName} {c.lastName}
                      </span>
                      <span style={{ marginLeft: 8, fontSize: 12, color: '#6b7280' }}>
                        <Briefcase size={11} style={{ verticalAlign: 'middle', marginRight: 3 }} />
                        {job.title ?? '—'}
                      </span>
                    </div>
                    <div style={{ marginLeft: 'auto' }}>
                      <StatusBadge status={interview.status} />
                    </div>
                  </div>

                  <p style={{ margin: '0 0 6px', fontWeight: 600, fontSize: 13, color: '#374151' }}>
                    {interview.title}
                  </p>

                  <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap', fontSize: 12, color: '#6b7280' }}>
                    {interview.durationMinutes && (
                      <span><Clock size={12} style={{ verticalAlign: 'middle', marginRight: 3 }} />{interview.durationMinutes} min</span>
                    )}
                    {interview.location && (
                      <span><MapPin size={12} style={{ verticalAlign: 'middle', marginRight: 3 }} />{interview.location}</span>
                    )}
                    {interview.meetingUrl && (
                      <a href={interview.meetingUrl} target="_blank" rel="noopener noreferrer"
                         style={{ color: '#2563eb', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 3 }}>
                        <Video size={12} /> Rejoindre
                      </a>
                    )}
                  </div>

                  {interview.notes && (
                    <p style={{ margin: '6px 0 0', fontSize: 12, color: '#9ca3af', fontStyle: 'italic' }}>
                      {interview.notes}
                    </p>
                  )}
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', paddingLeft: 12 }}>
                  <button
                    className="btn-ghost"
                    style={{ fontSize: 12 }}
                    onClick={() => navigate(`/candidatures?candidateId=${c.candidateId}`)}
                  >
                    Dossier
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
