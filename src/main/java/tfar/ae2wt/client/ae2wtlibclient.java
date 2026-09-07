package tfar.ae2wt.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ScreenRegistration;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.container.AEBaseContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import tfar.ae2wt.init.Menus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import tfar.ae2wt.magnet.MagnetFilterScreen;
import tfar.ae2wt.net.C2SHotkeyPacket;
import tfar.ae2wt.net.C2SOpenCuriosTerminalPacket;
import tfar.ae2wt.net.PacketHandler;
import tfar.ae2wt.net.server.C2SOpenMagnetFilterGui;
import tfar.ae2wt.net.server.C2SToggleMagnetEnabled;
import tfar.ae2wt.net.server.C2SToggleMagnetPickupMode;
import tfar.ae2wt.util.ChemicalHelper;
import tfar.ae2wt.util.CuriosHelper;
import tfar.ae2wt.wirelesschemicalterminal.WirelessChemicalTerminalContainer;
import tfar.ae2wt.wirelesschemicalterminal.WirelessChemicalTerminalScreen;
import tfar.ae2wt.wirelesscraftingterminal.WirelessCraftingTerminalScreen;
import tfar.ae2wt.wirelessfluidterminal.WirelessFluidTerminalScreen;
import tfar.ae2wt.wirelessinterfaceterminal.WITScreen;
import tfar.ae2wt.wpt.WirelessPatternTerminalScreen;
import net.minecraft.client.gui.ScreenManager;

import java.io.FileNotFoundException;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ae2wtlib", value = Dist.CLIENT)

public class ae2wtlibclient {

    // Key bindings
    public static KeyBinding wct = new KeyBinding("key.ae2wtlib.wct", GLFW.GLFW_KEY_UNKNOWN, "key.category.ae2wtlib");
    public static KeyBinding wpt = new KeyBinding("key.ae2wtlib.wpt", GLFW.GLFW_KEY_UNKNOWN, "key.category.ae2wtlib");
    public static KeyBinding wit = new KeyBinding("key.ae2wtlib.wit", GLFW.GLFW_KEY_UNKNOWN, "key.category.ae2wtlib");
    public static KeyBinding fluid = new KeyBinding("key.ae2wtlib.fluid", GLFW.GLFW_KEY_UNKNOWN, "key.category.ae2wtlib");
    public static KeyBinding chemical = new KeyBinding("key.ae2wtlib.chemical", GLFW.GLFW_KEY_UNKNOWN, "key.category.ae2wtlib");
    public static KeyBinding openCuriosTerminal = new KeyBinding("key.ae2wtlib.open_curios_terminal", GLFW.GLFW_KEY_UNKNOWN, "key.category.ae2wtlib");
    public static KeyBinding toggleMagnet = new KeyBinding("key.ae2wtlib.toggle_magnet", GLFW.GLFW_KEY_UNKNOWN, "key.category.ae2wtlib");
    public static KeyBinding togglePickupMode = new KeyBinding("key.ae2wtlib.toggle_pickup_mode", GLFW.GLFW_KEY_UNKNOWN, "key.category.ae2wtlib");
    public static KeyBinding openMagnetFilter = new KeyBinding("key.ae2wtlib.open_magnet_filter", GLFW.GLFW_KEY_UNKNOWN, "key.category.ae2wtlib");


    public static void setup(FMLClientSetupEvent e) {
        register(Menus.WCT, WirelessCraftingTerminalScreen::new,"/screens/wtlib/wireless_crafting_terminal.json");
        register(Menus.PATTERN, WirelessPatternTerminalScreen::new,"/screens/wtlib/wireless_pattern_terminal.json");
        register(Menus.WIT, WITScreen::new,"/screens/wtlib/wireless_interface_terminal.json");
        register(Menus.WIRELESS_FLUID_TERMINAL, WirelessFluidTerminalScreen::new,"/screens/wtlib/wireless_fluid_terminal.json");
        ScreenManager.registerFactory(Menus.MAGNET_FILTER_CONTAINER, MagnetFilterScreen::new);

        // messy implementation, i sure hope this doesn't cause anything to break.
        if (ChemicalHelper.CHEMICALS_PRESENT) {
            register(Menus.WIRELESS_CHEMICAL_TERMINAL,
                    (ScreenRegistration.StyledScreenFactory) (container, playerInventory, title, style) -> new WirelessChemicalTerminalScreen((WirelessChemicalTerminalContainer) container, playerInventory, title, style),
                    "/screens/wtlib/wireless_chemical_terminal.json");
        }


        ClientRegistry.registerKeyBinding(wct);
        ClientRegistry.registerKeyBinding(wpt);
        ClientRegistry.registerKeyBinding(wit);
        ClientRegistry.registerKeyBinding(fluid);
        ClientRegistry.registerKeyBinding(toggleMagnet);
        ClientRegistry.registerKeyBinding(togglePickupMode);
        ClientRegistry.registerKeyBinding(openMagnetFilter);

        if (ChemicalHelper.CHEMICALS_PRESENT) {
            ClientRegistry.registerKeyBinding(chemical);
        }

        if (CuriosHelper.CURIOS_PRESENT) {
            ClientRegistry.registerKeyBinding(openCuriosTerminal);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        while (wct.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new C2SHotkeyPacket("crafting"));
        }
        while (wpt.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new C2SHotkeyPacket("pattern"));
        }
        while (wit.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new C2SHotkeyPacket("interface"));
        }
        while (fluid.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new C2SHotkeyPacket("fluid"));
        }
        while (chemical.isPressed() && ChemicalHelper.CHEMICALS_PRESENT) {
            PacketHandler.INSTANCE.sendToServer(new C2SHotkeyPacket("chemical"));
        }
        while (openCuriosTerminal.isPressed() && CuriosHelper.CURIOS_PRESENT) {
            PacketHandler.INSTANCE.sendToServer(new C2SOpenCuriosTerminalPacket());
        }
        while (toggleMagnet.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new C2SToggleMagnetEnabled());
        }
        while (togglePickupMode.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new C2SToggleMagnetPickupMode());
        }
        while (openMagnetFilter.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new C2SOpenMagnetFilterGui());
        }
    }

    /**
     * Registers a screen for a given container and ensures the given style is applied after opening the screen.
     */
    private static <M extends AEBaseContainer, U extends AEBaseScreen<M>> void register(ContainerType<M> type,
                                                                                        ScreenRegistration.StyledScreenFactory<M, U> factory,
                                                                                        String stylePath) {
       // CONTAINER_STYLES.put(type, stylePath);
        ScreenManager.<M,U>registerFactory(type, (container, playerInv, title) -> {
            ScreenStyle style;
            try {
                style = StyleManager.loadStyleDoc(stylePath);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to read Screen JSON file: " + stylePath + ": " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException("Failed to read Screen JSON file: " + stylePath, e);
            }

            return factory.create(container, playerInv, title, style);
        });
    }
}