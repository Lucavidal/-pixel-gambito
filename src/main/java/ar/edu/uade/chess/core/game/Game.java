package ar.edu.uade.chess.core.game;

import ar.edu.uade.chess.core.commands.CommandHistory;
import ar.edu.uade.chess.core.commands.MoveCommand;
import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.rules.CheckDetector;
import ar.edu.uade.chess.core.rules.MoveValidator;

import java.util.Set;

/**
 * Orquesta una partida: turnos, validación, ejecución de movimientos,
 * deshacer/rehacer y estado (jaque, jaque mate, ahogado).
 *
 * Todas sus dependencias entran por el constructor (inyección de dependencias,
 * nivel 4). No crea nada con "new" adentro, salvo los comandos de movimiento,
 * que son datos de cada jugada.
 */
public class Game implements IGame {

    private final Board board;
    private final MoveValidator validator;
    private final CheckDetector checkDetector;
    private final CommandHistory history;

    private Color turn;
    private GameStatus status;

    public Game(Board board,
                MoveValidator validator,
                CheckDetector checkDetector,
                CommandHistory history) {
        this(board, validator, checkDetector, history, Color.WHITE);
    }

    /** Permite arrancar con otro turno (útil para tests con posiciones armadas). */
    public Game(Board board,
                MoveValidator validator,
                CheckDetector checkDetector,
                CommandHistory history,
                Color initialTurn) {
        this.board = board;
        this.validator = validator;
        this.checkDetector = checkDetector;
        this.history = history;
        this.turn = initialTurn;
        updateStatus();
    }

    @Override
    public MoveResult move(Position from, Position to) {
        if (status.isOver()) {
            return MoveResult.GAME_OVER;
        }
        if (!board.contains(from) || !board.contains(to)) {
            return MoveResult.OUT_OF_BOARD;
        }
        Piece piece = board.pieceAt(from);
        if (piece == null) {
            return MoveResult.EMPTY_SQUARE;
        }
        if (piece.getColor() != turn) {
            return MoveResult.OPPONENT_PIECE;
        }
        if (!validator.isValidMove(board, from, to)) {
            return MoveResult.INVALID_PIECE_MOVE;
        }
        if (validator.leavesKingInCheck(board, from, to, turn)) {
            return MoveResult.LEAVES_KING_IN_CHECK;
        }

        history.execute(new MoveCommand(board, from, to));
        switchTurn();
        return MoveResult.SUCCESS;
    }

    @Override
    public boolean undo() {
        if (!history.undo()) {
            return false;
        }
        switchTurn();
        return true;
    }

    @Override
    public boolean redo() {
        if (!history.redo()) {
            return false;
        }
        switchTurn();
        return true;
    }

    @Override
    public Set<Position> legalMoves(Position origin) {
        if (!board.contains(origin)) {
            return Set.of();
        }
        return validator.legalMoves(board, origin);
    }

    @Override public Color getTurn() { return turn; }
    @Override public GameStatus getStatus() { return status; }
    @Override public BoardView getBoard() { return board; }

    @Override
    public boolean isInCheck(Color color) {
        return checkDetector.isInCheck(board, color);
    }

    private void switchTurn() {
        turn = turn.opposite();
        updateStatus();
    }

    /** El estado se deriva del tablero; así deshacer/rehacer nunca lo dejan inconsistente. */
    private void updateStatus() {
        boolean inCheck = checkDetector.isInCheck(board, turn);
        boolean canMove = validator.hasAnyLegalMove(board, turn);

        if (inCheck && !canMove) {
            status = GameStatus.CHECKMATE;
        } else if (!canMove) {
            status = GameStatus.STALEMATE;
        } else if (inCheck) {
            status = GameStatus.CHECK;
        } else {
            status = GameStatus.IN_PROGRESS;
        }
    }
}
