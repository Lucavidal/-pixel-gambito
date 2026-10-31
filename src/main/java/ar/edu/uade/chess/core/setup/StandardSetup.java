package ar.edu.uade.chess.core.setup;

import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import java.util.List;
import java.util.function.Function;

/** Tablero clásico de 8x8 con la disposición inicial reglamentaria. */
public final class StandardSetup implements IBoardSetup {

    private static final List<Function<Color, Piece>> BACK_ROW = List.of(
            PieceFactory::rook, PieceFactory::knight, PieceFactory::bishop, PieceFactory::queen,
            PieceFactory::king, PieceFactory::bishop, PieceFactory::knight, PieceFactory::rook);

    @Override
    public Board createBoard() {
        Board board = new Board(8, 8);
        for (int column = 0; column < 8; column++) {
            board.placePiece(BACK_ROW.get(column).apply(Color.WHITE), new Position(0, column));
            board.placePiece(PieceFactory.pawn(Color.WHITE), new Position(1, column));
            board.placePiece(PieceFactory.pawn(Color.BLACK), new Position(6, column));
            board.placePiece(BACK_ROW.get(column).apply(Color.BLACK), new Position(7, column));
        }
        return board;
    }
}
