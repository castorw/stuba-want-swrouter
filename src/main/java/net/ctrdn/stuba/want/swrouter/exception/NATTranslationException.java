package net.ctrdn.stuba.want.swrouter.exception;

public class NATTranslationException extends NATException {

    public NATTranslationException(String message) {
        super(message);
    }

    public NATTranslationException(String message, Throwable suppressed) {
        super(message);
        this.addSuppressed(suppressed);
    }

}
