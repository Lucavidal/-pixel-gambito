# Cambios: Torneo escalera contra la IA

## 1. Resumen

1. Se agregó un **campeonato tipo escalera**: el usuario juega una partida por ronda contra un rival controlado por la IA.
2. Cada rival es un **jugador histórico** = nombre + descripción + **estilo** (agresivo, defensivo, equilibrado) + **dificultad** (fácil, media, difícil).
3. La dificultad sube ronda a ronda. Jaque mate a favor = pasa de ronda; en contra = eliminado; ahogado = se repite la ronda. Ganar la final = campeón.
4. La IA es **núcleo puro**. Elige solo entre jugadas legales que le pide a `IGame` y evalúa cada jugada simulándola (`move` + `undo`), sin duplicar reglas.
5. **No se modificó ninguna clase existente del núcleo.** Solo cambió `Main`, que es donde se conectan las implementaciones. Funciona en 10x10 y con piezas nuevas.

## 2. Archivos creados y modificados

| Ruta | Estado | Para qué sirve / qué cambió |
|---|---|---|
| `src/main/java/ar/edu/uade/chess/core/ai/Move.java` | Nuevo | Record `from`/`to`: una jugada candidata. |
| `core/ai/IMoveChooser.java` | Nuevo | Interfaz "elegí una jugada para el que tiene el turno". Es lo único que el torneo conoce del rival. |
| `core/ai/ComputerPlayer.java` | Nuevo | La IA. Arma las jugadas legales, juega al azar con la probabilidad de la dificultad y, si no, elige la de mayor puntaje del estilo. `Random` inyectado. |
| `core/ai/IPlayStyle.java` | Nuevo | Strategy del estilo: `score(MoveFacts)`, `getDisplayName()`, constante `CHECKMATE_BONUS`. |
| `core/ai/AggressiveStyle.java` | Nuevo | Puntúa capturas (por valor) y jaques. |
| `core/ai/DefensiveStyle.java` | Nuevo | Penaliza dejar piezas atacadas y premia piezas propias junto al rey. |
| `core/ai/BalancedStyle.java` | Nuevo | Suma agresivo + defensivo (composición). |
| `core/ai/Difficulty.java` | Nuevo | Enum `EASY`/`MEDIUM`/`HARD` con probabilidad de jugada al azar (40 % / 15 % / 0 %). |
| `core/ai/MoveAnalyzer.java` | Nuevo | Simula una jugada sobre `IGame` (move → mirar → undo) y devuelve sus `MoveFacts`. |
| `core/ai/MoveFacts.java` | Nuevo | Record con los datos de una jugada: captura, jaque, mate, material colgado y piezas junto al rey. |
| `core/ai/PieceValue.java` | Nuevo | Valor de una pieza = su movilidad desde el centro de un tablero vacío del mismo tamaño. No conoce tipos de pieza. |
| `core/tournament/ITournament.java` | Nuevo | **Puerto de entrada** del torneo. |
| `core/tournament/Tournament.java` | Nuevo | Implementación: rondas, avance, eliminación, repetición por tablas. |
| `core/tournament/TournamentStatus.java` | Nuevo | Enum `NOT_STARTED`/`IN_PROGRESS`/`ELIMINATED`/`CHAMPION` con nombre en español. |
| `core/tournament/RoundOutcome.java` | Nuevo | Enum `ONGOING`/`WON`/`LOST`/`DRAWN`: cómo quedó la ronda tras un turno. |
| `core/tournament/TurnReport.java` | Nuevo | Record con el resultado de un turno: jugada del usuario, respuesta de la IA y resultado de la ronda. |
| `core/tournament/HistoricalPlayer.java` | Nuevo | Record: nombre + descripción + estilo + dificultad. |
| `core/tournament/HistoricalPlayerFactory.java` | Nuevo | Factory del plantel, ordenado por dificultad. `DEFAULT_ROUNDS = 3`. |
| `core/tournament/IGameFactory.java` | Nuevo | Fábrica de partidas: el torneo pide una partida nueva por ronda. |
| `core/tournament/BoardSetupGameFactory.java` | Nuevo | Implementación que arma una `Game` a partir de un `IBoardSetup`. |
| `core/tournament/IOpponentFactory.java` | Nuevo | Arma la IA de un rival a partir de su ficha. |
| `adapters/console/TournamentConsoleAdapter.java` | Nuevo | Adaptador de consola del torneo. Reusa `AlgebraicNotation` y `TextRenderer`. |
| `src/main/java/ar/edu/uade/chess/Main.java` | **Modificado** | **Qué:** elige el modo (pregunta o lee el argumento `torneo`) y conecta torneo + IA + adaptador. Constante `TOURNAMENT_USER_COLOR`. **Por qué:** es el único lugar donde se eligen implementaciones concretas. La partida libre se arma igual que antes. |
| `src/test/.../core/ai/ComputerPlayerTest.java` | Nuevo | Tests de la IA (ver sección 6). |
| `src/test/.../core/ai/PlayStyleTest.java` | Nuevo | Tests de los estilos. |
| `src/test/.../core/ai/PieceValueTest.java` | Nuevo | Tests del valor de piezas. |
| `src/test/.../core/extensibility/AiExtensibilityTest.java` | Nuevo | IA en 10x10 y con pieza nueva. |
| `src/test/.../core/tournament/TournamentTest.java` | Nuevo | Reglas del torneo. |
| `src/test/.../core/tournament/HistoricalPlayerFactoryTest.java` | Nuevo | Plantel y orden. |
| `src/test/.../adapters/console/TournamentConsoleAdapterTest.java` | Nuevo | Integración consola + núcleo + IA. |
| `docs/generate_uml.py` | **Modificado** | **Qué:** se agregaron los clusters `ai` y `tournament`, el adaptador nuevo, los atributos/métodos nuevos de `Main` y sus relaciones. **Por qué:** el UML tiene que coincidir con el código. |
| `docs/class-diagram.dot` | **Regenerado** | Salida de `generate_uml.py`. |
| `README.md` | **Modificado** | **Qué:** sección del torneo, comandos, estructura de paquetes, cantidad de tests (61 → 91) y alcance. **Por qué:** documentación de entrega. |
| `docs/CAMBIOS_TORNEO.md` | Nuevo | Este documento. |

No se tocaron `Game`, `Board`, `Piece`, `MoveValidator`, `CheckDetector`, las reglas de movimiento, `ConsoleAdapter`, `TestHelper` ni ningún test existente.

## 3. Cómo se conecta

```
                       ┌──────────────────────────── Main (inyección de dependencias) ──────────────────────────┐
                       │ crea: StandardSetup, CheckDetector, MoveValidator, PieceValue, MoveAnalyzer, Random     │
                       │       BoardSetupGameFactory, HistoricalPlayerFactory.createRoster(),                    │
                       │       rival -> new ComputerPlayer(rival.style(), rival.difficulty(), analyzer, random)  │
                       └───────────────┬─────────────────────────────────────────────────────┬────────────────┘
                                       │                                                     │
   ADAPTADOR                           ▼                                                     │
   ┌────────────────────────────────────────────┐                                            │
   │ TournamentConsoleAdapter                   │                                            │
   │  (AlgebraicNotation, TextRenderer)         │                                            │
   └──────────────────┬─────────────────────────┘                                            │
                      │ usa solo el puerto                                                   │
 ═════════════════════╪══════════════════════ NÚCLEO ════════════════════════════════════════╪═══════════
                      ▼                                                                      ▼
            «puerto» ITournament ◄── Tournament ──► IGameFactory ◄── BoardSetupGameFactory ──► Game (IGame)
                                         │  │                                                   ▲
                                         │  └──► IOpponentFactory ──crea──► IMoveChooser         │
                                         │                                     ▲                 │
                                         ▼                                     │                 │
                               List<HistoricalPlayer>                    ComputerPlayer ─────────┤ legalMoves()
                          (nombre, descr., IPlayStyle, Difficulty)        │      │               │
                                                                           │      ▼               │
                                                         IPlayStyle ◄──────┘  MoveAnalyzer ───────┘ move()+undo()
                                              Aggressive / Defensive / Balanced    │
                                                                                    ▼
                                                                              PieceValue ──► Piece.possibleMoves()
```

Todas las flechas entre el adaptador y el núcleo apuntan hacia el núcleo. La IA y el torneo dependen de `IGame` y `BoardView`, nunca de `Game` ni de `Board`. Excepción: `BoardSetupGameFactory` crea la `Game` concreta, y `PieceValue` crea un `Board` vacío para medir movilidad.

## 4. Patrones

### Usados

| Patrón | Dónde | Por qué se justifica |
|---|---|---|
| **Strategy** | `IPlayStyle` + `AggressiveStyle`, `DefensiveStyle`, `BalancedStyle` | Hay variación real: tres formas de puntuar una jugada, intercambiables en tiempo de ejecución. Un estilo nuevo es una clase nueva. Sin esto, `ComputerPlayer` tendría un `switch` por estilo. |
| **Composición** (en vez de herencia) | `HistoricalPlayer` (estilo + dificultad), `BalancedStyle` (agresivo + defensivo), `ComputerPlayer` | Estilo y dificultad son ejes independientes. Con herencia serían 3 × 3 = 9 clases, y cada estilo nuevo sumaría 3. Es el mismo criterio que `Piece` + `IMovementRule`. |
| **Factory** | `HistoricalPlayerFactory` | Es el único lugar donde se decide el plantel y su orden. Cambiar rivales o rondas se hace en un solo archivo. Sigue el estilo de `PieceFactory` (métodos estáticos, constructor privado). |
| **Factory (fábrica inyectada)** | `IGameFactory` / `BoardSetupGameFactory`, `IOpponentFactory` | El torneo necesita una partida y una IA nuevas por ronda, pero no debe hacer `new Game` ni `new ComputerPlayer` adentro. Así el torneo no conoce el tablero (8x8 o 10x10) ni la IA concreta, y los tests le pasan posiciones armadas y rivales guionados. |
| **Puerto de entrada (hexagonal)** | `ITournament` | Es lo único que conoce el adaptador. Un adaptador web o gráfico futuro usaría el mismo puerto. No expone `undo` ni `IGame`, para que el usuario no pueda deshacer la jugada de la IA. |
| **Inyección de dependencias** | Constructores de `ComputerPlayer`, `MoveAnalyzer`, `Tournament`, `BoardSetupGameFactory`, `TournamentConsoleAdapter`; todo se conecta en `Main` | El mismo nivel que ya usa `Game`. Permite testear sin infraestructura, y el `Random` con semilla hace que los tests sean deterministas. |

### Evaluados y descartados

| Patrón | Por qué no |
|---|---|
| **State** para el torneo | Los 4 estados solo informan: no cambian el comportamiento de ninguna operación más allá de "si no está en curso, no se juega". Con un enum alcanza, igual que `GameStatus`. State sería sobreingeniería. |
| **Template Method** para los estilos | Obliga a heredar de una clase base. Los estilos no comparten un algoritmo con pasos variables: comparten datos (`MoveFacts`), que se calculan aparte en `MoveAnalyzer`. |
| **Decorator** para la dificultad | La dificultad podría "envolver" la elección del estilo. Pero hoy es un único número (probabilidad de jugar al azar) y un enum es más simple de leer y de defender. Sería la opción si la dificultad empieza a tener comportamientos distintos (ver sección 5). |
| **Observer** para avisar el resultado de la ronda | Hay un solo consumidor (el adaptador) y es sincrónico: devolver un `TurnReport` es más directo. |
| **Command** para la jugada de la IA | Ya existe: la IA juega con `game.move(...)`, que internamente usa `MoveCommand`. No hacía falta otro. |
| **Minimax / búsqueda en profundidad** | No es un patrón, pero se evaluó como "dificultad". En 10x10 o con piezas nuevas el costo crece rápido, es más difícil de testear y de defender, y no lo pide la cátedra. `hangingValue` ya equivale a mirar una respuesta del rival. |

## 5. Decisiones de diseño

| Qué | Por qué | Cuándo romperla |
|---|---|---|
| La IA usa solo `IGame` (y `BoardView`) | Reutiliza la legalidad y el jaque existentes, sin duplicar `MoveValidator`. Funciona con cualquier tablero o pieza. | Si la IA necesitara buscar a mucha profundidad y `move` + `undo` resultara lento. Ahí convendría una copia del tablero o una interfaz de simulación propia. |
| Evaluar jugadas simulando con `move()` + `undo()` | Es el mismo truco que ya usa `MoveValidator.leavesKingInCheck`. La partida queda exactamente igual (hay un test que lo verifica). | Si en el futuro la partida tuviera efectos al mover (por ejemplo, registrar las jugadas en un archivo o avisar a una pantalla), simular dispararía esos efectos. |
| `MoveAnalyzer` calcula los datos y los estilos solo ponderan | Un estilo nuevo es solo una fórmula: no repite la simulación y se testea con `MoveFacts` armados a mano. | Si un estilo necesita un dato que no está en `MoveFacts`: se agrega el campo en `MoveFacts` y su cálculo en `MoveAnalyzer`. |
| Valor de pieza = movilidad en tablero vacío | No conoce tipos de pieza: el canciller y el 10x10 funcionan solos, y da el orden clásico dama > torre > alfil > caballo > peón. | Si el grupo quiere valores clásicos (1/3/3/5/9): convertir `PieceValue` en interfaz y agregar otra implementación. |
| Dificultad = probabilidad de jugar al azar (40 / 15 / 0 %) | Es simple, se explica en una frase y es determinista con semilla. Además es un eje separado del estilo. | Si se quiere una IA "difícil" de verdad: agregar profundidad de búsqueda como otro atributo de `Difficulty`. |
| Todos los estilos dan mate si pueden (`CHECKMATE_BONUS`) | Un rival que no da mate en una sería poco creíble y haría el torneo trivial. | Si se quiere un fácil que "no ve" el mate: el azar del `EASY` ya lo hace a veces; para quitarlo del todo, sacar el bonus del estilo. |
| Empates de puntaje: se desempata con el `Random` | Evita que la IA juegue siempre la misma apertura. Sigue siendo determinista con semilla. | Nunca, salvo que se quiera una IA totalmente predecible (en ese caso, tomar la primera). |
| 3 rondas = tamaño del plantel | Un solo lugar define rivales y rondas, y no hay forma de que no coincidan. | Si se quisieran más rondas que rivales distintos (por ejemplo, repetir rivales): pasar la cantidad de rondas aparte. |
| Usuario con blancas (`TOURNAMENT_USER_COLOR` en `Main`) | Es lo pedido. `Tournament` ya soporta negras: la IA abre cada ronda (hay un test). | Cambiar la constante en `Main`. |
| Derrota = eliminado; tablas = se repite la ronda (partida nueva, mismo rival) | Es lo pedido. Las reglas están en `Tournament.resolve()`. | Si se quieren "vidas" o puntos por tablas: cambiar `resolve()`. |
| Sin deshacer/rehacer en el torneo | Deshacer tendría que deshacer también la jugada de la IA, y sería una trampa. El puerto no lo expone. | Si el grupo quiere un modo práctica: agregar `undo()` al puerto, que deshaga dos jugadas. |
| ~~Sin puerto de salida~~ → **se agregaron dos** (ver sección 13) | Al principio no se guardaba nada. Después se pidieron inicio de sesión e historial: `IUserRepository` e `IMatchHistoryRepository`, con adaptador SQLite. | Para "guardar y continuar" una partida a medias: otro puerto que guarde la lista de jugadas. |
| `TournamentStatus` incluye `NOT_STARTED` | El puerto tiene `start()`, así que antes de llamarlo el torneo no está "en curso". Sigue siendo un enum simple. | — |
| Identificadores en inglés, textos visibles en español (`IN_PROGRESS` → "En curso") | Es la convención del proyecto (ver README). | — |
| Plantel: Petrosian (defensivo, fácil), Capablanca (equilibrado, media), Tal (agresivo, difícil) | Son nombres reales con descripciones neutrales e iguales para todos. El estilo es una elección de juego, no una afirmación biográfica. | Cuando el grupo quiera otro plantel (ver sección 10). |

## 6. Tests agregados (30; total 91 = 61 previos + 30)

**`core/ai/ComputerPlayerTest`** (4)
- `chosenMove_IsAlwaysLegal_InAWholeGameOfAiAgainstAi`: la IA contra la IA durante 30 jugadas con 5 semillas. Cada jugada está en `legalMoves` y `move` devuelve `SUCCESS`.
- `sameSeed_ChoosesTheSameMoves`: con la misma semilla, las 10 primeras jugadas son idénticas.
- `neverChoosesAMoveThatLeavesItsOwnKingInCheck`: con una torre clavada, la IA agresiva nunca captura la dama (sería ilegal) y su rey nunca queda en jaque.
- `easyDifficulty_SometimesMissesTheBestMove_HardNever`: en 50 decisiones, el fácil a veces no captura y a veces sí; el difícil captura siempre.

**`core/ai/PlayStyleTest`** (6)
- `aggressive_CapturesWhenACaptureIsAvailable`: el agresivo captura el caballo libre.
- `defensive_DoesNotLeaveAPieceHangingWhenItHasAnAlternative`: con el caballo atacado, el defensivo lo mueve a una casilla segura en vez de mover el rey.
- `defensive_KeepsPiecesNextToItsKing`: más piezas junto al rey = más puntaje.
- `balanced_PrefersASafeCaptureOverTheSameCaptureLeavingAPieceHanging`: el equilibrado distingue captura segura de captura arriesgada; al agresivo le da igual.
- `everyStyle_DeliversMateInOne`: los tres estilos dan el mate de pasillo.
- `analyzingAMove_LeavesTheGameExactlyAsItWas`: simular no cambia el tablero ni el turno.

**`core/ai/PieceValueTest`** (2)
- `classicPieces_KeepTheUsualOrder_WithoutATablePerType`: dama > torre > alfil > caballo > peón.
- `biggerBoard_ScalesTheValueByItself`: una torre vale más en 10x10.

**`core/extensibility/AiExtensibilityTest`** (4)
- `ai_CapturesOnARowThatOnlyExistsInATenByTenBoard`: captura en la fila 10.
- `ai_PlaysOnlyLegalMovesInATenByTenBoard`: 20 jugadas de la IA contra la IA en 10x10, con un canciller.
- `ai_UsesANewPieceBuiltFromExistingRules`: la IA captura con el salto de caballo del canciller.
- `newPiece_IsValuedFromItsRules`: el canciller vale más que la torre, sin escribir nada.

**`core/tournament/TournamentTest`** (9)
- `winningARound_AdvancesToTheNextRival`: el mate pasa a la ronda 2 con otro rival, y la IA no responde.
- `losingARound_EliminatesTheUser`: el mate en contra da `ELIMINATED`; jugar después devuelve `GAME_OVER`.
- `drawnRound_IsReplayedAgainstTheSameRivalWithANewGame`: el ahogado repite la ronda 1 con el mismo rival y una partida nueva.
- `winningTheFinal_MakesTheUserChampion`: tres victorias dan `CHAMPION`.
- `rivals_ComeInOrderOfDifficulty`: la dificultad nunca baja de una ronda a la siguiente.
- `validUserMove_IsAnsweredByTheAi`: después de una jugada válida, la IA responde.
- `invalidUserMove_DoesNotTriggerTheAiReply`: después de una jugada inválida, la IA no juega.
- `userPlayingBlack_TheAiOpensEachRound`: con negras, la IA mueve primero.
- `beforeStart_StatusIsNotStarted`: antes de `start()`, el estado es `NOT_STARTED` y no hay tablero.

**`core/tournament/HistoricalPlayerFactoryTest`** (3)
- `roster_HasOneRivalPerRound`: el plantel tiene `DEFAULT_ROUNDS` rivales.
- `roster_IsOrderedFromEasiestToHardest`: el orden es `EASY`, `MEDIUM`, `HARD`.
- `roster_HasDifferentStylesAndNames`: no se repiten nombres ni estilos.

**`adapters/console/TournamentConsoleAdapterTest`** (2)
- `showsRoundAndRival_AndTheAiAnswers`: muestra la ronda, el rival con estilo y dificultad y la jugada de la IA real.
- `winningEveryRound_AnnouncesTheChampion`: tres mates por consola anuncian "Campeón".

Para armar las posiciones se usaron los helpers existentes (`TestHelper.sq`, `emptyBoard`, `gameWith`, `snapshot`). El torneo se testea con un rival guionado (`ScriptedOpponent`), sin consola ni azar.

**Nota sobre semillas:** con `java.util.Random`, semillas consecutivas (0, 1, 2…) dan un primer `nextDouble()` casi igual. Por eso los tests de azar usan **una** IA que decide muchas veces, no muchas IAs con semillas seguidas.

## 7. Cambios en el UML

- Cluster nuevo **`ai (Strategy: IPlayStyle)`**: `Move`, `IMoveChooser`, `ComputerPlayer`, `IPlayStyle`, `AggressiveStyle`, `DefensiveStyle`, `BalancedStyle`, `Difficulty`, `MoveAnalyzer`, `MoveFacts`, `PieceValue`.
- Cluster nuevo **`tournament (puerto ITournament, Factory)`**: `ITournament`, `Tournament`, `TournamentStatus`, `RoundOutcome`, `TurnReport`, `HistoricalPlayer`, `HistoricalPlayerFactory`, `IGameFactory`, `BoardSetupGameFactory`, `IOpponentFactory`.
- Adaptadores: se agregó `TournamentConsoleAdapter` → `ITournament`, `AlgebraicNotation`, `TextRenderer`.
- `Main`: atributo `TOURNAMENT_USER_COLOR`, método `wantsTournament(...)` y dependencias hacia `TournamentConsoleAdapter`, `Tournament`, `ComputerPlayer`, `HistoricalPlayerFactory` y `BoardSetupGameFactory`.
- Relaciones nuevas: realizaciones (los 3 estilos → `IPlayStyle`; `ComputerPlayer` → `IMoveChooser`; `Tournament` → `ITournament`; `BoardSetupGameFactory` → `IGameFactory`), composición `BalancedStyle` ◆ agresivo/defensivo, agregación `Tournament` ◇ `HistoricalPlayer` 1..*, y dependencias hacia `IGame`/`BoardView` (nunca hacia `Game`/`Board`, salvo la fábrica que crea la `Game`).
- Se verificó con un script que cada clase de `src/main` tiene su nodo, que no hay nodos sin clase y que no hay flechas hacia clases inexistentes.

Para regenerar (requiere Graphviz instalado):
```bash
cd docs
python generate_uml.py
dot -Tpng class-diagram.dot -o class-diagram.png
dot -Tsvg class-diagram.dot -o class-diagram.svg
```
> `class-diagram.dot`, `class-diagram.png` y `class-diagram.svg` están regenerados con Graphviz 16.1.0 e incluyen el torneo, la IA y la interfaz gráfica.

## 8. Cómo se juega el torneo

```bash
java -jar target/tpo-chess-1.0.0.jar              # elegir "2" (consola) o "4" (ventana)
java -jar target/tpo-chess-1.0.0.jar torneo       # directo, en consola
java -jar target/tpo-chess-1.0.0.jar gui-torneo   # directo, en ventana (ver sección 11)
```
En IntelliJ: correr `Main` y escribir `2`, o agregar `torneo` en *Program arguments*.

| Comando | Qué hace |
|---|---|
| `e2 e4` | Juega la jugada. Si es válida, la IA responde y se muestra su jugada ("Tigran Petrosian juega g7 g5."). |
| `ver g1` | Destinos legales de esa pieza. |
| `rival` | Muestra nombre, estilo, dificultad y descripción del rival actual. |
| `ayuda` | Lista de comandos. |
| `salir` | Abandona el torneo. |

Al terminar cada ronda se informa "Le ganaste a …", "… te ganó" o "Rey ahogado: tablas. Se repite la ronda N", y después la ronda siguiente con su rival.

**Endpoints web:** no hay. El proyecto no tiene `adapters/web` y, como se pidió, no se creó.

## 9. Cómo responder en la defensa

**"Agregá un jugador histórico nuevo"**
Se agrega un `new HistoricalPlayer("Nombre", "Ajedrecista histórico. En este torneo juega con estilo X.", new XStyle(), Difficulty.Y)` en `HistoricalPlayerFactory.createRoster()`. Se ordena solo por dificultad. Ninguna otra clase cambia. Si se agrega como rival extra, también cambia la cantidad de rondas (ver más abajo).

**"Agregá un estilo nuevo"** (por ejemplo, "posicional": premia avanzar piezas hacia el centro)
1. Crear `PositionalStyle implements IPlayStyle` con su fórmula sobre `MoveFacts`.
2. Si necesita un dato nuevo (por ejemplo, la distancia al centro), agregar el campo en `MoveFacts` y su cálculo en `MoveAnalyzer`. Son clases del propio módulo de la IA; las del núcleo de ajedrez no se tocan.
3. Usarlo en un `HistoricalPlayer` de la fábrica.
`ComputerPlayer` y `Tournament` no cambian, porque dependen de la interfaz.

**"Cambiá a 4 rondas"**
Agregar un cuarto rival en `HistoricalPlayerFactory.createRoster()` y poner `DEFAULT_ROUNDS = 4`. `Tournament` usa `rivals.size()`, así que no cambia. El test `roster_IsOrderedFromEasiestToHardest` compara contra una lista fija y habría que actualizarlo (por ejemplo, `EASY, MEDIUM, HARD, HARD`). Si se quiere una dificultad intermedia nueva, se agrega un valor a `Difficulty` en el orden que corresponda.

**"Jugá en 10x10"**
En `Main`, reemplazar `new StandardSetup()` por un `IBoardSetup` de 10x10 (una clase nueva o una lambda, como en `ExtensibilityTest`). `BoardSetupGameFactory` lo usa para cada ronda. La IA recorre `rows()` × `columns()`, pide la legalidad a `IGame` y valora las piezas por movilidad en un tablero del mismo tamaño, así que no cambia nada. `AiExtensibilityTest` lo demuestra. La consola ya dibuja tableros de cualquier tamaño.

**"Agregá una pieza nueva"** (relacionada)
Se arma combinando reglas existentes, como el canciller. La IA la valora y la usa sin cambios: lo demuestran `ai_UsesANewPieceBuiltFromExistingRules` y `newPiece_IsValuedFromItsRules`.

## 10. Decisiones pendientes para el grupo

1. **Cantidad de rondas:** hoy son 3. ¿4 o 5 con dificultades repetidas o intermedias?
2. **Regla de tablas:** hoy se repite la ronda indefinidamente. ¿Limitar las repeticiones (por ejemplo, a la tercera tablas se elimina o se avanza)? ¿Contar el ahogado como medio punto?
3. **Partidas sin fin:** el motor no tiene tablas por material insuficiente, repetición ni 50 movimientos, así que rey contra rey nunca termina. El usuario puede abandonar con `salir`. ¿Agregar un límite de jugadas por ronda en `Tournament` (por ejemplo, 100 = tablas)?
4. **Plantel:** Petrosian / Capablanca / Tal y su asignación de estilo y dificultad son una propuesta. ¿Los mantienen? ¿Descripciones más ricas, cuidando de no inventar datos?
5. **Porcentajes de dificultad** (40 / 15 / 0 %): conviene ajustarlos jugando.
6. **Rendirse:** ¿agregar un comando `rendirse` que cuente como derrota (distinto de `salir`)?
7. **Color del usuario:** ¿siempre blancas, o alternar por ronda? El torneo ya soporta negras.
8. ~~**Carpeta duplicada `tpo-chess/tpo-chess/`**~~: resuelto, se borró. Era una copia idéntica del proyecto original, sin estos cambios.
9. **Interfaz gráfica:** ¿se entrega como prototipo o se pule (ver sección 11)?

## 11. Agregado posterior: interfaz gráfica de prueba

Se agregó un **adaptador gráfico con Swing** (incluido en el JDK, sin dependencias nuevas en el pom). Es un
**prototipo**: alcanza para jugar con clics la partida libre y el torneo, pero no se pulió el diseño visual.

| Ruta | Estado | Para qué sirve |
|---|---|---|
| `adapters/gui/GameWindow.java` | Nuevo | Ventana de partida libre sobre `IGame`, con botones *Deshacer* y *Rehacer*. |
| `adapters/gui/TournamentWindow.java` | Nuevo | Ventana del torneo sobre `ITournament`: ronda, rival, estilo, dificultad, jugada de la IA, aviso de cada resultado y botón *Reiniciar torneo*. |
| `adapters/gui/BoardPanel.java` | Nuevo | `JPanel` que dibuja cualquier `BoardView` (calcula el tamaño de casilla con `rows()`/`columns()`, así que 10x10 funciona) y traduce clics a `Position`. |
| `adapters/gui/ClickToMove.java` | Nuevo | Lógica "clic en pieza → clic en destino", sin Swing, para poder testearla. |
| `adapters/gui/PieceGlyphs.java` | Nuevo | Figura Unicode por símbolo de pieza. Una pieza desconocida (el canciller `K`) se dibuja con su letra. |
| `Main.java` | Modificado | Menú con 4 modos y argumentos `gui` / `gui-torneo`. La creación del torneo pasó a un método `tournament(...)` compartido por consola y ventana. |
| `docs/generate_uml.py`, `class-diagram.dot` | Modificado / regenerado | Cluster nuevo `adapters.gui` con sus 5 clases y relaciones. |
| `test/.../adapters/gui/ClickToMoveTest.java` | Nuevo (4 tests) | Seleccionar y mover, cambiar de selección, cancelar con clic afuera. |
| `test/.../adapters/gui/BoardPanelTest.java` | Nuevo (4 tests) | Clic → casilla en 8x8 y en 10x10, clic fuera del tablero, figura de pieza clásica y de pieza nueva. |

**Decisiones:**
- Es un adaptador más: depende de los mismos puertos que la consola. El núcleo no se tocó.
- `ClickToMove` está separada de la ventana para poder testear la interacción sin abrir nada. Las ventanas en sí no tienen tests automáticos: se probaron abriéndolas.
- `TournamentWindow` reusa `AlgebraicNotation` del adaptador de consola para escribir las jugadas de la IA. Es una dependencia entre adaptadores, aceptable para un prototipo. Si molesta, se mueve la clase a un paquete común de presentación.
- La IA calcula su respuesta en el mismo hilo de la interfaz. En 8x8 tarda milisegundos; si se agregara búsqueda en profundidad, habría que pasarla a un `SwingWorker` para no congelar la ventana.

Total de tests: **99** (61 originales + 30 del torneo + 8 de la interfaz gráfica).

## 12. Agregado posterior: demora de la IA ("está pensando...")

**Qué:** la IA espera `AI_THINKING_DELAY_MILLIS = 900` ms (constante en `Main`) antes de mostrar su jugada, en la consola y en la ventana. Primero se ve la jugada del usuario, después "X está pensando..." y por último la respuesta.

**Cómo:** el puerto `ITournament` sumó tres métodos que parten `play()` en dos mitades:
`playUserMove(from, to)`, `playOpponentMove()` e `isAwaitingOpponent()`. `play()` sigue existiendo y ahora llama a las dos mitades seguidas, así que su comportamiento y sus tests no cambiaron. Mientras se espera a la IA, `playUserMove` devuelve `OPPONENT_PIECE`: el usuario no puede jugar dos veces seguidas.

**Por qué la demora está en los adaptadores y no en el núcleo:**
- Es presentación pura: la IA calcula en milisegundos y el núcleo no tiene por qué esperar.
- Un `Thread.sleep` dentro de la IA haría lentos todos los tests de la IA y del torneo.
- En Swing, dormir dentro de `play()` congela la ventana y la jugada del usuario no llega a dibujarse. Además, la IA simula jugadas sobre el tablero real (`move`/`undo`), así que calcularla en otro hilo mientras la ventana dibuja mostraría jugadas "fantasma". La ventana usa un `javax.swing.Timer`, que corre en el mismo hilo de la interfaz: la ventana no se congela y no hay concurrencia.
- La consola usa `Thread.sleep` en `TournamentConsoleAdapter`. El constructor anterior (sin demora) se mantiene y es el que usan los tests.

**Archivos:** `ITournament`, `Tournament`, `TurnReport` (documentación de `userResult == null` cuando solo juega la IA), `TournamentConsoleAdapter`, `TournamentWindow`, `Main`, `generate_uml.py` y el diagrama regenerado.

**Tests nuevos (4):**
- `TournamentTest.userMoveAlone_LeavesTheTournamentWaitingForTheAi_AndBlocksAnotherUserMove`: después de la jugada del usuario la IA todavía no jugó, no se puede mover otra vez y `playOpponentMove()` aplica la respuesta.
- `TournamentTest.opponentMove_WhenItIsTheUsersTurn_IsRejected`: pedirle a la IA que juegue en el turno del usuario es un error.
- `TournamentTest.userMoveThatEndsTheRound_DoesNotWaitForTheAi`: si la jugada del usuario termina la ronda, no queda esperando a la IA.
- `TournamentConsoleAdapterTest.withADelay_TheAiSaysItIsThinkingBeforeAnswering`: en la consola aparece "pensando" antes que la jugada.

**Cuándo cambiarla:** para quitar la demora, `AI_THINKING_DELAY_MILLIS = 0`. Si el grupo quiere que "piense" más según la dificultad, se puede calcular a partir de `rival.difficulty()` en el adaptador. Limitación: si el usuario juega con negras, la jugada de apertura de la IA al empezar cada ronda sale sin demora (ocurre dentro de `start()`).

Total de tests: **103**.

## 13. Agregado posterior: inicio de sesión e historial en base de datos (SQLite)

**Qué:**
- **Inicio de sesión:** al arrancar se pide iniciar sesión o crear una cuenta, en la consola (`ConsoleLogin`) o en una ventana (`LoginDialog`), según el modo.
- **Historial:** cada ronda del torneo que termina se guarda con usuario, rival, ronda, resultado, jugadas y fecha. Se consulta con `historial` (consola) o con *Mi historial* (ventana), con totales: rondas, ganadas, perdidas, tablas y torneos ganados.
- **Dónde se guarda:** en `ajedrez.db` (SQLite), en la carpeta desde donde se ejecuta.

**Arquitectura (puertos y adaptadores):**
```
 ConsoleLogin / LoginDialog ──► «entrada» IAuthService ◄── AuthService ──► «salida» IUserRepository ◄── JdbcUserRepository ──┐
                                                               └──► IPasswordHasher ◄── Pbkdf2PasswordHasher                   ├─► SqliteDatabase ─► ajedrez.db
 Consola / Ventana del torneo ──► ITournament ◄── RecordingTournament ──► «salida» IMatchHistoryRepository ◄── JdbcMatch... ──┘
                                                    (Decorator)  └── envuelve ──► Tournament (sin cambios)
 HistoryPrinter / HistoryDialog ──► «entrada» IHistoryService ◄── HistoryService ──► IMatchHistoryRepository
```
- El núcleo (`core/auth`, `core/history`) **no conoce SQL ni JDBC**. Solo define interfaces con sus propios tipos (`UserAccount`, `RoundRecord`).
- El SQL está únicamente en `adapters/persistence`, y `Main` es el único lugar que conecta las implementaciones concretas.
- **`Tournament` no se modificó.** El historial lo agrega `RecordingTournament`, un **Decorator** que implementa `ITournament`, delega todo y guarda la ronda cuando termina. Para los adaptadores es un `ITournament` más.

**Patrones:**
- **Decorator** (`RecordingTournament`): agrega comportamiento (guardar) sin tocar la clase envuelta. Se puede sacar o reemplazar sin cambiar las reglas del torneo. Se evaluó meter el repositorio dentro de `Tournament`, pero se descartó porque mezcla reglas de juego con persistencia y obliga a cambiar todos sus tests.
- **Repository** (`IUserRepository`, `IMatchHistoryRepository`): el núcleo pide "guardá / buscá" sin saber cómo se hace.
- **Strategy** (`IPasswordHasher`): el algoritmo de hash es intercambiable. Los tests usan PBKDF2 con pocas iteraciones para ser rápidos.

**Seguridad (decisiones):**
- **Contraseñas:** se guardan con hash PBKDF2-HMAC-SHA256 (del JDK, sin librerías), 120.000 iteraciones y **sal aleatoria por usuario**. Nunca en texto plano (hay un test que lo verifica).
- **Verificación del hash:** se compara en tiempo constante (`MessageDigest.isEqual`).
- **SQL:** siempre `PreparedStatement` con parámetros `?`, sin concatenar texto. Hay un test con un usuario `x' OR '1'='1`.
- **Nombres de usuario:** de 3 a 20 caracteres `[a-z0-9_]`, sin distinguir mayúsculas.
- **Contraseña:** mínimo 4 caracteres (fácil de cambiar en `AuthService.MIN_PASSWORD_LENGTH`).
- **Intentos:** en la consola, después de 3 intentos fallidos el programa termina.
- **En la ventana**, la contraseña se escribe oculta (`JPasswordField`) y se borra de memoria después de usarla.

**Limitaciones conocidas (para la defensa):**
- **Contraseña visible en consola:** se ve mientras se escribe, porque la entrada llega como `BufferedReader` para poder testearla. Se podría usar `System.console().readPassword()` cuando hay consola real, pero IntelliJ no la provee.
- **Usuarios existentes:** un usuario inexistente responde un poco más rápido que uno existente con mala contraseña (no se hashea nada). Eso permitiría adivinar qué usuarios existen. No importa para una app local, pero se corrige hasheando una contraseña de relleno.
- **Partida libre:** no guarda historial; solo se guarda el torneo.
- **Historial completo:** se guardan solo las rondas terminadas. Si el usuario abandona a mitad de ronda, esa ronda no queda.

**Dependencias nuevas (`pom.xml`):**
- `org.xerial:sqlite-jdbc:3.46.1.0`: el driver, usado solo por el adaptador.
- `org.slf4j:slf4j-nop:1.7.36` (runtime): silencia una advertencia de logging del driver.
- `maven-shade-plugin`: mete el driver dentro del `.jar`, así `java -jar` funciona sin armar el classpath.

**Archivos nuevos:**
- `core/auth`: `User`, `UserAccount`, `IUserRepository`, `IPasswordHasher`, `Pbkdf2PasswordHasher`, `AuthResult`, `IAuthService`, `AuthService`.
- `core/history`: `RoundRecord`, `IMatchHistoryRepository`, `HistorySummary`, `IHistoryService`, `HistoryService`, `RecordingTournament`.
- `adapters/persistence`: `SqliteDatabase`, `DatabaseException`, `JdbcUserRepository`, `JdbcMatchHistoryRepository`.
- `adapters/console`: `ConsoleLogin`, `HistoryPrinter`.
- `adapters/gui`: `LoginDialog`, `HistoryDialog`.

**Archivos modificados:**
- `Main`: abre la base, pide el inicio de sesión y envuelve el torneo con `RecordingTournament`.
- `TournamentConsoleAdapter`: comando `historial`; el constructor corto sigue sin historial.
- `TournamentWindow`: botón *Mi historial* y usuario en el título.
- `pom.xml`, `README.md`, `generate_uml.py` y el diagrama regenerado (clusters `auth`, `history` y `persistence`; 75 clases, verificado contra el código).

**Tablas:**
```sql
users(username TEXT PRIMARY KEY, password_hash TEXT NOT NULL)
round_results(id INTEGER PK AUTOINCREMENT, username TEXT NOT NULL REFERENCES users(username),
              rival TEXT, round INTEGER, total_rounds INTEGER, outcome TEXT, user_moves INTEGER, played_at TEXT)
```

**Tests nuevos (31):**
- `AuthServiceTest` (7): registrar y entrar; mala contraseña; usuario inexistente; nombre duplicado aunque cambien las mayúsculas; nombres inválidos; contraseña corta; la contraseña no queda en texto plano.
- `Pbkdf2PasswordHasherTest` (4): verifica la buena y rechaza la mala; la sal hace distintos dos hashes de la misma clave; verifica hashes hechos con otras iteraciones; un hash mal formado no lanza error.
- `RecordingTournamentTest` (5): guarda la ronda ganada (con el rival vencido y no el siguiente), la perdida (turno en dos pasos) y las tablas; no guarda rondas en curso ni jugadas inválidas; un torneo completo deja 3 filas y 1 título.
- `HistoryServiceTest` (4): totales por resultado y títulos; cada usuario ve solo lo suyo; orden de más nueva a más vieja con límite; usuario nuevo con historial vacío.
- `SqliteRepositoriesTest` (6, integración con SQLite real en un archivo temporal): guardar y buscar usuario; la base rechaza duplicados; el historial vuelve ordenado; la clave foránea rechaza historial de un usuario inexistente; los datos sobreviven a reabrir la base; un nombre con comillas no rompe el SQL.
- `ConsoleLoginTest` (4): crear cuenta deja la sesión iniciada; entrar con la contraseña correcta; si las contraseñas no coinciden no se crea la cuenta; 3 fallos terminan el inicio de sesión.
- `TournamentConsoleAdapterTest.historyCommand_ShowsTheRoundsSavedWhilePlaying` (1): el comando `historial` muestra la ronda recién ganada.

Total de tests: **134**.

## 14. Agregado posterior: arreglo del inicio de sesión e interfaz gráfica completa

### El problema del inicio de sesión
El código de cuentas funcionaba (lo confirmó un programa que manejó la ventana con clics simulados), pero
tenía dos trampas de uso que hacían parecer que "no dejaba entrar". La base estaba vacía: ninguna cuenta se había creado.
1. **Usuario demasiado restrictivo:** solo aceptaba `[a-z0-9_]`. Un nombre como `Luca Vidal` o un mail se rechazaba
   con un mensaje rojo chico, fácil de no ver.
2. **Enter = "Ingresar":** la primera vez, al escribir usuario y contraseña y apretar Enter, intentaba entrar con una cuenta
   que no existía y respondía "incorrectos", sin explicar que primero había que crearla.

**Arreglo:**
- **Usuario:** acepta de 3 a 30 caracteres: letras (con acentos), números, espacios y `. _ - @`. Sigue sin distinguir mayúsculas.
- **Mensajes distintos:** "Ese usuario no existe…" (`UNKNOWN_USER`) y "Contraseña incorrecta" (`WRONG_PASSWORD`). Para eso, `IAuthService` sumó `isRegistered(username)`.
- **Usuario inexistente:** la ventana pasa sola a *Crear cuenta* con el nombre ya escrito.
- **Usuario ya existente al crear la cuenta:** pasa a *Iniciar sesión*.
- **Contraseña:** se usa exactamente como se escribió, en consola y en ventana. Antes la consola recortaba espacios y la ventana no, así que una cuenta creada en un lado podía no abrir en el otro.
- **Errores visibles:** con `javaw` no hay consola, así que un error se perdía. Ahora `GuiApplication` lo muestra en una ventana.

### Interfaz gráfica (adapters/gui)
| Clase | Qué hace |
|---|---|
| `GuiApplication` | Navegación: inicio de sesión → menú → ventanas → volver al menú → cerrar sesión. Recibe puertos y fábricas; no crea nada del núcleo. |
| `LoginDialog` | Pestañas *Iniciar sesión* / *Crear cuenta*, requisitos a la vista, mensajes claros y Enter según la pestaña. |
| `MainMenuWindow` | Saludo, totales del historial y tarjetas: partida libre, torneo, mi historial, qué incluye. Recibe las acciones (`Actions`) en vez de conocer las otras ventanas. |
| `FeaturesDialog` | "Qué incluye el juego": reglas, modos, IA (estilos y dificultad, armados desde el plantel real), historial y lo no implementado. |
| `GameWindow` (reescrita) | Panel lateral con turno y estado, planilla, capturas y ayuda. Botones Deshacer/Rehacer (Ctrl+Z/Y), Nueva partida y Volver al menú. Aviso de fin de partida. |
| `TournamentWindow` (reescrita) | Escalera de rivales, ficha del rival, estado ("pensando…"), planilla, capturas y aviso por ronda. |
| `BoardPanel` (ampliada) | Resalta la última jugada y el rey en jaque (rojo). Muestra anillo en capturas y punto en casillas libres. Coordenadas centradas. |
| `MoveLog` | Planilla para MOSTRAR (no reemplaza a `CommandHistory`): jugadas en notación (`Cg1-f3`, `e4xf5`, `Dd1-h5+`), capturas por color y última jugada. Sigue a deshacer/rehacer con dos pilas. Sin Swing, testeable. |
| `MoveListPanel`, `CapturedPanel`, `Theme` | Bloques visuales reutilizados por las dos ventanas. |

**Cambios fuera de la interfaz (mínimos, en puertos propios del torneo y de la IA):**
- `ITournament.getRivals()`: la ventana dibuja la escalera completa. Lo implementan `Tournament` y `RecordingTournament`.
- `IPlayStyle.getDescription()`: cada estilo se describe solo ("Busca capturar piezas y dar jaque…"). Un estilo nuevo trae su propia explicación y la interfaz no tiene que conocer los estilos.

**Decisiones:**
- **Planilla y capturas en `MoveLog`, no en el núcleo:** `IGame` no expone su historial y no hace falta. Es información de pantalla, y la interfaz ya sabe qué jugada hizo.
- **Capturas de la IA:** se saca una foto del tablero antes de su jugada, porque después la pieza capturada ya no está.
- **Look & feel del sistema (Windows) y textos largos en HTML con ancho fijo:** así se acomodan en varias líneas en vez de cortarse con el escalado de pantalla.
- **Verificación:** las ventanas no tienen tests automáticos. Se verificaron con un programa que las maneja con clics simulados y saca capturas reales: login con usuario inexistente → crear cuenta → menú → partida hasta jaque → volver → torneo con respuesta de la IA → qué incluye → historial.

**Tests nuevos (8):**
- `AuthServiceTest` (+3): nombres completos y mails son válidos; la contraseña se usa tal cual; `isRegistered`. Se ajustó `invalidUsernames_AreRejected`.
- `MoveLogTest` (5): planilla en formato de dos columnas; capturas por color; deshacer/rehacer y una jugada nueva anula el rehacer; pieza nueva con su letra; planilla vacía.

Total de tests: **142**. UML: 82 clases, verificado contra el código.

## Más rivales y elegir la dificultad (versión web)

**Qué cambió:**
- El plantel pasó de 3 a **12 ajedrecistas históricos**, 4 por dificultad, con los tres estilos en cada nivel
  (`HistoricalPlayerFactory.allPlayers()`):
  - **Fácil:** Tigran Petrosian, Paul Morphy, Max Euwe, Wilhelm Steinitz.
  - **Media:** José Raúl Capablanca, Mikhail Botvinnik, David Bronstein, Vasily Smyslov.
  - **Difícil:** Mikhail Tal, Bobby Fischer, Alexander Alekhine, Boris Spassky.
- En la web, antes de empezar, el usuario **elige desde qué dificultad arranca la escalera**
  (`HistoricalPlayerFactory.createLadder(nivel, random)`). La regla de la escalera se mantiene: la dificultad
  **sube** ronda a ronda. Son `LADDER_ROUNDS = 4` rondas: `RIVALS_PER_LEVEL = 2` rivales del nivel elegido y 2 del
  siguiente; si no hay un nivel más, se completa con el último.
  - Fácil = 2 fáciles + 2 medios. Media = 2 medios + 2 difíciles. Difícil = 4 difíciles.
  - Los rivales de cada nivel se **sortean** entre los 4 del plantel (`Random` inyectado: los tests son deterministas).
- La consola y la ventana Swing siguen con la escalera clásica de 3 rondas (`createRoster()`: Petrosian, Capablanca, Tal).

**Por qué así:**
- **`Tournament` no se modificó.** Elegir la dificultad es elegir con qué rivales se arma el torneo: el adaptador web
  (`WebTournamentSession`) recibe una fábrica `Difficulty -> ITournament` y pide un torneo nuevo en cada `start(nivel)`.
  Es el mismo criterio de siempre: el torneo recibe sus rivales por constructor.
- **El plantel se arma una sola vez:** los estilos no tienen estado, y así el mismo rival es el mismo objeto en
  todas las escaleras (`HistoricalPlayer` es un record y se compara por sus campos).
- **Las descripciones siguen siendo neutrales:** el estilo y la dificultad son una decisión de juego, no una
  afirmación sobre la persona.

**Endpoint:** `POST /api/tournament/start {"level":"MEDIUM"}` (EASY / MEDIUM / HARD; sin `level`, se repite la
última; la primera vez, Fácil). `GET /api/tournament/state` trae `levels` (cada dificultad con la descripción de su
escalera) y `allRivals` (todo el plantel).

**Tests nuevos:** `HistoricalPlayerFactoryTest` (+5: plantel de 12, 4 por nivel con los 3 estilos, escalera que
sube desde el nivel elegido, sin rivales repetidos y con sorteo, mismo sorteo = misma escalera),
`WebTournamentSessionTest` (+4: niveles y plantel antes de empezar, descripciones, empezar en un nivel, repetir el
último) y `WebServerTest` (+1: `start` con nivel y nivel desconocido = 400).
