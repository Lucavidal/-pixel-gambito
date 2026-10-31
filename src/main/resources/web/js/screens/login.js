// login.js — pantalla de inicio: pestañas "Iniciar sesión" y "Crear cuenta".
// Se comporta como LoginDialog (Swing): si el usuario no existe, pasa sola a "Crear cuenta"
// con el nombre ya escrito; si al crear la cuenta el nombre ya está tomado, vuelve a "Iniciar sesión".
// Las reglas de usuario y contraseña las decide el núcleo: acá solo se muestran sus mensajes,
// traducidos por el CÓDIGO del AuthResult (UNKNOWN_USER, WRONG_PASSWORD...), no por su texto.
import { api } from '../api.js';
import { t, has, onLangChange } from '../i18n.js';

const $ = (id) => document.getElementById(id);

export function createLoginScreen({ onLoggedIn }) {
  const element = $('screen-login');
  const tabs = { login: $('tab-login'), register: $('tab-register') };
  const forms = { login: $('form-login'), register: $('form-register') };
  const loginForm = forms.login.elements;
  const registerForm = forms.register.elements;
  let sending = false;
  // El error a la vista de cada formulario, como función que arma su texto: se re-traduce al cambiar de idioma
  const errors = { login: null, register: null };

  onLangChange(() => {
    for (const name of Object.keys(forms)) {
      forms[name].querySelector('.form-error').textContent = errors[name]?.() ?? '';
    }
  });

  tabs.login.addEventListener('click', () => select('login'));
  tabs.register.addEventListener('click', () => select('register'));
  // Flechas izquierda/derecha entre pestañas, como pide el patrón de tabs accesibles
  for (const tab of Object.values(tabs)) {
    tab.addEventListener('keydown', (event) => {
      if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
      event.preventDefault();
      select(tab === tabs.login ? 'register' : 'login');
      (tab === tabs.login ? tabs.register : tabs.login).focus();
    });
  }

  forms.login.addEventListener('submit', (event) => { event.preventDefault(); login(); });
  forms.register.addEventListener('submit', (event) => { event.preventDefault(); register(); });

  function select(name, { focus = true } = {}) {
    for (const key of Object.keys(tabs)) {
      const active = key === name;
      tabs[key].setAttribute('aria-selected', String(active));
      tabs[key].tabIndex = active ? 0 : -1;
      forms[key].hidden = !active;
      showError(key, null);
    }
    if (focus) firstEmpty(forms[name]).focus();
  }

  async function login() {
    const username = loginForm.username.value;
    if (!username.trim()) return showError('login', () => t('login.error.noUsername'), loginForm.username);

    const response = await send(forms.login, () => api.auth.login(username, loginForm.password.value));
    if (!response) return;
    if (response.success) return finish(response);
    if (response.result === 'UNKNOWN_USER') {
      select('register', { focus: false });
      registerForm.username.value = username.trim();
      return showError('register', authText(response), registerForm.password);
    }
    loginForm.password.value = '';
    showError('login', () => t('login.error.retype', { message: authText(response)() }), loginForm.password);
  }

  async function register() {
    const { username, password, repeat, displayName } = registerForm;
    if (password.value !== repeat.value) {
      repeat.value = '';
      return showError('register', () => t('login.error.mismatch'), repeat);
    }

    const response = await send(forms.register,
      () => api.auth.register(username.value, password.value, displayName.value));
    if (!response) return;
    if (response.success) return finish(response);
    if (response.result === 'DISPLAY_NAME_TOO_LONG') return showError('register', authText(response), displayName);
    if (response.result === 'USERNAME_TAKEN') {
      select('login', { focus: false });
      loginForm.username.value = username.value.trim();
      return showError('login', authText(response), loginForm.password);
    }
    if (response.result === 'PASSWORD_TOO_SHORT') {
      password.value = '';
      repeat.value = '';
      return showError('register', authText(response), password);
    }
    showError('register', authText(response), username);
  }

  /** Manda el pedido con el botón bloqueado; si falla la conexión, lo dice en el formulario. */
  async function send(form, call) {
    if (sending) return null;
    sending = true;
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    try {
      return await call();
    } catch (error) {
      showError(form === forms.login ? 'login' : 'register', () => t('login.error.network'));
      return null;
    } finally {
      sending = false;
      button.disabled = false;
    }
  }

  /** response trae username y displayName (el nombre para saludar, nunca el mail completo). */
  function finish({ username, displayName }) {
    for (const form of Object.values(forms)) form.reset();
    onLoggedIn({ username, displayName });
  }

  /** El mensaje de un AuthResult por su código; si el front no conoce el código, el texto del servidor. */
  function authText(response) {
    const key = `auth.${response.result}`;
    return () => (has(key) ? t(key) : response.message);
  }

  /** text: función que devuelve el mensaje en el idioma actual (null = sin error). */
  function showError(name, text, field) {
    errors[name] = text;
    const box = forms[name].querySelector('.form-error');
    const message = text?.() ?? '';
    box.textContent = message;
    for (const input of forms[name].querySelectorAll('input')) {
      input.toggleAttribute('aria-invalid', input === field);
    }
    if (field) {
      field.focus();
      field.select?.();
    }
  }

  function firstEmpty(form) {
    return [...form.querySelectorAll('input')].find((input) => !input.value) ?? form.querySelector('input');
  }

  return {
    element,
    enter() { select('login'); },
  };
}
