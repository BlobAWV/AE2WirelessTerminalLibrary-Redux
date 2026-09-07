package tfar.ae2wt.wirelesschemicalterminal;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEStack;
import appeng.container.ContainerLocator;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.container.me.common.MEMonitorableContainer;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraftforge.fml.network.NetworkHooks;
import tfar.ae2wt.WTConfig;
import tfar.ae2wt.init.Menus;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.terminal.IWirelessTerminalContainer;
import tfar.ae2wt.util.ChemicalHelper;
import tfar.ae2wt.util.CuriosHelper;
import tfar.ae2wt.wut.WUTItem;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public class WirelessChemicalTerminalContainer extends MEMonitorableContainer implements IWirelessTerminalContainer {
    // Raw type because IAEChemicalStack is not on classpath at compile time

    @Override
    public ItemStack getTerminalStack() {
        return guiObject.getItemStack();
    }

    private final WChemGuiObject guiObject;
    private double powerMultiplier = 1.0;
    private int ticks = 0;


    public static WirelessChemicalTerminalContainer openClient(int windowId, PlayerInventory inv, PacketBuffer data) {
        boolean hasLocator = data.readBoolean();
        PlayerEntity player = inv.player;
        if (hasLocator) {
            ContainerLocator locator = ContainerLocator.read(data);
            ItemStack it = player.inventory.getStackInSlot(locator.getItemIndex());
            WChemGuiObject host = new WChemGuiObject((AbstractWirelessTerminalItem) it.getItem(), it, player, locator.getItemIndex());
            return new WirelessChemicalTerminalContainer(windowId, inv, host);
        } else {
            ItemStack stack = data.readItemStack();
            WChemGuiObject host = new WChemGuiObject((AbstractWirelessTerminalItem) stack.getItem(), stack, player, -1);
            return new WirelessChemicalTerminalContainer(windowId, inv, host);
        }
    }

    public static void openServer(PlayerEntity player, ContainerLocator locator) {
        ItemStack it = player.inventory.getStackInSlot(locator.getItemIndex());
        WChemGuiObject accessInterface = new WChemGuiObject((AbstractWirelessTerminalItem) it.getItem(), it, player, locator.getItemIndex());
        if (locator.hasItemIndex()) {
            NetworkHooks.openGui((ServerPlayerEntity) player,
                    new tfar.ae2wt.wirelesschemicalterminal.TermFactory(accessInterface, locator),
                    buf -> {
                        if (locator != null) {
                            buf.writeBoolean(true);
                            locator.write(buf);
                        } else {
                            buf.writeBoolean(false);
                            buf.writeItemStack(it);
                        }
                    });
        }
    }

    public WirelessChemicalTerminalContainer(int id, PlayerInventory ip, ITerminalHost host) {
        super(Menus.WIRELESS_CHEMICAL_TERMINAL, id, ip, host, true, ChemicalHelper.getChemicalStorageChannel());
        this.guiObject = (WChemGuiObject) host;
        int slotIndex = ((IInventorySlotAware) guiObject).getInventorySlot();
        if (slotIndex >= 0 && slotIndex < 36) {
            lockPlayerInventorySlot(slotIndex);
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (!guiObject.rangeCheck()) {
            if (isValidContainer()) {
                getPlayerInventory().player.sendMessage(PlayerMessages.OutOfRange.get(), Util.DUMMY_UUID);
                getPlayerInventory().player.closeScreen();
            }
            setValidContainer(false);
            return;
        }

        powerMultiplier = WTConfig.getPowerMultiplier(guiObject.getRange(), guiObject.isOutOfRange());
        ticks++;
        if (ticks > 10) {
            guiObject.extractAEPower(powerMultiplier * ticks, Actionable.MODULATE, PowerMultiplier.CONFIG);
            ticks = 0;
        }

        if (guiObject.extractAEPower(1, Actionable.SIMULATE, PowerMultiplier.ONE) == 0) {
            if (isValidContainer()) {
                getPlayerInventory().player.sendMessage(PlayerMessages.DeviceNotPowered.get(), Util.DUMMY_UUID);
                getPlayerInventory().player.closeScreen();
            }
            setValidContainer(false);
        }
    }

    @Override
    public void doAction(ServerPlayerEntity player, InventoryAction action, int slot, long id) {
        if (action == InventoryAction.FILL_ITEM || action == InventoryAction.EMPTY_ITEM) {
            handleNetworkInteraction(player, null, action);
            return;
        }
        super.doAction(player, action, slot, id);
    }

    protected void updateHeld(ServerPlayerEntity player) {
        if (player == null) return;
        appeng.core.sync.network.NetworkHandler.instance().sendTo(
                new appeng.core.sync.packets.InventoryActionPacket(appeng.helpers.InventoryAction.UPDATE_HAND, 0, player.inventory.getItemStack()),
                player
        );
        this.detectAndSendChanges();
    }

    @Override
    protected void handleNetworkInteraction(ServerPlayerEntity player, @Nullable IAEStack stack, InventoryAction action) {

        if (action != InventoryAction.FILL_ITEM && action != InventoryAction.EMPTY_ITEM) {
            return;
        }

        ItemStack heldItem = player.inventory.getItemStack();
        if (heldItem.getCount() != 1) {
            return;
        }

        IStorageChannel<?> channel = ChemicalHelper.getChemicalStorageChannel();
        if (channel == null) {
            return;
        }

        if (monitor == null || powerSource == null) {
            return;
        }

        IActionSource actionSource = this.getActionSource();
        Object executeAction = ChemicalHelper.getAction("EXECUTE");
        Object simulateAction = ChemicalHelper.getAction("SIMULATE");
        if (executeAction == null || simulateAction == null) {
            return;
        }

        try {
            Method createStackMethod = channel.getClass().getMethod("createStack", Object.class);

            if (action == InventoryAction.FILL_ITEM && stack != null) {

                Object chemicalStack = ChemicalHelper.getChemicalStackMethod.invoke(stack);
                if (chemicalStack == null) {
                    return;
                }


                Object chemical = ChemicalHelper.getChemicalTypeMethod.invoke(chemicalStack);
                long requestedAmount = ChemicalHelper.getChemicalStackSize(stack);

                Object handler = ChemicalHelper.getChemicalHandler(heldItem, chemicalStack);
                if (handler == null) {
                    return;
                }


                Object remainingAfterInsert = ChemicalHelper.insertChemicalIntoHandler(handler, chemicalStack, simulateAction);
                long cannotInsert = remainingAfterInsert != null ? ChemicalHelper.getChemicalAmount(remainingAfterInsert) : requestedAmount;
                long canInsert = requestedAmount - cannotInsert;
                if (canInsert <= 0) {
                    return;
                }


                long currentNetworkAmount = ChemicalHelper.getNetworkAmount((IMEMonitor) monitor, chemical, channel);

                long maxExtract = Math.min(canInsert, Math.max(0, currentNetworkAmount - 1000)); // leave 1000mB
                if (maxExtract <= 0) {
                    return;
                }
                Object extractStack = ChemicalHelper.createChemicalStack(chemicalStack, maxExtract);
                if (extractStack == null) {
                    return;
                }
                Object iaeExtractStack = createStackMethod.invoke(channel, extractStack);
                if (iaeExtractStack == null) {
                    return;
                }
                Object extracted = Platform.poweredExtraction(powerSource, (IMEMonitor) monitor, (IAEStack) iaeExtractStack, actionSource);
                if (extracted == null) {
                    return;
                }


                Object extractedChemicalStack = ChemicalHelper.getChemicalStackMethod.invoke(extracted);
                if (extractedChemicalStack == null) {
                    return;
                }

                Object remaining = ChemicalHelper.insertChemicalIntoHandler(handler, extractedChemicalStack, executeAction);
                if (remaining != null && ChemicalHelper.getChemicalAmount(remaining) > 0) {

                    Object iaeRemaining = createStackMethod.invoke(channel, remaining);
                    if (iaeRemaining != null) {
                        ((IMEMonitor) monitor).injectItems((IAEStack) iaeRemaining, Actionable.MODULATE, actionSource);
                    }
                }


                if (monitor != null) {
                    monitor.getStorageList();

                }


                player.inventory.setItemStack(heldItem);
                this.updateHeld(player);


            } else if (action == InventoryAction.EMPTY_ITEM) {


                Object handler = null;
                Object extractedStack = null;
                String foundCap = "none";
                String[] capNames = {"GAS_HANDLER_CAPABILITY", "SLURRY_HANDLER_CAPABILITY", "INFUSION_HANDLER_CAPABILITY", "PIGMENT_HANDLER_CAPABILITY"};
                for (String capName : capNames) {
                    try {
                        Class<?> capClass = Class.forName("mekanism.common.capabilities.Capabilities");
                        java.lang.reflect.Field capField = capClass.getField(capName);
                        Object capability = capField.get(null);
                        Method getCapMethod = heldItem.getClass().getMethod("getCapability", net.minecraftforge.common.capabilities.Capability.class, net.minecraft.util.Direction.class);
                        Object lazyOptional = getCapMethod.invoke(heldItem, capability, null);
                        Method isPresentMethod = lazyOptional.getClass().getMethod("isPresent");
                        if ((boolean) isPresentMethod.invoke(lazyOptional)) {
                            Method resolveMethod = lazyOptional.getClass().getMethod("resolve");
                            Object optional = resolveMethod.invoke(lazyOptional);
                            Method isPresentOptional = optional.getClass().getMethod("isPresent");
                            if ((boolean) isPresentOptional.invoke(optional)) {
                                Method getMethod = optional.getClass().getMethod("get");
                                handler = getMethod.invoke(optional);
                                if (handler != null) {
                                    Object testExtract = ChemicalHelper.extractChemicalFromHandler(handler, Integer.MAX_VALUE, simulateAction);
                                    if (testExtract != null && ChemicalHelper.getChemicalAmount(testExtract) > 0) {
                                        extractedStack = testExtract;
                                        foundCap = capName;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }

                if (extractedStack == null || ChemicalHelper.getChemicalAmount(extractedStack) <= 0) {
                    return;
                }

                long totalAmount = ChemicalHelper.getChemicalAmount(extractedStack);

                IMEMonitor freshMonitor = null;
                if (guiObject != null) {
                    IStorageGrid sg = guiObject.getIStorageGrid();
                    if (sg != null) {
                        freshMonitor = sg.getInventory(ChemicalHelper.getChemicalStorageChannel());
                        if (freshMonitor != null) {
                            freshMonitor.getStorageList();
                        }
                    }
                }
                if (freshMonitor == null) {
                    freshMonitor = (IMEMonitor) monitor;
                }

                Object chemical = ChemicalHelper.getChemicalTypeMethod.invoke(extractedStack);
                long networkAmount = ChemicalHelper.getNetworkAmount(freshMonitor, chemical, channel);
                boolean isPresent = networkAmount > 0;


                if (!isPresent) {

                    Object ghostChemicalStack = ChemicalHelper.createChemicalStack(extractedStack, 1000);
                    if (ghostChemicalStack != null) {
                        Object iaeGhost = createStackMethod.invoke(channel, ghostChemicalStack);
                        if (iaeGhost != null) {
                            Object ghostRemainder = freshMonitor.injectItems((IAEStack) iaeGhost, Actionable.MODULATE, actionSource);

                        }
                    }
                }

                Object iaeExtracted = createStackMethod.invoke(channel, extractedStack);
                if (iaeExtracted == null) {

                    return;
                }

                Object remainder = freshMonitor.injectItems((IAEStack) iaeExtracted, Actionable.MODULATE, actionSource);


                if (remainder == null) {
                    Object actualExtracted = ChemicalHelper.extractChemicalFromHandler(handler, totalAmount, executeAction);
                    if (actualExtracted == null || ChemicalHelper.getChemicalAmount(actualExtracted) <= 0) {
                        return;
                    }
                    player.inventory.setItemStack(heldItem);
                    this.updateHeld(player);
                    return;
                }

                long notInsertedAmount = ((IAEStack) remainder).getStackSize();
                long insertedAmount = totalAmount - notInsertedAmount;

                if (insertedAmount <= 0) {
                    return;
                }

                Object actualExtracted = ChemicalHelper.extractChemicalFromHandler(handler, insertedAmount, executeAction);
                if (actualExtracted == null || ChemicalHelper.getChemicalAmount(actualExtracted) <= 0) {
                    return;
                }


                player.inventory.setItemStack(heldItem);
                this.updateHeld(player);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isWUT() {
        return guiObject.getItemStack().getItem() instanceof WUTItem;
    }


    @Override
    public boolean canInteractWith(PlayerEntity player) {
        ItemStack terminal = guiObject.getItemStack();
        if (terminal.isEmpty()) return false;

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (player.inventory.getStackInSlot(i) == terminal) return true;
        }

        if (CuriosHelper.CURIOS_PRESENT && CuriosHelper.isTerminalInCurios(player, terminal)) {
            return true;
        }

        return false;
    }

}