// router.js — qué pantalla se ve, según el hash de la URL (#/login, #/menu, #/libre, #/torneo).
// Usar el hash hace que funcionen el botón "atrás" del navegador y recargar la página,
// sin que el servidor tenga que conocer las rutas del front.

/**
 * @param routes  { nombre: { element, enter?, leave? } }
 * @param resolve (nombre) => nombre al que hay que ir de verdad (para mandar al login si no hay sesión)
 */
export function createRouter(routes, { initial, resolve = (name) => name }) {
  let current = null;

  async function show() {
    const asked = location.hash.replace(/^#\/?/, '');
    const name = resolve(routes[asked] ? asked : initial);
    if (`#/${name}` !== location.hash) {
      history.replaceState(null, '', `#/${name}`); // sin agregar una entrada más al historial
    }
    if (name === current) return;
    const previous = current;
    current = name;
    if (previous) routes[previous].leave?.();
    // Dos rutas pueden compartir la pantalla (libre y torneo usan el mismo tablero)
    const visible = routes[name].element;
    for (const route of Object.values(routes)) route.element.hidden = route.element !== visible;
    await routes[name].enter?.();
  }

  window.addEventListener('hashchange', show);

  return {
    start: show,
    current: () => current,
    go(name) {
      if (location.hash === `#/${name}`) show();
      else location.hash = `#/${name}`;
    },
  };
}
