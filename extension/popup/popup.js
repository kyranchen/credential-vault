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

  // Route through background service worker — popup pages are subject to CORS
  // but background service workers bypass it for URLs in host_permissions
  let result;
  try {
    result = await chrome.runtime.sendMessage({ type: 'LOGIN', email, password });
  } catch (e) {
    errorEl.textContent = 'Background service worker unavailable — try reloading the extension';
    return;
  }

  if (!result) {
    errorEl.textContent = 'No response from background worker — reload the extension at chrome://extensions';
    return;
  }

  if (result.error) {
    errorEl.textContent = result.error;
  } else {
    showMain();
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

  let fillResult;
  try {
    fillResult = await chrome.tabs.sendMessage(tabId, {
      type: 'FILL_CREDENTIAL',
      username: result.username,
      password: result.password
    });
  } catch {
    // Content script not injected — user needs to refresh the page
    document.getElementById('status').textContent = 'Refresh the page and try again';
    btn.disabled = false;
    btn.textContent = 'Fill';
    return;
  }

  if (!fillResult?.filled) {
    document.getElementById('status').textContent = fillResult?.reason ?? 'Could not find form fields';
    btn.disabled = false;
    btn.textContent = 'Fill';
    return;
  }

  btn.textContent = 'Filled ✓';
  setTimeout(() => window.close(), 800);
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]));
}
