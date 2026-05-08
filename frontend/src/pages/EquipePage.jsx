import React, { useEffect, useState } from 'react';
import { UserPlus, Trash2, Mail, Phone, Clock, CheckCircle, XCircle, AlertCircle, Building2, Plus, Search, Users } from 'lucide-react';
import { api } from '../lib/api';
import { useAuth } from '../context/AuthContext';

const ROLE_STYLE = {
  ADMIN:     { label: 'Admin',     bg: '#ede9fe', color: '#7c3aed' },
  RECRUITER: { label: 'Recruteur', bg: '#dbeafe', color: '#1d4ed8' },
  VIEWER:    { label: 'Lecteur',   bg: '#f1f5f9', color: '#475569' },
};

const INV_STATUS = {
  PENDING:   { label: 'En attente', icon: Clock,        color: '#d97706' },
  ACCEPTED:  { label: 'Acceptée',   icon: CheckCircle,  color: '#059669' },
  EXPIRED:   { label: 'Expirée',    icon: AlertCircle,  color: '#9ca3af' },
  CANCELLED: { label: 'Annulée',    icon: XCircle,      color: '#dc2626' },
};

function RoleBadge({ role }) {
  const s = ROLE_STYLE[role] ?? { label: role ?? '—', bg: '#f1f5f9', color: '#6b7280' };
  return (
    <span style={{ background: s.bg, color: s.color, padding: '2px 9px', borderRadius: 20, fontSize: 11, fontWeight: 700 }}>
      {s.label}
    </span>
  );
}

function avatarInitials(name, email) {
  if (name) return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
  if (email) return email[0].toUpperCase();
  return '?';
}

function timeAgo(date) {
  if (!date) return '';
  const diff = Date.now() - new Date(date).getTime();
  const days = Math.floor(diff / 86400000);
  if (days === 0) return "aujourd'hui";
  if (days === 1) return 'hier';
  return `il y a ${days}j`;
}

const TABS = ['Membres', 'Invitations', 'Départements'];
const EMPTY_FORM = { email: '', role: 'RECRUITER' };

export default function EquipePage() {
  const { session } = useAuth();
  const token = session?.token;

  const [recruiters,   setRecruiters]   = useState([]);
  const [invitations,  setInvitations]  = useState([]);
  const [loading,      setLoading]      = useState(true);
  const [activeTab,    setActiveTab]    = useState('Membres');
  const [showDrawer,   setShowDrawer]   = useState(false);
  const [form,         setForm]         = useState(EMPTY_FORM);
  const [saving,       setSaving]       = useState(false);
  const [formError,    setFormError]    = useState('');
  const [departments,  setDepartments]  = useState([]);
  const [deptForm,     setDeptForm]     = useState({ name: '', description: '' });
  const [deptSaving,   setDeptSaving]   = useState(false);
  const [showDeptForm, setShowDeptForm] = useState(false);
  const [pool,         setPool]         = useState([]);
  const [poolSearch,   setPoolSearch]   = useState('');
  const [poolLoading,  setPoolLoading]  = useState(false);

  useEffect(() => { load(); }, []);

  async function load() {
    setLoading(true);
    const [recs, invs] = await Promise.all([
      api.get('/api/recruiters',                  token).catch(() => []),
      api.get('/api/company-members/invitations', token).catch(() => []),
    ]);
    setRecruiters(Array.isArray(recs) ? recs : []);
    setInvitations(Array.isArray(invs) ? invs : []);
    setLoading(false);
  }

  useEffect(() => {
    if (activeTab === 'Départements' && departments.length === 0) {
      api.get('/api/departments', token).catch(() => []).then(d => setDepartments(Array.isArray(d) ? d : []));
    }
    if (activeTab === 'Vivier') {
      loadPool();
    }
  }, [activeTab]);

  async function loadPool(search = poolSearch) {
    setPoolLoading(true);
    try {
      const url = search.trim()
        ? `/api/pool?query=${encodeURIComponent(search.trim())}`
        : '/api/pool';
      const data = await api.get(url, token).catch(() => []);
      setPool(Array.isArray(data) ? data : []);
    } finally {
      setPoolLoading(false);
    }
  }

  async function handleCreateDept(e) {
    e.preventDefault();
    setDeptSaving(true);
    try {
      const created = await api.post('/api/departments', deptForm, token);
      setDepartments(p => [...p, created]);
      setDeptForm({ name: '', description: '' });
      setShowDeptForm(false);
    } catch { /* silent */ } finally {
      setDeptSaving(false);
    }
  }

  async function handleDeleteDept(id) {
    if (!confirm('Supprimer ce département ?')) return;
    await api.del(`/api/departments/${id}`, token).catch(() => {});
    setDepartments(p => p.filter(d => d.id !== id));
  }

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));

  async function handleInvite(e) {
    e.preventDefault();
    if (!form.email || !form.role) { setFormError('Email et rôle sont obligatoires.'); return; }
    setSaving(true); setFormError('');
    try {
      const created = await api.post('/api/company-members/invitations', form, token);
      setInvitations(p => [created, ...p]);
      setShowDrawer(false);
      setActiveTab('Invitations');
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteRecruiter(rec) {
    if (!confirm(`Retirer ${rec.name ?? rec.email} de l'équipe ?`)) return;
    await api.del(`/api/recruiters/${rec.recruiterId}`, token).catch(() => {});
    setRecruiters(p => p.filter(r => r.recruiterId !== rec.recruiterId));
  }

  async function handleCancelInvitation(inv) {
    if (!confirm(`Annuler l'invitation pour ${inv.email} ?`)) return;
    await api.del(`/api/company-members/invitations/${inv.invitationId}`, token).catch(() => {});
    setInvitations(p => p.filter(i => i.invitationId !== inv.invitationId));
  }

  const pendingCount = invitations.filter(i => i.status === 'PENDING').length;

  return (
    <div className="equipe-page">

      {/* Toolbar */}
      <div className="page-toolbar">
        <div className="filter-tabs">
          {TABS.map(tab => (
            <button
              key={tab}
              className={`filter-tab${activeTab === tab ? ' active' : ''}`}
              onClick={() => setActiveTab(tab)}
            >
              {tab}
              {tab === 'Membres'     && <span className="filter-tab-count">{recruiters.length}</span>}
              {tab === 'Invitations' && <span className="filter-tab-count">{pendingCount}</span>}
              {tab === 'Départements' && departments.length > 0 && <span className="filter-tab-count">{departments.length}</span>}
            </button>
          ))}
        </div>
        <button className="btn-primary" onClick={() => { setForm(EMPTY_FORM); setFormError(''); setShowDrawer(true); }}>
          <UserPlus size={16} strokeWidth={2} /> Inviter
        </button>
      </div>

      {loading ? (
        <p className="panel-empty" style={{ marginTop: 32 }}>Chargement…</p>
      ) : (
        <>
          {/* ── Membres ── */}
          {activeTab === 'Membres' && (
            recruiters.length === 0 ? (
              <div className="empty-state">
                <div className="empty-state-icon"><UserPlus size={32} strokeWidth={1.5} /></div>
                <p className="empty-state-title">Aucun membre</p>
                <p className="empty-state-hint">Invitez des recruteurs pour collaborer sur vos offres.</p>
              </div>
            ) : (
              <div className="member-grid">
                {recruiters.map(rec => (
                  <div key={rec.recruiterId} className="member-card">
                    <div className="member-card-top">
                      <div className="member-avatar">{avatarInitials(rec.name, rec.email)}</div>
                      <button className="icon-btn danger" onClick={() => handleDeleteRecruiter(rec)} title="Retirer">
                        <Trash2 size={14} />
                      </button>
                    </div>

                    <h3 className="member-name">{rec.name ?? '—'}</h3>

                    <div className="member-meta">
                      {rec.email && (
                        <span className="member-meta-row"><Mail size={12} /> {rec.email}</span>
                      )}
                      {rec.phone && (
                        <span className="member-meta-row"><Phone size={12} /> {rec.phone}</span>
                      )}
                    </div>

                    {rec.bio && <p className="member-bio">{rec.bio.length > 80 ? rec.bio.slice(0, 80) + '…' : rec.bio}</p>}

                    <div className="member-card-footer">
                      <RoleBadge role={rec.role} />
                      {rec.status && (
                        <span className={`member-status ${rec.status?.toLowerCase()}`}>{rec.status}</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )
          )}

          {/* ── Départements ── */}
          {activeTab === 'Départements' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <button className="btn-primary" onClick={() => setShowDeptForm(v => !v)}>
                  <Plus size={15} /> Nouveau département
                </button>
              </div>
              {showDeptForm && (
                <form style={{ display: 'flex', flexDirection: 'column', gap: 10, padding: 16, border: '1px solid #e5e7eb', borderRadius: 10, background: '#f8fafc' }} onSubmit={handleCreateDept}>
                  <div className="form-field">
                    <label>Nom *</label>
                    <input value={deptForm.name} onChange={e => setDeptForm(f => ({...f, name: e.target.value}))} placeholder="Ex: Produit, Ingénierie…" required />
                  </div>
                  <div className="form-field">
                    <label>Description</label>
                    <input value={deptForm.description} onChange={e => setDeptForm(f => ({...f, description: e.target.value}))} placeholder="Optionnel" />
                  </div>
                  <button type="submit" className="btn-primary" disabled={deptSaving} style={{ alignSelf: 'flex-end' }}>
                    {deptSaving ? 'Création…' : 'Créer'}
                  </button>
                </form>
              )}
              {departments.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-icon"><Building2 size={32} strokeWidth={1.5} /></div>
                  <p className="empty-state-title">Aucun département</p>
                  <p className="empty-state-hint">Organisez votre équipe par département.</p>
                </div>
              ) : (
                <div className="member-grid">
                  {departments.map(d => (
                    <div key={d.id} className="member-card">
                      <div className="member-card-top">
                        <div className="member-avatar" style={{ background: '#4338ca' }}><Building2 size={18} /></div>
                        <button className="icon-btn danger" onClick={() => handleDeleteDept(d.id)}><Trash2 size={14} /></button>
                      </div>
                      <h3 className="member-name">{d.name}</h3>
                      {d.description && <p className="member-bio">{d.description}</p>}
                      {d.recruiters?.length > 0 && (
                        <div className="member-meta">
                          <span className="member-meta-row"><Users size={12} /> {d.recruiters.length} membre{d.recruiters.length > 1 ? 's' : ''}</span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* ── Vivier ── */}
          {activeTab === 'Vivier' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div style={{ display: 'flex', gap: 8 }}>
                <div style={{ flex: 1, position: 'relative' }}>
                  <Search size={14} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: '#9ca3af' }} />
                  <input
                    style={{ width: '100%', paddingLeft: 32, paddingRight: 10, height: 36, border: '1px solid #e2e8f0', borderRadius: 8, fontSize: 13, outline: 'none', boxSizing: 'border-box' }}
                    placeholder="Rechercher par compétence, ville…"
                    value={poolSearch}
                    onChange={e => setPoolSearch(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && loadPool(e.target.value)}
                  />
                </div>
                <button className="btn-primary" onClick={() => loadPool(poolSearch)}>Rechercher</button>
              </div>
              {poolLoading ? <p className="panel-empty">Chargement…</p>
              : pool.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-icon"><Users size={32} strokeWidth={1.5} /></div>
                  <p className="empty-state-title">Vivier vide</p>
                  <p className="empty-state-hint">Les candidats ajoutés au vivier apparaissent ici.</p>
                </div>
              ) : (
                <div className="member-grid">
                  {pool.map((c, i) => (
                    <div key={c.candidateId ?? i} className="member-card">
                      <div className="member-card-top">
                        <div className="member-avatar">
                          {(c.firstName?.[0] ?? '').toUpperCase()}{(c.lastName?.[0] ?? '').toUpperCase()}
                        </div>
                      </div>
                      <h3 className="member-name">{c.firstName} {c.lastName}</h3>
                      <div className="member-meta">
                        {c.email && <span className="member-meta-row"><Mail size={12} /> {c.email}</span>}
                        {c.location && <span className="member-meta-row">{c.location}</span>}
                      </div>
                      {c.school && <p className="member-bio">{c.school}</p>}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* ── Invitations ── */}
          {activeTab === 'Invitations' && (
            invitations.length === 0 ? (
              <div className="empty-state">
                <div className="empty-state-icon"><Mail size={32} strokeWidth={1.5} /></div>
                <p className="empty-state-title">Aucune invitation</p>
                <p className="empty-state-hint">Cliquez sur "Inviter" pour envoyer une invitation par e-mail.</p>
              </div>
            ) : (
              <div className="inv-list">
                {invitations.map(inv => {
                  const s = INV_STATUS[inv.status] ?? INV_STATUS.PENDING;
                  const StatusIcon = s.icon;
                  return (
                    <div key={inv.invitationId} className="inv-row">
                      <div className="inv-avatar">{(inv.email?.[0] ?? '?').toUpperCase()}</div>

                      <div className="inv-body">
                        <span className="inv-email">{inv.email}</span>
                        <div className="inv-sub">
                          <RoleBadge role={inv.role} />
                          <span className="inv-date">{timeAgo(inv.createdAt)}</span>
                        </div>
                      </div>

                      <div className="inv-status-wrap" style={{ color: s.color }}>
                        <StatusIcon size={14} />
                        <span>{s.label}</span>
                      </div>

                      {inv.status === 'PENDING' && (
                        <button className="icon-btn danger" onClick={() => handleCancelInvitation(inv)} title="Annuler">
                          <XCircle size={15} />
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            )
          )}
        </>
      )}

      {/* Drawer */}
      {showDrawer && (
        <>
          <div className="drawer-overlay" onClick={() => setShowDrawer(false)} />
          <aside className="drawer">
            <div className="drawer-header">
              <h3>Inviter un recruteur</h3>
              <button className="icon-btn" onClick={() => setShowDrawer(false)}>✕</button>
            </div>

            <form className="drawer-form" onSubmit={handleInvite}>
              <div className="form-field">
                <label>Adresse e-mail *</label>
                <input
                  type="email"
                  value={form.email}
                  onChange={set('email')}
                  placeholder="nom@entreprise.com"
                  required
                />
              </div>

              <div className="form-field">
                <label>Rôle *</label>
                <select value={form.role} onChange={set('role')}>
                  <option value="RECRUITER">Recruteur</option>
                  <option value="ADMIN">Admin</option>
                  <option value="VIEWER">Lecteur</option>
                </select>
              </div>

              {formError && <p className="auth-error">{formError}</p>}

              <div className="drawer-footer">
                <button type="button" className="btn-outline" onClick={() => setShowDrawer(false)}>Annuler</button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? 'Envoi…' : 'Envoyer l\'invitation'}
                </button>
              </div>
            </form>
          </aside>
        </>
      )}
    </div>
  );
}
