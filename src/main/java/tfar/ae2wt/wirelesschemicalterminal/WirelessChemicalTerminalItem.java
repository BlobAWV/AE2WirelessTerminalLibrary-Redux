package tfar.ae2wt.wirelesschemicalterminal;

import appeng.container.ContainerLocator;
import appeng.core.AEConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import tfar.ae2wt.AE2WirelessTerminals;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;

public class WirelessChemicalTerminalItem extends AbstractWirelessTerminalItem {
    public WirelessChemicalTerminalItem() {
        super(AEConfig.instance().getWirelessTerminalBattery(),
                new Properties().group(AE2WirelessTerminals.ITEM_GROUP).maxStackSize(1));
    }

    @Override
    public void open(PlayerEntity player, ContainerLocator locator) {
        WirelessChemicalTerminalContainer.openServer(player, locator);
    }

    @Override
    public boolean canHandle(ItemStack is) {
        return is.getItem() instanceof WirelessChemicalTerminalItem;
    }
}