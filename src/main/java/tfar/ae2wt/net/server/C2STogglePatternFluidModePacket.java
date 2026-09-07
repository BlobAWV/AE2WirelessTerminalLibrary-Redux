package tfar.ae2wt.net.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.ae2wt.wpt.WirelessPatternTerminalContainer;

import java.util.function.Supplier;

public class C2STogglePatternFluidModePacket {

    private final boolean fluidMode;

    public C2STogglePatternFluidModePacket(boolean fluidMode) {
        this.fluidMode = fluidMode;
    }

    public C2STogglePatternFluidModePacket(PacketBuffer buf) {
        this.fluidMode = buf.readBoolean();
    }

    public void encode(PacketBuffer buf) {
        buf.writeBoolean(fluidMode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PlayerEntity player = ctx.get().getSender();
        if (player == null) return;
        ctx.get().enqueueWork(() -> {
            if (player.openContainer instanceof WirelessPatternTerminalContainer) {
                WirelessPatternTerminalContainer container = (WirelessPatternTerminalContainer) player.openContainer;
                container.setFluidMode(fluidMode);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}