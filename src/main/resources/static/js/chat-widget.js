(() => {
  const STORAGE_CONV_ID = 'mclub.chat.conversationId';

  function getScopeFromDom() {
    const el = document.querySelector('[data-chat-scope]');
    if (!el) return {};
    return {
      clubId: el.getAttribute('data-club-id') || null,
      eventId: el.getAttribute('data-event-id') || null,
    };
  }

  async function ensureConversationId() {
    let conversationId = localStorage.getItem(STORAGE_CONV_ID) || '';
    if (conversationId) return conversationId;
    const res = await fetch('/api/chat/conversation-id');
    if (!res.ok) return '';
    const data = await res.json();
    if (data && data.conversationId) {
      conversationId = data.conversationId;
      localStorage.setItem(STORAGE_CONV_ID, conversationId);
    }
    return conversationId;
  }

  function createDom() {
    if (document.getElementById('mclubChatFab')) return;

    const fab = document.createElement('button');
    fab.id = 'mclubChatFab';
    fab.type = 'button';
    fab.className = 'mclub-chat-fab';
    fab.innerHTML = '<i class="bi bi-chat-dots"></i>';

    const dialog = document.createElement('div');
    dialog.id = 'mclubChatDialog';
    dialog.className = 'mclub-chat-dialog';
    dialog.innerHTML = `
      <div class="mclub-chat-header">
        <div class="mclub-chat-title">MClub Assistant</div>
        <div class="mclub-chat-actions">
          <button type="button" class="btn btn-sm btn-outline-light" id="mclubChatClear" title="Clear">Clear</button>
          <button type="button" class="btn btn-sm btn-outline-light" id="mclubChatClose" title="Close">×</button>
        </div>
      </div>
      <div class="mclub-chat-messages" id="mclubChatMessages">
        <div class="mclub-chat-msg ai">Hi! Ask me about clubs, events, attendance, ratings, or comments.</div>
      </div>
      <div class="mclub-chat-error" id="mclubChatError"></div>
      <div class="mclub-chat-compose">
        <textarea id="mclubChatInput" rows="1" placeholder="Type a message…"></textarea>
        <button class="btn btn-primary" id="mclubChatSend" type="button">Send</button>
      </div>
    `;

    document.body.appendChild(fab);
    document.body.appendChild(dialog);

    return { fab, dialog };
  }

  function addMessage(role, text) {
    const messagesEl = document.getElementById('mclubChatMessages');
    const div = document.createElement('div');
    div.className = 'mclub-chat-msg ' + (role === 'user' ? 'user' : 'ai');
    div.textContent = text;
    messagesEl.appendChild(div);
    messagesEl.scrollTop = messagesEl.scrollHeight;
  }

  function setBusy(busy) {
    const sendBtn = document.getElementById('mclubChatSend');
    const input = document.getElementById('mclubChatInput');
    if (sendBtn) sendBtn.disabled = busy;
    if (input) input.disabled = busy;
  }

  async function loadHistory() {
    const messagesEl = document.getElementById('mclubChatMessages');
    if (!messagesEl) return;

    const conversationId = await ensureConversationId();
    const qs = conversationId ? ('?conversationId=' + encodeURIComponent(conversationId)) : '';
    const res = await fetch('/api/chat/history' + qs);
    if (!res.ok) return;
    const data = await res.json();
    const msgs = (data && data.messages) ? data.messages : [];
    if (msgs.length > 0) messagesEl.innerHTML = '';
    for (const m of msgs) {
      if (!m || !m.content) continue;
      addMessage(m.role === 'user' ? 'user' : 'ai', m.content);
    }
  }

  async function clearHistory() {
    const errorEl = document.getElementById('mclubChatError');
    if (errorEl) errorEl.textContent = '';
    setBusy(true);
    try {
      const conversationId = await ensureConversationId();
      const qs = conversationId ? ('?conversationId=' + encodeURIComponent(conversationId)) : '';
      const res = await fetch('/api/chat/history' + qs, { method: 'DELETE' });
      if (!res.ok) {
        const body = await res.text();
        if (errorEl) errorEl.textContent = 'HTTP ' + res.status + ': ' + body;
        return;
      }
      const messagesEl = document.getElementById('mclubChatMessages');
      messagesEl.innerHTML = '';
      addMessage('ai', 'Chat cleared. How can I help?');
    } finally {
      setBusy(false);
    }
  }

  async function send() {
    const inputEl = document.getElementById('mclubChatInput');
    const errorEl = document.getElementById('mclubChatError');
    const text = (inputEl.value || '').trim();
    if (!text) return;

    if (errorEl) errorEl.textContent = '';
    addMessage('user', text);
    inputEl.value = '';
    setBusy(true);

    try {
      const conversationId = await ensureConversationId();
      const scope = getScopeFromDom();

      const res = await fetch('/api/chat/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          conversationId,
          from: 'web',
          text,
          clubId: scope.clubId,
          eventId: scope.eventId,
        })
      });

      if (!res.ok) {
        const body = await res.text();
        if (errorEl) errorEl.textContent = 'HTTP ' + res.status + ': ' + body;
        addMessage('ai', 'Sorry — I ran into an error handling that message.');
        return;
      }

      const data = await res.json();
      addMessage('ai', data.assistantMessage ?? '(empty reply)');
    } catch (e) {
      if (errorEl) errorEl.textContent = e.message || String(e);
      addMessage('ai', 'Sorry — I ran into an error handling that message.');
    } finally {
      setBusy(false);
    }
  }

  function toggle(open) {
    const dialog = document.getElementById('mclubChatDialog');
    if (!dialog) return;
    const willOpen = (open != null) ? open : !dialog.classList.contains('open');
    dialog.classList.toggle('open', willOpen);
    if (willOpen) loadHistory();
  }

  function init() {
    const dom = createDom();
    if (!dom) return;

    document.getElementById('mclubChatFab').addEventListener('click', () => toggle());
    document.getElementById('mclubChatClose').addEventListener('click', () => toggle(false));
    document.getElementById('mclubChatSend').addEventListener('click', send);
    document.getElementById('mclubChatClear').addEventListener('click', clearHistory);

    const input = document.getElementById('mclubChatInput');
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        send();
      }
    });

    // If scope changes due to navigation, no extra work: we read data attrs at send time.
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

