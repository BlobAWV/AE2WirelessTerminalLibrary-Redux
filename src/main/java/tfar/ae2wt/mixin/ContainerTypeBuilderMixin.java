package tfar.ae2wt.mixin;

import appeng.container.ContainerLocator;
import appeng.container.implementations.ContainerTypeBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.util.ChemicalHelper;
import tfar.ae2wt.util.ContainerHelper;
import tfar.ae2wt.wirelesschemicalterminal.WChemGuiObject;
import tfar.ae2wt.wirelesschemicalterminal.WirelessChemicalTerminalItem;
import tfar.ae2wt.wirelesscraftingterminal.WCTGuiObject;
import tfar.ae2wt.wirelesscraftingterminal.WCTItem;
import tfar.ae2wt.wirelessfluidterminal.WFluidTGuiObject;
import tfar.ae2wt.wirelessfluidterminal.WirelessFluidTerminalItem;
import tfar.ae2wt.wirelessinterfaceterminal.WITGuiObject;
import tfar.ae2wt.wirelessinterfaceterminal.WITItem;
import tfar.ae2wt.wpt.WPTGuiObject;
import tfar.ae2wt.wpt.WPTItem;
import tfar.ae2wt.wut.WUTHandler;
import tfar.ae2wt.wut.WUTItem;

@Mixin(value = ContainerTypeBuilder.class, remap = false)
public class ContainerTypeBuilderMixin<I> {
    @Shadow
    @Final
    private Class<I> hostInterface;

    @Inject(method = "getHostFromPlayerInventory", at = @At("HEAD"), cancellable = true)
    private void getWirelessHostFromPlayerInventory(PlayerEntity player, ContainerLocator locator, CallbackInfoReturnable<I> cir) {
        ItemStack it = ContainerHelper.resolveTerminal(player, locator, AbstractWirelessTerminalItem.class);
        if (it.isEmpty()) return;

        AbstractWirelessTerminalItem awti = (AbstractWirelessTerminalItem) it.getItem();
        int slot = locator != null ? locator.getItemIndex() : -1;

        if (awti instanceof WCTItem) {
            cir.setReturnValue(hostInterface.cast(new WCTGuiObject(awti, it, player, slot)));
        } else if (awti instanceof WPTItem) {
            cir.setReturnValue(hostInterface.cast(new WPTGuiObject(awti, it, player, slot)));
        } else if (awti instanceof WITItem) {
            cir.setReturnValue(hostInterface.cast(new WITGuiObject(awti, it, player, slot)));
        } else if (awti instanceof WirelessFluidTerminalItem) {
            cir.setReturnValue(hostInterface.cast(new WFluidTGuiObject(awti, it, player, slot)));
        } else if (awti instanceof WirelessChemicalTerminalItem && ChemicalHelper.CHEMICALS_PRESENT) {
            cir.setReturnValue(hostInterface.cast(new WChemGuiObject(awti, it, player, slot)));
        } else if (awti instanceof WUTItem) {
            String current = WUTHandler.getCurrentTerminal(it);
            switch (current) {
                case "crafting":  cir.setReturnValue(hostInterface.cast(new WCTGuiObject(awti, it, player, slot))); break;
                case "pattern":   cir.setReturnValue(hostInterface.cast(new WPTGuiObject(awti, it, player, slot))); break;
                case "interface": cir.setReturnValue(hostInterface.cast(new WITGuiObject(awti, it, player, slot))); break;
                case "fluid":     cir.setReturnValue(hostInterface.cast(new WFluidTGuiObject(awti, it, player, slot))); break;
                case "chemical":
                    if (ChemicalHelper.CHEMICALS_PRESENT) cir.setReturnValue(hostInterface.cast(new WChemGuiObject(awti, it, player, slot)));
                    break;
            }
        }
    }
}
