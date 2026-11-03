package dev.thehale.papermc_plugin_template.music;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class MinBgmLog {

    private MinBgmLog() {
    }

    public static void info(Logger logger, String message) {
        logger.info("[min_bgm INFO] " + message);
    }

    public static void warning(Logger logger, String message) {
        logger.warning("[min_bgm WARNING] " + message);
    }

    public static void severe(Logger logger, String message) {
        logger.severe("[min_bgm SEVERE] " + message);
    }

    public static void severe(Logger logger, String message, Throwable exception) {
        logger.log(Level.SEVERE, "[min_bgm SEVERE] " + message, exception);
    }
}