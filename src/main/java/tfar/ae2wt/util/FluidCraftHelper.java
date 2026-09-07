package tfar.ae2wt.util;

import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.fluids.client.gui.FluidBlitter;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FluidCraftHelper {
    public static final boolean PRESENT = ModList.get().isLoaded("ae2fc");
    private static Class<?> itemFluidEncodedPatternClass;
    private static Class<?> itemFluidPacketClass;
    private static Class<?> fcItemsClass;
    private static Method encodeStackMethod;
    private static Method getFluidStackMethod;
    private static Method newStackMethod;
    private static Object denseEncodedPatternInstance;
    private static Class<?> itemFluidDropClass;

    static {
        if (PRESENT) {
            try {
                itemFluidEncodedPatternClass = Class.forName("com.glodblock.github.common.item.ItemFluidEncodedPattern");
                itemFluidPacketClass = Class.forName("com.glodblock.github.common.item.ItemFluidPacket");
                fcItemsClass = Class.forName("com.glodblock.github.loader.FCItems");
                denseEncodedPatternInstance = fcItemsClass.getField("DENSE_ENCODED_PATTERN").get(null);
                encodeStackMethod = denseEncodedPatternInstance.getClass().getMethod("encodeStack", ItemStack[].class, ItemStack[].class);
                getFluidStackMethod = itemFluidPacketClass.getMethod("getFluidStack", ItemStack.class);
                newStackMethod = itemFluidPacketClass.getMethod("newStack", FluidStack.class);
                itemFluidDropClass = Class.forName("com.glodblock.github.common.item.ItemFluidDrop");

            } catch (Exception e) {
                throw new RuntimeException("AE2wtlib: Failed to initialize FluidCraft Helper", e);
            }
        }
    }
    public static boolean isFluidEncodedPattern(ItemStack stack) {
        if (!PRESENT || stack.isEmpty()) return false;
        return itemFluidEncodedPatternClass.isInstance(stack.getItem());
    }

    public static boolean isFluidDrop(ItemStack stack) {
        if (!PRESENT || stack.isEmpty()) return false;
        return itemFluidDropClass.isInstance(stack.getItem());
    }

    public static ItemStack encodeFluidPattern(ItemStack[] inputs, ItemStack[] outputs) {
        if (!PRESENT) return ItemStack.EMPTY;
        try {
            List<ItemStack> inputList = new ArrayList<>();
            for (ItemStack stack : inputs) {
                if (stack != null && !stack.isEmpty()) {
                    inputList.add(stack);
                }
            }
            List<ItemStack> outputList = new ArrayList<>();
            for (ItemStack stack : outputs) {
                if (stack != null && !stack.isEmpty()) {
                    outputList.add(stack);
                }
            }
            if (inputList.isEmpty() || outputList.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack[] in = inputList.toArray(new ItemStack[0]);
            ItemStack[] out = outputList.toArray(new ItemStack[0]);

            return (ItemStack) encodeStackMethod.invoke(denseEncodedPatternInstance, (Object) in, (Object) out);
        } catch (Exception e) {
            e.printStackTrace();
            return ItemStack.EMPTY;
        }
    }


    public static FluidStack getFluidFromPacket(ItemStack stack) {
        if (!PRESENT || stack.isEmpty()) return FluidStack.EMPTY;
        try {
            return (FluidStack) getFluidStackMethod.invoke(null, stack);
        } catch (Exception e) {
            return FluidStack.EMPTY;
        }
    }

    public static ItemStack newFluidPacket(FluidStack fluid) {
        if (!PRESENT || fluid.isEmpty()) return ItemStack.EMPTY;
        try {
            return (ItemStack) newStackMethod.invoke(null, fluid);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static FluidStack getFluidFromItem(ItemStack stack) {
        if (!PRESENT || stack.isEmpty()) return FluidStack.EMPTY;
        LazyOptional<IFluidHandlerItem> cap = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
        if (cap.isPresent()) {
            IFluidHandlerItem handler = cap.resolve().orElse(null);
            if (handler != null) {
                FluidStack fluid = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                if (!fluid.isEmpty()) {
                    return fluid;
                }
            }
        }
        return FluidStack.EMPTY;
    }

    public static boolean isFluidPacket(ItemStack stack) {
        if (!PRESENT || stack.isEmpty()) return false;
        return itemFluidPacketClass.isInstance(stack.getItem());
    }


    public static void renderFluidPacketIntoSlot(MatrixStack matrixStack, Slot slot, ItemStack stack,
                                                 @Nullable StackSizeRenderer renderer, FontRenderer font, int blitOffset) {
        FluidStack fluid = getFluidFromPacket(stack);
        if (!fluid.isEmpty()) {
            FluidBlitter.create(fluid).dest(slot.xPos, slot.yPos, 16, 16).blit(matrixStack, blitOffset);
            if (renderer != null) {
                renderer.renderStackSize(font, fluid.getAmount(), false, slot.xPos, slot.yPos);
            }
        }
    }

    public static FluidStack getFluidFromDrop(ItemStack stack) {
        if (!PRESENT || stack.isEmpty() || !isFluidDrop(stack)) return FluidStack.EMPTY;
        try {
            Method method = itemFluidDropClass.getMethod("getFluidStack", ItemStack.class);
            return (FluidStack) method.invoke(null, stack);
        } catch (Exception e) {
            return FluidStack.EMPTY;
        }
    }
}