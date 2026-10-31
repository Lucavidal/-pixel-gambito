// theme.js — tema claro / oscuro de toda la página. El CSS lo lee de <html data-theme="...">
// y la escena 3D (scene.js, board.js) se suscribe con onThemeChange() para cambiar fondo, luces y marco.
// El primer valor ya lo puso el script chico del <head> de index.html (así no hay un destello del
// otro tema mientras cargan los módulos); acá se lee de ahí.

const KEY = 'ajedrez.tema';
const SWITCH_MS = 250;   // un poco más que la transición de 200 ms del CSS

const listeners = new Set();
const systemLight = window.matchMedia?.('(prefers-color-scheme: light)');
const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
let current = document.documentElement.dataset.theme === 'light' ? 'light' : 'dark';
let switchTimer = 0;

// Si la persona nunca eligió, se sigue al sistema también cuando cambia (por ejemplo, de noche)
systemLight?.addEventListener?.('change', (event) => {
  if (readStored() === null) apply(event.matches ? 'light' : 'dark');
});

/** 'dark' | 'light' */
export function getTheme() {
  return current;
}

/** Elige el tema y lo recuerda en este navegador. */
export function setTheme(theme) {
  store(theme);
  apply(theme);
}

export function toggleTheme() {
  setTheme(current === 'dark' ? 'light' : 'dark');
}

/** fn(tema) en cada cambio. Devuelve la función para desuscribirse. */
export function onThemeChange(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

function apply(theme) {
  if (theme !== 'dark' && theme !== 'light') return;
  if (theme === current) return;
  current = theme;
  const root = document.documentElement;
  // La transición suave de colores dura solo mientras se cambia de tema (no molesta al resto del tiempo)
  if (!reducedMotion) {
    root.classList.add('theme-switching');
    clearTimeout(switchTimer);
    switchTimer = setTimeout(() => root.classList.remove('theme-switching'), SWITCH_MS);
  }
  root.dataset.theme = theme;
  root.style.colorScheme = theme;
  for (const fn of listeners) fn(theme);
}

// La preferencia es una comodidad: si el navegador bloquea el almacenamiento, la página anda igual.
function readStored() {
  try {
    const value = localStorage.getItem(KEY);
    return value === 'dark' || value === 'light' ? value : null;
  } catch {
    return null;
  }
}

function store(theme) {
  try {
    localStorage.setItem(KEY, theme);
  } catch {
    // modo privado o almacenamiento bloqueado: no se recuerda, y no pasa nada
  }
}
