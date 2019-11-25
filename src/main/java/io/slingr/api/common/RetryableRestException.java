package io.slingr.api.common;

/**
 * This is an exception when calling a REST web service that can be retried. For example if the
 * server returns a 503 it might means that it is not available at the moment but will be later.
 *
 * Created by dgaviola on 29/5/15.
 */
public class RetryableRestException extends RestException {
    public RetryableRestException(RestErrorType error, Object description) {
        super(error, description);
    }

    public RetryableRestException(RestErrorType error, Object description, Exception cause) {
        super(error, description, cause);
    }
}
