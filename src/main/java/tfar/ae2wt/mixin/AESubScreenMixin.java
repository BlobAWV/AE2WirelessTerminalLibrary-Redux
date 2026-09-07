package tfar.ae2wt.mixin;

import appeng.client.gui.implementations.AESubScreen;
import appeng.helpers.WirelessTerminalGuiObject;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.ae2wt.init.Menus;
import tfar.ae2wt.terminal.WTGuiObject;
import tfar.ae2wt.wirelesscraftingterminal.WCTGuiObject;
import tfar.ae2wt.wirelessfluidterminal.WFluidTGuiObject;
import tfar.ae2wt.wirelessinterfaceterminal.WITGuiObject;
import tfar.ae2wt.wpt.WPTGuiObject;
import tfar.ae2wt.wirelesschemicalterminal.WChemGuiObject;
import tfar.ae2wt.wut.WUTItem;
import tfar.ae2wt.wut.WUTHandler;

import java.lang.reflect.Field;

@Mixin(value = AESubScreen.class, remap = false)
public class AESubScreenMixin {

    @Mutable
    @Shadow
    @Final
    private ContainerType<?> previousContainerType;

    @Mutable
    @Shadow
    @Final
    private ItemStack previousContainerIcon;

    private static Field effectiveItemField;

    static {
        try {
            effectiveItemField = WirelessTerminalGuiObject.class.getDeclaredField("effectiveItem");
            effectiveItemField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            //ignore
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void detectWirelessTerminals(Object containerHost, CallbackInfo ci) {


        if (containerHost instanceof WTGuiObject) {
            WTGuiObject gui = (WTGuiObject) containerHost;
            ItemStack stack = gui.getItemStack();

            setContainerTypeFromItem(stack);
            return;
        }


        if (containerHost instanceof WirelessTerminalGuiObject && effectiveItemField != null) {
            try {
                ItemStack stack = (ItemStack) effectiveItemField.get(containerHost);

                setContainerTypeFromItem(stack);
                return;
            } catch (IllegalAccessException e) {
                //ignore
            }
        }

    }

    private void setContainerTypeFromItem(ItemStack stack) {
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof WUTItem) {
            // See what terminal WUT is currently on
            String currentTerminal = WUTHandler.getCurrentTerminal(stack);

            switch (currentTerminal) {
                case "crafting":
                    previousContainerType = Menus.WCT;
                    previousContainerIcon = stack;
                    break;
                case "pattern":
                    previousContainerType = Menus.PATTERN;
                    previousContainerIcon = stack;
                    break;
                case "interface":
                    previousContainerType = Menus.WIT;
                    previousContainerIcon = stack;
                    break;
                case "fluid":
                    previousContainerType = Menus.WIRELESS_FLUID_TERMINAL;
                    previousContainerIcon = stack;
                    break;
                case "chemical":
                    if (tfar.ae2wt.util.ChemicalHelper.CHEMICALS_PRESENT) {
                        previousContainerType = Menus.WIRELESS_CHEMICAL_TERMINAL;
                        previousContainerIcon = stack;
                    }
                    break;
                default:
                    // Fallback to crafting terminal
                    previousContainerType = Menus.WCT;
                    previousContainerIcon = stack;
                    break;
            }

            return;
        }

        // Non-WUT terminal: detect by item class
        if (stack.getItem() instanceof tfar.ae2wt.wirelesscraftingterminal.WCTItem) {
            previousContainerType = Menus.WCT;
            previousContainerIcon = stack;
        } else if (stack.getItem() instanceof tfar.ae2wt.wpt.WPTItem) {
            previousContainerType = Menus.PATTERN;
            previousContainerIcon = stack;
        } else if (stack.getItem() instanceof tfar.ae2wt.wirelessinterfaceterminal.WITItem) {
            previousContainerType = Menus.WIT;
            previousContainerIcon = stack;
        } else if (stack.getItem() instanceof tfar.ae2wt.wirelessfluidterminal.WirelessFluidTerminalItem) {
            previousContainerType = Menus.WIRELESS_FLUID_TERMINAL;
            previousContainerIcon = stack;
        } else if (stack.getItem() instanceof tfar.ae2wt.wirelesschemicalterminal.WirelessChemicalTerminalItem && tfar.ae2wt.util.ChemicalHelper.CHEMICALS_PRESENT) {
            previousContainerType = Menus.WIRELESS_CHEMICAL_TERMINAL;
            previousContainerIcon = stack;
        } else {
            // Fallback to crafting terminal
            previousContainerType = tfar.ae2wt.init.Menus.WCT;
            previousContainerIcon = stack;
        }
    }
}