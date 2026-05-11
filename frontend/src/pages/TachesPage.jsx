import React, { useEffect, useState } from 'react';
import { Plus, Trash2, ChevronRight, ChevronLeft, AlertCircle, Calendar, Bot } from 'lucide-react';
import { api } from '../lib/api';
import { useAuth } from '../context/AuthContext';

const PRIORITY_STYLE = {
  LOW:    { label: 'Faible', bg: '#f0fdf4', color: '#16a34a' },
  MEDIUM: { label: 'Moyen',  bg: '#fefce8', color: '#ca8a04' },
  HIGH:   { label: 'Élevé',  bg: '#fff7ed', color: '#ea580c' },
  URGENT: { label: 'Urgent', bg: '#fef2f2', color: '#dc2626' },
};

const COLUMNS = [
  { key: 'TODO',        label: 'À faire',  color: '#6b7280' },
  { key: 'IN_PROGRESS', label: 'En cours', color: '#4854e8' },
  { key: 'DONE',        label: 'Terminé',  color: '#059669' },
];

const STATUS_FORWARD = { TODO: 'IN_PROGRESS', IN_PROGRESS: 'DONE' };
const STATUS_BACK    = { IN_PROGRESS: 'TODO', DONE: 'IN_PROGRESS' };

function formatDate(d) {
  if (!d) return null;
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
}

const EMPTY_FORM = { applicationId: '', title: '', description: '', priority: 'MEDIUM', dueDate: '', assigneeId: '' };

export default function TachesPage() {
  const { session } = useAuth();
  const token = session?.token;

  const [tasks,      setTasks]      = useState([]);
  const [apps,       setApps]       = useState([]);
  const [recruiters, setRecruiters] = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [filter,     setFilter]     = useState('all');
  const [showDrawer, setShowDrawer] = useState(false);
  const [form,       setForm]       = useState(EMPTY_FORM);
  const [saving,     setSaving]     = useState(false);
  const [formError,  setFormError]  = useState('');

  useEffect(() => { load(); }, []);

  async function load() {
    setLoading(true);
    const [tasksRes, appsRes, recruitersRes] = await Promise.all([
      api.get('/api/tasks/mine', token).catch(() => []),
      api.get('/api/applications', token).catch(() => []),
      api.get('/api/recruiters', token).catch(() => []),
    ]);
    setTasks(Array.isArray(tasksRes)     ? tasksRes     : []);
    setApps(Array.isArray(appsRes)       ? appsRes      : []);
    setRecruiters(Array.isArray(recruitersRes) ? recruitersRes : []);
    setLoading(false);
  }

  const visible  = filter === 'overdue' ? tasks.filter(t => t.overdue) : tasks;
  const byStatus = col => visible.filter(t => t.status === col);

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));

  function openDrawer() {
    setForm(EMPTY_FORM); setFormError(''); setShowDrawer(true);
  }

  async function handleCreate(e) {
    e.preventDefault();
    if (!form.applicationId || !form.title || !form.assigneeId) {
      setFormError('Candidature, titre et assigné sont obligatoires.'); return;
    }
    setSaving(true); setFormError('');
    try {
      const created = await api.post(
        `/api/applications/${form.applicationId}/tasks`,
        { title: form.title, description: form.description || null, priority: form.priority, dueDate: form.dueDate || null, assigneeId: form.assigneeId },
        token
      );
      setTasks(p => [created, ...p]);
      setShowDrawer(false);
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function moveStatus(task, newStatus) {
    const updated = await api.patch(`/api/tasks/${task.id}/status`, { status: newStatus }, token).catch(() => null);
    if (updated) setTasks(p => p.map(t => t.id === task.id ? updated : t));
  }

  async function handleDelete(task) {
    if (!confirm(`Supprimer "${task.title}" ?`)) return;
    await api.del(`/api/tasks/${task.id}`, token).catch(() => {});
    setTasks(p => p.filter(t => t.id !== task.id));
  }

  const overdueCount = tasks.filter(t => t.overdue).length;

  return (
    <div className="taches-page">

      {/* Toolbar */}
      <div className="page-toolbar">
        <div className="filter-tabs">
          <button className={`filter-tab${filter === 'all' ? ' active' : ''}`} onClick={() => setFilter('all')}>
            Toutes <span className="filter-tab-count">{tasks.length}</span>
          </button>
          <button className={`filter-tab${filter === 'overdue' ? ' active' : ''}`} onClick={() => setFilter('overdue')}>
            En retard <span className="filter-tab-count">{overdueCount}</span>
          </button>
        </div>
        <button className="btn-primary" onClick={openDrawer}>
          <Plus size={16} strokeWidth={2.5} /> Nouvelle tâche
        </button>
      </div>

      {/* Kanban */}
      {loading ? (
        <p className="panel-empty" style={{ marginTop: 32 }}>Chargement…</p>
      ) : (
        <div className="kanban">
          {COLUMNS.map(col => {
            const colTasks = byStatus(col.key);
            return (
              <div key={col.key} className="kanban-col">
                <div className="kanban-col-header">
                  <span className="kanban-col-dot" style={{ background: col.color }} />
                  <span className="kanban-col-label">{col.label}</span>
                  <span className="kanban-col-count">{colTasks.length}</span>
                </div>

                <div className="kanban-col-body">
                  {colTasks.length === 0 ? (
                    <p className="kanban-empty">Aucune tâche</p>
                  ) : colTasks.map(task => (
                    <TaskCard
                      key={task.id}
                      task={task}
                      apps={apps}
                      recruiters={recruiters}
                      onForward={STATUS_FORWARD[task.status] ? () => moveStatus(task, STATUS_FORWARD[task.status]) : null}
                      onBack={STATUS_BACK[task.status]        ? () => moveStatus(task, STATUS_BACK[task.status])    : null}
                      onDelete={() => handleDelete(task)}
                    />
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Drawer création */}
      {showDrawer && (
        <>
          <div className="drawer-overlay" onClick={() => setShowDrawer(false)} />
          <aside className="drawer">
            <div className="drawer-header">
              <h3>Nouvelle tâche</h3>
              <button className="icon-btn" onClick={() => setShowDrawer(false)}>✕</button>
            </div>

            <form className="drawer-form" onSubmit={handleCreate}>
              <div className="form-field">
                <label>Candidature *</label>
                <select value={form.applicationId} onChange={set('applicationId')} required>
                  <option value="">— Sélectionner —</option>
                  {apps.map(a => (
                    <option key={a.applicationId} value={a.applicationId}>
                      {a.candidate?.firstName} {a.candidate?.lastName} — {a.job?.title ?? '?'}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-field">
                <label>Titre *</label>
                <input value={form.title} onChange={set('title')} placeholder="Ex: Planifier l'entretien" required />
              </div>

              <div className="form-field">
                <label>Description</label>
                <textarea value={form.description} onChange={set('description')} placeholder="Détails…" rows={3} />
              </div>

              <div className="form-field">
                <label>Assigné à *</label>
                <select value={form.assigneeId} onChange={set('assigneeId')} required>
                  <option value="">— Sélectionner un membre —</option>
                  {recruiters.map(r => (
                    <option key={r.recruiterId} value={r.recruiterId}>
                      {r.name ?? r.email}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-row-2">
                <div className="form-field">
                  <label>Priorité</label>
                  <select value={form.priority} onChange={set('priority')}>
                    <option value="LOW">Faible</option>
                    <option value="MEDIUM">Moyen</option>
                    <option value="HIGH">Élevé</option>
                    <option value="URGENT">Urgent</option>
                  </select>
                </div>
                <div className="form-field">
                  <label>Échéance</label>
                  <input type="date" value={form.dueDate} onChange={set('dueDate')} />
                </div>
              </div>

              {formError && <p className="auth-error">{formError}</p>}

              <div className="drawer-footer">
                <button type="button" className="btn-outline" onClick={() => setShowDrawer(false)}>Annuler</button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? 'Création…' : 'Créer la tâche'}
                </button>
              </div>
            </form>
          </aside>
        </>
      )}
    </div>
  );
}

function TaskCard({ task, apps, recruiters, onForward, onBack, onDelete }) {
  const p        = PRIORITY_STYLE[task.priority] ?? PRIORITY_STYLE.MEDIUM;
  const app      = apps.find(a => String(a.applicationId) === String(task.applicationId));
  const name     = app ? `${app.candidate?.firstName ?? ''} ${app.candidate?.lastName ?? ''}`.trim() : null;
  const assignee = recruiters?.find(r => String(r.recruiterId) === String(task.assigneeId));
  const assigneeName = assignee ? (assignee.name ?? assignee.email) : null;

  return (
    <div className={`task-card${task.overdue ? ' overdue' : ''}`}>
      <div className="task-card-top">
        <span className="priority-badge" style={{ background: p.bg, color: p.color }}>{p.label}</span>
        {task.overdue && <AlertCircle size={13} className="overdue-icon" />}
      </div>

      <p className="task-card-title">{task.title}</p>

      {task.description && (
        <p className="task-card-desc">
          {task.description.length > 80 ? task.description.slice(0, 80) + '…' : task.description}
        </p>
      )}

      {task.aiResult && (
        <div className="task-ai-result">
          <Bot size={11} style={{ color: '#6366f1', flexShrink: 0 }} />
          <span>{task.aiResult.length > 100 ? task.aiResult.slice(0, 100) + '…' : task.aiResult}</span>
        </div>
      )}

      <div className="task-card-footer">
        {task.dueDate && (
          <span className={`task-due${task.overdue ? ' task-due-late' : ''}`}>
            <Calendar size={11} /> {formatDate(task.dueDate)}
          </span>
        )}
        {name && <span className="task-candidate">{name}</span>}
        {assigneeName && (
          <span className="task-assignee" title={`Assigné à ${assigneeName}`}>
            {assigneeName.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()}
          </span>
        )}
      </div>

      <div className="task-card-actions">
        {onBack    && <button className="task-action-btn"         onClick={onBack}    title="Reculer" ><ChevronLeft  size={14} /></button>}
        {onForward && <button className="task-action-btn primary" onClick={onForward} title="Avancer"><ChevronRight size={14} /></button>}
        <button className="task-action-btn danger" onClick={onDelete} title="Supprimer"><Trash2 size={14} /></button>
      </div>
    </div>
  );
}
