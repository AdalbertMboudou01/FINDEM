import React, { useEffect, useState } from 'react';
import { Search, Star, MapPin, GraduationCap, Mail, Phone, ArrowRight, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { useAuth } from '../context/AuthContext';

function timeAgo(date) {
  if (!date) return '';
  const days = Math.floor((Date.now() - new Date(date)) / 86400000);
  if (days === 0) return "aujourd'hui";
  if (days === 1) return 'hier';
  return `il y a ${days}j`;
}

export default function VivierPage() {
  const { session } = useAuth();
  const token       = session?.token;
  const navigate    = useNavigate();

  const [apps,     setApps]     = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [search,   setSearch]   = useState('');
  const [relancing, setRelancing] = useState({});

  useEffect(() => {
    api.get('/api/applications', token)
      .then(data => {
        const vivierApps = Array.isArray(data)
          ? data.filter(a => a.status?.code === 'vivier')
          : [];
        setApps(vivierApps);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [token]);

  const filtered = apps.filter(a => {
    if (!search) return true;
    const name = `${a.candidate?.firstName ?? ''} ${a.candidate?.lastName ?? ''} ${a.job?.title ?? ''}`.toLowerCase();
    return name.includes(search.toLowerCase());
  });

  async function handleRelance(app) {
    if (relancing[app.applicationId]) return;
    setRelancing(p => ({ ...p, [app.applicationId]: true }));
    try {
      await api.patch(`/api/applications/${app.applicationId}/status`, { statusCode: 'en_etude' }, token);
      setApps(p => p.filter(a => a.applicationId !== app.applicationId));
    } catch { /* silent */ } finally {
      setRelancing(p => ({ ...p, [app.applicationId]: false }));
    }
  }

  return (
    <div className="offres-page">
      <div className="page-toolbar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Star size={18} style={{ color: '#9333ea' }} />
          <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: '#111827' }}>Vivier</h2>
          {!loading && (
            <span style={{ background: '#f3e8ff', color: '#9333ea', borderRadius: 20, padding: '2px 10px', fontSize: 12, fontWeight: 700 }}>
              {apps.length} candidat{apps.length !== 1 ? 's' : ''}
            </span>
          )}
        </div>
        <div className="cand-search-bar" style={{ maxWidth: 280 }}>
          <Search size={14} className="cand-search-icon" />
          <input
            className="cand-search-input"
            placeholder="Rechercher…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
          {search && <button className="cand-search-clear" onClick={() => setSearch('')}><X size={13} /></button>}
        </div>
      </div>

      {loading ? (
        <p className="panel-empty" style={{ marginTop: 32 }}>Chargement…</p>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon"><Star size={32} strokeWidth={1.5} style={{ color: '#d8b4fe' }} /></div>
          <p className="empty-state-title">Vivier vide</p>
          <p className="empty-state-hint">
            {search ? 'Aucun candidat ne correspond à la recherche.' : 'Les candidats ajoutés au vivier depuis leurs dossiers apparaissent ici.'}
          </p>
        </div>
      ) : (
        <div className="job-grid">
          {filtered.map(app => {
            const c = app.candidate ?? {};
            const inits = ((c.firstName?.[0] ?? '') + (c.lastName?.[0] ?? '')).toUpperCase() || '?';
            return (
              <div key={app.applicationId} className="vivier-card">
                <div className="vivier-card-top">
                  <div className="vivier-avatar">{inits}</div>
                  <div className="vivier-identity">
                    <span className="vivier-name">{c.firstName} {c.lastName}</span>
                    <span className="vivier-job">{app.job?.title ?? '—'}</span>
                  </div>
                  <Star size={14} style={{ color: '#9333ea', flexShrink: 0 }} />
                </div>

                <div className="vivier-meta">
                  {c.email    && <span><Mail size={12} /> {c.email}</span>}
                  {c.phone    && <span><Phone size={12} /> {c.phone}</span>}
                  {c.location && <span><MapPin size={12} /> {c.location}</span>}
                  {c.school   && <span><GraduationCap size={12} /> {c.school}</span>}
                </div>

                <div className="vivier-footer">
                  <span className="vivier-date">Ajouté {timeAgo(app.updatedAt ?? app.createdAt)}</span>
                  <div style={{ display: 'flex', gap: 6 }}>
                    <button
                      className="btn-ghost"
                      onClick={() => navigate(`/candidatures?applicationId=${app.applicationId}`)}
                    >
                      Dossier
                    </button>
                    <button
                      className="btn-primary"
                      style={{ fontSize: 12, padding: '5px 12px' }}
                      onClick={() => handleRelance(app)}
                      disabled={relancing[app.applicationId]}
                    >
                      {relancing[app.applicationId] ? 'Relance…' : <><ArrowRight size={12} /> Relancer</>}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
