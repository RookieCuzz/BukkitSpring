package top.skillstone.consumableskillbridge.engine;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import top.skillstone.consumableskillbridge.ConsumeBehavior;
import top.skillstone.consumableskillbridge.ConsumableSkillBridgePlugin;
import top.skillstone.consumableskillbridge.event.ConsumablePostUseEvent;
import top.skillstone.consumableskillbridge.event.ConsumablePreUseEvent;
import top.skillstone.consumableskillbridge.factory.ItemFactory;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;

import java.util.Locale;
import java.util.Map;

public class ConsumeEngine {
    private final ConsumableSkillBridgePlugin plugin;
    private final ItemFactory itemFactory;
    private final RequirementEngine requirementEngine;
    private final SkillEngine skillEngine;
    private final EffectEngine effectEngine;
    private final CommandEngine commandEngine;
    private final SoundEngine soundEngine;

    public ConsumeEngine(
            ConsumableSkillBridgePlugin plugin,
            ItemFactory itemFactory,
            RequirementEngine requirementEngine,
            SkillEngine skillEngine,
            EffectEngine effectEngine,
            CommandEngine commandEngine,
            SoundEngine soundEngine
    ) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.requirementEngine = requirementEngine;
        this.skillEngine = skillEngine;
        this.effectEngine = effectEngine;
        this.commandEngine = commandEngine;
        this.soundEngine = soundEngine;
    }

    public boolean consume(Player player, EquipmentSlot hand, ItemStack handItem, ConsumableDefinition definition, TriggerSource source) {
        return consumeDetailed(player, hand, handItem, definition, source).isSuccess();
    }

    public ConsumeResult consumeDetailed(Player player, EquipmentSlot hand, ItemStack handItem, ConsumableDefinition definition, TriggerSource source) {
        if (hand == null) {
            return ConsumeResult.failed();
        }
        ItemSlotAdapter slotAdapter = new HandItemSlotAdapter(player, hand);
        return consumeInternal(player, hand, slotAdapter, definition, source, null, false);
    }

    public ConsumeResult consumeVanillaEating(Player player, EquipmentSlot hand, ItemStack handItem, ConsumableDefinition definition) {
        if (hand == null) {
            return ConsumeResult.failed();
        }
        ItemSlotAdapter slotAdapter = new HandItemSlotAdapter(player, hand);
        return consumeInternal(player, hand, slotAdapter, definition, TriggerSource.EAT, null, true);
    }

    public boolean consumeOnCursor(Player player, InventoryClickEvent clickEvent, ConsumableDefinition definition, ItemStack targetItem) {
        return consumeOnCursorDetailed(player, clickEvent, definition, targetItem).isSuccess();
    }

    public ConsumeResult consumeOnCursorDetailed(Player player, InventoryClickEvent clickEvent, ConsumableDefinition definition, ItemStack targetItem) {
        if (clickEvent == null) {
            return ConsumeResult.failed();
        }
        ItemSlotAdapter slotAdapter = new CursorItemSlotAdapter(clickEvent);
        return consumeInternal(player, null, slotAdapter, definition, TriggerSource.ON_ITEM, targetItem, false);
    }

    private ConsumeResult consumeInternal(
            Player player,
            EquipmentSlot hand,
            ItemSlotAdapter itemSlot,
            ConsumableDefinition definition,
            TriggerSource source,
            ItemStack targetItem,
            boolean allowVanillaAmountConsume
    ) {
        ItemStack sourceItem = itemSlot.get();
        if (sourceItem == null || sourceItem.getType().isAir()) {
            return ConsumeResult.failed();
        }

        if (source != TriggerSource.ON_ITEM && definition.isInedible()) {
            player.sendMessage(ChatColor.RED + "This consumable cannot be consumed directly.");
            return ConsumeResult.failed();
        }

        RequirementEngine.RequirementResult req = requirementEngine.check(player, definition);
        if (!req.isSuccess()) {
            player.sendMessage(ChatColor.RED + "Cannot use this consumable: " + req.getReason());
            return ConsumeResult.failed();
        }

        String cooldownKey = itemFactory.readCooldownKey(sourceItem, definition);
        long now = System.currentTimeMillis();
        long itemCooldownUntil = itemFactory.readCooldownUntil(sourceItem);
        long playerCooldownUntil = readPlayerCooldownUntil(player, cooldownKey);
        long cooldownUntil = Math.max(itemCooldownUntil, playerCooldownUntil);
        if (cooldownUntil > now) {
            double remaining = (cooldownUntil - now) / 1000D;
            player.sendMessage(ChatColor.RED + "Item is on cooldown: " + String.format("%.1f", remaining) + "s");
            return ConsumeResult.failed();
        }

        MutationPlan mutationPlan = resolveMutationPlan(sourceItem, definition, source);
        ConsumablePreUseEvent preUseEvent = new ConsumablePreUseEvent(
                player,
                definition,
                sourceItem,
                targetItem,
                hand,
                source,
                mutationPlan.shouldConsumeAmount()
        );
        Bukkit.getPluginManager().callEvent(preUseEvent);
        if (preUseEvent.isCancelled()) {
            return ConsumeResult.failed();
        }
        mutationPlan = mutationPlan.withConsumeAmount(preUseEvent.isConsumeAmount(mutationPlan.shouldConsumeAmount()));

        if (!skillEngine.cast(player, definition)) {
            player.sendMessage(ChatColor.RED + "Failed to cast consumable skill.");
            return ConsumeResult.failed();
        }

        boolean letVanillaConsumeAmount = allowVanillaAmountConsume && mutationPlan.shouldConsumeAmount();
        try {
            effectEngine.apply(player, sourceItem, definition, letVanillaConsumeAmount);
            commandEngine.execute(player, definition);
            soundEngine.playConsume(player, definition);
            requirementEngine.applyCosts(player, definition);
        } catch (RuntimeException ex) {
            plugin.warn("Consumable execution failed for item '" + definition.getId() + "': " + ex.getMessage());
            return ConsumeResult.failed();
        }

        if (definition.getCooldownSeconds() > 0D) {
            long cooldownMs = Math.max(1L, Math.round(definition.getCooldownSeconds() * 1000D));
            long cooldownUntilAt = now + cooldownMs;
            itemFactory.writeCooldownUntil(sourceItem, cooldownUntilAt, definition);
            writePlayerCooldownUntil(player, cooldownKey, cooldownUntilAt);
        }

        if (!letVanillaConsumeAmount) {
            applyConsumeMutation(player, itemSlot, sourceItem, definition, mutationPlan);
        }
        Bukkit.getPluginManager().callEvent(new ConsumablePostUseEvent(
                player,
                definition,
                sourceItem,
                targetItem,
                hand,
                source,
                mutationPlan.shouldConsumeAmount(),
                true
        ));
        plugin.debug("source=" + source.name()
                + ", player=" + player.getName()
                + ", item=" + definition.getId()
                + ", cooldownKey=" + cooldownKey);
        return ConsumeResult.success(mutationPlan.shouldConsumeAmount(), letVanillaConsumeAmount);
    }

    private MutationPlan resolveMutationPlan(ItemStack sourceItem, ConsumableDefinition definition, TriggerSource source) {
        int maxConsume = itemFactory.readMaxConsume(sourceItem, definition);
        int usesLeft = itemFactory.readUsesLeft(sourceItem, definition);
        if (usesLeft < 1 || usesLeft > maxConsume) {
            usesLeft = maxConsume;
        }

        if (definition.isDisableRightClickConsume()
                && (source == TriggerSource.RIGHT_CLICK || source == TriggerSource.SHIFT_RIGHT_CLICK || source == TriggerSource.EAT)) {
            return MutationPlan.noConsume(usesLeft, maxConsume);
        }

        boolean consumeAmount = definition.getConsumeBehavior() == ConsumeBehavior.ALWAYS;
        int usesLeftAfter = usesLeft;
        boolean updateUses = false;

        if (!consumeAmount && maxConsume > 1) {
            usesLeftAfter = usesLeft - 1;
            if (usesLeftAfter <= 0) {
                consumeAmount = definition.getConsumeBehavior() != ConsumeBehavior.NEVER;
                usesLeftAfter = usesLeft;
            } else {
                updateUses = true;
            }
        } else if (!consumeAmount && maxConsume <= 1) {
            consumeAmount = definition.getConsumeBehavior() != ConsumeBehavior.NEVER;
        }

        if (consumeAmount) {
            return MutationPlan.consume(usesLeft, maxConsume);
        }
        if (updateUses) {
            return MutationPlan.updateUses(usesLeft, maxConsume, usesLeftAfter);
        }
        return MutationPlan.noConsume(usesLeft, maxConsume);
    }

    private void applyConsumeMutation(
            Player player,
            ItemSlotAdapter itemSlot,
            ItemStack sourceItem,
            ConsumableDefinition definition,
            MutationPlan mutationPlan
    ) {
        if (mutationPlan.shouldConsumeAmount()) {
            consumeOneAmount(itemSlot, sourceItem);
            return;
        }

        if (mutationPlan.shouldUpdateUses()) {
            updateUsesWithSplit(player, itemSlot, sourceItem, definition, mutationPlan.getUsesLeftAfter());
        }
    }

    private void consumeOneAmount(ItemSlotAdapter itemSlot, ItemStack sourceItem) {
        if (sourceItem.getAmount() <= 1) {
            itemSlot.set(null);
            return;
        }

        sourceItem.setAmount(sourceItem.getAmount() - 1);
        itemSlot.set(sourceItem);
    }

    private void updateUsesWithSplit(
            Player player,
            ItemSlotAdapter itemSlot,
            ItemStack sourceItem,
            ConsumableDefinition definition,
            int newUsesLeft
    ) {
        if (sourceItem.getAmount() > 1) {
            ItemStack remaining = sourceItem.clone();
            remaining.setAmount(sourceItem.getAmount() - 1);

            ItemStack usedItem = sourceItem.clone();
            usedItem.setAmount(1);
            itemFactory.writeUsesLeft(usedItem, newUsesLeft, definition);
            itemSlot.set(usedItem);

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(remaining);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            return;
        }

        itemFactory.writeUsesLeft(sourceItem, newUsesLeft, definition);
        itemSlot.set(sourceItem);
    }

    private long readPlayerCooldownUntil(Player player, String cooldownKey) {
        NamespacedKey key = toPlayerCooldownKey(cooldownKey);
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Long found = pdc.get(key, PersistentDataType.LONG);
        return found == null ? 0L : Math.max(0L, found);
    }

    private void writePlayerCooldownUntil(Player player, String cooldownKey, long cooldownUntilEpochMillis) {
        NamespacedKey key = toPlayerCooldownKey(cooldownKey);
        player.getPersistentDataContainer().set(key, PersistentDataType.LONG, Math.max(0L, cooldownUntilEpochMillis));
    }

    private NamespacedKey toPlayerCooldownKey(String cooldownKey) {
        String raw = cooldownKey == null ? "" : cooldownKey.toLowerCase(Locale.ROOT);
        StringBuilder sanitized = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/') {
                sanitized.append(c);
            } else {
                sanitized.append('_');
            }
        }

        String base = sanitized.toString();
        if (base.length() > 64) {
            base = base.substring(0, 64);
        }
        String hash = Integer.toUnsignedString(raw.hashCode(), 16);
        return new NamespacedKey(plugin, "pcd_" + base + "_" + hash);
    }

    public enum TriggerSource {
        RIGHT_CLICK,
        LEFT_CLICK,
        SHIFT_RIGHT_CLICK,
        SHIFT_LEFT_CLICK,
        EAT,
        ON_ITEM
    }

    public static final class ConsumeResult {
        private static final ConsumeResult FAILED = new ConsumeResult(false, false, false);

        private final boolean success;
        private final boolean consumedAmount;
        private final boolean vanillaConsumeAmount;

        private ConsumeResult(boolean success, boolean consumedAmount, boolean vanillaConsumeAmount) {
            this.success = success;
            this.consumedAmount = consumedAmount;
            this.vanillaConsumeAmount = vanillaConsumeAmount;
        }

        public static ConsumeResult failed() {
            return FAILED;
        }

        public static ConsumeResult success(boolean consumedAmount, boolean vanillaConsumeAmount) {
            return new ConsumeResult(true, consumedAmount, vanillaConsumeAmount);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isConsumedAmount() {
            return consumedAmount;
        }

        public boolean shouldVanillaConsumeAmount() {
            return vanillaConsumeAmount;
        }
    }

    private interface ItemSlotAdapter {
        ItemStack get();

        void set(ItemStack stack);
    }

    private static final class HandItemSlotAdapter implements ItemSlotAdapter {
        private final Player player;
        private final EquipmentSlot hand;

        private HandItemSlotAdapter(Player player, EquipmentSlot hand) {
            this.player = player;
            this.hand = hand;
        }

        @Override
        public ItemStack get() {
            return hand == EquipmentSlot.OFF_HAND
                    ? player.getInventory().getItemInOffHand()
                    : player.getInventory().getItemInMainHand();
        }

        @Override
        public void set(ItemStack stack) {
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(stack);
            } else {
                player.getInventory().setItemInMainHand(stack);
            }
        }
    }

    private static final class CursorItemSlotAdapter implements ItemSlotAdapter {
        private final InventoryClickEvent clickEvent;

        private CursorItemSlotAdapter(InventoryClickEvent clickEvent) {
            this.clickEvent = clickEvent;
        }

        @Override
        public ItemStack get() {
            return clickEvent.getCursor();
        }

        @Override
        public void set(ItemStack stack) {
            clickEvent.setCursor(stack);
        }
    }

    private static final class MutationPlan {
        private final int usesLeft;
        private final int maxConsume;
        private final boolean consumeAmount;
        private final boolean updateUses;
        private final int usesLeftAfter;

        private MutationPlan(int usesLeft, int maxConsume, boolean consumeAmount, boolean updateUses, int usesLeftAfter) {
            this.usesLeft = usesLeft;
            this.maxConsume = maxConsume;
            this.consumeAmount = consumeAmount;
            this.updateUses = updateUses;
            this.usesLeftAfter = usesLeftAfter;
        }

        public static MutationPlan consume(int usesLeft, int maxConsume) {
            return new MutationPlan(usesLeft, maxConsume, true, false, usesLeft);
        }

        public static MutationPlan updateUses(int usesLeft, int maxConsume, int usesLeftAfter) {
            return new MutationPlan(usesLeft, maxConsume, false, true, usesLeftAfter);
        }

        public static MutationPlan noConsume(int usesLeft, int maxConsume) {
            return new MutationPlan(usesLeft, maxConsume, false, false, usesLeft);
        }

        public boolean shouldConsumeAmount() {
            return consumeAmount;
        }

        public boolean shouldUpdateUses() {
            return updateUses;
        }

        public int getUsesLeftAfter() {
            return usesLeftAfter;
        }

        public MutationPlan withConsumeAmount(boolean overrideConsumeAmount) {
            if (overrideConsumeAmount == consumeAmount) {
                return this;
            }
            if (overrideConsumeAmount) {
                return consume(usesLeft, maxConsume);
            }
            return noConsume(usesLeft, maxConsume);
        }
    }
}
