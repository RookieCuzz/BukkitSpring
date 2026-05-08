package top.skillstone.consumableskillbridge.integration;

import org.bukkit.inventory.ItemStack;

public final class NoopItemVisualBridge implements ItemVisualBridge {
    public static final NoopItemVisualBridge INSTANCE = new NoopItemVisualBridge();

    private NoopItemVisualBridge() {
    }

    @Override
    public String getProviderName() {
        return "none";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public ItemStack createItemStack(String visualId, int amount) {
        return null;
    }
}
