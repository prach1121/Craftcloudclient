package com.craftcloudclient.client.gui;

import com.craftcloudclient.client.CraftcloudClient;
import com.craftcloudclient.client.config.HudPosition;
import com.craftcloudclient.client.hud.DraggablePanel;
import com.craftcloudclient.client.hud.HudManager;
import com.craftcloudclient.client.hud.HudRect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight overlay for repositioning HUD panels without touching the
 * main settings menu: the world stays visible, the real HUD renders as-is,
 * and every panel returned by {@link HudManager#collectDraggables()} - the
 * corner-grouped module panels, the server banner, and the waypoint
 * tracker cards - can be repositioned two ways:
 * <ul>
 *   <li><b>Drag</b> - left-click and hold a panel, drag it anywhere on the
 *   screen, and let go to place it at that exact pixel position. Works on
 *   every draggable panel.</li>
 *   <li><b>Quick pick</b> - right-click a panel to pop up a small 2x2
 *   "move to corner" grid right where you clicked, for when you just want
 *   to jump straight to a specific corner without dragging. Only offered
 *   for panels where {@link com.craftcloudclient.client.hud.DraggablePanel#supportsCornerSnap()}
 *   is true - currently just the corner-grouped module panels, since the
 *   server banner and waypoint cards are meant to stay near top-center.</li>
 * </ul>
 * Positions are stored as free-form pixel coordinates per panel, while the
 * quick-pick popup still uses the corner-based {@link HudPosition} enum.
 *
 * Opened via the (unbound by default) "Edit HUD Layout" keybind, or the
 * button at the top of the HUD tab in {@link ConfigScreen}.
 */
public class HudLayoutScreen extends Screen {

    private static final int BTN_W = 48;
    private static final int BTN_H = 20;
    private static final int GRID_GAP = 4;
    private static final int GHOST_FILL = 0x992A6EFF;

    private final HudManager hudManager = CraftcloudClient.getHudManager();
    private final List<ButtonWidget> popupWidgets = new ArrayList<>();

    private HudManager.GroupBox popupBox;
    private int popupPanelX, popupPanelY, popupPanelW, popupPanelH;

    // --- Drag state ---
    // draggingBox is the panel currently being dragged (null when nothing
    // is being dragged); the offsets keep the panel following the cursor
    // at the same spot within it that was originally grabbed, instead of
    // snapping its top-left corner to the cursor.
    private DraggablePanel draggingBox;
    private int dragOffsetX, dragOffsetY;

    public HudLayoutScreen() {
        super(Text.literal("Edit HUD Layout"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        double mouseX = click.x();
        double mouseY = click.y();

        if (hudManager != null) {
            if (click.button() == 1) {
                // Right click: quick "jump to corner" popup, unchanged.
                for (HudManager.GroupBox box : hudManager.collectGroupBoxes()) {
                    if (box.contains(mouseX, mouseY)) {
                        openPopup(box, (int) mouseX, (int) mouseY);
                        return true;
                    }
                }
            } else if (click.button() == 0) {
                // Left click: grab the panel under the cursor and start dragging it.
                // Covers both the corner-grouped module panels and the
                // server banner / waypoint cards.
                for (DraggablePanel panel : hudManager.collectDraggables()) {
                    HudRect b = panel.bounds();
                    if (b.contains(mouseX, mouseY)) {
                        closePopup();
                        draggingBox = panel;
                        dragOffsetX = (int) mouseX - b.x();
                        dragOffsetY = (int) mouseY - b.y();
                        return true;
                    }
                }
            }
        }

        if (popupBox != null) {
            closePopup();
            return true;
        }

        return false;
    }

    private void openPopup(HudManager.GroupBox box, int clickX, int clickY) {
        closePopup();
        popupBox = box;

        int gridW = BTN_W * 2 + GRID_GAP;
        int gridH = BTN_H * 2 + GRID_GAP;
        int padding = 8;

        int baseX = clickX - gridW / 2;
        int baseY = clickY - gridH / 2;
        baseX = Math.max(padding, Math.min(baseX, this.width - gridW - padding));
        baseY = Math.max(padding, Math.min(baseY, this.height - gridH - padding));

        popupPanelX = baseX - padding;
        popupPanelY = baseY - padding;
        popupPanelW = gridW + padding * 2;
        popupPanelH = gridH + padding * 2;

        addCornerButton(baseX, baseY, HudPosition.TOP_LEFT, "\u2196 TL");
        addCornerButton(baseX + BTN_W + GRID_GAP, baseY, HudPosition.TOP_RIGHT, "\u2197 TR");
        addCornerButton(baseX, baseY + BTN_H + GRID_GAP, HudPosition.BOTTOM_LEFT, "\u2199 BL");
        addCornerButton(baseX + BTN_W + GRID_GAP, baseY + BTN_H + GRID_GAP, HudPosition.BOTTOM_RIGHT, "\u2198 BR");
    }

    private void addCornerButton(int x, int y, HudPosition target, String label) {
        GlassButton button = new GlassButton(x, y, BTN_W, BTN_H, label, () -> {
            HudManager.moveGroup(popupBox, target);
            closePopup();
        }, () -> popupBox != null && popupBox.position == target);
        popupWidgets.add(button);
        this.addDrawableChild(button);
    }

    private void closePopup() {
        for (ButtonWidget widget : popupWidgets) {
            this.remove(widget);
        }
        popupWidgets.clear();
        popupBox = null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (hudManager != null) {
            hudManager.render(context);
        }

        if (draggingBox != null) {
            updateDrag(mouseX, mouseY);
        }
        // Re-check: updateDrag() may have just finished the drag this frame.
        if (draggingBox != null) {
            renderDragGhost(context, mouseX, mouseY);
        }

        // Highlight whichever panel the popup is currently open for.
        if (popupBox != null) {
            int x = popupBox.x, y = popupBox.y, w = popupBox.width, h = popupBox.height;
            context.fill(x - 2, y - 2, x + w + 2, y - 1, GlassTheme.ACCENT);
            context.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, GlassTheme.ACCENT);
            context.fill(x - 2, y - 2, x - 1, y + h + 2, GlassTheme.ACCENT);
            context.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, GlassTheme.ACCENT);

            GlassTheme.panel(context, popupPanelX, popupPanelY, popupPanelW, popupPanelH);
        }

        String hint = draggingBox != null
                ? "Release to place it freely \u00b7 Esc to cancel"
                : "Drag a panel to move it \u00b7 Right-click for quick corners \u00b7 Esc to finish";
        int hintWidth = this.textRenderer.getWidth(hint);
        context.drawTextWithShadow(this.textRenderer, hint, (this.width - hintWidth) / 2, 10, GlassTheme.TEXT_MAIN);

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * Polled every frame instead of relying on a mouseReleased/mouseDragged
     * override, since this codebase already leans on the newer {@code Click}-
     * based input API for {@link #mouseClicked} and guessing at further
     * method signatures there risks another mapping-mismatch compile error.
     * Reading the raw GLFW button state directly is version-stable and
     * needs nothing beyond what {@link com.craftcloudclient.client.KeyBindings}
     * already imports from LWJGL.
     */
    private void updateDrag(int mouseX, int mouseY) {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        boolean leftStillDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!leftStillDown) {
            HudRect b = draggingBox.bounds();
            int x = Math.max(6, Math.min(mouseX - dragOffsetX, this.width - b.width() - 6));
            int y = Math.max(6, Math.min(mouseY - dragOffsetY, this.height - b.height() - 6));
            draggingBox.savePosition(x, y);
            draggingBox = null;
        }
    }

    /** Translucent copy of the panel being dragged, following the cursor. */
    private void renderDragGhost(DrawContext context, int mouseX, int mouseY) {
        HudRect b = draggingBox.bounds();
        int w = b.width();
        int h = b.height();
        int x = mouseX - dragOffsetX;
        int y = mouseY - dragOffsetY;

        GlassTheme.panel(context, x, y, w, h, GHOST_FILL);

        String label = "Drop to place";
        int tw = this.textRenderer.getWidth(label);
        context.drawTextWithShadow(this.textRenderer, label, mouseX - tw / 2, mouseY + 14, GlassTheme.ACCENT);
    }

    /** Which corner a drop at this screen position would snap to - simply whichever quadrant the cursor is in. */
    private HudPosition cornerFor(int mouseX, int mouseY) {
        boolean right = mouseX > this.width / 2;
        boolean bottom = mouseY > this.height / 2;
        if (right && bottom) return HudPosition.BOTTOM_RIGHT;
        if (right) return HudPosition.TOP_RIGHT;
        if (bottom) return HudPosition.BOTTOM_LEFT;
        return HudPosition.TOP_LEFT;
    }
}
