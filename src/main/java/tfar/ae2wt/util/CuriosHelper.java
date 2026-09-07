package tfar.ae2wt.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.wut.WUTHandler;


import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public class CuriosHelper {

    public static final boolean CURIOS_PRESENT = ModList.get().isLoaded("curios");

    @Nullable
    private static Class<?> curiosApiClass;
    @Nullable
    private static Method getCuriosHandlerMethod;

    static {
        if (CURIOS_PRESENT) {
            try {
                curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                getCuriosHandlerMethod = curiosApiClass.getMethod("getCuriosHandler", PlayerEntity.class);
            } catch (Exception e) {
                //ignore
            }
        }
    }

    public static boolean isTerminalInCurios(PlayerEntity player, ItemStack terminal) {
        if (!CURIOS_PRESENT || player == null || terminal.isEmpty()) return false;
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosHelperMethod = curiosApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHelperMethod.invoke(null);
            Method getCuriosHandlerMethod = curiosHelper.getClass().getMethod("getCuriosHandler", LivingEntity.class);
            Object lazyOptional = getCuriosHandlerMethod.invoke(curiosHelper, player);
            Method resolveMethod = lazyOptional.getClass().getMethod("resolve");
            Optional<?> optional = (Optional<?>) resolveMethod.invoke(lazyOptional);
            if (!optional.isPresent()) return false;
            Object handler = optional.get();
            Method getCuriosMethod = handler.getClass().getMethod("getCurios");
            Map<?, ?> curiosMap = (Map<?, ?>) getCuriosMethod.invoke(handler);
            for (Object value : curiosMap.values()) {
                Method getStacksMethod = value.getClass().getMethod("getStacks");
                Object stacksHandler = getStacksMethod.invoke(value);
                if (stacksHandler instanceof IItemHandler) {
                    IItemHandler itemHandler = (IItemHandler) stacksHandler;
                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        ItemStack stack = itemHandler.getStackInSlot(i);
                        if (stack == terminal) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    @Nullable
    public static ItemStack findFirstTerminal(PlayerEntity player) {
        if (!CURIOS_PRESENT || player == null) return ItemStack.EMPTY;
        try {

            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosHelperMethod = curiosApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHelperMethod.invoke(null);


            Method getCuriosHandlerMethod = curiosHelper.getClass().getMethod("getCuriosHandler", LivingEntity.class);
            Object lazyOptional = getCuriosHandlerMethod.invoke(curiosHelper, player);


            Method resolveMethod = lazyOptional.getClass().getMethod("resolve");
            Optional<?> optional = (Optional<?>) resolveMethod.invoke(lazyOptional);
            if (!optional.isPresent()) {

                return ItemStack.EMPTY;
            }
            Object handler = optional.get();


            Method getCuriosMethod = handler.getClass().getMethod("getCurios");
            Map<?, ?> curiosMap = (Map<?, ?>) getCuriosMethod.invoke(handler);



            for (Object value : curiosMap.values()) {
                Method getStacksMethod = value.getClass().getMethod("getStacks");
                Object stacksHandler = getStacksMethod.invoke(value);
                if (stacksHandler instanceof IItemHandler) {
                    IItemHandler itemHandler = (IItemHandler) stacksHandler;

                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        ItemStack stack = itemHandler.getStackInSlot(i);
                        if (isWirelessTerminal(stack)) {

                            return stack;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ItemStack.EMPTY;
    }


    public static boolean isWirelessTerminal(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof tfar.ae2wt.terminal.AbstractWirelessTerminalItem ||
                stack.getItem() instanceof tfar.ae2wt.wut.WUTItem;
    }

    public static ItemStack findTerminalOfType(PlayerEntity player, String type) {
        if (!CURIOS_PRESENT || player == null) return ItemStack.EMPTY;
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosHelperMethod = curiosApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHelperMethod.invoke(null);
            Method getCuriosHandlerMethod = curiosHelper.getClass().getMethod("getCuriosHandler", LivingEntity.class);
            Object lazyOptional = getCuriosHandlerMethod.invoke(curiosHelper, player);
            Method resolveMethod = lazyOptional.getClass().getMethod("resolve");
            Optional<?> optional = (Optional<?>) resolveMethod.invoke(lazyOptional);
            if (!optional.isPresent()) return ItemStack.EMPTY;
            Object handler = optional.get();
            Method getCuriosMethod = handler.getClass().getMethod("getCurios");
            Map<?, ?> curiosMap = (Map<?, ?>) getCuriosMethod.invoke(handler);
            for (Object value : curiosMap.values()) {
                Method getStacksMethod = value.getClass().getMethod("getStacks");
                Object stacksHandler = getStacksMethod.invoke(value);
                if (stacksHandler instanceof IItemHandler) {
                    IItemHandler itemHandler = (IItemHandler) stacksHandler;
                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        ItemStack stack = itemHandler.getStackInSlot(i);
                        if (matchesTerminalType(stack, type)) {
                            return stack;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ItemStack.EMPTY;
    }

    public static boolean matchesTerminalType(ItemStack stack, String type) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (type.equals("crafting") && item instanceof tfar.ae2wt.wirelesscraftingterminal.WCTItem) return true;
        if (type.equals("pattern") && item instanceof tfar.ae2wt.wpt.WPTItem) return true;
        if (type.equals("interface") && item instanceof tfar.ae2wt.wirelessinterfaceterminal.WITItem) return true;
        if (type.equals("fluid") && item instanceof tfar.ae2wt.wirelessfluidterminal.WirelessFluidTerminalItem) return true;
        if (type.equals("chemical") && ChemicalHelper.CHEMICALS_PRESENT && item instanceof tfar.ae2wt.wirelesschemicalterminal.WirelessChemicalTerminalItem) return true;
        if (item instanceof tfar.ae2wt.wut.WUTItem && WUTHandler.hasTerminal(stack, type)) return true;
        return false;
    }

    public static ItemStack findTerminalWithMagnet(PlayerEntity player) {
        if (!CURIOS_PRESENT || player == null) return ItemStack.EMPTY;
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosHelperMethod = curiosApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHelperMethod.invoke(null);
            Method getCuriosHandlerMethod = curiosHelper.getClass().getMethod("getCuriosHandler", LivingEntity.class);
            Object lazyOptional = getCuriosHandlerMethod.invoke(curiosHelper, player);
            Method resolveMethod = lazyOptional.getClass().getMethod("resolve");
            Optional<?> optional = (Optional<?>) resolveMethod.invoke(lazyOptional);
            if (!optional.isPresent()) return ItemStack.EMPTY;
            Object handler = optional.get();
            Method getCuriosMethod = handler.getClass().getMethod("getCurios");
            Map<?, ?> curiosMap = (Map<?, ?>) getCuriosMethod.invoke(handler);
            for (Object value : curiosMap.values()) {
                Method getStacksMethod = value.getClass().getMethod("getStacks");
                Object stacksHandler = getStacksMethod.invoke(value);
                if (stacksHandler instanceof IItemHandler) {
                    IItemHandler itemHandler = (IItemHandler) stacksHandler;
                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        ItemStack stack = itemHandler.getStackInSlot(i);
                        if (isTerminalWithMagnet(stack)) {
                            return stack;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ItemStack.EMPTY;
    }

    private static boolean isTerminalWithMagnet(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return false;
        return AbstractWirelessTerminalItem.hasMagnetUpgrade(stack);
    }
}