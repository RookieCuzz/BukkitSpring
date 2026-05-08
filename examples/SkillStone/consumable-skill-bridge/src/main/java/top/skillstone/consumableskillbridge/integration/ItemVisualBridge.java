package top.skillstone.consumableskillbridge.integration;

import org.bukkit.inventory.ItemStack;

public interface ItemVisualBridge {
    String getProviderName();

    boolean isAvailable();

    ItemStack createItemStack(String visualId, int amount);
}
