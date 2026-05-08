package top.skillstone.consumableskillbridge.integration.craftengine;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import top.skillstone.consumableskillbridge.ConsumableSkillBridgePlugin;
import top.skillstone.consumableskillbridge.integration.ItemVisualBridge;

import java.util.HashSet;
import java.util.Set;

public class CraftEngineItemVisualBridge implements ItemVisualBridge {
    private final ConsumableSkillBridgePlugin plugin;
    private final Set<String> warnedMissing = new HashSet<>();
    private final Set<String> warnedInvalid = new HashSet<>();

    public CraftEngineItemVisualBridge(ConsumableSkillBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getProviderName() {
        return "CraftEngine";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ItemStack createItemStack(String visualId, int amount) {
        String normalized = visualId == null ? "" : visualId.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        final Key key;
        try {
            key = Key.from(normalized);
        } catch (Throwable ex) {
            warnInvalidOnce(normalized, "Invalid CraftEngine item id: '" + normalized + "'.");
            return null;
        }

        final CustomItem<ItemStack> customItem;
        try {
            customItem = CraftEngineItems.byId(key);
        } catch (Throwable ex) {
            plugin.warn("Failed to query CraftEngine item '" + normalized + "': " + ex.getMessage());
            return null;
        }

        if (customItem == null) {
            warnMissingOnce(normalized, "CraftEngine item not found: '" + normalized + "'.");
            return null;
        }

        try {
            int safeAmount = Math.max(1, amount);
            ItemStack built = customItem.buildItemStack(safeAmount);
            if (built == null || built.getType().isAir()) {
                warnInvalidOnce(normalized, "CraftEngine item '" + normalized + "' resolved to an empty stack.");
                return null;
            }
            built.setAmount(safeAmount);
            return built;
        } catch (Throwable ex) {
            plugin.warn("Failed to build CraftEngine item '" + normalized + "': " + ex.getMessage());
            return null;
        }
    }

    private void warnMissingOnce(String id, String message) {
        if (warnedMissing.add(id)) {
            plugin.warn(message);
        }
    }

    private void warnInvalidOnce(String id, String message) {
        if (warnedInvalid.add(id)) {
            plugin.warn(message);
        }
    }
}
