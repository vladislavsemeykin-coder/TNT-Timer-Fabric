package dev.tnttimer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Client-side TNT timers and draggable HUD, no server mod required. */
public final class TntTimerClient implements ClientModInitializer {
    static final int WIDTH = 172;
    static final int ROW_HEIGHT = 16;
    static final int MAX_ROWS = 10;
    private static final double RANGE = 100.0;
    private static final double RANGE_SQ = RANGE * RANGE;
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("tnttimer.properties");
    private static int panelX = -1;
    private static int panelY = 12;
    private static KeyBinding editKey;

    @Override
    public void onInitializeClient() {
        load();
        editKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tnttimer.edit", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.tnttimer"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (editKey.wasPressed()) {
                if (client.currentScreen instanceof TntTimerEditorScreen) {
                    client.setScreen(null);
                } else if (client.player != null) {
                    client.setScreen(new TntTimerEditorScreen());
                }
            }
        });
        HudLayerRegistrationCallback.EVENT.register(layers -> layers.attachLayerBefore(
            IdentifiedLayer.CHAT, Identifier.of("tnttimer", "countdown_overlay"), TntTimerClient::render
        ));
    }

    static int x(int screenWidth) {
        return Math.max(0, Math.min(panelX < 0 ? screenWidth - WIDTH - 10 : panelX, Math.max(0, screenWidth - WIDTH)));
    }

    static int y(int screenHeight, int panelHeight) {
        return Math.max(0, Math.min(panelY, Math.max(0, screenHeight - panelHeight)));
    }

    static void moveTo(int x, int y, int screenWidth, int screenHeight, int panelHeight) {
        panelX = Math.max(0, Math.min(x, Math.max(0, screenWidth - WIDTH)));
        panelY = Math.max(0, Math.min(y, Math.max(0, screenHeight - panelHeight)));
    }

    static void save() {
        Properties properties = new Properties();
        properties.setProperty("x", String.valueOf(panelX));
        properties.setProperty("y", String.valueOf(panelY));
        try {
            Files.createDirectories(CONFIG.getParent());
            try (OutputStream stream = Files.newOutputStream(CONFIG)) {
                properties.store(stream, "TNT Timer HUD position in scaled Minecraft GUI pixels; x=-1 means top right");
            }
        } catch (IOException error) {
            System.err.println("TNT Timer: could not save HUD position: " + error.getMessage());
        }
    }

    private static void load() {
        if (!Files.isRegularFile(CONFIG)) return;
        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(CONFIG)) {
            properties.load(stream);
            panelX = Integer.parseInt(properties.getProperty("x", "-1"));
            panelY = Integer.parseInt(properties.getProperty("y", "12"));
        } catch (IOException | NumberFormatException error) {
            System.err.println("TNT Timer: could not load HUD position: " + error.getMessage());
        }
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden || client.currentScreen instanceof TntTimerEditorScreen) return;
        List<TntEntity> tnts = nearby(client);
        if (!tnts.isEmpty()) draw(context, client, tnts, tickCounter.getTickDelta(false), false);
    }

    private static List<TntEntity> nearby(MinecraftClient client) {
        Box area = client.player.getBoundingBox().expand(RANGE);
        List<TntEntity> tnts = client.world.getEntitiesByClass(TntEntity.class, area,
            tnt -> tnt.isAlive() && tnt.squaredDistanceTo(client.player) <= RANGE_SQ);
        tnts.sort(Comparator.comparingDouble(tnt -> tnt.squaredDistanceTo(client.player)));
        return tnts;
    }

    static int previewHeight() { return height(3, 3); }

    private static int height(int rows, int total) { return 34 + rows * ROW_HEIGHT + (total > rows ? 14 : 0); }

    static void drawPreview(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        int px = x(context.getScaledWindowWidth());
        int py = y(context.getScaledWindowHeight(), previewHeight());
        drawPanel(context, client, px, py, 3, 3, true);
        drawRow(context, client, px, py, 0, "12 m", "9,2 с", 0xFF79EAB0);
        drawRow(context, client, px, py, 1, "28 m", "2,4 с", 0xFFFFD66A);
        drawRow(context, client, px, py, 2, "47 m", "0,8 с", 0xFFFF7272);
    }

    private static void draw(DrawContext context, MinecraftClient client, List<TntEntity> tnts, float tickDelta, boolean editing) {
        int rows = Math.min(MAX_ROWS, tnts.size());
        int px = x(context.getScaledWindowWidth());
        int py = y(context.getScaledWindowHeight(), height(rows, tnts.size()));
        drawPanel(context, client, px, py, rows, tnts.size(), editing);
        for (int i = 0; i < rows; i++) {
            TntEntity tnt = tnts.get(i);
            double seconds = Math.max(0, tnt.getFuse() - tickDelta) / 20.0;
            int meters = (int) Math.round(Math.sqrt(tnt.squaredDistanceTo(client.player)));
            int color = seconds < 1 ? 0xFFFF7272 : seconds < 2.5 ? 0xFFFFD66A : 0xFF79EAB0;
            drawRow(context, client, px, py, i, meters + " m", String.format(Locale.forLanguageTag("ru"), "%.1f с", seconds), color);
        }
        if (tnts.size() > MAX_ROWS) {
            context.drawTextWithShadow(client.textRenderer, "+" + (tnts.size() - MAX_ROWS) + " TNT", px + 12,
                py + 34 + rows * ROW_HEIGHT, 0xFFAAB8C9);
        }
    }

    private static void drawPanel(DrawContext context, MinecraftClient client, int px, int py, int rows, int total, boolean editing) {
        int h = height(rows, total);
        context.fill(px + 2, py + 3, px + WIDTH + 2, py + h + 3, 0x70000000);
        context.fill(px, py, px + WIDTH, py + h, 0xDD131B28);
        context.fill(px, py, px + WIDTH, py + 2, editing ? 0xFF5DC8FF : 0xFFFF765B);
        context.fill(px + 8, py + 29, px + WIDTH - 8, py + 30, 0x554F6178);
        context.drawTextWithShadow(client.textRenderer, editing ? "TNT TIMER  [DRAG]" : "TNT TIMER", px + 12, py + 10, 0xFFF0F4FA);
        String count = String.valueOf(total);
        context.drawTextWithShadow(client.textRenderer, count, px + WIDTH - 13 - client.textRenderer.getWidth(count), py + 10, 0xFFFFAF8B);
    }

    private static void drawRow(DrawContext context, MinecraftClient client, int px, int py, int row, String distance, String seconds, int color) {
        int yy = py + 35 + row * ROW_HEIGHT;
        context.drawTextWithShadow(client.textRenderer, distance, px + 12, yy, 0xFFB5C2D3);
        int w = client.textRenderer.getWidth(seconds);
        context.drawTextWithShadow(client.textRenderer, seconds, px + WIDTH - 13 - w, yy, color);
    }
}
