package io.slingr.api.common;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Utilities to handle path expressions. Path expressions are things like:
 *
 * name
 * phoneNumbers[2]
 * address.addressLine1
 * addresses[0].zipCode
 *
 * User: dgaviola
 * Date: 21/04/13
 */
public class PathUtils {
    public static boolean hasIndex(String path) {
        if (path.contains("[")) {
            return true;
        } else {
            return false;
        }
    }

    public static String removeIndex(String path) {
        int index = path.indexOf("[");
        if (index == -1) {
            return path;
        } else {
            return path.substring(0, index);
        }
    }

    public static String removeAllIndexes(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        if (!hasIndex(path)) {
            return path;
        }
        return path.replaceAll("\\[.*?\\]", "");
    }

    public static int getIndex(String path) {
        int start = path.indexOf("[");
        int end = path.indexOf("]");
        String indexString = path.substring(start + 1, end);
        return Integer.parseInt(indexString);
    }

    public static String buildPath(String prefix, String fieldName) {
        if (StringUtils.isBlank(prefix)) {
            return fieldName;
        } else {
            return prefix + "." + fieldName;
        }
    }

    public static String buildPathLabel(String prefix, String fieldName) {
        if (StringUtils.isBlank(prefix)) {
            return fieldName;
        } else {
            return prefix + " > " + fieldName;
        }
    }

    public static String buildPath(String base, int index) {
        return base + "[" + index + "]";
    }

    public static String buildPath(String prefix, String fieldName, int index) {
        String base = buildPath(prefix, fieldName);
        return base + "[" + index + "]";
    }

    public static String getParentPath(String path) {
        String[] parts = StringUtils.split(path, ".");
        if (parts == null || parts.length <= 1) {
            return "";
        } else {
            return StringUtils.join(parts, ".", 0, parts.length - 1);
        }
    }

    public static String getRootPath(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        Stack<String> paths = getExplodedPath(path);
        if (!paths.empty()) {
            return paths.lastElement();
        }
        return null;
    }

    public static Stack<String> getExplodedPath(String path) {
        Stack<String> explodedPath = new Stack<String>();
        String[] splitPath = path.split("\\.");
        for (int i = splitPath.length - 1; i >= 0; i--) {
            explodedPath.push(splitPath[i]);
        }
        return explodedPath;
    }

    public static void appendPath(StringBuilder path, String part) {
        if (path.length() == 0) {
            path.append(part);
        } else {
            path.append(".").append(part);
        }
    }

    public static String mergePath(String path, String fieldName) {
        if (path == null) {
            return fieldName;
        }
        String[] fieldNameParts = StringUtils.split(fieldName, ".");
        String[] pathParts = StringUtils.split(path, ".");
        List<String> res = new ArrayList<>();
        if (fieldNameParts != null) {
            for (int i = 0; i < fieldNameParts.length; i++) {
                if (i >= pathParts.length) {
                    for (int j = i; j < fieldNameParts.length; j++) {
                        res.add(fieldNameParts[j]);
                    }
                    break;
                }
                String fieldNamePart = fieldNameParts[i];
                String pathPartWithoutIndex = PathUtils.removeIndex(pathParts[i]);
                if (fieldNamePart.equals(pathPartWithoutIndex)) {
                    res.add(pathParts[i]);
                } else {
                    for (int j = i; j < fieldNameParts.length; j++) {
                        res.add(fieldNameParts[j]);
                    }
                    break;
                }
            }
        }
        return StringUtils.join(res, ".");
    }

    public static String convertPathToSize(String path, int maxSize) {
        String[] parts = StringUtils.split(path, ".");
        int numberOfParts = parts.length;
        if (numberOfParts > maxSize/3) { //this is first letter + last letter + dot separator
            throw new IllegalArgumentException(String.format("The number of parts of path (%s) cannot be more than a third of max size (%s)", numberOfParts, maxSize/3));
        }
        String newPath = path;
        int numberOfChanged = 0;
        while (numberOfChanged < numberOfParts) {
            parts[parts.length-(1+numberOfChanged)] = replaceAvailableFieldString(parts[parts.length-(1+numberOfChanged)]);
            newPath = String.join(".", parts);
            numberOfChanged++;
        }
        numberOfChanged = 0;
        while (newPath.length() > maxSize && numberOfChanged != numberOfParts) {
            parts[parts.length-(1+numberOfChanged)] = getCamelCaseAcronym(parts[parts.length-(1+numberOfChanged)], false);
            newPath = String.join(".", parts);
            numberOfChanged++;
        }
        if (newPath.length() > maxSize) { //it is still greater than max size, reduce to only first and last letter
            numberOfChanged = 0;
            while (newPath.length() > maxSize && numberOfChanged != numberOfParts) {
                parts[parts.length-(1+numberOfChanged)] = getCamelCaseAcronym(parts[parts.length-(1+numberOfChanged)], true);
                newPath = String.join(".", parts);
                numberOfChanged++;
            }
        }
        return newPath;
    }

    public static String lastPathPart(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        String[] parts = StringUtils.split(path, ".");
        return parts[parts.length - 1];
    }

    private static String getCamelCaseAcronym(String string, boolean onlyFirstAndLast) {
        String newString = "";
        if (!StringUtils.isBlank(string)) {
            char[] chars = string.toCharArray();
            int index = 0;
            if (onlyFirstAndLast) {
                newString = newString + chars[0];
            } else {
                for (char character : chars) {
                    if (index == 0 || Character.isUpperCase(character) || Character.isDigit(character)) {
                        newString = newString + character;
                    }
                    index++;
                }
            }
            //if the acronym has not more than one letter, force to use first and last letter
            if (newString.length() == 1 && string.length() != 1) {
                newString = newString + chars[index-1];
            }
        }
        return newString;
    }

    private static String replaceAvailableFieldString(String string) {
        String newString = "";
        if (!StringUtils.isBlank(string)) {
            newString = StringUtils.replace(string, "availableFields", "avFs");
        }
        return newString;
    }
}
