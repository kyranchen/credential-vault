// Injected into every page. Listens for fill commands from the popup.
// Never holds the JWT — all API calls go through the background service worker.

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message.type === 'FILL_CREDENTIAL') {
    const { username, password } = message;
    fillForm(username, password).then(result => sendResponse(result));
    return true; // async response
  }
});

async function fillForm(username, password) {
  const passwordInput = findPasswordInput();

  if (passwordInput) {
    // Password field is already visible — fill both fields at once
    const usernameInput = findUsernameInput(passwordInput);
    if (usernameInput) setNativeInputValue(usernameInput, username);
    setNativeInputValue(passwordInput, password);
    return { filled: true };
  }

  // No password field yet — this is a multi-step login (e.g. X.com, Google).
  // Fill the username field and click Next, then wait for password to appear.
  const usernameInput = findUsernameInput(null);
  if (!usernameInput) return { filled: false, reason: 'No input fields found' };

  setNativeInputValue(usernameInput, username);

  // Click the Next / Continue button
  const nextBtn = findNextButton();
  if (nextBtn) nextBtn.click();

  // Wait up to 3s for the password field to appear
  const appeared = await waitForPasswordInput(3000);
  if (!appeared) return { filled: false, reason: 'Password field did not appear' };

  setNativeInputValue(appeared, password);
  return { filled: true };
}

// ── DOM helpers ──────────────────────────────────────────────────────────────

function findPasswordInput() {
  return document.querySelector('input[type="password"]') ?? null;
}

function findUsernameInput(nearElement) {
  const selectors = [
    'input[autocomplete="username"]',
    'input[autocomplete="email"]',
    'input[type="email"]',
    'input[name="username"]',
    'input[name="email"]',
    'input[name="text"]',   // X.com uses name="text" for its username field
    'input[type="text"]',
  ];

  // Prefer an input inside the same form as the password field
  const scope = nearElement?.closest('form') ?? document;

  for (const sel of selectors) {
    const el = scope.querySelector(sel);
    if (el) return el;
  }
  return null;
}

function findNextButton() {
  // Try common patterns for "Next" / "Continue" buttons
  const candidates = Array.from(document.querySelectorAll('button, [role="button"]'));
  return candidates.find(el => /next|continue|log in|sign in/i.test(el.textContent)) ?? null;
}

function waitForPasswordInput(timeoutMs) {
  return new Promise(resolve => {
    // Already there
    const existing = findPasswordInput();
    if (existing) { resolve(existing); return; }

    const observer = new MutationObserver(() => {
      const input = findPasswordInput();
      if (input) {
        observer.disconnect();
        clearTimeout(timer);
        // Small delay so the field finishes its entrance animation/focus
        setTimeout(() => resolve(input), 100);
      }
    });

    observer.observe(document.body, { childList: true, subtree: true });

    const timer = setTimeout(() => {
      observer.disconnect();
      resolve(null);
    }, timeoutMs);
  });
}

// React and other frameworks use synthetic events — setting .value alone
// won't trigger state updates. We must fire the native input event.
function setNativeInputValue(input, value) {
  const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
    window.HTMLInputElement.prototype,
    'value'
  )?.set;

  if (nativeInputValueSetter) {
    nativeInputValueSetter.call(input, value);
  } else {
    input.value = value;
  }

  input.dispatchEvent(new Event('input', { bubbles: true }));
  input.dispatchEvent(new Event('change', { bubbles: true }));
  input.dispatchEvent(new FocusEvent('blur', { bubbles: true }));
}
