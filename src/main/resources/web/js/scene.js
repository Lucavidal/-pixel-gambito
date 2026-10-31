// scene.js — renderer, luces y cámara. No sabe nada de ajedrez.
// Con el tema de la página (theme.js) cambian el color de fondo y la luz de ambiente; las piezas no.
import * as THREE from 'three';
import { getTheme, onThemeChange } from './theme.js';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { tween, ease, reducedMotion } from './anim.js';

/** Fondo (el mismo que --bg en styles.css) y luces por tema: en claro, un poco más de ambiente. */
const PAGE_THEMES = {
  dark: { background: 0x2b2a28, hemi: 1.05, sun: 2.2 },     // gris oscuro cálido
  light: { background: 0xebe6dc, hemi: 1.35, sun: 2.0 },    // crema
};
const THEME_MS = 200;
const FOV = 35;
const POLAR = 0.87;              // ~50° desde la vertical
const FIT_DISTANCE = 16.5;       // distancia a la que el tablero entra justo en 16:10 (sin taparse con la barra)
const FIT_ASPECT = 1.6;

// Vista del inicio: más cerca, más baja y con más perspectiva. El canvas sigue a pantalla completa;
// setViewOffset corre el centro de la imagen para que el tablero quede en la mitad izquierda
// (o arriba, en pantallas angostas).
const HOME_FOV = 44;
const HOME_POLAR = 0.98;          // ~56° desde la vertical
const HOME_THETA = Math.PI / 4;   // en diagonal, desde la esquina de h1
const HOME_RADIUS = 6.1;          // lo que tiene que entrar en su mitad: tablero, marco y piezas
const HOME_ORBIT_SPEED = 0.045;   // rad/s: una vuelta cada ~2 min 20 s
const STACK_WIDTH = 900;          // igual que el @media de .home en styles.css
const STACK_BOARD = 0.55;         // apilado, el tablero ocupa el 55 % de arriba
const FLIGHT_MS = 900;

export function createScene(container) {
  // Render: antialias, sRGB, tone mapping suave y sombras suaves
  const renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setSize(container.clientWidth, container.clientHeight);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.15;
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  const background = new THREE.Color(PAGE_THEMES[getTheme()].background);
  renderer.setClearColor(background, 1); // fondo liso (el mismo color que el body en styles.css)
  container.appendChild(renderer.domElement);

  const scene = new THREE.Scene();

  // Ambiente: el suelo es claro para que el lado en sombra de las piezas no quede negro
  const hemi = new THREE.HemisphereLight(0xfff6e8, 0x8f887c, 1.05);
  scene.add(hemi);

  // Las luces direccionales van en un grupo que gira con la cámara al "Ver desde negras":
  // así la principal siempre viene de adelante a la izquierda y el contraluz desde atrás.
  const rig = new THREE.Group();
  scene.add(rig);

  // Luz principal alta, desde adelante a la izquierda, con sombras suaves
  const sun = new THREE.DirectionalLight(0xfff2de, 2.2);
  sun.position.set(-4, 12, 7);
  sun.castShadow = true;
  sun.shadow.mapSize.set(2048, 2048);
  // la cámara de sombra abarca justo el tablero + el marco + las filas de capturadas
  const s = sun.shadow.camera;
  s.left = -6.2; s.right = 6.2; s.top = 6.2; s.bottom = -6.2;
  s.near = 4; s.far = 26;
  sun.shadow.bias = -0.0004;
  sun.shadow.normalBias = 0.025;
  sun.shadow.radius = 5;
  rig.add(sun);

  // Contraluz cálido desde atrás, sin sombras: marca el contorno de cada pieza contra el tablero
  const rim = new THREE.DirectionalLight(0xffd9a8, 0.8);
  rim.position.set(0, 6, -9);
  rig.add(rim);

  // Relleno frío muy tenue desde la derecha (el lado opuesto a la luz principal)
  const fill = new THREE.DirectionalLight(0xb8c4ff, 0.25);
  fill.position.set(7, 5, 2);
  rig.add(fill);

  // Tema de la página: el fondo y las luces pasan suave (200 ms, como el CSS) al otro tema
  const applyLights = ({ hemi: h, sun: s }) => { hemi.intensity = h; sun.intensity = s; };
  applyLights(PAGE_THEMES[getTheme()]);
  onThemeChange((theme) => {
    const to = PAGE_THEMES[theme];
    const fromColor = background.clone();
    const toColor = new THREE.Color(to.background);
    const from = { hemi: hemi.intensity, sun: sun.intensity };
    tween({
      duration: THEME_MS,
      easing: ease.outCubic,
      ambient: true,
      onUpdate: (k) => {
        background.copy(fromColor).lerp(toColor, k);
        renderer.setClearColor(background, 1);
        applyLights({ hemi: from.hemi + (to.hemi - from.hemi) * k, sun: from.sun + (to.sun - from.sun) * k });
      },
    });
  });

  // Cámara más baja y en perspectiva, como en una foto; las blancas quedan del lado de la cámara (+Z)
  const camera = new THREE.PerspectiveCamera(FOV, container.clientWidth / container.clientHeight, 0.1, 100);
  const controls = new OrbitControls(camera, renderer.domElement);
  controls.target.set(0, 0, 0);
  controls.enableDamping = true;
  controls.dampingFactor = 0.08;
  controls.enablePan = false;
  controls.minDistance = 7;
  controls.maxDistance = 24;
  controls.minPolarAngle = 0.12;          // casi cenital
  controls.maxPolarAngle = Math.PI * 0.43; // nunca por debajo del tablero
  controls.rotateSpeed = 0.7;

  let side = 'white';
  placeCamera(camera, side, container);
  controls.update();

  // 'game' = la mueve OrbitControls; 'home' = gira sola alrededor del tablero; 'flying' = la mueve un tween
  let cameraMode = 'game';
  let homeTheta = HOME_THETA;
  let offset = { x: 0, y: 0 };   // corrimiento del centro de la imagen, en fracción de la pantalla
  let flight = 0;                // descarta un vuelo viejo si empieza otro
  let lastNow = 0;
  const spherical = new THREE.Spherical();

  // Responsive: tamaño y aspecto siguen a la ventana; si cambia mucho el aspecto
  // (girar el celular, achicar la ventana) se re-encuadra para que entre el tablero.
  let lastFit = fitDistance(container);
  function onResize() {
    // Oculto (se juega en 2D) mide 0x0: no se toca nada; al volver a mostrarse se llama de nuevo.
    if (!container.clientWidth || !container.clientHeight) return;
    camera.aspect = container.clientWidth / container.clientHeight;
    camera.updateProjectionMatrix();
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(container.clientWidth, container.clientHeight);
    const fit = fitDistance(container);
    controls.maxDistance = Math.max(24, fit * 1.3);
    // En el inicio (o volando) la pose se recalcula en cada frame con el tamaño nuevo
    if (cameraMode === 'game' && Math.abs(fit - lastFit) / lastFit > 0.05) {
      const offset = camera.position.clone().sub(controls.target).setLength(fit);
      camera.position.copy(controls.target).add(offset);
      lastFit = fit;
    }
  }
  controls.maxDistance = Math.max(24, lastFit * 1.3);
  window.addEventListener('resize', onResize);

  /** Gira la cámara 180° alrededor del tablero (lado de blancas <-> negras). */
  async function flipCamera() {
    side = side === 'white' ? 'black' : 'white';
    const offset = camera.position.clone().sub(controls.target);
    const spherical = new THREE.Spherical().setFromVector3(offset);
    const startTheta = spherical.theta;
    const targetTheta = side === 'white' ? 0 : Math.PI;
    let delta = targetTheta - startTheta;
    delta = Math.atan2(Math.sin(delta), Math.cos(delta)); // camino más corto
    if (Math.abs(delta) < 0.01) delta = Math.PI;
    controls.enabled = false; // que el mouse no pelee con la animación
    const startRig = rig.rotation.y;
    const targetRig = side === 'white' ? 0 : Math.PI;
    await tween({
      duration: 700,
      easing: ease.inOutCubic,
      onUpdate: (k) => {
        spherical.theta = startTheta + delta * k;
        camera.position.setFromSpherical(spherical).add(controls.target);
        camera.lookAt(controls.target);
        rig.rotation.y = startRig + (targetRig - startRig) * k; // las luces acompañan a la cámara
      },
    });
    controls.enabled = true;
    return side;
  }

  /** Pose del inicio: el tablero entra justo en su mitad de la pantalla (o en el 55 % de arriba). */
  function homePose(theta) {
    const w = container.clientWidth;
    const h = container.clientHeight;
    const stacked = w < STACK_WIDTH;
    const regionW = stacked ? w : w / 2;
    const regionH = stacked ? h * STACK_BOARD : h;
    // El ángulo más chico (horizontal o vertical) de esa región decide la distancia
    const tan = Math.tan(THREE.MathUtils.degToRad(HOME_FOV) / 2) * Math.min(regionW, regionH) / h;
    return {
      theta,
      polar: HOME_POLAR,
      distance: HOME_RADIUS / Math.sin(Math.atan(tan)),
      fov: HOME_FOV,
      offsetX: stacked ? 0 : 0.25,
      offsetY: stacked ? 0.5 - STACK_BOARD / 2 : 0,
    };
  }

  const gamePose = () => ({
    theta: side === 'white' ? 0 : Math.PI, polar: POLAR, distance: fitDistance(container), fov: FOV, offsetX: 0, offsetY: 0,
  });

  function currentPose() {
    spherical.setFromVector3(camera.position.clone().sub(controls.target));
    return { theta: spherical.theta, polar: spherical.phi, distance: spherical.radius, fov: camera.fov, offsetX: offset.x, offsetY: offset.y };
  }

  function applyPose(pose) {
    const w = container.clientWidth;
    const h = container.clientHeight;
    offset = { x: pose.offsetX, y: pose.offsetY };
    camera.fov = pose.fov;
    if (w && h && (offset.x || offset.y)) camera.setViewOffset(w, h, offset.x * w, offset.y * h, w, h);
    else camera.clearViewOffset();
    camera.updateProjectionMatrix();
    spherical.set(pose.distance, pose.polar, pose.theta);
    camera.position.setFromSpherical(spherical).add(controls.target);
    camera.lookAt(controls.target);
    rig.rotation.y = pose.theta; // las luces acompañan a la cámara, como al girar el tablero
  }

  /** Vuela desde donde esté la cámara hasta la pose que devuelve `target` (se recalcula si cambia la ventana). */
  async function fly(target, endMode) {
    const thisFlight = ++flight;
    controls.enabled = false;
    cameraMode = 'flying';
    const from = currentPose();
    const turn = (to) => Math.atan2(Math.sin(to - from.theta), Math.cos(to - from.theta)); // camino más corto
    await tween({
      duration: FLIGHT_MS,
      easing: ease.inOutCubic,
      onUpdate: (k) => {
        if (thisFlight !== flight) return;
        const to = target();
        const mix = (key) => from[key] + (to[key] - from[key]) * k;
        applyPose({
          theta: from.theta + turn(to.theta) * k,
          polar: mix('polar'), distance: mix('distance'), fov: mix('fov'), offsetX: mix('offsetX'), offsetY: mix('offsetY'),
        });
      },
    });
    if (thisFlight !== flight) return false;
    cameraMode = endMode;
    return true;
  }

  /** De la vista del inicio a la de juego (lado blancas). Si ya está en la de juego, no hace nada. */
  async function toGame() {
    if (cameraMode === 'game') return;
    side = 'white';
    if (!await fly(gamePose, 'game')) return;
    lastFit = fitDistance(container);
    controls.enabled = true;
    controls.update();
  }

  /** A la vista del inicio; sin animar al abrir la página directamente en el inicio. */
  async function toHome({ animate = true } = {}) {
    if (cameraMode === 'home') return;
    homeTheta = HOME_THETA;
    if (animate) {
      await fly(() => homePose(homeTheta), 'home');
    } else {
      flight++;
      controls.enabled = false;
      applyPose(homePose(homeTheta));
      cameraMode = 'home';
    }
  }

  /** Una vez por frame: OrbitControls en la partida, giro lento en el inicio. */
  function update(now) {
    const dt = lastNow ? Math.min(now - lastNow, 100) / 1000 : 0;
    lastNow = now;
    if (cameraMode === 'game') {
      controls.update();
    } else if (cameraMode === 'home') {
      if (!reducedMotion) homeTheta += dt * HOME_ORBIT_SPEED;
      applyPose(homePose(homeTheta));
    }
  }

  return {
    renderer, scene, camera, controls, flipCamera, getSide: () => side, resize: onResize,
    update, toGame, toHome, cameraMode: () => cameraMode,
  };
}

/**
 * Distancia según el aspecto: en 16:10 el tablero (con las capturadas a los costados) entra justo;
 * en pantallas más angostas la cámara se aleja para que siga entrando.
 */
function fitDistance(container) {
  const aspect = container.clientWidth / container.clientHeight;
  return FIT_DISTANCE * Math.max(1, FIT_ASPECT / aspect);
}

function placeCamera(camera, side, container) {
  const distance = fitDistance(container);
  const polar = POLAR;
  const z = Math.sin(polar) * distance * (side === 'white' ? 1 : -1);
  camera.position.set(0, Math.cos(polar) * distance, z);
  camera.lookAt(0, 0, 0);
}
