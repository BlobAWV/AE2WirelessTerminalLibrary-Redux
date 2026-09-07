package tfar.ae2wt.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;

import java.util.ArrayList;
import java.util.List;

public class MagnetHelper {

    private static ItemStack cachedTerminal = ItemStack.EMPTY;
    private static List<ItemStack> cachedFilters = new ArrayList<>();
    private static String cachedFilterMode = "WHITELIST";
    private static boolean cachedNbtMatch = false;
    private static String cachedPickupMode = "INVENTORY";
    private static boolean cachedMagnetEnabled = false;
    private static boolean cachedHasUpgrade = false;
    private static int cachedHash = 0; // simple dirty check

    public static ItemStack getMagnetTerminal(PlayerEntity player) {
        PlayerInventory inv = player.inventory;

        if (!cachedTerminal.isEmpty()) {
            boolean valid = false;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.getStackInSlot(i) == cachedTerminal) {
                    valid = true;
                    break;
                }
            }

            if (!valid && CuriosHelper.CURIOS_PRESENT) {
                valid = CuriosHelper.isTerminalInCurios(player, cachedTerminal);
            }
            if (valid) {

                int currentHash = getNbtHash(cachedTerminal);
                if (currentHash != cachedHash) {
                    refreshCache(cachedTerminal);
                }
                return cachedTerminal;
            }

            clearCache();
        }


        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (isMagnetTerminal(stack)) {
                cachedTerminal = stack;
                refreshCache(stack);
                return cachedTerminal;
            }
        }


        if (CuriosHelper.CURIOS_PRESENT) {
            ItemStack curiosTerminal = findAnyMagnetTerminalInCurios(player);
            if (!curiosTerminal.isEmpty()) {
                cachedTerminal = curiosTerminal;
                refreshCache(curiosTerminal);
                return cachedTerminal;
            }
        }

        clearCache();
        return ItemStack.EMPTY;
    }

    private static void refreshCache(ItemStack stack) {
        cachedHasUpgrade = AbstractWirelessTerminalItem.hasMagnetUpgrade(stack);
        cachedMagnetEnabled = AbstractWirelessTerminalItem.isMagnetEnabled(stack);
        cachedPickupMode = AbstractWirelessTerminalItem.getMagnetPickupMode(stack);
        cachedFilterMode = AbstractWirelessTerminalItem.getMagnetFilterMode(stack);
        cachedNbtMatch = AbstractWirelessTerminalItem.getMagnetNbtMatch(stack);
        cachedFilters = AbstractWirelessTerminalItem.getMagnetFiltersAsList(stack);
        cachedHash = getNbtHash(stack);
    }

    private static int getNbtHash(ItemStack stack) {

        CompoundNBT tag = stack.getTag();
        if (tag == null) return 0;
        int hash = 0;
        hash = 31 * hash + (tag.getBoolean("magnetEnabled") ? 1 : 0);
        hash = 31 * hash + tag.getString("magnetPickupMode").hashCode();
        hash = 31 * hash + tag.getString("magnetFilterMode").hashCode();
        hash = 31 * hash + (tag.getBoolean("magnetNbtMatch") ? 1 : 0);

        ListNBT filters = tag.getList("magnetFilters", 10);
        hash = 31 * hash + filters.hashCode();
        return hash;
    }

    private static boolean isMagnetTerminal(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return false;
        return AbstractWirelessTerminalItem.hasMagnetUpgrade(stack);
    }

    private static ItemStack findAnyMagnetTerminalInCurios(PlayerEntity player) {
        if (!CuriosHelper.CURIOS_PRESENT) return ItemStack.EMPTY;
        return CuriosHelper.findTerminalWithMagnet(player);
    }

    public static void clearCache() {
        cachedTerminal = ItemStack.EMPTY;
        cachedFilters.clear();
        cachedHash = 0;
    }


    public static boolean isMagnetEnabled() {
        return cachedMagnetEnabled;
    }

    public static String getPickupMode() {
        return cachedPickupMode;
    }

    public static String getFilterMode() {
        return cachedFilterMode;
    }

    public static boolean isNbtMatch() {
        return cachedNbtMatch;
    }

    public static List<ItemStack> getFilters() {
        return cachedFilters;
    }
}