package tfar.ae2wt.net;

import appeng.container.AEBaseContainer;
import appeng.container.ContainerLocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.ae2wt.terminal.IWirelessTerminalContainer;
import tfar.ae2wt.util.TerminalOpener;
import tfar.ae2wt.wut.WUTItem;
import tfar.ae2wt.wut.WUTHandler;

import java.util.function.Supplier;

public class C2SCycleTerminalPacket {

    public void encode(PacketBuffer buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PlayerEntity player = ctx.get().getSender();
        if (player == null) return;

        ctx.get().enqueueWork(() -> {
            final Container screenHandler = player.openContainer;
            if (!(screenHandler instanceof AEBaseContainer)) return;

            final AEBaseContainer container = (AEBaseContainer) screenHandler;
            final ContainerLocator locator = container.getLocator();

            ItemStack terminal = ItemStack.EMPTY;
            if (container instanceof IWirelessTerminalContainer) {
                terminal = ((IWirelessTerminalContainer) container).getTerminalStack();
            }
            if (terminal.isEmpty() || !(terminal.getItem() instanceof WUTItem)) {
                return;
            }

            WUTHandler.cycle(terminal);

            boolean reopened = false;
            if (locator != null && locator.hasItemIndex()) {
                int slotIndex = locator.getItemIndex();
                if (slotIndex >= 0 && slotIndex < player.inventory.getSizeInventory()) {
                    ItemStack inSlot = player.inventory.getStackInSlot(slotIndex);
                    if (inSlot == terminal) {
                        WUTHandler.open(player, locator);
                        reopened = true;
                    }
                }
            }
            if (!reopened) {
                TerminalOpener.openFromStack(player, terminal, null);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}