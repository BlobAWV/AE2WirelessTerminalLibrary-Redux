package tfar.ae2wt.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkHooks;
import tfar.ae2wt.wirelesscraftingterminal.WCTGuiObject;
import tfar.ae2wt.wirelesscraftingterminal.TermFactory;
import tfar.ae2wt.wirelessfluidterminal.WFluidTGuiObject;
import tfar.ae2wt.wirelessinterfaceterminal.WITGuiObject;
import tfar.ae2wt.wpt.WPTGuiObject;
import tfar.ae2wt.wirelesschemicalterminal.WChemGuiObject;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.wut.WUTHandler;
import tfar.ae2wt.wut.WUTItem;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class TerminalOpener {

    public static void openFromStack(PlayerEntity player, ItemStack stack, @Nullable String requestedType) {
        if (!(player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return;
        AbstractWirelessTerminalItem item = (AbstractWirelessTerminalItem) stack.getItem();

        Consumer<PacketBuffer> dataWriter = buf -> {
            buf.writeBoolean(false);
            buf.writeItemStack(stack);
        };


        if (item instanceof WUTItem) {
            String terminalToOpen;
            if (requestedType != null && WUTHandler.hasTerminal(stack, requestedType)) {
                terminalToOpen = requestedType;
            } else {
                terminalToOpen = WUTHandler.getCurrentTerminal(stack);
            }

            if (terminalToOpen.equals("crafting")) {
                WCTGuiObject gui = new WCTGuiObject(item, stack, player, -1);
                NetworkHooks.openGui(serverPlayer, new tfar.ae2wt.wirelesscraftingterminal.TermFactory(gui, null), dataWriter);
            } else if (terminalToOpen.equals("pattern")) {
                WPTGuiObject gui = new WPTGuiObject(item, stack, player, -1);
                NetworkHooks.openGui(serverPlayer, new tfar.ae2wt.wpt.TermFactory(gui, null), dataWriter);
            } else if (terminalToOpen.equals("interface")) {
                WITGuiObject gui = new WITGuiObject(item, stack, player, -1);
                NetworkHooks.openGui(serverPlayer, new tfar.ae2wt.wirelessinterfaceterminal.TermFactory(gui, null), dataWriter);
            } else if (terminalToOpen.equals("fluid")) {
                WFluidTGuiObject gui = new WFluidTGuiObject(item, stack, player, -1);
                NetworkHooks.openGui(serverPlayer, new tfar.ae2wt.wirelessfluidterminal.TermFactory(gui, null), dataWriter);
            } else if (terminalToOpen.equals("chemical") && ChemicalHelper.CHEMICALS_PRESENT) {
                WChemGuiObject gui = new WChemGuiObject(item, stack, player, -1);
                NetworkHooks.openGui(serverPlayer, new tfar.ae2wt.wirelesschemicalterminal.TermFactory(gui, null), dataWriter);
            }
        } else if (item instanceof tfar.ae2wt.wirelesscraftingterminal.WCTItem) {
            WCTGuiObject gui = new WCTGuiObject(item, stack, player, -1);
            NetworkHooks.openGui(serverPlayer, new TermFactory(gui, null), dataWriter);
        } else if (item instanceof tfar.ae2wt.wpt.WPTItem) {
            WPTGuiObject gui = new WPTGuiObject(item, stack, player, -1);
            NetworkHooks.openGui(serverPlayer, new tfar.ae2wt.wpt.TermFactory(gui, null), dataWriter);
        } else if (item instanceof tfar.ae2wt.wirelessinterfaceterminal.WITItem) {
            WITGuiObject gui = new WITGuiObject(item, stack, player, -1);
            NetworkHooks.openGui(serverPlayer, new tfar.ae2wt.wirelessinterfaceterminal.TermFactory(gui, null), dataWriter);
        } else if (item instanceof tfar.ae2wt.wirelessfluidterminal.WirelessFluidTerminalItem) {
            WFluidTGuiObject gui = new WFluidTGuiObject(item, stack, player, -1);
            NetworkHooks.openGui(serverPlayer, new tfar.ae2wt.wirelessfluidterminal.TermFactory(gui, null), dataWriter);
        } else if (item instanceof tfar.ae2wt.wirelesschemicalterminal.WirelessChemicalTerminalItem && ChemicalHelper.CHEMICALS_PRESENT) {
            WChemGuiObject gui = new WChemGuiObject(item, stack, player, -1);
            NetworkHooks.openGui(serverPlayer, new tfar.ae2wt.wirelesschemicalterminal.TermFactory(gui, null), dataWriter);
        }
    }
}