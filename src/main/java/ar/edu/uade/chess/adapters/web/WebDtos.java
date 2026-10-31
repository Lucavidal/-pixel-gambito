package ar.edu.uade.chess.adapters.web;

import java.util.List;

/**
 * Los datos que viajan por HTTP/JSON entre el servidor y el front 3D.
 *
 * Son records simples (DTOs): no tienen lógica ni exponen clases del núcleo.
 * Las casillas siempre viajan en notación algebraica ("e2"); traducirlas a
 * Position es trabajo de este adaptador, nunca del núcleo.
 */
public final class WebDtos {

    private WebDtos() { }

    /** Una pieza sobre el tablero. type = pawn/rook/knight/bishop/queen/king (o unknown para piezas nuevas). */
    public record PieceDto(String square, String type, String color, String symbol, String name) { }

    /**
     * Una jugada hecha, para el historial y para resaltar la última. text es la planilla en español
     * ("Cg1-f3"); pieceType (pawn/knight/... o unknown), capture y suffix ("+", "#" o "") sirven para
     * que el front la escriba en otro idioma ("Ng1-f3") sin depender de ese texto.
     */
    public record PlayedMoveDto(String from, String to, String text, String color,
                                String pieceType, boolean capture, String suffix) { }

    /** Foto completa de la partida: el front la usa para sincronizar la escena, nunca su estado local. */
    public record StateDto(
            String turn,
            String status,
            boolean gameOver,
            String checkedKing,            // casilla del rey en jaque, o null
            List<PieceDto> pieces,
            List<PlayedMoveDto> history,
            List<String> capturedWhite,    // tipos de piezas blancas capturadas, en orden
            List<String> capturedBlack,
            boolean canUndo,
            boolean canRedo) { }

    /** Un destino legal; capture = true si hay una pieza rival en esa casilla. */
    public record MoveOptionDto(String to, boolean capture) { }

    public record LegalMovesDto(String from, List<MoveOptionDto> moves) { }

    /** Cuerpo del POST /api/move. */
    public record MoveRequest(String from, String to) { }

    /** result es el nombre del MoveResult del núcleo (SUCCESS, OPPONENT_PIECE, LEAVES_KING_IN_CHECK, ...). */
    public record MoveResponse(String result, boolean success, String message, StateDto state) { }

    /** Respuesta de deshacer / rehacer / reiniciar. */
    public record ActionResponse(boolean success, StateDto state) { }

    public record ErrorDto(String message) { }

    /** Cuerpo del POST /api/auth/login y /api/auth/register. displayName solo al crear la cuenta (opcional). */
    public record CredentialsRequest(String username, String password, String displayName) { }

    /**
     * Resultado de iniciar sesión o crear una cuenta. Igual que MoveResponse, equivocarse
     * es parte normal del uso: viaja con success = false, no como error HTTP.
     * result es el nombre del AuthResult del núcleo; username y displayName son null si falló.
     * displayName es el nombre para saludar (User.visibleName()): nunca el mail completo.
     */
    public record AuthResponse(boolean success, String result, String message, String username, String displayName) { }

    /** El usuario que tiene la sesión abierta (GET /api/auth/me). displayName = el nombre para saludar. */
    public record UserDto(String username, String displayName) { }

    // ---------- Torneo ----------

    /**
     * Un rival de la escalera. standing = won / playing / lost / pending: cómo le fue
     * al usuario contra él (para dibujar la escalera como en la ventana Swing).
     * style, styleDescription y difficulty van en español; styleId (AGGRESSIVE / BALANCED / DEFENSIVE,
     * o null si es un estilo nuevo) y difficultyId (EASY / MEDIUM / HARD) son los códigos para traducir.
     */
    public record RivalDto(String name, String description, String style, String styleDescription,
                           String difficulty, int randomMovePercent, String standing,
                           String styleId, String difficultyId) { }

    /**
     * Una dificultad para empezar el torneo. id = EASY / MEDIUM / HARD (el Difficulty del núcleo);
     * description cuenta cómo es su escalera ("4 rondas: 2 de dificultad fácil y 2 de dificultad media.")
     * y parts lo mismo en datos, para que el front la escriba en su idioma.
     */
    public record LevelDto(String id, String name, int rounds, String description, List<LevelPartDto> parts) { }

    /** Cuántos rivales de cada dificultad tiene una escalera (difficulty = EASY / MEDIUM / HARD). */
    public record LevelPartDto(String difficulty, int count) { }

    /** Cuerpo del POST /api/tournament/start: {"level":"MEDIUM"}. Sin level se repite la última. */
    public record StartRequest(String level) { }

    /**
     * Foto del torneo. status es el TournamentStatus del núcleo (NOT_STARTED, IN_PROGRESS,
     * ELIMINATED, CHAMPION). board es la partida de la ronda actual, la MISMA foto que la
     * partida libre (así las vistas 2D y 3D la dibujan igual); es null si el torneo no empezó.
     * level es la dificultad desde la que arrancó la escalera (null si todavía no se eligió);
     * levels son las dificultades para elegir y allRivals todo el plantel.
     */
    public record TournamentStateDto(String status, String statusText, int round, int totalRounds,
                                     String userColor, boolean awaitingOpponent,
                                     List<RivalDto> rivals, StateDto board,
                                     String level, String levelName, List<LevelDto> levels,
                                     List<RivalDto> allRivals) { }

    /**
     * Cómo terminó una ronda (outcome = WON / LOST / DRAWN), con el mismo mensaje que muestra
     * la ventana Swing. Si se ganó o hubo tablas, el estado ya trae la partida de la ronda siguiente.
     */
    public record RoundResultDto(String outcome, int round, String rival, String message) { }

    /**
     * Respuesta a una jugada del torneo (del usuario o de la IA).
     * result/success/message son los del MoveResult del núcleo (en la jugada de la IA, result es null:
     * siempre es válida). move es la jugada hecha, o null si fue rechazada. roundResult es null si
     * la ronda sigue.
     */
    public record TournamentMoveResponse(String result, boolean success, String message, PlayedMoveDto move,
                                         RoundResultDto roundResult, TournamentStateDto state) { }

    // ---------- Historial ----------

    /**
     * Totales del historial. text es la frase de la ventana ("12 rondas jugadas: ...") y
     * record la línea del menú ("Tu récord en el torneo: 7 ganadas · 2 tablas · 3 perdidas").
     */
    public record HistorySummaryDto(int roundsPlayed, int wins, int losses, int draws, int titles,
                                    String text, String record) { }

    /**
     * Una ronda terminada. playedAt va en ISO (2026-10-23T12:00) y date ya formateada ("23 oct 2026, 12:00").
     * outcome = WON / LOST / DRAWN / CHAMPION (ganar la última ronda); outcomeText es lo que se muestra.
     * difficulty = EASY / MEDIUM / HARD, o null si la ronda no la tiene guardada; difficultyText ya en
     * español ("Fácil", o "—" si no hay).
     */
    public record RoundRecordDto(String playedAt, String date, int round, int totalRounds, String rival,
                                 String difficulty, String difficultyText,
                                 String outcome, String outcomeText, int userMoves) { }

    /** Una opción del filtro por dificultad del historial: id = EASY / MEDIUM / HARD. */
    public record DifficultyOptionDto(String id, String name) { }

    /**
     * "Mi historial": totales y últimas rondas, de la más nueva a la más vieja. difficulty es el filtro
     * aplicado (null = todas) y los totales ya lo respetan; difficulties son las opciones del filtro.
     */
    public record HistoryDto(String username, String displayName, String difficulty, HistorySummaryDto summary,
                             List<RoundRecordDto> rounds, List<DifficultyOptionDto> difficulties) { }
}
