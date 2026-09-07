package tfar.ae2wt.net.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.util.MagnetHelper;

import java.util.function.Supplier;

import static tfar.ae2wt.terminal.AbstractWirelessTerminalItem.MAGNET_PICKUP_MODE_KEY;

public class C2SToggleMagnetPickupMode {

    public C2SToggleMagnetPickupMode() {}

    public C2SToggleMagnetPickupMode(PacketBuffer buf) {}

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

            String current = terminal.getOrCreateTag().getString(MAGNET_PICKUP_MODE_KEY);
            if (current.isEmpty()) current = "INVENTORY";
            String newMode = current.equals("INVENTORY") ? "ME" : "INVENTORY";
            terminal.getOrCreateTag().putString(MAGNET_PICKUP_MODE_KEY, newMode);
            player.sendMessage(new StringTextComponent("Magnet pickup mode: " + newMode), player.getUniqueID());
        });
        ctx.get().setPacketHandled(true);
    }
}