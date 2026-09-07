package tfar.ae2wt.net;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.ae2wt.util.CuriosHelper;
import tfar.ae2wt.util.TerminalOpener;

import java.util.function.Supplier;

public class C2SOpenCuriosTerminalPacket {

    public C2SOpenCuriosTerminalPacket() {}

    public C2SOpenCuriosTerminalPacket(PacketBuffer buf) {}

    public void encode(PacketBuffer buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PlayerEntity player = ctx.get().getSender();
        if (player == null) return;

        ctx.get().enqueueWork(() -> {
            if (!CuriosHelper.CURIOS_PRESENT) {
                player.sendMessage(new net.minecraft.util.text.StringTextComponent("Curios is not installed"), player.getUniqueID());
                return;
            }

            ItemStack terminal = CuriosHelper.findFirstTerminal(player);
            if (terminal == null||terminal.isEmpty()) {
                player.sendMessage(new net.minecraft.util.text.StringTextComponent("No wireless terminal found in curios slot"), player.getUniqueID());
                return;
            }

            TerminalOpener.openFromStack(player, terminal, null);
        });
        ctx.get().setPacketHandled(true);
    }
}