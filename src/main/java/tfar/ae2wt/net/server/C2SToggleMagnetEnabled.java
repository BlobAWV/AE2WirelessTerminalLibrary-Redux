package tfar.ae2wt.net.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.util.MagnetHelper;

import java.util.function.Supplier;

public class C2SToggleMagnetEnabled {

    public C2SToggleMagnetEnabled() {}

    public C2SToggleMagnetEnabled(PacketBuffer buf) {}

    public void encode(PacketBuffer buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PlayerEntity player = ctx.get().getSender();
        if (player == null) return;
        ctx.get().enqueueWork(() -> {
            ItemStack terminal = MagnetHelper.getMagnetTerminal(player);
            if (terminal.isEmpty()) {
                player.sendMessage(new StringTextComponent("No terminal found"), player.getUniqueID());
                return;
            }
            if (!AbstractWirelessTerminalItem.hasMagnetUpgrade(terminal)) {
                player.sendMessage(new StringTextComponent("No magnet upgrade installed"), player.getUniqueID());
                return;
            }

            boolean current = AbstractWirelessTerminalItem.isMagnetEnabled(terminal);
            boolean newState = !current;
            AbstractWirelessTerminalItem.setMagnetEnabled(terminal, newState);
            player.sendMessage(new StringTextComponent("Magnet " + (newState ? "enabled" : "disabled")), player.getUniqueID());
        });
        ctx.get().setPacketHandled(true);
    }
}