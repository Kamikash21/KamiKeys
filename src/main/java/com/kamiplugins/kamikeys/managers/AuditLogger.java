package com.kamiplugins.kamikeys.managers;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.models.enums.KeyState;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogger {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MM-yyyy");

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final Main plugin;

    public AuditLogger(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * MÉTODO ÚNICO OFICIAL DE AUDITORIA DE KEYS
     */
    public void logKeyEvent(
            AuditAction action,
            String keyCode,
            String tipo,
            String origem,
            KeyState fromState,
            KeyState toState,
            AuditActor actor,
            String ip,
            AuditSource source,
            String motivo
    ) {

        try {
            // ===== SAFETY FALLBACKS =====
            if (keyCode == null) keyCode = "UNKNOWN";
            String fromStateName = (fromState != null) ? fromState.name() : "NONE";
            String toStateName = (toState != null) ? toState.name() : "NONE";
            if (actor == null) actor = AuditActor.system("UNKNOWN");
            if (ip == null || ip.isEmpty()) ip = "SYSTEM";
            if (source == null) source = AuditSource.system("UNKNOWN");
            if (motivo == null || motivo.isEmpty()) motivo = "N/A";

            LocalDateTime now = LocalDateTime.now();
            String timestamp = now.format(DATE_TIME_FORMAT);

            // ===== PATH RESOLUTION =====
            File baseLogs = new File(plugin.getDataFolder(), "logs");
            File monthFolder = new File(baseLogs, now.format(MONTH_FORMAT));

            if (!monthFolder.exists() && !monthFolder.mkdirs()) {
                plugin.getLogger().warning("[AuditLogger] Falha ao criar pasta mensal de logs.");
                return;
            }

            File auditFile = new File(
                    monthFolder,
                    "Audit_" + now.format(DAY_FORMAT) + ".log"
            );

            // ===== LOG BUILD =====
            StringBuilder log = new StringBuilder();
            log.append("[").append(timestamp).append("] ")
                    .append("[").append(action.name()).append("]\n");

            log.append("| Key = ").append(keyCode).append("\n");
            log.append("| Tipo = ").append(tipo).append("\n");
            log.append("| Origem = ").append(origem).append("\n");
            log.append("| Estado = ").append(fromStateName)
                    .append(" -> ").append(toStateName).append("\n");

            log.append("| Por = ").append(actor.format()).append("\n");
            log.append("| IP = ").append(ip).append("\n");
            log.append("| Fonte = ").append(source.format()).append("\n");
            log.append("| Motivo = ").append(motivo).append("\n\n");

            // ===== WRITE =====
            try (FileWriter writer = new FileWriter(auditFile, true)) {
                writer.write(log.toString());
            }

        } catch (IOException ex) {
            plugin.getLogger().severe("[AuditLogger] Erro ao escrever auditoria:");
            ex.printStackTrace();
        }
    }

    // =====================================================
    // ================== TIPOS AUXILIARES =================
    // =====================================================

    public enum AuditAction {
        GERADA,
        RESERVADA,
        DESRESERVADA,
        ATIVADA,
        VOUCHER,
        EXPIRADA,
        EXCLUIDA,
        BACKUP
    }

    public static class AuditActor {

        private final String type;
        private final String name;

        private AuditActor(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public static AuditActor admin(String name) {
            return new AuditActor("ADMIN", name);
        }

        public static AuditActor player(String name) {
            return new AuditActor("PLAYER", name);
        }

        public static AuditActor system(String name) {
            return new AuditActor("SYSTEM", name);
        }

        public String format() {
            return type + " : " + name;
        }
    }

    public static class AuditSource {

        private final String type;
        private final String value;

        private AuditSource(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public static AuditSource command(String command) {
            return new AuditSource("COMANDO", command);
        }

        public static AuditSource gui(String gui) {
            return new AuditSource("GUI", gui);
        }

        public static AuditSource system(String system) {
            return new AuditSource("SISTEMA", system);
        }

        public String format() {
            return type + " : " + value;
        }
    }

}
