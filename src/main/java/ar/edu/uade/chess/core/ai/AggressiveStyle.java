package ar.edu.uade.chess.core.ai;

/** Prioriza capturar y dar jaque; no le preocupa dejar piezas atacadas. */
public final class AggressiveStyle implements IPlayStyle {

    private static final int CAPTURE_WEIGHT = 10;
    private static final int CHECK_BONUS = 50;

    @Override
    public int score(MoveFacts facts) {
        int score = facts.capturedValue() * CAPTURE_WEIGHT;
        if (facts.givesCheck()) {
            score += CHECK_BONUS;
        }
        if (facts.givesCheckmate()) {
            score += CHECKMATE_BONUS;
        }
        return score;
    }

    @Override
    public String getDisplayName() { return "Agresivo"; }

    @Override
    public String getDescription() { return "Busca capturar piezas y dar jaque, aunque deje piezas expuestas."; }
}
