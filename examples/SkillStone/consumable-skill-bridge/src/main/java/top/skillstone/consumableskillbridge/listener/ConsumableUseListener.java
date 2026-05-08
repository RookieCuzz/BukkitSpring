package top.skillstone.consumableskillbridge.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import top.skillstone.consumableskillbridge.ConsumableSkillBridgePlugin;
import top.skillstone.consumableskillbridge.engine.ConsumeEngine;
import top.skillstone.consumableskillbridge.factory.ItemFactory;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;
import top.skillstone.consumableskillbridge.model.TriggerMode;

public class ConsumableUseListener implements Listener {
    private final ConsumableSkillBridgePlugin plugin;
    private final ItemFactory itemFactory;
    private final ConsumeEngine consumeEngine;

    public ConsumableUseListener(ConsumableSkillBridgePlugin plugin, ItemFactory itemFactory, ConsumeEngine consumeEngine) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.consumeEngine = consumeEngine;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.isBridgeEnabled()) {
            return;
        }
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        InteractTrigger interactTrigger = resolveInteractTrigger(event);
        if (interactTrigger == null) {
            return;
        }

        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack handItem = hand == EquipmentSlot.OFF_HAND ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType().isAir()) {
            return;
        }

        ConsumableDefinition definition = itemFactory.resolveDefinition(handItem);
        if (definition == null) {
            return;
        }
        if (definition.isVanillaEating()) {
            return;
        }
        if (definition.getTrigger() != interactTrigger.mode) {
            return;
        }
        if (event.hasBlock()
                && plugin.isDisableConsumableBlockClicks()
                && event.getClickedBlock() != null
                && event.getClickedBlock().getType().isInteractable()) {
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
        consumeEngine.consume(player, hand, handItem, definition, interactTrigger.source);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!plugin.isBridgeEnabled()) {
            return;
        }
        ItemStack eventItem = event.getItem();
        if (eventItem == null || eventItem.getType().isAir()) {
            return;
        }

        ConsumableDefinition definition = itemFactory.resolveDefinition(eventItem);
        if (definition == null) {
            return;
        }
        if (definition.getTrigger() != TriggerMode.EAT && !definition.isVanillaEating()) {
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = resolveConsumeHand(player, event.getHand(), definition.getId());
        ItemStack handItem = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType().isAir()) {
            event.setCancelled(true);
            return;
        }

        ConsumeEngine.ConsumeResult result = definition.isVanillaEating()
                ? consumeEngine.consumeVanillaEating(player, hand, handItem, definition)
                : consumeEngine.consumeDetailed(player, hand, handItem, definition, ConsumeEngine.TriggerSource.EAT);

        if (!result.isSuccess()) {
            event.setCancelled(true);
            return;
        }

        if (!definition.isVanillaEating()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(!result.shouldVanillaConsumeAmount());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUseOnItem(InventoryClickEvent event) {
        if (!plugin.isBridgeEnabled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) {
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return;
        }

        ConsumableDefinition definition = itemFactory.resolveDefinition(cursor);
        if (definition == null || !definition.getUseOnItem().isEnabled()) {
            return;
        }
        if (definition.getUseOnItem().isPlayerInventoryOnly() && event.getClickedInventory() != event.getWhoClicked().getInventory()) {
            return;
        }

        ItemStack target = event.getCurrentItem();
        if (!definition.getUseOnItem().matchesTarget(target)) {
            return;
        }

        event.setCancelled(true);
        consumeEngine.consumeOnCursor((Player) event.getWhoClicked(), event, definition, target);
    }

    private EquipmentSlot resolveConsumeHand(Player player, EquipmentSlot preferred, String expectedItemId) {
        if (preferred != null) {
            ItemStack preferredItem = preferred == EquipmentSlot.OFF_HAND
                    ? player.getInventory().getItemInOffHand()
                    : player.getInventory().getItemInMainHand();
            if (isExpectedItem(preferredItem, expectedItemId)) {
                return preferred;
            }
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        if (isExpectedItem(main, expectedItemId)) {
            return EquipmentSlot.HAND;
        }

        ItemStack off = player.getInventory().getItemInOffHand();
        if (isExpectedItem(off, expectedItemId)) {
            return EquipmentSlot.OFF_HAND;
        }

        return preferred == EquipmentSlot.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
    }

    private boolean isExpectedItem(ItemStack item, String expectedItemId) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        String found = itemFactory.readItemId(item);
        return found != null && found.equalsIgnoreCase(expectedItemId);
    }

    private InteractTrigger resolveInteractTrigger(PlayerInteractEvent event) {
        Action action = event.getAction();
        boolean sneaking = event.getPlayer().isSneaking();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            return sneaking
                    ? new InteractTrigger(TriggerMode.SHIFT_RIGHT_CLICK, ConsumeEngine.TriggerSource.SHIFT_RIGHT_CLICK)
                    : new InteractTrigger(TriggerMode.RIGHT_CLICK, ConsumeEngine.TriggerSource.RIGHT_CLICK);
        }
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            return sneaking
                    ? new InteractTrigger(TriggerMode.SHIFT_LEFT_CLICK, ConsumeEngine.TriggerSource.SHIFT_LEFT_CLICK)
                    : new InteractTrigger(TriggerMode.LEFT_CLICK, ConsumeEngine.TriggerSource.LEFT_CLICK);
        }
        return null;
    }

    private static final class InteractTrigger {
        private final TriggerMode mode;
        private final ConsumeEngine.TriggerSource source;

        private InteractTrigger(TriggerMode mode, ConsumeEngine.TriggerSource source) {
            this.mode = mode;
            this.source = source;
        }
    }
}
