package ar.edu.uade.chess.core.ai;

/**
 * Dificultad de la IA: es un eje APARTE del estilo. El estilo dice qué jugada
 * le parece mejor; la dificultad dice cuántas veces "se equivoca" y juega al azar.
 *
 * Los porcentajes son una decisión de diseño fácil de cambiar (ver docs/CAMBIOS_TORNEO.md).
 * El orden de declaración es el orden de dificultad (de menor a mayor).
 */
public enum Difficulty {
    EASY("Fácil", 0.40),
    MEDIUM("Media", 0.15),
    HARD("Difícil", 0.0);

    private final String displayName;
    private final double randomMoveProbability;

    Difficulty(String displayName, double randomMoveProbability) {
        this.displayName = displayName;
        this.randomMoveProbability = randomMoveProbability;
    }

    public String getDisplayName() { return displayName; }

    /** Probabilidad (0 a 1) de elegir una jugada legal al azar en vez de la mejor según el estilo. */
    public double randomMoveProbability() { return randomMoveProbability; }
}
