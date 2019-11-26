package io.slingr.api.common;

import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * General helper methods.
 *
 * User: dgaviola
 * Date: 09/04/13
 */
@SuppressWarnings("unchecked")
public class Helper {

    public static boolean equals(Object o1, Object o2) {
        return o1 == o2 || o1 != null && o1.equals(o2);
    }

    public static boolean notEquals(Object o1, Object o2) {
        return !equals(o1, o2);
    }

    public static Double getDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        } else if (o != null) {
            try {
                return Double.parseDouble(o.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static Double getDouble(Object o, Double defaultValue) {
        Double value = getDouble(o);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    public static Integer getInteger(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        } else if (o != null) {
            try {
                return Integer.parseInt(o.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static Long getLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        } else if (o != null) {
            try {
                return Long.parseLong(o.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static Long getLong(Object o, Long defaultValue) {
        Long value = getLong(o);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }


    public static int getIntValue(String s) {
        final Integer i = getInteger(s);
        if (i != null) {
            return i;
        } else {
            return 0;
        }
    }

    public static boolean collectionSameSize(List<?> list1, List<?> list2) {
        if (list1 == null && list2 == null) {
            return true;
        } else if (list1 == null && list2.isEmpty()) {
            return true;
        } else if (list1 != null && list1.isEmpty() && list2 == null) {
            return true;
        } else if (list1 != null && list2 != null && list1.size() == list2.size()) {
            return true;
        }
        return false;
    }

    public static <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2) {
        if (collectionSameSize(list1, list2)) {
            return new HashSet<>(checkList(list1)).equals(new HashSet<>(checkList(list2)));
        }
        return false;
    }

    public static boolean isNotEmpty(Object o) {
        return !isEmpty(o);
    }

    public static boolean isEmpty(Object o) {
        if (o == null) {
            return true;
        }
        if (o instanceof String) {
            return StringUtils.isBlank((String) o);
        }
        if (o instanceof Map) {
            return ((Map) o).isEmpty();
        }
        if (o instanceof Collection) {
            return ((Collection) o).isEmpty();
        }
        if (o.getClass().isArray()) {
            return ((Object[]) o).length > 0;
        }
        return false;
    }

    public static boolean isGreater(Object value1, Object value2) {
        return value1 instanceof Comparable && value2 instanceof Comparable && ((Comparable) value1).compareTo(value2) > 0;
    }

    public static boolean isGreaterOrEquals(Object value1, Object value2) {
        return value1 instanceof Comparable && value2 instanceof Comparable && ((Comparable) value1).compareTo(value2) >= 0;
    }

    public static boolean isLess(Object value1, Object value2) {
        return value1 instanceof Comparable && value2 instanceof Comparable && ((Comparable) value1).compareTo(value2) < 0;
    }

    public static boolean isLessOrEquals(Object value1, Object value2) {
        return value1 instanceof Comparable && value2 instanceof Comparable && ((Comparable) value1).compareTo(value2) <= 0;
    }

    public static boolean between(Object value, Object min, Object max) {
        if (!isEmpty(min) && isEmpty(max)) {
            return Helper.isGreaterOrEquals(value, min);
        } else if (Helper.isEmpty(min) && !isEmpty(max)) {
            return Helper.isLessOrEquals(value, max);
        }
        return isGreaterOrEquals(value, min) && isLessOrEquals(value, max);
    }

    public static Object convertToLong(Object value) {
        return genericConverter(value, value1 -> {
            if (value1 == null) {
                return null;
            } else if (value1 instanceof Long) {
                return value1;
            } else {
                try {
                    return Long.valueOf(value1.toString().trim());
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Value [%s] cannot be converted to number", value1));
                }
            }
        });
    }

    public static Object convertToBoolean(Object value) {
        return genericConverter(value, value1 -> {
            if (value1 instanceof Boolean) {
                return value1;
            } else if (value1 instanceof String) {
                String str = ((String) value1).trim();
                if (str.equalsIgnoreCase("null")) {
                    return null;
                }
                if (str.equalsIgnoreCase("yes") || str.equalsIgnoreCase("true") || str.equals("1")) {
                    return true;
                } else if (str.equalsIgnoreCase("no") || str.equalsIgnoreCase("false") || str.equals("0")) {
                    return false;
                } else {
                    throw new RuntimeException(String.format("Value [%s] cannot be converted to boolean", value1));
                }
            }
            return null;
        });
    }

    public static Object convertToString(Object value) {
        return genericConverter(value, value1 -> {
            if (value1 instanceof String) {
                return value1;
            } else if (value1 != null) {
                return value1.toString();
            }
            return null;
        });
    }

    public static Object convertToDateTime(Object value) {
        return genericConverter(value, value1 -> {
            if (value1 instanceof Date) {
                return value1;
            } else if (value1 instanceof String) {
                value1 = ((String) value1).trim();
                try {
                    if (StringUtils.isNumeric((String) value1)) {
                        return new Date(Long.valueOf((String) value1));
                    } else {
                        return DateParserSafe.parse(((String) value1).trim(), GlobalProperty.DEFAULT_DATE_TIME_FORMAT_FILTER);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Value [%s] cannot be converted to date time", value1));
                }
            } else if (value1 instanceof Long) {
                return new Date((Long) value1);
            }
            return value1;
        });
    }

    public static Object convertToDate(Object value) {
        return genericConverter(value, value1 -> {
            if (value1 instanceof Date) {
                return DateParserSafe.format((Date) value1, GlobalProperty.DEFAULT_DATE_FORMAT);
            } else if (value1 instanceof String) {
                value1 = ((String) value1).trim();
                try {
                    if (StringUtils.isNumeric((String) value1)) {
                        Date date = new Date(Long.valueOf((String) value1));
                        return DateParserSafe.format(date, GlobalProperty.DEFAULT_DATE_FORMAT);
                    } else {
                        DateParserSafe.parse(((String) value1).trim(), GlobalProperty.DEFAULT_DATE_FORMAT);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Value [%s] is not a valid date", value1));
                }
                // we just return the orginal string if it passes validations
                return value1;
            } else if (value1 instanceof Long) {
                try {
                    Date date = new Date((Long) value1);
                    return DateParserSafe.format(date, GlobalProperty.DEFAULT_DATE_FORMAT);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Milliseconds [%s] cannot be converted to a date", value1));
                }
            }
            return value1;
        });
    }

    public static Object convertToYearMonth(Object value) {
        return genericConverter(value, value1 -> {
            if (value1 instanceof Date) {
                return DateParserSafe.format((Date) value1, GlobalProperty.DEFAULT_DATE_YEAR_MONTH_FORMAT);
            } else if (value1 instanceof String) {
                value1 = ((String) value1).trim();
                try {
                    if (StringUtils.isNumeric((String) value1)) {
                        Date date = new Date(Long.valueOf((String) value1));
                        return DateParserSafe.format(date, GlobalProperty.DEFAULT_DATE_YEAR_MONTH_FORMAT);
                    } else {
                        DateParserSafe.parse(((String) value1).trim(), GlobalProperty.DEFAULT_DATE_YEAR_MONTH_FORMAT);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Value [%s] is not a valid year-month", value1));
                }
                // we just return the orginal string if it passes validations
                return value1;
            } else if (value1 instanceof Long) {
                try {
                    Date date = new Date((Long) value1);
                    return DateParserSafe.format(date, GlobalProperty.DEFAULT_DATE_YEAR_MONTH_FORMAT);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Milliseconds [%s] cannot be converted to a year-month", value1));
                }
            }
            return value1;
        });
    }

    public static Object convertToMonthDay(Object value) {
        return genericConverter(value, value1 -> {
            if (value1 instanceof Date) {
                return DateParserSafe.format((Date) value1, GlobalProperty.DEFAULT_DATE_MONTH_DAY_FORMAT);
            } else if (value1 instanceof String) {
                value1 = ((String) value1).trim();
                try {
                    if (StringUtils.isNumeric((String) value1)) {
                        Date date = new Date(Long.valueOf((String) value1));
                        return DateParserSafe.format(date, GlobalProperty.DEFAULT_DATE_MONTH_DAY_FORMAT);
                    } else {
                        DateParserSafe.parse(((String) value1).trim(), GlobalProperty.DEFAULT_DATE_MONTH_DAY_FORMAT);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Value [%s] is not a valid month-day", value1));
                }
                // we just return the orginal string if it passes validations
                return value1;
            } else if (value1 instanceof Long) {
                try {
                    Date date = new Date((Long) value1);
                    return DateParserSafe.format(date, GlobalProperty.DEFAULT_DATE_MONTH_DAY_FORMAT);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Milliseconds [%s] cannot be converted to a month-year", value1));
                }
            }
            return value1;
        });
    }

    public static Object convertToDatabaseDecimal(Object value) {
        return genericConverter(value, value1 -> {
            BigDecimal decimal = null;
            if (value1 instanceof Long) {
                decimal = new BigDecimal((Long) value1);
            } else if (value1 instanceof Integer) {
                decimal = new BigDecimal((Integer) value1);
            } else if (value1 instanceof Float || value1 instanceof Double) {
                decimal = new BigDecimal(((Number) value1).doubleValue());
            } else if (value1 instanceof String) {
                value1 = ((String) value1).trim();
                try {
                    decimal = new BigDecimal((String) value1);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Value [%s] cannot be converted to number", value1));
                }
            }
            return decimal;
        });
    }

    public static Object convertToTimeDuration(Object value) {
        return genericConverter(value, value1 -> {
            if (value1 instanceof String) {
                value1 = ((String) value1).trim();
                try {
                    if (StringUtils.isNumeric((String) value1)) {
                        return Long.valueOf((String) value1);
                    } else {
                        return TimeUtils.parseDuration((String) value1);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Value [%s] cannot be converted to time duration", value1));
                }
            } else if (value1 instanceof Long) {
                return value1;
            } else if (value1 != null) {
                throw new RuntimeException(String.format("Value [%s] cannot be converted to time duration", value1));
            }
            return null;
        });
    }

    public static Object convertToTime(Object value) {
        return genericConverter(value, value1 -> {
            if (value1 instanceof String) {
                value1 = ((String) value1).trim();
                String time = (String) value1;
                if (time.length() != 5 || StringUtils.split(time, ":").length != 2) {
                    throw new RuntimeException(String.format("Value [%s] cannot be converted to time", value1));
                }
                return time;
            } else if (value1 != null) {
                throw new RuntimeException(String.format("Value [%s] cannot be converted to time", value1));
            }
            return null;
        });
    }

    public static Object genericConverter(Object value, Converter converter) {
        if (value == null) {
            return null;
        } else if (value instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object item : (List) value) {
                Object convertedValue = converter.convert(item);
                list.add(convertedValue);
            }
            return list;
        } else {
            return converter.convert(value);
        }
    }

    private interface Converter {
        Object convert(Object value);
    }

    public interface ValueGetter {
        Object getValue();
    }

    public static Object nullIfNpe(ValueGetter valueGetter) {
        try {
            return valueGetter.getValue();
        } catch (NullPointerException npe) {
            return null;
        }
    }

    private static <T> List checkList(List<T> list) {
        if (list == null) {
            return new ArrayList();
        }
        return list;
    }
}
