// board.js — tablero generado por código, coordenadas y resaltados de casillas.
// Todo el resto del front convierte casillas <-> mundo SOLO con toWorld / fromWorld.
// Con el tema de la página (theme.js) cambia el tablero entero: casillas, marco, línea y letras del borde
// (de noche verde y crema; de día arena y madera clara). Las piezas no cambian.
import * as THREE from 'three';
import { getTheme, onThemeChange } from './theme.js';
import { tween, ease } from './anim.js';

const FILES = 'abcdefgh';
const SQUARE_HEIGHT = 0.1;   // BoxGeometry de 1 x 0.1 x 1; la cara de arriba queda en y = 0
const OVERLAY_Y = 0.004;     // los resaltados flotan apenas sobre la casilla

/**
 * Temas del tablero. Cada resaltado es [color, opacidad].
 * - clasico: casillas lisas verde y crema (estilo chess.com), marco fino; no compite con las piezas.
 * - madera: el tablero anterior, con grano pixelado y marco de madera oscura con zócalo.
 */
const THEMES = {
  clasico: {
    light: 0xebecd0,
    dark: 0x779556,
    noise: false,
    roughness: { light: 0.9, dark: 0.9 },
    frame: 'thin',
    frameColor: 0xdcddc0,    // el crema de las casillas, apenas más oscuro
    lineColor: 0x5f7a45,     // línea fina verde entre el marco y las casillas
    floorY: -0.18,           // donde apoyan el marco, la sombra y las piezas capturadas
    label: { color: '#5a7340', size: 0.3, edge: 4.24 },
    hover: [0xffffff, 0.2],
    selected: [0xf6f669, 0.55],
    selectedFrame: [0xc9c43a, 0.95],
    lastFrom: [0xf6f669, 0.35],
    lastTo: [0xf6f669, 0.45],
    move: [0x1f1f1f, 0.36],
    capture: [0x1f1f1f, 0.4],
    checkFrame: [0xe03a2c, 0.9],
    checkGlow: [0xff3b30, 0.3],
    // Modo día (página en claro): arena y madera clara. Lo que no está acá queda como de noche.
    day: { light: 0xf0d9b5, dark: 0xb58863, frameColor: 0xcfb791, lineColor: 0x8b6a45, labelColor: '#6b4e2e' },
  },
  madera: {
    light: 0xdccaa3,
    dark: 0x84633f,
    noise: true,
    roughness: { light: 0.78, dark: 0.7 },
    frame: 'wood',
    frameColor: 0x37271b,
    frameEdge: 0x3a2a1f,
    lineColor: 0xb08d57,
    floorY: -0.5,
    label: { color: '#cdb68a', size: 0.42, edge: 4.38 },
    hover: [0xffffff, 0.14],
    selected: [0xe6b85c, 0.45],
    selectedFrame: [0xe6b85c, 0.95],
    lastFrom: [0xd9c25a, 0.2],
    lastTo: [0xd9c25a, 0.3],
    move: [0x3fbf9f, 0.75],
    capture: [0xd2463b, 0.9],
    checkFrame: [0xff3b30, 0.9],
    checkGlow: [0xff3b30, 0.25],
  },
};

/** Tema elegido: 'clasico' o 'madera'. */
export const BOARD_THEME = 'clasico';
const THEME = THEMES[BOARD_THEME];

/** Colores del tablero según el tema de la página: de noche los del tema del tablero; de día, THEME.day. */
function pageColors(pageTheme) {
  const night = {
    light: THEME.light, dark: THEME.dark, frameColor: THEME.frameColor,
    lineColor: THEME.lineColor, labelColor: THEME.label.color,
  };
  return pageTheme === 'light' ? { ...night, ...THEME.day } : night;
}

const BOARD_THEME_MS = 200;   // como la transición del CSS

/** Altura del "piso" alrededor del tablero (ahí van las piezas capturadas). */
export const BOARD_FLOOR_Y = THEME.floorY;

// ---------- Coordenadas ----------

/**
 * Columna a..h (file 0..7) -> x = 0..7 y fila 1..8 (rank) -> z = 7..0, centrado restando 3.5.
 * Las blancas (fila 1) quedan en +Z, del lado de la cámara.
 */
export function toWorld(file, rank) {
  return new THREE.Vector3(file - 3.5, 0, (8 - rank) - 3.5);
}

/** Inversa de toWorld; devuelve null si el punto cae fuera del tablero. */
export function fromWorld(x, z) {
  const file = Math.round(x + 3.5);
  const rank = 8 - Math.round(z + 3.5);
  if (file < 0 || file > 7 || rank < 1 || rank > 8) return null;
  return { file, rank };
}

/** "e2" -> posición en el mundo (usa toWorld). */
export function squareToWorld(square) {
  return toWorld(FILES.indexOf(square[0]), Number(square.slice(1)));
}

/** posición en el mundo -> "e2" (usa fromWorld). */
export function worldToSquare(x, z) {
  const cell = fromWorld(x, z);
  return cell ? FILES[cell.file] + cell.rank : null;
}

// ---------- Construcción del tablero ----------

export function createBoard(scene) {
  const group = new THREE.Group();
  group.name = 'board';
  scene.add(group);

  // Casillas: color liso (clásico) o textura pixelada sutil con filtro NEAREST (madera)
  const squareGeometry = new THREE.BoxGeometry(1, SQUARE_HEIGHT, 1);
  const squareMaterial = (isLight, variant) => {
    const roughness = isLight ? THEME.roughness.light : THEME.roughness.dark;
    if (!THEME.noise) {
      return new THREE.MeshStandardMaterial({ color: isLight ? THEME.light : THEME.dark, roughness, metalness: 0 });
    }
    const seed = (isLight ? 11 : 29) + variant;
    return new THREE.MeshStandardMaterial({
      map: pixelNoiseTexture(isLight ? THEME.light : THEME.dark, seed), roughness, metalness: 0,
    });
  };
  // Se comparten: 2 materiales lisos, o 3 variantes de ruido por color
  const materials = { light: [0, 1, 2].map((i) => squareMaterial(true, i)), dark: [0, 1, 2].map((i) => squareMaterial(false, i)) };
  const squares = [];
  for (let file = 0; file < 8; file++) {
    for (let rank = 1; rank <= 8; rank++) {
      const isLight = (file + rank) % 2 === 0; // a1 (0 + 1) es oscura
      const variants = materials[isLight ? 'light' : 'dark'];
      const material = THEME.noise ? variants[(file * 3 + rank) % variants.length] : variants[0];
      const mesh = new THREE.Mesh(squareGeometry, material);
      mesh.position.copy(toWorld(file, rank));
      mesh.position.y = -SQUARE_HEIGHT / 2;
      mesh.receiveShadow = true;
      mesh.userData.square = FILES[file] + rank;
      group.add(mesh);
      squares.push(mesh);
    }
  }

  const { frame: frameMaterial, line: lineMaterial } = THEME.frame === 'thin' ? addThinFrame(group) : addWoodFrame(group);

  // Piso invisible que solo recibe la sombra del tablero (da sensación de apoyo)
  const ground = new THREE.Mesh(new THREE.CircleGeometry(16, 48), new THREE.ShadowMaterial({ opacity: 0.3 }));
  ground.rotation.x = -Math.PI / 2;
  ground.position.y = THEME.floorY;
  ground.receiveShadow = true;
  group.add(ground);

  const labels = addLabels(group);

  // Tema de la página: casillas, marco y línea pasan suave al otro color; las letras se redibujan.
  // (El tablero de madera tiene textura con grano: ese no cambia de color.)
  const tinted = THEME.noise ? [] : [
    [materials.light, 'light'], [materials.dark, 'dark'], [[frameMaterial], 'frameColor'], [[lineMaterial], 'lineColor'],
  ];
  function applyPageTheme(pageTheme, animate) {
    const colors = pageColors(pageTheme);
    labels.setColor(colors.labelColor);
    const steps = tinted.map(([list, key]) => [list, list[0].color.clone(), new THREE.Color(colors[key])]);
    const paint = (k) => {
      for (const [list, from, to] of steps) for (const material of list) material.color.copy(from).lerp(to, k);
    };
    if (animate) tween({ duration: BOARD_THEME_MS, easing: ease.outCubic, onUpdate: paint, ambient: true });
    else paint(1);
  }
  applyPageTheme(getTheme(), false);
  onThemeChange((pageTheme) => applyPageTheme(pageTheme, true));

  const overlays = createOverlays(group);
  return { group, squares, ...overlays };
}

/** Clásico: marco delgado y bajo del crema de las casillas, con una línea fina verde junto a ellas. */
function addThinFrame(group) {
  const height = -THEME.floorY;
  const frameMaterial = new THREE.MeshStandardMaterial({ color: THEME.frameColor, roughness: 0.9, metalness: 0 });
  const frame = new THREE.Mesh(new THREE.BoxGeometry(8.9, height, 8.9), frameMaterial);
  frame.position.y = -height / 2 - 0.012;
  frame.receiveShadow = true;
  frame.castShadow = true;
  group.add(frame);

  const lineMaterial = new THREE.MeshStandardMaterial({ color: THEME.lineColor, roughness: 0.9, metalness: 0 });
  const line = new THREE.Mesh(new THREE.BoxGeometry(8.1, 0.02, 8.1), lineMaterial);
  line.position.y = -0.011;
  line.receiveShadow = true;
  group.add(line);
  return { frame: frameMaterial, line: lineMaterial };
}

/** Madera: marco oscuro con un bisel dorado y un zócalo más ancho abajo (el tablero anterior). */
function addWoodFrame(group) {
  const woodMap = pixelNoiseTexture(THEME.frameColor, 71, 32, 0.08);
  woodMap.repeat.set(3, 3);
  woodMap.wrapS = woodMap.wrapT = THREE.RepeatWrapping;
  const frameMaterial = new THREE.MeshStandardMaterial({ map: woodMap, roughness: 0.62, metalness: 0 });
  const frame = new THREE.Mesh(new THREE.BoxGeometry(9.5, 0.34, 9.5), frameMaterial);
  frame.position.y = -0.17 - 0.012;
  frame.receiveShadow = true;
  frame.castShadow = true;
  group.add(frame);

  const lip = new THREE.Mesh(
    new THREE.BoxGeometry(8.14, 0.02, 8.14),
    new THREE.MeshStandardMaterial({ color: THEME.lineColor, roughness: 0.45, metalness: 0.25 }),
  );
  lip.position.y = -0.012;
  lip.receiveShadow = true;
  group.add(lip);

  const plinth = new THREE.Mesh(
    new THREE.BoxGeometry(9.9, 0.14, 9.9),
    new THREE.MeshStandardMaterial({ color: THEME.frameEdge, roughness: 0.7 }),
  );
  plinth.position.y = -0.42;
  plinth.receiveShadow = true;
  plinth.castShadow = true;
  group.add(plinth);
  return { frame: frameMaterial, line: lip.material };
}

/**
 * Letras a–h y números 1–8 en el borde, en los cuatro lados (los de enfrente, girados).
 * Devuelve setColor(color): rehace las texturas con otro color (al cambiar el tema de la página).
 */
function addLabels(group) {
  const geometry = new THREE.PlaneGeometry(THEME.label.size, THEME.label.size);
  const y = -0.01;
  const edge = THEME.label.edge;
  const materials = [];   // [material, texto]
  let color = THEME.label.color;
  const make = (text, x, z, flipped) => {
    const material = new THREE.MeshBasicMaterial({ map: labelTexture(text, color), transparent: true, depthWrite: false });
    materials.push([material, text]);
    const mesh = new THREE.Mesh(geometry, material);
    mesh.rotation.x = -Math.PI / 2;
    if (flipped) mesh.rotation.z = Math.PI;
    mesh.position.set(x, y, z);
    group.add(mesh);
  };
  for (let i = 0; i < 8; i++) {
    const fileX = toWorld(i, 1).x;
    make(FILES[i], fileX, edge, false);       // lado blancas
    make(FILES[i], fileX, -edge, true);       // lado negras
    const rankZ = toWorld(0, i + 1).z;
    make(String(i + 1), -edge, rankZ, false);
    make(String(i + 1), edge, rankZ, true);
  }
  return {
    setColor(next) {
      if (next === color) return;
      color = next;
      for (const [material, text] of materials) {
        material.map.dispose();
        material.map = labelTexture(text, color);
        material.needsUpdate = true;
      }
    },
  };
}

// ---------- Resaltados ----------

function createOverlays(group) {
  const flat = (geometry, color, opacity) => {
    const mesh = new THREE.Mesh(geometry, new THREE.MeshBasicMaterial({
      color, transparent: true, opacity, depthWrite: false,
    }));
    mesh.rotation.x = -Math.PI / 2;
    mesh.visible = false;
    mesh.renderOrder = 2;
    group.add(mesh);
    return mesh;
  };

  const squarePlane = new THREE.PlaneGeometry(1, 1);
  const hover = flat(squarePlane, ...THEME.hover);
  const selected = flat(squarePlane, ...THEME.selected);
  const lastFrom = flat(squarePlane, ...THEME.lastFrom);
  const lastTo = flat(squarePlane, ...THEME.lastTo);

  // Las piezas tapan casi toda su casilla (base de 0.875), así que los avisos sobre una
  // casilla ocupada son MARCOS en el borde que queda a la vista. Un anillo de 4 lados girado
  // 45° es un marco cuadrado: medio lado = radio · cos 45° (acá, de 0.43 a 0.515).
  const frameGeometry = new THREE.RingGeometry(0.61, 0.73, 4, 1, Math.PI / 4);
  const selectedFrame = flat(frameGeometry, ...THEME.selectedFrame);
  const checkFrame = flat(frameGeometry, ...THEME.checkFrame);
  const checkGlow = flat(squarePlane, ...THEME.checkGlow);
  const checkFrameOpacity = THEME.checkFrame[1];
  const checkGlowOpacity = THEME.checkGlow[1];

  // Destinos: disco para casilla libre, marco para captura
  const discGeometry = new THREE.CircleGeometry(0.16, 28);
  const targets = [];

  const place = (mesh, square, lift = 0) => {
    if (!square) { mesh.visible = false; return; }
    mesh.position.copy(squareToWorld(square));
    mesh.position.y = OVERLAY_Y + lift;
    mesh.visible = true;
  };

  let checkSquare = null;

  return {
    setHover: (square) => place(hover, square, 0.001),
    setSelected: (square) => { place(selected, square, 0.002); place(selectedFrame, square, 0.006); },
    setLastMove: (move) => { place(lastFrom, move?.from); place(lastTo, move?.to); },
    setCheck: (square) => {
      checkSquare = square;
      place(checkFrame, square, 0.004);
      place(checkGlow, square, 0.003);
    },
    showTargets: (moves) => {
      clearTargets();
      for (const { to, capture } of moves) {
        const mesh = flat(capture ? frameGeometry : discGeometry, ...(capture ? THEME.capture : THEME.move));
        place(mesh, to, 0.005);
        mesh.userData.square = to;
        targets.push(mesh);
      }
    },
    clearTargets,
    /** Pulso del aviso de jaque (se llama en cada frame). */
    update: (now) => {
      if (!checkSquare) return;
      const pulse = 0.5 + 0.5 * Math.sin(now / 180);
      checkFrame.material.opacity = checkFrameOpacity * (0.6 + 0.4 * pulse);
      checkGlow.material.opacity = checkGlowOpacity * (0.4 + 0.6 * pulse);
    },
  };

  function clearTargets() {
    for (const mesh of targets) {
      group.remove(mesh);
      mesh.material.dispose();
    }
    targets.length = 0;
  }
}

// ---------- Texturas generadas por código ----------

/** Textura de 16x16 con ruido por píxel: da grano "pixel art" sin archivos extra. */
function pixelNoiseTexture(hex, seed, size = 16, amount = 0.05) {
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = size;
  const ctx = canvas.getContext('2d');
  const base = new THREE.Color(hex);
  let s = seed;
  const random = () => ((s = (s * 16807) % 2147483647) / 2147483647);
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const k = 1 + (random() - 0.5) * 2 * amount;
      const c = base.clone().multiplyScalar(k);
      ctx.fillStyle = `rgb(${c.r * 255 | 0},${c.g * 255 | 0},${c.b * 255 | 0})`;
      ctx.fillRect(x, y, 1, 1);
    }
  }
  const texture = new THREE.CanvasTexture(canvas);
  texture.magFilter = THREE.NearestFilter;
  texture.minFilter = THREE.NearestFilter;
  texture.generateMipmaps = false;
  texture.colorSpace = THREE.SRGBColorSpace;
  return texture;
}

function labelTexture(text, color) {
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = 128;
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = color;
  ctx.font = '600 82px "Iowan Old Style", Palatino, Georgia, serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(text, 64, 70);
  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.anisotropy = 4;
  return texture;
}
