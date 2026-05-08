package top.skillstone.consumableskillbridge.model;

public class RequirementSpec {
    private final String permission;
    private final int minLevel;
    private final int minFood;
    private final double minMana;
    private final double minStamina;
    private final double manaCost;
    private final double staminaCost;

    public RequirementSpec(
            String permission,
            int minLevel,
            int minFood,
            double minMana,
            double minStamina,
            double manaCost,
            double staminaCost
    ) {
        this.permission = permission == null ? "" : permission;
        this.minLevel = Math.max(0, minLevel);
        this.minFood = Math.max(0, minFood);
        this.minMana = Math.max(0D, minMana);
        this.minStamina = Math.max(0D, minStamina);
        this.manaCost = Math.max(0D, manaCost);
        this.staminaCost = Math.max(0D, staminaCost);
    }

    public String getPermission() {
        return permission;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMinFood() {
        return minFood;
    }

    public double getMinMana() {
        return minMana;
    }

    public double getMinStamina() {
        return minStamina;
    }

    public double getManaCost() {
        return manaCost;
    }

    public double getStaminaCost() {
        return staminaCost;
    }

    public boolean hasResourceRequirementOrCost() {
        return minMana > 0D || minStamina > 0D || manaCost > 0D || staminaCost > 0D;
    }
}
