package tfar.ae2wt.recipe;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import tfar.ae2wt.init.ModItems;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;

public class MagnetInstallRecipe extends SpecialRecipe {

    public static final ResourceLocation ID = new ResourceLocation("ae2wtlib", "magnet_install");

    public MagnetInstallRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        boolean hasTerminal = false;
        boolean hasMagnet = false;
        int count = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            count++;
            if (isTerminal(stack)) hasTerminal = true;
            else if (stack.getItem() == ModItems.MAGNET_CARD) hasMagnet = true;
            else return false;
        }
        return count == 2 && hasTerminal && hasMagnet;
    }

    @Override
    public ItemStack getCraftingResult(CraftingInventory inv) {
        ItemStack terminal = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (isTerminal(stack)) {
                terminal = stack.copy();
                break;
            }
        }
        if (terminal.isEmpty()) return ItemStack.EMPTY;
        AbstractWirelessTerminalItem.enableMagnetUpgrade(terminal);
        return terminal;
    }

    private boolean isTerminal(ItemStack stack) {
        return stack.getItem() instanceof AbstractWirelessTerminalItem;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return MagnetInstallSerializer.INSTANCE;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }
}