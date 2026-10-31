package ar.edu.uade.chess.core.tournament;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.ai.IPlayStyle;

import java.util.Objects;

/**
 * Un rival del torneo, armado por COMPOSICIÓN: nombre + descripción + estilo + dificultad.
 *
 * No hay una clase por combinación (AgresivoDificil, DefensivoFacil, ...): con
 * 3 estilos y 3 dificultades serían 9 clases, y cada estilo nuevo sumaría 3 más.
 */
public record HistoricalPlayer(String name, String description, IPlayStyle style, Difficulty difficulty) {

    public HistoricalPlayer {
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        Objects.requireNonNull(style);
        Objects.requireNonNull(difficulty);
    }
}
