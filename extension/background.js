// Service worker — handles secure token storage and API calls.
// Runs in an isolated context; content scripts message this worker rather
// than holding the JWT themselves.

const API_BASE = 'http://localhost:8080';

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message.type === 'USE_CREDENTIAL') {
    handleUseCredential(message.credentialId).then(sendResponse);
    return true; // keeps the channel open for the async response
  }

  if (message.type === 'GET_CREDENTIALS_FOR_URL') {
    handleGetCredentials(message.url).then(sendResponse);
    return true;
  }

  if (message.type === 'LOGOUT') {
    chrome.storage.local.remove(['token', 'orgId']);
    sendResponse({ ok: true });
  }
});

async function handleUseCredential(credentialId) {
  const { token } = await chrome.storage.local.get('token');
  if (!token) return { error: 'Not authenticated' };

  try {
    const res = await fetch(`${API_BASE}/api/credentials/${credentialId}/use`, {
      headers: { Authorization: `Bearer ${token}` }
    });

    if (!res.ok) return { error: `Server returned ${res.status}` };

    const data = await res.json();
    // Return plaintext to content script — it must zero it from memory after fill
    return { username: data.username, password: data.password };
  } catch (e) {
    return { error: e.message };
  }
}

async function handleGetCredentials(pageUrl) {
  const { token } = await chrome.storage.local.get('token');
  if (!token) return { credentials: [], error: 'Not authenticated' };

  try {
    const res = await fetch(`${API_BASE}/api/credentials`, {
      headers: { Authorization: `Bearer ${token}` }
    });

    if (!res.ok) return { credentials: [], error: `Server returned ${res.status}` };

    const all = await res.json();
    // Filter to credentials whose URL matches the current page's hostname
    const hostname = new URL(pageUrl).hostname;
    const matching = all.filter(c => {
      try {
        return new URL(c.url).hostname === hostname;
      } catch {
        return false;
      }
    });

    return { credentials: matching };
  } catch (e) {
    return { credentials: [], error: e.message };
  }
}
