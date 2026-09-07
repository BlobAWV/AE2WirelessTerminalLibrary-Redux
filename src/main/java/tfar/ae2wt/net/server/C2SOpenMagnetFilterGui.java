package tfar.ae2wt.net.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkHooks;
import tfar.ae2wt.magnet.MagnetFilterProvider;
import tfar.ae2wt.util.MagnetHelper;

import java.util.function.Supplier;

public class C2SOpenMagnetFilterGui {

    public C2SOpenMagnetFilterGui() {}

    public C2SOpenMagnetFilterGui(PacketBuffer buf) {}

    public void encode(PacketBuffer buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PlayerEntity player = ctx.get().getSender();
        if (player == null) return;
        ctx.get().enqueueWork(() -> {
            ItemStack terminal = MagnetHelper.getMagnetTerminal(player);
            if (terminal.isEmpty()) return;
            if (!(player instanceof ServerPlayerEntity)) return;
            NetworkHooks.openGui((ServerPlayerEntity) player, new MagnetFilterProvider(terminal), buf -> buf.writeItemStack(terminal));
        });
        ctx.get().setPacketHandled(true);
    }
}