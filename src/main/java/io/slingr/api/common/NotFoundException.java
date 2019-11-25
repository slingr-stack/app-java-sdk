package io.slingr.api.common;


/**
 * Exception thrown when the requested or referenced resource is not found.
 * <p/>
 * User: dgaviola
 * Date: 1/19/13
 */
public class NotFoundException extends WsException {
    public NotFoundException() {
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Object... params) {
        super(message, params);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getErrorCode() {
        return "notFound";
    }

    @Override
    public String getDefaultErrorMessage() {
        return "exception.message.notFound";
    }

    @Override
    public boolean logError() {
        return false;
    }
}
