package net.tasuposed.projectredacted.network.packets;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Packet that triggers sound playback on the client
 */
public class PlaySoundPacket {
    private final ResourceLocation sound;
    private final SoundSource source;
    private final float volume;
    private final float pitch;
    private final boolean distorted;
    
    public PlaySoundPacket(ResourceLocation sound, SoundSource source, float volume, float pitch, boolean distorted, boolean b) {
        this.sound = sound;
        this.source = source;
        this.volume = volume;
        this.pitch = pitch;
        this.distorted = distorted;
    }
    
    public static void encode(PlaySoundPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.sound);
        buffer.writeEnum(packet.source);
        buffer.writeFloat(packet.volume);
        buffer.writeFloat(packet.pitch);
        buffer.writeBoolean(packet.distorted);
    }
    
    public static PlaySoundPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation sound = buffer.readResourceLocation();
        SoundSource source = buffer.readEnum(SoundSource.class);
        float volume = buffer.readFloat();
        float pitch = buffer.readFloat();
        boolean distorted = buffer.readBoolean();
        return new PlaySoundPacket(sound, source, volume, pitch, distorted, false);
    }
    
    public static void handle(PlaySoundPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Make sure we're on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft minecraft = Minecraft.getInstance();
                SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(packet.sound);
                
                if (soundEvent != null) {
                    minecraft.level.playLocalSound(
                        minecraft.player.getX(),
                        minecraft.player.getY(),
                        minecraft.player.getZ(),
                        soundEvent,
                        packet.source,
                        packet.volume,
                        packet.pitch,
                        false
                    );
                }
            });
        });
        context.setPacketHandled(true);
    }
} 