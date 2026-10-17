package tfar.ae2wt.util;

import appeng.api.util.AEPartLocation;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerLocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import tfar.ae2wt.wut.WUTItem;

import java.lang.reflect.Constructor;

public final class ContainerHelper {

    /**
     * creates a @link ContainerLocator} for any Inventory Slot since it's constructor is private and there is no static method which directly allows this
     * @param slot the slot the container is in
     * @return The new {@link ContainerLocator}
     */
    public static ContainerLocator getContainerLocatorForSlot(int slot) {
        try {
            Object containerLocatorTypePLAYER_INVENTORY = null;
            Class<?> containerLocatorTypeClass = Class.forName("appeng.container.ContainerLocator$Type");
            for (Object obj : containerLocatorTypeClass.getEnumConstants()) {
                if(obj.toString().equals("PLAYER_INVENTORY")) {
                    containerLocatorTypePLAYER_INVENTORY = obj;
                    break;
                }
            }

            Constructor<ContainerLocator> constructor = ContainerLocator.class.getDeclaredConstructor(containerLocatorTypeClass, int.class, ResourceLocation.class, BlockPos.class, AEPartLocation.class);
            constructor.setAccessible(true);
            ContainerLocator containerLocator = constructor.newInstance(containerLocatorTypePLAYER_INVENTORY, slot, null, null, null);
            constructor.setAccessible(false);
            return containerLocator;
        } catch(Exception ignored) {}
        return null;
    }

    public static ContainerLocator curiosLocator() {
        return getContainerLocatorForSlot(-1);   // -1 = "in curios"
    }

    public static ItemStack resolveTerminal(PlayerEntity player, ContainerLocator locator, Class<? extends Item> primaryClass) {
        // Try the locator slot if it is a real, non‑negative index
        if (locator != null && locator.hasItemIndex()) {
            int idx = locator.getItemIndex();
            if (idx >= 0 && idx < player.inventory.getSizeInventory()) {
                ItemStack s = player.inventory.getStackInSlot(idx);
                if (primaryClass.isInstance(s.getItem()) || s.getItem() instanceof WUTItem) {
                    return s;
                }
            }
        }
        // Fallback to curios (covers the sentinel as well as invalid slots)
        if (CuriosHelper.CURIOS_PRESENT) {
            ItemStack s = CuriosHelper.findFirstTerminal(player);
            if (!s.isEmpty() && (primaryClass.isInstance(s.getItem()) || s.getItem() instanceof WUTItem)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }
}