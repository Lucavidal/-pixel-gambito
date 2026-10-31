// api.js — ÚNICO módulo que conoce las URLs del backend (adaptador web en Java).
// El front no valida nada: todo lo que sea regla del ajedrez se le pregunta al servidor.
// La sesión viaja sola en una cookie HttpOnly: este módulo nunca ve el token.

import { t } from './i18n.js';

const BASE = '/api';

let unauthorizedHandler = () => {};

async function request(method, path, body) {
  const options = { method, headers: {} };
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }
  const response = await fetch(BASE + path, options);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    // 401 fuera de /auth = la sesión se cerró (por ejemplo, se reinició el servidor)
    if (response.status === 401 && !path.startsWith('/auth/')) unauthorizedHandler();
    // 400 = pedido mal armado (casilla inválida); el servidor manda { message }
    const error = new Error(data.message || t('error.server', { status: response.status }));
    error.status = response.status;
    throw error;
  }
  return data;
}

export const api = {
  /** Qué hacer si el servidor dice que no hay sesión en medio del juego. */
  onUnauthorized(handler) { unauthorizedHandler = handler; },

  auth: {
    /** El usuario logueado ({ username, displayName }); si no hay sesión, devuelve null. */
    me: () => request('GET', '/auth/me').catch((error) => {
      if (error.status === 401) return null;
      throw error;
    }),
    /** { success, result, message, username, displayName }: result es el AuthResult del núcleo. */
    login: (username, password) => request('POST', '/auth/login', { username, password }),
    /** displayName es opcional ("¿Cómo te llamamos?"); vacío = el servidor usa el usuario. */
    register: (username, password, displayName) =>
      request('POST', '/auth/register', { username, password, displayName }),
    logout: () => request('POST', '/auth/logout'),
  },

  /** La partida libre del usuario logueado. */
  game: {
    /** Foto completa de la partida (piezas, turno, jaque, historial, capturadas). */
    state: () => request('GET', '/game/state'),

    /** Destinos legales de la pieza en `from` ("e2"): { from, moves: [{ to, capture }] }. */
    legalMoves: (from) => request('GET', `/game/moves?from=${encodeURIComponent(from)}`),

    /** Intenta mover. Responde { result, success, message, state } con el MoveResult del núcleo. */
    move: (from, to) => request('POST', '/game/move', { from, to }),

    undo: () => request('POST', '/game/undo'),
    redo: () => request('POST', '/game/redo'),
    reset: () => request('POST', '/game/reset'),
  },

  /**
   * El torneo escalera del usuario logueado. Las respuestas traen state = { status, round, rivals,
   * awaitingOpponent, board, ... }; board es la misma foto que la partida libre.
   */
  tournament: {
    state: () => request('GET', '/tournament/state'),
    /** Torneo nuevo en la ronda 1, con la escalera desde esa dificultad (EASY / MEDIUM / HARD; sin nivel, la última). */
    start: (level) => request('POST', '/tournament/start', level ? { level } : undefined),
    legalMoves: (from) => request('GET', `/tournament/moves?from=${encodeURIComponent(from)}`),
    /** SOLO la jugada del usuario: { result, success, message, move, roundResult, state }. */
    move: (from, to) => request('POST', '/tournament/move', { from, to }),
    /** La respuesta de la IA (mismo formato). Solo después de una jugada válida del usuario. */
    opponent: () => request('POST', '/tournament/opponent'),
  },

  /**
   * "Mi historial": { username, displayName, difficulty, summary: { text, record, wins, ... },
   * rounds: [{ date, rival, difficulty, difficultyText, outcomeText, ... }], difficulties: [{ id, name }] }.
   * difficulty (EASY / MEDIUM / HARD) filtra las rondas y los totales; sin ella, todas.
   */
  history: (limit = 50, difficulty = null) =>
    request('GET', `/history?limit=${limit}${difficulty ? `&difficulty=${encodeURIComponent(difficulty)}` : ''}`),
};
