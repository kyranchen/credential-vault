// Injected into every page. Listens for fill commands from the popup.
// Never holds the JWT — all API calls go through the background service worker.

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message.type === 'FILL_CREDENTIAL') {
    const { username, password } = message;
    const filled = fillForm(username, password);
    sendResponse({ filled });
    // Zero out the password string reference as quickly as possible.
    // Note: JS strings are immutable; we can't truly scrub memory, but
    // at minimum we drop our reference so GC can collect it.
  }
});

function fillForm(username, password) {
  const passwordInputs = Array.from(document.querySelectorAll('input[type="password"]'));
  if (passwordInputs.length === 0) return false;

  // Use the password field's form to find the associated username/email field
  const passwordInput = passwordInputs[0];
  const form = passwordInput.closest('form');

  const usernameInput = form
    ? form.querySelector('input[type="email"], input[type="text"], input[autocomplete="username"]')
    : document.querySelector('input[type="email"], input[type="text"]');

  if (usernameInput) {
    setNativeInputValue(usernameInput, username);
  }
  setNativeInputValue(passwordInput, password);

  return true;
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
}
