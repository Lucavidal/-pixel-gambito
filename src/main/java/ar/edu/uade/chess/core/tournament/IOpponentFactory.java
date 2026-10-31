package ar.edu.uade.chess.core.tournament;

import ar.edu.uade.chess.core.ai.IMoveChooser;

/**
 * Arma "el cerebro" de un rival a partir de su ficha (estilo + dificultad).
 *
 * En Main se conecta con la IA real (ComputerPlayer); en los tests, con un
 * rival que juega jugadas fijas. Así el torneo no depende de la IA concreta.
 */
public interface IOpponentFactory {

    IMoveChooser create(HistoricalPlayer rival);
}
