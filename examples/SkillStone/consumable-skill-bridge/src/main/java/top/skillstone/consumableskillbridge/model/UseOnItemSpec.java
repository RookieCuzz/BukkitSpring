package top.skillstone.consumableskillbridge.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class UseOnItemSpec {
    private final boolean enabled;
    private final boolean playerInventoryOnly;
    private final boolean allowAirTarget;
    private final Set<Material> allowedTargetMaterials;
    private final Set<Material> deniedTargetMaterials;

    public UseOnItemSpec(
            boolean enabled,
            boolean playerInventoryOnly,
            boolean allowAirTarget,
            Set<Material> allowedTargetMaterials,
            Set<Material> deniedTargetMaterials
    ) {
        this.enabled = enabled;
        this.playerInventoryOnly = playerInventoryOnly;
        this.allowAirTarget = allowAirTarget;
        this.allowedTargetMaterials = sanitize(allowedTargetMaterials);
        this.deniedTargetMaterials = sanitize(deniedTargetMaterials);
    }

    public static UseOnItemSpec disabled() {
        return new UseOnItemSpec(false, true, false, Collections.emptySet(), Collections.emptySet());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPlayerInventoryOnly() {
        return playerInventoryOnly;
    }

    public boolean isAllowAirTarget() {
        return allowAirTarget;
    }

    public Set<Material> getAllowedTargetMaterials() {
        return allowedTargetMaterials;
    }

    public Set<Material> getDeniedTargetMaterials() {
        return deniedTargetMaterials;
    }

    public boolean matchesTarget(ItemStack target) {
        if (target == null || target.getType().isAir()) {
            return allowAirTarget;
        }

        Material type = target.getType();
        if (!allowedTargetMaterials.isEmpty() && !allowedTargetMaterials.contains(type)) {
            return false;
        }
        return deniedTargetMaterials.isEmpty() || !deniedTargetMaterials.contains(type);
    }

    private Set<Material> sanitize(Set<Material> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Material> out = new HashSet<>();
        for (Material material : input) {
            if (material == null || material == Material.AIR) {
                continue;
            }
            out.add(material);
        }
        return Collections.unmodifiableSet(out);
    }

    @Override
    public String toString() {
        return "UseOnItemSpec{"
                + "enabled=" + enabled
                + ", playerInventoryOnly=" + playerInventoryOnly
                + ", allowAirTarget=" + allowAirTarget
                + ", allowedTargetMaterials=" + toCompactString(allowedTargetMaterials)
                + ", deniedTargetMaterials=" + toCompactString(deniedTargetMaterials)
                + '}';
    }

    private String toCompactString(Set<Material> materials) {
        if (materials.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (Material material : materials) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append(material.name().toLowerCase(Locale.ROOT));
        }
        builder.append(']');
        return builder.toString();
    }
}
