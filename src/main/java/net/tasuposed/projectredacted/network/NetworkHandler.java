package net.tasuposed.projectredacted.network;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.tasuposed.projectredacted.ProjectRedacted;
import net.tasuposed.projectredacted.network.packets.FakeCrashPacket;
import net.tasuposed.projectredacted.network.packets.GlitchEntityPacket;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;
import net.tasuposed.projectredacted.network.packets.InventoryShiftPacket;
import net.tasuposed.projectredacted.network.packets.PlaySoundPacket;
import net.tasuposed.projectredacted.network.packets.RenderDistancePacket;
import net.tasuposed.projectredacted.network.packets.TextureGlitchPacket;

/**
 * Handles registration and sending of network packets
 */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ProjectRedacted.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int id = 0;
    
    /**
     * Register all packets
     */
    public static void registerPackets() {
        // Register each packet with a unique ID
        CHANNEL.registerMessage(id++, GlitchEntityPacket.class, 
                GlitchEntityPacket::encode, 
                GlitchEntityPacket::decode, 
                GlitchEntityPacket::handle);
        
        CHANNEL.registerMessage(id++, GlitchScreenPacket.class, 
                GlitchScreenPacket::encode, 
                GlitchScreenPacket::decode, 
                GlitchScreenPacket::handle);
        
        CHANNEL.registerMessage(id++, PlaySoundPacket.class, 
                PlaySoundPacket::encode, 
                PlaySoundPacket::decode, 
                PlaySoundPacket::handle);
                
        CHANNEL.registerMessage(id++, TextureGlitchPacket.class, 
                TextureGlitchPacket::encode, 
                TextureGlitchPacket::decode, 
                TextureGlitchPacket::handle);
                
        CHANNEL.registerMessage(id++, FakeCrashPacket.class, 
                FakeCrashPacket::encode, 
                FakeCrashPacket::decode, 
                FakeCrashPacket::handle);
                
        CHANNEL.registerMessage(id++, InventoryShiftPacket.class, 
                InventoryShiftPacket::encode, 
                InventoryShiftPacket::decode, 
                InventoryShiftPacket::handle);
                
        // Register our new render distance packet
        CHANNEL.registerMessage(id++, RenderDistancePacket.class, 
                RenderDistancePacket::encode, 
                RenderDistancePacket::decode, 
                RenderDistancePacket::handle);
    }
    
    /**
     * Send a packet to a specific player
     */
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
    
    /**
     * Send a packet to all players
     */
    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
    
    /**
     * Send a packet to the server
     */
    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
    
    /**
     * Send a packet to all players in a certain dimension
     */
    public static void sendToDimension(Object packet, ResourceKey<Level> dimensionKey) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimensionKey), packet);
    }
} 