package tfar.ae2wt.net;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.ae2wt.wpt.WirelessPatternTerminalContainer;

import java.util.function.Supplier;

public class C2SPatternSlotPacket {

    IAEItemStack slotItem;
    boolean shift;
    IAEItemStack[] pattern = new IAEItemStack[9];

    public C2SPatternSlotPacket(IAEItemStack slotItem, boolean shift, IAEItemStack[] pattern) {
        this.slotItem = slotItem;
        this.shift = shift;
        if (pattern != null && pattern.length == 9) {
            this.pattern = pattern;
        } else {
            this.pattern = new IAEItemStack[9];
        }
    }

    public C2SPatternSlotPacket(PacketBuffer buf) {
        if (buf.readBoolean()) {
            slotItem = AEItemStack.fromPacket(buf);
        } else {
            slotItem = null;
        }
        shift = buf.readBoolean();
        for (int i = 0; i < 9; i++) {
            if (buf.readBoolean()) {
                pattern[i] = AEItemStack.fromPacket(buf);
            } else {
                pattern[i] = null;
            }
        }
    }

    public void encode(PacketBuffer buf) {
        if (slotItem != null) {
            buf.writeBoolean(true);
            slotItem.writeToPacket(buf);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeBoolean(shift);
        for (int i = 0; i < 9; i++) {
            if (pattern[i] != null) {
                buf.writeBoolean(true);
                pattern[i].writeToPacket(buf);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PlayerEntity player = ctx.get().getSender();
        if (player == null) return;
        ctx.get().enqueueWork(() -> {
            MinecraftServer server = player.getServer();
            server.execute(() -> {
                if (player.openContainer instanceof WirelessPatternTerminalContainer) {
                    final WirelessPatternTerminalContainer patternTerminal = (WirelessPatternTerminalContainer) player.openContainer;
                    patternTerminal.craftOrGetItem(slotItem, shift, pattern);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}