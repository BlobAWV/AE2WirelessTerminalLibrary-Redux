package tfar.ae2wt.net.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.wpt.WirelessPatternTerminalContainer;

import java.util.function.Supplier;

public class C2SLoadPatternPacket {

    private final ItemStack[] inputs;   // length 9
    private final ItemStack[] outputs;  // length 3
    private final boolean craftingMode;
    private final boolean fluidMode;

    public C2SLoadPatternPacket(ItemStack[] inputs, ItemStack[] outputs, boolean craftingMode, boolean fluidMode) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.craftingMode = craftingMode;
        this.fluidMode = fluidMode;
    }

    public C2SLoadPatternPacket(PacketBuffer buf) {
        int inLen = buf.readByte();
        inputs = new ItemStack[inLen];
        for (int i = 0; i < inLen; i++) {
            inputs[i] = buf.readItemStack();
        }
        int outLen = buf.readByte();
        outputs = new ItemStack[outLen];
        for (int i = 0; i < outLen; i++) {
            outputs[i] = buf.readItemStack();
        }
        craftingMode = buf.readBoolean();
        fluidMode = buf.readBoolean();
    }

    public void encode(PacketBuffer buf) {
        buf.writeByte(inputs.length);
        for (ItemStack stack : inputs) {
            buf.writeItemStack(stack);
        }
        buf.writeByte(outputs.length);
        for (ItemStack stack : outputs) {
            buf.writeItemStack(stack);
        }
        buf.writeBoolean(craftingMode);
        buf.writeBoolean(fluidMode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        PlayerEntity player = ctx.get().getSender();
        if (player == null) return;
        ctx.get().enqueueWork(() -> {
            if (player.openContainer instanceof WirelessPatternTerminalContainer) {
                WirelessPatternTerminalContainer container = (WirelessPatternTerminalContainer) player.openContainer;
                // Set modes and force NBT save
                container.craftingMode = craftingMode;
                container.fluidMode = fluidMode;
                AbstractWirelessTerminalItem.setBoolean(container.getPatternTerminal().getItemStack(), craftingMode, "craftingMode");
                AbstractWirelessTerminalItem.setBoolean(container.getPatternTerminal().getItemStack(), fluidMode, "fluidMode");
                container.getPatternTerminal().setCraftingMode(craftingMode);
                container.getPatternTerminal().setFluidMode(fluidMode);
                for (int i = 0; i < 9 && i < inputs.length; i++) {
                    container.getCraftingGridSlot(i).putStack(inputs[i]);
                }
                for (int i = 0; i < 3 && i < outputs.length; i++) {
                    container.getOutputSlot(i).putStack(outputs[i]);
                }
                container.detectAndSendChanges();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}