package net.tasuposed.projectredacted.network.packets;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.tasuposed.projectredacted.client.effects.TextureManager;

/**
 * Packet for creating texture glitch effects on the client
 */
public class TextureGlitchPacket {
    private final int glitchType;
    private final int duration;
    
    public TextureGlitchPacket(int glitchType, int duration) {
        this.glitchType = glitchType;
        this.duration = duration;
    }
    
    public static void encode(TextureGlitchPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.glitchType);
        buffer.writeInt(packet.duration);
    }
    
    public static TextureGlitchPacket decode(FriendlyByteBuf buffer) {
        return new TextureGlitchPacket(
                buffer.readInt(),
                buffer.readInt()
        );
    }
    
    public static void handle(TextureGlitchPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Make sure we're on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet));
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static void handleClient(TextureGlitchPacket packet) {
        // In a full implementation, we'd have a client-side texture manager
        // that would apply glitch effects to textures
        // For now, we'll use a placeholder implementation
        
        // Initialize texture manager if needed
        if (TextureManager.INSTANCE == null) {
            TextureManager.init();
        }
        
        // Apply the texture glitch effect
        TextureManager.INSTANCE.applyGlitch(packet.glitchType, packet.duration);
    }
} 