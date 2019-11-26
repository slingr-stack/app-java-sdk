package io.slingr.api.common;

import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This object contains validation errors that can be returned
 * <p/>
 * User: dgaviola
 * Date: 1/26/13
 */
@SuppressWarnings("unchecked")
public class ValidationErrors {
    private String currentPath;
    private String pathLabel;
    private List<ValidationError> errors;

    public ValidationErrors() {
        errors = new ArrayList<>();
        currentPath = "";
    }

    public static ValidationErrors getErrorsFromResponse(Map<String, Object> response) {
        ValidationErrors validationErrors = new ValidationErrors();
        if (response != null) {
            List<Map<String, Object>> errors = (List) response.get("errors");
            if (errors != null) {
                for (Object error : errors) {
                    if (error instanceof Map) {
                        validationErrors.add((String) ((Map) error).get("field"),
                                ValidationErrors.ErrorCode.fromString((String) ((Map) error).get("code")),
                                (String) ((Map) error).get("message"));
                    } else if (error instanceof String) {
                        validationErrors.add(ErrorCode.INVALID, (String) error);
                    }
                }
            }
        }
        return validationErrors;
    }

    public void add(ErrorCode errorCode, String message, Object... params) {
        add(errorCode, message, null, params);
    }

    public void add(String path, ErrorCode errorCode, String message, Object... params) {
        add(path, errorCode, message, null, params);
    }

    public void addInvalid(Object value, String message, Object... params) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("rejectedValue", value.toString());
        add(ErrorCode.INVALID, message, additionalInfo, params);
    }

    public void add(ErrorCode errorCode, String message, Map<String, Object> additionalInfo, Object... params) {
        errors.add(new ValidationError(currentPath, pathLabel, errorCode, message, additionalInfo, params));
    }

    public void add(String path, ErrorCode errorCode, String message, Map<String, Object> additionalInfo, Object... params) {
        String pathToAdd = currentPath;
        if (pathToAdd.isEmpty()) {
            pathToAdd = path;
        } else {
            pathToAdd = currentPath + "." + path;
        }
        errors.add(new ValidationError(pathToAdd, pathLabel, errorCode, message, additionalInfo, params));
    }

    public void push(String path) {
        this.push(path, null);
    }

    public void push(String path, String pathLabel) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        if (currentPath.isEmpty()) {
            currentPath = path;
        } else {
            currentPath += "." + path;
        }
        this.pathLabel = pathLabel;
    }

    public void pushList(String path) {
        this.pushList(path, null);
    }

    public void pushList(String path, String pathLabel) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        if (currentPath.isEmpty()) {
            currentPath = path + "[0]";
        } else {
            currentPath += "." + path + "[0]";
        }
        this.pathLabel = pathLabel;
    }

    public void next() {
        Integer lastIndex = getLastIndex();
        if (lastIndex == null) {
            throw new IllegalStateException("Cannot call next when there is no list");
        }
        replaceLastIndex(lastIndex + 1);
    }

    public void pop() {
        currentPath = removeLastPart();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public int errorsSize() {
        return errors.size();
    }

    public ValidationError getError(int index) {
        return errors.get(index);
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void addErrors(ValidationErrors errorsToAdd, String prefix) {
        if (errorsToAdd == null || !errorsToAdd.hasErrors()) {
            return;
        }
        for (ValidationError error : errorsToAdd.getErrors()) {
            add(prefix + error.getField(), ErrorCode.fromString(error.getCode()), error.getMessage(), error.getAdditionalInfo(), error.getParams());
        }
    }

    public void addErrors(ValidationErrors errorsToAdd) {
        addErrors(errorsToAdd, false);
    }

    public void addErrors(ValidationErrors errorsToAdd, boolean maintainPath) {
        if (errorsToAdd == null || !errorsToAdd.hasErrors()) {
            return;
        }
        String previousPath = "";
        if (!maintainPath) {
            previousPath = currentPath;
            currentPath = "";
        }
        for (ValidationError error : errorsToAdd.getErrors()) {
            if (StringUtils.isNotBlank(error.getField())) {
                add(error.getField(), ErrorCode.fromString(error.getCode()), error.getMessage(), error.getAdditionalInfo(), error.getParams());
            } else {
                add(ErrorCode.fromString(error.getCode()), error.getMessage(), error.getAdditionalInfo(), error.getParams());
            }
        }
        if (!maintainPath) {
            currentPath = previousPath;
        }
    }

    private String removeLastPart() {
        int index = currentPath.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return currentPath.substring(0, index);
    }

    private Integer getLastIndex() {
        int openIndex = currentPath.lastIndexOf("[");
        int closeIndex = currentPath.lastIndexOf("]");
        if (openIndex == -1 || closeIndex == -1) {
            return null;
        }
        return Integer.parseInt(currentPath.substring(openIndex + 1, closeIndex));
    }

    private void replaceLastIndex(int newIndex) {
        int openIndex = currentPath.lastIndexOf("[");
        int closeIndex = currentPath.lastIndexOf("]");
        currentPath = currentPath.substring(0, openIndex + 1) + newIndex + currentPath.substring(closeIndex);
    }

    @Override
    public String toString() {
        if (errors == null) {
            return "No errors";
        }
        return StringUtils.join(errors.stream().map(ValidationError::toString).collect(Collectors.toList()), ", ");
    }

    public Json toJson() {
        return Json.fromList(errors.stream().map(error -> error.toJson()).collect(Collectors.toList()));
    }

    public enum ErrorCode {
        REQUIRED("required"),
        INVALID_STRUCTURE("invalidStructure"),
        NOT_FOUND("notFound"),
        INVALID("invalid"),
        INVALID_EMAIL("invalidEmail"),
        UNIQUE("unique"),
        NOT_UNIQUE("notUnique"),
        ALLOWED_OPERATIONS("allowedOperations"),
        GENERAL("general");

        private String code;

        ErrorCode(String code) {
            this.code = code;
        }

        public static ErrorCode fromString(String errorCode) {
            for (ErrorCode tmpErrorCode : values()) {
                if (tmpErrorCode.getCode().equals(errorCode)) {
                    return tmpErrorCode;
                }
            }
            return null;
        }

        public String getCode() {
            return code;
        }
    }

    public static class ValidationError {
        private String field;
        private String fieldLabel;
        private ErrorCode code;
        private String message;
        private Map<String, Object> additionalInfo;
        private Object[] params;

        public ValidationError(String field, String fieldLabel, ErrorCode code, String message, Map<String, Object> additionalInfo, Object... params) {
            this.field = field;
            this.fieldLabel = fieldLabel;
            this.code = code;
            this.message = message;
            this.additionalInfo = additionalInfo;
            this.params = params;
        }

        public String getCode() {
            return this.code != null ? this.code.getCode() : null;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getFieldLabel() {
            return fieldLabel;
        }

        public void setFieldLabel(String fieldLabel) {
            this.fieldLabel = fieldLabel;
        }

        public Map<String, Object> getAdditionalInfo() {
            return additionalInfo;
        }

        public void setAdditionalInfo(Map<String, Object> additionalInfo) {
            this.additionalInfo = additionalInfo;
        }

        public Object[] getParams() {
            return this.params;
        }

        public void setParams(Object[] params) {
            this.params = params;
        }

        public boolean hasParams() {
            return this.params != null && this.params.length > 0;
        }

        @Override
        public String toString() {
            return StringUtils.isNotBlank(this.field)
                    ? this.code + " > " + this.field + ": " + (this.hasParams() ? String.format(this.message, this.params) : this.message)
                    : this.code + ": " + (this.hasParams() ? String.format(this.message, this.params) : this.message);
        }

        public Json toJson() {
            Json json = Json.map();
            if (code != null) {
                json.set("code", code);
            }
            if (!StringUtils.isBlank(field)) {
                json.set("field", field);
            }
            if (!StringUtils.isBlank(fieldLabel)) {
                json.set("fieldLabel", fieldLabel);
            }
            if (!StringUtils.isBlank(message)) {
                json.set("message", message);
            }
            if (additionalInfo != null) {
                json.set("additionalInfo", Json.fromMap(additionalInfo));
            }
            return json;
        }
    }
}
