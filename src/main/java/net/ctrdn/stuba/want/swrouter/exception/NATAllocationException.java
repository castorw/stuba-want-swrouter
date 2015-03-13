package net.ctrdn.stuba.want.swrouter.exception;

public class NATAllocationException extends NATException {

    public NATAllocationException(String message) {
        super(message);
    }

    public NATAllocationException(String message, Throwable suppressed) {
        super(message);
        this.addSuppressed(suppressed);
    }

}
