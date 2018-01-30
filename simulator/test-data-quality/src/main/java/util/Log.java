package util;

/**
 * Created by matthias on 09.11.17.
 */
public class Log {
    public static boolean ENABLE_LOGGING = true;

    public static void error(String message) {
        if (ENABLE_LOGGING)
            System.out.println("ERROR: " + message);
    }

    public static void warn(String message) {
        if (ENABLE_LOGGING)
            System.out.println("WARN: " + message);
    }

    public static void info(String message) {
        if (ENABLE_LOGGING)
            System.out.println("INFO: " + message);
    }
}
