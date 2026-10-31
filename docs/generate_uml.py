#!/usr/bin/env python3
"""Genera el diagrama de clases UML (Graphviz DOT) del TPO Ajedrez.
Uso:  python3 generate_uml.py && dot -Tpng class-diagram.dot -o class-diagram.png
"""

def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

def cls(name, stereotype=None, attrs=(), methods=(), color="#ffffff"):
    rows = []
    title = ""
    if stereotype:
        title += f'<FONT POINT-SIZE="9">«{stereotype}»</FONT><BR/>'
    title += f"<I><B>{name}</B></I>" if stereotype == "interface" else f"<B>{name}</B>"
    rows.append(f'<TR><TD BGCOLOR="{color}">{title}</TD></TR>')
    if attrs is not None:
        txt = '<BR ALIGN="LEFT"/>'.join(esc(a) for a in attrs)
        rows.append(f'<TR><TD ALIGN="LEFT" BALIGN="LEFT">{txt}<BR ALIGN="LEFT"/></TD></TR>' if attrs else '<TR><TD> </TD></TR>')
    if methods:
        txt = '<BR ALIGN="LEFT"/>'.join(esc(m) for m in methods)
        rows.append(f'<TR><TD ALIGN="LEFT" BALIGN="LEFT">{txt}<BR ALIGN="LEFT"/></TD></TR>')
    return f'{name} [label=<<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">{"".join(rows)}</TABLE>>];'

CORE = "#e8f0fe"   # núcleo
INT = "#fff4d6"    # interfaces / puertos
ADA = "#e7f6ec"    # adaptadores

model = [
    cls("Color", "enumeration", ["WHITE, BLACK"], ["+forwardDirection(): int", "+opposite(): Color"], CORE),
    cls("Position", "record", ["+row: int", "+column: int"],
        ["+offset(int, int): Position", "+offset(Direction): Position"], CORE),
    cls("Direction", "record", ["+rowDelta: int", "+columnDelta: int",
                                "+ORTHOGONAL, DIAGONAL, KNIGHT_JUMPS: List<Direction>"], ["+all(): List<Direction>"], CORE),
    cls("Piece", None, ["-name: String", "-symbol: char", "-color: Color", "-isKing: boolean",
                        "-movementRules: List<IMovementRule>", "-hasMoved: boolean"],
        ["+possibleMoves(BoardView, Position): Set<Position>", "+isEnemyOf(Piece): boolean",
         "+getName(): String", "+getSymbol(): char", "+getColor(): Color",
         "+isKing(): boolean", "+hasMoved(): boolean",
         "+markAsMoved()", "+restoreHasMoved(boolean)"], CORE),
    cls("BoardView", "interface", None, ["+rows(): int", "+columns(): int", "+contains(Position): boolean",
                                         "+pieceAt(Position): Piece", "+isEmpty(Position): boolean"], INT),
    cls("Board", None, ["-rows: int", "-columns: int", "-squares: Piece[][]"],
        ["+placePiece(Piece, Position)", "+removePiece(Position): Piece", "+movePiece(Position, Position): Piece",
         "+positionsOf(Color): List<Position>", "+kingPosition(Color): Position"], CORE),
]
movement = [
    cls("IMovementRule", "interface", None, ["+destinations(BoardView, Position, Piece): Set<Position>"], INT),
    cls("SlidingMovement", None, ["-directions: List<Direction>"], ["+destinations(...)"], CORE),
    cls("JumpMovement", None, ["-jumps: List<Direction>"], ["+destinations(...)"], CORE),
    cls("PawnMovement", None, [], ["+destinations(...)"], CORE),
]
setup = [
    cls("PieceFactory", "factory", [], ["+pawn(Color), rook(Color), knight(Color)", "+bishop(Color), queen(Color), king(Color)"], CORE),
    cls("IBoardSetup", "interface", None, ["+createBoard(): Board"], INT),
    cls("StandardSetup", None, ["-BACK_ROW: List<Function<Color, Piece>>"], ["+createBoard(): Board"], CORE),
]
rules = [
    cls("CheckDetector", None, [], ["+isInCheck(Board, Color): boolean", "+isAttacked(Board, Position, Color): boolean"], CORE),
    cls("MoveValidator", None, ["-checkDetector: CheckDetector"],
        ["+legalMoves(Board, Position): Set<Position>", "+isValidMove(Board, Position, Position): boolean",
         "+leavesKingInCheck(Board, Position, Position, Color): boolean", "+hasAnyLegalMove(Board, Color): boolean"], CORE),
]
commands = [
    cls("ICommand", "interface", None, ["+execute()", "+undo()"], INT),
    cls("MoveCommand", None, ["-board: Board", "-from: Position", "-to: Position",
                              "-capturedPiece: Piece", "-hadMovedBefore: boolean"],
        ["+execute()", "+undo()", "+getCapturedPiece(): Piece", "+getFrom(): Position", "+getTo(): Position"], CORE),
    cls("CommandHistory", None, ["-done: Deque<ICommand>", "-undone: Deque<ICommand>"],
        ["+execute(ICommand)", "+undo(): boolean", "+redo(): boolean",
         "+canUndo(): boolean", "+canRedo(): boolean", "+moveCount(): int"], CORE),
]
game = [
    cls("IGame", "interface", None,
        ["+move(Position, Position): MoveResult", "+undo(): boolean", "+redo(): boolean",
         "+getTurn(): Color", "+getStatus(): GameStatus", "+isInCheck(Color): boolean",
         "+legalMoves(Position): Set<Position>", "+getBoard(): BoardView"], INT),
    cls("Game", None, ["-board: Board", "-validator: MoveValidator", "-checkDetector: CheckDetector",
                       "-history: CommandHistory", "-turn: Color", "-status: GameStatus"],
        ["+move(...)", "+undo()", "+redo()", "+legalMoves(...)", "+isInCheck(...)",
         "-switchTurn()", "-updateStatus()"], CORE),
    cls("GameStatus", "enumeration", ["IN_PROGRESS, CHECK,", "CHECKMATE, STALEMATE"], ["+isOver(): boolean"], CORE),
    cls("MoveResult", "enumeration", ["SUCCESS, OUT_OF_BOARD,", "EMPTY_SQUARE, OPPONENT_PIECE,",
                                      "INVALID_PIECE_MOVE,", "LEAVES_KING_IN_CHECK, GAME_OVER"],
        ["+getMessage(): String", "+isSuccess(): boolean"], CORE),
]
ai = [
    cls("Move", "record", ["+from: Position", "+to: Position"], [], CORE),
    cls("IMoveChooser", "interface", None, ["+chooseMove(IGame): Move"], INT),
    cls("ComputerPlayer", None, ["-style: IPlayStyle", "-difficulty: Difficulty", "-analyzer: MoveAnalyzer",
                                 "-random: Random"],
        ["+chooseMove(IGame): Move", "-bestMove(IGame, List<Move>): Move", "-legalMoves(IGame): List<Move>",
         "+getStyle(): IPlayStyle", "+getDifficulty(): Difficulty"], CORE),
    cls("IPlayStyle", "interface", ["+CHECKMATE_BONUS: int"], ["+score(MoveFacts): int", "+getDisplayName(): String",
                                                               "+getDescription(): String"], INT),
    cls("AggressiveStyle", None, [], ["+score(MoveFacts): int"], CORE),
    cls("DefensiveStyle", None, [], ["+score(MoveFacts): int"], CORE),
    cls("BalancedStyle", None, ["-aggressive: IPlayStyle", "-defensive: IPlayStyle"], ["+score(MoveFacts): int"], CORE),
    cls("Difficulty", "enumeration", ["EASY, MEDIUM, HARD"],
        ["+getDisplayName(): String", "+randomMoveProbability(): double"], CORE),
    cls("MoveAnalyzer", None, ["-pieceValue: PieceValue"],
        ["+analyze(IGame, Move): MoveFacts", "~squaresOf(BoardView, Color): Set<Position>"], CORE),
    cls("MoveFacts", "record", ["+capturedValue: int", "+givesCheck: boolean", "+givesCheckmate: boolean",
                                "+hangingValue: int", "+kingShelter: int"], [], CORE),
    cls("PieceValue", None, [], ["+of(Piece, int, int): int"], CORE),
]
tournament = [
    cls("ITournament", "interface", None,
        ["+start()", "+getCurrentRound(): int", "+getTotalRounds(): int",
         "+getCurrentRival(): HistoricalPlayer", "+getRivals(): List<HistoricalPlayer>",
         "+play(Position, Position): TurnReport",
         "+playUserMove(Position, Position): TurnReport", "+playOpponentMove(): TurnReport",
         "+isAwaitingOpponent(): boolean", "+getBoard(): BoardView", "+getGameStatus(): GameStatus", "+getUserColor(): Color",
         "+legalMoves(Position): Set<Position>", "+getStatus(): TournamentStatus"], INT),
    cls("Tournament", None, ["-rivals: List<HistoricalPlayer>", "-gameFactory: IGameFactory",
                             "-opponentFactory: IOpponentFactory", "-userColor: Color", "-roundIndex: int",
                             "-game: IGame", "-opponent: IMoveChooser", "-status: TournamentStatus"],
        ["+start()", "+play(...)", "+playUserMove(...)", "+playOpponentMove()", "+isAwaitingOpponent()",
         "-startRound()", "-playOpponent(): Move",
         "-currentOutcome(): RoundOutcome", "-resolve(RoundOutcome)"], CORE),
    cls("TournamentStatus", "enumeration", ["NOT_STARTED, IN_PROGRESS,", "ELIMINATED, CHAMPION"],
        ["+getDisplayName(): String", "+isOver(): boolean"], CORE),
    cls("RoundOutcome", "enumeration", ["ONGOING, WON, LOST, DRAWN"], ["+isFinished(): boolean"], CORE),
    cls("TurnReport", "record", ["+userResult: MoveResult", "+aiReply: Move", "+outcome: RoundOutcome"], [], CORE),
    cls("HistoricalPlayer", "record", ["+name: String", "+description: String", "+style: IPlayStyle",
                                       "+difficulty: Difficulty"], [], CORE),
    cls("HistoricalPlayerFactory", "factory", ["+DEFAULT_ROUNDS: int", "+LADDER_ROUNDS: int", "+RIVALS_PER_LEVEL: int"],
        ["+createRoster(): List<HistoricalPlayer>", "+allPlayers(): List<HistoricalPlayer>",
         "+createLadder(Difficulty, Random): List<HistoricalPlayer>"], CORE),
    cls("IGameFactory", "interface", None, ["+create(): IGame"], INT),
    cls("BoardSetupGameFactory", None, ["-setup: IBoardSetup", "-validator: MoveValidator",
                                        "-checkDetector: CheckDetector"], ["+create(): IGame"], CORE),
    cls("IOpponentFactory", "interface", None, ["+create(HistoricalPlayer): IMoveChooser"], INT),
]
auth = [
    cls("IAuthService", "interface", None, ["+register(String, String): AuthResult",
                                            "+login(String, String): Optional<User>",
                                            "+isRegistered(String): boolean"], INT),
    cls("AuthService", None, ["+MIN_PASSWORD_LENGTH, MIN_/MAX_USERNAME_LENGTH: int", "-users: IUserRepository",
                              "-hasher: IPasswordHasher"],
        ["+register(...): AuthResult", "+login(...): Optional<User>", "-normalize(String): String"], CORE),
    cls("AuthResult", "enumeration", ["SUCCESS, INVALID_USERNAME,", "PASSWORD_TOO_SHORT,",
                                      "USERNAME_TAKEN, UNKNOWN_USER,", "WRONG_PASSWORD"],
        ["+getMessage(): String", "+isSuccess(): boolean"], CORE),
    cls("User", "record", ["+username: String"], [], CORE),
    cls("UserAccount", "record", ["+username: String", "+passwordHash: String"], [], CORE),
    cls("IUserRepository", "interface", ["«puerto de salida»"], ["+findByUsername(String): Optional<UserAccount>",
                                                                "+save(UserAccount)"], INT),
    cls("IPasswordHasher", "interface", None, ["+hash(String): String", "+matches(String, String): boolean"], INT),
    cls("Pbkdf2PasswordHasher", None, ["+DEFAULT_ITERATIONS: int", "-iterations: int", "-random: SecureRandom"],
        ["+hash(String): String", "+matches(String, String): boolean"], CORE),
]
history = [
    cls("RecordingTournament", "decorator", ["-tournament: ITournament", "-history: IMatchHistoryRepository",
                                             "-user: User", "-clock: Clock", "-userMovesThisRound: int"],
        ["+play(...), playUserMove(...),", "+playOpponentMove(): TurnReport", "-recording(Supplier<TurnReport>)",
         "+(el resto delega en tournament)"], CORE),
    cls("IMatchHistoryRepository", "interface", ["«puerto de salida»"],
        ["+save(RoundRecord)", "+findByUsername(String): List<RoundRecord>"], INT),
    cls("RoundRecord", "record", ["+username: String", "+rivalName: String", "+round: int", "+totalRounds: int",
                                  "+outcome: RoundOutcome", "+userMoves: int", "+playedAt: LocalDateTime"],
        ["+wonTheTournament(): boolean"], CORE),
    cls("IHistoryService", "interface", None, ["+summary(User): HistorySummary",
                                               "+recentRounds(User, int): List<RoundRecord>"], INT),
    cls("HistoryService", None, ["-repository: IMatchHistoryRepository"],
        ["+summary(User): HistorySummary", "+recentRounds(User, int): List<RoundRecord>"], CORE),
    cls("HistorySummary", "record", ["+roundsPlayed, wins, losses,", "+draws, titles: int"], [], CORE),
]
persistence = [
    cls("SqliteDatabase", None, ["-url: String"], ["+connect(): Connection", "+createSchema()"], ADA),
    cls("JdbcUserRepository", None, ["-database: SqliteDatabase"],
        ["+findByUsername(String): Optional<UserAccount>", "+save(UserAccount)"], ADA),
    cls("JdbcMatchHistoryRepository", None, ["-database: SqliteDatabase"],
        ["+save(RoundRecord)", "+findByUsername(String): List<RoundRecord>"], ADA),
    cls("DatabaseException", "exception", [], [], ADA),
]
adapters = [
    cls("ConsoleLogin", None, ["+MAX_FAILED_LOGINS: int", "-auth: IAuthService"],
        ["+run(): Optional<User>", "-login(), -register()"], ADA),
    cls("HistoryPrinter", None, ["+RECENT_ROUNDS: int", "-history: IHistoryService", "-user: User"],
        ["+print(PrintStream)"], ADA),
    cls("ConsoleAdapter", None, ["-game: IGame", "-notation: AlgebraicNotation", "-renderer: TextRenderer"], ["+play()"], ADA),
    cls("TournamentConsoleAdapter", None, ["-tournament: ITournament", "-notation: AlgebraicNotation",
                                           "-renderer: TextRenderer", "-opponentDelayMillis: int",
                                           "-historyPrinter: HistoryPrinter"],
        ["+play()", "-pause()", "-showHistory()"], ADA),
    cls("AlgebraicNotation", None, [], ["+toPosition(String): Position", "+toText(Position): String"], ADA),
    cls("TextRenderer", None, [], ["+render(BoardView): String"], ADA),
]
gui = [
    cls("GuiApplication", None, ["-auth: IAuthService", "-history: IHistoryService", "-freeGames: Supplier<IGame>",
                                 "-tournaments: Function<User, ITournament>", "-roster: List<HistoricalPlayer>",
                                 "-aiDelayMillis: int"],
        ["+start(boolean)", "-loginAndShowMenu(boolean)", "-openFreeGame(...)", "-openTournament(...)",
         "-showErrorsOnScreen()"], ADA),
    cls("MainMenuWindow", None, ["-user: User", "-history: IHistoryService", "-roster: List<HistoricalPlayer>",
                                 "-actions: Actions"],
        ["+show()", "+showAgain()", "+hide()", "+close()", "-refreshStats()", "-card(...)"], ADA),
    cls("FeaturesDialog", None, ["-roster: List<HistoricalPlayer>"], ["+show(Component)"], ADA),
    cls("GameWindow", None, ["-games: Supplier<IGame>", "-game: IGame", "-log: MoveLog", "-boardPanel: BoardPanel",
                             "-selector: ClickToMove", "-moveList: MoveListPanel", "-captured: CapturedPanel"],
        ["+show()", "-onSquareClicked(Position)", "-play(Move)", "-undo()", "-redo()", "-newGame()",
         "-refresh(String)", "~kingOf(BoardView, Color): Position"], ADA),
    cls("TournamentWindow", None, ["-tournament: ITournament", "-log: MoveLog", "-boardPanel: BoardPanel",
                                   "-selector: ClickToMove", "-opponentDelayMillis: int", "-opponentTimer: Timer",
                                   "-historyDialog: HistoryDialog", "-moveList: MoveListPanel", "-captured: CapturedPanel"],
        ["+show()", "-onSquareClicked(Position)", "-play(Move)", "-replyFromOpponent(...)",
         "-finishRound(RoundOutcome, HistoricalPlayer)", "-refreshLadder()", "-refresh(String)"], ADA),
    cls("MoveLog", None, ["-notation: AlgebraicNotation", "-done: List<Entry>", "-undone: Deque<Entry>"],
        ["+record(Move, Piece, Piece, String)", "+undo()", "+redo()", "+clear()", "+lastMove(): Move",
         "+capturedPiecesOf(Color): List<Piece>", "+text(Entry): String", "+numberedLines(): List<String>"], ADA),
    cls("MoveListPanel", "JPanel", ["-text: JTextArea"], ["+show(List<String>)"], ADA),
    cls("CapturedPanel", "JPanel", ["-glyphs: PieceGlyphs"], ["+show(MoveLog)"], ADA),
    cls("Theme", None, ["+colores y fuentes (constantes)"], ["+card(String, JComponent): JPanel", "+wrap(String, int): String",
                                                             "+label(...)", "+button(...)"], ADA),
    cls("BoardPanel", "JPanel", ["-glyphs: PieceGlyphs", "-board: BoardView", "-selected: Position",
                                 "-highlighted: Set<Position>", "-lastMove: Move", "-kingInCheck: Position"],
        ["+showBoard(BoardView, Position, Set<Position>, Move, Position)", "+onSquareClicked(Consumer<Position>)",
         "+positionAt(int, int): Position"], ADA),
    cls("ClickToMove", None, ["-selected: Position", "-targets: Set<Position>"],
        ["+click(Position, Set<Position>): Optional<Move>", "+clear()",
         "+getSelected(): Position", "+getTargets(): Set<Position>"], ADA),
    cls("PieceGlyphs", None, [], ["+of(Piece): String"], ADA),
    cls("LoginDialog", None, ["-auth: IAuthService", "-tabs: JTabbedPane", "-loggedIn: User"],
        ["+ask(): Optional<User>", "-login()", "-register()"], ADA),
    cls("HistoryDialog", None, ["+RECENT_ROUNDS: int", "-history: IHistoryService", "-user: User"],
        ["+show(Component)"], ADA),
]
root = [cls("Main", "inyección de dependencias", ["-TOURNAMENT_USER_COLOR: Color", "-AI_THINKING_DELAY_MILLIS: int",
                                                 "-DATABASE_FILE: String"],
             ["+main(String[])", "-recordedTournament(...): ITournament", "-chooseMode(...): String"], "#f3e8fd")]

def cluster(name, label, nodes, color):
    return (f'subgraph cluster_{name} {{ label=<<B>{label}</B>>; style="rounded,dashed"; color="{color}"; fontsize=12;\n'
            + "\n".join(nodes) + "\n}")

edges = [
    # realizaciones (implements)
    ("Board", "BoardView", "real"),
    ("SlidingMovement", "IMovementRule", "real"),
    ("JumpMovement", "IMovementRule", "real"),
    ("PawnMovement", "IMovementRule", "real"),
    ("StandardSetup", "IBoardSetup", "real"),
    ("MoveCommand", "ICommand", "real"),
    ("Game", "IGame", "real"),
    # composición / agregación
    ("Piece", "IMovementRule", "comp", "1..*"),
    ("Board", "Piece", "agg", "0..*"),
    ("CommandHistory", "ICommand", "agg", "0..*"),
    # asociaciones (dependencias inyectadas)
    ("Game", "Board", "assoc"),
    ("Game", "MoveValidator", "assoc"),
    ("Game", "CheckDetector", "assoc"),
    ("Game", "CommandHistory", "assoc"),
    ("MoveValidator", "CheckDetector", "assoc"),
    ("MoveCommand", "Board", "assoc"),
    ("Piece", "Color", "assoc"),
    ("ConsoleAdapter", "IGame", "assoc"),
    ("ConsoleAdapter", "AlgebraicNotation", "assoc"),
    ("ConsoleAdapter", "TextRenderer", "assoc"),
    # dependencias (usa)
    ("Game", "MoveCommand", "dep", "crea"),
    ("Game", "GameStatus", "dep"),
    ("Game", "MoveResult", "dep"),
    ("IGame", "BoardView", "dep"),
    ("PieceFactory", "Piece", "dep", "crea"),
    ("PieceFactory", "SlidingMovement", "dep"),
    ("PieceFactory", "JumpMovement", "dep"),
    ("PieceFactory", "PawnMovement", "dep"),
    ("StandardSetup", "PieceFactory", "dep"),
    ("StandardSetup", "Board", "dep", "crea"),
    ("SlidingMovement", "Direction", "dep"),
    ("JumpMovement", "Direction", "dep"),
    ("IMovementRule", "BoardView", "dep"),
    ("CheckDetector", "Board", "dep"),
    ("Board", "Position", "dep"),
    ("TextRenderer", "BoardView", "dep"),
    ("AlgebraicNotation", "Position", "dep"),
    ("Main", "ConsoleAdapter", "dep", "crea y conecta"),
    ("Main", "Game", "dep"),
    ("Main", "StandardSetup", "dep"),
    # --- IA (core.ai) ---
    ("ComputerPlayer", "IMoveChooser", "real"),
    ("AggressiveStyle", "IPlayStyle", "real"),
    ("DefensiveStyle", "IPlayStyle", "real"),
    ("BalancedStyle", "IPlayStyle", "real"),
    ("ComputerPlayer", "IPlayStyle", "assoc"),
    ("ComputerPlayer", "Difficulty", "assoc"),
    ("ComputerPlayer", "MoveAnalyzer", "assoc"),
    ("BalancedStyle", "AggressiveStyle", "comp", "1"),
    ("BalancedStyle", "DefensiveStyle", "comp", "1"),
    ("MoveAnalyzer", "PieceValue", "assoc"),
    ("MoveAnalyzer", "MoveFacts", "dep", "crea"),
    ("MoveAnalyzer", "IGame", "dep", "simula y deshace"),
    ("IMoveChooser", "IGame", "dep"),
    ("IMoveChooser", "Move", "dep"),
    ("IPlayStyle", "MoveFacts", "dep"),
    ("PieceValue", "Piece", "dep"),
    ("Move", "Position", "dep"),
    # --- Torneo (core.tournament) ---
    ("Tournament", "ITournament", "real"),
    ("BoardSetupGameFactory", "IGameFactory", "real"),
    ("Tournament", "HistoricalPlayer", "agg", "1..*"),
    ("Tournament", "IGameFactory", "assoc"),
    ("Tournament", "IOpponentFactory", "assoc"),
    ("Tournament", "IMoveChooser", "assoc"),
    ("Tournament", "IGame", "assoc"),
    ("Tournament", "TournamentStatus", "dep"),
    ("Tournament", "TurnReport", "dep", "crea"),
    ("TurnReport", "RoundOutcome", "dep"),
    ("TurnReport", "Move", "dep"),
    ("TurnReport", "MoveResult", "dep"),
    ("HistoricalPlayer", "IPlayStyle", "assoc"),
    ("HistoricalPlayer", "Difficulty", "assoc"),
    ("HistoricalPlayerFactory", "HistoricalPlayer", "dep", "crea"),
    ("BoardSetupGameFactory", "IBoardSetup", "assoc"),
    ("BoardSetupGameFactory", "Game", "dep", "crea"),
    ("IOpponentFactory", "IMoveChooser", "dep", "crea"),
    ("ITournament", "BoardView", "dep"),
    # --- adaptador y raíz ---
    ("TournamentConsoleAdapter", "ITournament", "assoc"),
    ("TournamentConsoleAdapter", "AlgebraicNotation", "assoc"),
    ("TournamentConsoleAdapter", "TextRenderer", "assoc"),
    ("Main", "TournamentConsoleAdapter", "dep", "crea y conecta"),
    ("Main", "Tournament", "dep"),
    ("Main", "ComputerPlayer", "dep"),
    ("Main", "HistoricalPlayerFactory", "dep"),
    ("Main", "BoardSetupGameFactory", "dep"),
    # --- adaptador gráfico (adapters.gui) ---
    ("GameWindow", "IGame", "assoc"),
    ("GameWindow", "BoardPanel", "assoc"),
    ("GameWindow", "ClickToMove", "comp", "1"),
    ("TournamentWindow", "ITournament", "assoc"),
    ("TournamentWindow", "BoardPanel", "assoc"),
    ("TournamentWindow", "ClickToMove", "comp", "1"),
    ("TournamentWindow", "AlgebraicNotation", "assoc"),
    ("BoardPanel", "PieceGlyphs", "assoc"),
    ("BoardPanel", "BoardView", "dep"),
    ("ClickToMove", "Move", "dep", "crea"),
    ("PieceGlyphs", "Piece", "dep"),
    ("Main", "GuiApplication", "dep", "crea y conecta"),
    ("GuiApplication", "LoginDialog", "dep", "crea"),
    ("GuiApplication", "MainMenuWindow", "dep", "crea"),
    ("GuiApplication", "GameWindow", "dep", "crea"),
    ("GuiApplication", "TournamentWindow", "dep", "crea"),
    ("GuiApplication", "FeaturesDialog", "dep", "crea"),
    ("GuiApplication", "IAuthService", "assoc"),
    ("GuiApplication", "IHistoryService", "assoc"),
    ("MainMenuWindow", "IHistoryService", "assoc"),
    ("GameWindow", "MoveLog", "assoc"),
    ("TournamentWindow", "MoveLog", "assoc"),
    ("GameWindow", "MoveListPanel", "comp", "1"),
    ("GameWindow", "CapturedPanel", "comp", "1"),
    ("TournamentWindow", "MoveListPanel", "comp", "1"),
    ("TournamentWindow", "CapturedPanel", "comp", "1"),
    ("MoveLog", "AlgebraicNotation", "assoc"),
    ("MoveLog", "Move", "dep"),
    ("CapturedPanel", "PieceGlyphs", "assoc"),
    # --- cuentas (core.auth) ---
    ("AuthService", "IAuthService", "real"),
    ("Pbkdf2PasswordHasher", "IPasswordHasher", "real"),
    ("AuthService", "IUserRepository", "assoc"),
    ("AuthService", "IPasswordHasher", "assoc"),
    ("AuthService", "AuthResult", "dep"),
    ("AuthService", "User", "dep", "crea"),
    ("IUserRepository", "UserAccount", "dep"),
    # --- historial (core.history) ---
    ("RecordingTournament", "ITournament", "real"),
    ("RecordingTournament", "ITournament", "assoc", "envuelve"),
    ("RecordingTournament", "IMatchHistoryRepository", "assoc"),
    ("RecordingTournament", "User", "assoc"),
    ("RecordingTournament", "RoundRecord", "dep", "crea"),
    ("HistoryService", "IHistoryService", "real"),
    ("HistoryService", "IMatchHistoryRepository", "assoc"),
    ("HistoryService", "HistorySummary", "dep", "crea"),
    ("IMatchHistoryRepository", "RoundRecord", "dep"),
    ("RoundRecord", "RoundOutcome", "dep"),
    # --- persistencia (adapters.persistence) ---
    ("JdbcUserRepository", "IUserRepository", "real"),
    ("JdbcMatchHistoryRepository", "IMatchHistoryRepository", "real"),
    ("JdbcUserRepository", "SqliteDatabase", "assoc"),
    ("JdbcMatchHistoryRepository", "SqliteDatabase", "assoc"),
    ("JdbcUserRepository", "DatabaseException", "dep"),
    ("JdbcMatchHistoryRepository", "DatabaseException", "dep"),
    # --- adaptadores de entrada nuevos ---
    ("ConsoleLogin", "IAuthService", "assoc"),
    ("HistoryPrinter", "IHistoryService", "assoc"),
    ("TournamentConsoleAdapter", "HistoryPrinter", "assoc"),
    ("LoginDialog", "IAuthService", "assoc"),
    ("HistoryDialog", "IHistoryService", "assoc"),
    ("TournamentWindow", "HistoryDialog", "assoc"),
    ("Main", "SqliteDatabase", "dep", "crea y conecta"),
    ("Main", "AuthService", "dep"),
    ("Main", "RecordingTournament", "dep"),
    ("Main", "ConsoleLogin", "dep"),
]

styles = {
    "real": 'style=dashed, arrowhead=empty',
    "comp": 'dir=back, arrowtail=diamond, arrowhead=none',
    "agg":  'dir=back, arrowtail=odiamond, arrowhead=none',
    "assoc": 'arrowhead=vee',
    "dep":  'style=dashed, arrowhead=vee, color="#777777"',
}

lines = ['digraph UML {',
         'graph [rankdir=TB, fontname="Helvetica", nodesep=0.35, ranksep=0.55, newrank=true, pad=0.3, '
         'label=<<B>TPO Ajedrez — Diagrama de clases</B><BR/><FONT POINT-SIZE="10">Azul: núcleo · Amarillo: interfaces · Verde: adaptadores · Violeta: donde se inyectan las dependencias. Todas las flechas entre adaptadores y núcleo apuntan HACIA el núcleo.</FONT>>, labelloc=t, fontsize=16];',
         'node [shape=plain, fontname="Helvetica", fontsize=10];',
         'edge [fontname="Helvetica", fontsize=9];']
lines.append('subgraph cluster_core { label=<<B>NÚCLEO (ar.edu.uade.chess.core) — sin dependencias de frameworks ni UI</B>>; style="rounded"; color="#3b6fd8"; penwidth=2; fontsize=13;')
lines.append(cluster("auth", "auth  (puerto de entrada IAuthService, puerto de salida IUserRepository)", auth, "#3b6fd8"))
lines.append(cluster("history", "history  (Decorator, puerto de salida IMatchHistoryRepository)", history, "#3b6fd8"))
lines.append(cluster("tournament", "tournament  (puerto ITournament, Factory)", tournament, "#3b6fd8"))
lines.append(cluster("ai", "ai  (Strategy: IPlayStyle)", ai, "#3b6fd8"))
lines.append(cluster("game", "game", game, "#3b6fd8"))
lines.append(cluster("commands", "commands  (Command)", commands, "#3b6fd8"))
lines.append(cluster("rules", "rules", rules, "#3b6fd8"))
lines.append(cluster("setup", "setup  (Factory)", setup, "#3b6fd8"))
lines.append(cluster("movement", "movement  (Strategy)", movement, "#3b6fd8"))
lines.append(cluster("model", "model", model, "#3b6fd8"))
lines.append('}')
lines.append(cluster("adapters", "ADAPTADORES (ar.edu.uade.chess.adapters.console)", adapters, "#2e9e5b"))
lines.append(cluster("gui", "ADAPTADOR GRÁFICO de prueba (ar.edu.uade.chess.adapters.gui, Swing)", gui, "#2e9e5b"))
lines.append(cluster("persistence", "ADAPTADOR DE SALIDA (ar.edu.uade.chess.adapters.persistence, SQLite vía JDBC)", persistence, "#2e9e5b"))
lines.extend(root)
lines.append('{rank=min; Main;}')
for e in edges:
    src, dst, kind = e[0], e[1], e[2]
    if kind in ("comp", "agg"):
        lines.append(f'{src} -> {dst} [{styles[kind]}, headlabel="{e[3]}"];')
    else:
        label = f', label="{e[3]}"' if len(e) > 3 else ""
        lines.append(f'{src} -> {dst} [{styles[kind]}{label}];')
lines.append('}')

open("class-diagram.dot", "w", encoding="utf-8").write("\n".join(lines))
print("ok")
