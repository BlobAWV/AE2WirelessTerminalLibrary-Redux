package tfar.ae2wt.util;

import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IItemList;
import appeng.core.Api;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.ModList;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ChemicalHelper {
    public static final boolean AE_ADDITIONS_PRESENT = ModList.get().isLoaded("aeadditions");
    public static final boolean MEKANISM_PRESENT = ModList.get().isLoaded("mekanism");
    public static final boolean CHEMICALS_PRESENT = AE_ADDITIONS_PRESENT && MEKANISM_PRESENT;


    private static Class<?> iaeChemicalStackClass;    // IAEChemicalStack
    private static Class<?> chemicalDummyItemClass;
    private static Class<?> chemicalStackClass;       // mekanism.api.chemical.ChemicalStack
    private static Class<?> chemicalClass;            // mekanism.api.chemical.Chemical
    private static Class<?> mekanismClass;




    public static Method getChemicalStackMethod;      // IAEChemicalStack.getChemicalStack()
    public static Method getChemicalTypeMethod;       // ChemicalStack.getType()
    private static Method getChemicalAmountMethod;     // ChemicalStack.getAmount()
    private static Method getChemicalIconMethod;       // Chemical.getIcon()
    private static Method getChemicalTintMethod;       // Chemical.getTint()
    private static Method getChemicalTextComponentMethod; // Chemical.getTextComponent()
    private static Method getChemicalRegistryNameMethod; // Chemical.getRegistryName()
    private static Method fillContainerMethod;
    private static Method extractContainerMethod;
    private static Method capabilityFromItemMethod;


    private static IStorageChannel<?> chemicalStorageChannel;

    static {
        if (CHEMICALS_PRESENT) {
            try {
                // AE Additions
                iaeChemicalStackClass = Class.forName("com.the9grounds.aeadditions.api.chemical.IAEChemicalStack");
                chemicalDummyItemClass = Class.forName("com.the9grounds.aeadditions.item.ChemicalDummyItem");

                // Mekanism
                chemicalStackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
                chemicalClass = Class.forName("mekanism.api.chemical.Chemical");


                getChemicalStackMethod = iaeChemicalStackClass.getMethod("getChemicalStack");


                getChemicalTypeMethod = chemicalStackClass.getMethod("getType");
                getChemicalAmountMethod = chemicalStackClass.getMethod("getAmount");


                getChemicalIconMethod = chemicalClass.getMethod("getIcon");
                getChemicalTintMethod = chemicalClass.getMethod("getTint");
                getChemicalTextComponentMethod = chemicalClass.getMethod("getTextComponent");
                getChemicalRegistryNameMethod = chemicalClass.getMethod("getRegistryName");


                Class<?> mekanismClass = Class.forName("com.the9grounds.aeadditions.integration.mekanism.Mekanism");

            } catch (Exception e) {
                e.printStackTrace();
                //ignore
            }
        }
    }

    @Nullable
    public static synchronized IStorageChannel<?> getChemicalStorageChannel() {
        if (!CHEMICALS_PRESENT) return null;
        if (chemicalStorageChannel != null) return chemicalStorageChannel;

        try {
            Class<?> storageChannelsClass = Class.forName("com.the9grounds.aeadditions.util.StorageChannels");
            Method getChemicalMethod = storageChannelsClass.getMethod("getCHEMICAL");
            Object channel = getChemicalMethod.invoke(null);
            if (channel instanceof IStorageChannel<?>) {
                chemicalStorageChannel = (IStorageChannel<?>) channel;
                return chemicalStorageChannel;
            }
        } catch (Exception e) {
            // ignore
        }

        try {
            Class<?> chemicalStorageChannelClass = Class.forName("com.the9grounds.aeadditions.api.chemical.IChemicalStorageChannel");
            @SuppressWarnings("unchecked")
            IStorageChannel<?> channel = Api.instance().storage().getStorageChannel((Class<IStorageChannel>) chemicalStorageChannelClass);
            if (channel != null) {
                chemicalStorageChannel = channel;
                return chemicalStorageChannel;
            }
        } catch (Exception e) {
            // ignore
        }

        return null;
    }


    public static boolean isChemicalDummyItem(ItemStack stack) {
        if (!CHEMICALS_PRESENT || stack.isEmpty()) return false;
        return chemicalDummyItemClass.isInstance(stack.getItem());
    }

    public static Object createAEChemicalStack(Object chemicalStack) {
        if (!CHEMICALS_PRESENT || chemicalStack == null) return null;
        try {
            Class<?> aeChemicalStackClass = Class.forName("com.the9grounds.aeadditions.integration.mekanism.chemical.AEChemicalStack");
            Constructor<?> constructor = aeChemicalStackClass.getConstructor(chemicalStack.getClass());
            return constructor.newInstance(chemicalStack);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static Object getChemicalStackFromDummy(ItemStack stack) {
        if (!CHEMICALS_PRESENT || !isChemicalDummyItem(stack)) return null;
        try {

            Method method = chemicalDummyItemClass.getMethod("getChemicalStack", ItemStack.class);
            return method.invoke(null, stack);
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack createDummyItemFromChemical(Object chemicalStack) {
        if (!CHEMICALS_PRESENT || chemicalStack == null) return ItemStack.EMPTY;
        try {
            ItemStack dummy = new ItemStack((net.minecraft.item.Item) chemicalDummyItemClass.newInstance());
            Method method = chemicalDummyItemClass.getMethod("setChemicalStack", ItemStack.class, chemicalStackClass);
            method.invoke(null, dummy, chemicalStack);
            return dummy;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean isChemicalContainer(ItemStack stack) {
        if (!CHEMICALS_PRESENT || stack.isEmpty()) return false;
        try {
            Class<?> capClass = Class.forName("mekanism.api.chemical.ChemicalCapabilities");
            java.lang.reflect.Field capField = capClass.getField("CHEMICAL_HANDLER_CAPABILITY");
            Object capability = capField.get(null);
            Method getCapMethod = stack.getClass().getMethod("getCapability", net.minecraftforge.common.capabilities.Capability.class, net.minecraft.util.Direction.class);
            Object lazyOptional = getCapMethod.invoke(stack, capability, null);
            Method isPresentMethod = lazyOptional.getClass().getMethod("isPresent");
            return (boolean) isPresentMethod.invoke(lazyOptional);
        } catch (Exception e) {
            return false;
        }
    }

    public static long getChemicalAmountFromContainer(ItemStack stack) {
        if (!CHEMICALS_PRESENT || !isChemicalContainer(stack)) return 0;
        try {
            Class<?> capClass = Class.forName("mekanism.api.chemical.ChemicalCapabilities");
            java.lang.reflect.Field capField = capClass.getField("CHEMICAL_HANDLER_CAPABILITY");
            Object capability = capField.get(null);
            Method getCapMethod = stack.getClass().getMethod("getCapability", net.minecraftforge.common.capabilities.Capability.class, net.minecraft.util.Direction.class);
            Object lazyOptional = getCapMethod.invoke(stack, capability, null);
            Method resolveMethod = lazyOptional.getClass().getMethod("resolve");
            Object chemicalHandler = resolveMethod.invoke(lazyOptional);
            if (chemicalHandler == null) return 0;
            Method getChemicalInTankMethod = chemicalHandler.getClass().getMethod("getChemicalInTank", int.class);
            Object chemStack = getChemicalInTankMethod.invoke(chemicalHandler, 0);
            if (chemStack == null) return 0;
            return (long) getChemicalAmountMethod.invoke(chemStack);
        } catch (Exception e) {
            return 0;
        }
    }


    public static void renderChemicalIntoSlot(MatrixStack matrixStack, int x, int y, int width, int height, Object iaeStack, long amount) {
        if (!CHEMICALS_PRESENT || iaeStack == null) return;
        try {

            Object chemicalStack = getChemicalStackMethod.invoke(iaeStack);
            if (chemicalStack == null) return;


            Object chemical = getChemicalTypeMethod.invoke(chemicalStack);
            if (chemical == null) return;


            int tint = (int) getChemicalTintMethod.invoke(chemical);
            ResourceLocation icon = (ResourceLocation) getChemicalIconMethod.invoke(chemical);
            if (icon == null) {
                renderFallback(matrixStack, x, y, width, height, tint);
                return;
            }


            Minecraft.getInstance().getTextureManager().bindTexture(PlayerContainer.LOCATION_BLOCKS_TEXTURE);
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE).apply(icon);
            if (sprite != null) {
                renderSprite(matrixStack, x, y, width, height, sprite, tint);
            } else {
                renderFallback(matrixStack, x, y, width, height, tint);
            }
        } catch (Exception e) {

            renderErrorFallback(matrixStack, x, y, width, height);
        }
    }

    private static void renderErrorFallback(MatrixStack matrixStack, int x, int y, int width, int height) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, x, y + height, 0).color(1f, 0f, 0f, 1f).endVertex();
        buffer.pos(matrix, x + width, y + height, 0).color(1f, 0f, 0f, 1f).endVertex();
        buffer.pos(matrix, x + width, y, 0).color(1f, 0f, 0f, 1f).endVertex();
        buffer.pos(matrix, x, y, 0).color(1f, 0f, 0f, 1f).endVertex();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        tessellator.draw();
        RenderSystem.disableBlend();
    }

    private static void renderSprite(MatrixStack matrixStack, int x, int y, int width, int height, TextureAtlasSprite sprite, int tint) {
        int r = (tint >> 16) & 0xFF;
        int g = (tint >> 8) & 0xFF;
        int b = tint & 0xFF;
        if (r == 0 && g == 0 && b == 0) {
            r = 255; g = 255; b = 255;
        }
        float red = r / 255f;
        float green = g / 255f;
        float blue = b / 255f;

        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f matrix = matrixStack.getLast().getMatrix();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
        buffer.pos(matrix, x, y + height, 0).color(red, green, blue, 1.0f).tex(minU, maxV).endVertex();
        buffer.pos(matrix, x + width, y + height, 0).color(red, green, blue, 1.0f).tex(maxU, maxV).endVertex();
        buffer.pos(matrix, x + width, y, 0).color(red, green, blue, 1.0f).tex(maxU, minV).endVertex();
        buffer.pos(matrix, x, y, 0).color(red, green, blue, 1.0f).tex(minU, minV).endVertex();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        tessellator.draw();
        RenderSystem.disableBlend();
    }

    private static void renderFallback(MatrixStack matrixStack, int x, int y, int width, int height, int tint) {
        int r = (tint >> 16) & 0xFF;
        int g = (tint >> 8) & 0xFF;
        int b = tint & 0xFF;
        if (r == 0 && g == 0 && b == 0) {
            r = 255; g = 255; b = 255;
        }
        float red = r / 255f;
        float green = g / 255f;
        float blue = b / 255f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f matrix = matrixStack.getLast().getMatrix();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, x, y + height, 0).color(red, green, blue, 1.0f).endVertex();
        buffer.pos(matrix, x + width, y + height, 0).color(red, green, blue, 1.0f).endVertex();
        buffer.pos(matrix, x + width, y, 0).color(red, green, blue, 1.0f).endVertex();
        buffer.pos(matrix, x, y, 0).color(red, green, blue, 1.0f).endVertex();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        tessellator.draw();
        RenderSystem.disableBlend();
    }


    public static String getChemicalDisplayName(Object iaeStack) {
        if (!CHEMICALS_PRESENT || iaeStack == null) return "";
        try {
            Object chemicalStack = getChemicalStackMethod.invoke(iaeStack);
            if (chemicalStack == null) return "";
            Object chemical = getChemicalTypeMethod.invoke(chemicalStack);
            if (chemical == null) return "";
            Object text = getChemicalTextComponentMethod.invoke(chemical);
            if (text instanceof ITextComponent) {
                return ((ITextComponent) text).getString();
            }
            return text.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static long getChemicalStackSize(Object iaeStack) {
        if (!CHEMICALS_PRESENT || iaeStack == null) return 0;
        try {

            Method getStackSizeMethod = iaeStack.getClass().getMethod("getStackSize");
            return (long) getStackSizeMethod.invoke(iaeStack);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getChemicalModId(Object iaeStack) {
        if (!CHEMICALS_PRESENT || iaeStack == null) return "";
        try {
            Object chemicalStack = getChemicalStackMethod.invoke(iaeStack);
            if (chemicalStack == null) return "";
            Object chemical = getChemicalTypeMethod.invoke(chemicalStack);
            if (chemical == null) return "";
            ResourceLocation rl = (ResourceLocation) getChemicalRegistryNameMethod.invoke(chemical);
            return rl.getNamespace();
        } catch (Exception e) {
            return "";
        }
    }

    public static Object getChemicalHandler(ItemStack stack, Object chemicalStack) {
        if (!CHEMICALS_PRESENT || stack.isEmpty() || chemicalStack == null) return null;
        try {
            Method getTypeMethod = chemicalStack.getClass().getMethod("getType");
            Object chemical = getTypeMethod.invoke(chemicalStack);
            Class<?> chemicalClass = chemical.getClass();

            String capName;
            if (Class.forName("mekanism.api.chemical.gas.Gas").isAssignableFrom(chemicalClass)) {
                capName = "GAS_HANDLER_CAPABILITY";
            } else if (Class.forName("mekanism.api.chemical.slurry.Slurry").isAssignableFrom(chemicalClass)) {
                capName = "SLURRY_HANDLER_CAPABILITY";
            } else if (Class.forName("mekanism.api.chemical.infuse.InfuseType").isAssignableFrom(chemicalClass)) {
                capName = "INFUSION_HANDLER_CAPABILITY";
            } else if (Class.forName("mekanism.api.chemical.pigment.Pigment").isAssignableFrom(chemicalClass)) {
                capName = "PIGMENT_HANDLER_CAPABILITY";
            } else {
                return null;
            }

            Class<?> capClass = Class.forName("mekanism.common.capabilities.Capabilities");
            java.lang.reflect.Field capField = capClass.getField(capName);
            Object capability = capField.get(null);
            Method getCapMethod = stack.getClass().getMethod("getCapability", net.minecraftforge.common.capabilities.Capability.class, net.minecraft.util.Direction.class);
            Object lazyOptional = getCapMethod.invoke(stack, capability, null);
            Method isPresentMethod = lazyOptional.getClass().getMethod("isPresent");
            if ((boolean) isPresentMethod.invoke(lazyOptional)) {
                Method resolveMethod = lazyOptional.getClass().getMethod("resolve");
                Object optional = resolveMethod.invoke(lazyOptional);
                Method isPresentOptional = optional.getClass().getMethod("isPresent");
                if ((boolean) isPresentOptional.invoke(optional)) {
                    Method getMethod = optional.getClass().getMethod("get");
                    return getMethod.invoke(optional);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object extractChemicalFromContainer(ItemStack stack) {
        if (!CHEMICALS_PRESENT || stack.isEmpty()) return null;
        String[] capNames = {"GAS_HANDLER_CAPABILITY", "SLURRY_HANDLER_CAPABILITY", "INFUSION_HANDLER_CAPABILITY", "PIGMENT_HANDLER_CAPABILITY"};
        for (String capName : capNames) {
            try {
                Class<?> capClass = Class.forName("mekanism.common.capabilities.Capabilities");
                java.lang.reflect.Field capField = capClass.getField(capName);
                Object capability = capField.get(null);
                Method getCapMethod = stack.getClass().getMethod("getCapability", net.minecraftforge.common.capabilities.Capability.class, net.minecraft.util.Direction.class);
                Object lazyOptional = getCapMethod.invoke(stack, capability, null);
                Method isPresentMethod = lazyOptional.getClass().getMethod("isPresent");
                if ((boolean) isPresentMethod.invoke(lazyOptional)) {
                    Method resolveMethod = lazyOptional.getClass().getMethod("resolve");
                    Object optional = resolveMethod.invoke(lazyOptional);
                    Method isPresentOptional = optional.getClass().getMethod("isPresent");
                    if ((boolean) isPresentOptional.invoke(optional)) {
                        Method getMethod = optional.getClass().getMethod("get");
                        Object handler = getMethod.invoke(optional);
                        if (handler != null) {
                            Object extracted = extractChemicalFromHandler(handler, Integer.MAX_VALUE);
                            if (extracted != null && getChemicalAmount(extracted) > 0) {
                                return extracted;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    public static Object extractChemicalFromContainer(ItemStack stack, long maxAmount) {
        if (!CHEMICALS_PRESENT || stack.isEmpty() || maxAmount <= 0) return null;
        String[] capNames = {"GAS_HANDLER_CAPABILITY", "SLURRY_HANDLER_CAPABILITY", "INFUSION_HANDLER_CAPABILITY", "PIGMENT_HANDLER_CAPABILITY"};
        for (String capName : capNames) {
            try {
                Class<?> capClass = Class.forName("mekanism.common.capabilities.Capabilities");
                java.lang.reflect.Field capField = capClass.getField(capName);
                Object capability = capField.get(null);
                Method getCapMethod = stack.getClass().getMethod("getCapability", net.minecraftforge.common.capabilities.Capability.class, net.minecraft.util.Direction.class);
                Object lazyOptional = getCapMethod.invoke(stack, capability, null);
                Method isPresentMethod = lazyOptional.getClass().getMethod("isPresent");
                if ((boolean) isPresentMethod.invoke(lazyOptional)) {
                    Method resolveMethod = lazyOptional.getClass().getMethod("resolve");
                    Object optional = resolveMethod.invoke(lazyOptional);
                    Method isPresentOptional = optional.getClass().getMethod("isPresent");
                    if ((boolean) isPresentOptional.invoke(optional)) {
                        Method getMethod = optional.getClass().getMethod("get");
                        Object handler = getMethod.invoke(optional);
                        if (handler != null) {
                            Object extracted = extractChemicalFromHandler(handler, maxAmount);
                            if (extracted != null && getChemicalAmount(extracted) > 0) {
                                return extracted;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }
    public static Object insertChemicalIntoHandler(Object handler, Object chemicalStack) {
        if (!CHEMICALS_PRESENT || handler == null || chemicalStack == null) return chemicalStack;
        try {
            Class<?> actionClass = Class.forName("mekanism.api.Action");
            Object executeAction = null;
            for (Object o : actionClass.getEnumConstants()) {
                if (o.toString().equals("EXECUTE")) {
                    executeAction = o;
                    break;
                }
            }
            if (executeAction == null) return chemicalStack;
            // Determine the specific ChemicalStack class from the chemicalStack object
            Class<?> specificStackClass = chemicalStack.getClass(); // e.g., SlurryStack, GasStack
            Method insertMethod = handler.getClass().getMethod("insertChemical", specificStackClass, actionClass);
            System.out.println("[ChemicalHelper] insertChemicalIntoHandler: invoking insertChemical with " + chemicalStack);
            Object result = insertMethod.invoke(handler, chemicalStack, executeAction);
            System.out.println("[ChemicalHelper] insertChemicalIntoHandler: result = " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return chemicalStack;
        }
    }

    public static Object extractChemicalFromHandler(Object handler, long maxAmount) {
        if (!CHEMICALS_PRESENT || handler == null || maxAmount <= 0) return null;
        try {
            Class<?> actionClass = Class.forName("mekanism.api.Action");
            Object executeAction = null;
            for (Object o : actionClass.getEnumConstants()) {
                if (o.toString().equals("EXECUTE")) {
                    executeAction = o;
                    break;
                }
            }
            if (executeAction == null) return null;
            Method extractMethod = handler.getClass().getMethod("extractChemical", long.class, actionClass);
            Object result = extractMethod.invoke(handler, maxAmount, executeAction);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static long getChemicalAmount(Object chemicalStack) {
        if (!CHEMICALS_PRESENT || chemicalStack == null) return 0;
        try {
            return (long) getChemicalAmountMethod.invoke(chemicalStack);
        } catch (Exception e) {
            return 0;
        }
    }

    public static Object createChemicalStack(Object chemicalOrStack, long amount) {
        if (!CHEMICALS_PRESENT || chemicalOrStack == null) return null;
        try {
            Object chemical;
            Class<?> stackClass;

            if (chemicalOrStack.getClass().getSimpleName().contains("Stack")) {
                Method getTypeMethod = chemicalOrStack.getClass().getMethod("getType");
                chemical = getTypeMethod.invoke(chemicalOrStack);
                stackClass = chemicalOrStack.getClass();
            } else {
                chemical = chemicalOrStack;
                String className = chemical.getClass().getName();
                String stackClassName;
                if (className.contains("gas")) {
                    stackClassName = "mekanism.api.chemical.gas.GasStack";
                } else if (className.contains("slurry")) {
                    stackClassName = "mekanism.api.chemical.slurry.SlurryStack";
                } else if (className.contains("infuse")) {
                    stackClassName = "mekanism.api.chemical.infuse.InfusionStack";
                } else if (className.contains("pigment")) {
                    stackClassName = "mekanism.api.chemical.pigment.PigmentStack";
                } else {
                    return null;
                }
                stackClass = Class.forName(stackClassName);
            }

            try {
                Constructor<?> constructor = stackClass.getConstructor(chemical.getClass(), long.class);
                return constructor.newInstance(chemical, amount);
            } catch (NoSuchMethodException e1) {

                if (chemicalOrStack.getClass().getSimpleName().contains("Stack")) {
                    try {
                        Constructor<?> constructor = stackClass.getConstructor(chemicalOrStack.getClass(), long.class);
                        return constructor.newInstance(chemicalOrStack, amount);
                    } catch (NoSuchMethodException e2) {

                        Method copyMethod = stackClass.getMethod("copy");
                        Object copy = copyMethod.invoke(chemicalOrStack);

                        try {
                            Method setAmountMethod = stackClass.getMethod("setAmount", long.class);
                            setAmountMethod.invoke(copy, amount);
                            return copy;
                        } catch (NoSuchMethodException e3) {

                            java.lang.reflect.Field amountField = stackClass.getDeclaredField("amount");
                            amountField.setAccessible(true);
                            amountField.set(copy, amount);
                            return copy;
                        }
                    }
                } else {

                    try {
                        Constructor<?> constructor = stackClass.getConstructor(Class.forName("mekanism.api.chemical.Chemical"), long.class);
                        return constructor.newInstance(chemical, amount);
                    } catch (Exception e4) {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[ChemicalHelper] createChemicalStack: exception: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static Object extractChemicalFromHandler(Object handler, long maxAmount, Object action) {
        if (!CHEMICALS_PRESENT || handler == null || action == null) return null;
        try {
            Method extractMethod = null;
            Class<?> actionClass = action.getClass();
            for (Method m : handler.getClass().getMethods()) {
                if (m.getName().equals("extractChemical")) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 2 && params[0] == long.class && params[1].isAssignableFrom(actionClass)) {
                        extractMethod = m;
                        break;
                    }
                }
            }
            if (extractMethod == null) {

                for (Method m : handler.getClass().getMethods()) {
                    if (m.getName().equals("extractChemical")) {
                        extractMethod = m;
                        break;
                    }
                }
            }
            if (extractMethod == null) return null;
            extractMethod.setAccessible(true);
            return extractMethod.invoke(handler, maxAmount, action);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object insertChemicalIntoHandler(Object handler, Object chemicalStack, Object action) {
        if (!CHEMICALS_PRESENT || handler == null || chemicalStack == null || action == null) return chemicalStack;
        try {

            Method insertMethod = null;
            Class<?> actionClass = action.getClass();
            for (Method m : handler.getClass().getMethods()) {
                if (m.getName().equals("insertChemical")) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 2) {

                        if (params[0].isAssignableFrom(chemicalStack.getClass()) && params[1].isAssignableFrom(actionClass)) {
                            insertMethod = m;
                            break;
                        }
                    }
                }
            }
            if (insertMethod == null) {

                Class<?> chemicalStackSuper = Class.forName("mekanism.api.chemical.ChemicalStack");
                for (Method m : handler.getClass().getMethods()) {
                    if (m.getName().equals("insertChemical")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 2 && params[0].isAssignableFrom(chemicalStackSuper) && params[1].isAssignableFrom(actionClass)) {
                            insertMethod = m;
                            break;
                        }
                    }
                }
            }
            if (insertMethod == null) {
                return chemicalStack;
            }
            insertMethod.setAccessible(true);
            return insertMethod.invoke(handler, chemicalStack, action);
        } catch (Exception e) {
            e.printStackTrace();
            return chemicalStack;
        }
    }

    public static Object getAction(String name) {
        try {
            Class<?> actionClass = Class.forName("mekanism.api.Action");
            for (Object o : actionClass.getEnumConstants()) {
                if (o.toString().equals(name)) {
                    return o;
                }
            }
        } catch (Exception e) {}
        return null;
    }

    public static long getNetworkAmount(IMEMonitor monitor, Object chemical, IStorageChannel<?> channel) {
        if (!CHEMICALS_PRESENT || monitor == null || chemical == null || channel == null) return 0;
        try {

            Object storageList = monitor.getStorageList();
            if (storageList == null) return 0;

            Method createStackMethod = channel.getClass().getMethod("createStack", Object.class);
            Object dummyStack = createStackMethod.invoke(channel, chemical);
            if (dummyStack == null) return 0;

            Method iteratorMethod = storageList.getClass().getMethod("iterator");
            java.util.Iterator iterator = (java.util.Iterator) iteratorMethod.invoke(storageList);
            while (iterator.hasNext()) {
                Object stack = iterator.next();

                Method equalsMethod = stack.getClass().getMethod("equals", Object.class);
                if ((boolean) equalsMethod.invoke(stack, dummyStack)) {
                    Method getStackSize = stack.getClass().getMethod("getStackSize");
                    return (long) getStackSize.invoke(stack);
                }
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
