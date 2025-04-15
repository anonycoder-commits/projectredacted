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
        Minecraft minecraft = Minecraft.getInstance();
        
        if (minecraft.player == null) {
            return;
        }
        
        Inventory inventory = minecraft.player.getInventory();
        
        // We'll shift more items (4-8) instead of just 2-5
        int itemsToShift = 4 + random.nextInt(5);
        
        // Get non-empty slots to potentially shift
        List<Integer> nonEmptySlots = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!inventory.getItem(i).isEmpty()) {
                nonEmptySlots.add(i);
            }
        }
        
        // If we don't have enough items, reduce the number to shift
        itemsToShift = Math.min(itemsToShift, nonEmptySlots.size() / 2);
        
        // Shuffle the non-empty slots
        Collections.shuffle(nonEmptySlots);
        
        // Swap pairs of items
        for (int i = 0; i < itemsToShift && i * 2 + 1 < nonEmptySlots.size(); i++) {
            int slotA = nonEmptySlots.get(i * 2);
            int slotB = nonEmptySlots.get(i * 2 + 1);
            
            // Get the items
            ItemStack itemA = inventory.getItem(slotA).copy();
            ItemStack itemB = inventory.getItem(slotB).copy();
            
            // Swap them
            inventory.setItem(slotA, itemB);
            inventory.setItem(slotB, itemA);
        }
        
        // 35% chance to also play a creepy sound for more impact
        if (random.nextFloat() < 0.35f) {
            minecraft.level.playLocalSound(
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ(),
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.HOSTILE,
                0.5f,
                0.5f + random.nextFloat() * 0.5f,
                false
            );
        }
    }
} 