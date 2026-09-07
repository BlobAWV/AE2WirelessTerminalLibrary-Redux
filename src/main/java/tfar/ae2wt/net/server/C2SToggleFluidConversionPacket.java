package tfar.ae2wt.net.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.ae2wt.wpt.WirelessPatternTerminalContainer;

import java.util.function.Supplier;

public class C2SToggleFluidConversionPacket {

    private final boolean enabled;

    public C2SToggleFluidConversionPacket(boolean enabled) {
        this.enabled = enabled;
    }

    public C2SToggleFluidConversionPacket(PacketBuffer buf) {
        this.enabled = buf.readBoolean();
    }

    public void encode(PacketBuffer buf) {
        buf.writeBoolean(enabled);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PlayerEntity player = ctx.get().getSender();
        if (player == null) return;
        ctx.get().enqueueWork(() -> {
            if (player.openContainer instanceof WirelessPatternTerminalContainer) {
                ((WirelessPatternTerminalContainer) player.openContainer).setFluidConversion(enabled);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}