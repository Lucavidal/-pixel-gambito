package ar.edu.uade.chess.core.history;

import java.util.List;

/**
 * PUERTO DE SALIDA: dónde se guarda el historial de rondas jugadas.
 * La implementación real (SQLite) vive en adapters/persistence.
 */
public interface IMatchHistoryRepository {

    void save(RoundRecord record);

    /** Rondas de ese usuario, de la más nueva a la más vieja. */
    List<RoundRecord> findByUsername(String username);
}
