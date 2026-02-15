package com.kamiplugins.kamikeys.utils;

public class ConsoleColorUtils {

    // ANSI colors
    public static final String RESET = "\u001B[0m";

    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bold
    public static final String BOLD = "\u001B[1m";

    // ✅ Windows Terminal / Linux geralmente suportam ANSI
    public static boolean supportsAnsi() {
        String os = System.getProperty("os.name");
        String term = System.getenv("TERM");

        // Se TERM existir (Linux/Mac) já ajuda
        if (term != null && !term.isBlank()) return true;

        // Windows 10+ costuma suportar no terminal novo
        return os.contains("windows");
    }

    public static String colorize(String text) {
        if (!supportsAnsi()) return stripAnsi(text);
        return text + RESET;
    }

    public static String stripAnsi(String text) {
        return text.replaceAll("\\u001B\\[[;\\d]*m", "");
    }
}
