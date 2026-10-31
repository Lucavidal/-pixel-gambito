package ar.edu.uade.chess.core.ai;

/** Prioriza no dejar piezas atacadas y mantener piezas propias junto al rey. */
public final class DefensiveStyle implements IPlayStyle {

    private static final int HANGING_WEIGHT = 10;
    private static final int SHELTER_WEIGHT = 5;

    @Override
    public int score(MoveFacts facts) {
        int score = -facts.hangingValue() * HANGING_WEIGHT + facts.kingShelter() * SHELTER_WEIGHT;
        if (facts.givesCheckmate()) {
            score += CHECKMATE_BONUS;
        }
        return score;
    }

    @Override
    public String getDisplayName() { return "Defensivo"; }

    @Override
    public String getDescription() { return "Evita dejar piezas atacadas y mantiene piezas cerca de su rey."; }
}
