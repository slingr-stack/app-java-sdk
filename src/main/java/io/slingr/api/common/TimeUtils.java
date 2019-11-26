package io.slingr.api.common;

import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities to handle time.
 * <p/>
 * User: dgaviola
 * Date: 09/04/13
 */
public class TimeUtils {
    private static Pattern patternMonths = Pattern
            .compile("(\\d+)\\s?M(onth)?(onths)?");
    private static Pattern patternDays = Pattern
            .compile("(\\d+)\\s?d(ay)?(ays)?");
    private static Pattern patternHours = Pattern
            .compile("(\\d+)\\s?h(our)?(ours)?");
    private static Pattern patternCapitalHours = Pattern
            .compile("(\\d+)\\s?H(our)?(ours)?");
    private static Pattern patternMinutes = Pattern
            .compile("(\\d+)\\s?m(in)?(ins)?(inute)?(inutes)?");
    private static Pattern patternSeconds = Pattern
            .compile("(\\d+)\\s?s(ec)?(ecs)?(econd)?(econds)?");

    /**
     * Parses a duration string of the form "98d 01h 23m 45s" into milliseconds.
     */
    public static long parseDuration(String duration) throws ParseException {
        Matcher matcherMonths = patternMonths.matcher(duration);
        Matcher matcherDays = patternDays.matcher(duration);
        Matcher matcherHours = patternHours.matcher(duration);
        Matcher matcherCapHours = patternCapitalHours.matcher(duration);
        Matcher matcherMinutes = patternMinutes.matcher(duration);
        Matcher matcherSeconds = patternSeconds.matcher(duration);

        long secInMillis = 1000;
        long minInMillis = 60 * secInMillis;
        long hourInMillis = 60 * minInMillis;
        long dayInMillis = 24 * hourInMillis;
        long monthInMillis = 30 * dayInMillis;

        long months = 0;
        long days = 0;
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        long totalMillis;

        if (matcherMonths.find()) {
            months = Long.parseLong(matcherMonths.group(1));
        }
        if (matcherDays.find()) {
            days = Long.parseLong(matcherDays.group(1));
        }
        if (matcherHours.find()) {
            hours = Long.parseLong(matcherHours.group(1));
        } else if (matcherCapHours.find()) {
            hours = Long.parseLong(matcherCapHours.group(1));
        }
        if (matcherMinutes.find()) {
            minutes = Long.parseLong(matcherMinutes.group(1));
        }
        if (matcherSeconds.find()) {
            seconds = Long.parseLong(matcherSeconds.group(1));
        }
        totalMillis = months * monthInMillis + days * dayInMillis + hours * hourInMillis + minutes * minInMillis + seconds * secInMillis;

        return totalMillis;
    }

    /**
     * Formats milliseconds to something like "2d 5h 20m 20s"
     *
     * @param duration time duration in milliseconds
     * @return formatted string
     */
    public static String formatDuration(long duration) {
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        duration -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        duration -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        duration -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        List<String> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(days + "d");
        }
        if (hours > 0) {
            parts.add(hours + "h");
        }
        if (minutes > 0) {
            parts.add(minutes + "m");
        }
        if (seconds > 0) {
            parts.add(seconds + "s");
        }
        if (parts.size() > 0) {
            return StringUtils.join(parts, " ");
        } else {
            return "-";
        }
    }
}
