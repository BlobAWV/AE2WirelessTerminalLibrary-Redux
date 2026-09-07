package tfar.ae2wt.magnet;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class MagnetFilterProvider implements INamedContainerProvider {

    private final ItemStack terminal;

    public MagnetFilterProvider(ItemStack terminal) {
        this.terminal = terminal;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("gui.ae2wtlib.magnet_filter");
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
        return new MagnetFilterContainer(id, inv, terminal);
    }
}