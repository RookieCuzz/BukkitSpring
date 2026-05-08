package top.skillstone.consumableskillbridge.factory;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import top.skillstone.consumableskillbridge.ConsumableSkillBridgePlugin;
import top.skillstone.consumableskillbridge.integration.ItemVisualBridge;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;
import top.skillstone.consumableskillbridge.registry.ItemDefinitionRegistry;

public class ItemFactory {
    public static final int STATE_SCHEMA_VERSION = 3;

    private final ConsumableSkillBridgePlugin plugin;
    private final ItemDefinitionRegistry registry;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey usesLeftKey;
    private final NamespacedKey maxConsumeKey;
    private final NamespacedKey cooldownKey;
    private final NamespacedKey cooldownUntilKey;
    private final NamespacedKey stateSchemaKey;

    public ItemFactory(ConsumableSkillBridgePlugin plugin, ItemDefinitionRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.usesLeftKey = new NamespacedKey(plugin, "uses_left");
        this.maxConsumeKey = new NamespacedKey(plugin, "max_consume");
        this.cooldownKey = new NamespacedKey(plugin, "cooldown_key");
        this.cooldownUntilKey = new NamespacedKey(plugin, "cooldown_until");
        this.stateSchemaKey = new NamespacedKey(plugin, "state_schema");
    }

    public ItemStack create(String itemId, int amount) {
        ConsumableDefinition definition = registry.getDefinition(itemId);
        if (definition == null) {
            return null;
        }
        return create(definition, amount);
    }

    public ItemStack create(ConsumableDefinition definition, int amount) {
        int safeAmount = Math.max(1, amount);
        ItemVisualBridge itemVisualBridge = plugin.getItemVisualBridge();
        ItemStack item = null;
        boolean craftedByExternalBridge = false;

        if (itemVisualBridge != null && !definition.getCraftEngineId().isEmpty()) {
            item = itemVisualBridge.createItemStack(definition.getCraftEngineId(), safeAmount);
            craftedByExternalBridge = item != null;
            if (craftedByExternalBridge) {
                plugin.debug("Created consumable '" + definition.getId() + "' via " + itemVisualBridge.getProviderName()
                        + " item '" + definition.getCraftEngineId() + "'.");
            }
        }

        if (item == null) {
            item = new ItemStack(definition.getMaterial(), safeAmount);
        } else if (item.getAmount() != safeAmount) {
            item.setAmount(safeAmount);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(definition.getDisplayName());
        if (!craftedByExternalBridge) {
            if (definition.getCustomModelData() != null) {
                meta.setCustomModelData(definition.getCustomModelData());
            }
            if (!definition.getItemModel().isEmpty()) {
                NamespacedKey itemModel = NamespacedKey.fromString(definition.getItemModel());
                if (itemModel != null) {
                    meta.setItemModel(itemModel);
                }
            }
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemIdKey, PersistentDataType.STRING, definition.getId());
        pdc.set(usesLeftKey, PersistentDataType.INTEGER, Math.max(1, definition.getMaxConsume()));
        pdc.set(maxConsumeKey, PersistentDataType.INTEGER, Math.max(1, definition.getMaxConsume()));
        pdc.set(cooldownKey, PersistentDataType.STRING, definition.getCooldownKey());
        pdc.set(cooldownUntilKey, PersistentDataType.LONG, 0L);
        pdc.set(stateSchemaKey, PersistentDataType.INTEGER, STATE_SCHEMA_VERSION);
        item.setItemMeta(meta);
        refreshDynamicLore(item, definition);
        return item;
    }

    public ConsumableDefinition resolveDefinition(ItemStack item) {
        String id = readItemId(item);
        if (id == null || id.isEmpty()) {
            return null;
        }
        return registry.getDefinition(id);
    }

    public String readItemId(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    public int readUsesLeft(ItemStack item, ConsumableDefinition definition) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return Math.max(1, definition.getMaxConsume());
        Integer found = meta.getPersistentDataContainer().get(usesLeftKey, PersistentDataType.INTEGER);
        if (found == null || found <= 0) {
            return readMaxConsume(item, definition);
        }
        return found;
    }

    public int readMaxConsume(ItemStack item, ConsumableDefinition definition) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return Math.max(1, definition.getMaxConsume());
        Integer found = meta.getPersistentDataContainer().get(maxConsumeKey, PersistentDataType.INTEGER);
        if (found == null || found <= 0) {
            return Math.max(1, definition.getMaxConsume());
        }
        return found;
    }

    public void writeUsesLeft(ItemStack item, int usesLeft) {
        writeUsesLeft(item, usesLeft, resolveDefinitionFromPdc(item));
    }

    public void writeUsesLeft(ItemStack item, int usesLeft, ConsumableDefinition definition) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(usesLeftKey, PersistentDataType.INTEGER, Math.max(1, usesLeft));
        item.setItemMeta(meta);
        refreshDynamicLore(item, definition);
    }

    public String readCooldownKey(ItemStack item, ConsumableDefinition definition) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return definition.getCooldownKey();
        String found = meta.getPersistentDataContainer().get(cooldownKey, PersistentDataType.STRING);
        return found == null || found.trim().isEmpty() ? definition.getCooldownKey() : found;
    }

    public long readCooldownUntil(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return 0L;
        Long found = meta.getPersistentDataContainer().get(cooldownUntilKey, PersistentDataType.LONG);
        return found == null ? 0L : Math.max(0L, found);
    }

    public void writeCooldownUntil(ItemStack item, long cooldownUntilEpochMillis) {
        writeCooldownUntil(item, cooldownUntilEpochMillis, resolveDefinitionFromPdc(item));
    }

    public void writeCooldownUntil(ItemStack item, long cooldownUntilEpochMillis, ConsumableDefinition definition) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(cooldownUntilKey, PersistentDataType.LONG, Math.max(0L, cooldownUntilEpochMillis));
        item.setItemMeta(meta);
        refreshDynamicLore(item, definition);
    }

    public int readStateSchema(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return 0;
        Integer found = meta.getPersistentDataContainer().get(stateSchemaKey, PersistentDataType.INTEGER);
        return found == null ? 0 : Math.max(0, found);
    }

    public void refreshDynamicLore(ItemStack item, ConsumableDefinition definition) {
        if (item == null || item.getType().isAir() || definition == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        if (definition.getLore().isEmpty()) {
            return;
        }

        int maxConsume = readMaxConsume(item, definition);
        int usesLeft = readUsesLeft(item, definition);
        long cooldownUntil = readCooldownUntil(item);
        long remainingMillis = Math.max(0L, cooldownUntil - System.currentTimeMillis());
        long cooldownSecondsLeft = (remainingMillis + 999L) / 1000L;

        java.util.List<String> rendered = new java.util.ArrayList<>(definition.getLore().size());
        for (String line : definition.getLore()) {
            if (line == null) {
                rendered.add("");
                continue;
            }
            String updated = line
                    .replace("{uses_left}", String.valueOf(usesLeft))
                    .replace("%uses_left%", String.valueOf(usesLeft))
                    .replace("{max_consume}", String.valueOf(maxConsume))
                    .replace("%max_consume%", String.valueOf(maxConsume))
                    .replace("{cooldown_seconds}", String.valueOf(cooldownSecondsLeft))
                    .replace("%cooldown_seconds%", String.valueOf(cooldownSecondsLeft));
            rendered.add(updated);
        }
        meta.setLore(rendered);
        item.setItemMeta(meta);
    }

    private ConsumableDefinition resolveDefinitionFromPdc(ItemStack item) {
        String itemId = readItemId(item);
        if (itemId == null || itemId.trim().isEmpty()) {
            return null;
        }
        return registry.getDefinition(itemId);
    }
}
