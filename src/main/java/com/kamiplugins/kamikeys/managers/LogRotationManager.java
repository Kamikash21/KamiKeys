package com.kamiplugins.kamikeys.managers;

import com.kamiplugins.kamikeys.Main;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class LogRotationManager {
    private final Main plugin;
    private Timer rotationTimer;

    public LogRotationManager(Main plugin) {
        this.plugin = plugin;
    }

    public void scheduleAutoRotation() {
        if (rotationTimer != null) {
            rotationTimer.cancel();
        }

        rotationTimer = new Timer();
        rotationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                rotateLogs();
            }
        }, getNextMidnight(), 24 * 60 * 60 * 1000); // Rodar a cada 24 horas
    }

    private void rotateLogs() {
        int keepDays = plugin.getConfig().getInt("Logs.KeepDays", -1);

        if (keepDays == -1) {
            return; // Não rotacionar
        }

        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            return;
        }

        File[] logFiles = logsDir.listFiles();
        if (logFiles == null) return;

        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (File logFile : logFiles) {
            if (logFile.isFile() && logFile.getName().endsWith(".log")) {
                String fileName = logFile.getName();
                // Extrair data do nome do arquivo (ex: keys_2023-01-01.log)
                if (fileName.contains("_")) {
                    String[] parts = fileName.split("_");
                    if (parts.length >= 2) {
                        String dateStr = parts[parts.length - 1].replace(".log", "");
                        try {
                            LocalDate fileDate = LocalDate.parse(dateStr, formatter);
                            if (now.isAfter(fileDate.plusDays(keepDays))) {
                                logFile.delete();
                            }
                        } catch (Exception e) {
                            // Arquivo não tem formato de data, não rotacionar
                        }
                    }
                }
            }
        }
    }

    private long getNextMidnight() {
        long now = System.currentTimeMillis();
        long oneDay = 24 * 60 * 60 * 1000;
        return oneDay - (now % oneDay);
    }
}