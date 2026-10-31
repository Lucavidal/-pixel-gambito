package ar.edu.uade.chess.core.ai;

import ar.edu.uade.chess.core.game.GameStatus;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Direction;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Calcula los datos de una jugada (MoveFacts) SIMULÁNDOLA sobre la partida:
 * la hace con game.move(...), mira cómo quedó el tablero y la revierte con game.undo().
 *
 * Usa solo el puerto IGame, así que reutiliza toda la lógica de legalidad y de
 * jaque que ya existe (no la duplica) y funciona con cualquier tamaño de tablero
 * y cualquier pieza. Es el mismo razonamiento que MoveValidator usa para saber
 * si un movimiento deja al rey en jaque: simular y revertir.
 */
public class MoveAnalyzer {

    private final PieceValue pieceValue;

    public MoveAnalyzer(PieceValue pieceValue) {
        this.pieceValue = pieceValue;
    }

    /** La jugada debe ser legal para el bando que tiene el turno. La partida queda exactamente como estaba. */
    public MoveFacts analyze(IGame game, Move move) {
        BoardView board = game.getBoard();
        Color mover = game.getTurn();
        Piece target = board.pieceAt(move.to());
        int capturedValue = target == null ? 0 : valueOf(board, target);

        if (!game.move(move.from(), move.to()).isSuccess()) {
            throw new IllegalArgumentException("La jugada no es legal: " + move);
        }
        MoveFacts facts = new MoveFacts(
                capturedValue,
                game.isInCheck(mover.opposite()),
                game.getStatus() == GameStatus.CHECKMATE,
                hangingValue(game, mover),
                kingShelter(board, mover));
        game.undo();
        return facts;
    }

    /** Valor de las piezas propias que el rival (que ahora tiene el turno) puede capturar legalmente. */
    private int hangingValue(IGame game, Color own) {
        BoardView board = game.getBoard();
        Set<Position> attacked = new HashSet<>();
        for (Position origin : squaresOf(board, own.opposite())) {
            attacked.addAll(game.legalMoves(origin));
        }
        int total = 0;
        for (Position square : squaresOf(board, own)) {
            if (attacked.contains(square)) {
                total += valueOf(board, board.pieceAt(square));
            }
        }
        return total;
    }

    private int kingShelter(BoardView board, Color own) {
        for (Position square : squaresOf(board, own)) {
            if (board.pieceAt(square).isKing()) {
                int count = 0;
                for (Direction d : Direction.all()) {
                    Position neighbour = square.offset(d);
                    if (board.contains(neighbour) && !board.isEmpty(neighbour)
                            && board.pieceAt(neighbour).getColor() == own) {
                        count++;
                    }
                }
                return count;
            }
        }
        return 0;
    }

    private int valueOf(BoardView board, Piece piece) {
        return pieceValue.of(piece, board.rows(), board.columns());
    }

    /** Casillas ocupadas por piezas de ese color, recorriendo el tablero sin asumir su tamaño. */
    static Set<Position> squaresOf(BoardView board, Color color) {
        Set<Position> result = new LinkedHashSet<>();
        for (int row = 0; row < board.rows(); row++) {
            for (int column = 0; column < board.columns(); column++) {
                Position p = new Position(row, column);
                Piece piece = board.pieceAt(p);
                if (piece != null && piece.getColor() == color) {
                    result.add(p);
                }
            }
        }
        return result;
    }
}
