package tfar.ae2wt.magnet;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import tfar.ae2wt.init.Menus;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;

import javax.annotation.Nonnull;

public class MagnetFilterContainer extends Container {

    private final ItemStack terminal;
    private final ItemStackHandler filterHandler;
    private final int filterSlots = 16;

    public MagnetFilterContainer(int windowId, PlayerInventory inv, ItemStack terminal) {
        super(Menus.MAGNET_FILTER_CONTAINER, windowId);
        this.terminal = terminal;
        this.filterHandler = new ItemStackHandler(filterSlots) {
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                saveFiltersToNBT();
                detectAndSendChanges();
            }
        };

        loadFiltersFromNBT();

        int startX = 53;
        int startY = 56;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int slotIndex = row * 4 + col;
                int x = startX + col * 18;
                int y = startY + row * 18;
                addSlot(new FilterSlot(filterHandler, slotIndex, x, y));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 139 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 197));
        }
    }

    private void loadFiltersFromNBT() {
        ListNBT filters = AbstractWirelessTerminalItem.getMagnetFilters(terminal);
        for (int i = 0; i < filterSlots && i < filters.size(); i++) {
            CompoundNBT tag = filters.getCompound(i);
            filterHandler.setStackInSlot(i, ItemStack.read(tag));
        }
    }

    public void saveFiltersToNBT() {
        ListNBT list = new ListNBT();
        for (int i = 0; i < filterHandler.getSlots(); i++) {
            ItemStack stack = filterHandler.getStackInSlot(i);
            CompoundNBT tag = new CompoundNBT();
            if (!stack.isEmpty()) stack.write(tag);
            list.add(tag);
        }
        AbstractWirelessTerminalItem.setMagnetFilters(terminal, list);
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, PlayerEntity player) {
        if (slotId >= 0 && slotId < inventorySlots.size()) {
            Slot slot = inventorySlots.get(slotId);
            if (slot instanceof FilterSlot) {
                ItemStack cursorStack = player.inventory.getItemStack();
                if (clickTypeIn == ClickType.PICKUP || clickTypeIn == ClickType.QUICK_MOVE) {
                    if (cursorStack.isEmpty()) {
                        slot.putStack(ItemStack.EMPTY);
                    } else {

                        ItemStack copy = cursorStack.copy();
                        copy.setCount(1);
                        slot.putStack(copy);
                    }

                    detectAndSendChanges();
                    return ItemStack.EMPTY;
                }
            }
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    public ItemStack getTerminal() {
        return terminal;
    }

    public IItemHandler getFilterHandler() {
        return filterHandler;
    }

    public void setFilterMode(String mode) {
        AbstractWirelessTerminalItem.setMagnetFilterMode(terminal, mode);
        saveFiltersToNBT();
    }

    public void setNbtMatch(boolean match) {
        AbstractWirelessTerminalItem.setMagnetNbtMatch(terminal, match);
        saveFiltersToNBT();
    }

    @Override
    public boolean canInteractWith(PlayerEntity player) {
        return true;
    }

    private static class FilterSlot extends SlotItemHandler {
        public FilterSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            return true;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }

        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canTakeStack(PlayerEntity playerIn) {
            return false;
        }
    }
}