package com.kamiplugins.kamikeys.services;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.managers.AuditLogger;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.Voucher;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.repositories.VoucherRepository;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import com.kamiplugins.kamikeys.utils.MessageUtils;
import com.kamiplugins.kamikeys.utils.UxService;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoucherService {

    private final Main plugin;
    private final VoucherRepository voucherRepository;
    private final KeyService keyService;
    private final Map<UUID, Long> lastActivation = new ConcurrentHashMap<>();
    private final NamespacedKey voucherKeyCodeKey;
    private final UxService uxService;
    private final AuditLogger auditLogger;

    public VoucherService(Main plugin, VoucherRepository voucherRepository, KeyService keyService) {
        this.plugin = plugin;
        this.voucherRepository = voucherRepository;
        this.keyService = keyService;
        this.voucherKeyCodeKey = new NamespacedKey(plugin, "voucher_key_code");
        this.uxService = new UxService(plugin);
        this.auditLogger = new AuditLogger(plugin);
    }

    /* =========================
       CRIAÇÃO DO VOUCHER
       ========================= */

    public Voucher createVoucherFromKey(Key key, String ownerUuid, String ownerName, int daysValid) {

        if (key == null) {
            throw new IllegalArgumentException("Key inválida.");
        }
        if (key.getState() != KeyState.ATIVA) {
            throw new IllegalStateException("Esta key não está ATIVA e não pode virar voucher.");
        }

        long expirationTime;
        if (daysValid == -1) {
            expirationTime = -1;
        } else {
            expirationTime = System.currentTimeMillis()
                    + (daysValid * 24L * 60L * 60L * 1000L);
        }

        Voucher voucher = new Voucher(
                UUID.randomUUID(),
                key.getCode(),
                ownerUuid,
                ownerName,
                expirationTime,
                ownerName,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );

        voucher.setValidityDays(daysValid);
        voucherRepository.save(voucher);

        // ✅ Auditoria de estado da KEY fica no KeyService
        Player creator = Bukkit.getPlayer(UUID.fromString(ownerUuid));

        keyService.changeKeyState(
                key,
                KeyState.VOUCHER,
                "Key convertida em voucher",
                ownerName
        );


        return voucher;
    }

    /* =========================
       ATIVAÇÃO DO VOUCHER
       ========================= */

    public void activateVoucher(Player player, ItemStack item) {
        if (player == null || item == null || item.getType() == Material.AIR) return;

        // Refresh visual sempre
        refreshVoucherLore(item);

        // Anti double-click
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastTime = lastActivation.get(playerId);
        if (lastTime != null && (now - lastTime) < 800) return;
        lastActivation.put(playerId, now);

        String keyCode = getLinkedKeyCodeFromItem(item);
        if (keyCode == null) {
            sendErrorMessage(player, "&cEste voucher é inválido.");
            removeVoucherItem(player, item);
            return;
        }

        String voucherId = item.getItemMeta()
                .getPersistentDataContainer()
                .get(
                        new NamespacedKey(plugin, "voucher_internal_id"),
                        PersistentDataType.STRING
                );

        if (voucherId == null) {
            sendErrorMessage(player, "&cVoucher inválido.");
            removeVoucherItem(player, item);
            return;
        }

        Optional<Voucher> voucherOpt = voucherRepository.findById(voucherId);
        if (voucherOpt.isEmpty()) {
            sendErrorMessage(player, "&cEste voucher não existe no sistema.");
            removeVoucherItem(player, item);
            uxService.playSoundFromConfig(player, "error");
            return;
        }

        Voucher voucher = voucherOpt.get();

        // OBS: se você quiser eliminar esse estado "active=false" no futuro,
        // pode trocar por "não existe no repositório". Mas mantive como estava.
        if (!voucher.isActive()) {
            sendErrorMessage(player, "&cEste voucher já foi utilizado.");
            removeVoucherItem(player, item);
            return;
        }

        /* =========================
           VOUCHER EXPIRADO
           ========================= */
        if (isVoucherExpired(voucher.getExpirationTime())) {

            // ✅ Agora trata a KEY (sem transição inválida pra USADA)
            Optional<Key> keyOpt = keyService.findByCode(keyCode);

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ColorUtils.translate("&c&l✖ Voucher Expirado"));
            meta.setLore(buildExpiredVoucherLore(meta));
            item.setItemMeta(meta);

            if (keyOpt.isPresent()) {
                Key key = keyOpt.get();

                keyService.changeKeyState(
                        key,
                        KeyState.EXPIRADA,
                        "Voucher expirado – tentativa de uso",
                        player.getName()
                );

                keyService.changeKeyState(
                        key,
                        KeyState.USADA,
                        "Key removida após voucher expirado",
                        "SYSTEM"
                );

                keyService.deleteKey(key.getCode());
            }

            // ✅ Agora sim remove o voucher do YAML (ÚLTIMO PASSO do voucher)
            markVoucherAsUsed(voucher);

            applyVoucherExpiredEffects(player);
            removeVoucherItem(player, item);
            uxService.playSoundFromConfig(player, "error");
            return;
        }

        /* =========================
           VOUCHER VÁLIDO
           ========================= */

        Optional<Key> keyOpt = keyService.findByCode(keyCode);
        if (keyOpt.isEmpty()) {
            sendErrorMessage(player, "&cA key associada a este voucher não existe mais.");

            // Se a key sumiu, ainda assim o voucher não pode continuar existindo
            markVoucherAsUsed(voucher);
            removeVoucherItem(player, item);
            return;
        }

        Key key = keyOpt.get();
        String typeKey = key.getTypeKey();

        // Executa comandos da key
        List<String> commands = plugin.getConfig()
                .getStringList("Types." + typeKey + ".Commands");

        for (String commandStr : commands) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    commandStr.replace("{player}", player.getName())
            );
        }

        // ✅ Ordem correta:
        // 1) Key USADA
        keyService.changeKeyState(
                key,
                KeyState.USADA,
                "Voucher ativado com sucesso",
                player.getName()
        );

        key.setActivatedBy(player.getName());
        key.setActivatedByUuid(player.getUniqueId().toString());
        key.setActivatedAt(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );

        // 2) Key EXCLUIDA
        keyService.changeKeyState(
                key,
                KeyState.EXCLUIDA,
                "Key consumida após ativação do voucher",
                "SYSTEM"
        );

        // Exclusão física obrigatória
        keyService.deleteKey(key.getCode());

        // 3) Só agora remove o voucher do YAML + loga a remoção do voucher
        markVoucherAsUsed(voucher);

        // 4) Remove item e aplica efeitos
        removeVoucherItem(player, item);
        applyVoucherActivationEffects(player, typeKey);
    }

    /* =========================
       LORE NORMAL
       ========================= */

    private List<String> buildNormalVoucherLore(ItemMeta meta, Voucher voucher, String reward) {

        List<String> lore = new ArrayList<>();

        lore.add(" ");
        lore.add(ColorUtils.translate("&8──────────────────"));
        lore.add(ColorUtils.translate("&7🎁 Recompensa: &e" + reward));
        lore.add(ColorUtils.translate("&7✏ Criado por: &d" + voucher.getCreatedBy()));

        int days = voucher.getValidityDays();

        Integer storedDays = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "voucher_valid_days"),
                PersistentDataType.INTEGER
        );
        if (storedDays != null) days = storedDays;

        if (days == -1) {
            lore.add(ColorUtils.translate("&7⏳ Validade: &a∞ Infinita"));
        } else if (days > 0) {
            lore.add(ColorUtils.translate("&7⏳ Validade: &f" + days + " dias"));
        } else if (days == 0) {
            lore.add(ColorUtils.translate("&7⏳ Validade: &cInstantâneo"));
        }

        if (voucher.getExpirationTime() != -1L) {
            String date = new SimpleDateFormat("dd/MM/yyyy HH:mm")
                    .format(new Date(voucher.getExpirationTime()));
            lore.add(ColorUtils.translate("&7⏰ Expira em: &f" + date));
        }

        lore.add(ColorUtils.translate("&8──────────────────"));
        lore.add("");
        lore.add(ColorUtils.translate("&6✦ Voucher exclusivo ✦"));
        lore.add(ColorUtils.translate("&cUso único • não reembolsável"));
        lore.add("");
        lore.add(ColorUtils.translate("&a▶ Clique direito para ativar"));

        return lore;
    }

    /* =========================
       LORE EXPIRADA
       ========================= */

    private List<String> buildExpiredVoucherLore(ItemMeta meta) {

        List<String> lore = new ArrayList<>();

        Long expTime = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "voucher_expiry"),
                PersistentDataType.LONG
        );

        lore.add(" ");
        lore.add(ColorUtils.translate("&8──────────────────"));

        if (expTime != null && expTime > 0) {
            String date = new SimpleDateFormat("dd/MM/yyyy HH:mm")
                    .format(new Date(expTime));
            lore.add(ColorUtils.translate("&7📅 Expirou em: &c" + date));
        } else {
            lore.add(ColorUtils.translate("&7📅 Status: &cExpirado"));
        }

        lore.add(" ");
        lore.add(ColorUtils.translate("&7Este voucher atingiu o tempo limite"));
        lore.add(ColorUtils.translate("&7e não pode mais ser utilizado."));
        lore.add(ColorUtils.translate("&8──────────────────"));
        lore.add(" ");
        lore.add(ColorUtils.translate("&8O item será removido ao tentar usar."));

        return lore;
    }

    /* =========================
       ITEM STACK
       ========================= */

    public ItemStack createVoucherItemStack(Voucher voucher, Key keyOriginal) {

        Material material;
        try {
            material = Material.valueOf(
                    plugin.getConfig().getString("Voucher.ItemMaterial", "SUNFLOWER")
                            .toUpperCase()
            );
        } catch (Exception e) {
            material = Material.SUNFLOWER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String reward = getRewardDescription(keyOriginal.getTypeKey());
        meta.setDisplayName(ColorUtils.translate("&e&l💰 Voucher de " + reward));
        meta.setLore(buildNormalVoucherLore(meta, voucher, reward));

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "voucher_internal_id"),
                PersistentDataType.STRING,
                voucher.getInternalId().toString()
        );

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "voucher_type"),
                PersistentDataType.STRING,
                keyOriginal.getTypeKey()
        );

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "voucher_expiry"),
                PersistentDataType.LONG,
                voucher.getExpirationTime()
        );

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "voucher_key_code"),
                PersistentDataType.STRING,
                voucher.getLinkedKeyCode()
        );

        // ✅ Validade fixa (dias) salva no item (source of truth visual)
        NamespacedKey validityKey = new NamespacedKey(plugin, "voucher_valid_days");
        meta.getPersistentDataContainer().set(
                validityKey,
                PersistentDataType.INTEGER,
                voucher.getValidityDays()
        );

        item.setItemMeta(meta);
        return item;
    }

    /* =========================
       REFRESH VISUAL
       ========================= */

    public boolean refreshVoucherLore(ItemStack item) {

        if (!isVoucherItem(item)) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String id = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "voucher_internal_id"),
                PersistentDataType.STRING
        );

        if (id == null) return false;

        Optional<Voucher> opt = voucherRepository.findById(id);
        if (opt.isEmpty()) return false;

        Voucher voucher = opt.get();

        // 🔒 SOURCE OF TRUTH: expirationTime
        if (isVoucherExpired(voucher.getExpirationTime())) {

            meta.setDisplayName(ColorUtils.translate("&c&l✖ Voucher Expirado"));
            meta.setLore(buildExpiredVoucherLore(meta));
            item.setItemMeta(meta);
            return true;
        }

        // ✅ SE NÃO ESTIVER EXPIRADO, SEMPRE RECONSTRÓI A LORE NORMAL
        String typeKey = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "voucher_type"),
                PersistentDataType.STRING
        );

        String reward = getRewardDescription(typeKey);

        meta.setDisplayName(ColorUtils.translate("&e&l💰 Voucher de " + reward));
        meta.setLore(buildNormalVoucherLore(meta, voucher, reward));
        item.setItemMeta(meta);

        return true;
    }

    /* =========================
       UTILITÁRIOS
       ========================= */

    public boolean isVoucherItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey(plugin, "voucher_type"),
                PersistentDataType.STRING
        );
    }

    public boolean isVoucherExpired(long expirationTime) {
        if (expirationTime == -1L) return false;
        return System.currentTimeMillis() >= expirationTime;
    }

    public String getLinkedKeyCodeFromItem(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(
                voucherKeyCodeKey,
                PersistentDataType.STRING
        );
    }

    private void sendErrorMessage(Player player, String message) {
        player.sendMessage(ColorUtils.translate(message));
    }

    private void removeVoucherItem(Player player, ItemStack item) {
        player.getInventory().removeItem(item);
        player.updateInventory();
    }

    /**
     * ✅ Agora o voucher SOME do vouchers.yml (tempo real)
     * e loga somente a remoção física do voucher.
     */
    private void markVoucherAsUsed(Voucher voucher) {

        // ❌ REMOÇÃO FÍSICA (FONTE DE VERDADE)
        voucherRepository.delete(voucher.getInternalId().toString());
    }

    private String getRewardDescription(String typeName) {
        String title = plugin.getConfig().getString("Types." + typeName + ".Recompensa");
        return title != null ? ColorUtils.translate(title) : "Recompensa";
    }

    private void applyVoucherActivationEffects(Player player, String typeKey) {
        UxService uxService = new UxService(plugin);
        String reward = plugin.getConfig().getString("Types." + typeKey + ".Recompensa", "Recompensa");

        Map<String, String> placeholders = MessageUtils.createPlaceholders(
                "reward", reward
        );

        String msg = MessageUtils.applyPlaceholders(
                MessageUtils.applyColor(
                        uxService.getMessage("player.activation_success_voucher")
                ),
                placeholders
        );

        player.sendMessage(msg);
        uxService.playSoundFromConfig(player, "success");
        uxService.playFirework(player);
    }

    private void applyVoucherExpiredEffects(Player player) {
        player.sendTitle(
                ColorUtils.translate("&c&l⏳ Voucher Expirado!"),
                ColorUtils.translate("&7Este voucher não pode mais ser usado."),
                10, 60, 20
        );

        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(
                        ColorUtils.translate("&cVoucher expirado • item removido")
                )
        );

        try {
            player.getWorld().spawnParticle(
                    Particle.SMOKE,
                    player.getLocation().add(0, 1, 0),
                    20
            );
        } catch (Exception ignored) {}
    }

    // Mantidos para compatibilidade (mesmo se não usados no fluxo atual)
    private String resolveKeyType(String keyCode) {
        return keyService.findByCode(keyCode)
                .map(Key::getTypeKey)
                .orElse("UNKNOWN");
    }

    private String resolveKeyOrigin(String keyCode) {
        return keyService.findByCode(keyCode)
                .map(k -> k.getOrigin().name())
                .orElse("UNKNOWN");
    }
}
