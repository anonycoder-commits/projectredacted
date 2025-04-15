package net.tasuposed.projectredacted.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.tasuposed.projectredacted.ProjectRedacted;

/**
 * General mod configuration settings
 */
@Mod.EventBusSubscriber(modid = ProjectRedacted.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ForgeConfigSpec SPEC;
    
    public static final BooleanValue DEBUG_MODE;
    public static final BooleanValue SHOW_MESSAGES;
    
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        
        builder.comment("General Settings")
               .push("general");
        
        DEBUG_MODE = builder
                .comment("Enable debug mode with additional logging")
                .define("debugMode", false);
        
        SHOW_MESSAGES = builder
                .comment("Show mod messages in chat")
                .define("showMessages", true);
        
        builder.pop();
        
        SPEC = builder.build();
    }
    
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        // Only mark as loaded if this is the right config file
        if (event.getConfig().getSpec() == SPEC) {
            // Mark config as loaded
            net.tasuposed.projectredacted.horror.HorrorManager.markConfigLoaded();
        }
    }
} 