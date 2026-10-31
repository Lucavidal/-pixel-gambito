// menu.js — menú principal: saludo, opciones y "Qué incluye el juego".
// Mismas opciones que MainMenuWindow (Swing): la tarjeta del torneo muestra la escalera de rivales
// y debajo del saludo va tu récord en el torneo. Los textos se arman con i18n.js a partir de los
// datos del servidor (códigos y números), así se redibujan al cambiar de idioma.
import { api } from '../api.js';
import { t, has, formatList, onLangChange } from '../i18n.js';
import { difficultyName } from '../tournament-panel.js';
import { recordText } from './history.js';

const $ = (id) => document.getElementById(id);

export function createMenuScreen({ getUser, onLoggedOut }) {
  const element = $('screen-menu');
  const features = $('features');
  let tournament = null;   // lo último que mandó el servidor
  let summary = null;
  let visible = false;

  $('btn-features').addEventListener('click', () => {
    features.showModal();
    features.scrollTop = 0; // el foco cae en "Cerrar" (abajo de todo); se arranca leyendo desde arriba
  });
  $('btn-features-close').addEventListener('click', () => features.close());
  // Clic en el fondo oscuro (fuera de la tarjeta) también cierra
  features.addEventListener('click', (event) => {
    if (event.target === features) features.close();
  });

  $('btn-logout').addEventListener('click', async () => {
    try {
      await api.auth.logout();
    } finally {
      onLoggedOut(); // aunque falle la conexión, el front se olvida del usuario
    }
  });

  function renderGreeting() {
    const user = getUser();
    if (!user) return;
    // El servidor manda el nombre visible (nunca el mail completo)
    $('menu-greeting').textContent = t('menu.greeting', { name: user.displayName ?? user.username });
  }

  /** Escalera del torneo en la tarjeta y en "Qué incluye el juego". */
  function renderTournament() {
    if (!tournament) return;
    // Sin torneo empezado, la escalera todavía no existe: depende de la dificultad que se elija
    const ladder = $('menu-ladder');
    if (tournament.status === 'NOT_STARTED') {
      const li = document.createElement('li');
      li.className = 'hint';
      li.textContent = t('menu.tournament.levelsHint', {
        levels: formatList(tournament.levels.map((l) => difficultyName(l.id, l.name))),
      });
      ladder.replaceChildren(li);
    } else {
      ladder.replaceChildren(...tournament.rivals.map((rival, i) => {
        const li = document.createElement('li');
        li.className = rival.standing;
        li.textContent = `${i + 1}   ${rival.name}`;
        return li;
      }));
    }
    $('tournament-cta').textContent = t(`menu.tournament.cta.${tournament.status}`, {
      round: tournament.round, level: difficultyName(tournament.level, tournament.levelName),
    });

    // Todo el plantel, agrupado por dificultad
    const byLevel = new Map();
    for (const rival of tournament.allRivals) {
      if (!byLevel.has(rival.difficultyId)) byLevel.set(rival.difficultyId, []);
      byLevel.get(rival.difficultyId).push(rival);
    }
    $('features-rivals').replaceChildren(...[...byLevel].map(([difficulty, rivals]) => {
      const li = document.createElement('li');
      li.innerHTML = '<b></b> <span></span>';
      li.querySelector('b').textContent = `${difficultyName(difficulty, rivals[0].difficulty)}:`;
      li.querySelector('span').textContent = formatList(rivals.map((r) => t('features.rival', {
        name: r.name,
        style: (r.styleId && has(`style.${r.styleId}`) ? t(`style.${r.styleId}`) : r.style).toLocaleLowerCase(),
      }))) + '.';
      return li;
    }));
  }

  async function loadTournament() {
    try {
      tournament = await api.tournament.state();
    } catch {
      return;   // sin la escalera el menú sirve igual
    }
    renderTournament();
  }

  /** "Tu récord en el torneo: 3 ganadas · 1 tablas · 2 perdidas" debajo del saludo. */
  async function loadRecord() {
    summary = null;
    $('menu-record').textContent = '';
    try {
      summary = (await api.history(1)).summary;
    } catch {
      return;   // sin el récord el menú sirve igual
    }
    $('menu-record').textContent = recordText(summary);
  }

  onLangChange(() => {
    if (!visible) return;
    renderGreeting();
    renderTournament();
    if (summary) $('menu-record').textContent = recordText(summary);
  });

  return {
    element,
    enter() {
      visible = true;
      renderGreeting();
      $('tile-free').focus({ preventScroll: true });
      loadTournament();
      loadRecord();
    },
    leave() {
      visible = false;
      if (features.open) features.close();
    },
  };
}
