// i18n.js — los textos de la página en cada idioma. Es el ÚNICO lugar con textos visibles:
// el HTML los pide con data-i18n (texto), data-i18n-html (texto con <b>), data-i18n-aria (aria-label)
// y data-i18n-title (title); el JS con t('clave', { variables }).
//
// Lo que llega del servidor se traduce por su CÓDIGO (MoveResult, AuthResult, GameStatus, dificultad,
// estilo...), nunca por el texto en español que también manda.
//
// El nombre del juego, el año y el mail salen de brand.js: todos los textos los reciben como
// variables {name}, {year} y {email} (no hace falta pasarlas).
//
// Para agregar un idioma: copiar el bloque `en` de DICTIONARY con otra clave (por ejemplo `pt`),
// traducir los valores, sumarlo a LANGS y a NOTATION, y agregar su botón en index.html (#lang-switch).

import { BRAND } from './brand.js';

const KEY = 'ajedrez.idioma';

export const LANGS = ['es', 'en'];

/** Para fechas y listas: el idioma con su región. */
const LOCALE = { es: 'es-AR', en: 'en-US' };

/** Letra de cada pieza en la planilla (los peones no llevan). */
const NOTATION = {
  es: { king: 'R', queen: 'D', rook: 'T', bishop: 'A', knight: 'C', pawn: '' },
  en: { king: 'K', queen: 'Q', rook: 'R', bishop: 'B', knight: 'N', pawn: '' },
};

/**
 * Un valor puede ser un texto con {variables} o, si depende de una cantidad, { one, other }
 * (se elige con Intl.PluralRules usando la variable count).
 */
const DICTIONARY = {
  es: {
    'app.title': '{name}',
    'brand.tagline': 'Ajedrez, pixel por pixel.',
    'common.eyebrow': 'TPO · Ingeniería de Software',
    'prefs.label': 'Preferencias',
    'prefs.theme.toDark': 'Activar modo nocturno',
    'prefs.theme.toLight': 'Activar modo claro',
    'prefs.lang': 'Idioma',

    'loader.title': 'Preparando {name}…',
    'loader.text': 'Cargando piezas…',
    'loader.progress': 'Cargando piezas… {loaded}/{total}',
    'loader.serverError': 'No se pudo conectar con el servidor. ¿Está corriendo el programa en modo "web"?',

    'home.lede': 'Movés, capturás y deshacés jugadas sobre un tablero hecho a mano. Las reglas las decide el servidor.',
    'home.play': 'Jugar',
    'home.howto': 'Cómo se juega',
    'howto.select': '<b>Clic en una pieza</b> para elegirla: se levanta y se marcan sus destinos legales.',
    'howto.move': '<b>Clic en un destino</b> para mover. Si el servidor rechaza la jugada, te dice por qué.',
    'howto.esc': '<b>Esc</b> suelta la pieza elegida.',
    'howto.undo': '<b>Ctrl+Z</b> deshace y <b>Ctrl+Y</b> rehace (en la partida libre).',
    'howto.camera': '<b>Arrastrá</b> el tablero para girar la cámara y usá la <b>rueda</b> para acercarte.',
    'howto.ok': 'Entendido',

    'login.back': '← Volver al inicio',
    'login.title': '{name}',
    'login.lede': 'Partidas entre dos personas y un torneo escalera contra la computadora. Tus rondas quedan guardadas en tu cuenta.',
    'login.tabs': 'Elegí cómo entrar',
    'login.tabLogin': 'Iniciar sesión',
    'login.tabRegister': 'Crear cuenta',
    'login.username': 'Usuario',
    'login.password': 'Contraseña',
    'login.firstTime': '¿Primera vez? Elegí "Crear cuenta" arriba.',
    'login.submit': 'Entrar',
    'login.usernameHint': 'De 3 a 30 caracteres: letras, números, espacios, . _ - @',
    'login.displayName': '¿Cómo te llamamos?',
    'login.optional': '(opcional)',
    'login.displayNameHint': 'Hasta 30 caracteres. Si lo dejás vacío, usamos tu usuario.',
    'login.passwordHint': 'Al menos 4 caracteres.',
    'login.repeat': 'Repetir contraseña',
    'login.registerSubmit': 'Crear cuenta y entrar',
    'login.error.noUsername': 'Escribí tu usuario para ingresar.',
    'login.error.mismatch': 'Las contraseñas no coinciden. Escribí la misma contraseña otra vez.',
    'login.error.network': 'No se pudo conectar con el servidor. ¿Sigue corriendo el programa?',
    'login.error.retype': '{message} Volvé a escribirla.',

    'auth.INVALID_USERNAME': 'El usuario debe tener entre 3 y 30 caracteres: letras, números, espacios o . _ - @',
    'auth.DISPLAY_NAME_TOO_LONG': 'El nombre puede tener hasta 30 caracteres.',
    'auth.PASSWORD_TOO_SHORT': 'La contraseña debe tener al menos 4 caracteres.',
    'auth.USERNAME_TAKEN': 'Ese usuario ya existe: iniciá sesión con él o elegí otro nombre.',
    'auth.UNKNOWN_USER': 'Ese usuario no existe. Si es tu primera vez, creá una cuenta.',
    'auth.WRONG_PASSWORD': 'Contraseña incorrecta.',

    'menu.eyebrow': '{name}',
    'menu.greeting': 'Hola, {name}',
    'menu.question': '¿Qué querés jugar?',
    'menu.home': 'Inicio',
    'menu.logout': 'Cerrar sesión',
    'menu.free.title': 'Partida libre',
    'menu.free.text': 'Dos personas en la misma computadora. Se marcan las jugadas legales y podés deshacer, rehacer y seguir la planilla.',
    'menu.free.cta': 'Jugar →',
    'menu.tournament.title': 'Torneo escalera',
    'menu.tournament.text': 'Elegís la dificultad y jugás con blancas contra rivales históricos, cada uno más difícil. Ganá todas las rondas para ser campeón.',
    'menu.tournament.entry': 'Entrar al torneo →',
    'menu.tournament.levelsHint': 'Elegís desde qué dificultad empezás: {levels}.',
    'menu.tournament.cta.NOT_STARTED': 'Elegir dificultad →',
    'menu.tournament.cta.IN_PROGRESS': 'Seguir en la ronda {round} ({level}) →',
    'menu.tournament.cta.ELIMINATED': 'Volver a intentarlo →',
    'menu.tournament.cta.CHAMPION': 'Ver tu título →',
    'menu.history.title': 'Mi historial',
    'menu.history.text': 'Tus totales y las últimas rondas del torneo, con rival, resultado y fecha.',
    'menu.history.cta': 'Ver historial →',
    'menu.features.title': 'Qué incluye el juego',
    'menu.features.text': 'Las reglas implementadas, cómo se juega y qué se guarda en tu cuenta.',
    'menu.features.cta': 'Ver detalle →',

    'features.rules': 'Reglas del ajedrez',
    'features.rules.1': 'Tablero de 8×8 con las 6 piezas y su movimiento: peón (avance doble inicial y captura en diagonal), torre, caballo, alfil, dama y rey.',
    'features.rules.2': 'Turnos alternados, capturas y rechazo de cualquier movimiento inválido, con el motivo.',
    'features.rules.3': '<b>Jaque</b> (el rey se marca en rojo), <b>jaque mate</b> y <b>rey ahogado</b> (tablas).',
    'features.rules.4': 'Nunca se permite una jugada que deje a tu propio rey en jaque.',
    'features.rules.5': 'No incluye enroque, captura al paso ni promoción del peón.',
    'features.free': 'Partida libre (dos personas)',
    'features.free.1': 'Clic en una pieza: se levanta y aparecen sus destinos legales (disco = casilla libre, marco = captura; en 2D, punto y anillo).',
    'features.free.2': '<b>Deshacer</b> y <b>rehacer</b> jugadas (también con Ctrl+Z / Ctrl+Y). Esc deselecciona.',
    'features.free.3': 'Planilla de jugadas y la última jugada resaltada. La cámara se gira arrastrando y se acerca con la rueda.',
    'features.tournament': 'Torneo escalera contra la IA',
    'features.tournament.1': 'Elegís desde qué dificultad empezás (fácil, media o difícil) y jugás con blancas, una partida por ronda, con rivales cada vez más difíciles. Los rivales se sortean entre los ajedrecistas históricos de cada nivel:',
    'features.tournament.2': 'Jaque mate a favor: pasás de ronda. En contra: quedás eliminado. Ahogado: se repite la ronda. Si ganás la última, sos <b>campeón</b>.',
    'features.tournament.3': 'La IA solo elige jugadas legales. El <b>estilo</b> decide qué jugada le parece mejor; la <b>dificultad</b> decide cuántas veces se equivoca a propósito.',
    'features.tournament.4': 'En el torneo no se puede deshacer: la IA ya respondió.',
    'features.account': 'Cuenta e historial',
    'features.account.1': 'Cada ronda terminada se guarda en tu cuenta (base de datos SQLite): rival, resultado, jugadas, dificultad y fecha.',
    'features.account.2': '"Mi historial" muestra tus totales y las últimas rondas.',
    'features.account.3': 'Las contraseñas se guardan cifradas con hash (PBKDF2), nunca en texto plano.',
    'features.rival': '{name} ({style})',
    'features.close': 'Cerrar',

    'history.eyebrow': 'Torneo escalera',
    'history.title': 'Mi historial',
    'history.toTournament': 'Ir al torneo',
    'history.toMenu': 'Volver al menú',
    'history.filter': 'Filtrar por dificultad',
    'history.filter.all': 'Todas',
    'history.table': 'Últimas rondas del torneo',
    'history.col.date': 'Fecha',
    'history.col.round': 'Ronda',
    'history.col.rival': 'Rival',
    'history.col.difficulty': 'Dificultad',
    'history.col.result': 'Resultado',
    'history.col.moves': 'Jugadas',
    'history.roundOf': '{round} de {total}',
    'history.loading': 'Cargando…',
    'history.loadError': 'No se pudo cargar el historial. ¿Sigue corriendo el programa?',
    'history.empty': 'Todavía no jugaste ninguna ronda. Entrá al torneo escalera desde el menú: cada ronda que termines aparece acá.',
    'history.emptyFiltered': 'No hay rondas guardadas con dificultad {difficulty}. Las rondas de antes de que se guardara la dificultad aparecen en "Todas".',
    'history.summary.none': 'Sin rondas jugadas.',
    'history.summary': '{rounds}: {wins}, {losses} y {draws}.',
    'history.summary.titles': '{summary} Ganaste {tournaments}.',

    'count.roundsPlayed': { one: '{count} ronda jugada', other: '{count} rondas jugadas' },
    'count.wins': { one: '{count} ganada', other: '{count} ganadas' },
    'count.losses': { one: '{count} perdida', other: '{count} perdidas' },
    'count.draws': { one: '{count} tablas', other: '{count} tablas' },
    'count.titles': { one: '{count} título', other: '{count} títulos' },
    'count.tournaments': { one: '{count} torneo', other: '{count} torneos' },
    'stat.wins': { one: 'ganada', other: 'ganadas' },
    'stat.draws': { one: 'tablas', other: 'tablas' },
    'stat.losses': { one: 'perdida', other: 'perdidas' },
    'stat.titles': { one: 'título', other: 'títulos' },
    'record.none': 'Todavía no jugaste el torneo.',
    'record.text': 'Tu récord en el torneo: {wins} · {draws} · {losses}',
    'record.withTitles': '{record} · {titles}',

    'outcome.WON': 'Ganada',
    'outcome.LOST': 'Perdida',
    'outcome.DRAWN': 'Tablas',
    'outcome.CHAMPION': 'Campeón',
    'outcome.ONGOING': 'En curso',
    'difficulty.EASY': 'Fácil',
    'difficulty.MEDIUM': 'Media',
    'difficulty.HARD': 'Difícil',
    'difficulty.none': '—',
    'style.AGGRESSIVE': 'Agresivo',
    'style.BALANCED': 'Equilibrado',
    'style.DEFENSIVE': 'Defensivo',
    'style.AGGRESSIVE.description': 'Busca capturar piezas y dar jaque, aunque deje piezas expuestas.',
    'style.BALANCED.description': 'Captura cuando es seguro: pesa lo que gana contra lo que arriesga.',
    'style.DEFENSIVE.description': 'Evita dejar piezas atacadas y mantiene piezas cerca de su rey.',
    'tournamentStatus.NOT_STARTED': 'Sin empezar',
    'tournamentStatus.IN_PROGRESS': 'En curso',
    'tournamentStatus.ELIMINATED': 'Eliminado',
    'tournamentStatus.CHAMPION': 'Campeón',

    'color.white': 'blancas',
    'color.black': 'negras',
    'kingColor.white': 'blanco',
    'kingColor.black': 'negro',
    'piece.king': 'rey',
    'piece.queen': 'dama',
    'piece.rook': 'torre',
    'piece.bishop': 'alfil',
    'piece.knight': 'caballo',
    'piece.pawn': 'peón',
    'piece.unknown': 'pieza',

    'game.eyebrow.libre': 'Partida libre',
    'game.eyebrow.torneo': 'Torneo escalera',
    'game.title': '{name}',
    'game.moves': 'Jugadas',
    'game.noMoves': 'Todavía no hay jugadas.',
    'game.lastMove': 'Última: {move}',
    'game.actions': 'Acciones de la partida',
    'game.undo': 'Deshacer',
    'game.undoTitle': 'Deshacer (Ctrl+Z)',
    'game.redo': 'Rehacer',
    'game.redoTitle': 'Rehacer (Ctrl+Y)',
    'game.flip.toBlack': 'Ver desde negras',
    'game.flip.toWhite': 'Ver desde blancas',
    'game.flipTitle': 'Girar el tablero al otro bando',
    'game.view.to2d': 'Ver en 2D',
    'game.view.to3d': 'Ver en 3D',
    'game.viewTitle': 'Cambiar entre el tablero 2D y el 3D',
    'game.reset': 'Reiniciar',
    'game.resetTournament': 'Reiniciar torneo',
    'game.myHistory': 'Mi historial',
    'game.home': 'Inicio',
    'game.homeTitle': 'Volver a la pantalla de inicio (la partida queda guardada)',
    'game.menu': 'Volver al menú',

    'turn.player': 'Turno de las {color}',
    'turn.over': 'Partida terminada',
    'turn.chooseLevel': 'Elegí la dificultad',
    'turn.rival': 'Juega {rival}',
    'turn.yours': 'Tu turno ({color})',
    'turn.thinking': 'Piensa {rival}…',
    'turn.champion': '¡Campeón del torneo!',
    'turn.eliminated': 'Quedaste eliminado',
    'turn.draw': 'Tablas',
    'turn.roundOver': 'Ronda terminada',
    'status.CHECK': '¡Jaque! El rey {color} está amenazado.',
    'status.CHECKMATE': 'Jaque mate',
    'status.STALEMATE': 'Rey ahogado: tablas',

    'banner.mate.title': 'Jaque mate',
    'banner.mate.sub': 'Ganan las {color}.',
    'banner.stalemate.title': 'Tablas',
    'banner.stalemate.sub': 'Rey ahogado: las {color} no tienen jugadas legales.',
    'banner.again': 'Jugar otra',

    'moveResult.OUT_OF_BOARD': 'La casilla está fuera del tablero.',
    'moveResult.EMPTY_SQUARE': 'No hay ninguna pieza en la casilla de origen.',
    'moveResult.OPPONENT_PIECE': 'Esa pieza es del rival: no es tu turno.',
    'moveResult.INVALID_PIECE_MOVE': 'La pieza no puede moverse a esa casilla según sus reglas.',
    'moveResult.LEAVES_KING_IN_CHECK': 'Ese movimiento deja a tu rey en jaque.',
    'moveResult.GAME_OVER': 'La partida ya terminó.',

    'error.server': 'Error {status} del servidor',

    'footer.copyright': '© {year} {name}',
    'footer.project': 'TPO Ingeniería de Software',
    'footer.school': 'UADE',
    'footer.contact': 'Contacto:',
    'footer.mailAria': 'Escribir a {name}',
    'footer.credits': 'Créditos',
    'footer.aria': 'Derechos de autor y contacto',
    'credits.title': 'Créditos',
    'credits.models': 'Piezas 3D modeladas en Blender, texturas pixel art en Krita.',
    'credits.tech': 'Hecho con Java, Javalin y Three.js (licencia MIT).',
    'credits.rights': 'Todos los derechos reservados.',
    'credits.close': 'Cerrar',
    'toast.sessionClosed': 'Se cerró tu sesión. Volvé a iniciar sesión.',
    'toast.no3d': 'No se pudieron cargar los modelos 3D: se juega en 2D.',
    'toast.rivalPiece': 'Esa pieza es de {rival}: jugás con las {color}.',
    'toast.notYourTurn': 'Es el turno de las {color}.',
    'toast.noMoves': 'Esa pieza no tiene movimientos legales.',

    'level.title': 'Elegí la dificultad',
    'level.sub': 'La escalera sube desde el nivel que elijas. Los rivales se sortean entre los ajedrecistas de cada nivel.',
    'level.cancel': 'Cancelar',
    'level.backToMenu': 'Volver al menú',
    'level.description': '{rounds} rondas: {parts}.',
    'level.part': '{count} de dificultad {difficulty}',

    'round.title.WON': 'Ronda ganada',
    'round.title.CHAMPION': '¡Campeón!',
    'round.title.LOST': 'Eliminado',
    'round.title.DRAWN': 'Tablas',
    'round.msg.champion': '¡Jaque mate a {rival}! Ganaste las {total} rondas: sos campeón del torneo.',
    'round.msg.won': '¡Jaque mate! Le ganaste a {rival}.\nPasás a la ronda {next} contra {nextRival}.',
    'round.msg.lost': '¡Jaque mate! {rival} te ganó.\nQuedaste eliminado en la ronda {round}.',
    'round.msg.drawn': 'Rey ahogado: tablas.\nSe repite la ronda {round} contra {rival}.',
    'round.next': 'Ir a la ronda {round}',
    'round.repeat': 'Repetir la ronda',
    'round.newTournament': 'Jugar otro torneo',
    'round.menu': 'Volver al menú',

    'tournament.aria': 'Torneo',
    'tournament.round': 'Ronda {round} de {total} · {level} · {status}',
    'tournament.rivalLabel': 'Tu rival',
    'tournament.rivalMeta': '{style} · {difficulty}',
    'tournament.rivalText': '{description} Juega al azar el {percent} % de las veces.',
    'tournament.thinking': '{name} está pensando',
    'tournament.ladderStep': 'Ronda {round}: {name}, {standing}',
    'standing.won': 'ganada',
    'standing.playing': 'jugando',
    'standing.lost': 'perdida',
    'standing.pending': 'pendiente',

    'board2d.aria': 'Tablero',
    'square.empty': '{square}: vacía',
    'square.piece': '{square}: {piece} de las {color}',
    'square.check': ', en jaque',
    'square.selected': ', elegida',
    'square.capture': ', se puede capturar',
    'square.target': ', destino posible',
  },

  en: {
    'app.title': '{name}',
    'brand.tagline': 'Chess, pixel by pixel.',
    'common.eyebrow': 'TPO · Software Engineering',
    'prefs.label': 'Preferences',
    'prefs.theme.toDark': 'Switch to dark mode',
    'prefs.theme.toLight': 'Switch to light mode',
    'prefs.lang': 'Language',

    'loader.title': 'Setting up {name}…',
    'loader.text': 'Loading pieces…',
    'loader.progress': 'Loading pieces… {loaded}/{total}',
    'loader.serverError': 'Couldn\'t reach the server. Is the program running in "web" mode?',

    'home.lede': 'Move, capture and take back moves on a handmade board. The server decides the rules.',
    'home.play': 'Play',
    'home.howto': 'How to play',
    'howto.select': '<b>Click a piece</b> to pick it: it lifts and its legal destinations are marked.',
    'howto.move': '<b>Click a destination</b> to move. If the server rejects the move, it tells you why.',
    'howto.esc': '<b>Esc</b> drops the selected piece.',
    'howto.undo': '<b>Ctrl+Z</b> undoes and <b>Ctrl+Y</b> redoes (in a free game).',
    'howto.camera': '<b>Drag</b> the board to rotate the camera and use the <b>wheel</b> to zoom in.',
    'howto.ok': 'Got it',

    'login.back': '← Back to start',
    'login.title': '{name}',
    'login.lede': 'Two-player games and a ladder tournament against the computer. Your rounds are saved to your account.',
    'login.tabs': 'Choose how to sign in',
    'login.tabLogin': 'Sign in',
    'login.tabRegister': 'Create account',
    'login.username': 'Username',
    'login.password': 'Password',
    'login.firstTime': 'First time? Choose "Create account" above.',
    'login.submit': 'Sign in',
    'login.usernameHint': '3 to 30 characters: letters, numbers, spaces, . _ - @',
    'login.displayName': 'What should we call you?',
    'login.optional': '(optional)',
    'login.displayNameHint': 'Up to 30 characters. If you leave it empty, we use your username.',
    'login.passwordHint': 'At least 4 characters.',
    'login.repeat': 'Repeat password',
    'login.registerSubmit': 'Create account and sign in',
    'login.error.noUsername': 'Type your username to sign in.',
    'login.error.mismatch': 'The passwords don\'t match. Type the same password again.',
    'login.error.network': 'Couldn\'t reach the server. Is the program still running?',
    'login.error.retype': '{message} Type it again.',

    'auth.INVALID_USERNAME': 'The username must have 3 to 30 characters: letters, numbers, spaces or . _ - @',
    'auth.DISPLAY_NAME_TOO_LONG': 'The name can have up to 30 characters.',
    'auth.PASSWORD_TOO_SHORT': 'The password must have at least 4 characters.',
    'auth.USERNAME_TAKEN': 'That username already exists: sign in with it or choose another one.',
    'auth.UNKNOWN_USER': 'That user doesn\'t exist. If it\'s your first time, create an account.',
    'auth.WRONG_PASSWORD': 'Wrong password.',

    'menu.eyebrow': '{name}',
    'menu.greeting': 'Hi, {name}',
    'menu.question': 'What do you want to play?',
    'menu.home': 'Home',
    'menu.logout': 'Sign out',
    'menu.free.title': 'Free game',
    'menu.free.text': 'Two people on the same computer. Legal moves are marked and you can undo, redo and follow the scoresheet.',
    'menu.free.cta': 'Play →',
    'menu.tournament.title': 'Ladder tournament',
    'menu.tournament.text': 'Choose the difficulty and play White against historical rivals, each one harder. Win every round to become champion.',
    'menu.tournament.entry': 'Enter the tournament →',
    'menu.tournament.levelsHint': 'You choose the difficulty you start from: {levels}.',
    'menu.tournament.cta.NOT_STARTED': 'Choose difficulty →',
    'menu.tournament.cta.IN_PROGRESS': 'Continue round {round} ({level}) →',
    'menu.tournament.cta.ELIMINATED': 'Try again →',
    'menu.tournament.cta.CHAMPION': 'See your title →',
    'menu.history.title': 'My history',
    'menu.history.text': 'Your totals and latest tournament rounds, with opponent, result and date.',
    'menu.history.cta': 'View history →',
    'menu.features.title': 'What\'s in the game',
    'menu.features.text': 'The rules implemented, how to play and what is saved to your account.',
    'menu.features.cta': 'See details →',

    'features.rules': 'Chess rules',
    'features.rules.1': '8×8 board with all 6 pieces and their moves: pawn (initial double step and diagonal capture), rook, knight, bishop, queen and king.',
    'features.rules.2': 'Alternating turns, captures and rejection of any invalid move, with the reason.',
    'features.rules.3': '<b>Check</b> (the king is marked in red), <b>checkmate</b> and <b>stalemate</b> (draw).',
    'features.rules.4': 'A move that leaves your own king in check is never allowed.',
    'features.rules.5': 'Castling, en passant and pawn promotion are not included.',
    'features.free': 'Free game (two players)',
    'features.free.1': 'Click a piece: it lifts and its legal destinations appear (disc = empty square, frame = capture; in 2D, dot and ring).',
    'features.free.2': '<b>Undo</b> and <b>redo</b> moves (also with Ctrl+Z / Ctrl+Y). Esc deselects.',
    'features.free.3': 'Scoresheet with the last move highlighted. Drag to rotate the camera and use the wheel to zoom in.',
    'features.tournament': 'Ladder tournament against the AI',
    'features.tournament.1': 'You choose the starting difficulty (easy, medium or hard) and play White, one game per round, against ever harder rivals. Rivals are drawn from the historical players of each level:',
    'features.tournament.2': 'Checkmate in your favor: you move up a round. Against you: you are eliminated. Stalemate: the round is replayed. Win the last one and you are <b>champion</b>.',
    'features.tournament.3': 'The AI only plays legal moves. The <b>style</b> decides which move it likes best; the <b>difficulty</b> decides how often it blunders on purpose.',
    'features.tournament.4': 'There is no undo in the tournament: the AI has already replied.',
    'features.account': 'Account and history',
    'features.account.1': 'Every finished round is saved to your account (SQLite database): opponent, result, moves, difficulty and date.',
    'features.account.2': '"My history" shows your totals and latest rounds.',
    'features.account.3': 'Passwords are stored as a hash (PBKDF2), never in plain text.',
    'features.rival': '{name} ({style})',
    'features.close': 'Close',

    'history.eyebrow': 'Ladder tournament',
    'history.title': 'My history',
    'history.toTournament': 'Go to the tournament',
    'history.toMenu': 'Back to menu',
    'history.filter': 'Filter by difficulty',
    'history.filter.all': 'All',
    'history.table': 'Latest tournament rounds',
    'history.col.date': 'Date',
    'history.col.round': 'Round',
    'history.col.rival': 'Opponent',
    'history.col.difficulty': 'Difficulty',
    'history.col.result': 'Result',
    'history.col.moves': 'Moves',
    'history.roundOf': '{round} of {total}',
    'history.loading': 'Loading…',
    'history.loadError': 'Couldn\'t load the history. Is the program still running?',
    'history.empty': 'You haven\'t played any rounds yet. Enter the ladder tournament from the menu: every round you finish shows up here.',
    'history.emptyFiltered': 'There are no saved rounds on {difficulty}. Rounds from before the difficulty was saved show up under "All".',
    'history.summary.none': 'No rounds played.',
    'history.summary': '{rounds}: {wins}, {losses} and {draws}.',
    'history.summary.titles': '{summary} You won {tournaments}.',

    'count.roundsPlayed': { one: '{count} round played', other: '{count} rounds played' },
    'count.wins': { one: '{count} win', other: '{count} wins' },
    'count.losses': { one: '{count} loss', other: '{count} losses' },
    'count.draws': { one: '{count} draw', other: '{count} draws' },
    'count.titles': { one: '{count} title', other: '{count} titles' },
    'count.tournaments': { one: '{count} tournament', other: '{count} tournaments' },
    'stat.wins': { one: 'win', other: 'wins' },
    'stat.draws': { one: 'draw', other: 'draws' },
    'stat.losses': { one: 'loss', other: 'losses' },
    'stat.titles': { one: 'title', other: 'titles' },
    'record.none': 'You haven\'t played the tournament yet.',
    'record.text': 'Your tournament record: {wins} · {draws} · {losses}',
    'record.withTitles': '{record} · {titles}',

    'outcome.WON': 'Won',
    'outcome.LOST': 'Lost',
    'outcome.DRAWN': 'Draw',
    'outcome.CHAMPION': 'Champion',
    'outcome.ONGOING': 'In progress',
    'difficulty.EASY': 'Easy',
    'difficulty.MEDIUM': 'Medium',
    'difficulty.HARD': 'Hard',
    'difficulty.none': '—',
    'style.AGGRESSIVE': 'Aggressive',
    'style.BALANCED': 'Balanced',
    'style.DEFENSIVE': 'Defensive',
    'style.AGGRESSIVE.description': 'Goes for captures and checks, even if it leaves pieces exposed.',
    'style.BALANCED.description': 'Captures when it is safe: weighs what it wins against what it risks.',
    'style.DEFENSIVE.description': 'Avoids leaving pieces under attack and keeps pieces close to its king.',
    'tournamentStatus.NOT_STARTED': 'Not started',
    'tournamentStatus.IN_PROGRESS': 'In progress',
    'tournamentStatus.ELIMINATED': 'Eliminated',
    'tournamentStatus.CHAMPION': 'Champion',

    'color.white': 'White',
    'color.black': 'Black',
    'kingColor.white': 'white',
    'kingColor.black': 'black',
    'piece.king': 'king',
    'piece.queen': 'queen',
    'piece.rook': 'rook',
    'piece.bishop': 'bishop',
    'piece.knight': 'knight',
    'piece.pawn': 'pawn',
    'piece.unknown': 'piece',

    'game.eyebrow.libre': 'Free game',
    'game.eyebrow.torneo': 'Ladder tournament',
    'game.title': '{name}',
    'game.moves': 'Moves',
    'game.noMoves': 'No moves yet.',
    'game.lastMove': 'Last: {move}',
    'game.actions': 'Game actions',
    'game.undo': 'Undo',
    'game.undoTitle': 'Undo (Ctrl+Z)',
    'game.redo': 'Redo',
    'game.redoTitle': 'Redo (Ctrl+Y)',
    'game.flip.toBlack': 'View from Black',
    'game.flip.toWhite': 'View from White',
    'game.flipTitle': 'Turn the board to the other side',
    'game.view.to2d': '2D view',
    'game.view.to3d': '3D view',
    'game.viewTitle': 'Switch between the 2D and the 3D board',
    'game.reset': 'Restart',
    'game.resetTournament': 'Restart tournament',
    'game.myHistory': 'My history',
    'game.home': 'Home',
    'game.homeTitle': 'Back to the start screen (the game is kept)',
    'game.menu': 'Back to menu',

    'turn.player': '{color} to move',
    'turn.over': 'Game over',
    'turn.chooseLevel': 'Choose the difficulty',
    'turn.rival': '{rival} to move',
    'turn.yours': 'Your move ({color})',
    'turn.thinking': '{rival} is thinking…',
    'turn.champion': 'Tournament champion!',
    'turn.eliminated': 'You were eliminated',
    'turn.draw': 'Draw',
    'turn.roundOver': 'Round over',
    'status.CHECK': 'Check! The {color} king is under attack.',
    'status.CHECKMATE': 'Checkmate',
    'status.STALEMATE': 'Stalemate: draw',

    'banner.mate.title': 'Checkmate',
    'banner.mate.sub': '{color} wins.',
    'banner.stalemate.title': 'Draw',
    'banner.stalemate.sub': 'Stalemate: {color} has no legal moves.',
    'banner.again': 'Play again',

    'moveResult.OUT_OF_BOARD': 'That square is off the board.',
    'moveResult.EMPTY_SQUARE': 'There is no piece on the starting square.',
    'moveResult.OPPONENT_PIECE': 'That piece belongs to your opponent: it\'s not your turn.',
    'moveResult.INVALID_PIECE_MOVE': 'That piece can\'t move to that square by its rules.',
    'moveResult.LEAVES_KING_IN_CHECK': 'That move leaves your king in check.',
    'moveResult.GAME_OVER': 'The game is already over.',

    'error.server': 'Server error {status}',

    'footer.copyright': '© {year} {name}',
    'footer.project': 'Software Engineering project',
    'footer.school': 'UADE',
    'footer.contact': 'Contact:',
    'footer.mailAria': 'Email {name}',
    'footer.credits': 'Credits',
    'footer.aria': 'Copyright and contact',
    'credits.title': 'Credits',
    'credits.models': '3D pieces modeled in Blender, pixel art textures in Krita.',
    'credits.tech': 'Built with Java, Javalin and Three.js (MIT license).',
    'credits.rights': 'All rights reserved.',
    'credits.close': 'Close',
    'toast.sessionClosed': 'Your session ended. Please sign in again.',
    'toast.no3d': 'Couldn\'t load the 3D models: playing in 2D.',
    'toast.rivalPiece': 'That piece belongs to {rival}: you play {color}.',
    'toast.notYourTurn': 'It\'s {color}\'s turn.',
    'toast.noMoves': 'That piece has no legal moves.',

    'level.title': 'Choose the difficulty',
    'level.sub': 'The ladder climbs from the level you choose. Rivals are drawn from the players of each level.',
    'level.cancel': 'Cancel',
    'level.backToMenu': 'Back to menu',
    'level.description': '{rounds} rounds: {parts}.',
    'level.part': '{count} {difficulty}',

    'round.title.WON': 'Round won',
    'round.title.CHAMPION': 'Champion!',
    'round.title.LOST': 'Eliminated',
    'round.title.DRAWN': 'Draw',
    'round.msg.champion': 'Checkmate against {rival}! You won all {total} rounds: you are the tournament champion.',
    'round.msg.won': 'Checkmate! You beat {rival}.\nYou move on to round {next} against {nextRival}.',
    'round.msg.lost': 'Checkmate! {rival} beat you.\nYou were eliminated in round {round}.',
    'round.msg.drawn': 'Stalemate: draw.\nRound {round} against {rival} is replayed.',
    'round.next': 'Go to round {round}',
    'round.repeat': 'Replay the round',
    'round.newTournament': 'Play another tournament',
    'round.menu': 'Back to menu',

    'tournament.aria': 'Tournament',
    'tournament.round': 'Round {round} of {total} · {level} · {status}',
    'tournament.rivalLabel': 'Your opponent',
    'tournament.rivalMeta': '{style} · {difficulty}',
    'tournament.rivalText': '{description} Plays a random move {percent}% of the time.',
    'tournament.thinking': '{name} is thinking',
    'tournament.ladderStep': 'Round {round}: {name}, {standing}',
    'standing.won': 'won',
    'standing.playing': 'playing',
    'standing.lost': 'lost',
    'standing.pending': 'pending',

    'board2d.aria': 'Board',
    'square.empty': '{square}: empty',
    'square.piece': '{square}: {color} {piece}',
    'square.check': ', in check',
    'square.selected': ', selected',
    'square.capture': ', can be captured',
    'square.target': ', possible destination',
  },
};

const listeners = new Set();
let lang = LANGS.includes(document.documentElement.lang) ? document.documentElement.lang : detect();

/** 'es' | 'en' */
export function getLang() {
  return lang;
}

/** Cambia el idioma, lo recuerda, re-traduce el HTML y avisa a los módulos (sin recargar). */
export function setLang(next) {
  if (!LANGS.includes(next) || next === lang) return;
  lang = next;
  store(next);
  translateDom();
  for (const fn of listeners) fn(lang);
}

/** fn(idioma) en cada cambio: cada módulo vuelve a dibujar sus textos. Devuelve la función para desuscribirse. */
export function onLangChange(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

/** El texto de esa clave con sus {variables}; si tiene plural, se elige con vars.count. */
export function t(key, vars = {}) {
  vars = { name: BRAND.name, year: BRAND.year, email: BRAND.email, ...vars };
  let entry = DICTIONARY[lang][key] ?? DICTIONARY.es[key];
  if (entry === undefined) {
    console.warn(`[i18n] falta la clave "${key}"`);
    return key;
  }
  if (typeof entry === 'object') {
    entry = entry[new Intl.PluralRules(lang).select(vars.count ?? 0)] ?? entry.other;
  }
  return entry.replace(/\{(\w+)\}/g, (match, name) => (name in vars ? String(vars[name]) : match));
}

/** ¿Hay texto para esa clave? Sirve para códigos del servidor que el front puede no conocer. */
export function has(key) {
  return key in DICTIONARY[lang];
}

/** Traduce todo lo marcado con data-i18n* dentro de root (por defecto, la página entera). */
export function translateDom(root = document) {
  document.documentElement.lang = lang;
  document.title = t('app.title');
  for (const el of root.querySelectorAll('[data-i18n]')) el.textContent = t(el.dataset.i18n);
  // Solo textos del diccionario (no del usuario ni del servidor): es seguro usar innerHTML
  for (const el of root.querySelectorAll('[data-i18n-html]')) el.innerHTML = t(el.dataset.i18nHtml);
  for (const el of root.querySelectorAll('[data-i18n-aria]')) el.setAttribute('aria-label', t(el.dataset.i18nAria));
  for (const el of root.querySelectorAll('[data-i18n-title]')) el.title = t(el.dataset.i18nTitle);
}

/**
 * Una jugada escrita en el idioma actual, a partir de sus datos (no del texto del servidor):
 * "Cg1-f3" en español, "Ng1-f3" en inglés. Una pieza desconocida usa el texto del servidor.
 */
export function moveText(move) {
  if (!move.pieceType || !(move.pieceType in NOTATION[lang])) return move.text;
  return `${NOTATION[lang][move.pieceType]}${move.from}${move.capture ? 'x' : '-'}${move.to}${move.suffix ?? ''}`;
}

/** "22 oct 2026, 10:42" / "Oct 22, 2026, 10:42 AM" desde la fecha ISO del servidor. */
export function formatDate(iso) {
  return new Intl.DateTimeFormat(LOCALE[lang], {
    day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(new Date(iso));
}

/** ["a", "b", "c"] -> "a, b y c" / "a, b, and c". */
export function formatList(items) {
  return new Intl.ListFormat(LOCALE[lang], { type: 'conjunction' }).format(items);
}

/** Si nunca eligió: el idioma del navegador ("es-AR" -> español; cualquier otro -> inglés). */
function detect() {
  try {
    const stored = localStorage.getItem(KEY);
    if (LANGS.includes(stored)) return stored;
  } catch {
    // almacenamiento bloqueado: se usa el del navegador
  }
  return (navigator.language || 'es').toLowerCase().startsWith('es') ? 'es' : 'en';
}

function store(value) {
  try {
    localStorage.setItem(KEY, value);
  } catch {
    // modo privado o almacenamiento bloqueado: no se recuerda, y no pasa nada
  }
}
