// view3d.js — el tablero 3D (Three.js) detrás de la interfaz de "vista" que usa main.js.
// Es lo mismo que antes hacía main.js con la escena; ahora main.js también puede usar view2d.js.
//
// Interfaz común de las vistas (la misma en view2d.js):
//   sync(state, { animate })   la escena coincide con el estado del backend (piezas, jaque, última jugada)
//   clear()                    tablero vacío
//   select(square) / releaseSelection() / clearSelection() / lower(square)
//   setTargets(moves) / hasTarget(square)
//   move(from, to) / unmove(lastMove)   animan; el estado nuevo lo aplica después main.js con sync()
//   flip() -> lado ('white' | 'black'), getSide(), isAnimating(), setVisible(visible)
// Solo en 3D: toGame() / toHome() (vuelo de la cámara entre el inicio y la partida) y hop(square).
import * as THREE from 'three';
import { createScene } from './scene.js';
import { createBoard, squareToWorld } from './board.js';
import { loadModels, PieceSet } from './pieces.js';
import { createDecor } from './decor.js';
import { createInput } from './input.js';
import { updateTweens, isAnimating, animateMove, animateCapture, animateLift } from './anim.js';

const SELECT_LIFT = 0.15;
const HOP_HEIGHT = 0.1;   // saltito idle de las piezas del inicio
const EMPTY = { pieces: [], capturedWhite: [], capturedBlack: [], checkedKing: null, history: [] };

export function createView3d({ container, onSquareClick, onCancel, isLocked }) {
  const view = createScene(container);
  const board = createBoard(view.scene);
  let pieces = null;
  let input = null;
  let selected = null;     // casilla de la pieza levantada
  let visible = true;

  let decor = null;          // piezas decorativas del inicio (decor.js), cuando cargaron los modelos

  // Loop de render: tweens, pulso del jaque, cámara (con damping en la partida, girando en el inicio).
  // Oculta, solo avanzan los tweens (que ninguna animación quede a medias) y no se dibuja nada.
  // Las estadísticas (draw calls) se cuentan por frame: tablero + decoración.
  view.renderer.info.autoReset = false;
  function frame() {
    const now = performance.now();
    updateTweens(now);
    if (!visible) return;
    view.renderer.info.reset();
    board.update(now);
    view.update(now);
    view.renderer.render(view.scene, view.camera);
    decor?.update(now);       // segunda pasada en el mismo canvas (solo si se ve)
  }
  view.renderer.setAnimationLoop(frame);

  function sync(state, { animate = true } = {}) {
    if (!pieces) return;                       // los modelos todavía no cargaron (o fallaron)
    pieces.sync(state, { animateNew: animate });
    board.setCheck(state.checkedKing);
    board.setLastMove(state.history.at(-1) ?? null);
    input.resetHover();
  }

  function releaseSelection() {
    selected = null;
    input?.clearSelection();
  }

  return {
    name: '3d',
    /** ¿Están los modelos .glb? Sin ellos la vista 3D no se puede usar (queda la 2D). */
    get ready() { return pieces !== null; },

    /** Carga los 12 modelos. onProgress(cargados, total) alimenta la barra de carga. */
    async load(onProgress) {
      const manager = new THREE.LoadingManager();
      manager.onProgress = (_url, loaded, total) => onProgress(loaded, total);
      const templates = await loadModels(manager);
      pieces = new PieceSet(view.scene, templates);
      decor = createDecor({ renderer: view.renderer, templates, container });
      input = createInput({
        camera: view.camera,
        domElement: view.renderer.domElement,
        board,
        pieces,
        onSquareClick: (square) => onSquareClick(square),
        onCancel,
        isLocked,
      });
    },

    /** Oculta, la escena deja de dibujarse: no gasta la placa de video mientras se juega en 2D. */
    setVisible(value) {
      visible = value;
      container.hidden = !value;
      if (value) view.resize(); // la ventana pudo cambiar de tamaño mientras estaba oculta
    },

    sync,
    clear() { sync(EMPTY, { animate: false }); },

    select(square) {
      if (selected && selected !== square) animateLift(pieces.at(selected), 0);
      selected = square;
      const object = pieces.at(square);
      animateLift(object, SELECT_LIFT);
      input.select(object, square);
    },

    /** Saca los resaltados pero deja la pieza levantada (se va a mover o a bajar con lower()). */
    releaseSelection,

    /** Saca los resaltados y baja la pieza. */
    clearSelection() {
      const object = selected && pieces?.at(selected);
      if (object) animateLift(object, 0);
      releaseSelection();
    },

    async lower(square) {
      const object = pieces?.at(square);
      if (object) await animateLift(object, 0);
    },

    setTargets(moves) { input.setTargets(moves); },
    hasTarget: (square) => input?.hasTarget(square) ?? false,

    /** Anima la pieza de `from` a `to` y, si había una rival en `to`, la captura. */
    async move(from, to) {
      const mover = pieces.at(from);
      if (!mover) return;
      const victim = pieces.at(to);
      board.setHover(null);
      await Promise.all([
        animateMove(mover, squareToWorld(to), { jumpy: mover.userData.type === 'knight' }),
        victim ? animateCapture(victim, { delay: 160 }) : null,
      ]);
      if (victim) pieces.discard(victim);
      pieces.rekey(from, to);
    },

    /** Deshacer: la pieza vuelve; la capturada reaparece con el sync() del estado del backend. */
    async unmove(last) {
      const object = pieces.at(last.to);
      if (!object) return;
      await animateMove(object, squareToWorld(last.from), { jumpy: object.userData.type === 'knight', duration: 300 });
      pieces.rekey(last.to, last.from);
    },

    flip: () => view.flipCamera(),

    /** Piezas decorativas alrededor del menú del inicio (anchor = la columna de texto). */
    showDecor: (anchor) => decor?.show(anchor),
    hideDecor: () => decor?.hide(),
    decorInfo: () => decor?.debug(),

    /** Vuelo de la cámara a la vista de juego (lado blancas); la cámara vuelve a ser de OrbitControls. */
    toGame: () => view.toGame(),
    /** Vuelo a la vista del inicio (giro lento, sin OrbitControls). */
    toHome(options) {
      board.setHover(null);
      return view.toHome(options);
    },
    /** ¿La cámara está en el inicio (o volando)? */
    isHome: () => view.cameraMode() !== 'game',

    /** Saltito sin cambiar de casilla: sube y baja HOP_HEIGHT. */
    async hop(square) {
      const object = pieces?.at(square);
      if (!object || object.position.y !== 0) return;
      await animateLift(object, HOP_HEIGHT);
      await animateLift(object, 0);
    },
    getSide: () => view.getSide(),
    isAnimating,

    /** Para pruebas automáticas: proyecta una casilla a coordenadas de pantalla. */
    screenOf(square, height = 0) {
      const point = squareToWorld(square);
      point.y = height;
      point.project(view.camera);
      const rect = view.renderer.domElement.getBoundingClientRect();
      return { x: rect.left + (point.x + 1) / 2 * rect.width, y: rect.top + (1 - point.y) / 2 * rect.height };
    },
    info: () => ({ ...view.renderer.info.render, geometries: view.renderer.info.memory.geometries }),
  };
}
