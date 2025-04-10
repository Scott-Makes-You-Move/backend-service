package nl.optifit.backendservice.exception;

public class BootstrapException extends RuntimeException {
    public BootstrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
