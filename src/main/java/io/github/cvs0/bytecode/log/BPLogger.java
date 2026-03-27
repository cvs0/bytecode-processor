package io.github.cvs0.bytecode.log;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * <p>Outputs colored, timestamped messages to {@code System.out} (for {@link Level#DEBUG}
 * and {@link Level#INFO}) or {@code System.err} (for {@link Level#WARN} and {@link Level#ERROR}).
 * ANSI colour support can be toggled globally via {@link #setColorsEnabled(boolean)}.
 *
 * <p>Obtain an instance with {@link #of(Class)} or {@link #of(String)}, then log at the
 * desired level:
 * <pre>{@code
 *   private static final BPLogger LOG = BPLogger.of(MyClass.class);
 *   LOG.info("Loaded %d classes", count);
 * }</pre>
 */
public final class BPLogger {

    /** Supported log levels in ascending severity. */
    public enum Level {
        DEBUG(0, "\u001B[36m",  "DEBUG"),
        INFO (1, "\u001B[32m",  "INFO "),
        WARN (2, "\u001B[33m",  "WARN "),
        ERROR(3, "\u001B[31m",  "ERROR");

        final int priority;
        final String ansi;
        final String label;

        Level(int priority, String ansi, String label) {
            this.priority = priority;
            this.ansi     = ansi;
            this.label    = label;
        }
    }

    // ── ANSI escape helpers ──────────────────────────────────────────────
    private static final String RESET = "\u001B[0m";
    private static final String BOLD  = "\u001B[1m";
    private static final String DIM   = "\u001B[2m";

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // ── Global state ─────────────────────────────────────────────────────
    private static volatile Level   globalLevel   = Level.INFO;
    private static volatile boolean colorsEnabled = true;

    // ── Instance state ───────────────────────────────────────────────────
    private final String name;

    private BPLogger(String name) {
        this.name = name;
    }

    /** Create a logger whose tag is the simple name of {@code clazz}. */
    public static BPLogger of(Class<?> clazz) {
        return new BPLogger(clazz.getSimpleName());
    }

    /** Create a logger with an arbitrary tag. */
    public static BPLogger of(String name) {
        return new BPLogger(name);
    }

    // ── Global configuration ─────────────────────────────────────────────

    /** Set the minimum level that will be emitted (default {@link Level#INFO}). */
    public static void setLevel(Level level) {
        globalLevel = level;
    }

    /** Return the current global log level. */
    public static Level getLevel() {
        return globalLevel;
    }

    /** Enable or disable ANSI colour codes in output. */
    public static void setColorsEnabled(boolean enabled) {
        colorsEnabled = enabled;
    }

    /** Check whether ANSI colours are currently enabled. */
    public static boolean isColorsEnabled() {
        return colorsEnabled;
    }

    // ── Logging API ──────────────────────────────────────────────────────

    public void debug(String msg)                    { log(Level.DEBUG, msg, null); }
    public void debug(String fmt, Object... args)    { log(Level.DEBUG, String.format(fmt, args), null); }

    public void info(String msg)                     { log(Level.INFO, msg, null); }
    public void info(String fmt, Object... args)     { log(Level.INFO, String.format(fmt, args), null); }

    public void warn(String msg)                     { log(Level.WARN, msg, null); }
    public void warn(String msg, Throwable t)        { log(Level.WARN, msg, t); }
    public void warn(String fmt, Object... args)     { log(Level.WARN, String.format(fmt, args), null); }

    public void error(String msg)                    { log(Level.ERROR, msg, null); }
    public void error(String msg, Throwable t)       { log(Level.ERROR, msg, t); }
    public void error(String fmt, Object... args)    { log(Level.ERROR, String.format(fmt, args), null); }

    // ── Core rendering ───────────────────────────────────────────────────

    private void log(Level level, String msg, Throwable t) {
        if (level.priority < globalLevel.priority) {
            return;
        }

        PrintStream out = (level.priority >= Level.WARN.priority) ? System.err : System.out;
        String time = LocalTime.now().format(TIME_FMT);

        if (colorsEnabled) {
            // Example: 17:04:23.456 INFO  JarAnalyzer │ Loaded 42 classes
            out.printf("%s%s%s %s%s%s %s%-14s%s │ %s%n",
                    DIM, time, RESET,
                    level.ansi, level.label, RESET,
                    BOLD, name, RESET,
                    msg);
        } else {
            out.printf("%s %s %-14s │ %s%n", time, level.label, name, msg);
        }

        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            if (colorsEnabled) {
                out.print(DIM + trace + RESET);
            } else {
                out.print(trace);
            }
        }
    }
}
