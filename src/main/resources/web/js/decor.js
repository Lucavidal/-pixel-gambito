// decor.js — decoración de la pantalla de inicio: algunas piezas 3D flotando en los márgenes, alrededor
// de la columna del menú. Suman ambiente sin distraer: chicas, apagadas, lentas y nunca encima del texto,
// el botón ni el tablero.
//
// Usa el MISMO renderer que el tablero (no otro canvas): una escena y una cámara propias que se dibujan
// después de la escena principal, borrando solo la profundidad. No participa del raycast ni del input
// (input.js solo mira las piezas del tablero). Reutiliza los modelos ya cargados (PieceSet.create).
import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';
import { PieceSet } from './pieces.js';
import { setOpacity, tween, ease, reducedMotion } from './anim.js';
import { getTheme, onThemeChange } from './theme.js';

// ---------- Constantes para ajustar el movimiento ----------
const MIN_WIDTH = 1100;            // más angosto, no se muestran
const FLOAT_AMPLITUDE = 0.06;      // flotación: sube y baja ± esto
const FLOAT_PERIOD_S = [4, 7];     // período de la flotación (s)
const SPIN_DEG_PER_S = [4, 8];     // giro continuo (°/s)
const SWAY_DEG = 20;               // vaivén: ± estos grados
const GESTURE_EVERY_S = [6, 10];   // cada cuánto una pieza hace un gesto
const PARALLAX_MAX = 0.15;         // desplazamiento máximo con el mouse (unidades), en la más cercana
const PARALLAX_LERP = 0.04;        // suavizado del paralaje por frame (más chico = más lento)
const SHOW_MS = 400;
const SHOW_STAGGER_MS = 120;
const HIDE_MS = 250;
const DIM = 0.75;                  // las piezas se oscurecen a este factor (las lejanas, más)
const DESATURATE = 0.25;           // cuánto se acercan al gris

// La cámara de decoración: mira de frente al plano z = 0
const FOV = 30;
const CAMERA_Z = 12;
// Profundidades: cerca, medio, lejos (las lejanas, más chicas, más apagadas y con menos paralaje)
const DEPTHS = [
  { z: 0, size: 1, dim: 1, parallax: 1 },
  { z: -3, size: 0.8, dim: 0.88, parallax: 0.6 },
  { z: -6, size: 0.65, dim: 0.76, parallax: 0.35 },
];

const MARGIN_PX = 44;              // aire alrededor del menú (incluye flotación, paralaje y gestos)
const TOP_RESERVED_PX = 64;        // los controles de tema e idioma
const BOTTOM_RESERVED_PX = 48;     // el footer

/**
 * Las piezas y dónde van. slot dice en qué margen del menú (ver layout()); scale entre 0.6 y 0.9;
 * spin = 'spin' (vuelta completa lenta) o 'sway' (ida y vuelta ±SWAY_DEG).
 */
const PIECES = [
  { type: 'pawn', color: 'black', slot: 'topLeft', depth: 2, scale: 0.7, spin: 'sway' },
  { type: 'bishop', color: 'white', slot: 'topRight', depth: 1, scale: 0.8, spin: 'spin' },
  { type: 'king', color: 'black', slot: 'rightUpper', depth: 0, scale: 0.9, spin: 'sway' },
  { type: 'knight', color: 'white', slot: 'rightLower', depth: 0, scale: 0.75, spin: 'sway' },
  { type: 'rook', color: 'white', slot: 'bottomLeft', depth: 1, scale: 0.65, spin: 'spin' },
  { type: 'queen', color: 'black', slot: 'bottomRight', depth: 2, scale: 0.85, spin: 'spin' },
];

const LIGHTS = {
  dark: { hemi: 1.0, sun: 1.3 },
  light: { hemi: 1.25, sun: 1.2 },
};

const between = ([a, b]) => a + Math.random() * (b - a);
const DEG = Math.PI / 180;

/**
 * @param renderer  el renderer del tablero (el mismo canvas)
 * @param templates los modelos ya cargados por pieces.js
 * @param container el elemento del canvas (para el tamaño)
 */
export function createDecor({ renderer, templates, container }) {
  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(FOV, 1, 0.1, 100);
  camera.position.set(0, 0, CAMERA_Z);
  camera.lookAt(0, 0, 0);

  const hemi = new THREE.HemisphereLight(0xfff6e8, 0x8f887c, 1);
  const sun = new THREE.DirectionalLight(0xfff2de, 1.3);   // sin sombras
  sun.position.set(-4, 8, 10);
  scene.add(hemi, sun);
  const applyLights = (theme) => { hemi.intensity = LIGHTS[theme].hemi; sun.intensity = LIGHTS[theme].sun; };
  applyLights(getTheme());
  onThemeChange(applyLights);

  // PieceSet solo para reusar create(): mismos modelos y tintes que el tablero, sin cargar nada
  const factory = new PieceSet(scene, templates);
  const kingHeight = heightOf(templates.white.king);
  const pieces = PIECES.map((spec, i) => makePiece(spec, i));

  let anchor = null;        // la columna del menú (para no taparla)
  let wanted = false;       // la pantalla de inicio quiere decoración
  let shown = false;        // se está dibujando
  let time = 0;             // reloj propio en segundos: no avanza con la pestaña oculta ni con movimiento reducido
  let lastNow = 0;
  let nextGestureAt = between(GESTURE_EVERY_S);
  let gestureRunning = false;
  const pointer = { x: 0, y: 0 };   // -1..1

  window.addEventListener('pointermove', (event) => {
    pointer.x = (event.clientX / window.innerWidth) * 2 - 1;
    pointer.y = (event.clientY / window.innerHeight) * 2 - 1;
  });
  window.addEventListener('resize', () => resize());

  function makePiece(spec, index) {
    const object = factory.create(spec.type, spec.color);
    // Sin sombra de contacto, sin sombras propias, sin brillo, y más apagada que en el tablero
    for (const child of [...object.children]) if (child.userData.contactShadow) object.remove(child);
    const depth = DEPTHS[spec.depth];
    const gray = new THREE.Color();
    object.traverse((child) => {
      if (!child.isMesh) return;
      child.castShadow = false;
      child.receiveShadow = false;
      child.raycast = () => {};   // por las dudas: nunca se puede "tocar"
      for (const material of [].concat(child.material)) {
        if (material.color) {
          const c = material.color;
          const l = c.r * 0.3 + c.g * 0.59 + c.b * 0.11;
          c.lerp(gray.setRGB(l, l, l), DESATURATE).multiplyScalar(DIM * depth.dim);
        }
        if (material.emissive) material.emissive.setRGB(0, 0, 0);
      }
    });
    mergeByMaterial(object);   // menos draw calls: una malla por material en vez de una por bloque
    object.rotation.set(0.22, 0, 0);   // un poco inclinadas hacia la cámara: se ve la parte de arriba
    object.visible = false;
    scene.add(object);
    return {
      spec, object, depth,
      height: heightOf(templates[spec.color][spec.type]),  // alto del modelo sin escalar
      baseScale: 1, base: new THREE.Vector3(), placed: false,
      floatPeriod: between(FLOAT_PERIOD_S), floatPhase: Math.random() * Math.PI * 2,
      spinSpeed: between(SPIN_DEG_PER_S) * DEG * (index % 2 ? -1 : 1),
      swayPeriod: between([9, 14]), swayPhase: Math.random() * Math.PI * 2,
      baseRotY: Math.random() * Math.PI * 2,
      offset: new THREE.Vector2(),          // paralaje actual (se acerca al objetivo con lerp)
      gesture: { y: 0, z: 0, rot: 0, tilt: 0 },
      appear: 0,                            // 0..1 de la aparición / desaparición
    };
  }

  /**
   * Ubica cada pieza en su margen, según el tamaño de la pantalla y dónde está el menú de verdad
   * (getBoundingClientRect). Si en ese margen no entra sin tapar nada, la pieza no se muestra.
   */
  function layout() {
    const w = container.clientWidth;
    const h = container.clientHeight;
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
    if (!anchor) return;
    const r = contentRect(anchor);
    const box = { left: r.left - MARGIN_PX, right: r.right + MARGIN_PX, top: r.top - MARGIN_PX, bottom: r.bottom + MARGIN_PX };
    const kingPx = Math.min(150, Math.max(90, h * 0.16));
    // Espacio libre de cada margen (en px de pantalla)
    const topBand = { top: TOP_RESERVED_PX, bottom: box.top };
    const bottomBand = { top: box.bottom, bottom: h - BOTTOM_RESERVED_PX };
    const rightBand = { left: box.right, right: w - 16 };
    const slots = {
      topLeft: { x: r.left + r.width * 0.2, y: (topBand.top + topBand.bottom) / 2, maxH: topBand.bottom - topBand.top },
      topRight: { x: r.left + r.width * 0.72, y: (topBand.top + topBand.bottom) / 2, maxH: topBand.bottom - topBand.top },
      rightUpper: { x: (rightBand.left + rightBand.right) / 2, y: r.top + r.height * 0.28, maxH: r.height * 0.4, maxW: rightBand.right - rightBand.left },
      rightLower: { x: (rightBand.left + rightBand.right) / 2, y: r.top + r.height * 0.78, maxH: r.height * 0.4, maxW: rightBand.right - rightBand.left },
      bottomLeft: { x: r.left + r.width * 0.3, y: (bottomBand.top + bottomBand.bottom) / 2, maxH: bottomBand.bottom - bottomBand.top },
      bottomRight: { x: r.left + r.width * 0.8, y: (bottomBand.top + bottomBand.bottom) / 2, maxH: bottomBand.bottom - bottomBand.top },
    };
    for (const piece of pieces) {
      const slot = slots[piece.spec.slot];
      const depth = piece.depth;
      const dist = CAMERA_Z - depth.z;
      const worldPerPx = (2 * dist * Math.tan((FOV * DEG) / 2)) / h;
      // Alto en px: el rey de referencia por la escala de la pieza y su profundidad, y lo que mide su modelo
      let px = kingPx * piece.spec.scale * depth.size * (piece.height / kingHeight);
      px = Math.min(px, slot.maxH * 0.85);
      const widthPx = px * 0.75;   // las piezas son más altas que anchas
      const fits = px >= 36
        && (slot.maxW === undefined || widthPx <= slot.maxW)
        && slot.x - widthPx / 2 > w / 2 + 16          // nunca sobre el tablero (mitad izquierda)
        && slot.x + widthPx / 2 < w - 8;
      piece.placed = fits;
      if (!fits) { piece.object.visible = false; continue; }
      piece.baseScale = (px * worldPerPx) / piece.height;
      // El origen del modelo está en su base: se centra en el alto
      piece.base.set((slot.x - w / 2) * worldPerPx, (h / 2 - slot.y) * worldPerPx - (px * worldPerPx) / 2, depth.z);
    }
  }

  /** Un gesto de una sola pieza a la vez: saltito del caballo, medio giro de la torre, pasito del peón... */
  function gesture() {
    const candidates = pieces.filter((p) => p.placed);
    if (!candidates.length) return;
    const piece = candidates[Math.floor(Math.random() * candidates.length)];
    const g = piece.gesture;
    gestureRunning = true;
    let run;
    switch (piece.spec.type) {
      case 'knight': // saltito en arco
        run = tween({ duration: 800, easing: ease.linear, ambient: true, onUpdate: (k) => { g.y = 0.22 * 4 * k * (1 - k); g.z = 0.1 * Math.sin(Math.PI * k); } });
        break;
      case 'rook': { // medio giro
        const from = g.rot;
        run = tween({ duration: 1200, easing: ease.inOutCubic, ambient: true, onUpdate: (k) => { g.rot = from + Math.PI * k; } });
        break;
      }
      case 'pawn': // pasito adelante y vuelta
        run = tween({ duration: 1000, easing: ease.inOutCubic, ambient: true, onUpdate: (k) => { g.z = 0.35 * Math.sin(Math.PI * k); } });
        break;
      default: // el resto: una reverencia chiquita
        run = tween({ duration: 900, easing: ease.inOutCubic, ambient: true, onUpdate: (k) => { g.tilt = 0.12 * Math.sin(Math.PI * k); } });
    }
    run.then(() => { gestureRunning = false; });
  }

  function appear(visible) {
    const list = pieces.filter((p) => p.placed);
    return Promise.all(list.map((piece, i) => {
      const from = piece.appear;
      if (visible) piece.object.visible = true;
      return tween({
        duration: visible ? SHOW_MS : HIDE_MS,
        delay: visible ? i * SHOW_STAGGER_MS : 0,
        easing: visible ? ease.outCubic : ease.inCubic,
        ambient: true,
        onUpdate: (k) => { piece.appear = from + ((visible ? 1 : 0) - from) * k; },
      });
    }));
  }

  /** Muestra la decoración alrededor de `anchorEl` (la columna del menú del inicio). */
  function show(anchorEl) {
    anchor = anchorEl ?? anchor;
    wanted = true;
    if (container.clientWidth < MIN_WIDTH || shown) return;
    layout();
    shown = true;
    for (const piece of pieces) { piece.appear = 0; piece.object.visible = false; }
    appear(true);
  }

  /** Las desvanece (250 ms) y deja de dibujarlas. */
  async function hide() {
    wanted = false;
    if (!shown) return;
    await appear(false);
    if (wanted) return;   // volvieron a pedirla mientras se iba
    shown = false;
    for (const piece of pieces) piece.object.visible = false;
  }

  /** En resize: se reubican, y se ocultan o vuelven según el ancho. */
  function resize() {
    if (!wanted) return;
    if (container.clientWidth < MIN_WIDTH) {
      shown = false;
      for (const piece of pieces) piece.object.visible = false;
      return;
    }
    if (!shown) return show();
    layout();
    for (const piece of pieces) piece.object.visible = piece.placed;
  }

  /** Una vez por frame, después de dibujar el tablero: anima y dibuja la decoración (si se ve). */
  function update(now) {
    const dt = lastNow ? Math.min(now - lastNow, 50) / 1000 : 0;
    lastNow = now;
    if (!shown || document.hidden) return;
    if (!reducedMotion) {
      time += dt;
      if (!gestureRunning && time >= nextGestureAt) {
        gesture();
        nextGestureAt = time + between(GESTURE_EVERY_S);
      }
    }
    for (const piece of pieces) {
      if (!piece.placed) continue;
      const { object, base, gesture: g } = piece;
      // Paralaje: con movimiento reducido, quietas
      const k = reducedMotion ? 0 : PARALLAX_MAX * piece.depth.parallax;
      piece.offset.x += (pointer.x * k - piece.offset.x) * PARALLAX_LERP;
      piece.offset.y += (-pointer.y * k - piece.offset.y) * PARALLAX_LERP;
      const float = reducedMotion ? 0 : FLOAT_AMPLITUDE * Math.sin((2 * Math.PI * time) / piece.floatPeriod + piece.floatPhase);
      const spin = reducedMotion ? 0
        : piece.spec.spin === 'spin' ? piece.spinSpeed * time
        : SWAY_DEG * DEG * Math.sin((2 * Math.PI * time) / piece.swayPeriod + piece.swayPhase);
      object.position.set(base.x + piece.offset.x, base.y + float + g.y + piece.offset.y, base.z + g.z);
      object.rotation.y = piece.baseRotY + spin + g.rot;
      object.rotation.x = 0.22 + g.tilt;
      // Aparición: escala y opacidad juntas
      object.scale.setScalar(piece.baseScale * (0.6 + 0.4 * piece.appear));
      setOpacity(object, piece.appear, true);
    }
    // Segunda pasada sobre el mismo canvas: solo se borra la profundidad, el tablero queda
    renderer.autoClear = false;
    renderer.clearDepth();
    renderer.render(scene, camera);
    renderer.autoClear = true;
  }

  /** Para pruebas: dónde quedó cada pieza en pantalla (rectángulo proyectado) y el del menú. */
  function debug() {
    const w = container.clientWidth;
    const h = container.clientHeight;
    const toPx = (v) => ({ x: (v.x + 1) / 2 * w, y: (1 - v.y) / 2 * h });
    return {
      wanted, shown, menu: anchor && contentRect(anchor),
      pieces: pieces.map((p) => {
        const box = new THREE.Box3().setFromObject(p.object);
        const corners = [];
        for (const x of [box.min.x, box.max.x]) for (const y of [box.min.y, box.max.y]) for (const z of [box.min.z, box.max.z]) {
          corners.push(toPx(new THREE.Vector3(x, y, z).project(camera)));
        }
        const xs = corners.map((c) => c.x);
        const ys = corners.map((c) => c.y);
        return {
          type: p.spec.type, placed: p.placed, visible: p.object.visible,
          rect: { left: Math.min(...xs), right: Math.max(...xs), top: Math.min(...ys), bottom: Math.max(...ys) },
        };
      }),
    };
  }

  return { show, hide, update, resize, debug };
}

/**
 * Junta las mallas de la pieza que comparten material en una sola (con su posición relativa a la pieza):
 * cada .glb tiene muchas mallas chicas y cada una es un draw call. Si las geometrías no se pueden juntar
 * (atributos distintos), la pieza queda como estaba.
 */
function mergeByMaterial(root) {
  root.updateMatrixWorld(true);
  const toRoot = root.matrixWorld.clone().invert();
  const groups = new Map();   // material -> geometrías ya ubicadas
  const meshes = [];
  root.traverse((child) => {
    if (!child.isMesh || Array.isArray(child.material)) return;
    meshes.push(child);
    const geometry = child.geometry.clone().applyMatrix4(toRoot.clone().multiply(child.matrixWorld));
    if (!groups.has(child.material)) groups.set(child.material, []);
    groups.get(child.material).push(geometry);
  });
  const merged = [];
  for (const [material, geometries] of groups) {
    const geometry = geometries.length === 1 ? geometries[0] : mergeGeometries(geometries, false);
    if (!geometry) return;   // no se pudo: se deja la pieza original
    const mesh = new THREE.Mesh(geometry, material);
    mesh.raycast = () => {};
    merged.push(mesh);
  }
  for (const child of [...root.children]) root.remove(child);
  root.add(...merged);
}

/**
 * El contorno REAL del menú: el texto de cada línea (no el ancho de la columna, que es más ancho)
 * y los botones enteros. Es lo que las piezas no pueden tapar.
 */
function contentRect(column) {
  const rect = { left: Infinity, top: Infinity, right: -Infinity, bottom: -Infinity };
  const add = (r) => {
    if (!r.width && !r.height) return;
    rect.left = Math.min(rect.left, r.left);
    rect.top = Math.min(rect.top, r.top);
    rect.right = Math.max(rect.right, r.right);
    rect.bottom = Math.max(rect.bottom, r.bottom);
  };
  const range = document.createRange();
  for (const element of column.querySelectorAll('*')) {
    if (element.matches('button, a')) {
      add(element.getBoundingClientRect());
    } else if ([...element.childNodes].some((node) => node.nodeType === Node.TEXT_NODE && node.textContent.trim())) {
      range.selectNodeContents(element);
      add(range.getBoundingClientRect());
    }
  }
  if (rect.left === Infinity) return column.getBoundingClientRect();
  return { ...rect, width: rect.right - rect.left, height: rect.bottom - rect.top };
}

function heightOf(object) {
  const box = new THREE.Box3().setFromObject(object);
  return box.max.y - box.min.y;
}
