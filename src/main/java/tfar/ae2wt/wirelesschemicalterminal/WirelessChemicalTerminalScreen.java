package tfar.ae2wt.wirelesschemicalterminal;

import appeng.client.gui.me.common.MEMonitorableScreen;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IScrollSource;
import appeng.container.me.common.GridInventoryEntry;
import appeng.container.me.common.MEMonitorableContainer;
import appeng.helpers.InventoryAction;
import appeng.util.prioritylist.IPartitionList;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import tfar.ae2wt.ae2copies.ChemicalRepo;
import tfar.ae2wt.util.ChemicalHelper;
import tfar.ae2wt.wut.CycleTerminalButton;
import tfar.ae2wt.wut.IUniversalTerminalCapable;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WirelessChemicalTerminalScreen extends MEMonitorableScreen implements IUniversalTerminalCapable {
    // Raw type because we don't have IAEChemicalStack at compile time

    public WirelessChemicalTerminalScreen(WirelessChemicalTerminalContainer container, PlayerInventory playerInventory, ITextComponent title, ScreenStyle style) {
        super(container, playerInventory, title, style);
        if (container.isWUT()) {
            widgets.add("cycleTerminal", new CycleTerminalButton(btn -> cycleTerminal()));
        }
    }

    @Override
    protected Repo createRepo(IScrollSource scrollSource) {
        return new ChemicalRepo(scrollSource, this);
    }

    @Nullable
    @Override
    protected IPartitionList createPartitionList(List list) {
        return null;
    }

    @Override
    protected void renderGridInventoryEntry(MatrixStack matrices, int x, int y, GridInventoryEntry entry) {
        Object chemicalStack = entry.getStack();
        if (chemicalStack != null) {
            long amount = entry.getStoredAmount();
            ChemicalHelper.renderChemicalIntoSlot(matrices, x, y, 16, 16, chemicalStack, amount);
        }
    }

    public static WirelessChemicalTerminalScreen create(WirelessChemicalTerminalContainer container, PlayerInventory playerInventory, ITextComponent title, ScreenStyle style) {
        return new WirelessChemicalTerminalScreen(container, playerInventory, title, style);
    }

    @Override
    protected void renderGridInventoryEntryTooltip(MatrixStack matrices, GridInventoryEntry entry, int x, int y) {
        Object chemicalStack = entry.getStack();
        if (chemicalStack == null) return;

        String formattedAmount = NumberFormat.getNumberInstance(Locale.US).format((double) entry.getStoredAmount() / 1000.0D) + " B";
        String modName = ChemicalHelper.getChemicalModId(chemicalStack);
        String displayName = ChemicalHelper.getChemicalDisplayName(chemicalStack);

        List<ITextComponent> list = new ArrayList<>();
        list.add(new StringTextComponent(displayName));
        list.add(new StringTextComponent(formattedAmount));
        list.add(new StringTextComponent(modName).mergeStyle(TextFormatting.BLUE, TextFormatting.ITALIC));
        this.renderWrappedToolTip(matrices, list, x, y, this.font);
    }

    @Override
    protected void handleGridInventoryEntryMouseClick(@Nullable GridInventoryEntry entry, int mouseButton, ClickType clickType) {
        if (clickType == ClickType.PICKUP) {
            if (mouseButton == 0 && entry != null) {
                ((MEMonitorableContainer) this.container).handleInteraction(entry.getSerial(), InventoryAction.FILL_ITEM);
            } else {
                ((MEMonitorableContainer) this.container).handleInteraction(-1L, InventoryAction.EMPTY_ITEM);
            }
        }
    }
}