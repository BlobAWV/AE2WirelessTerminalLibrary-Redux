package tfar.ae2wt.init;

import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tfar.ae2wt.AE2WirelessTerminals;
import tfar.ae2wt.recipe.MagnetInstallSerializer;
import tfar.ae2wt.wut.recipe.CombineSerializer;
import tfar.ae2wt.wut.recipe.UpgradeSerializer;

@Mod.EventBusSubscriber(modid = AE2WirelessTerminals.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRecipeSerializers {

    @SubscribeEvent
    public static void registerSerializers(RegistryEvent.Register<IRecipeSerializer<?>> event) {
        event.getRegistry().register(CombineSerializer.INSTANCE.setRegistryName(CombineSerializer.ID));
        event.getRegistry().register(UpgradeSerializer.INSTANCE.setRegistryName(UpgradeSerializer.ID));
        event.getRegistry().register(MagnetInstallSerializer.INSTANCE.setRegistryName(MagnetInstallSerializer.ID));
    }
}