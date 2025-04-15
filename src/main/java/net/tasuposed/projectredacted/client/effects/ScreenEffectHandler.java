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
        
        // Create wave pattern
        for (int i = 0; i < height; i += 3) {
            float waveAmplitude = 5.0f * intensity;
            float waveOffset = (float) Math.sin((time / 10.0f) + (i / 20.0f)) * waveAmplitude;
            
            int lineColor = 0x22FFFFFF; // Semi-transparent white
            
            // Draw horizontal wavy lines
            graphics.fill((int) waveOffset, i, width + (int) waveOffset, i + 1, lineColor);
        }
    }
    
    private void renderCorruptEffect(GuiGraphics graphics, float intensity, int width, int height) {
        // Render corrupt/glitched graphics
        Minecraft minecraft = Minecraft.getInstance();
        float time = minecraft.level.getGameTime() % 1000000;
        
        // Draw glitch blocks
        int numBlocks = (int)(20 * intensity);
        for (int i = 0; i < numBlocks; i++) {
            int blockWidth = (int)(Math.random() * width / 5);
            int blockHeight = 10 + (int)(Math.random() * 30);
            int blockX = (int)(Math.random() * width);
            int blockY = (int)(Math.random() * height);
            
            // Random color with transparency
            int r = (int)(Math.random() * 255);
            int g = (int)(Math.random() * 255);
            int b = (int)(Math.random() * 255);
            int a = (int)(128 * intensity);
            
            int blockColor = (a << 24) | (r << 16) | (g << 8) | b;
            
            // Draw the glitch block
            graphics.fill(blockX, blockY, blockX + blockWidth, blockY + blockHeight, blockColor);
        }
        
        // Draw scanlines
        int scanlineSpacing = 4;
        int scanlineAlpha = (int)(100 * intensity);
        int scanlineColor = (scanlineAlpha << 24) | 0xFFFFFF;
        
        for (int y = 0; y < height; y += scanlineSpacing) {
            graphics.fill(0, y, width, y + 1, scanlineColor);
        }
    }
    
    private void renderInvertEffect(GuiGraphics graphics, float intensity, int width, int height) {
        // Invert screen colors
        Minecraft minecraft = Minecraft.getInstance();
        
        // Apply a simple screen-wide inversion effect with variable transparency
        int alpha = (int)(200 * intensity);
        int invertColor = (alpha << 24) | 0xFFFFFF; // White with transparency
        
        // Draw full-screen overlay with blend mode
        graphics.fill(0, 0, width, height, invertColor);
        
        // Add additional visual noise for more disturbing effect
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