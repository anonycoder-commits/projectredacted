package net.tasuposed.projectredacted.network.packets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet to randomly shift items in the player's inventory
 */
public class InventoryShiftPacket {
    private static final Random random = new Random();
    
    // Empty constructor as we don't need to send any data
    public InventoryShiftPacket() {
    }
    
    /**
     * Read packet data from buffer (no data needed)
     */
    public static InventoryShiftPacket decode(FriendlyByteBuf buf) {
        return new InventoryShiftPacket();
    }
    
    /**
     * Write packet data to buffer (no data needed)
     */
    public void encode(FriendlyByteBuf buf) {
        // No data to encode
    }
    
    /**
     * Handle the packet
     */
    public static void handle(InventoryShiftPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Only process on client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleOnClient(packet));
        });
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * Client-side handler
     */
    private static void handleOnClient(InventoryShiftPacket packet) {
        try {
            // Get client and player
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            
            Inventory inventory = mc.player.getInventory();
            
            // Create a list of all inventory items
            List<ItemStack> hotbarItems = new ArrayList<>();
            List<ItemStack> mainInventoryItems = new ArrayList<>();
            
            // Split inventory into hotbar and main sections
            for (int i = 0; i < 9; i++) {
                hotbarItems.add(inventory.getItem(i).copy());
            }
            
            for (int i = 9; i < 36; i++) {
                mainInventoryItems.add(inventory.getItem(i).copy());
            }
            
            // Log inventory status before shifting
            System.out.println("[InventoryShift] Starting inventory shift with " + 
                hotbarItems.size() + " hotbar items and " + 
                mainInventoryItems.size() + " main inventory items");
            
            // Shuffle each section separately for more controlled chaos
            Collections.shuffle(hotbarItems, random);
            Collections.shuffle(mainInventoryItems, random);
            
            // Redistribute items
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, hotbarItems.get(i));
            }
            
            for (int i = 0; i < mainInventoryItems.size(); i++) {
                inventory.setItem(i + 9, mainInventoryItems.get(i));
            }
            
            // Play UI sound to indicate something happened
            mc.getSoundManager().play(new net.minecraft.client.resources.sounds.SimpleSoundInstance(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 
                    net.minecraft.sounds.SoundSource.MASTER, 
                    0.5F, 
                    1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F, 
                    random.nextLong(), 
                    null));
            
            // Log success
            System.out.println("[InventoryShift] Successfully completed inventory shift");
        } catch (Exception e) {
            // Log any errors that occur during shifting
            System.err.println("[InventoryShift] Error shifting inventory: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 