package net.tasuposed.projectredacted.network.packets;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.tasuposed.projectredacted.client.effects.ScreenEffectHandler;

/**
 * Packet for temporarily changing the client's render distance
 */
public class RenderDistancePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderDistancePacket.class);
    
    private final int renderDistance; // New render distance in chunks
    private final int duration;       // Duration in ticks
    private final boolean fadeEffect; // Whether to apply a fog fade effect
    
    public RenderDistancePacket(int renderDistance, int duration, boolean fadeEffect) {
        this.renderDistance = renderDistance;
        this.duration = duration;
        this.fadeEffect = fadeEffect;
    }
    
    public static void encode(RenderDistancePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.renderDistance);
        buffer.writeInt(packet.duration);
        buffer.writeBoolean(packet.fadeEffect);
    }
    
    public static RenderDistancePacket decode(FriendlyByteBuf buffer) {
        return new RenderDistancePacket(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readBoolean()
        );
    }
    
    public static void handle(RenderDistancePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Make sure we're on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet));
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static void handleClient(RenderDistancePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        
        try {
            // Store the current render distance to restore later
            final int originalRenderDistance = minecraft.options.renderDistance().get();
            
            // Ensure the requested render distance is within reasonable limits
            int safeRenderDistance = Math.max(2, Math.min(packet.renderDistance, 32));
            
            // Apply the new render distance
            minecraft.options.renderDistance().set(safeRenderDistance);
            
            // Apply an additional screen effect if requested
            if (packet.fadeEffect && ScreenEffectHandler.INSTANCE != null) {
                float intensity = 0.7f;
                ScreenEffectHandler.INSTANCE.startEffect(ScreenEffectHandler.EFFECT_STATIC, intensity, 20);
            }
            
            // Create reference holder for original distance to prevent garbage collection issues
            final int[] originalDistanceRef = new int[] { originalRenderDistance };
            
            // Schedule restoration using Forge's event system with additional safeguards
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Object() {
                private int ticksRemaining = packet.duration;
                private boolean isRestored = false;
                
                @net.minecraftforge.eventbus.api.SubscribeEvent
                public void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
                    if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                        // Decrease the timer
                        if (--ticksRemaining <= 0 && !isRestored) {
                            // Time to restore the render distance
                            try {
                                minecraft.options.renderDistance().set(originalDistanceRef[0]);
                                
                                // Apply a brief visual effect when restoring normal view
                                if (packet.fadeEffect && ScreenEffectHandler.INSTANCE != null) {
                                    ScreenEffectHandler.INSTANCE.startEffect(ScreenEffectHandler.EFFECT_STATIC, 0.4f, 15);
                                }
                                
                                LOGGER.debug("Restored original render distance: {}", originalDistanceRef[0]);
                            } catch (Exception e) {
                                LOGGER.error("Error restoring render distance", e);
                            } finally {
                                // Mark as restored regardless of success to prevent repeated attempts
                                isRestored = true;
                                
                                // Unregister this handler
                                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
                            }
                        }
                    }
                }
            });
            
            LOGGER.debug("Changed render distance from {} to {} for {} ticks", 
                    originalRenderDistance, safeRenderDistance, packet.duration);
            
        } catch (Exception e) {
            LOGGER.error("Failed to change render distance", e);
        }
    }
} 