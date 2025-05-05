package net.tasuposed.projectredacted.client.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.tasuposed.projectredacted.ProjectRedacted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles client-side screen effects like glitches, distortions, etc.
 */
@OnlyIn(Dist.CLIENT)
public class ScreenEffectHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenEffectHandler.class);
    public static ScreenEffectHandler INSTANCE;
    
    // Effect type constants
    public static final int EFFECT_STATIC = 0;
    public static final int EFFECT_DISTORTION = 1;
    public static final int EFFECT_CORRUPT = 2;
    public static final int EFFECT_INVERT = 3;
    
    private int currentEffect = -1;
    private float effectIntensity = 0.0f;
    private int effectDuration = 0;
    private int effectTimer = 0;
    
    private ScreenEffectHandler() {
        // Register for rendering events
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    /**
     * Initialize the screen effect handler
     */
    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new ScreenEffectHandler();
        }
    }
    
    /**
     * Static method for packet handlers to trigger effects
     */
    public static void addGlitchEffect(int effectType, float intensity, int duration) {
        if (INSTANCE != null) {
            INSTANCE.startEffect(effectType, intensity, duration);
        }
    }
    
    /**
     * Register the overlay with Forge
     */
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("horror_effects", (gui, poseStack, partialTick, width, height) -> {
            if (INSTANCE != null) {
                try {
                    INSTANCE.renderEffects(gui, poseStack, partialTick, width, height);
                } catch (Exception e) {
                    // Log error but don't crash the game if rendering fails
                    LOGGER.error("Failed to render horror effects: ", e);
                }
            }
        });
    }
    
    /**
     * Start a screen effect
     * @param effectType Type of effect to start
     * @param intensity Intensity of the effect (0-1)
     * @param duration Duration in ticks
     */
    public void startEffect(int effectType, float intensity, int duration) {
        this.currentEffect = effectType;
        this.effectIntensity = Math.min(1.0f, Math.max(0.0f, intensity));
        this.effectDuration = duration;
        this.effectTimer = 0;
    }
    
    /**
     * Stop any active screen effect
     */
    public void stopEffect() {
        this.currentEffect = -1;
        this.effectIntensity = 0.0f;
        this.effectDuration = 0;
        this.effectTimer = 0;
    }
    
    /**
     * Display a fake crash screen with a custom message
     */
    public static void displayFakeCrashScreen(String message) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new FakeCrashScreen(message));
        } catch (Exception e) {
            LOGGER.error("Failed to display fake crash screen: ", e);
        }
    }
    
    /**
     * Render the current effect if active
     */
    public void renderEffects(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (currentEffect == -1 || effectTimer >= effectDuration) {
            return;
        }
        
        // Update effect timer
        effectTimer++;
        
        // Calculate fade-in/fade-out intensity
        float fadeIntensity = effectIntensity;
        if (effectTimer < 10) {
            fadeIntensity = effectIntensity * (effectTimer / 10.0f);
        } else if (effectDuration - effectTimer < 10) {
            fadeIntensity = effectIntensity * ((effectDuration - effectTimer) / 10.0f);
        }
        
        try {
            // Apply the appropriate effect
            switch (currentEffect) {
                case EFFECT_STATIC:
                    renderStaticEffect(graphics, fadeIntensity, width, height);
                    break;
                case EFFECT_DISTORTION:
                    renderDistortionEffect(graphics, fadeIntensity, width, height);
                    break;
                case EFFECT_CORRUPT:
                    renderCorruptEffect(graphics, fadeIntensity, width, height);
                    break;
                case EFFECT_INVERT:
                    renderInvertEffect(graphics, fadeIntensity, width, height);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Error rendering effect: ", e);
            stopEffect(); // Stop effect if it causes errors
        }
        
        // Auto-stop when done
        if (effectTimer >= effectDuration) {
            stopEffect();
        }
    }
    
    // Effect rendering implementations
    private void renderStaticEffect(GuiGraphics graphics, float intensity, int width, int height) {
        // Render static noise effect on the screen
        Minecraft minecraft = Minecraft.getInstance();
        float time = minecraft.level.getGameTime() % 1000000;
        
        // Use random noise pattern based on time seed
        for (int i = 0; i < 1000 * intensity; i++) {
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            int size = 1 + (int) (Math.random() * 3 * intensity);
            int color = 0xFFFFFFFF;
            int alpha = (int) (128 * intensity * Math.random());
            
            int pixelColor = (alpha << 24) | (color & 0xFFFFFF);
            
            // Draw random pixels for static effect
            graphics.fill(x, y, x + size, y + size, pixelColor);
        }
    }
    
    private void renderDistortionEffect(GuiGraphics graphics, float intensity, int width, int height) {
        // Apply wave/wobble distortion effect
        Minecraft minecraft = Minecraft.getInstance();
        float time = minecraft.level.getGameTime() % 1000000;
        
        // Enhance distortion with more layers for better visual impact
        // First layer: horizontal wave pattern
        for (int i = 0; i < height; i += 3) {
            float waveAmplitude = 7.0f * intensity;
            float waveOffset = (float) Math.sin((time / 10.0f) + (i / 20.0f)) * waveAmplitude;
            
            int lineColor = 0x22FFFFFF; // Semi-transparent white
            
            // Draw horizontal wavy lines
            graphics.fill((int) waveOffset, i, width + (int) waveOffset, i + 1, lineColor);
        }
        
        // Second layer: vertical distortion (only with higher intensity)
        if (intensity > 0.4f) {
            float verticalIntensity = (intensity - 0.4f) * 1.67f; // Scale 0.4-1.0 to 0.0-1.0
            
            for (int i = 0; i < width; i += 5) {
                float waveAmplitude = 6.0f * verticalIntensity;
                float waveOffset = (float) Math.sin((time / 12.0f) + (i / 25.0f)) * waveAmplitude;
                
                int lineColor = 0x18FFFFFF; // More subtle vertical effect
                
                // Draw vertical wavy lines
                graphics.fill(i, (int) waveOffset, i + 1, height + (int) waveOffset, lineColor);
            }
        }
        
        // Third layer: RGB shift effect (for high intensity)
        if (intensity > 0.7f) {
            float rgbIntensity = (intensity - 0.7f) * 3.33f; // Scale 0.7-1.0 to 0.0-1.0
            int rgbShift = (int)(4.0f * rgbIntensity);
            
            if (rgbShift > 0) {
                // Add subtle RGB shift overlay
                int redOverlay = (int)(50 * rgbIntensity) << 24 | 0x00FF0000;
                int greenOverlay = (int)(50 * rgbIntensity) << 24 | 0x0000FF00;
                int blueOverlay = (int)(50 * rgbIntensity) << 24 | 0x000000FF;
                
                // Red shifted left
                graphics.fill(0, 0, width, height, redOverlay);
                
                // Green stays centered
                graphics.fill(rgbShift, 0, width + rgbShift, height, greenOverlay);
                
                // Blue shifted right
                graphics.fill(rgbShift * 2, 0, width + (rgbShift * 2), height, blueOverlay);
            }
        }
    }
    
    private void renderCorruptEffect(GuiGraphics graphics, float intensity, int width, int height) {
        // Corruption effect - create data corruption appearance
        Minecraft minecraft = Minecraft.getInstance();
        float time = minecraft.level.getGameTime() % 1000000;
        
        // First layer: Block corruption
        int blockSize = 8;
        int numBlocks = (int)(50 * intensity);
        
        for (int i = 0; i < numBlocks; i++) {
            // Compute positions based on time for movement
            int x = (int)((Math.sin(time / 20.0f + i) + 1) * width / 2);
            int y = (int)((Math.cos(time / 30.0f + i * 2) + 1) * height / 2);
            
            // Randomize block appearance
            int blockW = blockSize + (int)(Math.random() * blockSize * 2);
            int blockH = blockSize + (int)(Math.random() * blockSize);
            
            // Calculate color - mix of black, white and glitch colors
            int colorChoice = (int)(Math.random() * 5);
            int blockColor;
            
            switch (colorChoice) {
                case 0:
                    blockColor = 0xFF000000; // Black
                    break;
                case 1:
                    blockColor = 0xFFFFFFFF; // White
                    break;
                case 2:
                    blockColor = 0xFF0000FF; // Blue
                    break;
                case 3:
                    blockColor = 0xFF00FF00; // Green
                    break;
                default:
                    blockColor = 0xFFFF0000; // Red
                    break;
            }
            
            // Apply alpha based on intensity
            blockColor = ((int)(200 * intensity) << 24) | (blockColor & 0x00FFFFFF);
            
            // Draw corrupted block
            graphics.fill(x, y, x + blockW, y + blockH, blockColor);
        }
        
        // Second layer: Scan lines effect (more visible at higher intensities)
        if (intensity > 0.4f) {
            int scanLineSpacing = 4;
            int scanLineColor = ((int)(100 * intensity) << 24) | 0x000000;
            
            for (int y = 0; y < height; y += scanLineSpacing) {
                graphics.fill(0, y, width, y + 1, scanLineColor);
            }
        }
        
        // Third layer: Text corruption (for high intensity)
        if (intensity > 0.6f && Math.random() < 0.3) {
            String[] corruptText = {
                "ERROR", "CORRUPT", "DELETED", "FAULT", "BREACH",
                "01101", "NULL", "VOID", "WATCH", "SEE YOU"
            };
            
            int textCount = (int)(5 * intensity);
            for (int i = 0; i < textCount; i++) {
                int x = (int)(Math.random() * width);
                int y = (int)(Math.random() * height);
                String text = corruptText[(int)(Math.random() * corruptText.length)];
                
                // Random color
                int textColor = (int)(Math.random() * 0xFFFFFF);
                int alpha = (int)(255 * intensity);
                int finalColor = (alpha << 24) | textColor;
                
                // Draw the text
                graphics.drawString(minecraft.font, text, x, y, finalColor);
            }
        }
    }
    
    private void renderInvertEffect(GuiGraphics graphics, float intensity, int width, int height) {
        // Create a true inversion effect
        Minecraft minecraft = Minecraft.getInstance();
        
        // Apply a full screen color inversion with appropriate transparency
        int alpha = (int)(255 * intensity); // Full opacity for better inversion
        
        // Use a white overlay with specific RenderSystem settings for inversion effect
        int invertColor = (alpha << 24) | 0xFFFFFF; // White with alpha channel
        
        // Set up the blend function for a difference-like effect
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, intensity);
        
        // This blend function helps create an inversion-like effect
        com.mojang.blaze3d.systems.RenderSystem.blendFunc(
            com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
            com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR
        );
        
        // Draw full-screen overlay with our special blend function to invert colors
        graphics.fill(0, 0, width, height, 0xFFFFFFFF);
        
        // Restore normal blend mode
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Add additional visual noise for more disturbing effect if intensity is high
        if (intensity > 0.5f) {
            int noiseCount = (int)(200 * (intensity - 0.5f) * 2.0f);
            
            for (int i = 0; i < noiseCount; i++) {
                int x = (int)(Math.random() * width);
                int y = (int)(Math.random() * height);
                int size = 1 + (int)(Math.random() * 3);
                int noiseColor = 0xFF000000; // Black
                
                graphics.fill(x, y, x + size, y + size, noiseColor);
            }
        }
    }
    
    /**
     * A fake crash screen that looks like a real crash but isn't
     */
    @OnlyIn(Dist.CLIENT)
    public static class FakeCrashScreen extends Screen {
        private final String crashMessage;
        private int tickCount = 0;
        private final int displayTime = 150; // Display for about 7.5 seconds
        
        public FakeCrashScreen(String crashMessage) {
            super(Component.literal("Game Crashed"));
            this.crashMessage = crashMessage;
        }
        
        @Override
        protected void init() {
            super.init();
            
            // Add a close button that looks like a real crash dialog
            addRenderableWidget(Button.builder(Component.literal("Close"), button -> {
                Minecraft.getInstance().setScreen(null);
            }).pos(this.width / 2 - 100, this.height - 40).size(200, 20).build());
        }
        
        @Override
        public void tick() {
            super.tick();
            
            // Auto-close after display time
            if (++tickCount > displayTime) {
                // First apply a static effect
                if (INSTANCE != null) {
                    INSTANCE.startEffect(EFFECT_STATIC, 1.0f, 40);
                }
                
                // Then close after a short delay
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.setScreen(null);
            }
        }
        
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            try {
                // Fill background with dark red
                this.renderBackground(graphics);
                graphics.fill(0, 0, this.width, this.height, 0xFF500000);
                
                // Draw crash info
                graphics.drawCenteredString(this.font, "§c§lFATAL ERROR", this.width / 2, 20, 0xFFFFFFFF);
                graphics.drawCenteredString(this.font, "§c§lThe game has crashed!", this.width / 2, 40, 0xFFFFFFFF);
                
                // Format the crash message to fit on screen
                String[] lines = formatMultiline(crashMessage, 80);
                int y = 70;
                for (String line : lines) {
                    graphics.drawCenteredString(this.font, "§4" + line, this.width / 2, y, 0xFFFFFFFF);
                    y += 12;
                }
                
                // Draw fake Java error details
                String javaInfo = "---- Minecraft Crash Report ----";
                graphics.drawString(this.font, "§7" + javaInfo, 20, this.height - 100, 0xFFFFFFFF);
                
                String timeInfo = "Time: " + java.time.LocalDateTime.now().toString();
                graphics.drawString(this.font, "§7" + timeInfo, 20, this.height - 88, 0xFFFFFFFF);
                
                String descInfo = "Description: Fatal game error";
                graphics.drawString(this.font, "§7" + descInfo, 20, this.height - 76, 0xFFFFFFFF);
                
                // Randomly add glitch effects
                if (Math.random() < 0.05) {
                    renderGlitchEffect(graphics);
                }
                
                super.render(graphics, mouseX, mouseY, partialTick);
            } catch (Exception e) {
                LOGGER.error("Error rendering fake crash screen: ", e);
                // In case of error, close the screen
                Minecraft.getInstance().setScreen(null);
            }
        }
        
        private void renderGlitchEffect(GuiGraphics graphics) {
            // Add some glitch blocks that mimic corruption
            for (int i = 0; i < 20; i++) {
                int x = (int)(Math.random() * this.width);
                int y = (int)(Math.random() * this.height);
                int width = 5 + (int)(Math.random() * 100);
                int height = 10 + (int)(Math.random() * 30);
                
                int r = (int)(Math.random() * 255);
                int g = (int)(Math.random() * 100); // Lower green for more red tint
                int b = (int)(Math.random() * 100); // Lower blue for more red tint
                
                int color = 0xFF000000 | (r << 16) | (g << 8) | b;
                
                graphics.fill(x, y, x + width, y + height, color);
            }
        }
        
        private String[] formatMultiline(String text, int maxLineLength) {
            // First, calculate how many lines we'll need
            String[] words = text.split(" ");
            int lineCount = 0;
            StringBuilder testLine = new StringBuilder();
            
            for (String word : words) {
                if (testLine.length() + word.length() + 1 <= maxLineLength) {
                    if (testLine.length() > 0) {
                        testLine.append(" ");
                    }
                    testLine.append(word);
                } else {
                    lineCount++;
                    testLine = new StringBuilder(word);
                }
            }
            
            // Add the last line if needed
            if (testLine.length() > 0) {
                lineCount++;
            }
            
            // Now create the actual lines
            String[] lines = new String[lineCount];
            int currentLineIndex = 0;
            testLine = new StringBuilder();
            
            for (String word : words) {
                if (testLine.length() + word.length() + 1 <= maxLineLength) {
                    if (testLine.length() > 0) {
                        testLine.append(" ");
                    }
                    testLine.append(word);
                } else {
                    lines[currentLineIndex++] = testLine.toString();
                    testLine = new StringBuilder(word);
                }
            }
            
            // Add the last line if needed
            if (testLine.length() > 0 && currentLineIndex < lines.length) {
                lines[currentLineIndex] = testLine.toString();
            }
            
            return lines;
        }
        
        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }
} 