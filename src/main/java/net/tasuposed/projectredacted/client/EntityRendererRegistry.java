package net.tasuposed.projectredacted.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.tasuposed.projectredacted.ProjectRedacted;
import net.tasuposed.projectredacted.entity.AngryProtocol37;
import net.tasuposed.projectredacted.entity.DistantStalker;
import net.tasuposed.projectredacted.entity.EntityRegistry;
import net.tasuposed.projectredacted.entity.InvisibleProtocol37;
import net.tasuposed.projectredacted.entity.Iteration;
import net.tasuposed.projectredacted.entity.MiningEntity;
import net.tasuposed.projectredacted.entity.Protocol_37;

/**
 * Handles registration of entity renderers on the client side
 */
@OnlyIn(Dist.CLIENT)
public class EntityRendererRegistry {
    
    /**
     * Register all entity renderers
     */
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Use humanoid renderers for our custom entities
        // Iteration will use a player-like model with white eyes
        event.registerEntityRenderer(EntityRegistry.ITERATION.get(), 
            context -> new HumanoidMobRenderer<Iteration, PlayerModel<Iteration>>(
                context, 
                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 
                0.5F) {
                    @Override
                    public ResourceLocation getTextureLocation(Iteration entity) {
                        return new ResourceLocation(ProjectRedacted.MODID, 
                            "textures/entity/iteration.png");
                    }
                });
        
        // Protocol_37 entity will use a player-like model with white eyes
        event.registerEntityRenderer(EntityRegistry.PROTOCOL_37.get(), 
            context -> new HumanoidMobRenderer<Protocol_37, PlayerModel<Protocol_37>>(
                context, 
                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 
                0.5F) {
                    @Override
                    public ResourceLocation getTextureLocation(Protocol_37 entity) {
                        return new ResourceLocation(ProjectRedacted.MODID, 
                            "textures/entity/protocol_37.png");
                    }
                });
        
        // InvisibleProtocol37 will use a noop renderer (completely invisible)
        event.registerEntityRenderer(EntityRegistry.INVISIBLE_PROTOCOL_37.get(),
            NoopRenderer::new);
        
        // DistantStalker uses same texture as Protocol_37
        event.registerEntityRenderer(EntityRegistry.DISTANT_STALKER.get(), 
            context -> new HumanoidMobRenderer<DistantStalker, PlayerModel<DistantStalker>>(
                context, 
                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 
                0.5F) {
                    @Override
                    public ResourceLocation getTextureLocation(DistantStalker entity) {
                        return new ResourceLocation(ProjectRedacted.MODID, 
                            "textures/entity/distant_stalker.png");
                    }
                });
        
        // MiningEntity will use a noop renderer (completely invisible)
        event.registerEntityRenderer(EntityRegistry.MINING_ENTITY.get(),
            NoopRenderer::new);
        
        // AngryProtocol37 uses a red-tinted Protocol_37 texture
        event.registerEntityRenderer(EntityRegistry.ANGRY_PROTOCOL_37.get(), 
            context -> new HumanoidMobRenderer<AngryProtocol37, PlayerModel<AngryProtocol37>>(
                context, 
                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 
                0.5F) {
                    @Override
                    public ResourceLocation getTextureLocation(AngryProtocol37 entity) {
                        return new ResourceLocation(ProjectRedacted.MODID, 
                            "textures/entity/angry_protocol_37.png");
                    }
                });
    }
} 