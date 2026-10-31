# Pixel Gambito — ajedrez en 3D (TPO Ingeniería de Software, UADE)

**Contacto:** pixelgambito.contacto@gmail.com

## TPO Ajedrez — Ingeniería de Software (3.4.214)

Implementación del ajedrez en **Java 17+**, separando núcleo y adaptadores.
El núcleo no depende de ningún framework; las dependencias de ejecución las usan solo los adaptadores: el driver de SQLite (persistencia) y Javalin + Jackson (web).
Convención (la misma de los ejemplos del profesor): **comentarios en español, identificadores en inglés**.
Los textos que ve el jugador en la consola están en español.

## Requisitos
- JDK 17 o superior
- Maven 3.8+ (o un IDE que lo traiga integrado: IntelliJ IDEA, Eclipse, VS Code con Extension Pack for Java)

## Cómo correrlo

```bash
mvn test                       # corre los 227 tests
mvn package                    # genera target/tpo-chess-1.0.0.jar (incluye el driver de SQLite)
java -jar target/tpo-chess-1.0.0.jar               # versión web: abre el navegador en http://localhost:7070
java -jar target/tpo-chess-1.0.0.jar menu          # pregunta el modo (1 a 5)
java -jar target/tpo-chess-1.0.0.jar consola       # partida libre en consola
java -jar target/tpo-chess-1.0.0.jar torneo        # torneo contra la IA en consola
java -jar target/tpo-chess-1.0.0.jar gui           # interfaz gráfica: inicio de sesión + menú principal
java -jar target/tpo-chess-1.0.0.jar gui-torneo    # interfaz gráfica, directo al torneo después de iniciar sesión
java -jar target/tpo-chess-1.0.0.jar web           # tablero 3D en el navegador: http://localhost:7070
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

## Tablero 3D (navegador)

Otro adaptador más (`adapters/web`): un servidor **Javalin** expone la partida por HTTP/JSON y sirve un front hecho
con **Three.js** (`src/main/resources/web`) que usa los modelos `.glb` de las piezas. El front **no tiene reglas de
ajedrez**: pide al backend los movimientos legales, manda cada jugada y después sincroniza la escena con el estado
que devuelve el servidor. Three.js está incluido en `web/vendor`, así que funciona sin internet.

Correrlo: `java -jar target/tpo-chess-1.0.0.jar` (sin argumentos, o con `web`). Se abre el navegador solo; si no,
entrar a <http://localhost:7070>. En IntelliJ: correr `Main` (sin argumentos ya abre la web).

Cada usuario inicia sesión con su cuenta (la misma base que la consola y la ventana) y juega en **su propio
tablero**. La sesión es una cookie `HttpOnly; SameSite=Strict` con un token aleatorio; las sesiones viven en memoria
(`WebSessions`), así que al reiniciar el servidor hay que volver a entrar. Sin sesión, `/api/game/*` responde **401**.

| Endpoint                        | Qué hace                                                                   |
|---------------------------------|----------------------------------------------------------------------------|
| `POST /api/auth/register`       | `{"username","password"}` → crea la cuenta (`IAuthService`) y abre la sesión |
| `POST /api/auth/login`          | `{"username","password"}` → abre la sesión; si falla, `success:false` y el motivo (`UNKNOWN_USER` / `WRONG_PASSWORD`) |
| `POST /api/auth/logout`         | cierra la sesión                                                           |
| `GET  /api/auth/me`             | el usuario logueado, o 401                                                 |
| `GET  /api/game/state`          | foto de la partida: piezas, turno, estado, rey en jaque, jugadas, capturadas |
| `GET  /api/game/moves?from=e2`  | destinos legales de esa pieza (`IGame.legalMoves`), marcando las capturas  |
| `POST /api/game/move`           | `{"from":"e2","to":"e4"}` → resultado del núcleo (`MoveResult`) y estado nuevo |
| `POST /api/game/undo` / `redo`  | deshace / rehace con el `CommandHistory` del núcleo                         |
| `POST /api/game/reset`          | partida nueva                                                              |
| `GET  /api/tournament/state`    | escalera de rivales (ganado / jugando / perdido / pendiente), ronda, estado del torneo y tablero |
| `POST /api/tournament/start`    | `{"level":"MEDIUM"}` → torneo nuevo en la ronda 1, con la escalera desde esa dificultad (EASY / MEDIUM / HARD; sin `level`, la última) |
| `GET  /api/tournament/moves?from=e2` | destinos legales de una pieza **del usuario** (las de la IA no se eligen) |
| `POST /api/tournament/move`     | `{"from","to"}` → **solo** la jugada del usuario (`ITournament.playUserMove`) |
| `POST /api/tournament/opponent` | la respuesta de la IA (`playOpponentMove`); **409** si no le toca          |
| `GET  /api/history?limit=50`    | "Mi historial" (`IHistoryService`): totales y últimas rondas del usuario logueado, con los mismos textos que la ventana |

En el torneo, cada turno son dos pedidos, como en la ventana Swing: el front muestra tu jugada, espera un momento
("pensando…") y recién ahí pide la de la IA. Si la ronda termina, la respuesta trae `roundResult` con el mismo
mensaje que la ventana (pasás de ronda, eliminado, tablas o campeón) y el estado ya es el de la ronda siguiente.
Cada ronda terminada se guarda en el historial del usuario (`RecordingTournament`). El tablero viaja en el mismo
formato que la partida libre, así las vistas 2D y 3D lo dibujan sin cambios.

Pantallas (se eligen por el hash de la URL, así funcionan *atrás* y recargar la página):

1. `#/login` — pestañas *Iniciar sesión* y *Crear cuenta*, con el mismo comportamiento que la ventana Swing:
   si el usuario no existe pasa sola a *Crear cuenta* con el nombre escrito, y los mensajes son los del núcleo.
2. `#/menu` — saludo con tu récord en el torneo (+ganadas =tablas −perdidas), *Partida libre*, *Torneo escalera*
   (con la escalera de rivales y en qué ronda vas), *Mi historial*, *Qué incluye el juego* y *Cerrar sesión*.
3. `#/libre` — el tablero (2D o 3D). *Volver al menú* deja la partida guardada en la sesión: al volver (o al
   recargar) sigue donde estaba.
4. `#/torneo` — primero elegís **desde qué dificultad empezás** (Fácil, Media o Difícil). La escalera tiene 4 rondas
   y sube de dificultad: 2 rivales del nivel elegido y 2 del siguiente (Difícil = 4 difíciles), sorteados entre los
   **12 ajedrecistas históricos** del plantel (4 por nivel). *Reiniciar torneo* vuelve a mostrar el selector.
   Después, el mismo tablero (2D o 3D) contra la IA, con la escalera (ganada / jugando / perdida / pendiente),
   la ficha del rival (estilo y dificultad explicados) y "pensando…" mientras juega la IA. Al terminar cada ronda,
   un aviso con el mismo mensaje que la ventana Swing; la posición final queda a la vista hasta pasar a la ronda
   siguiente. No hay *Deshacer*: la IA ya respondió. Si se recarga la página después de tu jugada, la IA responde
   al volver. El botón *Mi historial* lleva al historial.
5. `#/historial` — totales (ganadas, tablas, perdidas, títulos) y las últimas rondas con fecha, ronda, rival,
   resultado (*Campeón* si fue la final ganada) y cantidad de jugadas. Son las mismas rondas que ven la consola y
   la ventana Swing: se guardan en `ajedrez.db`.

**Tablero 3D:** en `web/js/board.js`, la constante `BOARD_THEME` elige el tema: `'clasico'` (por defecto: casillas
lisas verde y crema, marco fino) o `'madera'` (el tablero anterior, con grano pixelado y marco oscuro). Las piezas
usan `PIECE_SCALE` (`pieces.js`) para dejar aire entre vecinas; la luz principal, el contraluz y la cámara están en
`scene.js` (las luces giran con la cámara al ver desde negras).

Sin sesión, cualquier pantalla manda a `#/login`; si la sesión se pierde en medio del juego (por ejemplo, se reinició
el servidor), avisa y vuelve al inicio. El tablero 3D queda siempre de fondo, detrás del inicio y del menú.

**2D o 3D:** el botón *Ver en 2D* / *Ver en 3D* cambia la vista del tablero en cualquier momento, sin perder la
partida (las dos vistas se sincronizan con el mismo estado del backend). La vista 2D es la de la ventana Swing:
casillas verde y crema, figuras Unicode, punto = casilla libre, anillo = captura, última jugada en amarillo y rey en
jaque en rojo; también se juega con el teclado (flechas + Enter). El navegador recuerda la vista elegida. Si los
modelos `.glb` no cargan, se juega igual en 2D.

Cómo se juega: clic en una pieza del turno (se levanta y aparecen sus destinos: disco = casilla libre,
marco = captura), clic en un destino para mover. Clic en vacío o **Esc** deselecciona. Botones *Deshacer*
(Ctrl+Z), *Rehacer* (Ctrl+Y), *Ver desde negras* (gira la cámara), *Reiniciar* y *Volver al menú*. Se gira la cámara arrastrando y
se hace zoom con la rueda.

Front, un módulo por responsabilidad (`web/js`): `view3d.js` y `view2d.js` (las dos vistas del tablero, con la
misma interfaz: `main.js` no sabe cuál está usando), `scene.js` (render, luces, cámara), `board.js` (tablero y
coordenadas: `toWorld` / `fromWorld`), `pieces.js` (carga, clonado y tinte de los `.glb`), `input.js` (raycast y
selección), `api.js` (único que conoce las URLs), `anim.js` (arcos, capturas), `hud.js` (turno, jaque, historial)
`router.js` (qué pantalla se ve según el hash), `screens/login.js`, `screens/menu.js` y `screens/history.js`
(las pantallas sin tablero),
`tournament-panel.js` (escalera, ficha del rival y aviso de fin de ronda)
y `main.js` (conecta todo). En `pieces.js`, `FACE_CAMERA = true` hace que todas las piezas miren a la cámara.

## Publicarlo en internet

El modo web es un solo jar (front, modelos 3D y Three.js incluidos). Opciones:

- **VPS con Java 17:** subir el jar y usar `deploy/pixelgambito.service` (systemd) y `deploy/Caddyfile` (HTTPS automático con dominio).
- **Docker (Railway, Fly.io, Render):** el `Dockerfile` compila y arranca el modo web en `$PORT`.
  Montar un **volumen persistente en `/data`** (ahí se guarda `ajedrez.db`) y poner `CHESS_SECURE_COOKIE=true` si el sitio va por HTTPS.

Propiedades: `-Dchess.port=8080` (puerto) y `-Dchess.secureCookie=true` (cookie de sesión `Secure`, solo con HTTPS).
Las sesiones viven en memoria: al reiniciar el servidor hay que volver a iniciar sesión.

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
    ├── persistence/             ← SqliteDatabase, JdbcUserRepository, JdbcMatchHistoryRepository (adaptadores de SALIDA)
    └── web/                     ← WebServer (Javalin, rutas HTTP), WebGameSession (IGame -> DTOs), WebDtos, PieceTypes
src/main/resources/web/          ← front 3D: index.html, styles.css, js/ (módulos), models/ (.glb), vendor/three
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
- **Tablero 3D:** adaptador web (Javalin) + front Three.js con los modelos de las piezas.
- **Opcional no implementado:** enroque, captura al paso, promoción, tablas por repetición/50 movimientos.


## Créditos y licencias

- Piezas 3D modeladas en Blender, texturas pixel art en Krita.
- Hecho con Java, Javalin y Three.js (licencia MIT).
- © 2026 Pixel Gambito. Todos los derechos reservados.

La marca (nombre, año y mail) está en un solo lugar en cada interfaz: `web/js/brand.js` para la web
y `adapters/Brand.java` para la ventana Swing.

### Three.js — licencia MIT

Three.js viene incluido en `src/main/resources/web/vendor/three` (archivo `LICENSE`):

```
The MIT License

Copyright © 2010-2024 three.js authors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
