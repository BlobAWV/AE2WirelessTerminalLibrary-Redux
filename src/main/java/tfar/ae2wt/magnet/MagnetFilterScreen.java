package tfar.ae2wt.magnet;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import tfar.ae2wt.net.PacketHandler;
import tfar.ae2wt.net.server.C2SUpdateMagnetSettings;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;

public class MagnetFilterScreen extends ContainerScreen<MagnetFilterContainer> {

    private Button filterModeButton;
    private Button nbtMatchButton;

    public MagnetFilterScreen(MagnetFilterContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.xSize = 176;
        this.ySize = 221;
    }

    @Override
    protected void init() {
        super.init();
        filterModeButton = new Button(guiLeft + 8, guiTop + 17, 80, 20,
                new TranslationTextComponent("gui.ae2wtlib.filter_mode.whitelist"), btn -> toggleFilterMode());
        addButton(filterModeButton);

        nbtMatchButton = new Button(guiLeft + 96, guiTop + 17, 72, 20,
                new StringTextComponent("NBT: OFF"), btn -> toggleNbtMatch());
        addButton(nbtMatchButton);

        updateButtons();
    }

    private void toggleFilterMode() {
        String current = AbstractWirelessTerminalItem.getMagnetFilterMode(container.getTerminal());
        String newMode = current.equals("WHITELIST") ? "BLACKLIST" : "WHITELIST";
        PacketHandler.INSTANCE.sendToServer(new C2SUpdateMagnetSettings(newMode, AbstractWirelessTerminalItem.getMagnetNbtMatch(container.getTerminal())));
        container.setFilterMode(newMode);
        updateButtons();
    }

    private void toggleNbtMatch() {
        boolean current = AbstractWirelessTerminalItem.getMagnetNbtMatch(container.getTerminal());
        boolean newVal = !current;
        PacketHandler.INSTANCE.sendToServer(new C2SUpdateMagnetSettings(AbstractWirelessTerminalItem.getMagnetFilterMode(container.getTerminal()), newVal));
        container.setNbtMatch(newVal);
        updateButtons();
    }

    private void updateButtons() {
        String mode = AbstractWirelessTerminalItem.getMagnetFilterMode(container.getTerminal());
        filterModeButton.setMessage(new TranslationTextComponent("gui.ae2wtlib.filter_mode." + mode.toLowerCase()));

        boolean nbtMatch = AbstractWirelessTerminalItem.getMagnetNbtMatch(container.getTerminal());
        nbtMatchButton.setMessage(new StringTextComponent("NBT: " + (nbtMatch ? "ON" : "OFF")));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        Minecraft.getInstance().getTextureManager().bindTexture(new ResourceLocation("ae2wtlib", "textures/magnet_filter_gui.png"));
        blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        font.drawText(matrixStack, title, (float)titleX, (float)titleY, 0x404040);
    }
}