// home.js — pantalla de inicio: el tablero 3D de la escena con una posición fija de muestra,
// el título y el botón para jugar. No es una partida: no se consulta ni se calcula ninguna regla.
// El vuelo de la cámara lo hace view3d (scene.js); qué pasa al tocar "Jugar" lo decide main.js.
import { reducedMotion } from './anim.js';

const $ = (id) => document.getElementById(id);

const FADE_MS = 250;
const HOP_EVERY_MS = [3000, 4000];

/** Medio juego para lucir los modelos: hay piezas de todos los tipos. Son solo datos. */
const SHOWCASE = [
  { square: 'g1', type: 'king', color: 'white' },
  { square: 'd2', type: 'queen', color: 'white' },
  { square: 'a1', type: 'rook', color: 'white' },
  { square: 'f1', type: 'rook', color: 'white' },
  { square: 'b3', type: 'bishop', color: 'white' },
  { square: 'g5', type: 'bishop', color: 'white' },
  { square: 'c3', type: 'knight', color: 'white' },
  { square: 'f3', type: 'knight', color: 'white' },
  { square: 'a3', type: 'pawn', color: 'white' },
  { square: 'b2', type: 'pawn', color: 'white' },
  { square: 'd3', type: 'pawn', color: 'white' },
  { square: 'e4', type: 'pawn', color: 'white' },
  { square: 'f2', type: 'pawn', color: 'white' },
  { square: 'g2', type: 'pawn', color: 'white' },
  { square: 'h3', type: 'pawn', color: 'white' },

  { square: 'g8', type: 'king', color: 'black' },
  { square: 'e7', type: 'queen', color: 'black' },
  { square: 'a8', type: 'rook', color: 'black' },
  { square: 'f8', type: 'rook', color: 'black' },
  { square: 'c5', type: 'bishop', color: 'black' },
  { square: 'e6', type: 'bishop', color: 'black' },
  { square: 'c6', type: 'knight', color: 'black' },
  { square: 'f6', type: 'knight', color: 'black' },
  { square: 'a7', type: 'pawn', color: 'black' },
  { square: 'b6', type: 'pawn', color: 'black' },
  { square: 'd6', type: 'pawn', color: 'black' },
  { square: 'e5', type: 'pawn', color: 'black' },
  { square: 'f7', type: 'pawn', color: 'black' },
  { square: 'g7', type: 'pawn', color: 'black' },
  { square: 'h6', type: 'pawn', color: 'black' },
];

/** Misma forma que el estado del backend, así PieceSet.sync() la dibuja sin código aparte. */
const SHOWCASE_STATE = { pieces: SHOWCASE, capturedWhite: [], capturedBlack: [], checkedKing: null, history: [] };

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * @param view          la vista 3D (sync, toHome, hop)
 * @param onPlay        "Jugar"
 * @param setTransition bloquea los clics mientras vuela la cámara
 * @param isBooting     true mientras arranca la página: la cámara se ubica sin volar
 */
export function createHomeScreen({ view, onPlay, setTransition, isBooting }) {
  const element = $('screen-home');
  const copy = $('home-copy');
  const howto = $('howto');
  let hopTimer = 0;
  let visit = 0;

  $('btn-play').addEventListener('click', () => onPlay());
  $('btn-howto').addEventListener('click', () => howto.showModal());
  $('btn-howto-close').addEventListener('click', () => howto.close());
  howto.addEventListener('click', (event) => {
    if (event.target === howto) howto.close();   // clic en el fondo oscuro
  });

  /** Cada 3-4 s una pieza al azar da un saltito (sin cambiar de casilla). */
  function scheduleHop() {
    if (reducedMotion) return;
    const [min, max] = HOP_EVERY_MS;
    hopTimer = setTimeout(() => {
      if (!document.hidden) view.hop(SHOWCASE[Math.floor(Math.random() * SHOWCASE.length)].square);
      scheduleHop();
    }, min + Math.random() * (max - min));
  }

  return {
    element,

    async enter() {
      const thisVisit = ++visit;
      const animate = !isBooting();
      copy.classList.add('faded');        // el texto aparece cuando llega la cámara
      if (animate) setTransition(true);
      view.sync(SHOWCASE_STATE, { animate: true });
      await view.toHome({ animate });
      if (thisVisit !== visit) return;
      copy.classList.remove('faded');
      view.showDecor(copy);   // las piezas de los costados aparecen con el texto
      setTransition(false);
      scheduleHop();
    },

    leave() {
      visit++;
      view.hideDecor();
      clearTimeout(hopTimer);
      if (howto.open) howto.close();
      setTransition(false);
    },

    /** Desvanece el texto antes de que la cámara vuele a la partida. */
    fadeOut() {
      copy.classList.add('faded');
      view.hideDecor();       // se desvanecen junto con el texto, antes del vuelo de la cámara
      return wait(reducedMotion ? 0 : FADE_MS);
    },
  };
}
