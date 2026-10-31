package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.adapters.gui.MoveLog;
import ar.edu.uade.chess.adapters.web.WebDtos.LegalMovesDto;
import ar.edu.uade.chess.adapters.web.WebDtos.LevelDto;
import ar.edu.uade.chess.adapters.web.WebDtos.LevelPartDto;
import ar.edu.uade.chess.adapters.web.WebDtos.PlayedMoveDto;
import ar.edu.uade.chess.adapters.web.WebDtos.RivalDto;
import ar.edu.uade.chess.adapters.web.WebDtos.RoundResultDto;
import ar.edu.uade.chess.adapters.web.WebDtos.StateDto;
import ar.edu.uade.chess.adapters.web.WebDtos.TournamentMoveResponse;
import ar.edu.uade.chess.adapters.web.WebDtos.TournamentStateDto;
import ar.edu.uade.chess.core.ai.AggressiveStyle;
import ar.edu.uade.chess.core.ai.BalancedStyle;
import ar.edu.uade.chess.core.ai.DefensiveStyle;
import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.ai.IPlayStyle;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.game.MoveResult;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.tournament.HistoricalPlayer;
import ar.edu.uade.chess.core.tournament.ITournament;
import ar.edu.uade.chess.core.tournament.RoundOutcome;
import ar.edu.uade.chess.core.tournament.TournamentStatus;
import ar.edu.uade.chess.core.tournament.TurnReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El torneo escalera que se juega desde el navegador, traducido a DTOs.
 *
 * Solo habla con el PUERTO ITournament (el mismo que usan la consola y la ventana Swing).
 * Cada turno son dos pedidos, como en la ventana: move() aplica SOLO la jugada del usuario
 * y opponentMove() la respuesta de la IA. Así el front muestra primero la jugada del usuario
 * y después, con una demora de "pensando...", la de la IA. La demora es cosa del front.
 *
 * El usuario elige desde qué dificultad empieza la escalera: cada start(nivel) pide un torneo
 * NUEVO a la fábrica que recibe por constructor (en Main: una escalera de HistoricalPlayerFactory
 * envuelta en RecordingTournament). Así el núcleo del torneo no cambia: solo cambian sus rivales.
 *
 * No sabe nada de HTTP (eso es WebServer), así que se testea sin levantar el servidor.
 * Los métodos son synchronized porque Javalin atiende cada pedido en un hilo distinto.
 */
public class WebTournamentSession {

    private final Function<Difficulty, ITournament> tournaments;
    private final List<HistoricalPlayer> roster;
    private final AlgebraicNotation notation;
    private final BoardStates states;
    /** Planilla de la ronda actual; se vacía cuando empieza una partida nueva. */
    private final MoveLog log;
    private final List<LevelDto> levels;

    /** El torneo en juego, o null si todavía no se eligió la dificultad. */
    private ITournament tournament;
    private Difficulty level;

    /**
     * @param tournaments arma un torneo (sin empezar) cuya escalera arranca en esa dificultad
     * @param roster      todos los rivales posibles (para "Qué incluye el juego")
     */
    public WebTournamentSession(Function<Difficulty, ITournament> tournaments, List<HistoricalPlayer> roster,
                                AlgebraicNotation notation, PieceTypes pieceTypes) {
        this.tournaments = tournaments;
        this.roster = List.copyOf(roster);
        this.notation = notation;
        this.states = new BoardStates(notation, pieceTypes);
        this.log = new MoveLog(notation);
        this.levels = describeLevels();
    }

    public synchronized TournamentStateDto state() {
        List<RivalDto> all = roster.stream().map(r -> rivalDto(r, "pending")).toList();
        if (tournament == null) {
            TournamentStatus status = TournamentStatus.NOT_STARTED;
            return new TournamentStateDto(status.name(), status.getDisplayName(), 0, 0, null, false,
                    List.of(), null, null, null, levels, all);
        }
        TournamentStatus status = tournament.getStatus();
        // En el torneo no se deshace: la IA ya respondió (ITournament no lo expone a propósito).
        StateDto board = status == TournamentStatus.NOT_STARTED ? null
                : states.state(tournament.getBoard(), turn(), tournament.getGameStatus(), log, false, false);
        return new TournamentStateDto(status.name(), status.getDisplayName(),
                tournament.getCurrentRound(), tournament.getTotalRounds(),
                BoardStates.colorName(tournament.getUserColor()), tournament.isAwaitingOpponent(),
                rivals(), board, level.name(), level.getDisplayName(), levels, all);
    }

    /**
     * Empieza un torneo nuevo en la ronda 1, con una escalera que arranca en esa dificultad.
     * Sin dificultad (null) se repite la última elegida; la primera vez, la más fácil.
     */
    public synchronized TournamentStateDto start(Difficulty chosen) {
        level = chosen != null ? chosen : level != null ? level : Difficulty.EASY;
        tournament = tournaments.apply(level);
        tournament.start();
        log.clear();
        return state();
    }

    /** Cómo es la escalera de cada dificultad: "4 rondas: 2 de dificultad fácil y 2 de dificultad media." */
    private List<LevelDto> describeLevels() {
        List<LevelDto> result = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            // Un torneo sin empezar solo para leer su escalera: armarlo no juega ni guarda nada.
            Map<Difficulty, Long> counts = tournaments.apply(difficulty).getRivals().stream()
                    .collect(Collectors.groupingBy(HistoricalPlayer::difficulty, TreeMap::new, Collectors.counting()));
            int rounds = counts.values().stream().mapToInt(Long::intValue).sum();
            List<String> parts = new ArrayList<>();
            List<LevelPartDto> partDtos = new ArrayList<>();
            counts.forEach((d, n) -> {
                parts.add(n + " de dificultad " + d.getDisplayName().toLowerCase(Locale.ROOT));
                partDtos.add(new LevelPartDto(d.name(), n.intValue()));
            });
            result.add(new LevelDto(difficulty.name(), difficulty.getDisplayName(), rounds,
                    rounds + " rondas: " + String.join(" y ", parts) + ".", partDtos));
        }
        return result;
    }

    /** Destinos legales de una pieza del usuario. Las de la IA, o en el turno de la IA, no tienen destinos. */
    public synchronized LegalMovesDto legalMoves(String from) {
        requireStarted();
        Position origin = notation.toPosition(from);
        BoardView board = tournament.getBoard();
        Piece piece = board.contains(origin) ? board.pieceAt(origin) : null;
        boolean usersTurn = tournament.getStatus() == TournamentStatus.IN_PROGRESS && !tournament.isAwaitingOpponent();
        Set<Position> destinations = usersTurn && piece != null && piece.getColor() == tournament.getUserColor()
                ? tournament.legalMoves(origin) : Set.of();
        return WebGameSession.legalMovesOf(notation, board, origin, destinations);
    }

    /** La jugada del usuario. Si es válida y la ronda sigue, el torneo queda esperando a opponentMove(). */
    public synchronized TournamentMoveResponse move(String from, String to) {
        requireStarted();
        Position origin = notation.toPosition(from);
        Position destination = notation.toPosition(to);
        // Se leen ANTES de jugar: si se gana la ronda, el torneo ya pasa al rival siguiente.
        HistoricalPlayer rival = tournament.getCurrentRival();
        int round = tournament.getCurrentRound();
        Map<Position, Piece> before = BoardStates.snapshot(tournament.getBoard());

        TurnReport report = tournament.playUserMove(origin, destination);
        MoveResult result = report.userResult();
        if (!result.isSuccess()) {
            return new TournamentMoveResponse(result.name(), false, result.getMessage(), null, null, state());
        }
        PlayedMoveDto played = record(new Move(origin, destination), before, report.outcome());
        return new TournamentMoveResponse(result.name(), true, result.getMessage(), played,
                roundResult(report.outcome(), round, rival), state());
    }

    /** La respuesta de la IA. Solo se puede pedir después de una jugada válida del usuario. */
    public synchronized TournamentMoveResponse opponentMove() {
        requireStarted();
        if (!tournament.isAwaitingOpponent()) {
            throw new IllegalStateException("No es el turno de la IA: primero jugá vos.");
        }
        HistoricalPlayer rival = tournament.getCurrentRival();
        int round = tournament.getCurrentRound();
        // La jugada de la IA no se conoce de antemano: se guarda el tablero para saber qué pieza cayó.
        Map<Position, Piece> before = BoardStates.snapshot(tournament.getBoard());

        TurnReport report = tournament.playOpponentMove();
        PlayedMoveDto played = record(report.aiReply(), before, report.outcome());
        return new TournamentMoveResponse(null, true, null, played, roundResult(report.outcome(), round, rival), state());
    }

    /** Anota la jugada en la planilla y la devuelve; si con ella empezó otra partida, la planilla arranca de cero. */
    private PlayedMoveDto record(Move move, Map<Position, Piece> before, RoundOutcome outcome) {
        log.record(move, before.get(move.from()), before.get(move.to()), suffix(outcome));
        PlayedMoveDto played = states.played(log, log.entries().get(log.entries().size() - 1));
        boolean newGameStarted = outcome == RoundOutcome.DRAWN
                || (outcome == RoundOutcome.WON && tournament.getStatus() == TournamentStatus.IN_PROGRESS);
        if (newGameStarted) {
            log.clear();
        }
        return played;
    }

    /**
     * Si la ronda terminó, el tablero del torneo puede ser ya el de la ronda siguiente:
     * el sufijo sale del resultado de la ronda, no del estado de la partida (igual que en Swing).
     */
    private String suffix(RoundOutcome outcome) {
        return switch (outcome) {
            case WON, LOST -> "#";
            case DRAWN -> "";
            case ONGOING -> BoardStates.checkSuffix(tournament.getGameStatus());
        };
    }

    /** Mismos mensajes que la ventana Swing (TournamentWindow). null si la ronda sigue. */
    private RoundResultDto roundResult(RoundOutcome outcome, int round, HistoricalPlayer rival) {
        String message = switch (outcome) {
            case WON -> tournament.getStatus() == TournamentStatus.CHAMPION
                    ? "¡Jaque mate a " + rival.name() + "! Ganaste las " + tournament.getTotalRounds()
                      + " rondas: sos campeón del torneo."
                    : "¡Jaque mate! Le ganaste a " + rival.name() + ".\nPasás a la ronda "
                      + tournament.getCurrentRound() + " contra " + tournament.getCurrentRival().name() + ".";
            case LOST -> "¡Jaque mate! " + rival.name() + " te ganó.\nQuedaste eliminado en la ronda " + round + ".";
            case DRAWN -> "Rey ahogado: tablas.\nSe repite la ronda " + round + " contra " + rival.name() + ".";
            case ONGOING -> null;
        };
        return message == null ? null : new RoundResultDto(outcome.name(), round, rival.name(), message);
    }

    /** La escalera completa, con cómo le fue al usuario contra cada rival. */
    private List<RivalDto> rivals() {
        TournamentStatus status = tournament.getStatus();
        int current = tournament.getCurrentRound() - 1;
        List<HistoricalPlayer> rivals = tournament.getRivals();
        List<RivalDto> result = new ArrayList<>();
        for (int i = 0; i < rivals.size(); i++) {
            HistoricalPlayer rival = rivals.get(i);
            String standing;
            if (i < current) {
                standing = "won";
            } else if (i > current) {
                standing = "pending";
            } else {
                standing = switch (status) {
                    case CHAMPION -> "won";
                    case ELIMINATED -> "lost";
                    case IN_PROGRESS -> "playing";
                    case NOT_STARTED -> "pending";
                };
            }
            result.add(rivalDto(rival, standing));
        }
        return result;
    }

    private static RivalDto rivalDto(HistoricalPlayer rival, String standing) {
        return new RivalDto(rival.name(), rival.description(), rival.style().getDisplayName(),
                rival.style().getDescription(), rival.difficulty().getDisplayName(),
                (int) Math.round(rival.difficulty().randomMoveProbability() * 100), standing,
                styleId(rival.style()), rival.difficulty().name());
    }

    /** Código del estilo para que el front lo traduzca; un estilo nuevo viaja como null (se usa su texto). */
    static String styleId(IPlayStyle style) {
        if (style instanceof AggressiveStyle) {
            return "AGGRESSIVE";
        }
        if (style instanceof BalancedStyle) {
            return "BALANCED";
        }
        if (style instanceof DefensiveStyle) {
            return "DEFENSIVE";
        }
        return null;
    }

    /**
     * ITournament no expone de quién es el turno: se deduce. En curso, es de la IA mientras se espera su
     * respuesta. Terminado, es del que quedó en jaque mate: el usuario si fue eliminado, la IA si salió campeón.
     */
    private Color turn() {
        Color user = tournament.getUserColor();
        return switch (tournament.getStatus()) {
            case ELIMINATED -> user;
            case CHAMPION -> user.opposite();
            default -> tournament.isAwaitingOpponent() ? user.opposite() : user;
        };
    }

    private void requireStarted() {
        if (tournament == null || tournament.getStatus() == TournamentStatus.NOT_STARTED) {
            throw new IllegalStateException("El torneo todavía no empezó: elegí la dificultad.");
        }
    }
}
