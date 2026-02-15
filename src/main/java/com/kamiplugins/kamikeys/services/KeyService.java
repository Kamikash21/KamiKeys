package com.kamiplugins.kamikeys.services;

import com.kamiplugins.kamikeys.Main;
import com.kamiplugins.kamikeys.managers.AuditLogger;
import com.kamiplugins.kamikeys.models.Key;
import com.kamiplugins.kamikeys.models.enums.KeyOrigin;
import com.kamiplugins.kamikeys.models.enums.KeyState;
import com.kamiplugins.kamikeys.repositories.KeyRepository;
import com.kamiplugins.kamikeys.utils.ColorUtils;
import com.kamiplugins.kamikeys.utils.KeyBatchDeleteResult;
import com.kamiplugins.kamikeys.utils.MessageUtils;
import com.kamiplugins.kamikeys.utils.UxService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class KeyService {
    private final Main plugin;
    private final KeyRepository keyRepository;
    private final AuditLogger auditLogger;
    private final UxService uxService;

    public KeyService(Main plugin, KeyRepository keyRepository) {
        this.plugin = plugin;
        this.keyRepository = keyRepository;
        this.auditLogger = new AuditLogger(plugin);
        this.uxService = new UxService(plugin);
    }

    // =====================================================
    // =================== AUDIT HELPERS ===================
    // =====================================================

    /**
     * Mapeia o estado final para uma ação de auditoria (Ação != Estado).
     * Mantém compatibilidade e evita IllegalArgumentException.
     */
    private AuditLogger.AuditAction mapActionFromState(KeyState newState) {
        if (newState == null) return AuditLogger.AuditAction.GERADA;

        return switch (newState) {
            case RESERVADA -> AuditLogger.AuditAction.RESERVADA;
            case USADA -> AuditLogger.AuditAction.ATIVADA;
            case VOUCHER -> AuditLogger.AuditAction.VOUCHER;
            case EXPIRADA -> AuditLogger.AuditAction.EXPIRADA;
            case EXCLUIDA -> AuditLogger.AuditAction.EXCLUIDA;

            // Estados que não têm ação “dedicada” no escopo da Fase A
            case VENDIDA, ATIVA, VENDA, BLOQUEADA -> AuditLogger.AuditAction.RESERVADA; // evento intermediário (compra/fluxo)
            default -> AuditLogger.AuditAction.GERADA;
        };
    }

    // Novo método para tipo dinâmico
    public Key generateKey(KeyOrigin origin, String typeKey, String generatedBy) {
        // Compatibilidade: chama a nova implementação sem jogador (SYSTEM)
        return generateKey(origin, typeKey, generatedBy, null);
    }

    // Nova sobrecarga que permite passar o Player (pode ser null)
    public Key generateKey(KeyOrigin origin, String typeKey, String generatedBy, @Nullable Player generatorPlayer) {
        String code = generateUniqueKeyCode(typeKey);

        KeyState initialState =
                (origin == KeyOrigin.VENDA)
                        ? KeyState.VENDA
                        : KeyState.ATIVA;

        Key newKey = new Key(
                UUID.randomUUID(),
                code,
                origin,
                typeKey,
                initialState,
                generatedBy,
                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date())
        );

        keyRepository.save(newKey);

        // 🔐 AUDITORIA NOVA (NONE -> estado inicial)
        auditLogger.logKeyEvent(
                AuditLogger.AuditAction.GERADA,
                code,
                typeKey,
                origin.name(),
                null,
                initialState,
                resolveActor("ADMIN", generatorPlayer),
                resolveIp(generatorPlayer),
                AuditLogger.AuditSource.system("KeyService.generateKey"),
                "Criação de keys"
        );
        return newKey;
    }

    private String generateUniqueKeyCode(String typeKey) {
        FileConfiguration config = plugin.getConfig();
        int length = config.getInt("Types." + typeKey + ".Length", 15);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        StringBuilder code = new StringBuilder();
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        String fullCode = code.toString();

        while (keyRepository.findByCode(fullCode).isPresent()) {
            code.setLength(0);
            for (int i = 0; i < length; i++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
            fullCode = code.toString();
        }

        return fullCode;
    }

    public Optional<Key> activateKey(String keyCode, String playerName, UUID playerUUID) {

        Optional<Key> keyOpt = keyRepository.findByCode(keyCode);
        if (!keyOpt.isPresent()) {
            return Optional.empty();
        }

        Key key = keyOpt.get();

        // ✅ Agora ATIVA e VENDIDA podem ser ativadas
        if (key.getState() != KeyState.ATIVA && key.getState() != KeyState.VENDIDA) {
            return Optional.empty();
        }

        // 🔒 Exclusividade (se existir)
        if (key.getExclusiveToName() != null &&
                !key.getExclusiveToName().equalsIgnoreCase(playerName)) {
            return Optional.empty();
        }

        Player player = Bukkit.getPlayer(playerUUID);

        // 🔐 AUDITORIA NOVA (ativação) - estado atual -> USADA
        auditLogger.logKeyEvent(
                AuditLogger.AuditAction.ATIVADA,
                key.getCode(),
                key.getTypeKey(),
                key.getOrigin().name(),
                key.getState(),
                KeyState.USADA,
                AuditLogger.AuditActor.player(playerName + " | UUID : " + player.getUniqueId().toString()),
                resolveIp(Bukkit.getPlayer(playerUUID)),
                AuditLogger.AuditSource.gui("Keys"),
                "Key ativada com sucesso"
        );


        // ===== EXECUTA RECOMPENSAS =====
        executeKeyCommands(key, playerName);


        // Mensagem de ativação
        String reward = plugin.getConfig()
                .getString("Types." + key.getTypeKey() + ".Recompensa", "Recompensa");

        String msg = MessageUtils.applyPlaceholders(
                MessageUtils.applyColor(
                        uxService.getMessage("player.activation_success_detailed")
                ),
                MessageUtils.createPlaceholders(
                        "reward", reward
                )
        );

        player.sendMessage(msg);


        // ===== FEEDBACK VISUAL E SONORO =====
        uxService.playSoundFromConfig(player, "success");
        uxService.playParticcle(player, "activation_success");
        uxService.playFirework(player);

        // Enviar title de sucesso específico
        String successTitle = uxService.getMessage("activation.success.title");
        String successSubtitle = uxService.getMessage("activation.success.subtitle");

        Map<String, String> titlePlaceholders = MessageUtils.createPlaceholders(
                "reward", reward
        );

        String processedTitle = MessageUtils.applyPlaceholders(MessageUtils.applyColor(successTitle), titlePlaceholders);
        String processedSubtitle = MessageUtils.applyPlaceholders(MessageUtils.applyColor(successSubtitle), titlePlaceholders);

        player.sendTitle(processedTitle, processedSubtitle, 10, 70, 20);

        // Actionbar
        player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        ColorUtils.translate(
                                uxService.getMessage("activation.success.actionbar")
                        )
                )
        );


        // ===== HISTÓRICO: USADA =====
        key.setActivatedBy(playerName);
        key.setActivatedByUuid(playerUUID != null ? playerUUID.toString() : null);
        key.setActivatedAt(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
        key.setState(KeyState.USADA);
        keyRepository.update(key);


        // ===== EXCLUSÃO DEFINITIVA =====
        deleteKey(keyCode);

        return Optional.of(key);
    }

    private void executeKeyCommands(Key key, String playerName) {
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("Types." + key.getTypeKey() + ".Commands");

        for (String command : commands) {
            String finalCommand = command.replace("{player}", playerName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        }
    }

    public List<Key> getKeysByOrigin(KeyOrigin origin) {
        return keyRepository.findByOrigin(origin.name());
    }

    public List<Key> getKeysByType(String typeKey) {
        return keyRepository.findByType(typeKey);
    }

    public List<Key> getAllKeys() {
        return keyRepository.findAll();
    }

    public boolean isKeyValidAndActive(String keyCode) {
        Optional<Key> keyOpt = keyRepository.findByCode(keyCode);
        return keyOpt.isPresent() && keyOpt.get().getState() == KeyState.ATIVA;
    }

    public void deleteKey(String keyCode) {
        Optional<Key> keyOpt = keyRepository.findByCode(keyCode);
        if (keyOpt.isPresent()) {
            Key key = keyOpt.get();

            // 🔐 AUDITORIA NOVA (exclusão direta via API)
            Player player = Bukkit.getPlayerExact(key.getActivatedBy());
            String playerNick = (player != null) ? player.getName() : "UNKNOWN";
            String playerIp = (player != null) ? player.getAddress().getHostString() : "UNKNOWN";


            auditLogger.logKeyEvent(
                    AuditLogger.AuditAction.EXCLUIDA,
                    key.getCode(),
                    key.getTypeKey(),
                    key.getOrigin().name(),
                    KeyState.USADA,
                    KeyState.EXCLUIDA,
                    AuditLogger.AuditActor.player(playerNick + " | UUID : " + player.getUniqueId().toString()),
                    playerIp,
                    AuditLogger.AuditSource.system("KeyService.deleteKey"),
                    "Key excluída do banco de dados via API"
            );

            keyRepository.delete(keyCode);

        }
    }

    public boolean assignExclusiveKey(String keyCode, String playerName, String playerUUID) {
        Optional<Key> keyOpt = keyRepository.findByCode(keyCode);

        if (!keyOpt.isPresent()) {
            return false;
        }

        Key key = keyOpt.get();

        if (key.getState() != KeyState.ATIVA) {
            return false;
        }

        key.setExclusiveToName(playerName);
        key.setExclusiveToUuid(playerUUID);
        keyRepository.update(key);

        keyRepository.update(key);
        return true;
    }

    public boolean isKeyExclusiveToPlayer(String keyCode, String playerName) {
        Optional<Key> keyOpt = keyRepository.findByCode(keyCode);

        if (!keyOpt.isPresent()) {
            return false;
        }

        Key key = keyOpt.get();

        if (key.getExclusiveToName() == null) {
            return false;
        }

        return key.getExclusiveToName().equalsIgnoreCase(playerName);
    }

    public ItemStack createKeyItemStack(Key key, Player player) {
        return createKeyItemStack(key, player, false);
    }

    // ✅ novo método com controle de origem
    public ItemStack createKeyItemStack(Key key, Player player, boolean showOrigin) {
        FileConfiguration config = plugin.getConfig();
        String materialName = config.getString("Items.AdminKeyItem", "TRIPWIRE_HOOK");
        ItemStack item;
        try {
            item = new ItemStack(Material.valueOf(materialName));
        } catch (IllegalArgumentException e) {
            item = new ItemStack(Material.TRIPWIRE_HOOK);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String prefixColor = config.getString("Types." + key.getTypeKey() + ".PrefixColor", "&b");
        meta.setDisplayName(ColorUtils.translate(prefixColor + key.getCode()));

        List<String> lore = new ArrayList<>();
        lore.add("");

        String reward = getRewardDescription(key.getTypeKey());
        lore.add(ColorUtils.translate("&7🎁 Recompensa: &e" + reward));

        lore.add("");

        String status = getStatusDescription(key.getState());
        lore.add(ColorUtils.translate("&7Status: " + status));

        lore.add("");

        // ✅ MOSTRAR ORIGEM SOMENTE QUANDO FOR MENU "TODAS AS KEYS"
        if (showOrigin) {
            String origemDisplay;
            switch (key.getOrigin()) {
                case VENDA:
                    origemDisplay = "&aVenda";
                    break;
                case INTERNA:
                    origemDisplay = "&bInterna";
                    break;
                case PLAYER:
                    origemDisplay = "&cExclusiva";
                    break;
                default:
                    origemDisplay = "&7Desconhecida";
                    break;
            }
            lore.add(ColorUtils.translate("&7Origem: " + origemDisplay));
            lore.add("");
        }

        lore.add(ColorUtils.translate("&7Gerada em: &f" + key.getCreatedAt()));
        lore.add(ColorUtils.translate("&7Gerada por: &f" + (key.getGeneratedBy() != null ? key.getGeneratedBy() : "Sistema")));

        if (key.getExclusiveToName() != null) {
            lore.add(ColorUtils.translate("&7Dono: &b" + key.getExclusiveToName()));
        }

        lore.add("");

        if (key.getState() == KeyState.ATIVA
                || key.getState() == KeyState.VENDA) {
            lore.add(ColorUtils.translate("&a✅ Clique esquerdo para copiar"));
            lore.add(ColorUtils.translate("&c❌ Clique direito para excluir"));
        } else {
            lore.add(ColorUtils.translate("&c❌ Key indisponível"));
        }

        meta.setLore(lore);

        if (config.getBoolean("Items.GlowOnKeys", false)) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        NamespacedKey keyNsKey = new NamespacedKey(plugin, "key_code");
        meta.getPersistentDataContainer().set(keyNsKey, PersistentDataType.STRING, key.getCode());

        item.setItemMeta(meta);
        return item;
    }

    public void applyKeyRewards(Key key, Player player) {

        if (key == null || player == null) return;

        String typeKey = key.getTypeKey();

        List<String> commands = plugin.getConfig()
                .getStringList("Types." + typeKey + ".Commands");

        if (commands == null || commands.isEmpty()) return;

        for (String command : commands) {
            String parsed = command
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString());

            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    parsed
            );
        }
    }

    private String getRewardDescription(String typeKey) {
        FileConfiguration config = plugin.getConfig();

        String title = config.getString("Types." + typeKey + ".Title");
        if (title != null) {
            return ColorUtils.translate(title);
        }

        List<String> comandos = config.getStringList("Types." + typeKey + ".Commands");
        if (comandos.isEmpty()) return "Recompensa desconhecida";

        for (String cmd : comandos) {
            if (cmd.contains("points")) {
                String[] parts = cmd.split(" ");
                if (parts.length >= 4 && parts[2].equals("give")) {
                    try {
                        int valor = Integer.parseInt(parts[3]);
                        return valor + " coins";
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        }

        return "Recompensa personalizada";
    }

    private String getStatusDescription(KeyState state) {
        switch (state) {
            case ATIVA:
                return "&a🟢 Ativa";
            case VENDA:
                return "&2💰 Venda";
            case RESERVADA:
                return "&e⏳ Reservada";
            case VENDIDA:
                return "&6✔ Vendida";
            case VOUCHER:
                return "&b🎟 Voucher";
            case EXPIRADA:
                return "&7🟣 Expirada";
            case USADA:
                return "&c🔴 Usada";
            case BLOQUEADA:
                return "&4⛔ Bloqueada";
            case EXCLUIDA:
                return "&8✖ Excluída";
            default:
                return "&7❓ Desconhecido";
        }
    }

    public void updateKey(Key key) {
        keyRepository.update(key);
    }

    // Adicione este método para obter keys do jogador
    public List<Key> getKeysForPlayer(String playerName) {
        return keyRepository.findByExclusiveToName(playerName);
    }

    // Adicione este método para encontrar key por código
    public Optional<Key> findByCode(String code) {
        return keyRepository.findByCode(code);
    }

    /**
     * Método centralizado para mudança segura de estado da Key.
     * NÃO remove compatibilidade com setState().
     *
     * @param key      Key alvo
     * @param newState Novo estado desejado
     * @param reason   Motivo da mudança (log/auditoria)
     * @param actor    Quem executou (admin, bot, sistema, player)
     * @return true se a transição foi aplicada
     */
    public boolean changeKeyState(Key key, KeyState newState, String reason, String actor) {

        if (key == null || newState == null) return false;

        KeyState fromState = key.getState();

        // 🔒 Não permitir alteração em key já excluída
        if (fromState == KeyState.EXCLUIDA) {
            plugin.getLogger().warning(
                    "[KeyService] Tentativa de alterar key EXCLUIDA: " + key.getCode()
            );
            return false;
        }

        // 🔁 Validação de transição
        if (!isValidTransition(fromState, newState)) {
            plugin.getLogger().warning(
                    "[KeyService] Transição inválida: "
                            + fromState + " -> " + newState
                            + " | Key=" + key.getCode()
                            + " | Actor=" + actor
                            + " | Reason=" + reason
            );
            return false;
        }

        // 📝 Log de transição (console)
        plugin.getLogger().info(
                "[KeyService] Key " + key.getCode()
                        + " | " + fromState + " -> " + newState
                        + " | Actor=" + actor
                        + " | Reason=" + reason
        );


        Player player = Bukkit.getPlayerExact(actor);

        AuditLogger.AuditActor auditActor;
        String ip = "SYSTEM";

        if (player != null) {
            auditActor = AuditLogger.AuditActor.player(
                    actor + " | UUID:" + player.getUniqueId()
            );
            ip = resolveIp(player);
        } else {
            // Pode ser SYSTEM ou player offline
            auditActor = AuditLogger.AuditActor.system(actor);
        }

        auditLogger.logKeyEvent(
                mapActionFromState(newState),
                key.getCode(),
                key.getTypeKey(),
                key.getOrigin().name(),
                fromState,
                newState,
                auditActor,
                ip,
                AuditLogger.AuditSource.system("KeyService.changeKeyState"),
                reason != null ? reason : "N/A"
        );



        // 🗑️ EXCLUSÃO FINAL (regra absoluta)
        if (newState == KeyState.EXCLUIDA) {

            // remoção física
            keyRepository.delete(key.getCode());
            return true;
        }

        // ✅ Aplicar mudança normal
        key.setState(newState);
        keyRepository.update(key);

        return true;
    }

    /**
     * Define todas as transições válidas de estado da Key.
     */
    private boolean isValidTransition(KeyState from, KeyState to) {

        switch (from) {

            case ATIVA:
                return to == KeyState.VOUCHER
                        || to == KeyState.RESERVADA
                        || to == KeyState.BLOQUEADA
                        || to == KeyState.EXCLUIDA;

            case VENDA:
                return to == KeyState.RESERVADA
                        || to == KeyState.EXCLUIDA;

            case RESERVADA:
                return to == KeyState.VENDIDA
                        || to == KeyState.ATIVA;

            case VENDIDA:
                return to == KeyState.USADA;

            case VOUCHER:
                return to == KeyState.USADA
                        || to == KeyState.EXPIRADA;

            case EXPIRADA:
                return to == KeyState.USADA;

            case USADA:
                return to == KeyState.EXCLUIDA;

            case BLOQUEADA:
                return to == KeyState.EXCLUIDA;

            default:
                return false;
        }
    }

    public boolean reserveKey(Key key, String actor) {
        return changeKeyState(
                key,
                KeyState.RESERVADA,
                "Key reservada para compra",
                actor
        );
    }

    public boolean confirmSale(Key key, String actor) {
        return changeKeyState(
                key,
                KeyState.VENDIDA,
                "Compra confirmada",
                actor
        );
    }

    /**
     * ✅ Exclusão em lote ULTRA SEGURA para o /kamikeys apagar
     *
     * Regras:
     * - NUNCA apaga keys que não sejam ATIVA
     * - Ignora silenciosamente qualquer estado != ATIVA
     * - A exclusão real é feita via changeKeyState(... EXCLUIDA ...)
     */
    public KeyBatchDeleteResult deleteBatch(
            List<Key> keys,
            String actor,
            String reason
    ) {
        if (keys == null) {
            return new KeyBatchDeleteResult(0, 0, 0);
        }

        int totalEncontradas = keys.size();
        int apagadas = 0;
        int ignoradasPorEstado = 0;

        for (Key key : keys) {
            if (key == null) {
                ignoradasPorEstado++;
                continue;
            }

            // 🔒 REGRA ABSOLUTA: só ATIVA pode ser apagada
            if (key.getState() != KeyState.ATIVA
                    && key.getState() != KeyState.VENDA) {
                ignoradasPorEstado++;
                continue;
            }

            boolean ok = changeKeyState(key, KeyState.EXCLUIDA, reason, actor);
            if (ok) {
                apagadas++;
            } else {
                ignoradasPorEstado++;
            }
        }

        // Log consolidado (1 vez por comando)
        plugin.getLogger().warning(
                "[KeyService] Batch delete executado | Actor=" + actor
                        + " | Reason=" + reason
                        + " | Total=" + totalEncontradas
                        + " | Apagadas=" + apagadas
                        + " | Ignoradas=" + ignoradasPorEstado
        );

        return new KeyBatchDeleteResult(totalEncontradas, apagadas, ignoradasPorEstado);
    }

    public KeyBatchDeleteResult deleteKeysByPlayer(
            @Nullable String playerName,
            String actor,
            String reason
    ) {

        List<Key> keys;

        // PLAYER ALL
        if (playerName == null) {
            keys = getAllKeys().stream()
                    .filter(k -> k.getOrigin() == KeyOrigin.PLAYER)
                    .toList();
        }
        // PLAYER ESPECÍFICO
        else {
            keys = getKeysForPlayer(playerName);
        }

        if (keys.isEmpty()) {
            return new KeyBatchDeleteResult(0, 0, 0);
        }

        // 🔒 REGRA ABSOLUTA DO PLAYER:
        // somente keys ATIVAS
        List<Key> deletables = keys.stream()
                .filter(k -> k.getState() == KeyState.ATIVA)
                .toList();

        return deleteBatch(deletables, actor, reason);
    }

    public KeyBatchDeleteResult deleteKeysByType(
            String type,
            String actor,
            String reason
    ) {
        // reaproveita método que JÁ EXISTE
        List<Key> keys = getAllKeysByType(type);

        return deleteBatch(keys, actor, reason);
    }

    public List<Key> getAllKeysByType(String type) {
        List<Key> result = new ArrayList<>();

        for (Key key : getAllKeys()) { // ⚠️ use o método REAL que você já tem
            if (key != null && type.equalsIgnoreCase(key.getTypeKey())) {
                result.add(key);
            }
        }
        return result;
    }

    private AuditLogger.AuditActor resolveActor(String actorName, @Nullable Player player) {
        if (player != null) {
            return AuditLogger.AuditActor.player(player.getName());
        }

        if (actorName == null || actorName.equalsIgnoreCase("SYSTEM")) {
            return AuditLogger.AuditActor.system("SYSTEM");
        }

        return AuditLogger.AuditActor.admin(actorName);
    }

    private String resolveIp(@Nullable Player player) {
        if (player == null || player.getAddress() == null) {
            return "SYSTEM";
        }
        return player.getAddress().getAddress().getHostAddress();
    }

}
