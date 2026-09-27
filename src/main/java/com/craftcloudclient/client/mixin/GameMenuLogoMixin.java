package com.craftcloudclient.client.mixin;

import com.craftcloudclient.client.gui.UpdateBannerButton;
import com.craftcloudclient.client.update.UpdateChecker;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Paints the bare Craftcloud wordmark texture (no background) into the
 * empty space between the pause screen's title and its first row of
 * buttons ("Back to Game").
 *
 * Mixes into the base {@link Screen} class rather than {@code GameMenuScreen}
 * directly for the same refmap-less reason {@link VanillaOptionsThemeMixin}
 * does - {@code width} is only declared on {@code Screen} - and is gated
 * behind an {@code instanceof GameMenuScreen} check so nothing else changes.
 *
 * Rather than hard-coding a Y offset (which would drift the moment the
 * screen's layout, GUI scale, or window resolution changes, or another mod
 * adds/removes a row of buttons), this reads the actual on-screen position
 * of the topmost button every frame and anchors the logo just above it.
 *
 * That "topmost button" position is found by walking the screen's children
 * recursively rather than just checking direct children: on this screen the
 * buttons aren't added to the screen directly, they're nested inside a
 * {@code GridWidget} that vanilla uses purely for layout math, so a
 * shallow, non-recursive scan finds nothing and this mixin would silently
 * never draw anything (which is exactly what happened before this fix).
 *
 * The gap between the title and the first button is normally quite small
 * in vanilla - nowhere near the ~65px this used to require - so rather than
 * bailing out when a "generous" size doesn't fit, the logo now scales
 * itself down (preserving aspect ratio) to whatever space is actually
 * available, only skipping entirely if that space is too small to show
 * anything legible.
 *
 * The recursive walk only counts widgets at least {@link #MIN_ANCHOR_WIDGET_WIDTH}
 * wide. Small square icon buttons some screens/mods tuck near the very top
 * of the screen (report flag, accessibility, etc.) are otherwise picked up
 * as the "topmost" widget, which anchors the logo way too high and pins it
 * right over the "Game Menu" title instead of in the gap below it - the
 * actual button rows are always comfortably wider than any of those.
 *
 * Drawn as the bare texture with no background panel behind it - it sits
 * directly on the blurred pause-screen backdrop.
 *
 * Also adds a separate top-center "Update available" banner widget (see
 * {@link #craftcloudclient$addUpdateBanner} and
 * {@link UpdateBannerButton}) when {@link UpdateChecker} has found a newer
 * release. Bundled into this same mixin purely because it already targets
 * {@code Screen} gated to {@code GameMenuScreen}; it's otherwise unrelated
 * to the logo above.
 */
@Mixin(Screen.class)
public class GameMenuLogoMixin {

    private static final Identifier LOGO = Identifier.of("craftcloudclient", "textures/gui/menu_logo.png");

    // Native size of the bundled texture (already cropped to its opaque content).
    private static final int LOGO_TEX_W = 288;
    private static final int LOGO_TEX_H = 71;
    private static final float LOGO_ASPECT = LOGO_TEX_H / (float) LOGO_TEX_W;

    // Preferred on-screen width when there's plenty of room, and the
    // smallest we'll shrink to before giving up rather than drawing
    // something illegibly tiny.
    private static final int PREFERRED_DRAW_W = 170;
    private static final int MIN_DRAW_W = 64;

    // Widgets narrower than this are treated as small icon-style buttons
    // (not a real menu button row) and ignored when looking for the
    // topmost row to anchor above - see the class javadoc.
    private static final int MIN_ANCHOR_WIDGET_WIDTH = 60;

    // No background panel behind the logo anymore - just the raw texture,
    // so only a margin below it (before the button row) is needed, not
    // padding on all sides for a box.
    private static final int GAP_BELOW_LOGO = 8;

    @Shadow public int width;

    @Shadow
    protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
        throw new AssertionError();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At("TAIL"),
            require = 0
    )
    private void craftcloudclient$paintMenuLogo(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!(((Object) this) instanceof GameMenuScreen)) {
            return;
        }

        int topOfButtons = findTopOfButtons((Screen) (Object) this);
        if (topOfButtons < 0) {
            return; // widgets not laid out yet this frame - just skip, next frame will have them
        }

        int availableHeight = topOfButtons - GAP_BELOW_LOGO;
        if (availableHeight < (int) (MIN_DRAW_W * LOGO_ASPECT)) {
            return; // genuinely no room for even the smallest legible version
        }

        int drawWFromHeight = (int) (availableHeight / LOGO_ASPECT);
        int drawW = MathHelper.clamp(Math.min(PREFERRED_DRAW_W, drawWFromHeight), MIN_DRAW_W, PREFERRED_DRAW_W);
        int drawH = (int) (drawW * LOGO_ASPECT);

        int logoX = (this.width - drawW) / 2;
        int logoY = topOfButtons - GAP_BELOW_LOGO - drawH;

        // NOTE: the (pipeline, sprite, x, y, u, v, width, height, textureWidth,
        // textureHeight) overload treats "width, height" as BOTH the drawn
        // size AND the sampled region size (i.e. a 1:1 crop, no scaling).
        // Since this logo is drawn at a dynamically shrunk drawW x drawH
        // that's usually smaller than the native 288x71 texture, that
        // overload just crops the top-left corner of the source image -
        // which is transparent padding - instead of scaling the whole
        // logo down, so nothing visible ever gets drawn. The 12-arg
        // overload keeps the sampled region (regionWidth/regionHeight) at
        // the full native texture size while independently scaling that
        // region to drawW x drawH on screen.
        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, logoX, logoY, 0, 0,
                drawW, drawH, LOGO_TEX_W, LOGO_TEX_H, LOGO_TEX_W, LOGO_TEX_H);
    }

    /**
     * Adds the top-center "Update available: vX.X.X" pill as a real
     * {@link UpdateBannerButton} once {@link UpdateChecker} has actually
     * confirmed a newer compatible release exists. Deliberately pinned to
     * the top of the screen rather than tucked near the logo/button layout
     * above - that layout already has to dynamically shrink to fit various
     * screen sizes (see the class javadoc), and there usually isn't spare
     * room to also fit a second line without fighting it for space.
     *
     * Injected at the TAIL of {@code init()} (rather than hand-painted and
     * hit-tested from {@code render()}/{@code mouseClicked} as before) so
     * clicks go through the same {@code addDrawableChild} widget pipeline
     * every other clickable element on this screen already uses - see
     * {@link UpdateBannerButton}'s javadoc for why that matters. Vanilla
     * clears a screen's children before every call to {@code init()}
     * (including on window resize), so this runs fresh each time with
     * nothing to remove first.
     */
    @Inject(
            method = "init()V",
            at = @At("TAIL"),
            require = 0
    )
    private void craftcloudclient$addUpdateBanner(CallbackInfo ci) {
        if (!(((Object) this) instanceof GameMenuScreen)) {
            return;
        }

        Screen self = (Screen) (Object) this;
        UpdateChecker.available().ifPresent(info ->
                this.addDrawableChild(UpdateBannerButton.create(this.width, info, self)));
    }

    /**
     * Y coordinate of the topmost {@link ClickableWidget} on the screen, or
     * -1 if none are laid out yet. Recurses into {@link ParentElement}s
     * (like the {@code GridWidget} the button rows are nested in) since
     * those containers - not the individual buttons - are often what's
     * actually registered as the screen's direct children.
     */
    private static int findTopOfButtons(Screen screen) {
        int top = topOfChildren(screen.children());
        return top == Integer.MAX_VALUE ? -1 : top;
    }

    private static int topOfChildren(Iterable<? extends Element> elements) {
        int top = Integer.MAX_VALUE;
        for (Element child : elements) {
            // Skip our own update banner - it's pinned to the very top of
            // the screen and is wide enough to otherwise get picked up as
            // the "topmost button row", which would anchor the logo right
            // underneath it instead of above the real button row.
            if (child instanceof UpdateBannerButton) {
                continue;
            }
            if (child instanceof ClickableWidget widget && widget.getWidth() >= MIN_ANCHOR_WIDGET_WIDTH) {
                top = Math.min(top, widget.getY());
            }
            if (child instanceof ParentElement parent) {
                top = Math.min(top, topOfChildren(parent.children()));
            }
        }
        return top;
    }
}
