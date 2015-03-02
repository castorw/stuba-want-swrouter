package net.ctrdn.stuba.want.swrouter.exception;

public class APIRegistryException extends APIException {

    public APIRegistryException(String message) {
        super(message);
    }

    public APIRegistryException(String message, Exception ex) {
        super(message, ex);
    }

}
