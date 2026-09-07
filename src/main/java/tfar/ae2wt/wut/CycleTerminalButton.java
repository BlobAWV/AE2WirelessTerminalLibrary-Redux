package tfar.ae2wt.wut;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ITooltip;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Arrays;
import java.util.List;

public class CycleTerminalButton extends Button implements ITooltip {

    private static final ResourceLocation CYCLE_ICON = new ResourceLocation("ae2wtlib", "textures/cycle_terminal.png");

    public CycleTerminalButton(IPressable onPress) {
        super(0, 0, 16, 16, StringTextComponent.EMPTY, onPress);
        visible = true;
        active = true;
    }

    @Override
    public void renderWidget(MatrixStack matrices, final int mouseX, final int mouseY, float partial) {
        if (this.visible) {
            Minecraft mc = Minecraft.getInstance();
            TextureManager textureManager = mc.getTextureManager();
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();

            Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(x, y).blit(matrices, getBlitOffset());

            textureManager.bindTexture(CYCLE_ICON);
            blit(matrices, x + 2, y + 2, 0, 0, 12, 12, 12, 12);

            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();

            if (isHovered()) {
                renderToolTip(matrices, mouseX, mouseY);
            }
        }
    }

    @Override
    public List<ITextComponent> getTooltipMessage() {
        return Arrays.asList(
                new TranslationTextComponent("gui.ae2wtlib.cycle_terminal"),
                new TranslationTextComponent("gui.ae2wtlib.cycle_terminal.desc")
        );
    }

    @Override
    public int getTooltipAreaX() {
        return x;
    }

    @Override
    public int getTooltipAreaY() {
        return y;
    }

    @Override
    public int getTooltipAreaWidth() {
        return 16;
    }

    @Override
    public int getTooltipAreaHeight() {
        return 16;
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }
}