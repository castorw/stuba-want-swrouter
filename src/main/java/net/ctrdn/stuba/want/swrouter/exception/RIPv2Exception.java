package net.ctrdn.stuba.want.swrouter.exception;

public class RIPv2Exception extends RouterException {

    public RIPv2Exception(String message) {
        super(message);
    }

    public RIPv2Exception(String message, Throwable suppressed) {
        super(message);
        this.addSuppressed(suppressed);
    }

}
