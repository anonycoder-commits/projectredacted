package net.tasuposed.projectredacted.horror.events;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tasuposed.projectredacted.ProjectRedacted;
import net.tasuposed.projectredacted.entity.Iteration;
import net.tasuposed.projectredacted.entity.Protocol_37;

/**
 * Handles preventing the player from sleeping when horror entities are nearby
 * or when random horror events might happen
 */
@Mod.EventBusSubscriber(modid = ProjectRedacted.MODID)
public class SleepPreventionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepPreventionHandler.class);
    private static final Random RANDOM = new Random();
    
    // Radius to search for entities
    private static final double DETECTION_RADIUS = 50.0D;
    
    // Random chance of having nightmare for the entire night
    private static final float NIGHTMARE_NIGHT_CHANCE = 0.15F; // 15% chance per night
    
    // Track which players are experiencing a nightmare night
    private static final Map<UUID, Long> playerNightmareNights = new HashMap<>();
    
    /**
     * Sleep prevention event handler
     */
    @SubscribeEvent
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        
        // Only check in survival/adventure mode
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        
        // First check if horror entities are nearby - this always prevents sleep
        if (areHorrorEntitiesNearby(player)) {
            preventSleep(event, player, true);
            return;
        }
        
        // Check if this is a nightmare night for the player
        UUID playerId = player.getUUID();
        Level level = player.level();
        long currentDay = level.getDayTime() / 24000L;
        
        // If not tracked yet for this day, roll to determine if it's a nightmare night
        if (!playerNightmareNights.containsKey(playerId) || playerNightmareNights.get(playerId) < currentDay) {
            boolean isNightmareNight = RANDOM.nextFloat() < NIGHTMARE_NIGHT_CHANCE;
            if (isNightmareNight) {
                playerNightmareNights.put(playerId, currentDay);
                LOGGER.debug("Player {} is experiencing a nightmare night", player.getName().getString());
            } else {
                // Not a nightmare night, allow sleep
                return;
            }
        }
        
        // If we reach here, it's a nightmare night
        if (playerNightmareNights.get(playerId) == currentDay) {
            preventSleep(event, player, false);
        }
    }
    
    /**
     * Prevent player from sleeping and show a message
     */
    private static void preventSleep(PlayerSleepInBedEvent event, Player player, boolean entityNearby) {
        // Prevent sleep
        event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        
        // Choose one of several creepy messages
        int messageChoice = RANDOM.nextInt(8);
        String message;
        
        if (entityNearby) {
            // More intense messages when an entity is actually nearby
            switch (messageChoice) {
                case 0:
                    message = "§4You feel like something is watching you...";
                    break;
                case 1:
                    message = "§4A presence prevents you from sleeping...";
                    break;
                case 2:
                    message = "§4Something is preventing you from sleeping...";
                    break;
                case 3:
                    message = "§4Whispers in the dark keep you awake...";
                    break;
                case 4:
                    message = "§4Something is nearby...";
                    break;
                case 5:
                    message = "§4You sense a presence and can't sleep...";
                    break;
                case 6:
                    message = "§4Your instincts scream danger...";
                    break;
                case 7:
                default:
                    message = "§4You feel a malevolent presence watching you...";
                    break;
            }
        } else {
            // More subtle messages for nightmare nights
            switch (messageChoice) {
                case 0:
                    message = "§7You feel uneasy and can't fall asleep...";
                    break;
                case 1:
                    message = "§7A strange sense of dread prevents you from sleeping...";
                    break;
                case 2:
                    message = "§7Nightmares await if you try to sleep...";
                    break;
                case 3:
                    message = "§7Something in your mind keeps you awake...";
                    break;
                case 4:
                    message = "§7Your mind races with disturbing thoughts...";
                    break;
                case 5:
                    message = "§7You feel restless tonight...";
                    break;
                case 6:
                    message = "§7Your instincts tell you it's not safe to sleep now...";
                    break;
                case 7:
                default:
                    message = "§7An unexplainable feeling of danger keeps you awake...";
                    break;
            }
        }
        
        player.displayClientMessage(Component.literal(message), true);
        
        // Log for debugging
        if (entityNearby) {
            LOGGER.debug("Prevented player {} from sleeping due to nearby horror entities", player.getName().getString());
        } else {
            LOGGER.debug("Prevented player {} from sleeping due to nightmare night", player.getName().getString());
        }
    }
    
    /**
     * Check world time changes to track when night begins/ends
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Clean up old nightmare nights periodically to prevent memory leaks
            if (event.level.getGameTime() % 1200 == 0) { // Every minute
                long currentDay = event.level.getDayTime() / 24000L;
                playerNightmareNights.entrySet().removeIf(entry -> entry.getValue() < currentDay);
            }
        }
    }
    
    /**
     * Check if horror entities are nearby the player
     */
    private static boolean areHorrorEntitiesNearby(Player player) {
        // Check area around player for horror entities
        AABB searchBox = player.getBoundingBox().inflate(DETECTION_RADIUS);
        
        // Check for Iteration entities first
        List<Iteration> iterations = player.level().getEntitiesOfClass(
                Iteration.class, 
                searchBox);
        
        if (!iterations.isEmpty()) {
            return true; // Found Iteration entity nearby
        }
        
        // Check for Protocol_37 entities
        List<Protocol_37> protocol37s = player.level().getEntitiesOfClass(
                Protocol_37.class, 
                searchBox);
        
        if (!protocol37s.isEmpty()) {
            return true; // Found Protocol_37 entity nearby
        }
        
        return false; // No horror entities found nearby
    }
} 