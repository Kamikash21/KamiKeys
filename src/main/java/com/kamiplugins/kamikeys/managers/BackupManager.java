package com.kamiplugins.kamikeys.managers;

import com.kamiplugins.kamikeys.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {

    private final Main plugin;

    public BackupManager(Main plugin) {
        this.plugin = plugin;
    }

    public void executeBackup(CommandSender sender) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            try {

                File dataFolder = plugin.getDataFolder();
                File backupFolder = new File(dataFolder, "backups");

                if (!backupFolder.exists()) {
                    backupFolder.mkdirs();
                }

                String timestamp = new SimpleDateFormat("dd_MM_yyyy_HH-mm-ss")
                        .format(new Date());

                File zipFile = new File(backupFolder,
                        "backup_" + timestamp + ".zip");

                try (ZipOutputStream zos = new ZipOutputStream(
                        new FileOutputStream(zipFile))) {

                    // ========================
                    // KEYS
                    // ========================
                    addFileToZip(zos,
                            new File(dataFolder, "keys.yml"),
                            "keys.yml");

                    // ========================
                    // VOUCHERS
                    // ========================
                    addFileToZip(zos,
                            new File(dataFolder, "vouchers.yml"),
                            "vouchers.yml");

                    // ========================
                    // LOGS (PASTA COMPLETA)
                    // ========================
                    File logsFolder = new File(dataFolder, "logs");

                    if (logsFolder.exists()) {
                        addFolderToZip(zos, logsFolder, "logs/");
                    }
                }

                sender.sendMessage("§aBackup criado com sucesso: §f"
                        + zipFile.getName());

            } catch (Exception e) {
                sender.sendMessage("§cErro ao gerar backup.");
                e.printStackTrace();
            }

        });
    }

    private void addFileToZip(ZipOutputStream zos,
                              File file,
                              String entryName) throws IOException {

        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {

            zos.putNextEntry(new ZipEntry(entryName));

            byte[] buffer = new byte[4096];
            int length;

            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
        }
    }

    private void addFolderToZip(ZipOutputStream zos,
                                File folder,
                                String parentFolder) throws IOException {

        for (File file : folder.listFiles()) {

            String entryName = parentFolder + file.getName();

            if (file.isDirectory()) {

                // cria a pasta no zip explicitamente
                zos.putNextEntry(new ZipEntry(entryName + "/"));
                zos.closeEntry();

                addFolderToZip(zos, file, entryName + "/");

            } else {

                try (FileInputStream fis = new FileInputStream(file)) {

                    zos.putNextEntry(new ZipEntry(entryName));

                    byte[] buffer = new byte[4096];
                    int length;

                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }

                    zos.closeEntry();
                }
            }
        }
    }


    // ===============================
    // ZIP SINGLE FILE
    // ===============================

    private void zipFile(File rootFolder,
                         String fileName,
                         ZipOutputStream zos) throws IOException {

        File file = new File(rootFolder, fileName);

        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {

            zos.putNextEntry(new ZipEntry(fileName));

            byte[] buffer = new byte[1024];
            int length;

            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
        }
    }

    // ===============================
    // ZIP DIRECTORY (RECURSIVO)
    // ===============================

    private void zipDirectory(File folder,
                              String parentFolder,
                              ZipOutputStream zos) throws IOException {

        for (File file : folder.listFiles()) {

            if (file.isDirectory()) {

                zipDirectory(file,
                        parentFolder + "/" + file.getName(),
                        zos);

            } else {

                try (FileInputStream fis = new FileInputStream(file)) {

                    zos.putNextEntry(
                            new ZipEntry(parentFolder + "/" + file.getName())
                    );

                    byte[] buffer = new byte[1024];
                    int length;

                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }

                    zos.closeEntry();
                }
            }
        }
    }

    private void zipDirectoryRecursive(File folder,
                                       String parentFolder,
                                       ZipOutputStream zos) throws IOException {

        if (folder == null || !folder.exists()) return;

        // 🔹 CRIA ENTRADA DA PASTA NO ZIP
        if (!parentFolder.endsWith("/")) {
            parentFolder = parentFolder + "/";
        }

        zos.putNextEntry(new ZipEntry(parentFolder));
        zos.closeEntry();

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {

            String entryName = parentFolder + file.getName();

            if (file.isDirectory()) {

                // 🔁 Recursão mantendo estrutura
                zipDirectoryRecursive(file, entryName, zos);

            } else {

                try (FileInputStream fis = new FileInputStream(file)) {

                    zos.putNextEntry(new ZipEntry(entryName));

                    byte[] buffer = new byte[1024];
                    int length;

                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }

                    zos.closeEntry();
                }
            }
        }
    }

}
