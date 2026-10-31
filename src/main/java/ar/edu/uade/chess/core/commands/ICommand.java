package ar.edu.uade.chess.core.commands;

/**
 * Patrón COMMAND: una acción convertida en objeto, para poder guardarla,
 * deshacerla y rehacerla.
 */
public interface ICommand {

    void execute();

    void undo();
}
