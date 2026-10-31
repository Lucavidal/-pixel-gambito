package ar.edu.uade.chess.core.tournament;

import ar.edu.uade.chess.core.commands.CommandHistory;
import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.rules.CheckDetector;
import ar.edu.uade.chess.core.rules.MoveValidator;
import ar.edu.uade.chess.core.setup.IBoardSetup;

/**
 * Fábrica de partidas a partir de una configuración de tablero.
 * Para jugar el torneo en 10x10 alcanza con pasarle otro IBoardSetup.
 */
public class BoardSetupGameFactory implements IGameFactory {

    private final IBoardSetup setup;
    private final MoveValidator validator;
    private final CheckDetector checkDetector;

    public BoardSetupGameFactory(IBoardSetup setup, MoveValidator validator, CheckDetector checkDetector) {
        this.setup = setup;
        this.validator = validator;
        this.checkDetector = checkDetector;
    }

    @Override
    public IGame create() {
        // Cada partida tiene su propio tablero y su propio historial.
        return new Game(setup.createBoard(), validator, checkDetector, new CommandHistory());
    }
}
