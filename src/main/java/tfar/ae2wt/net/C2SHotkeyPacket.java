package tfar.ae2wt.net;

import appeng.container.ContainerLocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.ae2wt.init.ModItems;
import tfar.ae2wt.util.ChemicalHelper;
import tfar.ae2wt.util.ContainerHelper;
import tfar.ae2wt.util.CuriosHelper;
import tfar.ae2wt.util.TerminalOpener;

import java.util.function.Supplier;

public class C2SHotkeyPacket {

    private String terminalName;

    public C2SHotkeyPacket(String terminalName) {
        this.terminalName = terminalName;
    }

    public C2SHotkeyPacket(PacketBuffer buf) {
        terminalName = buf.readString(32767);
    }

    public void encode(PacketBuffer buf) {
        buf.writeString(terminalName);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PlayerEntity player = ctx.get().getSender();
        if (player == null) return;

        ctx.get().enqueueWork(() -> {
            MinecraftServer server = player.getServer();
            server.execute(() -> {
                int slot = -1;
                PlayerInventory inv = player.inventory;
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack stack = inv.getStackInSlot(i);
                    if (CuriosHelper.matchesTerminalType(stack, terminalName)) {
                        slot = i;
                        break;
                    }
                }
                if (slot != -1) {
                    ContainerLocator locator = ContainerHelper.getContainerLocatorForSlot(slot);
                    openTerminal(player, terminalName, locator);
                    return;
                }

                if (CuriosHelper.CURIOS_PRESENT) {
                    ItemStack curiosStack = CuriosHelper.findTerminalOfType(player, terminalName);
                    if (!curiosStack.isEmpty()) {
                        TerminalOpener.openFromStack(player, curiosStack, terminalName);
                        return;
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private void openTerminal(PlayerEntity player, String type, ContainerLocator locator) {
        switch (type) {
            case "crafting": ModItems.CRAFTING_TERMINAL.open(player, locator); break;
            case "pattern": ModItems.PATTERN_TERMINAL.open(player, locator); break;
            case "interface": ModItems.INTERFACE_TERMINAL.open(player, locator); break;
            case "fluid": ModItems.WIRELESS_FLUID_TERMINAL.open(player, locator); break;
            case "chemical": if (ChemicalHelper.CHEMICALS_PRESENT) ModItems.WIRELESS_CHEMICAL_TERMINAL.open(player, locator); break;
        }
    }

}
