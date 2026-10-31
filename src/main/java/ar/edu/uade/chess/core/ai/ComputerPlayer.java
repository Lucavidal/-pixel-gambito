package ar.edu.uade.chess.core.ai;

import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La IA: elige una jugada combinando un ESTILO (qué jugada le parece mejor)
 * y una DIFICULTAD (cuánto se equivoca).
 *
 *  1. Arma la lista de jugadas legales pidiéndoselas a la partida (game.legalMoves),
 *     así nunca elige un movimiento ilegal ni uno que deje a su rey en jaque.
 *  2. Con la probabilidad de la dificultad, elige una al azar.
 *  3. Si no, elige la de mayor puntaje según el estilo; los empates se resuelven al azar.
 *
 * La aleatoriedad (Random) entra por constructor: con la misma semilla elige
 * siempre lo mismo, y eso hace que los tests sean deterministas.
 */
public class ComputerPlayer implements IMoveChooser {

    private final IPlayStyle style;
    private final Difficulty difficulty;
    private final MoveAnalyzer analyzer;
    private final Random random;

    public ComputerPlayer(IPlayStyle style, Difficulty difficulty, MoveAnalyzer analyzer, Random random) {
        this.style = style;
        this.difficulty = difficulty;
        this.analyzer = analyzer;
        this.random = random;
    }

    @Override
    public Move chooseMove(IGame game) {
        List<Move> candidates = legalMoves(game);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No hay jugadas legales: la partida ya terminó.");
        }
        if (random.nextDouble() < difficulty.randomMoveProbability()) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        return bestMove(game, candidates);
    }

    private Move bestMove(IGame game, List<Move> candidates) {
        List<Move> best = new ArrayList<>();
        int bestScore = Integer.MIN_VALUE;
        for (Move move : candidates) {
            int score = style.score(analyzer.analyze(game, move));
            if (score > bestScore) {
                bestScore = score;
                best.clear();
            }
            if (score == bestScore) {
                best.add(move);
            }
        }
        return best.get(random.nextInt(best.size()));
    }

    /** Todas las jugadas legales del bando que tiene el turno, en un orden fijo (fila por fila). */
    private List<Move> legalMoves(IGame game) {
        List<Move> moves = new ArrayList<>();
        for (Position origin : MoveAnalyzer.squaresOf(game.getBoard(), game.getTurn())) {
            for (Position destination : game.legalMoves(origin)) {
                moves.add(new Move(origin, destination));
            }
        }
        return moves;
    }

    public IPlayStyle getStyle() { return style; }
    public Difficulty getDifficulty() { return difficulty; }
}
