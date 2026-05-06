const API_BASE = 'http://localhost:8080';

document.addEventListener('DOMContentLoaded', async () => {
  const { token } = await chrome.storage.local.get('token');
  if (token) {
    showMain();
  } else {
    showAuth();
  }
});

// ── Auth ────────────────────────────────────────────────────────────────────

function showAuth() {
  document.getElementById('auth-section').style.display = 'block';
  document.getElementById('main-section').style.display = 'none';
}

document.getElementById('login-btn').addEventListener('click', async () => {
  const email = document.getElementById('email').value;
  const password = document.getElementById('login-password').value;
  const errorEl = document.getElementById('auth-error');
  errorEl.textContent = '';

  try {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    if (!res.ok) throw new Error('Invalid credentials');

    const { token } = await res.json();
    await chrome.storage.local.set({ token });
    showMain();
  } catch (e) {
    errorEl.textContent = e.message;
  }
});

document.getElementById('logout-btn').addEventListener('click', async () => {
  chrome.runtime.sendMessage({ type: 'LOGOUT' });
  showAuth();
});

// ── Main (credential list) ───────────────────────────────────────────────────

async function showMain() {
  document.getElementById('auth-section').style.display = 'none';
  document.getElementById('main-section').style.display = 'block';

  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  const { credentials, error } = await chrome.runtime.sendMessage({
    type: 'GET_CREDENTIALS_FOR_URL',
    url: tab.url
  });

  const listEl = document.getElementById('credentials-list');
  const statusEl = document.getElementById('status');

  if (error) {
    statusEl.textContent = error;
    return;
  }

  if (credentials.length === 0) {
    statusEl.textContent = 'No credentials for this site.';
    return;
  }

  listEl.innerHTML = '';
  credentials.forEach(cred => {
    const item = document.createElement('div');
    item.className = 'credential-item';
    item.innerHTML = `
      <div>
        <div class="credential-name">${escapeHtml(cred.name)}</div>
        <div class="credential-user">${escapeHtml(cred.username)}</div>
      </div>
      <button class="fill-btn" data-id="${escapeHtml(cred.id)}">Fill</button>
    `;
    listEl.appendChild(item);
  });

  listEl.querySelectorAll('.fill-btn').forEach(btn => {
    btn.addEventListener('click', () => fillCredential(btn.dataset.id, btn, tab.id));
  });
}

async function fillCredential(credentialId, btn, tabId) {
  btn.disabled = true;
  btn.textContent = '…';

  const result = await chrome.runtime.sendMessage({ type: 'USE_CREDENTIAL', credentialId });

  if (result.error) {
    document.getElementById('status').textContent = result.error;
    btn.disabled = false;
    btn.textContent = 'Fill';
    return;
  }

  await chrome.tabs.sendMessage(tabId, {
    type: 'FILL_CREDENTIAL',
    username: result.username,
    password: result.password
  });

  btn.textContent = 'Filled';
  // Close popup after short delay so user sees confirmation
  setTimeout(() => window.close(), 800);
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]));
}
