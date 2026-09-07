package tfar.ae2wt.net.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.ae2wt.magnet.MagnetFilterContainer;

import java.util.function.Supplier;

public class C2SUpdateMagnetSettings {

    private final String filterMode;
    private final boolean nbtMatch;

    public C2SUpdateMagnetSettings(String filterMode, boolean nbtMatch) {
        this.filterMode = filterMode;
        this.nbtMatch = nbtMatch;
    }

    public C2SUpdateMagnetSettings(PacketBuffer buf) {
        this.filterMode = buf.readString(32767);
        this.nbtMatch = buf.readBoolean();
    }

    public void encode(PacketBuffer buf) {
        buf.writeString(filterMode);
        buf.writeBoolean(nbtMatch);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PlayerEntity player = ctx.get().getSender();
        if (player == null) return;
        ctx.get().enqueueWork(() -> {
            if (!(player instanceof ServerPlayerEntity)) return;
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            Container container = serverPlayer.openContainer;
            if (container instanceof MagnetFilterContainer) {
                MagnetFilterContainer filterContainer = (MagnetFilterContainer) container;
                filterContainer.setFilterMode(filterMode);
                filterContainer.setNbtMatch(nbtMatch);
                filterContainer.saveFiltersToNBT();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}