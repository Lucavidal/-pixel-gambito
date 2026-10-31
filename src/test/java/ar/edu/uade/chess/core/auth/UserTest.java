package ar.edu.uade.chess.core.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El nombre con el que se saluda al usuario: un solo cálculo para todas las pantallas. */
class UserTest {

    @Test
    void withDisplayName_UsesIt() {
        assertEquals("Luca", new User("lucavidal26@gmail.com", "Luca").visibleName());
    }

    @Test
    void withoutDisplayName_UsesThePartBeforeTheAt_Capitalized() {
        assertEquals("Lucavidal26", new User("lucavidal26@gmail.com").visibleName());
        assertEquals("Lucavidal26", new User("lucavidal26@gmail.com", null).visibleName());
    }

    @Test
    void usernameThatIsNotAMail_IsUsedWhole_Capitalized() {
        assertEquals("Luca", new User("luca").visibleName());
        assertEquals("Ana maría", new User("ana maría").visibleName(), "solo la primera letra");
        assertEquals("Ñandú", new User("ñandú").visibleName());
    }

    @Test
    void blankOrSpacedDisplayName_IsIgnoredOrTrimmed() {
        assertEquals("Lucavidal26", new User("lucavidal26@gmail.com", "").visibleName());
        assertEquals("Lucavidal26", new User("lucavidal26@gmail.com", "   ").visibleName());
        assertNull(new User("luca", "  ").displayName(), "vacío es lo mismo que no haber elegido nombre");
        assertEquals("Luca V", new User("luca", "  Luca V  ").visibleName());
    }

    @Test
    void mailWithNothingBeforeTheAt_FallsBackToTheWholeUsername() {
        assertEquals("@raro", new User("@raro").visibleName());
    }

    @Test
    void neverShowsTheFullMail() {
        assertFalse(new User("lucavidal26@gmail.com").visibleName().contains("@"));
    }
}
