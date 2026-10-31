package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.adapters.gui.MoveLog;
import ar.edu.uade.chess.adapters.web.WebDtos.ActionResponse;
import ar.edu.uade.chess.adapters.web.WebDtos.LegalMovesDto;
import ar.edu.uade.chess.adapters.web.WebDtos.MoveOptionDto;
import ar.edu.uade.chess.adapters.web.WebDtos.MoveResponse;
import ar.edu.uade.chess.adapters.web.WebDtos.StateDto;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.game.MoveResult;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * La partida que se juega desde el navegador, traducida a DTOs.
 *
 * Solo habla con el PUERTO IGame: no valida movimientos ni calcula jaques,
 * le pregunta todo al núcleo. Su trabajo es traducir ("e2" <-> Position,
 * Piece -> PieceDto) y recordar la planilla de jugadas para mostrarla.
 *
 * No sabe nada de HTTP (eso es WebServer), así que se testea sin levantar el servidor.
 * Los métodos son synchronized porque Javalin atiende cada pedido en un hilo distinto.
 */
public class WebGameSession {

    private final Supplier<IGame> games;
    private final AlgebraicNotation notation;
    private final BoardStates states;
    private final MoveLog log;

    private IGame game;

    /** @param games fábrica de partidas nuevas (se usa al arrancar y al reiniciar) */
    public WebGameSession(Supplier<IGame> games, AlgebraicNotation notation, PieceTypes pieceTypes) {
        this.games = games;
        this.notation = notation;
        this.states = new BoardStates(notation, pieceTypes);
        // La planilla es la misma que usa la ventana Swing: es presentación pura, no depende de Swing.
        this.log = new MoveLog(notation);
        this.game = games.get();
    }

    public synchronized StateDto state() {
        return states.state(game.getBoard(), game.getTurn(), game.getStatus(), log,
                !log.entries().isEmpty(), log.canRedo());
    }

    /** Destinos legales de la pieza en esa casilla, según el núcleo. */
    public synchronized LegalMovesDto legalMoves(String from) {
        Position origin = notation.toPosition(from);
        return legalMovesOf(notation, game.getBoard(), origin, game.legalMoves(origin));
    }

    /** Intenta la jugada; el núcleo decide si es válida y por qué no. */
    public synchronized MoveResponse move(String from, String to) {
        Position origin = notation.toPosition(from);
        Position destination = notation.toPosition(to);
        BoardView board = game.getBoard();

        // Se miran ANTES de mover, solo para la planilla (qué pieza movió y cuál cayó).
        Piece moved = board.contains(origin) ? board.pieceAt(origin) : null;
        Piece captured = board.contains(destination) ? board.pieceAt(destination) : null;

        MoveResult result = game.move(origin, destination);
        if (result.isSuccess()) {
            log.record(new Move(origin, destination), moved, captured, BoardStates.checkSuffix(game.getStatus()));
        }
        return new MoveResponse(result.name(), result.isSuccess(), result.getMessage(), state());
    }

    /** Usa el deshacer del núcleo (CommandHistory / MoveCommand). */
    public synchronized ActionResponse undo() {
        boolean done = game.undo();
        if (done) {
            log.undo();
        }
        return new ActionResponse(done, state());
    }

    public synchronized ActionResponse redo() {
        boolean done = game.redo();
        if (done) {
            log.redo();
        }
        return new ActionResponse(done, state());
    }

    public synchronized ActionResponse reset() {
        game = games.get();
        log.clear();
        return new ActionResponse(true, state());
    }

    /** Los destinos que devolvió el núcleo, en orden, marcando cuáles son capturas. Lo usa también el torneo. */
    static LegalMovesDto legalMovesOf(AlgebraicNotation notation, BoardView board, Position origin,
                                      Iterable<Position> destinations) {
        List<MoveOptionDto> moves = new ArrayList<>();
        for (Position destination : destinations) {
            boolean capture = board.contains(destination) && board.pieceAt(destination) != null;
            moves.add(new MoveOptionDto(notation.toText(destination), capture));
        }
        moves.sort(Comparator.comparing(MoveOptionDto::to));
        return new LegalMovesDto(notation.toText(origin), moves);
    }
}
