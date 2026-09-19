package com.eyezoom;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EyeZoom implements ClientModInitializer {

    private static final String CATEGORY = "key.categories.misc";

    public static final List<KeyBinding> KEYS = new ArrayList<>();

    /**
     * Toggle, not hold. On a touchscreen you aim by dragging, so holding a
     * zoom button down with a second finger while dragging is awkward at best.
     * Toggle works identically well with a keyboard.
     */
    public static final KeyBinding TOGGLE_ZOOM = register("key.eyezoom.toggle", GLFW.GLFW_KEY_C);
    public static final KeyBinding ZOOM_IN = register("key.eyezoom.zoom_in", GLFW.GLFW_KEY_EQUAL);
    public static final KeyBinding ZOOM_OUT = register("key.eyezoom.zoom_out", GLFW.GLFW_KEY_MINUS);

    private static final double MIN_LEVEL = 1.5D;
    private static final double MAX_LEVEL = 30.0D;
    private static final double STEP = 1.5D;

    /** How fast the zoom eases in and out. 1.0 would be instant. */
    private static final double SMOOTHING = 0.45D;

    private static boolean zooming = false;
    private static double level = 6.0D;        // FOV divisor when fully zoomed

    // Interpolated between ticks so the transition is smooth at any framerate.
    private static double current = 1.0D;
    private static double previous = 1.0D;

    private static double savedSensitivity = -1.0D;

    private static KeyBinding register(String translationKey, int defaultKey) {
        KeyBinding binding = new KeyBinding(translationKey, InputUtil.Type.KEYSYM, defaultKey, CATEGORY);
        KEYS.add(binding);
        return binding;
    }

    @Override
    public void onInitializeClient() {
        loadConfig();
    }

    // ------------------------------------------------------------------ input

    public static void handleInput(MinecraftClient client) {
        while (TOGGLE_ZOOM.wasPressed()) {
            setZooming(client, !zooming);
        }
        while (ZOOM_IN.wasPressed()) {
            adjustLevel(client, STEP);
        }
        while (ZOOM_OUT.wasPressed()) {
            adjustLevel(client, -STEP);
        }
    }

    private static void adjustLevel(MinecraftClient client, double delta) {
        level = MathHelper.clamp(level + delta, MIN_LEVEL, MAX_LEVEL);
        saveConfig();
        if (zooming) {
            // Re-apply sensitivity scaling for the new level.
            restoreSensitivity(client);
            applySensitivity(client);
        }
    }

    private static void setZooming(MinecraftClient client, boolean value) {
        if (value == zooming) return;
        zooming = value;
        if (zooming) {
            applySensitivity(client);
        } else {
            restoreSensitivity(client);
        }
    }

    /**
     * Zooming without scaling look sensitivity makes fine aiming impossible,
     * because the same mouse delta now sweeps a much larger arc on screen.
     * We stash the user's value and divide it; sqrt keeps it from feeling
     * sluggish at high zoom levels.
     *
     * Note this mutates GameOptions in memory. We never call options.write()
     * while zoomed, so a crash mid-zoom cannot persist the scaled value.
     */
    private static void applySensitivity(MinecraftClient client) {
        if (client.options == null || savedSensitivity >= 0) return;
        savedSensitivity = client.options.mouseSensitivity;
        client.options.mouseSensitivity = savedSensitivity / Math.sqrt(level);
    }

    private static void restoreSensitivity(MinecraftClient client) {
        if (client.options == null || savedSensitivity < 0) return;
        client.options.mouseSensitivity = savedSensitivity;
        savedSensitivity = -1.0D;
    }

    // ------------------------------------------------------------------- tick

    public static void tick() {
        previous = current;
        double target = zooming ? level : 1.0D;
        current += (target - current) * SMOOTHING;
        if (Math.abs(target - current) < 0.001D) current = target;
    }

    /** Divisor applied to the vanilla FOV. 1.0 means no zoom. */
    public static double getFovDivisor(float tickDelta) {
        return MathHelper.lerp(tickDelta, previous, current);
    }

    public static boolean isZooming() {
        return zooming;
    }

    public static double getLevel() {
        return level;
    }

    /** Called when leaving a world so we never strand a scaled sensitivity. */
    public static void reset(MinecraftClient client) {
        restoreSensitivity(client);
        zooming = false;
    }

    // ----------------------------------------------------------------- config

    private static Path configPath() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config").resolve("eyezoom.properties");
    }

    private static void loadConfig() {
        Path path = configPath();
        if (!Files.exists(path)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
            level = MathHelper.clamp(
                    Double.parseDouble(props.getProperty("level", "6.0")),
                    MIN_LEVEL, MAX_LEVEL);
        } catch (IOException | NumberFormatException ignored) {
            // Corrupt or unreadable config: fall back to the default level.
        }
    }

    private static void saveConfig() {
        Path path = configPath();
        Properties props = new Properties();
        props.setProperty("level", Double.toString(level));
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "EyeZoom");
            }
        } catch (IOException ignored) {
        }
    }
}
