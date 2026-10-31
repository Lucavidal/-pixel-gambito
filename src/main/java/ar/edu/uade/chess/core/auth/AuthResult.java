package ar.edu.uade.chess.core.auth;

/**
 * Resultado de crear una cuenta o iniciar sesión. Un enum (y no excepciones)
 * por el mismo motivo que MoveResult: equivocarse de contraseña es parte normal del uso.
 * Los mensajes son los que ve el usuario, en español.
 */
public enum AuthResult {
    SUCCESS("Listo."),
    INVALID_USERNAME("El usuario debe tener entre " + AuthService.MIN_USERNAME_LENGTH + " y "
            + AuthService.MAX_USERNAME_LENGTH + " caracteres: letras, números, espacios o . _ - @"),
    DISPLAY_NAME_TOO_LONG("El nombre puede tener hasta " + AuthService.MAX_DISPLAY_NAME_LENGTH + " caracteres."),
    PASSWORD_TOO_SHORT("La contraseña debe tener al menos " + AuthService.MIN_PASSWORD_LENGTH + " caracteres."),
    USERNAME_TAKEN("Ese usuario ya existe: iniciá sesión con él o elegí otro nombre."),
    UNKNOWN_USER("Ese usuario no existe. Si es tu primera vez, creá una cuenta."),
    WRONG_PASSWORD("Contraseña incorrecta.");

    private final String message;

    AuthResult(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }

    public boolean isSuccess() { return this == SUCCESS; }
}
