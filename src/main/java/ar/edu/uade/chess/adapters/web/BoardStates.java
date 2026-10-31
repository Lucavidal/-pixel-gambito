package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.adapters.gui.MoveLog;
import ar.edu.uade.chess.adapters.web.WebDtos.PieceDto;
import ar.edu.uade.chess.adapters.web.WebDtos.PlayedMoveDto;
import ar.edu.uade.chess.adapters.web.WebDtos.StateDto;
import ar.edu.uade.chess.core.game.GameStatus;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Arma la "foto" del tablero (StateDto) que dibujan las vistas 2D y 3D del front.
 *
 * La usan la partida libre (WebGameSession) y el torneo (WebTournamentSession):
 * los dos mandan la misma foto, así el front dibuja igual las dos cosas.
 * Solo lee lo que exponen los puertos (BoardView, el turno y el GameStatus); no calcula reglas.
 */
class BoardStates {

    private final AlgebraicNotation notation;
    private final PieceTypes pieceTypes;

    BoardStates(AlgebraicNotation notation, PieceTypes pieceTypes) {
        this.notation = notation;
        this.pieceTypes = pieceTypes;
    }

    /** @param turn el bando que tiene el turno: el núcleo informa el GameStatus de ese bando */
    StateDto state(BoardView board, Color turn, GameStatus status, MoveLog log, boolean canUndo, boolean canRedo) {
        List<PieceDto> pieces = new ArrayList<>();
        String checkedKing = null;
        for (int row = 0; row < board.rows(); row++) {
            for (int column = 0; column < board.columns(); column++) {
                Position position = new Position(row, column);
                Piece piece = board.pieceAt(position);
                if (piece == null) {
                    continue;
                }
                pieces.add(new PieceDto(notation.toText(position), pieceTypes.typeOf(piece),
                        colorName(piece.getColor()), String.valueOf(piece.getSymbol()), piece.getName()));
                // El rey en jaque es el del bando que tiene el turno.
                boolean inCheck = status == GameStatus.CHECK || status == GameStatus.CHECKMATE;
                if (inCheck && piece.isKing() && piece.getColor() == turn) {
                    checkedKing = notation.toText(position);
                }
            }
        }
        pieces.sort(Comparator.comparing(PieceDto::square));

        List<PlayedMoveDto> history = new ArrayList<>();
        for (MoveLog.Entry entry : log.entries()) {
            history.add(played(log, entry));
        }

        return new StateDto(
                colorName(turn),
                status.name(),
                status.isOver(),
                checkedKing,
                pieces,
                history,
                typesOf(log.capturedPiecesOf(Color.WHITE)),
                typesOf(log.capturedPiecesOf(Color.BLACK)),
                canUndo,
                canRedo);
    }

    /** Una jugada de la planilla, para el historial y para resaltar la última. */
    PlayedMoveDto played(MoveLog log, MoveLog.Entry entry) {
        return new PlayedMoveDto(
                notation.toText(entry.move().from()),
                notation.toText(entry.move().to()),
                log.text(entry),
                colorName(entry.moved().getColor()),
                pieceTypes.typeOf(entry.moved()),
                entry.captured() != null,
                entry.suffix());
    }

    /**
     * Las piezas del tablero en este momento. Sirve para saber qué pieza movió y cuál cayó
     * después de una jugada que no se conocía de antemano (la de la IA).
     */
    static Map<Position, Piece> snapshot(BoardView board) {
        Map<Position, Piece> pieces = new HashMap<>();
        for (int row = 0; row < board.rows(); row++) {
            for (int column = 0; column < board.columns(); column++) {
                Position position = new Position(row, column);
                if (board.pieceAt(position) != null) {
                    pieces.put(position, board.pieceAt(position));
                }
            }
        }
        return pieces;
    }

    /** Lo que se agrega a la jugada en la planilla: "+" jaque, "#" mate. */
    static String checkSuffix(GameStatus status) {
        return switch (status) {
            case CHECKMATE -> "#";
            case CHECK -> "+";
            default -> "";
        };
    }

    static String colorName(Color color) {
        return color.name().toLowerCase(Locale.ROOT);
    }

    private List<String> typesOf(List<Piece> pieces) {
        return pieces.stream().map(pieceTypes::typeOf).toList();
    }
}
