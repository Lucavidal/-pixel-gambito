package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.core.auth.User;

/**
 * Lo que el servidor recuerda de un usuario que inició sesión en el navegador:
 * quién es, su propia partida libre y su propio torneo (que guarda sus rondas en
 * el historial de ESE usuario). Cada usuario juega en su tablero, sin pisarse con los demás.
 */
public record UserSession(User user, WebGameSession game, WebTournamentSession tournament) {
}
