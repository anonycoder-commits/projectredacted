package net.tasuposed.projectredacted.network.packets;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.tasuposed.projectredacted.client.effects.ScreenEffectHandler;

/**
 * Packet that triggers screen glitch effects on the client
 */
public class GlitchScreenPacket {
    private final int effectType;
    private final float intensity;
    private final int duration;
    
    public GlitchScreenPacket(int effectType, float intensity, int duration) {
        this.effectType = effectType;
        this.intensity = intensity;
        this.duration = duration;
    }
    
    public static void encode(GlitchScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.effectType);
        buffer.writeFloat(packet.intensity);
        buffer.writeInt(packet.duration);
    }
    
    public static GlitchScreenPacket decode(FriendlyByteBuf buffer) {
        int effectType = buffer.readInt();
        float intensity = buffer.readFloat();
        int duration = buffer.readInt();
        return new GlitchScreenPacket(effectType, intensity, duration);
    }
    
    public static void handle(GlitchScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Handle on the client side
            ScreenEffectHandler.addGlitchEffect(packet.effectType, packet.intensity, packet.duration);
        });
        context.setPacketHandled(true);
    }
} 