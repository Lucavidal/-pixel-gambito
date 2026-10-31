// view2d.js — el tablero 2D, como el de la ventana Swing (BoardPanel): casillas verde y crema,
// figuras Unicode, punto = casilla libre, anillo = captura, última jugada en amarillo y rey en jaque en rojo.
// Misma interfaz de "vista" que view3d.js (ver el comentario de arriba de ese archivo).
//
// Son botones HTML, no un canvas: se juega también con el teclado (flechas para moverse,
// Enter o Espacio para elegir) y un lector de pantalla dice qué hay en cada casilla (en el idioma elegido).
import { t, onLangChange } from './i18n.js';

const FILES = 'abcdefgh';
const SIZE = 8;
const SLIDE_MS = 200;

// Igual que PieceGlyphs (Swing): figura según el símbolo del núcleo; una pieza nueva se dibuja con su letra.
// ︎ pide la versión "texto" del símbolo (sin ella, algunos sistemas dibujan el peón negro como emoji).
const GLYPHS = {
  white: { R: '♔', D: '♕', T: '♖', A: '♗', C: '♘', P: '♙' },
  black: { R: '♚', D: '♛', T: '♜', A: '♝', C: '♞', P: '♟' },
};

const PIECE_KEYS = { king: 1, queen: 1, rook: 1, bishop: 1, knight: 1, pawn: 1 };

const reducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;

export function glyphOf(piece) {
  const symbol = piece.symbol.toUpperCase();
  return (GLYPHS[piece.color][symbol] ?? symbol) + '︎';
}

export function createView2d({ container, onSquareClick }) {
  const grid = document.createElement('div');
  grid.className = 'board2d-grid';
  grid.setAttribute('role', 'grid');
  grid.setAttribute('aria-label', t('board2d.aria'));
  container.append(grid);
  onLangChange(() => {
    grid.setAttribute('aria-label', t('board2d.aria'));
    if (state) render();
  });

  const cells = new Map();       // "e2" -> <button>
  let side = 'white';            // abajo de todo
  let state = null;
  let selected = null;
  let targets = new Map();       // casilla -> ¿es captura?
  let cursor = 'e2';             // casilla que recibe el foco con Tab (roving tabindex)
  let animating = 0;

  build();

  /** Arma las 64 casillas en el orden en que se ven desde `side`. */
  function build() {
    grid.replaceChildren();
    cells.clear();
    for (let r = 0; r < SIZE; r++) {
      const row = document.createElement('div');
      row.className = 'board2d-row';
      row.setAttribute('role', 'row');
      for (let c = 0; c < SIZE; c++) {
        const file = side === 'white' ? c : SIZE - 1 - c;
        const rank = side === 'white' ? SIZE - 1 - r : r;
        const square = FILES[file] + (rank + 1);
        const cell = document.createElement('button');
        cell.type = 'button';
        cell.className = `sq ${(file + rank) % 2 === 0 ? 'dark' : 'light'}`;
        cell.dataset.square = square;
        cell.setAttribute('role', 'gridcell');
        cell.tabIndex = square === cursor ? 0 : -1;
        cell.innerHTML = '<span class="glyph" aria-hidden="true"></span>';
        // Coordenadas como en un tablero de verdad: letras en la fila de abajo, números en la columna izquierda
        if (r === SIZE - 1) cell.insertAdjacentHTML('beforeend', `<span class="coord file" aria-hidden="true">${FILES[file]}</span>`);
        if (c === 0) cell.insertAdjacentHTML('beforeend', `<span class="coord rank" aria-hidden="true">${rank + 1}</span>`);
        cell.addEventListener('click', () => {
          focusSquare(square, false);
          onSquareClick(square);
        });
        row.append(cell);
        cells.set(square, cell);
      }
      grid.append(row);
    }
    if (state) render();
  }

  // Flechas: se mueve el foco según cómo se VE el tablero (arriba = hacia el lado de enfrente)
  grid.addEventListener('keydown', (event) => {
    const step = { ArrowUp: [0, 1], ArrowDown: [0, -1], ArrowLeft: [-1, 0], ArrowRight: [1, 0] }[event.key];
    if (!step) return;
    event.preventDefault();
    const flip = side === 'white' ? 1 : -1;
    const file = FILES.indexOf(cursor[0]) + step[0] * flip;
    const rank = Number(cursor.slice(1)) - 1 + step[1] * flip;
    if (file < 0 || file >= SIZE || rank < 0 || rank >= SIZE) return;
    focusSquare(FILES[file] + (rank + 1), true);
  });

  function focusSquare(square, moveFocus) {
    cells.get(cursor).tabIndex = -1;
    cursor = square;
    const cell = cells.get(square);
    cell.tabIndex = 0;
    if (moveFocus) cell.focus();
  }

  function render() {
    const bySquare = new Map(state.pieces.map((p) => [p.square, p]));
    const last = state.history.at(-1);
    for (const [square, cell] of cells) {
      const piece = bySquare.get(square);
      cell.querySelector('.glyph').textContent = piece ? glyphOf(piece) : '';
      cell.classList.toggle('last', square === last?.from || square === last?.to);
      cell.classList.toggle('check', square === state.checkedKing);
      cell.classList.toggle('selected', square === selected);
      cell.classList.toggle('target', targets.has(square) && !targets.get(square));
      cell.classList.toggle('capture', targets.get(square) === true);
      cell.setAttribute('aria-label', label(square, piece));
    }
  }

  /** "e2: peón de las blancas, elegida" / "e2: white pawn, selected". La pieza se nombra por su tipo. */
  function label(square, piece) {
    let text = piece
      ? t('square.piece', { square, piece: t(`piece.${piece.type in PIECE_KEYS ? piece.type : 'unknown'}`), color: t(`color.${piece.color}`) })
      : t('square.empty', { square });
    if (square === state.checkedKing) text += t('square.check');
    if (square === selected) text += t('square.selected');
    if (targets.has(square)) text += t(targets.get(square) ? 'square.capture' : 'square.target');
    return text;
  }

  /**
   * Desliza la figura de una casilla a otra (como el BoardPanel); la capturada se desvanece.
   * Al terminar, la figura queda en el destino (igual que en 3D): así se ve la posición final
   * aunque el estado nuevo llegue después (por ejemplo, con el aviso de fin de ronda abierto).
   */
  async function slide(from, to) {
    const glyph = cells.get(from)?.querySelector('.glyph');
    if (!glyph?.textContent) return;
    try {
      if (!reducedMotion()) await animateSlide(from, to);
    } finally {
      cells.get(to).querySelector('.glyph').textContent = glyph.textContent;
      glyph.textContent = '';
    }
  }

  async function animateSlide(from, to) {
    const fromCell = cells.get(from);
    const toCell = cells.get(to);
    const glyph = fromCell.querySelector('.glyph');
    const start = fromCell.getBoundingClientRect();
    const end = toCell.getBoundingClientRect();
    const box = container.getBoundingClientRect();

    const ghost = glyph.cloneNode(true);
    ghost.classList.add('ghost');
    Object.assign(ghost.style, {
      left: `${start.left - box.left}px`, top: `${start.top - box.top}px`,
      width: `${start.width}px`, height: `${start.height}px`,
    });
    container.append(ghost);
    glyph.style.visibility = 'hidden';
    const victim = toCell.querySelector('.glyph');

    animating++;
    try {
      // Tope por si el navegador no pinta (pestaña en segundo plano): la jugada no queda esperando.
      const timeout = new Promise((resolve) => setTimeout(resolve, SLIDE_MS * 2));
      await Promise.race([timeout, Promise.all([
        ghost.animate(
          [{ transform: 'translate(0, 0)' }, { transform: `translate(${end.left - start.left}px, ${end.top - start.top}px)` }],
          { duration: SLIDE_MS, easing: 'cubic-bezier(.4, 0, .2, 1)' }).finished,
        victim.textContent ? victim.animate([{ opacity: 1 }, { opacity: 0 }], { duration: SLIDE_MS, fill: 'forwards' }).finished : null,
      ])]);
    } finally {
      animating--;
      ghost.remove();
      glyph.style.visibility = '';
      victim.getAnimations().forEach((animation) => animation.cancel());
    }
  }

  return {
    name: '2d',
    ready: true,

    setVisible(visible) { container.hidden = !visible; },

    sync(newState) {
      state = newState;
      render();
    },
    clear() {
      state = { pieces: [], history: [], checkedKing: null };
      render();
    },

    select(square) {
      selected = square;
      targets = new Map();
      render();
    },
    releaseSelection() {
      selected = null;
      targets = new Map();
      if (state) render();
    },
    clearSelection() { this.releaseSelection(); },
    async lower() {},

    setTargets(moves) {
      targets = new Map(moves.map((m) => [m.to, m.capture]));
      render();
    },
    hasTarget: (square) => targets.has(square),

    move: (from, to) => slide(from, to),
    unmove: (last) => slide(last.to, last.from),

    /** Gira el tablero: las negras abajo (o vuelven las blancas). Sin animación, como en Swing. */
    async flip() {
      side = side === 'white' ? 'black' : 'white';
      build();
      return side;
    },
    getSide: () => side,
    isAnimating: () => animating > 0,
  };
}
