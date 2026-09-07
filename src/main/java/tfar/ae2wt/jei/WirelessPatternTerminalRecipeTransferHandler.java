package tfar.ae2wt.jei;

import com.mojang.blaze3d.matrix.MatrixStack;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiIngredient;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import tfar.ae2wt.net.PacketHandler;
import tfar.ae2wt.net.server.C2SLoadPatternPacket;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.util.FluidCraftHelper;
import tfar.ae2wt.wpt.WirelessPatternTerminalContainer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WirelessPatternTerminalRecipeTransferHandler implements IRecipeTransferHandler<WirelessPatternTerminalContainer> {

    @Nonnull
    @Override
    public Class<WirelessPatternTerminalContainer> getContainerClass() {
        return WirelessPatternTerminalContainer.class;
    }


    @Override
    public IRecipeTransferError transferRecipe(@Nonnull WirelessPatternTerminalContainer container,
                                               @Nonnull Object recipe,
                                               @Nonnull IRecipeLayout recipeLayout,
                                               @Nonnull PlayerEntity player,
                                               boolean maxTransfer,
                                               boolean doTransfer) {

        boolean recipeIsCrafting = recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.CRAFTING);
        boolean currentFluidMode = container.isFluidMode();

        ItemStack terminal = container.getPatternTerminal().getItemStack();
        boolean fluidConversionEnabled = AbstractWirelessTerminalItem.getBoolean(terminal, "fluidConversion");

        List<ItemStack> inputList = new ArrayList<>();
        List<ItemStack> outputList = new ArrayList<>();

        boolean hasFluid = false;

        for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : recipeLayout.getItemStacks().getGuiIngredients().entrySet()) {
            int slot = entry.getKey();
            IGuiIngredient<ItemStack> ingredient = entry.getValue();
            ItemStack stack = getFirstValidStack(ingredient);
            if (stack.isEmpty()) continue;

            ItemStack converted = stack;
            if (FluidCraftHelper.PRESENT) {
                FluidStack fluid = FluidCraftHelper.getFluidFromItem(stack);
                if (!fluid.isEmpty()) {
                    if (fluidConversionEnabled) {
                        converted = FluidCraftHelper.newFluidPacket(fluid);
                        hasFluid = true;   // marks this as a fluid recipe
                    } else {
                        // Keep the bucket/tank as-is
                        converted = stack;
                    }
                }
            }

            if (ingredient.isInput()) {
                if (recipeIsCrafting) {
                } else {

                    inputList.add(converted);
                }
            } else {
                outputList.add(converted);
            }
        }

        try {
            if (recipeLayout.getFluidStacks() != null) {
                for (Map.Entry<Integer, ? extends IGuiIngredient<FluidStack>> entry :
                        recipeLayout.getFluidStacks().getGuiIngredients().entrySet()) {
                    int slot = entry.getKey();
                    IGuiIngredient<FluidStack> ingredient = entry.getValue();
                    FluidStack fluid = getFirstValidFluid(ingredient);
                    if (fluid.isEmpty()) continue;

                    ItemStack packet = FluidCraftHelper.newFluidPacket(fluid);
                    if (packet.isEmpty()) continue;

                    if (ingredient.isInput()) {
                        if (recipeIsCrafting) {
                        } else {
                            inputList.add(packet);
                        }
                    } else {
                        outputList.add(packet);
                    }
                }
            }
        } catch (Exception ignored) {}

        if (recipeIsCrafting) {
            return handleCraftingTransfer(container, recipeLayout, inputList, outputList, doTransfer);
        }

        ItemStack[] inputs = new ItemStack[9];
        ItemStack[] outputs = new ItemStack[3];
        for (int i = 0; i < 9; i++) inputs[i] = ItemStack.EMPTY;
        for (int i = 0; i < 3; i++) outputs[i] = ItemStack.EMPTY;

        int inputIdx = 0;
        for (ItemStack stack : inputList) {
            if (inputIdx >= 9) break;
            if (!stack.isEmpty()) {
                inputs[inputIdx] = stack;
                inputIdx++;
            }
        }

        int outputIdx = 0;
        for (ItemStack stack : outputList) {
            if (outputIdx >= 3) break;
            if (!stack.isEmpty()) {
                outputs[outputIdx] = stack;
                outputIdx++;
            }
        }


        for (ItemStack stack : inputs) {
            if (FluidCraftHelper.isFluidPacket(stack)) { hasFluid = true; break; }
        }
        if (!hasFluid) {
            for (ItemStack stack : outputs) {
                if (FluidCraftHelper.isFluidPacket(stack)) { hasFluid = true; break; }
            }
        }

        boolean targetCraftingMode = recipeIsCrafting;
        boolean targetFluidMode = hasFluid && FluidCraftHelper.PRESENT;

        if (targetFluidMode && targetCraftingMode) {
            return createError("Fluid recipes must be processed as processing patterns");
        }

        if (doTransfer) {
            PacketHandler.INSTANCE.sendToServer(new C2SLoadPatternPacket(inputs, outputs, targetCraftingMode, targetFluidMode));
        }

        return null;
    }

    private IRecipeTransferError handleCraftingTransfer(WirelessPatternTerminalContainer container,
                                                        IRecipeLayout recipeLayout,
                                                        List<ItemStack> inputList,
                                                        List<ItemStack> outputList,
                                                        boolean doTransfer) {

        ItemStack[] inputs = new ItemStack[9];
        ItemStack[] outputs = new ItemStack[3];
        for (int i = 0; i < 9; i++) inputs[i] = ItemStack.EMPTY;
        for (int i = 0; i < 3; i++) outputs[i] = ItemStack.EMPTY;

        boolean hasFluid = false;


        ItemStack terminal = container.getPatternTerminal().getItemStack();
        boolean fluidConversionEnabled = AbstractWirelessTerminalItem.getBoolean(terminal, "fluidConversion");

        for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : recipeLayout.getItemStacks().getGuiIngredients().entrySet()) {
            int slot = entry.getKey();
            IGuiIngredient<ItemStack> ingredient = entry.getValue();
            ItemStack stack = getFirstValidStack(ingredient);
            if (stack.isEmpty()) continue;

            ItemStack converted = stack;
            if (FluidCraftHelper.PRESENT) {
                FluidStack fluid = FluidCraftHelper.getFluidFromItem(stack);
                if (!fluid.isEmpty()) {
                    if (fluidConversionEnabled) {
                        converted = FluidCraftHelper.newFluidPacket(fluid);
                        hasFluid = true;   // marks this as a fluid recipe
                    } else {
                        // Keep the bucket/tank as-is
                        converted = stack;

                    }
                }
            }

            if (ingredient.isInput()) {
                if (slot >= 1 && slot <= 9) {
                    inputs[slot - 1] = converted;
                }
            } else {
                if (slot == 0) {
                    outputs[0] = converted;
                } else {

                    for (int i = 0; i < 3; i++) {
                        if (outputs[i].isEmpty()) {
                            outputs[i] = converted;
                            break;
                        }
                    }
                }
            }
        }

        try {
            if (recipeLayout.getFluidStacks() != null) {
                for (Map.Entry<Integer, ? extends IGuiIngredient<FluidStack>> entry :
                        recipeLayout.getFluidStacks().getGuiIngredients().entrySet()) {
                    int slot = entry.getKey();
                    IGuiIngredient<FluidStack> ingredient = entry.getValue();
                    FluidStack fluid = getFirstValidFluid(ingredient);
                    if (fluid.isEmpty()) continue;

                    ItemStack packet = FluidCraftHelper.newFluidPacket(fluid);
                    if (packet.isEmpty()) continue;

                    if (ingredient.isInput()) {
                        if (slot >= 1 && slot <= 9) {

                            inputs[slot - 1] = packet;
                        }
                    } else {

                        for (int i = 0; i < 3; i++) {
                            if (outputs[i].isEmpty()) {
                                outputs[i] = packet;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        for (ItemStack stack : inputs) {
            if (FluidCraftHelper.isFluidPacket(stack)) { hasFluid = true; break; }
        }
        if (!hasFluid) {
            for (ItemStack stack : outputs) {
                if (FluidCraftHelper.isFluidPacket(stack)) { hasFluid = true; break; }
            }
        }

        boolean targetFluidMode = hasFluid && FluidCraftHelper.PRESENT;
        boolean targetCraftingMode = true;

        if (targetFluidMode) {
            return createError("Fluid recipes cannot be crafting patterns");
        }

        if (doTransfer) {
            PacketHandler.INSTANCE.sendToServer(new C2SLoadPatternPacket(inputs, outputs, targetCraftingMode, targetFluidMode));
        }

        return null;
    }

    private ItemStack getFirstValidStack(IGuiIngredient<ItemStack> ingredient) {
        for (ItemStack stack : ingredient.getAllIngredients()) {
            if (stack != null && !stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private FluidStack getFirstValidFluid(IGuiIngredient<FluidStack> ingredient) {
        for (FluidStack fluid : ingredient.getAllIngredients()) {
            if (fluid != null && !fluid.isEmpty()) {
                return fluid;
            }
        }
        return FluidStack.EMPTY;
    }

    private IRecipeTransferError createError(String message) {
        return new IRecipeTransferError() {
            @Override
            public Type getType() {
                return Type.USER_FACING;
            }

            @Override
            public void showError(MatrixStack matrixStack, int i, int i1, IRecipeLayout iRecipeLayout, int i2, int i3) {

            }

        };
    }
}