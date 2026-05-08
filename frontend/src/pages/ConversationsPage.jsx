import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Hash, Bot, Send, RefreshCw } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { decodeToken } from '../lib/auth';

const AI_PERSONAS = {
  '00000000-0000-0000-0000-000000000001': { name: 'FindemAssist', color: '#6366f1' },
  '00000000-0000-0000-0000-000000000002': { name: 'FindemLooker', color: '#0ea5e9' },
  '00000000-0000-0000-0000-000000000003': { name: 'FindemWorker', color: '#10b981' },
};

const MENTION_HINTS = ['@findemassist', '@findemlooker', '@findemworker'];
const MENTION_COLORS = {
  '@findemassist': '#6366f1',
  '@findemlooker': '#0ea5e9',
  '@findemworker': '#10b981',
};

async function apiFetch(path, options = {}, token) {
  const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) };
  const res = await fetch(path, { headers, ...options });
  if (!res.ok) throw new Error(await res.text().catch(() => 'Erreur'));
  const ct = res.headers.get('content-type') ?? '';
  return ct.includes('application/json') ? res.json() : null;
}

function renderContent(text) {
  if (!text) return '';
  // highlight @mentions
  const parts = text.split(/(@findemassist|@findemlooker|@findemworker)/gi);
  return parts.map((part, i) => {
    const lower = part.toLowerCase();
    if (MENTION_COLORS[lower]) {
      return <span key={i} className="conv-mention" style={{ color: MENTION_COLORS[lower] }}>{part}</span>;
    }
    // basic markdown bold
    if (part.includes('**')) {
      return <span key={i} dangerouslySetInnerHTML={{ __html: part.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>').replace(/\n/g, '<br/>') }} />;
    }
    return <span key={i} style={{ whiteSpace: 'pre-wrap' }}>{part}</span>;
  });
}

export default function ConversationsPage() {
  const { session } = useAuth();
  const token = session?.token;
  const claims = token ? decodeToken(token) : null;
  const currentRecruiterId = claims?.recruiterId ?? '';

  const [channels, setChannels]     = useState([]);
  const [selected, setSelected]     = useState(null);
  const [messages, setMessages]     = useState([]);
  const [input, setInput]           = useState('');
  const [sending, setSending]       = useState(false);
  const [loading, setLoading]       = useState(false);
  const [hint, setHint]             = useState(null); // mention dropdown
  const messagesEndRef = useRef(null);
  const textareaRef    = useRef(null);
  const pollRef        = useRef(null);

  // Load channels
  useEffect(() => {
    apiFetch('/api/team/channels', {}, token)
      .then(data => {
        // exclude workspace from conversations (has its own page)
        const filtered = (data || []).filter(c => c.type !== 'WORKSPACE');
        setChannels(filtered);
        if (filtered.length > 0 && !selected) setSelected(filtered[0]);
      })
      .catch(() => {});
  }, [token]);

  // Load messages when channel changes
  const loadMessages = useCallback(() => {
    if (!selected) return;
    apiFetch(`/api/team/channels/${selected.channelId}/messages`, {}, token)
      .then(data => setMessages(data || []))
      .catch(() => {});
  }, [selected, token]);

  useEffect(() => {
    if (!selected) return;
    setLoading(true);
    apiFetch(`/api/team/channels/${selected.channelId}/messages`, {}, token)
      .then(data => { setMessages(data || []); setLoading(false); })
      .catch(() => setLoading(false));
  }, [selected, token]);

  // Polling every 4s
  useEffect(() => {
    clearInterval(pollRef.current);
    if (!selected) return;
    pollRef.current = setInterval(loadMessages, 4000);
    return () => clearInterval(pollRef.current);
  }, [selected, loadMessages]);

  // Scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // @ mention hint
  function handleInputChange(e) {
    const val = e.target.value;
    setInput(val);
    const cursor = e.target.selectionStart;
    const before = val.slice(0, cursor);
    const match = before.match(/@(\w*)$/);
    if (match) {
      const partial = '@' + match[1].toLowerCase();
      const filtered = MENTION_HINTS.filter(h => h.startsWith(partial));
      setHint(filtered.length > 0 ? filtered : null);
    } else {
      setHint(null);
    }
  }

  function insertMention(mention) {
    const cursor = textareaRef.current.selectionStart;
    const before = input.slice(0, cursor).replace(/@\w*$/, '');
    const after = input.slice(cursor);
    setInput(before + mention + ' ' + after);
    setHint(null);
    textareaRef.current.focus();
  }

  async function handleSend(e) {
    e.preventDefault();
    if (!input.trim() || !selected || sending) return;
    setSending(true);
    try {
      const msg = await apiFetch(`/api/team/channels/${selected.channelId}/messages`, {
        method: 'POST',
        body: JSON.stringify({ content: input.trim() }),
      }, token);
      setMessages(prev => [...prev, msg]);
      setInput('');
      // poll after a delay to catch AI responses
      setTimeout(loadMessages, 3000);
      setTimeout(loadMessages, 7000);
    } catch { /* ignore */ } finally {
      setSending(false);
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(e); }
    if (e.key === 'Escape') setHint(null);
  }

  return (
    <div className="conv-shell">
      {/* ── Sidebar channels ── */}
      <aside className="conv-sidebar">
        <p className="conv-sidebar-label">Channels</p>
        {channels.map(ch => (
          <button
            key={ch.channelId}
            className={`conv-channel-btn${selected?.channelId === ch.channelId ? ' active' : ''}`}
            onClick={() => setSelected(ch)}
          >
            <Hash size={14} />
            <span>{ch.name}</span>
          </button>
        ))}
      </aside>

      {/* ── Chat area ── */}
      <div className="conv-main">
        {selected ? (
          <>
            <header className="conv-header">
              <Hash size={16} />
              <strong>{selected.name}</strong>
              <button className="conv-refresh-btn" onClick={loadMessages} title="Rafraîchir">
                <RefreshCw size={14} />
              </button>
              <span className="conv-hint-bar">
                Utilisez <code>@findemassist</code> <code>@findemlooker</code> <code>@findemworker</code> pour solliciter les IA
              </span>
            </header>

            <div className="conv-messages">
              {loading && messages.length === 0 && <p className="conv-loading">Chargement…</p>}
              {messages.map(m => <MessageBubble key={m.messageId} msg={m} currentRecruiterId={currentRecruiterId} />)}
              <div ref={messagesEndRef} />
            </div>

            <form className="conv-input-area" onSubmit={handleSend}>
              {hint && (
                <div className="conv-mention-dropdown">
                  {hint.map(h => (
                    <button key={h} type="button" className="conv-mention-item" onClick={() => insertMention(h)}>
                      <Bot size={13} style={{ color: MENTION_COLORS[h] }} /> {h}
                    </button>
                  ))}
                </div>
              )}
              <textarea
                ref={textareaRef}
                className="conv-textarea"
                rows={2}
                placeholder={`Message dans #${selected.name}… (Entrée pour envoyer, Maj+Entrée pour nouvelle ligne)`}
                value={input}
                onChange={handleInputChange}
                onKeyDown={handleKeyDown}
              />
              <button type="submit" className="conv-send-btn" disabled={sending || !input.trim()}>
                <Send size={16} />
              </button>
            </form>
          </>
        ) : (
          <div className="conv-empty">Sélectionnez un channel pour commencer</div>
        )}
      </div>
    </div>
  );
}

function MessageBubble({ msg, currentRecruiterId }) {
  const isAI = msg.authorType === 'AI_SYSTEM';
  const persona = isAI ? AI_PERSONAS[msg.authorId] : null;
  const isSelf = !isAI && msg.authorId === currentRecruiterId;

  const initials = isAI
    ? (persona?.name?.[0] ?? 'A')
    : (msg.authorName?.[0]?.toUpperCase() ?? '?');

  return (
    <div className={`conv-msg${isAI ? ' conv-msg-ai' : isSelf ? ' conv-msg-self' : ''}`}>
      <div className="conv-msg-avatar" style={{ background: isAI ? (persona?.color ?? '#6366f1') : '#64748b' }}>
        {isAI ? <Bot size={13} /> : initials}
      </div>
      <div className="conv-msg-body">
        <div className="conv-msg-meta">
          <span className="conv-msg-author" style={isAI ? { color: persona?.color } : {}}>
            {msg.authorName}
          </span>
          <span className="conv-msg-time">
            {msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) : ''}
          </span>
        </div>
        <div className="conv-msg-content">{renderContent(msg.content)}</div>
      </div>
    </div>
  );
}
