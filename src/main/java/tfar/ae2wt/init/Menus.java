package tfar.ae2wt.init;

import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.core.Api;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import tfar.ae2wt.AE2WirelessTerminals;
import tfar.ae2wt.WTConfig;
import tfar.ae2wt.magnet.MagnetFilterContainer;
import tfar.ae2wt.util.ChemicalHelper;
import tfar.ae2wt.wirelesschemicalterminal.WirelessChemicalTerminalContainer;
import tfar.ae2wt.wirelesscraftingterminal.WirelessCraftingTerminalContainer;
import tfar.ae2wt.wirelessfluidterminal.WirelessFluidTerminalContainer;
import tfar.ae2wt.wirelessinterfaceterminal.WirelessInterfaceTerminalContainer;
import tfar.ae2wt.wpt.WirelessPatternTerminalContainer;
import tfar.ae2wt.wut.WUTHandler;

public class Menus {
    public static ContainerType<WirelessCraftingTerminalContainer> WCT = IForgeContainerType.create(WirelessCraftingTerminalContainer::openClient);
    public static ContainerType<WirelessPatternTerminalContainer> PATTERN = IForgeContainerType.create(WirelessPatternTerminalContainer::openClient);
    public static ContainerType<WirelessInterfaceTerminalContainer> WIT = IForgeContainerType.create(WirelessInterfaceTerminalContainer::openClient);
    public static ContainerType<WirelessFluidTerminalContainer> WIRELESS_FLUID_TERMINAL = IForgeContainerType.create(WirelessFluidTerminalContainer::openClient);
    public static ContainerType<WirelessChemicalTerminalContainer> WIRELESS_CHEMICAL_TERMINAL = IForgeContainerType.create(WirelessChemicalTerminalContainer::openClient);
    public static ContainerType<MagnetFilterContainer> MAGNET_FILTER_CONTAINER = IForgeContainerType.create((windowId, inv, data) -> {
        ItemStack terminal = data.readItemStack();
        return new MagnetFilterContainer(windowId, inv, terminal);
    });

    public static void menus(RegistryEvent.Register<ContainerType<?>> e) {
        AE2WirelessTerminals.register("wireless_crafting_terminal",WCT,e.getRegistry());
        AE2WirelessTerminals.register( "wireless_pattern_terminal",PATTERN ,e.getRegistry());
        AE2WirelessTerminals.register( "wireless_interface_terminal",WIT,e.getRegistry());
        AE2WirelessTerminals.register( "wireless_fluid_terminal", WIRELESS_FLUID_TERMINAL,e.getRegistry());
        AE2WirelessTerminals.register("magnet_filter", MAGNET_FILTER_CONTAINER, e.getRegistry());

        WUTHandler.addTerminal("crafting", ModItems.CRAFTING_TERMINAL::open);
        WUTHandler.addTerminal("pattern", ModItems.PATTERN_TERMINAL::open);
        WUTHandler.addTerminal("interface", ModItems.INTERFACE_TERMINAL::open);
        WUTHandler.addTerminal("fluid", ModItems.WIRELESS_FLUID_TERMINAL::open);

        ContainerOpener.addOpener(WCT, (new CheckedOpener(WirelessCraftingTerminalContainer::openServer))::open);
        ContainerOpener.addOpener(PATTERN, (new CheckedOpener(WirelessPatternTerminalContainer::openServer))::open);
        ContainerOpener.addOpener(WIT, (new CheckedOpener(WirelessInterfaceTerminalContainer::openServer))::open);
        ContainerOpener.addOpener(WIRELESS_FLUID_TERMINAL, (new CheckedOpener(WirelessFluidTerminalContainer::openServer))::open);

        Api.instance().registries().charger().addChargeRate(ModItems.CRAFTING_TERMINAL, WTConfig.getChargeRate());
        Api.instance().registries().charger().addChargeRate(ModItems.PATTERN_TERMINAL, WTConfig.getChargeRate());
        Api.instance().registries().charger().addChargeRate(ModItems.INTERFACE_TERMINAL, WTConfig.getChargeRate());
        Api.instance().registries().charger().addChargeRate(ModItems.UNIVERSAL_TERMINAL, WTConfig.getChargeRate());
        Api.instance().registries().charger().addChargeRate(ModItems.WIRELESS_FLUID_TERMINAL, WTConfig.getChargeRate());

        if (ChemicalHelper.CHEMICALS_PRESENT && ModItems.WIRELESS_CHEMICAL_TERMINAL != null) {
            AE2WirelessTerminals.register("wireless_chemical_terminal", WIRELESS_CHEMICAL_TERMINAL, e.getRegistry());
            ContainerOpener.addOpener(WIRELESS_CHEMICAL_TERMINAL, (new CheckedOpener(WirelessChemicalTerminalContainer::openServer))::open);
            Api.instance().registries().charger().addChargeRate(ModItems.WIRELESS_CHEMICAL_TERMINAL, WTConfig.getChargeRate());
            WUTHandler.addTerminal("chemical", ModItems.WIRELESS_CHEMICAL_TERMINAL::open);
        }
    }

    private static class CheckedOpener {
        private final UncheckedOpener opener;
        public CheckedOpener(UncheckedOpener opener) {
            this.opener = opener;
        }

        public boolean open(PlayerEntity player, ContainerLocator locator) {
            if (!(player instanceof ServerPlayerEntity)) {
                // Cannot open containers on the client or for non-players
                return false;
            }

            if (!locator.hasItemIndex()) {
                return false;
            }

            opener.open(player, locator);
            return true;
        }
    }

    @FunctionalInterface
    public interface UncheckedOpener {
        void open(PlayerEntity player, ContainerLocator locator);
    }
}
