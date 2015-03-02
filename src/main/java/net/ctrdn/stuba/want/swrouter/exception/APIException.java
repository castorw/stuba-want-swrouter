package net.ctrdn.stuba.want.swrouter.exception;

public class APIException extends RouterException {

    public APIException(String message) {
        super(message);
    }

    public APIException(String message, java.lang.Exception ex) {
        super(message);
        this.addSuppressed(ex);
    }

}
