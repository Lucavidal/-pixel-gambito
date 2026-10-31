package ar.edu.uade.chess.core.ai;

import ar.edu.uade.chess.core.game.IGame;

/**
 * Algo que, dada una partida, decide qué jugada hacer con el bando que tiene el turno.
 *
 * Es lo único que el torneo conoce del rival. Así el torneo no depende de cómo
 * piensa la IA, y en los tests se puede reemplazar por un rival "guionado".
 */
public interface IMoveChooser {

    /** Devuelve una jugada legal para el bando que tiene el turno. La partida no debe haber terminado. */
    Move chooseMove(IGame game);
}
