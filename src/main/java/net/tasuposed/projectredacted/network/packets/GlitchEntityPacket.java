package net.tasuposed.projectredacted.network.packets;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet for making an entity appear glitched on the client
 */
public class GlitchEntityPacket {
    private final int entityId;
    
    public GlitchEntityPacket(int entityId) {
        this.entityId = entityId;
    }
    
    public static void encode(GlitchEntityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
    }
    
    public static GlitchEntityPacket decode(FriendlyByteBuf buffer) {
        return new GlitchEntityPacket(buffer.readInt());
    }
    
    public static void handle(GlitchEntityPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Make sure we're on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet));
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static void handleClient(GlitchEntityPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level.getEntity(packet.entityId);
        
        if (entity != null) {
            // Apply client-side effects to make entity appear glitched
            // This could set custom rendering data on the entity
            
            // For demonstration, we'll just make the entity glow
            entity.setGlowingTag(true);
            
            // In a full implementation, we'd have a client-side handler
            // that applies custom rendering to this entity
        }
    }
} 