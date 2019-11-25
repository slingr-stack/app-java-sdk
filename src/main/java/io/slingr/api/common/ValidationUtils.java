package io.slingr.api.common;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common validations.
 * <p/>
 * User: dgaviola
 * Date: 1/28/13
 */
public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return true;
        }
        final String emailPattern = "^([\\w-]+(?:[\\.\\+][\\w-]+)*)@((?:[\\w-]+\\.)*\\w[\\w-]{0,66})\\.([a-z]{2,15}(?:\\.[a-z]{2})?)$";
        Pattern pattern = Pattern.compile(emailPattern);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static boolean isValidPassword(String password) {
        return StringUtils.isBlank(password) || password.length() >= 6;
    }

    /**
     * Validate hexadecimal color with regular expression
     * @param color hexadecimal for validation
     * @return true valid hex, false invalid hex
     */
    public static boolean isValidHexadecimalColor(final String color) {
        final String hexPattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
        Pattern pattern = Pattern.compile(hexPattern);
        Matcher matcher = pattern.matcher(color);
        return matcher.matches();
    }

    /**
     * Checks if the property is present first. If it is not present, it will return false, as it means that it is
     * not empty, it just hasn't been specified.
     * <p/>
     * On the other hand, if the property is present, but its value is null or empty string (including only spaces),
     * it will return true,
     *
     * @param map  map where the property should be
     * @param prop name of the property in the map
     * @return true if the property is present and empty; false otherwise
     */
    public static boolean isEmptyProperty(Map<String, Object> map, String prop) {
        if (!map.containsKey(prop)) {
            return true;
        }
        Object value = map.get(prop);
        return value == null || value instanceof String && StringUtils.isBlank((String) value);
    }

    public static boolean isEmpty(Object val) {
        if (val == null) {
            return true;
        }
        if (val instanceof String && StringUtils.isBlank((String) val)) {
            return true;
        }
        if (val instanceof Collection && ((Collection) val).isEmpty()) {
            return true;
        }
        if (val instanceof Map && ((Map) val).isEmpty()) {
            return true;
        }
        if (val instanceof Json) {
            Json json = ((Json) val);
            if (json.isMap()) {
                return json.toMap().isEmpty();
            } else {
                return json.toList().isEmpty();
            }
        }
        return false;
    }

    public static boolean isValidURL(String url) {
        String[] schemes = {"http","https"};
        UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
        return urlValidator.isValid(url);
    }

    public static boolean isValidIP(String ip) {
        return InetAddressValidator.getInstance().isValid(ip);
    }


    public static boolean isValidDomainName(String name) {
        return name == null || name.matches("^[a-z\\d\\.]+$");
    }

    public static boolean isValidDomainNamePart(String name) {
        return name == null || name.matches("^[a-z\\d]+$");
    }

    public static boolean isValidKubernetesServiceName(String name) {
        return name == null || name.matches("^[a-z][a-z\\d]+$");
    }

    public static boolean isValidCodeName(String name) {
        return name == null || name.matches("^[A-Za-z\\d_]+$");
    }


    public static boolean isValidCodeNameWithDots(String name) {
        return name == null || name.matches("^[A-Za-z\\d_\\.]+$");
    }
}
