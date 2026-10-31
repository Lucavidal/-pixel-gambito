package ar.edu.uade.chess.adapters.console;

import ar.edu.uade.chess.core.model.Position;

/**
 * Traduce entre la notación que entiende una persona ("e2") y las coordenadas
 * del núcleo (fila 1, columna 4). Vive en el ADAPTADOR porque es un detalle de
 * presentación: el núcleo no sabe ni le importa cómo se escriben las casillas.
 *
 * Funciona para cualquier tamaño de tablero (columnas a..z, filas 1..n).
 */
public class AlgebraicNotation {

    /** "e2" -> Position(1, 4). Lanza IllegalArgumentException si el texto no tiene formato válido. */
    public Position toPosition(String text) {
        if (text == null || text.trim().length() < 2) {
            throw new IllegalArgumentException("Casilla inválida: " + text);
        }
        String clean = text.trim().toLowerCase();
        char letter = clean.charAt(0);
        if (letter < 'a' || letter > 'z') {
            throw new IllegalArgumentException("Columna inválida: " + letter);
        }
        int number;
        try {
            number = Integer.parseInt(clean.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Fila inválida: " + clean.substring(1));
        }
        return new Position(number - 1, letter - 'a');
    }

    /** Position(1, 4) -> "e2". */
    public String toText(Position position) {
        return "" + (char) ('a' + position.column()) + (position.row() + 1);
    }
}
