package top.skillstone.consumableskillbridge.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import top.skillstone.consumableskillbridge.engine.ConsumeEngine;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;

public class ConsumablePostUseEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ConsumableDefinition definition;
    private final ItemStack sourceItem;
    private final ItemStack targetItem;
    private final EquipmentSlot hand;
    private final ConsumeEngine.TriggerSource source;
    private final boolean consumedAmount;
    private final boolean success;

    public ConsumablePostUseEvent(
            Player player,
            ConsumableDefinition definition,
            ItemStack sourceItem,
            ItemStack targetItem,
            EquipmentSlot hand,
            ConsumeEngine.TriggerSource source,
            boolean consumedAmount,
            boolean success
    ) {
        this.player = player;
        this.definition = definition;
        this.sourceItem = sourceItem;
        this.targetItem = targetItem;
        this.hand = hand;
        this.source = source;
        this.consumedAmount = consumedAmount;
        this.success = success;
    }

    public Player getPlayer() {
        return player;
    }

    public ConsumableDefinition getDefinition() {
        return definition;
    }

    public ItemStack getSourceItem() {
        return sourceItem;
    }

    public ItemStack getTargetItem() {
        return targetItem;
    }

    public EquipmentSlot getHand() {
        return hand;
    }

    public ConsumeEngine.TriggerSource getSource() {
        return source;
    }

    public boolean isConsumedAmount() {
        return consumedAmount;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
