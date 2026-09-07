package tfar.ae2wt.wirelesscraftingterminal;

import appeng.container.ContainerLocator;
import appeng.core.localization.GuiText;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public class TermFactory implements INamedContainerProvider {
    private final WCTGuiObject obj;
    private final ContainerLocator locator;

    public TermFactory(WCTGuiObject obj, ContainerLocator locator) {
        this.obj = obj;
        this.locator = locator;
    }

    @Override
    public ITextComponent getDisplayName() {
        return GuiText.Terminal.text();
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
        WirelessCraftingTerminalContainer c = new WirelessCraftingTerminalContainer(id, inv, obj);
        c.setLocator(locator);
        return c;
    }
}