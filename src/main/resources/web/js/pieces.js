// pieces.js — carga de los 12 .glb, clonado, tinte por bando y sincronización con el backend.
import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import * as SkeletonUtils from 'three/addons/utils/SkeletonUtils.js';
import { squareToWorld, BOARD_FLOOR_Y } from './board.js';
import { animatePop, setOpacity } from './anim.js';

/** false = los bandos se miran entre sí; true = todas las piezas miran a la cámara (+Z). */
export const FACE_CAMERA = false;

const TINT = { white: new THREE.Color('#e8e0cc'), black: new THREE.Color('#1e1626') };
const TYPES = ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'];
const HOVER_EMISSIVE = new THREE.Color(0x3a3020);
const SELECTED_EMISSIVE = new THREE.Color(0x5a4420);
const CAPTURE_EMISSIVE = new THREE.Color(0x6e1610);
const CAPTURED_SCALE = 0.5;

/** Las piezas ocupan menos que su casilla: queda aire entre vecinas y se distinguen una por una. */
export const PIECE_SCALE = 0.82;

// Sombra de contacto: un disco con degradé apenas sobre la casilla, debajo de cada pieza.
// Mucho más barato que SSAO y "apoya" la pieza aunque la sombra de la luz caiga hacia atrás.
const CONTACT_SIZE = 0.95;      // en unidades de la pieza (antes de escalar)
const CONTACT_OPACITY = 0.38;
const contactGeometry = new THREE.PlaneGeometry(CONTACT_SIZE, CONTACT_SIZE);
const contactTexture = radialTexture();

/** Carga los 12 modelos UNA sola vez. Devuelve { white: {king: scene, ...}, black: {...} }. */
export async function loadModels(manager) {
  const loader = new GLTFLoader(manager);
  const templates = { white: {}, black: {} };
  const jobs = [];
  for (const type of TYPES) {
    for (const color of ['white', 'black']) {
      const url = `models/${type}${color === 'black' ? '_black' : ''}.glb`;
      jobs.push(loader.loadAsync(url).then((gltf) => {
        prepareTemplate(gltf.scene);
        templates[color][type] = gltf.scene;
      }));
    }
  }
  await Promise.all(jobs);
  return templates;
}

/** Sombras y texturas pixel art nítidas (sin esto se ven borrosas). */
function prepareTemplate(root) {
  root.traverse((child) => {
    if (!child.isMesh) return;
    child.castShadow = true;
    child.receiveShadow = true;
    for (const material of [].concat(child.material)) {
      for (const key of ['map', 'emissiveMap', 'normalMap', 'roughnessMap', 'metalnessMap']) {
        const texture = material[key];
        if (!texture) continue;
        texture.magFilter = THREE.NearestFilter;
        texture.minFilter = THREE.NearestFilter;
        texture.generateMipmaps = false;
        if (key === 'map' || key === 'emissiveMap') texture.colorSpace = THREE.SRGBColorSpace;
        texture.needsUpdate = true;
      }
    }
  });
}

/**
 * Las piezas del tablero, indexadas por casilla. La fuente de verdad SIEMPRE es el
 * estado que manda el backend: sync() hace que la escena coincida con él.
 */
export class PieceSet {
  constructor(scene, templates) {
    this.templates = templates;
    this.group = new THREE.Group();
    this.group.name = 'pieces';
    this.capturedGroup = new THREE.Group();
    this.capturedGroup.name = 'captured';
    scene.add(this.group, this.capturedGroup);
    this.bySquare = new Map();   // "e2" -> Object3D
    this.capturedKey = '';
  }

  /** Clona el modelo y clona sus materiales para teñir "Base" sin afectar a las otras piezas. */
  create(type, color) {
    const template = this.templates[color][type] ?? this.templates[color].pawn; // pieza desconocida -> peón
    const piece = SkeletonUtils.clone(template);
    piece.traverse((child) => {
      if (!child.isMesh) return;
      const cloned = [].concat(child.material).map((material) => {
        const copy = material.clone();
        if (copy.name === 'Base') copy.color.copy(TINT[color]);
        return copy;
      });
      child.material = Array.isArray(child.material) ? cloned : cloned[0];
    });
    // Material propio por pieza: setOpacity() lo desvanece junto con la pieza al capturarla
    const contact = new THREE.Mesh(contactGeometry, new THREE.MeshBasicMaterial({
      map: contactTexture, color: 0x000000, transparent: true, opacity: CONTACT_OPACITY, depthWrite: false,
    }));
    contact.rotation.x = -Math.PI / 2;
    contact.position.y = 0.006;
    contact.renderOrder = 1;
    contact.userData = { contactShadow: true, baseOpacity: CONTACT_OPACITY };
    piece.add(contact);

    // Las blancas giran 180° para mirar a las negras; las negras ya miran hacia +Z
    piece.rotation.y = FACE_CAMERA || color === 'black' ? 0 : Math.PI;
    piece.scale.setScalar(PIECE_SCALE);
    piece.userData = { isPiece: true, type, color, square: null };
    return piece;
  }

  at(square) {
    return this.bySquare.get(square) ?? null;
  }

  /** Lleva la escena al estado del backend: quita lo que sobra, agrega lo que falta, acomoda posiciones. */
  sync(state, { animateNew = true } = {}) {
    const wanted = new Map(state.pieces.map((p) => [p.square, p]));

    for (const [square, object] of this.bySquare) {
      const dto = wanted.get(square);
      if (!dto || dto.type !== object.userData.type || dto.color !== object.userData.color) {
        this.group.remove(object);
        this.bySquare.delete(square);
      }
    }

    let delay = 0;
    for (const [square, dto] of wanted) {
      let object = this.bySquare.get(square);
      if (!object) {
        object = this.create(dto.type, dto.color);
        this.group.add(object);
        this.bySquare.set(square, object);
        if (animateNew) {
          animatePop(object, PIECE_SCALE, delay);
          delay += 14;
        }
      }
      object.userData.square = square;
      object.position.copy(squareToWorld(square));
      setEmissive(object, null);
      setOpacity(object, 1);
    }
    this.syncCaptured(state);
  }

  /** Mueve la referencia de una pieza después de animarla (antes del sync). */
  rekey(from, to) {
    const object = this.bySquare.get(from);
    if (!object) return;
    this.bySquare.delete(from);
    this.bySquare.set(to, object);
    object.userData.square = to;
  }

  /** Saca una pieza de la escena (la capturada, después de animarla). */
  discard(object) {
    this.group.remove(object);
    for (const [square, o] of this.bySquare) {
      if (o === object) this.bySquare.delete(square);
    }
  }

  /**
   * Filas de capturadas al costado del tablero, cada una de su color:
   * blancas capturadas a la izquierda (x = -5.25), negras a la derecha (x = +5.25).
   */
  syncCaptured(state) {
    const key = state.capturedWhite.join() + '|' + state.capturedBlack.join();
    if (key === this.capturedKey) return;
    const previous = this.capturedKey ? this.capturedKey.split('|').map((s) => (s ? s.split(',').length : 0)) : [0, 0];
    this.capturedKey = key;
    this.capturedGroup.clear();

    const lay = (types, color, x, side, alreadyShown) => {
      types.forEach((type, i) => {
        const object = this.create(type, color);
        const column = Math.floor(i / 8);   // de a 8 por columna; la segunda queda más afuera
        const row = i % 8;
        object.position.set(x + side * column * 0.6, BOARD_FLOOR_Y, (row - 3.5) * 0.9);
        object.scale.setScalar(CAPTURED_SCALE);
        this.capturedGroup.add(object);
        if (i >= alreadyShown) animatePop(object, CAPTURED_SCALE, 120);
      });
    };
    lay(state.capturedWhite, 'white', -5.3, -1, previous[0]);
    lay(state.capturedBlack, 'black', 5.3, 1, previous[1]);
  }

  /** Todos los meshes de piezas del tablero, para el raycaster (sin las sombras de contacto). */
  meshes() {
    const list = [];
    this.group.traverse((child) => { if (child.isMesh && !child.userData.contactShadow) list.push(child); });
    return list;
  }
}

/** Degradé radial negro -> transparente para la sombra de contacto (64x64, hecho una sola vez). */
function radialTexture() {
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = 64;
  const ctx = canvas.getContext('2d');
  const gradient = ctx.createRadialGradient(32, 32, 0, 32, 32, 32);
  gradient.addColorStop(0, 'rgba(255,255,255,1)');
  gradient.addColorStop(0.55, 'rgba(255,255,255,0.55)');
  gradient.addColorStop(1, 'rgba(255,255,255,0)');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, 64, 64);
  const texture = new THREE.CanvasTexture(canvas);
  // MeshBasicMaterial con color negro: el canal alfa del degradé hace la sombra
  return texture;
}

/** Sube desde un mesh hasta el grupo de la pieza. */
export function pieceOf(mesh) {
  let node = mesh;
  while (node && !node.userData?.isPiece) node = node.parent;
  return node ?? null;
}

/** null = sin brillo, 'hover' = suave, 'selected' = más fuerte, 'capture' = rojizo (se puede capturar). */
export function setEmissive(object, mode) {
  const color = { selected: SELECTED_EMISSIVE, hover: HOVER_EMISSIVE, capture: CAPTURE_EMISSIVE }[mode] ?? null;
  object.traverse((child) => {
    if (!child.isMesh) return;
    for (const material of [].concat(child.material)) {
      if (!material.emissive) continue;
      material.emissive.copy(color ?? BLACK);
    }
  });
}

const BLACK = new THREE.Color(0x000000);
