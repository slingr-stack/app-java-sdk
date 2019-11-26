package io.slingr.api.common;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * <p>Exception that happens in the RestClient
 *
 * <p>Created by lefunes on 05/14/2015
 */
public class RestException extends RuntimeException {
    private static final Logger logger = Logger.getLogger(RestException.class);

    protected RestErrorType error = RestErrorType.GENERIC_ERROR;
    protected Object description = null;
    protected Map<String, Object> details;

    public RestException(RestErrorType error, Object description) {
        this(error, description, null);
    }

    public RestException(RestErrorType error, Exception cause) {
        this(error, cause.getMessage(), cause);
    }

    public RestException(RestErrorType error, Object description, Exception cause) {
        this(error, description, cause, null);
    }

    public RestException(RestErrorType error, Object description, Exception cause, Map<String, Object> details) {
        super(cause);
        this.description = description;
        this.details = details;

        if(description instanceof RestException){
            this.error = ((RestException) description).getError();
            this.description = ((RestException) description).getDescription();
        } else {
            if(error != null) {
                this.error = error;
            }
            if(description instanceof Exception){
                if(StringUtils.isNotBlank(((Exception) description).getMessage())) {
                    this.description = ((Exception) description).getMessage();
                } else {
                    this.description = description.toString();
                }
            }
        }
    }

    public RestErrorType getError() {
        return error;
    }

    public Object getDescription() {
        return description;
    }

    public Json toJson(Json extra) {
        Json json = extra != null ? extra.cloneJson() : Json.map();

        json.set("error", error.toJson());

        Json jsonDescription = null;
        try {
            if (description instanceof String) {
                jsonDescription = Json.fromMap(Json.stringToMap((String) description, false, true));
            } else if (description instanceof CharSequence) {
                jsonDescription = Json.fromMap(Json.stringToMap(description.toString(), false, true));
            } else {
                jsonDescription = Json.fromObject(description);
            }
        } catch (Exception ex){
            logger.trace(String.format("Exception to convert data [%s] to log error", description));
        } finally {
            Object ds = description;
            if(jsonDescription != null && !jsonDescription.isEmpty()){
                ds = jsonDescription;
            }
            json.set("description", ds);
        }

        if (details != null) {
            json.set("details", details);
        }

        return json;
    }

    /**
     * Returns the status code of the response. If the request was not made or timed out, it will return -1.
     *
     * @return the status code of the response or -1 if there is no response
     */
    public int getStatusCode() {
        if (getCause() instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) getCause();
            Response r = wae.getResponse();
            return r.getStatus();
        }
        return -1;
    }

    public Json toJson() {
        return toJson(null);
    }

    public Map<String, Object> toMap(Json extra) {
        return toJson(extra).toMap();
    }

    public Map<String, Object> toMap() {
        return toJson(null).toMap();
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static String getProcessingExceptionMessage(ProcessingException cause) {
        Throwable processingException = cause;
        String cm = processingException.getMessage();
        String lastMessage = cm != null ? cm : "";
        int tries = 10;
        while(tries > 0 && (cm == null || cm.startsWith("org.apache.http")) && processingException.getCause() != null) {
            processingException = processingException.getCause();
            cm = processingException.getMessage();
            if(StringUtils.isNotBlank(cm)){
                lastMessage = cm;
            }
            tries--;
        }
        return lastMessage;
    }
}