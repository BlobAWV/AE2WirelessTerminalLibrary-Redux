package tfar.ae2wt.util;

import appeng.container.ContainerLocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkHooks;
import tfar.ae2wt.wirelesschemicalterminal.WirelessChemicalTerminalItem;
import tfar.ae2wt.wirelesscraftingterminal.WCTGuiObject;
import tfar.ae2wt.wirelesscraftingterminal.WCTItem;
import tfar.ae2wt.wirelessfluidterminal.WFluidTGuiObject;
import tfar.ae2wt.wirelessfluidterminal.WirelessFluidTerminalItem;
import tfar.ae2wt.wirelessinterfaceterminal.WITGuiObject;
import tfar.ae2wt.wirelessinterfaceterminal.WITItem;
import tfar.ae2wt.wpt.WPTGuiObject;
import tfar.ae2wt.wirelesschemicalterminal.WChemGuiObject;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.wpt.WPTItem;
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

        int slot = findTerminalSlot(player, stack);
        ContainerLocator locator = (slot >= 0)
                ? ContainerHelper.getContainerLocatorForSlot(slot)
                : ContainerHelper.curiosLocator();

        Consumer<PacketBuffer> dataWriter = buf -> {
            buf.writeItemStack(stack);
            locator.write(buf);
        };

        if (item instanceof WUTItem) {
            String terminalToOpen = (requestedType != null && WUTHandler.hasTerminal(stack, requestedType))
                    ? requestedType
                    : WUTHandler.getCurrentTerminal(stack);
            switch (terminalToOpen) {
                case "crafting":
                    openWCT(serverPlayer, item, stack, locator, dataWriter);
                    break;
                case "pattern":
                    openWPT(serverPlayer, item, stack, locator, dataWriter);
                    break;
                case "interface":
                    openWIT(serverPlayer, item, stack, locator, dataWriter);
                    break;
                case "fluid":
                    openWFluid(serverPlayer, item, stack, locator, dataWriter);
                    break;
                case "chemical":
                    if (ChemicalHelper.CHEMICALS_PRESENT) openWChem(serverPlayer, item, stack, locator, dataWriter);
                    break;
            }
            return;
        }
        // Non-WUT terminals – open the matching container
        if (item instanceof WCTItem) openWCT(serverPlayer, item, stack, locator, dataWriter);
        else if (item instanceof WPTItem) openWPT(serverPlayer, item, stack, locator, dataWriter);
        else if (item instanceof WITItem) openWIT(serverPlayer, item, stack, locator, dataWriter);
        else if (item instanceof WirelessFluidTerminalItem) openWFluid(serverPlayer, item, stack, locator, dataWriter);
        else if (item instanceof WirelessChemicalTerminalItem && ChemicalHelper.CHEMICALS_PRESENT) openWChem(serverPlayer, item, stack, locator, dataWriter);
    }

    private static int findTerminalSlot(PlayerEntity player, ItemStack stack) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (player.inventory.getStackInSlot(i) == stack) return i;
        }
        return -1;
    }

    private static void openWCT(ServerPlayerEntity player, AbstractWirelessTerminalItem item, ItemStack stack, ContainerLocator locator, Consumer<PacketBuffer> writer) {
        WCTGuiObject gui = new WCTGuiObject(item, stack, player, locator.getItemIndex());
        NetworkHooks.openGui(player, new tfar.ae2wt.wirelesscraftingterminal.TermFactory(gui, locator), writer);
    }

    private static void openWPT(ServerPlayerEntity player, AbstractWirelessTerminalItem item, ItemStack stack, ContainerLocator locator, Consumer<PacketBuffer> writer) {
        WPTGuiObject gui = new WPTGuiObject(item, stack, player, locator.getItemIndex());
        NetworkHooks.openGui(player, new tfar.ae2wt.wpt.TermFactory(gui, locator), writer);
    }

    private static void openWIT(ServerPlayerEntity player, AbstractWirelessTerminalItem item, ItemStack stack, ContainerLocator locator, Consumer<PacketBuffer> writer) {
        WITGuiObject gui = new WITGuiObject(item, stack, player, locator.getItemIndex());
        NetworkHooks.openGui(player, new tfar.ae2wt.wirelessinterfaceterminal.TermFactory(gui, locator), writer);
    }

    private static void openWFluid(ServerPlayerEntity player, AbstractWirelessTerminalItem item, ItemStack stack, ContainerLocator locator, Consumer<PacketBuffer> writer) {
        WFluidTGuiObject gui = new WFluidTGuiObject(item, stack, player, locator.getItemIndex());
        NetworkHooks.openGui(player, new tfar.ae2wt.wirelessfluidterminal.TermFactory(gui, locator), writer);
    }

    private static void openWChem(ServerPlayerEntity player, AbstractWirelessTerminalItem item, ItemStack stack, ContainerLocator locator, Consumer<PacketBuffer> writer) {
        WChemGuiObject gui = new WChemGuiObject(item, stack, player, locator.getItemIndex());
        NetworkHooks.openGui(player, new tfar.ae2wt.wirelesschemicalterminal.TermFactory(gui, locator), writer);
    }
}