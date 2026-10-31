// tournament-panel.js — lo que el torneo agrega al HUD: la escalera de rivales, la ficha del rival
// (estilo y dificultad explicados), "pensando…" mientras juega la IA y el aviso al terminar cada ronda.
// Como en TournamentWindow (Swing). Solo dibuja: qué pasa en el torneo lo decide el servidor.
// Los textos se arman con i18n.js a partir de los CÓDIGOS del servidor (estado, dificultad, estilo,
// resultado), así se pueden volver a dibujar en otro idioma sin pedirle nada al servidor.
import { t, has, formatList, onLangChange } from './i18n.js';

const $ = (id) => document.getElementById(id);

const STANDING_MARK = { won: '✓', playing: '●', lost: '✕', pending: '○' };

/** Nombre traducido de un código del servidor; si no lo conoce, el texto que mandó el servidor. */
function translated(prefix, code, fallback) {
  return code && has(`${prefix}.${code}`) ? t(`${prefix}.${code}`) : fallback;
}

export const difficultyName = (id, fallback) => translated('difficulty', id, fallback);
const styleName = (rival) => translated('style', rival.styleId, rival.style);
const styleDescription = (rival) => translated('style', rival.styleId && `${rival.styleId}.description`, rival.styleDescription);

/** "4 rondas: 2 de dificultad fácil y 2 de dificultad media." armado con los datos de la escalera. */
export function levelDescription(level) {
  if (!level.parts) return level.description;
  const parts = level.parts.map((part) => t('level.part', {
    count: part.count, difficulty: difficultyName(part.difficulty, part.difficulty).toLocaleLowerCase(),
  }));
  return t('level.description', { rounds: level.rounds, parts: formatList(parts) });
}

export function createTournamentPanel({ onMenu }) {
  const info = $('tournament-info');
  const banner = $('round-banner');
  const picker = $('level-picker');
  let nextAction = null;
  let cancelAction = null;
  // Lo último que se dibujó, para volver a dibujarlo al cambiar de idioma
  let lastTournament = null;
  let thinking = null;
  let lastRound = null;    // { result, tournament, next }
  let lastLevels = null;   // { levels, current, hasCancel, onPick }

  $('btn-level-cancel').addEventListener('click', () => {
    const action = cancelAction;
    hideLevels();
    action?.();
  });

  function hideLevels() {
    picker.hidden = true;
    cancelAction = null;
    lastLevels = null;
  }

  $('btn-round-next').addEventListener('click', () => {
    const action = nextAction;
    hideRound();
    action?.();
  });
  $('btn-round-menu').addEventListener('click', () => {
    hideRound();
    onMenu();
  });

  function hideRound() {
    banner.hidden = true;
    nextAction = null;
    lastRound = null;
  }

  /** El rival de la ronda actual (o, si el torneo terminó, contra el que terminó). */
  function currentRival(tournament) {
    const rivals = tournament.rivals;
    return rivals.find((r) => r.standing === 'playing' || r.standing === 'lost')
      ?? rivals[tournament.round - 1];
  }

  function render(tournament) {
    lastTournament = tournament;
    info.hidden = false;
    $('t-round').textContent = t('tournament.round', {
      round: tournament.round, total: tournament.totalRounds,
      level: difficultyName(tournament.level, tournament.levelName),
      status: translated('tournamentStatus', tournament.status, tournament.statusText),
    });

    const ladder = $('t-ladder');
    ladder.replaceChildren();
    tournament.rivals.forEach((rival, i) => {
      const li = document.createElement('li');
      li.className = `ladder-step ${rival.standing}`;
      li.innerHTML = '<span class="ladder-mark" aria-hidden="true"></span><span class="ladder-name"></span><span class="ladder-meta"></span>';
      li.querySelector('.ladder-mark').textContent = STANDING_MARK[rival.standing];
      li.querySelector('.ladder-name').textContent = `${i + 1}. ${rival.name}`;
      li.querySelector('.ladder-meta').textContent = rivalMeta(rival);
      li.setAttribute('aria-label', t('tournament.ladderStep', {
        round: i + 1, name: rival.name, standing: t(`standing.${rival.standing}`),
      }));
      ladder.append(li);
    });

    const rival = currentRival(tournament);
    $('t-rival-name').textContent = rival.name;
    $('t-rival-meta').textContent = rivalMeta(rival);
    $('t-rival-text').textContent = t('tournament.rivalText', {
      description: styleDescription(rival), percent: rival.randomMovePercent,
    });
  }

  const rivalMeta = (rival) => t('tournament.rivalMeta', {
    style: styleName(rival), difficulty: difficultyName(rival.difficultyId, rival.difficulty),
  });

  function renderThinking() {
    const line = $('t-thinking');
    line.hidden = !thinking;
    line.textContent = thinking ? t('tournament.thinking', { name: thinking }) : '';
  }

  function renderLevels() {
    const { levels, current, hasCancel, onPick } = lastLevels;
    const options = $('level-options');
    options.replaceChildren(...levels.map((level) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = `level-option ${level.id.toLowerCase()}`;
      button.setAttribute('aria-pressed', String(level.id === current));
      button.innerHTML = '<span class="level-name"></span><span class="level-text"></span>';
      button.querySelector('.level-name').textContent = difficultyName(level.id, level.name);
      button.querySelector('.level-text').textContent = levelDescription(level);
      button.addEventListener('click', () => {
        hideLevels();
        onPick(level.id);
      });
      return button;
    }));
    $('btn-level-cancel').textContent = t(hasCancel ? 'level.cancel' : 'level.backToMenu');
  }

  /**
   * El mensaje de fin de ronda (el mismo que la ventana Swing), armado con el resultado y el estado
   * nuevo del torneo: si se ganó, el estado ya está en la ronda siguiente con su rival.
   */
  function renderRound() {
    const { result, tournament, next } = lastRound;
    const champion = tournament.status === 'CHAMPION';
    $('round-title').textContent = t(`round.title.${champion ? 'CHAMPION' : result.outcome}`);
    const vars = { rival: result.rival, round: result.round, total: tournament.totalRounds };
    $('round-text').textContent = {
      WON: champion ? t('round.msg.champion', vars)
        : t('round.msg.won', { ...vars, next: tournament.round, nextRival: currentRival(tournament).name }),
      LOST: t('round.msg.lost', vars),
      DRAWN: t('round.msg.drawn', vars),
    }[result.outcome] ?? result.message;
    $('btn-round-next').textContent = t(...next.label);
  }

  onLangChange(() => {
    if (lastTournament && !info.hidden) render(lastTournament);
    renderThinking();
    if (lastLevels && !picker.hidden) renderLevels();
    if (lastRound && !banner.hidden) renderRound();
  });

  return {
    /** Al entrar al torneo el panel se muestra recién con render(): sin torneo no hay escalera ni rival. */
    setVisible(visible) {
      if (!visible) {
        info.hidden = true;
        hideRound();
        hideLevels();
      }
    },

    /**
     * Elegir desde qué dificultad empieza la escalera. Las opciones vienen del servidor.
     * onCancel null = no hay torneo en juego (el botón vuelve al menú).
     */
    showLevels(levels, current, { onPick, onCancel }) {
      hideRound();
      lastLevels = { levels, current, hasCancel: Boolean(onCancel), onPick };
      renderLevels();
      cancelAction = onCancel ?? onMenu;
      picker.hidden = false;
      const options = $('level-options');
      (options.querySelector('[aria-pressed="true"]') ?? options.firstChild)?.focus();
    },

    hideLevels,
    isLevelsOpen: () => !picker.hidden,

    render,

    rivalName: (tournament) => currentRival(tournament).name,

    /** "Capablanca está pensando…" (null lo saca). */
    setThinking(name) {
      thinking = name;
      renderThinking();
    },

    /**
     * Aviso de fin de ronda.
     * @param next { label: [clave, variables], action }: el botón principal (siguiente ronda, repetir, otro torneo)
     */
    showRound(result, tournament, next) {
      lastRound = { result, tournament, next };
      banner.classList.toggle('lost', result.outcome === 'LOST');
      banner.classList.toggle('champion', tournament.status === 'CHAMPION');
      renderRound();
      nextAction = next.action;
      banner.hidden = false;
      $('btn-round-next').focus();
    },

    hideRound,
    isRoundOpen: () => !banner.hidden,
  };
}
