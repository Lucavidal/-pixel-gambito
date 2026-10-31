# TPO Ajedrez — Ingeniería de Software (3.4.214)

Implementación del ajedrez en **Java 17+**, separando núcleo y adaptadores.
El núcleo no depende de ningún framework; la única dependencia de ejecución es el driver de SQLite, que usa solo el adaptador de persistencia.
Convención (la misma de los ejemplos del profesor): **comentarios en español, identificadores en inglés**.
Los textos que ve el jugador en la consola están en español.

## Requisitos
- JDK 17 o superior
- Maven 3.8+ (o un IDE que lo traiga integrado: IntelliJ IDEA, Eclipse, VS Code con Extension Pack for Java)

## Cómo correrlo

```bash
mvn test                       # corre los 142 tests: 109 del núcleo + 14 de consola + 13 de la interfaz gráfica + 6 de la base
mvn package                    # genera target/tpo-chess-1.0.0.jar (incluye el driver de SQLite)
java -jar target/tpo-chess-1.0.0.jar               # pregunta el modo (1 a 4)
java -jar target/tpo-chess-1.0.0.jar torneo        # torneo contra la IA en consola
java -jar target/tpo-chess-1.0.0.jar gui           # interfaz gráfica: inicio de sesión + menú principal
java -jar target/tpo-chess-1.0.0.jar gui-torneo    # interfaz gráfica, directo al torneo después de iniciar sesión
```

## Cuentas e historial (SQLite)

Al arrancar se pide **iniciar sesión o crear una cuenta**: en la consola o en una ventana, según el modo elegido.
Las cuentas y el **historial del torneo** se guardan en el archivo `ajedrez.db`, que se crea en la carpeta desde
donde se ejecuta el programa. Cada ronda terminada (ganada, perdida o tablas) queda guardada con rival, fecha y
cantidad de jugadas. Se consulta con el comando `historial` en la consola o con el botón *Mi historial* en la ventana.
Las contraseñas se guardan con hash PBKDF2 y sal, nunca en texto plano.

En IntelliJ, después de bajar estos cambios: clic derecho en `pom.xml` → *Maven* → *Reload project* (para que baje el driver).

## Interfaz gráfica

Hecha con Swing, que viene en el JDK: no agrega dependencias. Es otro adaptador (`adapters/gui`) sobre los
mismos puertos que usa la consola (`IAuthService`, `IGame`, `ITournament`, `IHistoryService`).

1. **Inicio de sesión**: pestañas *Iniciar sesión* y *Crear cuenta* (con "repetir contraseña" y los requisitos a la vista).
   Si el usuario no existe, pasa sola a *Crear cuenta* con el nombre ya escrito.
2. **Menú principal**: saludo, totales de tu historial y cuatro tarjetas: *Partida libre*, *Torneo escalera*,
   *Mi historial* y *Qué incluye el juego*. Además, *Cerrar sesión* y *Salir*.
3. **Partida libre**: tablero con destinos legales (punto = casilla libre, anillo = captura), última jugada
   resaltada y rey en jaque en rojo. Panel lateral con turno y estado, planilla de jugadas (`e4xf5`, `Dd1-h5+`),
   piezas capturadas, ayuda y botones *Deshacer* (Ctrl+Z), *Rehacer* (Ctrl+Y), *Nueva partida* y *Volver al menú*.
4. **Torneo**: lo mismo, más la escalera de rivales (ganada / jugando / pendiente), la ficha del rival
   (estilo y dificultad explicados), la IA "pensando…" y un aviso al terminar cada ronda.
   Botones *Reiniciar*, *Mi historial* y *Volver al menú*.

Si ocurre un error inesperado, se muestra en una ventana (sin consola, como con `javaw`, se perdería).

En IntelliJ: *File → Open* → elegir la carpeta (detecta el `pom.xml`). Para jugar, correr `Main`; para los tests, clic derecho sobre `src/test/java` → *Run All Tests*.

## Cómo se juega (consola)

| Comando     | Qué hace                                  |
|-------------|-------------------------------------------|
| `e2 e4`     | mueve la pieza de e2 a e4                 |
| `ver g1`    | muestra los destinos legales de esa pieza |
| `deshacer`  | deshace la última jugada                  |
| `rehacer`   | rehace la jugada deshecha                 |
| `salir`     | termina                                   |

Blancas en MAYÚSCULA, negras en minúscula: P peón · T torre · C caballo · A alfil · D dama · R rey.

## Torneo escalera contra la IA

El usuario (blancas) juega una partida por ronda contra un rival "histórico" controlado por la IA.
Cada rival combina un **estilo** (agresivo, defensivo, equilibrado) y una **dificultad** (fácil, media, difícil)
que sube ronda a ronda. 3 rondas: jaque mate a favor = pasa de ronda, en contra = eliminado, ahogado = se repite la ronda.
Si gana la final, sale campeón.

| Comando     | Qué hace                                  |
|-------------|-------------------------------------------|
| `e2 e4`     | juega la jugada; la IA responde enseguida |
| `ver g1`    | muestra los destinos legales de esa pieza |
| `rival`     | muestra el rival, su estilo y dificultad  |
| `historial` | muestra tus rondas guardadas y totales    |
| `salir`     | abandona el torneo                        |

En el torneo no hay deshacer/rehacer. La IA "piensa" 0,9 s antes de responder (constante `AI_THINKING_DELAY_MILLIS` en `Main`). Diseño, patrones y decisiones: [docs/CAMBIOS_TORNEO.md](docs/CAMBIOS_TORNEO.md).

## Estructura

```
src/main/java/ar/edu/uade/chess/
├── Main.java                    ← donde se inyectan las dependencias (conecta todo)
├── core/                        ← reglas del juego, CERO dependencias externas
│   ├── model/      Color, Position, Direction, Piece, Board, BoardView
│   ├── movement/   IMovementRule + SlidingMovement, JumpMovement, PawnMovement   (Strategy)
│   ├── setup/      PieceFactory (Factory), IBoardSetup, StandardSetup
│   ├── rules/      CheckDetector, MoveValidator
│   ├── commands/   ICommand, MoveCommand, CommandHistory                         (Command)
│   ├── game/       IGame (puerto de entrada), Game, GameStatus, MoveResult
│   ├── auth/       IAuthService (puerto de entrada), AuthService, IUserRepository (puerto de salida),
│   │               IPasswordHasher, Pbkdf2PasswordHasher, User, UserAccount, AuthResult
│   ├── history/    RecordingTournament (Decorator), IMatchHistoryRepository (puerto de salida),
│   │               IHistoryService, HistoryService, RoundRecord, HistorySummary
│   ├── ai/         IMoveChooser, ComputerPlayer, IPlayStyle + 3 estilos (Strategy), Difficulty,
│   │               MoveAnalyzer, MoveFacts, PieceValue, Move
│   └── tournament/ ITournament (puerto de entrada), Tournament, HistoricalPlayer,
│                   HistoricalPlayerFactory (Factory), IGameFactory, IOpponentFactory, ...
└── adapters/
    ├── console/                 ← ConsoleAdapter, TournamentConsoleAdapter, ConsoleLogin, HistoryPrinter,
    │                              AlgebraicNotation, TextRenderer
    ├── gui/                     ← GuiApplication (navegación), LoginDialog, MainMenuWindow, GameWindow,
    │                              TournamentWindow, HistoryDialog, FeaturesDialog, BoardPanel, MoveLog,
    │                              MoveListPanel, CapturedPanel, ClickToMove, PieceGlyphs, Theme (Swing)
    └── persistence/             ← SqliteDatabase, JdbcUserRepository, JdbcMatchHistoryRepository (adaptadores de SALIDA)
docs/
├── class-diagram.png / .svg     ← UML (es el que se entrega)
├── class-diagram.dot            ← fuente del UML
└── generate_uml.py              ← regenera el .dot; después: dot -Tpng class-diagram.dot -o class-diagram.png
```

## Alcance
- **Obligatorio (completo):** tablero 8x8, las 6 piezas, turnos, captura, validación de movimientos inválidos, detección de jaque.
- **Opcional implementado:** jaque mate, rey ahogado, deshacer/rehacer, prohibición de dejar al propio rey en jaque.
- **Diferencial:** IA con estilos y dificultades + torneo escalera contra jugadores históricos.
- **Interfaz gráfica:** Swing, con inicio de sesión, menú, partida libre y torneo.
- **Persistencia:** inicio de sesión con cuentas e historial del torneo en SQLite.
- **Opcional no implementado:** enroque, captura al paso, promoción, tablas por repetición/50 movimientos.
