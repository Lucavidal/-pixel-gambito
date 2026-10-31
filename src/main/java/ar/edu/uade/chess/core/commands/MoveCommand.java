package ar.edu.uade.chess.core.commands;

import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

/**
 * Un movimiento de ajedrez como objeto.
 *
 * Al ejecutarse guarda TODO lo necesario para deshacerse con exactitud:
 *  - qué pieza capturó (si capturó alguna), para devolverla a su casilla;
 *  - si la pieza movida ya se había movido antes (importa para el doble paso del peón).
 *
 * No valida nada: la validación ocurre antes, en Game. Un comando solo
 * se crea para movimientos que ya se sabe que son legales.
 */
public final class MoveCommand implements ICommand {

    private final Board board;
    private final Position from;
    private final Position to;

    private Piece capturedPiece;     // se completa recién al ejecutar
    private boolean hadMovedBefore;  // idem

    public MoveCommand(Board board, Position from, Position to) {
        this.board = board;
        this.from = from;
        this.to = to;
    }

    @Override
    public void execute() {
        Piece piece = board.pieceAt(from);
        hadMovedBefore = piece.hasMoved();
        capturedPiece = board.movePiece(from, to);
        piece.markAsMoved();
    }

    @Override
    public void undo() {
        Piece piece = board.pieceAt(to);
        board.movePiece(to, from);
        piece.restoreHasMoved(hadMovedBefore);
        board.placePiece(capturedPiece, to); // si no capturó nada, deja la casilla vacía (null)
    }

    public Piece getCapturedPiece() { return capturedPiece; }
    public Position getFrom() { return from; }
    public Position getTo() { return to; }
}
