package tfar.ae2wt.wirelessfluidterminal;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.container.ContainerLocator;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.container.me.common.MEMonitorableContainer;
import appeng.core.AELog;
import appeng.core.Api;
import appeng.core.localization.PlayerMessages;
import appeng.fluids.util.AEFluidStack;
import appeng.fluids.util.FluidSoundHelper;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.network.NetworkHooks;
import tfar.ae2wt.WTConfig;
import tfar.ae2wt.init.Menus;
import tfar.ae2wt.terminal.IWirelessTerminalContainer;
import tfar.ae2wt.terminal.WTGuiObject;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.util.CuriosHelper;
import tfar.ae2wt.wut.WUTItem;


import javax.annotation.Nullable;

public class WirelessFluidTerminalContainer extends MEMonitorableContainer<IAEFluidStack> implements IWirelessTerminalContainer {

    @Override
    public ItemStack getTerminalStack() {
        return guiObject.getItemStack();
    }

    private final WTGuiObject guiObject;
    private double powerMultiplier = 1.0;
    private int ticks = 0;

    public WirelessFluidTerminalContainer(int id, PlayerInventory ip, ITerminalHost monitorable) {
        this(Menus.WIRELESS_FLUID_TERMINAL, id, ip, monitorable, true);
    }

    public WirelessFluidTerminalContainer(ContainerType<?> containerType, int id, PlayerInventory ip, ITerminalHost host, boolean bindInventory) {
        super(containerType, id, ip, host, bindInventory, Api.instance().storage().getStorageChannel(IFluidStorageChannel.class));
        this.guiObject = (WTGuiObject) host;
        int slotIndex = ((IInventorySlotAware) guiObject).getInventorySlot();
        if (slotIndex >= 0 && slotIndex < 36) {
            lockPlayerInventorySlot(slotIndex);
        }

    }

    public static void openServer(PlayerEntity player, ContainerLocator locator) {
        ItemStack it = player.inventory.getStackInSlot(locator.getItemIndex());
        WFluidTGuiObject accessInterface = new WFluidTGuiObject((AbstractWirelessTerminalItem) it.getItem(), it, player, locator.getItemIndex());
        if (locator.hasItemIndex()) {
            NetworkHooks.openGui((ServerPlayerEntity) player,
                    new tfar.ae2wt.wirelessfluidterminal.TermFactory(accessInterface, locator),
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

    public static WirelessFluidTerminalContainer openClient(int windowId, PlayerInventory inv, PacketBuffer data) {
        boolean hasLocator = data.readBoolean();
        PlayerEntity player = inv.player;
        if (hasLocator) {
            ContainerLocator locator = ContainerLocator.read(data);
            ItemStack it = player.inventory.getStackInSlot(locator.getItemIndex());
            WFluidTGuiObject host = new WFluidTGuiObject((AbstractWirelessTerminalItem) it.getItem(), it, player, locator.getItemIndex());
            return new WirelessFluidTerminalContainer(windowId, inv, host);
        } else {
            ItemStack stack = data.readItemStack();
            WFluidTGuiObject host = new WFluidTGuiObject((AbstractWirelessTerminalItem) stack.getItem(), stack, player, -1);
            return new WirelessFluidTerminalContainer(windowId, inv, host);
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
        } else {
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

    protected void handleNetworkInteraction(ServerPlayerEntity player, @Nullable IAEFluidStack stack, InventoryAction action) {
        if (action == InventoryAction.FILL_ITEM || action == InventoryAction.EMPTY_ITEM) {
            ItemStack held = player.inventory.getItemStack();
            if (held.getCount() == 1) {
                LazyOptional<IFluidHandlerItem> fhOpt = FluidUtil.getFluidHandler(held);
                if (fhOpt.isPresent()) {
                    IFluidHandlerItem fh = fhOpt.orElse(null);
                    IAEFluidStack notStorable;
                    int canFill;
                    IAEFluidStack pulled;
                    if (action == InventoryAction.FILL_ITEM && stack != null) {
                        stack.setStackSize(2147483647L);
                        int amountAllowed = fh.fill(stack.getFluidStack(), IFluidHandler.FluidAction.SIMULATE);
                        stack.setStackSize(amountAllowed);
                        notStorable = Platform.poweredExtraction(this.powerSource, this.monitor, stack, this.getActionSource(), Actionable.SIMULATE);
                        if (notStorable == null || notStorable.getStackSize() < 1L) {
                            return;
                        }

                        canFill = fh.fill(notStorable.getFluidStack(), IFluidHandler.FluidAction.SIMULATE);
                        if (canFill == 0) {
                            return;
                        }

                        stack.setStackSize(canFill);
                        pulled = Platform.poweredExtraction(this.powerSource, this.monitor, stack, this.getActionSource());


                        int used = fh.fill(pulled.getFluidStack(), IFluidHandler.FluidAction.EXECUTE);

                        player.inventory.setItemStack(fh.getContainer());
                        this.updateHeld(player);
                        FluidSoundHelper.playFillSound(player, pulled.getFluidStack());
                    } else if (action == InventoryAction.EMPTY_ITEM) {
                        FluidStack extract = fh.drain(2147483647, IFluidHandler.FluidAction.SIMULATE);
                        if (extract.isEmpty() || extract.getAmount() < 1) {
                            return;
                        }

                        notStorable = Platform.poweredInsert(this.powerSource, this.monitor, AEFluidStack.fromFluidStack(extract), this.getActionSource(), Actionable.SIMULATE);
                        if (notStorable != null && notStorable.getStackSize() > 0L) {
                            canFill = (int)((long)extract.getAmount() - notStorable.getStackSize());
                            FluidStack storable = fh.drain(canFill, IFluidHandler.FluidAction.SIMULATE);
                            if (storable.isEmpty() || storable.getAmount() == 0) {
                                return;
                            }

                            extract.setAmount(storable.getAmount());
                        }

                        FluidStack drained = fh.drain(extract, IFluidHandler.FluidAction.EXECUTE);
                        extract.setAmount(drained.getAmount());
                        pulled = Platform.poweredInsert(this.powerSource, this.monitor, AEFluidStack.fromFluidStack(extract), this.getActionSource());

                        player.inventory.setItemStack(fh.getContainer());
                        this.updateHeld(player);
                        FluidSoundHelper.playEmptySound(player, extract);
                    }

                }
            }
        }
    }
}
