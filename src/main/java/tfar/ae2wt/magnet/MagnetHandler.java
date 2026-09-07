package tfar.ae2wt.magnet;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import tfar.ae2wt.config.ModConfig;
import tfar.ae2wt.util.MagnetHelper;
import tfar.ae2wt.wirelesscraftingterminal.CraftingTerminalHandler;

import java.util.List;

public class MagnetHandler {

    private static int tickCounter = 0;
    private static final double RANGE = ModConfig.COMMON.magnetRange.get();
    private static final int TICK_INTERVAL = ModConfig.COMMON.magnetTickInterval.get();

    public static void doMagnet(PlayerEntity player) {

        tickCounter++;
        if (tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;


        if (player.isSneaking()) return;

        ItemStack terminal = MagnetHelper.getMagnetTerminal(player);
        if (terminal.isEmpty()) return;

        if (!MagnetHelper.isMagnetEnabled()) return;

        String pickupMode = MagnetHelper.getPickupMode();
        String filterMode = MagnetHelper.getFilterMode();
        boolean nbtMatch = MagnetHelper.isNbtMatch();
        List<ItemStack> filters = MagnetHelper.getFilters();

        if (filterMode.equals("WHITELIST") && filters.isEmpty()) return;

        List<ItemEntity> items = player.world.getEntitiesWithinAABB(
                ItemEntity.class,
                player.getBoundingBox().grow(RANGE),
                entity -> entity.isAlive() && !entity.getItem().isEmpty()
        );

        if (items.isEmpty()) return;

        boolean hasFilters = !filters.isEmpty();

        for (ItemEntity itemEntity : items) {

            if (itemEntity.getThrowerId() != null && itemEntity.getThrowerId().equals(player.getUniqueID())) {
                continue;
            }

            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;


            boolean matches = false;
            if (hasFilters) {
                for (ItemStack filter : filters) {
                    if (filter.isEmpty()) continue;
                    if (ItemStack.areItemsEqual(filter, stack)) {
                        if (nbtMatch) {
                            if (ItemStack.areItemStackTagsEqual(filter, stack)) {
                                matches = true;
                                break;
                            }
                        } else {
                            matches = true;
                            break;
                        }
                    }
                }
            }

            boolean shouldPickup = filterMode.equals("WHITELIST") ? matches : !matches;
            if (!shouldPickup) continue;


            if (pickupMode.equals("INVENTORY")) {
                if (player.inventory.addItemStackToInventory(stack.copy())) {
                    itemEntity.remove();
                }
            } else { //
                CraftingTerminalHandler handler = CraftingTerminalHandler.getCraftingTerminalHandler(player);
                if (handler.insertItemIntoME(stack)) {
                    itemEntity.remove();
                } else {

                    if (player.inventory.addItemStackToInventory(stack.copy())) {
                        itemEntity.remove();
                    }
                }
            }
        }
    }
}