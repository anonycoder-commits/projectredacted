package net.tasuposed.projectredacted.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.tasuposed.projectredacted.client.effects.ScreenEffectHandler;

import java.util.function.Supplier;

/**
 * Packet to display a fake crash screen with a custom message
 */
public class FakeCrashPacket {
    private final String crashMessage;
    
    public FakeCrashPacket(String crashMessage) {
        this.crashMessage = crashMessage;
    }
    
    /**
     * Read packet data from buffer
     */
    public static FakeCrashPacket decode(FriendlyByteBuf buf) {
        return new FakeCrashPacket(buf.readUtf());
    }
    
    /**
     * Write packet data to buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(crashMessage);
    }
    
    /**
     * Handle the packet
     */
    public static void handle(FakeCrashPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Only process on client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleOnClient(packet));
        });
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * Client-side handler
     */
    private static void handleOnClient(FakeCrashPacket packet) {
        // Get the crash message from the packet
        String message = packet.crashMessage;
        
        // Use the screen effect handler to display a fake crash screen
        ScreenEffectHandler.displayFakeCrashScreen(message);
    }
} 