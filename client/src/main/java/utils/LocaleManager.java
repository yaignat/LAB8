package utils;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.*;

public class LocaleManager {
    private static Locale currentLocale = new Locale("ru", "RU");
    private static ResourceBundle bundle;

    static {
        reloadBundle();
    }

    public static void setLocale(String language, String country) {
        currentLocale = new Locale(language, country);
        reloadBundle();
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    private static void reloadBundle() {
        bundle = ResourceBundle.getBundle("messages", currentLocale);
    }

    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    public static String getString(String key, Object... args) {
        return MessageFormat.format(getString(key), args);
    }

    public static NumberFormat getNumberFormat() {
        return NumberFormat.getNumberInstance(currentLocale);
    }

    public static DateFormat getDateFormat() {
        return DateFormat.getDateInstance(DateFormat.MEDIUM, currentLocale);
    }

    public static DateFormat getDateTimeFormat() {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, currentLocale);
    }

    public static List<Locale> getSupportedLocales() {
        return Arrays.asList(
                new Locale("ru", "RU"),
                new Locale("sk", "SK"),
                new Locale("lv", "LV"),
                new Locale("es", "CR")
        );
    }
}