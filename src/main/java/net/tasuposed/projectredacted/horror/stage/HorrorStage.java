package net.tasuposed.projectredacted.horror.stage;

import net.minecraft.world.entity.player.Player;

/**
 * Represents a stage of horror progression with its own events
 */
public interface HorrorStage {
    /**
     * Trigger a random horror event for this stage
     */
    void triggerRandomEvent(Player player);
} 