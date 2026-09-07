package tfar.ae2wt.wirelesschemicalterminal;

import appeng.container.ContainerLocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public class TermFactory implements INamedContainerProvider {
    private final WChemGuiObject obj;
    private final ContainerLocator locator;

    public TermFactory(WChemGuiObject obj, ContainerLocator locator) {
        this.obj = obj;
        this.locator = locator;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new net.minecraft.util.text.TranslationTextComponent("gui.ae2wtlib.chemical_terminal");
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
        WirelessChemicalTerminalContainer c = new WirelessChemicalTerminalContainer(id, inv, obj);
        c.setLocator(locator);
        return c;
    }

}