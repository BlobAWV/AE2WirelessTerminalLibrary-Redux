package tfar.ae2wt;

import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tfar.ae2wt.util.CuriosHelper;
import tfar.ae2wt.magnet.MagnetHandler;

import java.lang.reflect.Method;


public class Events {

    public static void serverTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.START && !e.player.world.isRemote) {
            MagnetHandler.doMagnet(e.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (CuriosHelper.CURIOS_PRESENT) {
            try {
                Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Method getSlotHelperMethod = curiosApiClass.getMethod("getSlotHelper");
                Object slotHelper = getSlotHelperMethod.invoke(null);
                if (slotHelper != null) {
                    Method unlockSlotTypeMethod = slotHelper.getClass().getMethod("unlockSlotType", String.class, LivingEntity.class);
                    unlockSlotTypeMethod.invoke(slotHelper, "wireless_terminal", event.getPlayer());
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Mod.EventBusSubscriber(modid = AE2WirelessTerminals.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onTextureStitch(TextureStitchEvent.Pre event) {
            if (event.getMap().getTextureLocation().equals(PlayerContainer.LOCATION_BLOCKS_TEXTURE)) {
                event.addSprite(new ResourceLocation("ae2wtlib", "item/empty_wireless_terminal_slot"));
            }
        }
    }

}
