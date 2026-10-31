package ar.edu.uade.chess.core.ai;

/**
 * Patrón STRATEGY: un estilo de juego, intercambiable.
 *
 * Cada estilo responde una sola pregunta: "¿cuánto me gusta esta jugada?".
 * Recibe los datos ya calculados (MoveFacts), así que agregar un estilo nuevo
 * es escribir una fórmula nueva, sin tocar la IA ni el torneo.
 */
public interface IPlayStyle {

    /** Dar mate gana la partida: todos los estilos lo prefieren por sobre cualquier otra cosa. */
    int CHECKMATE_BONUS = 100_000;

    /** Puntaje de la jugada: mayor es mejor. */
    int score(MoveFacts facts);

    /** Nombre que ve el jugador ("Agresivo", ...). */
    String getDisplayName();

    /** Qué prioriza este estilo, en una frase para el jugador. */
    String getDescription();
}
