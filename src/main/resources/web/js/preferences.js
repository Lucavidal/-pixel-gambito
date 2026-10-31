// preferences.js — los dos controles de arriba a la derecha: modo nocturno (claro / oscuro) e idioma.
// Solo conecta los botones con theme.js e i18n.js: qué cambia en cada pantalla lo decide cada módulo.
import { getTheme, toggleTheme, onThemeChange } from './theme.js';
import { getLang, setLang, onLangChange, t } from './i18n.js';

const $ = (id) => document.getElementById(id);

export function createPreferences() {
  const themeButton = $('btn-theme');
  const langButtons = [...document.querySelectorAll('#lang-switch .lang-option')];

  themeButton.addEventListener('click', toggleTheme);
  for (const button of langButtons) {
    button.addEventListener('click', () => setLang(button.dataset.lang));
  }

  /** El botón dice lo que HACE (pasar al otro tema), como pide un botón con aria-label. */
  function renderTheme() {
    const toLight = getTheme() === 'dark';
    themeButton.setAttribute('aria-label', t(toLight ? 'prefs.theme.toLight' : 'prefs.theme.toDark'));
    themeButton.title = themeButton.getAttribute('aria-label');
  }

  function renderLang() {
    for (const button of langButtons) button.setAttribute('aria-pressed', String(button.dataset.lang === getLang()));
    renderTheme();   // su texto también cambia de idioma
  }

  onThemeChange(renderTheme);
  onLangChange(renderLang);
  renderLang();
}
