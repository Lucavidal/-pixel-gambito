package ar.edu.uade.chess.core.ai;

import ar.edu.uade.chess.core.model.Position;

/**
 * Una jugada candidata: de dónde a dónde. Es un dato inmutable (record);
 * no sabe si es legal ni la ejecuta. Eso lo hace la partida (IGame).
 */
public record Move(Position from, Position to) {
}
