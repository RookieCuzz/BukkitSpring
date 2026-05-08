package top.skillstone.consumableskillbridge.model;

public class ConsumableCommandSpec {
    private final String format;
    private final boolean console;
    private final boolean op;
    private final double chance;
    private final long delayTicks;

    public ConsumableCommandSpec(String format, boolean console, boolean op, double chance, long delayTicks) {
        this.format = format;
        this.console = console;
        this.op = op;
        this.chance = Math.max(0, Math.min(1, chance));
        this.delayTicks = Math.max(0L, delayTicks);
    }

    public String getFormat() {
        return format;
    }

    public boolean isConsole() {
        return console;
    }

    public boolean isOp() {
        return op;
    }

    public double getChance() {
        return chance;
    }

    public long getDelayTicks() {
        return delayTicks;
    }
}
