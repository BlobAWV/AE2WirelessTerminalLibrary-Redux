package tfar.ae2wt.wpt;

import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.guiobjects.IPortableCell;
import appeng.api.implementations.tiles.IViewCellStorage;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import tfar.ae2wt.init.Menus;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.terminal.SlotType;
import tfar.ae2wt.terminal.WTGuiObject;
import tfar.ae2wt.terminal.InternalInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import tfar.ae2wt.util.FluidCraftHelper;

import java.lang.ref.WeakReference;

public class WPTGuiObject extends WTGuiObject implements IPortableCell, IAEAppEngInventory, IViewCellStorage {

    private boolean craftingMode = true;
    private boolean substitute = false;
    private final AppEngInternalInventory crafting;
    private final AppEngInternalInventory output;
    private final AppEngInternalInventory pattern;
    private boolean fluidMode = false;
    private boolean fluidConversionEnabled = true;
    private WeakReference<WirelessPatternTerminalContainer> containerRef;

    public void setContainer(WirelessPatternTerminalContainer container) {
        this.containerRef = new WeakReference<>(container);
    }

    public boolean isFluidMode() { return fluidMode; }

    public void setFluidMode(boolean mode) {
        this.fluidMode = mode; }

    public boolean isFluidConversionEnabled() {
        return fluidConversionEnabled;
    }

    public void setFluidConversionEnabled(boolean enabled) {
        this.fluidConversionEnabled = enabled;
    }

    public WPTGuiObject(final IWirelessTermHandler wh, final ItemStack is, final PlayerEntity ep, int inventorySlot) {
        super(wh, is, ep, inventorySlot);
        this.fluidMode = AbstractWirelessTerminalItem.getBoolean(is, "fluidMode");
        crafting = new InternalInventory(this, 9, SlotType.pattern_crafting, is);
        output = new InternalInventory(this, 3, SlotType.output, is);
        pattern = new InternalInventory(this, 2, SlotType.pattern, is);
    }

    public boolean isCraftingRecipe() {
        return craftingMode;
    }

    public AppEngInternalInventory getInventoryByName(final String name) {
        if(name.equals("crafting")) return crafting;

        if(name.equals("output")) return output;

        if(name.equals("pattern")) return pattern;

        return null;
    }

    @Override
    public void saveChanges() {}

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation mc, ItemStack removedStack, ItemStack newStack) {
        if (inv == pattern && slot == 1) {
            final ItemStack is = pattern.getStackInSlot(1);
            final ICraftingPatternDetails details = Api.instance().crafting().decodePattern(is, getPlayer().world, false);
            if (details != null) {
                for (int i = 0; i < crafting.getSlots(); i++) {
                    crafting.setStackInSlot(i, ItemStack.EMPTY);
                }
                for (int i = 0; i < output.getSlots(); i++) {
                    output.setStackInSlot(i, ItemStack.EMPTY);
                }

                setCraftingMode(details.isCraftable());
                if (details.isCraftable()) {
                    setSubstitution(details.canSubstitute());
                }

                if (FluidCraftHelper.PRESENT && FluidCraftHelper.isFluidEncodedPattern(is)) {
                    setCraftingMode(false);
                    setFluidMode(true);
                } else {
                    setFluidMode(false);
                }

                for (int x = 0; x < crafting.getSlots() && x < details.getSparseInputs().length; x++) {
                    final IAEItemStack item = details.getSparseInputs()[x];
                    ItemStack stack = item == null ? ItemStack.EMPTY : item.createItemStack();
                    stack = convertToFluidPacket(stack);
                    crafting.setStackInSlot(x, stack);
                }

                for (int x = 0; x < output.getSlots() && x < details.getSparseOutputs().length; x++) {
                    final IAEItemStack item = details.getSparseOutputs()[x];
                    ItemStack stack = item == null ? ItemStack.EMPTY : item.createItemStack();
                    stack = convertToFluidPacket(stack);
                    output.setStackInSlot(x, stack);
                }

                WirelessPatternTerminalContainer container = containerRef.get();
                if (container != null) {

                    if (details.isCraftable()) {
                        container.getAndUpdateOutput();
                        container.detectAndSendChanges();
                    }
                    container.detectAndSendChanges();
                }
            }

        } else if (inv == crafting) {
            fixCraftingRecipes();
        }
    }

    public ItemStack convertToFluidPacket(ItemStack stack) {
        if (!FluidCraftHelper.PRESENT || stack.isEmpty()) {
            return stack;
        }

        if (isFluidMode()) {
            if (FluidCraftHelper.isFluidPacket(stack)) {
                return stack;
            }

            if (FluidCraftHelper.isFluidDrop(stack)) {
                FluidStack fluid = FluidCraftHelper.getFluidFromDrop(stack);
                if (!fluid.isEmpty()) {
                    return FluidCraftHelper.newFluidPacket(fluid);
                }
            }

            if (fluidConversionEnabled) {
                FluidStack fluid = FluidCraftHelper.getFluidFromItem(stack);
                if (!fluid.isEmpty()) {
                    return FluidCraftHelper.newFluidPacket(fluid);
                }
            }
            else {
                return stack;
            }

        }

        return stack;
    }

    @Override
    public boolean isRemote() {
        return getPlayer().world.isRemote;
    }

    public void setCraftingMode(final boolean craftingMode) {
        this.craftingMode = craftingMode;
        fixCraftingRecipes();
    }

    public boolean isSubstitution() {
        return this.substitute;
    }

    public void setSubstitution(final boolean canSubstitute) {
        this.substitute = canSubstitute;
    }

    private void fixCraftingRecipes() {
        if(craftingMode) for(int x = 0; x < crafting.getSlots(); x++) {
            final ItemStack is = crafting.getStackInSlot(x);
            if(!is.isEmpty()) is.setCount(1);
        }
    }

    @Override
    public ContainerType<?> getType() {
        return Menus.PATTERN;
    }
}