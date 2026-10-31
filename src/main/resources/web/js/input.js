// input.js — raycast con el mouse, hover y selección. Lo que pasa al hacer clic lo decide main.js
// (que le pregunta al backend); acá solo se traduce el mouse a "casilla bajo el cursor".
import * as THREE from 'three';
import { worldToSquare } from './board.js';
import { pieceOf, setEmissive } from './pieces.js';

const CLICK_TOLERANCE_PX = 6; // si el mouse se movió más, fue un giro de cámara y no un clic

export function createInput({ camera, domElement, board, pieces, onSquareClick, onCancel, isLocked }) {
  const raycaster = new THREE.Raycaster();
  const pointer = new THREE.Vector2();
  let hoveredPiece = null;
  let selectedPiece = null;
  let down = null;
  let targetSquares = new Set();
  let capturable = new Set();   // piezas rivales que se pueden capturar (brillan en rojo)
  let pointerInside = false;

  /** Casilla bajo el cursor: primero las piezas (son altas y tapan casillas), después el tablero. */
  function pick(event) {
    const rect = domElement.getBoundingClientRect();
    pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
    raycaster.setFromCamera(pointer, camera);

    // Con una pieza elegida, si el rayo atraviesa un destino resaltado (aunque otra pieza lo tape
    // desde este ángulo) se elige ese destino: así se puede capturar una pieza "escondida".
    // (Si lo primero que toca es otra pieza propia, gana esa: es un cambio de selección.)
    if (targetSquares.size > 0) {
      const hits = raycaster.intersectObjects([...pieces.meshes(), ...board.squares], false);
      const first = hits[0] && pieceOf(hits[0].object);
      const ownColor = selectedPiece?.userData.color;
      const switching = first && first.userData.color === ownColor && !targetSquares.has(first.userData.square);
      for (const hit of switching ? [] : hits) {
        const square = pieceOf(hit.object)?.userData.square ?? hit.object.userData.square;
        if (targetSquares.has(square)) return { square, piece: pieces.at(square) };
      }
    }

    const pieceHit = raycaster.intersectObjects(pieces.meshes(), false)[0];
    if (pieceHit) {
      const piece = pieceOf(pieceHit.object);
      if (piece) return { square: piece.userData.square, piece };
    }
    const squareHit = raycaster.intersectObjects(board.squares, false)[0];
    if (squareHit) {
      const square = squareHit.object.userData.square ?? worldToSquare(squareHit.point.x, squareHit.point.z);
      return { square, piece: pieces.at(square) };
    }
    return { square: null, piece: null };
  }

  function setHover(hit) {
    const piece = hit.piece && hit.piece !== selectedPiece ? hit.piece : null;
    if (hoveredPiece && hoveredPiece !== piece && hoveredPiece !== selectedPiece) setEmissive(hoveredPiece, restingMode(hoveredPiece));
    hoveredPiece = piece;
    if (piece) setEmissive(piece, 'hover');
    board.setHover(hit.square);
    const clickable = hit.square && (targetSquares.has(hit.square) || hit.piece);
    domElement.style.cursor = isLocked() ? 'progress' : clickable ? 'pointer' : 'default';
  }

  const restingMode = (piece) => (capturable.has(piece) ? 'capture' : null);

  domElement.addEventListener('pointermove', (event) => {
    pointerInside = true;
    if (down && (event.buttons & 1)) return; // arrastrando: está girando la cámara
    setHover(pick(event));
  });
  domElement.addEventListener('pointerleave', () => {
    pointerInside = false;
    setHover({ square: null, piece: null });
  });
  domElement.addEventListener('pointerdown', (event) => {
    if (event.button === 0) down = { x: event.clientX, y: event.clientY };
  });
  domElement.addEventListener('pointerup', (event) => {
    if (event.button !== 0 || !down) return;
    const moved = Math.hypot(event.clientX - down.x, event.clientY - down.y);
    down = null;
    if (moved > CLICK_TOLERANCE_PX || isLocked()) return;
    const hit = pick(event);
    onSquareClick(hit.square, hit.piece);
  });
  window.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') onCancel();
  });

  return {
    /** La pieza seleccionada brilla más; su casilla se ilumina (main.js la levanta). */
    select(piece, square) {
      if (selectedPiece && selectedPiece !== piece) setEmissive(selectedPiece, null);
      selectedPiece = piece;
      if (piece) setEmissive(piece, 'selected');
      board.setSelected(square);
    },
    clearSelection() {
      if (selectedPiece) setEmissive(selectedPiece, null);
      selectedPiece = null;
      board.setSelected(null);
      targetSquares = new Set();
      for (const piece of capturable) setEmissive(piece, null);
      capturable = new Set();
      board.clearTargets();
    },
    setTargets(moves) {
      targetSquares = new Set(moves.map((m) => m.to));
      capturable = new Set(moves.filter((m) => m.capture).map((m) => pieces.at(m.to)).filter(Boolean));
      for (const piece of capturable) setEmissive(piece, 'capture');
      board.showTargets(moves);
    },
    hasTarget: (square) => targetSquares.has(square),
    /** Después de re-sincronizar la escena las piezas son objetos nuevos: se limpia el hover. */
    resetHover() {
      hoveredPiece = null;
      if (!pointerInside) board.setHover(null);
    },
  };
}
