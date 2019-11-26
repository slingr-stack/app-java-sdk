package io.slingr.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * This class help provide thread-safe date/time parsing.
 * The Java default DateTimeFormat is not thread safe.
 *
 */
public class DateParserSafe {

    private static final Logger logger = LoggerFactory.getLogger(DateParserSafe.class);

    private static final ThreadLocal<Map<String, DateFormat>> PARSERS = new ThreadLocal<Map<String, DateFormat>>() {
        protected Map<String, DateFormat> initialValue() {
            return new HashMap<>();
        }
    };

    private static final DateFormat getParser(final String pattern, TimeZone tz){
        Map<String, DateFormat> parserMap = PARSERS.get();
        String key = pattern;
        if (tz != null) {
            key += "-"+tz.getID();
        }
        DateFormat df = parserMap.get(key);
        if (df == null){
            df = new SimpleDateFormat(pattern);
            if (tz != null) {
                df.setTimeZone(tz);
            }
            parserMap.put(pattern, df);
        }
        return df;
    }

    /**
     * Static Public and Thread-Safe method to parse a date from the give String
     * @param date input string to parse
     * @param pattern date format pattern of the input string
     * @return Date value of the input string
     * @throws ParseException If parse exception happened
     */
    public static Date parse(final String date, final String pattern) throws ParseException {
        return getParser(pattern, null).parse(date);
    }

    /**
     * Static Public and Thread-Safe method to parse a date from the give String
     * @param date input string to parse
     * @param pattern date format pattern of the input string
     * @param tz time zone used to parse the date
     * @return Date value of the input string
     * @throws ParseException If parse exception happened
     */
    public static Date parse(final String date, final String pattern, TimeZone tz) throws ParseException {
        return getParser(pattern, tz).parse(date);
    }

    /**
     * Static Public and Thread-Safe method to parse a date from the give String
     * and return the long value of the result
     * @param date input string to parse
     * @param pattern date format pattern of the input string
     * @return Long date value of the input string
     * @throws ParseException If parse exception happened
     * @throws ParseException
     */
    public static long parseLongDate(final String date, final String pattern) throws ParseException {
        return parse(date, pattern).getTime();
    }

    /**
     * Static Public and Thread-Safe method to parse a date from the give String
     * and return the long value of the result
     * @param date input string to parse
     * @param pattern date format pattern of the input string
     * @param tz time zone used to parse the date
     * @return Long date value of the input string
     * @throws ParseException If parse exception happened
     * @throws ParseException
     */
    public static long parseLongDate(final String date, final String pattern, TimeZone tz) throws ParseException {
        return parse(date, pattern, tz).getTime();
    }

    /**
     * A thread-safe method to format a given Date based-on the given pattern
     * @param date Date to be formatted
     * @param pattern Pattern used to format the date
     * @return String of formatted date
     */
    public static String format(final Date date, final String pattern){
        return getParser(pattern, null).format(date);
    }

    /**
     * A thread-safe method to format a given Date based-on the given pattern
     * @param date Date to be formatted
     * @param pattern Pattern used to format the date
     * @param tz time zone used to parse the date
     * @return String of formatted date
     */
    public static String format(final Date date, final String pattern, TimeZone tz){
        return getParser(pattern, tz).format(date);
    }

    /**
     * A thread-safe method to format a given Date(in long) based-on the given pattern
     * @param date Date in long to be formatted
     * @param pattern Pattern used to format the date
     * @return String of formatted date
     */
    public static String format(final long date, final String pattern){
        return getParser(pattern, null).format(new Date(date));
    }

    /**
     * A thread-safe method to format a given Date(in long) based-on the given pattern
     * @param date Date in long to be formatted
     * @param pattern Pattern used to format the date
     * @param tz time zone used to parse the date
     * @return String of formatted date
     */
    public static String format(final long date, final String pattern, TimeZone tz){
        return getParser(pattern, tz).format(new Date(date));
    }

}
