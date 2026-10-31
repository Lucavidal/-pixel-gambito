package ar.edu.uade.chess.core.setup;

import ar.edu.uade.chess.core.model.Board;

/**
 * Cómo arranca una partida: tamaño del tablero y disposición inicial.
 *
 * Es una interfaz porque SÍ hay variación real esperable: la defensa individual
 * puede pedir otro tamaño de tablero u otra disposición. Una configuración
 * nueva es una clase nueva que implementa esto; nada existente se modifica.
 */
public interface IBoardSetup {

    Board createBoard();
}
