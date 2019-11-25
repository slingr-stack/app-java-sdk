package io.slingr.api.common;

import com.idea2.utils.Json;
import com.idea2.utils.ValidationErrors;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Builds an idea2 ws exception based on the http status
 * <p/>
 * User: smoyano
 * Date: 2/6/15
 */
public class WsExceptionFactory {


    public static WsException getWsExceptionByStatus(int status, String response) {
        return getWsExceptionByStatus(HttpStatus.valueOf(status), response);
    }

    public static WsException getWsExceptionByStatus(HttpStatus status, String response) {
          Map<String, Object> responseDetails = Json.stringToMap(response);
          switch (status) {
              case FORBIDDEN:
                  return new AccessForbiddenException(getErrorMessage(responseDetails));
              case SERVICE_UNAVAILABLE:
                  return new ApplicationInvalidStateException(getErrorMessage(responseDetails));
              case BAD_REQUEST:
                  if (responseDetails.get("code").equals("validationErrors")) {
                      ValidationErrors validationErrors = ValidationErrors.getErrorsFromResponse(responseDetails);
                      return new ValidationException(getErrorMessage(responseDetails), validationErrors);
                  } else {
                      return new BadRequestException(getErrorMessage(responseDetails));
                  }
              case NOT_FOUND:
                  return new NotFoundException(getErrorMessage(responseDetails));
              case CONFLICT:
                  return new OptimisticLockingException(getErrorMessage(responseDetails));
              case UNAUTHORIZED:
                  return new UnauthorizedException(getErrorMessage(responseDetails));
              default:
                  return new SystemException(getErrorMessage(responseDetails));
          }
    }

    private static String getErrorMessage(Map<String, Object> responseDetails) {
        if (responseDetails != null && responseDetails.get("message") != null) {
            return responseDetails.get("message").toString();
        }
        return null;
    }

}
