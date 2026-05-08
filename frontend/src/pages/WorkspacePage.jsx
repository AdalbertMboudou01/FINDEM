import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Bot, Send, Sparkles } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

async function apiFetch(path, options = {}, token) {
  const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) };
  const res = await fetch(path, { headers, ...options });
  if (!res.ok) throw new Error(await res.text().catch(() => 'Erreur'));
  const ct = res.headers.get('content-type') ?? '';
  return ct.includes('application/json') ? res.json() : null;
}

const SUGGESTIONS = [
  'Rédige un email de confirmation de candidature',
  'Génère 5 questions d\'entretien pour un dev backend Java',
  'Quelles sont les meilleures pratiques pour évaluer un alternant ?',
  'Rédige une fiche de poste pour un développeur React',
  'Comment structurer un entretien en 45 minutes ?',
];

function renderContent(text) {
  if (!text) return null;
  return text.split('\n').map((line, i) => (
    <span key={i}>
      <span dangerouslySetInnerHTML={{ __html: line.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>') }} />
      {i < text.split('\n').length - 1 && <br />}
    </span>
  ));
}

export default function WorkspacePage() {
  const { session } = useAuth();
  const token = session?.token;

  const [channelId, setChannelId] = useState(null);
  const [messages, setMessages]   = useState([]);
  const [input, setInput]         = useState('');
  const [sending, setSending]     = useState(false);
  const [booting, setBooting]     = useState(true);
  const messagesEndRef = useRef(null);
  const pollRef        = useRef(null);

  // Load workspace channel
  useEffect(() => {
    apiFetch('/api/team/channels/workspace', {}, token)
      .then(ch => {
        setChannelId(ch.channelId);
        return apiFetch(`/api/team/channels/${ch.channelId}/messages`, {}, token);
      })
      .then(data => { setMessages(data || []); setBooting(false); })
      .catch(() => setBooting(false));
  }, [token]);

  const loadMessages = useCallback(() => {
    if (!channelId) return;
    apiFetch(`/api/team/channels/${channelId}/messages`, {}, token)
      .then(data => setMessages(data || []))
      .catch(() => {});
  }, [channelId, token]);

  // Polling
  useEffect(() => {
    clearInterval(pollRef.current);
    if (!channelId) return;
    pollRef.current = setInterval(loadMessages, 4000);
    return () => clearInterval(pollRef.current);
  }, [channelId, loadMessages]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  async function handleSend(text) {
    const content = (text ?? input).trim();
    if (!content || !channelId || sending) return;
    setSending(true);
    setInput('');
    try {
      const msg = await apiFetch(`/api/team/channels/${channelId}/messages`, {
        method: 'POST',
        body: JSON.stringify({ content }),
      }, token);
      setMessages(prev => [...prev, msg]);
      setTimeout(loadMessages, 3500);
      setTimeout(loadMessages, 8000);
    } catch { /* ignore */ } finally {
      setSending(false);
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); }
  }

  if (booting) return <div className="workspace-boot">Chargement de votre espace…</div>;

  return (
    <div className="workspace-shell">
      {/* Header */}
      <div className="workspace-header">
        <div className="workspace-header-left">
          <div className="workspace-persona-badge">
            <Bot size={18} />
          </div>
          <div>
            <p className="workspace-title">Mon espace de travail</p>
            <p className="workspace-subtitle">FindemWorker — votre assistant RH personnel</p>
          </div>
        </div>
      </div>

      {/* Messages */}
      <div className="workspace-messages">
        {messages.length === 0 && (
          <div className="workspace-welcome">
            <Sparkles size={32} className="workspace-welcome-icon" />
            <p className="workspace-welcome-title">Bonjour ! Je suis FindemWorker.</p>
            <p className="workspace-welcome-hint">Je peux rédiger des emails, générer des questions d'entretien, créer des fiches de poste, et bien plus.</p>
            <div className="workspace-suggestions">
              {SUGGESTIONS.map((s, i) => (
                <button key={i} className="workspace-suggestion-btn" onClick={() => handleSend(s)}>
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map(m => (
          <div key={m.messageId} className={`ws-msg${m.authorType === 'AI_SYSTEM' ? ' ws-msg-ai' : ' ws-msg-human'}`}>
            {m.authorType === 'AI_SYSTEM' ? (
              <div className="ws-msg-ai-avatar"><Bot size={14} /></div>
            ) : (
              <div className="ws-msg-human-avatar">{m.authorName?.[0]?.toUpperCase() ?? 'M'}</div>
            )}
            <div className="ws-msg-body">
              <div className="ws-msg-meta">
                <span className="ws-msg-author">{m.authorType === 'AI_SYSTEM' ? 'FindemWorker' : 'Vous'}</span>
                <span className="ws-msg-time">
                  {m.createdAt ? new Date(m.createdAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) : ''}
                </span>
              </div>
              <div className="ws-msg-content">{renderContent(m.content)}</div>
            </div>
          </div>
        ))}

        {sending && (
          <div className="ws-msg ws-msg-ai">
            <div className="ws-msg-ai-avatar"><Bot size={14} /></div>
            <div className="ws-msg-body">
              <div className="ws-typing"><span/><span/><span/></div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="workspace-input-area">
        <textarea
          className="workspace-textarea"
          rows={2}
          placeholder="Demandez à FindemWorker… (ex: rédige un email de refus bienveillant)"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={sending}
        />
        <button
          className="workspace-send-btn"
          onClick={() => handleSend()}
          disabled={sending || !input.trim()}
        >
          <Send size={16} />
        </button>
      </div>
    </div>
  );
}
