package tfar.ae2wt.wpt;

import appeng.api.config.ActionItems;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.gui.me.items.ItemTerminalScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.TerminalStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.TabButton;
import appeng.container.SlotSemantic;
import appeng.core.localization.GuiText;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.ModList;
import tfar.ae2wt.net.PacketHandler;
import tfar.ae2wt.net.server.*;
import tfar.ae2wt.util.FluidCraftHelper;
import tfar.ae2wt.util.GuiStyleHelper;
import tfar.ae2wt.util.ItemButton;
import tfar.ae2wt.wut.CycleTerminalButton;
import tfar.ae2wt.wut.IUniversalTerminalCapable;

import java.util.Arrays;

public class WirelessPatternTerminalScreen extends ItemTerminalScreen<WirelessPatternTerminalContainer> implements IUniversalTerminalCapable {

    private static final boolean FLUID_CRAFT_PRESENT = ModList.get().isLoaded("ae2fc");

    private static final boolean SUBSITUTION_DISABLE = false;
    private static final boolean SUBSITUTION_ENABLE = true;
    private static final boolean CRAFTMODE_CRAFTING = true;
    private static final boolean CRAFTMODE_PROCESSING = false;

    private final TabButton tabCraftButton;
    private final TabButton tabProcessButton;
    private final ActionButton substitutionsEnabledBtn;
    private final ActionButton substitutionsDisabledBtn;

    private final ItemButton fluidModeEnabledBtn;
    private final ItemButton fluidModeDisabledBtn;
    private static final ResourceLocation FLUID_MODE_ENABLED = new ResourceLocation("ae2wtlib", "textures/fluid_mode_enabled.png");
    private static final ResourceLocation FLUID_MODE_DISABLED = new ResourceLocation("ae2wtlib", "textures/fluid_mode_disabled.png");

    private final ItemButton fluidConversionEnabledBtn;
    private final ItemButton fluidConversionDisabledBtn;
    private static final ResourceLocation CONVERSION_ENABLED = new ResourceLocation("ae2wtlib", "textures/fluid_conversion_enabled.png");
    private static final ResourceLocation CONVERSION_DISABLED = new ResourceLocation("ae2wtlib", "textures/fluid_conversion_disabled.png");


    public WirelessPatternTerminalScreen(WirelessPatternTerminalContainer container, PlayerInventory playerInventory, ITextComponent title, ScreenStyle style) {
        super(container, playerInventory, title, style);

        tabCraftButton = new TabButton(new ItemStack(Blocks.CRAFTING_TABLE), GuiText.CraftingPattern.text(), itemRenderer, btn -> toggleCraftMode(CRAFTMODE_PROCESSING));
        widgets.add("craftingPatternMode", tabCraftButton);

        tabProcessButton = new TabButton(new ItemStack(Blocks.FURNACE), GuiText.ProcessingPattern.text(), itemRenderer, btn -> toggleCraftMode(CRAFTMODE_CRAFTING));
        widgets.add("processingPatternMode", tabProcessButton);

        substitutionsEnabledBtn = new ActionButton(ActionItems.ENABLE_SUBSTITUTION, act -> toggleSubstitutions(SUBSITUTION_DISABLE));
        substitutionsEnabledBtn.setHalfSize(true);
        widgets.add("substitutionsEnabled", substitutionsEnabledBtn);

        substitutionsDisabledBtn = new ActionButton(ActionItems.DISABLE_SUBSTITUTION, act -> toggleSubstitutions(SUBSITUTION_ENABLE));
        substitutionsDisabledBtn.setHalfSize(true);
        widgets.add("substitutionsDisabled", substitutionsDisabledBtn);

        ActionButton clearBtn = new ActionButton(ActionItems.CLOSE, btn -> clear());
        clearBtn.setHalfSize(true);
        widgets.add("clearPattern", clearBtn);
        widgets.add("encodePattern", new ActionButton(ActionItems.ENCODE, act -> encode()));

        if (FLUID_CRAFT_PRESENT) {
            fluidModeEnabledBtn = new ItemButton(FLUID_MODE_ENABLED, btn -> toggleFluidMode(false));
            fluidModeEnabledBtn.setHalfSize(true);
            fluidModeEnabledBtn.setTooltipLines(Arrays.asList(
                    new TranslationTextComponent("gui.ae2wtlib.fluid_mode.enabled"),
                    new TranslationTextComponent("gui.ae2wtlib.fluid_mode.enabled.desc")
            ));
            widgets.add("fluidModeEnabled", fluidModeEnabledBtn);

            fluidModeDisabledBtn = new ItemButton(FLUID_MODE_DISABLED, btn -> toggleFluidMode(true));
            fluidModeDisabledBtn.setHalfSize(true);
            fluidModeDisabledBtn.setTooltipLines(Arrays.asList(
                    new TranslationTextComponent("gui.ae2wtlib.fluid_mode.disabled"),
                    new TranslationTextComponent("gui.ae2wtlib.fluid_mode.disabled.desc")
            ));
            widgets.add("fluidModeDisabled", fluidModeDisabledBtn);

            fluidConversionEnabledBtn = new ItemButton(CONVERSION_ENABLED, btn -> toggleFluidConversion(false));
            fluidConversionEnabledBtn.setHalfSize(true);
            fluidConversionEnabledBtn.setTooltipLines(Arrays.asList(
                    new TranslationTextComponent("gui.ae2wtlib.fluid_conversion.enabled"),
                    new TranslationTextComponent("gui.ae2wtlib.fluid_conversion.enabled.desc")
            ));
            widgets.add("fluidConversionEnabled", fluidConversionEnabledBtn);

            fluidConversionDisabledBtn = new ItemButton(CONVERSION_DISABLED, btn -> toggleFluidConversion(true));
            fluidConversionDisabledBtn.setHalfSize(true);
            fluidConversionDisabledBtn.setTooltipLines(Arrays.asList(
                    new TranslationTextComponent("gui.ae2wtlib.fluid_conversion.disabled"),
                    new TranslationTextComponent("gui.ae2wtlib.fluid_conversion.disabled.desc")
            ));
            widgets.add("fluidConversionDisabled", fluidConversionDisabledBtn);

        } else {
            fluidModeEnabledBtn = null;
            fluidModeDisabledBtn = null;
            fluidConversionEnabledBtn = null;
            fluidConversionDisabledBtn = null;
        }

        if (container.isWUT()) widgets.add("cycleTerminal", new CycleTerminalButton(btn -> cycleTerminal()));
    }

    private void toggleCraftMode(boolean mode) {
        PacketHandler.INSTANCE.sendToServer(new C2STogglePatternCraftingModePacket(mode));
    }

    private void toggleSubstitutions(boolean mode) {
        PacketHandler.INSTANCE.sendToServer(new C2STogglePatternSubsitutionPacket(mode));
    }

    private void toggleFluidMode(boolean newMode) {
        if (FLUID_CRAFT_PRESENT) {
            PacketHandler.INSTANCE.sendToServer(new C2STogglePatternFluidModePacket(newMode));
        }
    }

    private void toggleFluidConversion(boolean newState) {
        if (FLUID_CRAFT_PRESENT) {
            PacketHandler.INSTANCE.sendToServer(new C2SToggleFluidConversionPacket(newState));
        }
    }

    private void encode() {
        PacketHandler.INSTANCE.sendToServer(new C2SEncodePatternPacket());
    }

    private void clear() {
        PacketHandler.INSTANCE.sendToServer(new C2SClearPatternPacket());
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (container.isCraftingMode()) {
            tabCraftButton.visible = true;
            tabProcessButton.visible = false;
            // let's see if removing this line will break anything :)
            //toggleFluidMode(false);

            if (container.substitute) {
                substitutionsEnabledBtn.visible = true;
                substitutionsDisabledBtn.visible = false;
            } else {
                substitutionsEnabledBtn.visible = false;
                substitutionsDisabledBtn.visible = true;
            }
        } else {
            tabCraftButton.visible = false;
            tabProcessButton.visible = true;
            substitutionsEnabledBtn.visible = false;
            substitutionsDisabledBtn.visible = false;
        }

        if (FLUID_CRAFT_PRESENT && fluidModeEnabledBtn != null && fluidModeDisabledBtn != null && fluidConversionEnabledBtn != null && fluidConversionDisabledBtn != null) {
            boolean isProcessing = !container.isCraftingMode();
            boolean conversionState = container.fluidConversionEnabled;

            fluidModeEnabledBtn.visible = isProcessing && container.isFluidMode();
            fluidModeDisabledBtn.visible = isProcessing && !container.isFluidMode();

            fluidConversionEnabledBtn.visible = isProcessing && conversionState && container.isFluidMode();
            fluidConversionDisabledBtn.visible = isProcessing && !conversionState && container.isFluidMode();

            if (isProcessing && container.isFluidMode()) {
                substitutionsEnabledBtn.visible = false;
                substitutionsDisabledBtn.visible = false;

            }



        }

        setSlotsHidden(SlotSemantic.CRAFTING_RESULT, !container.isCraftingMode());
        setSlotsHidden(SlotSemantic.PROCESSING_RESULT, container.isCraftingMode());
    }

    @Override
    protected void moveItems(MatrixStack matrices, Slot slot) {
        if (FLUID_CRAFT_PRESENT && container.isFluidMode()) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && FluidCraftHelper.isFluidPacket(stack)) {
                TerminalStyle terminalStyle = GuiStyleHelper.getGuiStyle(this);
                StackSizeRenderer renderer = terminalStyle != null ? terminalStyle.getStackSizeRenderer() : null;
                FluidCraftHelper.renderFluidPacketIntoSlot(matrices, slot, stack, renderer, this.font, getBlitOffset());
                return;
            }
        }
        super.moveItems(matrices, slot);
    }

    @Override
    public void drawBG(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(matrixStack, offsetX, offsetY, mouseX, mouseY, partialTicks);
        if (!container.isCraftingMode()) {
            Blitter.texture("guis/pattern_modes.png").src(100, 77, 18, 54).dest(guiLeft + 109, guiTop + ySize - 159).blit(matrixStack, getBlitOffset());
        }
    }
}