package io.slingr.api.common;

import java.util.Map;

/**
 * This is the opposite of {@link RetryableRestException}. It is for errors that are permanent
 * and the result won't change it we try several times.
 *
 * Created by dgaviola on 29/5/15.
 */
public class PermanentRestException extends RestException {
    public PermanentRestException(RestErrorType error, Object description) {
        super(error, description);
    }

    public PermanentRestException(RestErrorType error, Object description, Exception cause) {
        super(error, description, cause);
    }

    public PermanentRestException(RestErrorType error, Object description, Exception cause, Map<String, Object> details) {
        super(error, description, cause, details);
    }
}
