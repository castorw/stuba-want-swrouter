package net.ctrdn.stuba.want.swrouter.exception;

public class NATException extends RouterException {

    public NATException(String message) {
        super(message);
    }

    public NATException(String message, Throwable suppressed) {
        super(message);
        this.addSuppressed(suppressed);
    }

}
