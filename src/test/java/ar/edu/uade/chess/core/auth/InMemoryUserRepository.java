package ar.edu.uade.chess.core.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Repositorio de cuentas en memoria, SOLO para tests: el núcleo se prueba sin base de datos. */
public class InMemoryUserRepository implements IUserRepository {

    private final Map<String, UserAccount> accounts = new HashMap<>();

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(accounts.get(username));
    }

    @Override
    public void save(UserAccount account) {
        accounts.put(account.username(), account);
    }
}
