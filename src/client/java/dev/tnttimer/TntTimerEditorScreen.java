package dev.tnttimer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Drag the HUD preview with the left mouse button. Escape or Shift saves it. */
public final class TntTimerEditorScreen extends Screen {
    private boolean dragging;
    private int offsetX;
    private int offsetY;

    public TntTimerEditorScreen() {
        super(Text.literal("TNT Timer — HUD Editor"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render a simple translucent editor background; the HUD is previewed with sample values.
        context.fill(0, 0, width, height, 0x99081017);
        context.drawCenteredTextWithShadow(textRenderer, "TNT TIMER — MOVE HUD", width / 2, 22, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Drag the panel with the left mouse button", width / 2, 38, 0xFFB8D1E9);
        context.drawCenteredTextWithShadow(textRenderer, "ESC — save and return to game", width / 2, 52, 0xFFB8D1E9);
        TntTimerClient.drawPreview(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = TntTimerClient.x(width);
        int y = TntTimerClient.y(height, TntTimerClient.previewHeight());
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseX >= x && mouseX < x + TntTimerClient.WIDTH
            && mouseY >= y && mouseY < y + TntTimerClient.previewHeight()) {
            dragging = true;
            offsetX = (int) mouseX - x;
            offsetY = (int) mouseY - y;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            TntTimerClient.moveTo((int) mouseX - offsetX, (int) mouseY - offsetY, width, height, TntTimerClient.previewHeight());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
            TntTimerClient.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        TntTimerClient.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
