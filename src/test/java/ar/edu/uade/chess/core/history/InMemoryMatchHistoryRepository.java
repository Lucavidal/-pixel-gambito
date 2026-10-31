package ar.edu.uade.chess.core.history;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Historial en memoria, SOLO para tests: el núcleo se prueba sin base de datos. */
public class InMemoryMatchHistoryRepository implements IMatchHistoryRepository {

    private final List<RoundRecord> records = new ArrayList<>();

    @Override
    public void save(RoundRecord record) {
        records.add(record);
    }

    @Override
    public List<RoundRecord> findByUsername(String username) {
        List<RoundRecord> result = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0; i--) { // del más nuevo al más viejo
            if (records.get(i).username().equals(username)) {
                result.add(records.get(i));
            }
        }
        result.sort(Comparator.comparing(RoundRecord::playedAt).reversed());
        return result;
    }

    public List<RoundRecord> all() {
        return List.copyOf(records);
    }
}
