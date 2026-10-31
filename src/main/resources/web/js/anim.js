// anim.js — tweens simples sobre requestAnimationFrame (sin librerías).
// Cada tween devuelve una Promise, así las animaciones se encadenan con await.

const tweens = new Set();

export const ease = {
  linear: (t) => t,
  inOutCubic: (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2),
  outCubic: (t) => 1 - Math.pow(1 - t, 3),
  inCubic: (t) => t * t * t,
  outBack: (t) => {
    const c1 = 1.70158, c3 = c1 + 1;
    return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
  },
};

/** Con prefers-reduced-motion: tweens casi instantáneos, sin giro de cámara ni saltitos. */
export const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

/**
 * Corre `onUpdate(k)` con k de 0 a 1 durante `duration` ms.
 * ambient = true: un cambio de ambiente (el fundido de colores del tema) que no bloquea el juego (isAnimating).
 */
export function tween({ duration, delay = 0, easing = ease.inOutCubic, onUpdate, ambient = false }) {
  return new Promise((resolve) => {
    const d = reducedMotion ? Math.min(duration, 60) : duration;
    tweens.add({ start: performance.now() + delay, duration: d, easing, onUpdate, resolve, ambient });
  });
}

/** Se llama una vez por frame desde el loop de render. */
export function updateTweens(now) {
  for (const t of tweens) {
    if (now < t.start) continue;
    const raw = Math.min(1, (now - t.start) / t.duration);
    t.onUpdate(t.easing(raw), raw);
    if (raw >= 1) {
      tweens.delete(t);
      t.resolve();
    }
  }
}

export const isAnimating = () => [...tweens].some((t) => !t.ambient);

/**
 * Movimiento en arco: x/z van en línea recta y la altura sigue una parábola.
 * El caballo salta más alto (es la pieza que "salta" por encima de las demás).
 */
export function animateMove(object, to, { jumpy = false, duration = 350 } = {}) {
  const from = object.position.clone();
  const distance = Math.hypot(to.x - from.x, to.z - from.z);
  const height = jumpy ? 1.15 + distance * 0.08 : 0.35 + distance * 0.09;
  return tween({
    duration: jumpy ? duration + 60 : duration,
    easing: ease.inOutCubic,
    onUpdate: (k) => {
      object.position.x = from.x + (to.x - from.x) * k;
      object.position.z = from.z + (to.z - from.z) * k;
      // parábola: 0 en los extremos, `height` en el medio (sobre la recta entre las alturas)
      object.position.y = from.y + (to.y - from.y) * k + 4 * height * k * (1 - k);
    },
  });
}

/** Captura: la pieza se achica desde SU escala (PIECE_SCALE en el tablero), gira un poco y se desvanece. */
export function animateCapture(object, { delay = 0 } = {}) {
  const startScale = object.scale.x;
  const startRotation = object.rotation.y;
  setOpacity(object, 1, true);
  return tween({
    duration: 300,
    delay,
    easing: ease.inCubic,
    onUpdate: (k) => {
      object.scale.setScalar(startScale * (1 - 0.85 * k));
      object.rotation.y = startRotation + k * 1.2;
      object.position.y = -0.05 * k;
      setOpacity(object, 1 - k);
    },
  });
}

/**
 * Aparición (piezas nuevas en el tablero o en la fila de capturadas). targetScale es la escala
 * final: PIECE_SCALE en el tablero, CAPTURED_SCALE en la fila de capturadas (las pasa pieces.js).
 */
export function animatePop(object, targetScale, delay = 0) {
  object.scale.setScalar(0.001);
  return tween({
    duration: 320,
    delay,
    easing: ease.outBack,
    onUpdate: (k) => object.scale.setScalar(Math.max(0.001, targetScale * k)),
  });
}

/** Sube o baja la pieza seleccionada (levantarla 0.15). */
export function animateLift(object, y) {
  const from = object.position.y;
  return tween({ duration: 120, easing: ease.outCubic, onUpdate: (k) => { object.position.y = from + (y - from) * k; } });
}

/**
 * Cambia la opacidad de todos los materiales de la pieza (sus materiales ya son clones propios).
 * Un mesh con userData.baseOpacity (la sombra de contacto) escala su propia opacidad.
 */
export function setOpacity(object, opacity, prepare = false) {
  object.traverse((child) => {
    if (!child.isMesh) return;
    for (const material of [].concat(child.material)) {
      if (prepare) material.transparent = true;
      material.opacity = opacity * (child.userData.baseOpacity ?? 1);
    }
  });
}
