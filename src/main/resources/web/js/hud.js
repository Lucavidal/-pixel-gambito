// hud.js — la capa HTML encima del canvas: turno, jaque, historial, botones, avisos y carga.
// Todos los textos salen de i18n.js; lo que manda el servidor se traduce por su código (status, color...).
import { t, moveText, onLangChange } from './i18n.js';

const $ = (id) => document.getElementById(id);

export function createHud({ onUndo, onRedo, onReset, onFlip, onToggleView }) {
  $('btn-undo').addEventListener('click', onUndo);
  $('btn-redo').addEventListener('click', onRedo);
  $('btn-reset').addEventListener('click', onReset);
  $('btn-banner-reset').addEventListener('click', onReset);
  $('btn-flip').addEventListener('click', onFlip);
  $('btn-view').addEventListener('click', onToggleView);

  // Atajos de teclado como en la versión Swing
  window.addEventListener('keydown', (event) => {
    if (!(event.ctrlKey || event.metaKey)) return;
    if (event.target.closest?.('input, textarea')) return; // en un campo de texto, Ctrl+Z es del campo
    const key = event.key.toLowerCase();
    if (key === 'z' && !event.shiftKey) { event.preventDefault(); onUndo(); }
    if (key === 'y' || (key === 'z' && event.shiftKey)) { event.preventDefault(); onRedo(); }
  });

  let toastTimer = 0;
  let busy = false;
  let lastState = null;
  let mode = 'libre';      // 'libre' | 'torneo'
  let flipSide = 'white';
  let viewMode = '3d';     // null = no hay vista 3D
  let turnOverride = null; // { key, vars }: el torneo reemplaza el "Turno de las blancas"
  let loadingProgress = null;

  function render(state) {
    lastState = state;
    turnOverride = null;
    // Turno
    $('turn-dot').classList.toggle('black', state.turn === 'black');
    renderTurn();

    // Jaque / mate / ahogado (todo lo decide el backend; acá solo se traduce el código)
    const status = $('status-text');
    const inCheck = state.status === 'CHECK';
    status.textContent = state.status === 'IN_PROGRESS' ? ''
      : t(`status.${state.status}`, { color: t(`kingColor.${state.turn}`) });
    status.classList.toggle('check', inCheck || state.status === 'CHECKMATE');

    // Historial "1. e2-e4  e7-e5", escrito en el idioma actual
    const list = $('history');
    list.replaceChildren();
    const moves = state.history;
    for (let i = 0; i < moves.length; i += 2) {
      const li = document.createElement('li');
      const n = document.createElement('span');
      n.className = 'n';
      n.textContent = `${i / 2 + 1}.`;
      li.append(n, cell(moves[i], i === moves.length - 1), cell(moves[i + 1], i + 1 === moves.length - 1));
      list.append(li);
    }
    $('history-empty').hidden = moves.length > 0;
    $('last-move').textContent = moves.length ? t('game.lastMove', { move: moveText(moves.at(-1)) }) : '';
    list.scrollTop = list.scrollHeight;

    // Fin de partida (en el torneo lo avisa el cartel de fin de ronda)
    const banner = $('banner');
    if (mode === 'torneo') {
      banner.hidden = true;
    } else if (state.status === 'CHECKMATE') {
      const winner = state.turn === 'white' ? 'black' : 'white';
      $('banner-title').textContent = t('banner.mate.title');
      $('banner-sub').textContent = t('banner.mate.sub', { color: t(`color.${winner}`) });
      banner.hidden = false;
    } else if (state.status === 'STALEMATE') {
      $('banner-title').textContent = t('banner.stalemate.title');
      $('banner-sub').textContent = t('banner.stalemate.sub', { color: t(`color.${state.turn}`) });
      banner.hidden = false;
    } else {
      banner.hidden = true;
    }
    refreshButtons();
  }

  function renderTurn() {
    if (turnOverride) {
      $('turn-text').textContent = t(turnOverride.key, turnOverride.vars);
    } else if (lastState) {
      $('turn-text').textContent = lastState.gameOver ? t('turn.over') : t('turn.player', { color: t(`color.${lastState.turn}`) });
    }
  }

  function cell(move, isLast) {
    const span = document.createElement('span');
    if (move) {
      span.textContent = moveText(move);
      if (isLast) span.className = 'last';
    }
    return span;
  }

  function refreshButtons() {
    $('btn-undo').disabled = busy || !lastState?.canUndo;
    $('btn-redo').disabled = busy || !lastState?.canRedo;
    $('btn-reset').disabled = busy;
    $('btn-flip').disabled = busy;
    $('btn-view').disabled = busy;
  }

  function renderLabels() {
    const tournament = mode === 'torneo';
    $('game-eyebrow').textContent = t(tournament ? 'game.eyebrow.torneo' : 'game.eyebrow.libre');
    $('btn-reset').textContent = t(tournament ? 'game.resetTournament' : 'game.reset');
    $('btn-flip').textContent = t(flipSide === 'white' ? 'game.flip.toBlack' : 'game.flip.toWhite');
    const view = $('btn-view');
    view.hidden = viewMode === null;
    view.textContent = t(viewMode === '2d' ? 'game.view.to3d' : 'game.view.to2d');
    view.setAttribute('aria-pressed', String(viewMode === '2d'));
    if (loadingProgress) $('loader-text').textContent = t('loader.progress', loadingProgress);
  }

  // Al cambiar de idioma se redibuja todo con el último estado: no se pierde nada ni se recarga
  onLangChange(() => {
    renderLabels();
    if (lastState) {
      const override = turnOverride;
      render(lastState);
      turnOverride = override;
      renderTurn();
    }
  });
  renderLabels();

  return {
    render,
    /** Mientras se anima o se espera al servidor, los botones quedan bloqueados. */
    setBusy(value) { busy = value; refreshButtons(); },
    /** Partida libre o torneo: en el torneo no se deshace (la IA ya respondió) y se reinicia el torneo entero. */
    setMode(value) {
      mode = value;
      const tournament = value === 'torneo';
      $('btn-undo').hidden = tournament;
      $('btn-redo').hidden = tournament;
      $('btn-history').hidden = !tournament;   // como en la ventana del torneo (Swing)
      $('banner').hidden = true;
      renderLabels();
    },
    /** Reemplaza el "Turno de las blancas" (el torneo dice "Tu turno" o "Piensa Capablanca…"): clave de i18n. */
    setTurnText(key, vars = {}) {
      turnOverride = { key, vars };
      renderTurn();
    },
    setFlipLabel(side) { flipSide = side; renderLabels(); },
    /** El botón ofrece la OTRA vista; null = no hay vista 3D (no cargaron los modelos) y se oculta. */
    setViewToggle(value) { viewMode = value; renderLabels(); },
    /** Un aviso ya traducido (el que llama usa t() o el texto de un código desconocido del servidor). */
    toast(message, kind = 'error') {
      const el = $('toast');
      el.textContent = message;
      el.className = `toast show ${kind}`;
      clearTimeout(toastTimer);
      toastTimer = setTimeout(() => { el.className = `toast ${kind}`; }, 2800);
    },
    loading(loaded, total) {
      loadingProgress = { loaded, total };
      $('loader-fill').style.width = `${Math.round((loaded / Math.max(total, 1)) * 100)}%`;
      $('loader-text').textContent = t('loader.progress', loadingProgress);
    },
    loaded() { loadingProgress = null; $('loader').classList.add('done'); },
    loadError(key) {
      loadingProgress = null;
      $('loader').classList.add('error');
      $('loader-text').removeAttribute('data-i18n');
      $('loader-text').dataset.i18n = key;   // así también cambia de idioma
      $('loader-text').textContent = t(key);
    },
  };
}
