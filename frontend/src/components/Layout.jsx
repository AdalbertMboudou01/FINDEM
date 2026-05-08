import React, { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Users, Briefcase, CheckSquare, UserCog,
  MessageSquare, Sparkles, Bell, LogOut, Check, Star, Calendar, ChevronDown,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { decodeToken } from '../lib/auth';
import { api } from '../lib/api';
import { createStompClient } from '../lib/websocket';

const NAV = [
  { to: '/dashboard',    icon: LayoutDashboard, label: 'Tableau de bord' },
  {
    group: 'offres',
    icon: Briefcase,
    label: 'Recrutement',
    children: [
      { to: '/offres',       icon: Briefcase, label: 'Offres'        },
      { to: '/candidatures', icon: Users,     label: 'Candidatures'  },
      { to: '/entretiens',   icon: Calendar,  label: 'Entretiens'    },
      { to: '/vivier',       icon: Star,      label: 'Vivier'        },
    ],
  },
  { to: '/taches',       icon: CheckSquare,      label: 'Tâches'         },
  { to: '/equipe',       icon: UserCog,          label: 'Équipe'         },
  { to: '/conversations', icon: MessageSquare,   label: 'Conversations'  },
  { to: '/workspace',    icon: Sparkles,         label: 'Mon espace'     },
];

const NO_PAD_ROUTES = ['/candidatures', '/conversations', '/workspace'];

const TITLES = {
  '/dashboard':    'Tableau de bord',
  '/candidatures': 'Candidatures',
  '/offres':       'Offres',
  '/entretiens':   'Entretiens',
  '/vivier':       'Vivier',
  '/taches':       'Tâches',
  '/equipe':       'Équipe',
  '/conversations': 'Conversations',
  '/workspace':    'Mon espace de travail',
};

const GROUP_PATHS = ['/offres', '/candidatures', '/entretiens', '/vivier'];

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

export default function Layout() {
  const { session, logout } = useAuth();
  const navigate  = useNavigate();
  const location  = useLocation();

  const claims    = session?.token ? decodeToken(session.token) : null;
  const email     = claims?.sub ?? '';
  const companyId = claims?.companyId ?? null;
  const initials  = email ? email[0].toUpperCase() : '?';
  const pageTitle = TITLES[location.pathname] ?? '';

  const [openGroups, setOpenGroups] = useState(() => {
    const isInGroup = GROUP_PATHS.includes(location.pathname);
    return { offres: isInGroup };
  });

  function toggleGroup(key) {
    setOpenGroups(prev => ({ ...prev, [key]: !prev[key] }));
  }

  /* ── Notifications state ── */
  const [notifOpen,    setNotifOpen]    = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount,  setUnreadCount]  = useState(0);
  const notifRef = useRef(null);

  useEffect(() => {
    if (!session?.token) return;
    loadNotifications();

    const client = createStompClient({
      token: session.token,
      onNotification: (notif) => {
        setNotifications(prev => [notif, ...prev]);
        setUnreadCount(prev => prev + 1);
      },
    });
    client.activate();
    return () => client.deactivate();
  }, [session?.token]);

  useEffect(() => {
    function handleClickOutside(e) {
      if (notifRef.current && !notifRef.current.contains(e.target)) setNotifOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  async function loadNotifications() {
    try {
      const data = await api.get('/api/internal-notifications/me', session?.token);
      if (data) {
        setNotifications(data.notifications ?? []);
        setUnreadCount(data.unreadCount ?? 0);
      }
    } catch { /* silent */ }
  }

  async function markAsRead(id) {
    try {
      await api.patch(`/api/internal-notifications/${id}/read`, {}, session?.token);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch { /* silent */ }
  }

  async function markAllAsRead() {
    try {
      await api.patch('/api/internal-notifications/read-all', {}, session?.token);
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch { /* silent */ }
  }

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <div className="app-shell">
      {/* ── Sidebar ───────────────────────────────────────────── */}
      <aside className="sidebar">
        <div className="sidebar-logo">Findem</div>

        <nav className="sidebar-nav">
          {NAV.map(item => {
            if (item.group) {
              const isOpen    = openGroups[item.group];
              const GroupIcon = item.icon;
              const hasActive = item.children.some(c => location.pathname === c.to);
              return (
                <div key={item.group} className="sidebar-group">
                  <button
                    className={`sidebar-group-toggle${hasActive ? ' has-active' : ''}`}
                    onClick={() => toggleGroup(item.group)}
                  >
                    <GroupIcon size={20} strokeWidth={1.8} />
                    <span>{item.label}</span>
                    <ChevronDown size={14} className={`sidebar-chevron${isOpen ? ' open' : ''}`} />
                  </button>
                  {isOpen && (
                    <div className="sidebar-group-children">
                      {item.children.map(({ to, icon: Icon, label }) => (
                        <NavLink
                          key={to}
                          to={to}
                          className={({ isActive }) => `sidebar-link sidebar-sublink${isActive ? ' active' : ''}`}
                        >
                          <Icon size={16} strokeWidth={1.8} />
                          <span>{label}</span>
                        </NavLink>
                      ))}
                    </div>
                  )}
                </div>
              );
            }
            const { to, icon: Icon, label } = item;
            return (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
              >
                <Icon size={20} strokeWidth={1.8} />
                <span>{label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-user">
            <div className="sidebar-avatar">{initials}</div>
            <div className="sidebar-user-info">
              <span className="sidebar-email" title={email}>{email}</span>
              <span className="sidebar-role">{session?.role}</span>
            </div>
          </div>
          <button className="sidebar-logout" onClick={handleLogout} aria-label="Se déconnecter">
            <LogOut size={18} strokeWidth={1.8} />
          </button>
        </div>
      </aside>

      {/* ── Body ──────────────────────────────────────────────── */}
      <div className="app-body">
        <header className="topbar">
          <h2 className="topbar-title">{pageTitle}</h2>
          <div className="topbar-actions">
            {/* Notifications bell */}
            <div className="notif-wrapper" ref={notifRef}>
              <button
                className="topbar-icon-btn notif-bell"
                aria-label="Notifications"
                onClick={() => setNotifOpen(v => !v)}
              >
                <Bell size={20} strokeWidth={1.8} />
                {unreadCount > 0 && (
                  <span className="notif-badge">{unreadCount > 9 ? '9+' : unreadCount}</span>
                )}
              </button>

              {notifOpen && (
                <div className="notif-dropdown">
                  <div className="notif-dropdown-header">
                    <span>Notifications</span>
                    {unreadCount > 0 && (
                      <button className="notif-mark-all" onClick={markAllAsRead}>
                        <Check size={12} /> Tout lire
                      </button>
                    )}
                  </div>
                  <div className="notif-list">
                    {notifications.length === 0 ? (
                      <p className="notif-empty">Aucune notification.</p>
                    ) : notifications.map((n, i) => (
                      <div
                        key={n.id ?? i}
                        className={`notif-item${n.read ? '' : ' unread'}`}
                        onClick={() => !n.read && markAsRead(n.id)}
                      >
                        {!n.read && <span className="notif-dot" />}
                        <div className="notif-body">
                          <p className="notif-message">{n.message ?? n.title ?? 'Notification'}</p>
                          <span className="notif-time">{timeAgo(n.createdAt)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
            <div className="topbar-avatar">{initials}</div>
          </div>
        </header>

        <main className={`app-content${NO_PAD_ROUTES.includes(location.pathname) ? ' no-pad' : ''}`}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
