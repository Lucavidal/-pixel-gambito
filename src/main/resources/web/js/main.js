// main.js — conecta los módulos. El flujo de una jugada es siempre:
// clic -> pedir al backend -> animar lo que el backend aceptó -> sincronizar la vista con su estado.
// Las pantallas (inicio, inicio de sesión, menú, partida libre, torneo) las elige router.js. El tablero se ve
// en 3D (view3d.js) o en 2D (view2d.js): las dos vistas tienen la misma interfaz, así que la lógica
// de las jugadas está una sola vez acá, para la partida libre y para el torneo.
// El recorrido es inicio -> iniciar sesión -> menú -> partida libre o torneo. En el inicio (home.js) el
// tablero 3D se ve a la izquierda con una posición de muestra; detrás del inicio de sesión y del menú
// sigue girando de fondo, y al elegir un modo la cámara vuela hasta la vista de juego.
// Los textos salen de i18n.js (español / inglés) y los colores de theme.js (claro / oscuro):
// los controles de arriba a la derecha (preferences.js) cambian los dos sin recargar ni perder la partida.
import { t, has, translateDom, onLangChange } from './i18n.js';
import { createPreferences } from './preferences.js';
import { createFooter } from './footer.js';
import { createHud } from './hud.js';
import { api } from './api.js';
import { createRouter } from './router.js';
import { createLoginScreen } from './screens/login.js';
import { createMenuScreen } from './screens/menu.js';
import { createHistoryScreen } from './screens/history.js';
import { createHomeScreen } from './home.js';
import { createTournamentPanel } from './tournament-panel.js';
import { createView3d } from './view3d.js';
import { createView2d } from './view2d.js';

const VIEW_KEY = 'ajedrez.vista';   // preferencia 2D / 3D de este navegador
const THINKING_MS = 900;            // cuánto "piensa" la IA antes de jugar (como AI_THINKING_DELAY_MILLIS en Main)

translateDom();                     // el HTML ya en el idioma elegido, antes de que se vea
createPreferences();
createFooter();

const hud = createHud({ onUndo: undo, onRedo: redo, onReset: reset, onFlip: flip, onToggleView: toggleView });
const panel = createTournamentPanel({ onMenu: () => router.go('menu') });

const views = {
  '3d': createView3d({
    container: document.getElementById('viewport'),
    onSquareClick,
    onCancel: deselect,
    isLocked,
  }),
  '2d': createView2d({ container: document.getElementById('board2d'), onSquareClick }),
};
let mode = readPreference();

let user = null;         // { username } del que inició sesión, o null
let afterLogin = null;   // a qué pantalla se quería ir antes de que pidiera iniciar sesión
let booted = false;

const app = document.getElementById('app');
const gameScreen = document.getElementById('screen-game');
const home = createHomeScreen({ view: views['3d'], onPlay: playFromHome, setTransition, isBooting: () => !booted });
const router = createRouter({
  inicio: home,
  login: createLoginScreen({ onLoggedIn }),
  menu: createMenuScreen({ getUser: () => user, onLoggedOut }),
  historial: createHistoryScreen(),
  libre: { element: gameScreen, enter: () => enterMatch('libre'), leave: leaveMatch },
  torneo: { element: gameScreen, enter: () => enterMatch('torneo'), leave: leaveMatch },
}, {
  initial: 'inicio',
  // El inicio se ve siempre. Sin sesión, lo demás manda a iniciar sesión (y después sigue ahí);
  // con sesión, iniciar sesión manda al menú.
  resolve: (name) => {
    if (name === 'inicio') return name;
    if (!user) {
      if (name !== 'login') afterLogin = name;
      return 'login';
    }
    return name === 'login' ? 'menu' : name;
  },
});

document.getElementById('btn-home').addEventListener('click', () => router.go('inicio'));
document.getElementById('btn-menu').addEventListener('click', () => router.go('menu'));
document.getElementById('btn-history').addEventListener('click', () => router.go('historial'));

api.onUnauthorized(() => {
  if (!user) return;
  onLoggedOut();
  hud.toast(t('toast.sessionClosed'), 'info');
});

let match = null;        // qué se juega en el tablero: 'libre' | 'torneo' | null (no hay tablero en pantalla)
let state = null;        // último estado del tablero recibido del backend (la única verdad)
let tournament = null;   // último estado del torneo (escalera, ronda...), solo en el torneo
let selected = null;     // casilla seleccionada ("e2") o null
let selectToken = 0;     // descarta respuestas de movimientos legales que llegan tarde
let visit = 0;           // cambia al salir de la pantalla: corta el "pensando…" de una visita anterior
let busy = false;

boot();

async function boot() {
  try {
    await views['3d'].load((loaded, total) => hud.loading(loaded, total));
  } catch (error) {
    // Sin los modelos se puede jugar igual: queda solo la vista 2D.
    console.error(error);
    mode = '2d';
    hud.setViewToggle(null);
    hud.toast(t('toast.no3d'), 'info');
  }
  try {
    user = await api.auth.me();               // si ya había sesión (recargar la página), se sigue
  } catch (error) {
    console.error(error);
    hud.loadError('loader.serverError');
    return;
  }
  if (views['3d'].ready) hud.setViewToggle(mode);
  await router.start();
  booted = true;
  hud.loaded();
}

/** Mientras vuela la cámara no se puede tocar nada (inert también saca el foco del teclado). */
function setTransition(on) {
  app.inert = on;
}

/**
 * "Jugar": se desvanece el texto y se pasa al menú (sin sesión, el router manda antes a iniciar sesión).
 * La cámara sigue girando detrás; vuela a la vista de juego recién al elegir un modo en el menú.
 */
async function playFromHome() {
  setTransition(true);
  await home.fadeOut();
  router.go('menu');
}

// ---------- Sesión y pantallas ----------

function onLoggedIn(loggedUser) {
  user = loggedUser;
  const next = afterLogin ?? 'menu';
  afterLogin = null;
  router.go(next);
}

/** Al cerrar sesión, el tablero de fondo queda vacío: la partida era del usuario que se fue. */
function onLoggedOut() {
  user = null;
  afterLogin = null;
  deselect();
  state = null;
  tournament = null;
  for (const view of Object.values(views)) view.clear();
  router.go('login');
}

async function enterMatch(kind) {
  match = kind;
  state = null;
  hud.setMode(kind);
  panel.setVisible(kind === 'torneo');
  gameScreen.classList.add('pending');        // el HUD aparece recién con el estado del servidor
  const thisVisit = visit;
  try {
    if (views['3d'].isHome()) {
      setTransition(true);
      await views['3d'].toGame();             // vuelo desde la vista del inicio hasta la de juego
      if (thisVisit !== visit) return;
    }
    showViews();
    hud.setFlipLabel(activeView().getSide());
    if (kind === 'libre') {
      const fresh = await api.game.state();
      if (thisVisit !== visit) return;
      applyState(fresh);
      revealHud();
      return;
    }
    const current = await api.tournament.state();
    if (thisVisit !== visit) return;
    if (current.status === 'NOT_STARTED') {
      revealHud();
      // Todavía no se eligió la dificultad: tablero vacío y el selector
      tournament = current;
      for (const view of Object.values(views)) view.clear();
      hud.render({ turn: 'white', status: 'IN_PROGRESS', gameOver: false, history: [], canUndo: false, canRedo: false });
      hud.setTurnText('turn.chooseLevel');
      chooseLevel();
      return;
    }
    applyTournament(current);
    revealHud();
    // Si se recargó la página después de tu jugada, falta la respuesta de la IA
    if (current.awaitingOpponent) await opponentTurn();
  } catch (error) {
    if (thisVisit === visit) revealHud();
    if (error.status !== 401) hud.toast(error.message);
  }
}

function revealHud() {
  gameScreen.classList.remove('pending');
  setTransition(false);
}

function leaveMatch() {
  visit++;
  revealHud();
  deselect();
  panel.setVisible(false);
  panel.setThinking(null);
  match = null;
  setBusy(false);
  showViews();
}

/** En la partida se ve la vista elegida; en el inicio y el menú, el tablero 3D de fondo. */
function showViews() {
  const inGame = match !== null;
  views['3d'].setVisible(!inGame || mode === '3d');
  views['2d'].setVisible(inGame && mode === '2d');
}

function activeView() {
  return views[mode];
}

/** Las llamadas de la partida que está en pantalla. */
function matchApi() {
  return match === 'torneo' ? api.tournament : api.game;
}

// ---------- 2D / 3D ----------

function toggleView() {
  if (isLocked() || !views['3d'].ready) return;
  deselect();
  mode = mode === '3d' ? '2d' : '3d';
  savePreference(mode);
  showViews();
  hud.setViewToggle(mode);
  hud.setFlipLabel(activeView().getSide());
  if (state) activeView().sync(state, { animate: false });
}

// La preferencia es una comodidad de este navegador: si no se puede leer o guardar, se usa 3D.
function readPreference() {
  try {
    return localStorage.getItem(VIEW_KEY) === '2d' ? '2d' : '3d';
  } catch {
    return '3d';
  }
}

function savePreference(value) {
  try {
    localStorage.setItem(VIEW_KEY, value);
  } catch {
    // modo privado o almacenamiento bloqueado: no se recuerda, y no pasa nada
  }
}

// ---------- Estado ----------

/** Sincroniza las dos vistas con el estado del backend; solo la que se ve anima. */
function applyState(newState) {
  state = newState;
  for (const view of Object.values(views)) view.sync(state, { animate: view === activeView() });
  hud.render(state);
  if (match === 'torneo' && tournament) hud.setTurnText(...tournamentTurnText());
}

function applyTournament(newTournament) {
  tournament = newTournament;
  panel.render(tournament);
  applyState(tournament.board);
}

function isLocked() {
  return busy || views['3d'].isAnimating() || views['2d'].isAnimating() || panel.isRoundOpen()
    || panel.isLevelsOpen() || match === null || !state;
}

function setBusy(value) {
  busy = value;
  hud.setBusy(value);
}

// ---------- Selección ----------

function onSquareClick(square) {
  if (isLocked()) return;                     // la vista 3D ya lo filtra; la 2D no
  if (!state || !square) return deselect();
  if (selected && activeView().hasTarget(square)) return play(selected, square);
  const piece = state.pieces.find((p) => p.square === square);
  const own = match !== 'torneo' || piece?.color === tournament.userColor;
  if (piece && own && !state.gameOver && piece.color === state.turn) {
    return square === selected ? deselect() : select(square);
  }
  if (piece && !selected && !state.gameOver) {
    hud.toast(match === 'torneo'
      ? t('toast.rivalPiece', { rival: panel.rivalName(tournament), color: t(`color.${tournament.userColor}`) })
      : t('toast.notYourTurn', { color: t(`color.${state.turn}`) }), 'info');
  }
  deselect();
}

async function select(square) {
  deselect();
  selected = square;
  const view = activeView();
  view.select(square);

  const token = ++selectToken;
  try {
    const { moves } = await matchApi().legalMoves(square);   // los destinos los calcula el núcleo
    if (token !== selectToken) return;
    view.setTargets(moves);
    if (moves.length === 0) hud.toast(t('toast.noMoves'), 'info');
  } catch (error) {
    if (token === selectToken) hud.toast(error.message);
  }
}

function deselect() {
  selectToken++;
  selected = null;
  activeView().clearSelection();
}

// ---------- Jugadas ----------

/** POST del movimiento; si el backend lo acepta se anima, si no se muestra el motivo. */
async function play(from, to) {
  const view = activeView();
  selected = null;
  selectToken++;
  view.releaseSelection();
  setBusy(true);
  try {
    const response = await matchApi().move(from, to);
    const board = match === 'torneo' ? response.state.board : response.state;
    if (!response.success) {
      hud.toast(rejectionReason(response));  // MoveResult del núcleo, traducido por su código
      await view.lower(from);
      applyState(board);
      return;
    }
    await view.move(from, to);
    if (match === 'torneo') await afterTournamentMove(response);
    else applyState(board);
  } catch (error) {
    hud.toast(error.message);
    view.lower(from);
  } finally {
    setBusy(false);
  }
}

/** Deshacer: usa el MoveCommand del backend y anima la vuelta de la última jugada. Solo en la partida libre. */
async function undo() {
  if (match !== 'libre' || isLocked() || !state?.canUndo) return;
  deselect();
  setBusy(true);
  const last = state.history.at(-1);
  try {
    const response = await api.game.undo();
    if (response.success && last) await activeView().unmove(last);
    applyState(response.state);               // la pieza capturada reaparece desde el estado del backend
  } catch (error) {
    hud.toast(error.message);
  } finally {
    setBusy(false);
  }
}

async function redo() {
  if (match !== 'libre' || isLocked() || !state?.canRedo) return;
  deselect();
  setBusy(true);
  try {
    const response = await api.game.redo();
    const next = response.state.history.at(-1);
    if (response.success && next) await activeView().move(next.from, next.to);
    applyState(response.state);
  } catch (error) {
    hud.toast(error.message);
  } finally {
    setBusy(false);
  }
}

/** Partida nueva (libre); en el torneo, elegir la dificultad para uno nuevo. */
async function reset() {
  if (match === null || busy || views['3d'].isAnimating() || views['2d'].isAnimating()) return;
  deselect();
  panel.hideRound();
  if (match === 'torneo') return chooseLevel();
  setBusy(true);
  try {
    const fresh = (await api.game.reset()).state;
    activeView().clear();                     // vacía el tablero
    applyState(fresh);                        // y lo vuelve a armar (en 3D, con pop-in)
  } catch (error) {
    hud.toast(error.message);
  } finally {
    setBusy(false);
  }
}

async function flip() {
  if (isLocked()) return;
  setBusy(true);
  const side = await activeView().flip();
  hud.setFlipLabel(side);
  setBusy(false);
}

// ---------- Torneo ----------

/**
 * Selector de dificultad. Si ya hay un torneo en juego, "Cancelar" lo deja como estaba;
 * si no, el botón vuelve al menú.
 */
function chooseLevel() {
  const inProgress = state !== null && tournament?.status !== 'NOT_STARTED';
  panel.showLevels(tournament.levels, tournament.level, {
    onPick: startTournament,
    onCancel: inProgress ? () => {} : null,
  });
}

/** Torneo nuevo desde la ronda 1, con la escalera que arranca en esa dificultad. */
async function startTournament(level) {
  setBusy(true);
  try {
    const fresh = await api.tournament.start(level);
    activeView().clear();                     // vacía el tablero
    applyTournament(fresh);                   // y lo vuelve a armar (en 3D, con pop-in)
  } catch (error) {
    if (error.status !== 401) hud.toast(error.message);
  } finally {
    setBusy(false);
  }
}

/** [clave, variables] del turno en el torneo (hud.setTurnText la traduce, también al cambiar de idioma). */
function tournamentTurnText() {
  if (tournament.status === 'CHAMPION') return ['turn.champion'];
  if (tournament.status === 'ELIMINATED') return ['turn.eliminated'];
  if (tournament.awaitingOpponent) return ['turn.rival', { rival: panel.rivalName(tournament) }];
  return ['turn.yours', { color: t(`color.${tournament.userColor}`) }];
}

/**
 * El motivo de una jugada rechazada, por el CÓDIGO del MoveResult (OPPONENT_PIECE, LEAVES_KING_IN_CHECK...).
 * El texto en español del servidor se usa solo si llega un código que el front no conoce.
 */
function rejectionReason(response) {
  const key = `moveResult.${response.result}`;
  return has(key) ? t(key) : response.message;
}

/** Después de tu jugada: o terminó la ronda, o le toca a la IA. */
async function afterTournamentMove(response) {
  if (response.roundResult) return finishRound(response);
  applyTournament(response.state);
  if (response.state.awaitingOpponent) await opponentTurn();
}

/** La IA "piensa" un momento (como en la ventana Swing), juega, y se anima su jugada. */
async function opponentTurn() {
  const thisVisit = visit;
  const rival = panel.rivalName(tournament);
  setBusy(true);
  panel.setThinking(rival);
  hud.setTurnText('turn.thinking', { rival });
  try {
    await new Promise((resolve) => setTimeout(resolve, THINKING_MS));
    if (thisVisit !== visit) return;          // se fue de la pantalla: al volver se retoma
    const reply = await api.tournament.opponent();
    if (thisVisit !== visit) return;
    panel.setThinking(null);
    await activeView().move(reply.move.from, reply.move.to);
    if (reply.roundResult) return finishRound(reply);
    applyTournament(reply.state);
  } catch (error) {
    if (error.status !== 401) hud.toast(error.message);
  } finally {
    if (thisVisit === visit) {
      panel.setThinking(null);
      setBusy(false);
    }
  }
}

/**
 * Terminó la ronda. Si se ganó (y quedan rondas) o hubo tablas, el servidor ya armó la partida
 * siguiente: se deja a la vista la posición final y la partida nueva se muestra al tocar el botón.
 * Si el torneo terminó (eliminado o campeón), el estado del servidor ES la posición final.
 */
function finishRound(response) {
  const result = response.roundResult;
  const next = response.state;
  const over = next.status === 'ELIMINATED' || next.status === 'CHAMPION';
  if (over) {
    applyTournament(next);
  } else {
    // La planilla muestra la última jugada y el resultado, sobre la posición final que dejó la animación
    hud.render({
      ...state,
      history: [...state.history, response.move],
      status: result.outcome === 'DRAWN' ? 'STALEMATE' : 'CHECKMATE',
      gameOver: true,
      canUndo: false,
    });
    hud.setTurnText(result.outcome === 'DRAWN' ? 'turn.draw' : 'turn.roundOver');
  }
  panel.showRound(result, next, over
    ? { label: ['round.newTournament'], action: reset }   // abre el selector de dificultad
    : {
        label: result.outcome === 'DRAWN' ? ['round.repeat'] : ['round.next', { round: next.round }],
        action: () => {
          activeView().clear();
          applyTournament(next);
        },
      });
}

// Al cambiar de idioma, el texto del turno del torneo se vuelve a armar (incluye el color traducido)
onLangChange(() => {
  if (match === 'torneo' && tournament && state && !busy && !panel.isRoundOpen()) hud.setTurnText(...tournamentTurnText());
});

// Ayuda para pruebas automáticas y para depurar desde la consola del navegador.
// No tiene lógica de juego.
window.__chess3d = {
  screenOf: (square, height) => views['3d'].screenOf(square, height),
  idle: () => !isLocked() && state !== null,
  hasTarget: (square) => activeView().hasTarget(square),
  info: () => views['3d'].info(),
  state: () => state,
  tournament: () => tournament,
  mode: () => mode,
  match: () => match,
  camera: () => views['3d'].isHome() ? 'home' : 'game',
  decor: () => views['3d'].decorInfo(),
  /** Pide una jugada cualquiera al servidor por el mismo camino que un clic (para ver un rechazo y su motivo). */
  tryMove: (from, to) => play(from, to),
};
