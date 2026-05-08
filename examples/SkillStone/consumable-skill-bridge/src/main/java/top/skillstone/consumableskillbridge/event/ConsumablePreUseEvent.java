package top.skillstone.consumableskillbridge.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import top.skillstone.consumableskillbridge.engine.ConsumeEngine;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;

public class ConsumablePreUseEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ConsumableDefinition definition;
    private final ItemStack sourceItem;
    private final ItemStack targetItem;
    private final EquipmentSlot hand;
    private final ConsumeEngine.TriggerSource source;
    private Boolean consumeAmount;
    private boolean cancelled;

    public ConsumablePreUseEvent(
            Player player,
            ConsumableDefinition definition,
            ItemStack sourceItem,
            ItemStack targetItem,
            EquipmentSlot hand,
            ConsumeEngine.TriggerSource source,
            boolean defaultConsumeAmount
    ) {
        this.player = player;
        this.definition = definition;
        this.sourceItem = sourceItem;
        this.targetItem = targetItem;
        this.hand = hand;
        this.source = source;
        this.consumeAmount = defaultConsumeAmount;
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

    public Boolean getConsumeAmount() {
        return consumeAmount;
    }

    public void setConsumeAmount(Boolean consumeAmount) {
        this.consumeAmount = consumeAmount;
    }

    public boolean isConsumeAmount(boolean defaultValue) {
        return consumeAmount == null ? defaultValue : consumeAmount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
