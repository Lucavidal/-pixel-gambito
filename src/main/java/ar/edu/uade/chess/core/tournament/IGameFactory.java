package ar.edu.uade.chess.core.tournament;

import ar.edu.uade.chess.core.game.IGame;

/**
 * Crea una partida nueva. El torneo la usa al empezar cada ronda (y al repetirla
 * por tablas), en vez de hacer "new Game" adentro: así el torneo no sabe con qué
 * tablero ni con qué reglas se juega, y en los tests se le pasan posiciones armadas.
 */
public interface IGameFactory {

    IGame create();
}
