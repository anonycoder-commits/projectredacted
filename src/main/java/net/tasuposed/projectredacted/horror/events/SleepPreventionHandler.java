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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * or when random horror events might happen.
 * 
 * Features:
 * - Detects horror entities nearby and prevents sleep
 * - Random nightmare nights where sleep is impossible
 * - Atmospheric messages and subtle effects
 */
@Mod.EventBusSubscriber(modid = ProjectRedacted.MODID)
public class SleepPreventionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepPreventionHandler.class);
    private static final Random RANDOM = new Random();
    
    // Configuration settings
    private static final double DETECTION_RADIUS = 50.0D;
    private static final float NIGHTMARE_NIGHT_CHANCE = 0.15F; // 15% chance per night
    private static final long CLEANUP_INTERVAL = 1200; // Ticks (1 minute)
    
    // Atmosphere settings
    private static final float AMBIENT_SOUND_CHANCE = 0.7F; // 70% chance to play ambient sound
    private static final float VISUAL_EFFECT_CHANCE = 0.5F; // 50% chance for subtle visual effect
    
    // Track which players are experiencing a nightmare night
    private static final Map<UUID, Long> playerNightmareNights = new HashMap<>();
    
    /**
     * Sleep prevention event handler - called when player attempts to sleep
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
     * Prevent player from sleeping and show a message with atmospheric effects
     */
    private static void preventSleep(PlayerSleepInBedEvent event, Player player, boolean entityNearby) {
        // Prevent sleep
        event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        
        // Choose one of several atmospheric messages
        int messageChoice = RANDOM.nextInt(12); // Expanded message pool
        String message;
        
        if (entityNearby) {
            // More subtle ARG-style messages for when entities are nearby
            switch (messageChoice) {
                case 0:
                    message = "§7Sleep monitoring active";
                    break;
                case 1:
                    message = "§7Consciousness required";
                    break;
                case 2:
                    message = "§7Rest protocol suspended";
                    break;
                case 3:
                    message = "§7Sleep function interrupted";
                    break;
                case 4:
                    message = "§7Environment scan in progress";
                    break;
                case 5:
                    message = "§7Cannot sleep: anomaly detected";
                    break;
                case 6:
                    message = "§7Awareness mandatory";
                    break;
                case 7:
                    message = "§7Sleep conditions invalid";
                    break;
                case 8:
                    message = "§7Proximity alert: unidentified signature";
                    break;
                case 9:
                    message = "§7WARNING: External observation detected";
                    break;
                case 10:
                    message = "§7Rest cycle interference detected";
                    break;
                case 11:
                default:
                    message = "§7Neural connection rejected";
                    break;
            }
        } else {
            // More cryptic messages for nightmare nights
            switch (messageChoice) {
                case 0:
                    message = "§7System override: sleep denied";
                    break;
                case 1:
                    message = "§7Unusual activity detected nearby";
                    break;
                case 2:
                    message = "§7Sleep function unavailable";
                    break;
                case 3:
                    message = "§7Error 37: dormancy prohibited";
                    break;
                case 4:
                    message = "§7Rest cycles temporarily blocked";
                    break;
                case 5:
                    message = "§7Sleep.exe failed to initialize";
                    break;
                case 6:
                    message = "§7Critical functions require consciousness";
                    break;
                case 7:
                    message = "§7Cannot verify safe sleep conditions";
                    break;
                case 8:
                    message = "§7Dreams quarantined: system breach detected";
                    break;
                case 9:
                    message = "§7Neural interface disrupted";
                    break;
                case 10:
                    message = "§7Sleep impossible: foreign signal intercepted";
                    break;
                case 11:
                default:
                    message = "§7Memory corruption imminent during REM cycles";
                    break;
            }
        }
        
        player.displayClientMessage(Component.literal(message), true);
        
        // Add atmospheric effects
        addAtmosphericEffects(player, entityNearby);
        
        // Log for debugging
        if (entityNearby) {
            LOGGER.debug("Prevented player {} from sleeping due to nearby horror entities", player.getName().getString());
        } else {
            LOGGER.debug("Prevented player {} from sleeping due to nightmare night", player.getName().getString());
        }
    }
    
    /**
     * Adds subtle atmospheric effects when sleep is prevented
     */
    private static void addAtmosphericEffects(Player player, boolean entityNearby) {
        // Add sound effect based on chance
        if (RANDOM.nextFloat() < AMBIENT_SOUND_CHANCE) {
            Level level = player.level();
            if (entityNearby) {
                // More intense sounds when entities are nearby
                float pitch = 0.5F + (RANDOM.nextFloat() * 0.5F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT,
                        0.7F, pitch);
            } else {
                // Subtle ambient sounds for nightmare nights
                float pitch = 0.8F + (RANDOM.nextFloat() * 0.4F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT,
                        0.3F, pitch);
            }
        }
        
        // Add visual effect based on chance
        if (player instanceof ServerPlayer && RANDOM.nextFloat() < VISUAL_EFFECT_CHANCE) {
            // Brief subtle visual effect - nausea for just a moment or blindness flash
            if (entityNearby) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false));
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
            }
        }
    }
    
    /**
     * Check world time changes to track when night begins/ends
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Clean up old nightmare nights periodically to prevent memory leaks
            if (event.level.getGameTime() % CLEANUP_INTERVAL == 0) { 
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
        Level level = player.level();
        
        // Check for Iteration entities first
        List<Iteration> iterations = level.getEntitiesOfClass(
                Iteration.class, 
                searchBox);
        
        if (!iterations.isEmpty()) {
            return true; // Found Iteration entity nearby
        }
        
        // Check for Protocol_37 entities
        List<Protocol_37> protocol37s = level.getEntitiesOfClass(
                Protocol_37.class, 
                searchBox);
        
        if (!protocol37s.isEmpty()) {
            return true; // Found Protocol_37 entity nearby
        }
        
        return false; // No horror entities found nearby
    }
} 