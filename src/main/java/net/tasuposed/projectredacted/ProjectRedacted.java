package net.tasuposed.projectredacted;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.tasuposed.projectredacted.client.EntityRendererRegistry;
import net.tasuposed.projectredacted.client.effects.ScreenEffectHandler;
import net.tasuposed.projectredacted.command.HorrorCommands;
import net.tasuposed.projectredacted.config.Config;
import net.tasuposed.projectredacted.config.HorrorConfig;
import net.tasuposed.projectredacted.entity.EntityRegistry;
import net.tasuposed.projectredacted.entity.Iteration;
import net.tasuposed.projectredacted.entity.Protocol_37;
import net.tasuposed.projectredacted.horror.HorrorManager;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.sound.SoundRegistry;
import net.tasuposed.projectredacted.world.DimensionRegistry;
import net.tasuposed.projectredacted.world.TheVoidPortalHandler;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ProjectRedacted.MODID)
public class ProjectRedacted
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "projectredacted";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public ProjectRedacted()
    {
        // Get the mod event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register our entity types
        EntityRegistry.register(modEventBus);
        
        // Register our sound events
        SoundRegistry.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        // Register our horror manager to handle events
        MinecraftForge.EVENT_BUS.register(HorrorManager.getInstance());

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec with unique filenames
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "projectredacted-general.toml");
        context.registerConfig(ModConfig.Type.COMMON, HorrorConfig.SPEC, "projectredacted-horror.toml");

        // Initialize horror manager
        HorrorManager.init();
        
        // Initialize the void portal handler
        TheVoidPortalHandler.init();
        
        // Note: SleepPreventionHandler is auto-registered through @Mod.EventBusSubscriber annotation
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Initializing Project REDACTED horror system...");
        
        // Register the network system for client-server communication
        event.enqueueWork(NetworkHandler::registerPackets);
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        // No items to add to creative tabs
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Project REDACTED active on server");
        
        // Load saved horror data
        HorrorManager.getInstance().loadAllPlayerStates(event.getServer());
    }
    
    @SubscribeEvent
    public void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        LOGGER.info("Saving Project REDACTED horror data");
        
        // Save horror data
        HorrorManager.getInstance().saveAllPlayerStates(event.getServer());
    }

    // Register commands when the server is starting up
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering Project REDACTED debug commands");
        HorrorCommands.register(event.getDispatcher());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("Project REDACTED client initialization");
            
            // Initialize client-side effect handlers
            ScreenEffectHandler.init();
        }
        
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            // Register our screen effect overlay
            ScreenEffectHandler.registerOverlay(event);
        }
        
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            LOGGER.info("Registering entity renderers for Project REDACTED");
            // Register our entity renderers
            EntityRendererRegistry.registerEntityRenderers(event);
        }
    }
}
