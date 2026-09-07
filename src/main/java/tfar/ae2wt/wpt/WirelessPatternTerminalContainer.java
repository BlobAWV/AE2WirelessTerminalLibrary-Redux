package tfar.ae2wt.wpt;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.ICraftingHelper;
import appeng.api.networking.IGridNode;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.ContainerLocator;
import appeng.container.ContainerNull;
import appeng.container.SlotSemantic;
import appeng.container.guisync.GuiSync;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.container.me.items.ItemTerminalContainer;
import appeng.container.slot.*;
import appeng.core.Api;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.IContainerCraftingPacket;
import appeng.helpers.InventoryAction;
import appeng.items.storage.ViewCellItem;
import appeng.me.helpers.MachineSource;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorItemHandler;
import appeng.util.inv.WrapperCursorItemHandler;
import appeng.util.item.AEItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.CraftingResultSlot;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import tfar.ae2wt.WTConfig;
import tfar.ae2wt.init.Menus;
import tfar.ae2wt.mixin.ContainerAccess;
import tfar.ae2wt.net.PacketHandler;
import tfar.ae2wt.net.server.C2STogglePatternCraftingModePacket;
import tfar.ae2wt.net.server.C2STogglePatternFluidModePacket;
import tfar.ae2wt.net.server.C2STogglePatternSubsitutionPacket;
import tfar.ae2wt.net.server.C2SToggleFluidConversionPacket;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.terminal.IWirelessTerminalContainer;
import tfar.ae2wt.terminal.WTInventoryHandler;
import tfar.ae2wt.util.CuriosHelper;
import tfar.ae2wt.util.FluidCraftHelper;
import tfar.ae2wt.wut.WUTItem;

import net.minecraftforge.fml.ModList;



public class WirelessPatternTerminalContainer extends ItemTerminalContainer implements IOptionalSlotHost, IContainerCraftingPacket, IWirelessTerminalContainer {

    @Override
    public ItemStack getTerminalStack() {
        return wptGUIObject.getItemStack();
    }

    public static WirelessPatternTerminalContainer openClient(int windowId, PlayerInventory inv, PacketBuffer data) {
        boolean hasLocator = data.readBoolean();
        PlayerEntity player = inv.player;
        if (hasLocator) {
            ContainerLocator locator = ContainerLocator.read(data);
            ItemStack it = player.inventory.getStackInSlot(locator.getItemIndex());
            WPTGuiObject host = new WPTGuiObject((AbstractWirelessTerminalItem) it.getItem(), it, player, locator.getItemIndex());
            return new WirelessPatternTerminalContainer(windowId, inv, host);
        } else {
            ItemStack stack = data.readItemStack();
            WPTGuiObject host = new WPTGuiObject((AbstractWirelessTerminalItem) stack.getItem(), stack, player, -1);
            return new WirelessPatternTerminalContainer(windowId, inv, host);
        }
    }

    private final FakeCraftingMatrixSlot[] craftingSlots = new FakeCraftingMatrixSlot[9];
    private final OptionalFakeSlot[] processingOutputSlots = new OptionalFakeSlot[3];
    private ICraftingRecipe currentRecipe;
    private final AppEngInternalInventory cOut = new AppEngInternalInventory(null, 1);
    private final AppEngInternalInventory craftingGridInv;
    private final WirelessPatternTermSlot craftSlot;
    private final RestrictedInputSlot blankPatternSlot;
    private final RestrictedInputSlot encodedPatternSlot;
    private final ICraftingHelper craftingHelper = Api.INSTANCE.crafting();


    public static void openServer(PlayerEntity player, ContainerLocator locator) {
        ItemStack it = player.inventory.getStackInSlot(locator.getItemIndex());
        WPTGuiObject accessInterface = new WPTGuiObject((AbstractWirelessTerminalItem) it.getItem(), it, player, locator.getItemIndex());
        if (locator.hasItemIndex()) {
            NetworkHooks.openGui((ServerPlayerEntity) player,
                    new tfar.ae2wt.wpt.TermFactory(accessInterface, locator),
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

    private final WPTGuiObject wptGUIObject;

    @GuiSync(97)
    public boolean craftingMode;
    @GuiSync(96)
    public boolean substitute;
    @GuiSync(101)
    public boolean fluidMode;
    private static final boolean FLUID_CRAFT_PRESENT = ModList.get().isLoaded("ae2fc");
    @GuiSync(102)
    public boolean fluidConversionEnabled = true;

    public WirelessPatternTerminalContainer(int id, final PlayerInventory ip, final WPTGuiObject gui) {
        super(Menus.PATTERN, id, ip, gui, false);
        wptGUIObject = gui;
        wptGUIObject.setContainer(this);

        if (isServer()) {
            this.fluidMode = gui.isFluidMode();
            this.craftingMode = gui.isCraftingRecipe();
            this.substitute = gui.isSubstitution();
            this.fluidConversionEnabled = gui.isFluidConversionEnabled();
            gui.setCraftingMode(this.craftingMode);
            gui.setFluidMode(this.fluidMode);
            gui.setSubstitution(this.substitute);
            gui.setFluidConversionEnabled(this.fluidConversionEnabled);
        }

        int slotIndex = ((IInventorySlotAware) wptGUIObject).getInventorySlot();
        if (slotIndex >= 0 && slotIndex < 36) {
            lockPlayerInventorySlot(slotIndex);
        }
        final AppEngInternalInventory patternInv = getPatternTerminal().getInventoryByName("pattern");
        final AppEngInternalInventory output = getPatternTerminal().getInventoryByName("output");

        final WTInventoryHandler fixedWPTInv = new WTInventoryHandler(getPlayerInventory(), wptGUIObject.getItemStack(), this);

        craftingGridInv = getPatternTerminal().getInventoryByName("crafting");

        for (int y = 0; y < 9; y++) {
            addSlot(craftingSlots[y] = new FakeCraftingMatrixSlot(craftingGridInv, y), SlotSemantic.CRAFTING_GRID);
        }

        addSlot(craftSlot = new WirelessPatternTermSlot(ip.player, getActionSource(), powerSource, gui, craftingGridInv,
                        patternInv, this, 2, this)
                , SlotSemantic.CRAFTING_RESULT);
        craftSlot.setIcon(null);

        for (int y = 0; y < 3; y++) {
            this.addSlot(this.processingOutputSlots[y] = new PatternOutputsSlot(output, this, y, 1), SlotSemantic.PROCESSING_RESULT);
            this.processingOutputSlots[y].setRenderDisabled(false);
            this.processingOutputSlots[y].setIcon(null);
        }

        this.addSlot(this.blankPatternSlot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.BLANK_PATTERN, patternInv, 0), SlotSemantic.BLANK_PATTERN);
        this.addSlot(this.encodedPatternSlot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, patternInv, 1), SlotSemantic.ENCODED_PATTERN);
        this.encodedPatternSlot.setStackLimit(1);
        this.createPlayerInventorySlots(ip);

        if (isClient()) {
            fluidMode = AbstractWirelessTerminalItem.getBoolean(wptGUIObject.getItemStack(), "fluidMode");
            craftingMode = AbstractWirelessTerminalItem.getBoolean(wptGUIObject.getItemStack(), "craftingMode");
            substitute = AbstractWirelessTerminalItem.getBoolean(wptGUIObject.getItemStack(), "substitute");
            fluidConversionEnabled = AbstractWirelessTerminalItem.getBoolean(wptGUIObject.getItemStack(), "fluidConversion");
            if (FLUID_CRAFT_PRESENT) {
                PacketHandler.INSTANCE.sendToServer(new C2STogglePatternFluidModePacket(fluidMode));
                PacketHandler.INSTANCE.sendToServer(new C2SToggleFluidConversionPacket(fluidConversionEnabled));
            }

            PacketHandler.INSTANCE.sendToServer(new C2STogglePatternCraftingModePacket(craftingMode));

            PacketHandler.INSTANCE.sendToServer(new C2STogglePatternSubsitutionPacket(substitute));


        }
    }

    private int ticks = 0;

    @Override
    public void detectAndSendChanges() {
        if (isClient()) return;
        super.detectAndSendChanges();

        if (!wptGUIObject.rangeCheck()) {
            if (isValidContainer()) {
                getPlayerInventory().player.sendMessage(PlayerMessages.OutOfRange.get(), Util.DUMMY_UUID);
                getPlayerInventory().player.closeScreen();
            }
            setValidContainer(false);
        } else {
            double powerMultiplier = WTConfig.getPowerMultiplier(wptGUIObject.getRange(), wptGUIObject.isOutOfRange());
            ticks++;
            if (ticks > 10) {
                wptGUIObject.extractAEPower((powerMultiplier) * ticks, Actionable.MODULATE, PowerMultiplier.CONFIG);
                ticks = 0;
            }

            if (wptGUIObject.extractAEPower(1, Actionable.SIMULATE, PowerMultiplier.ONE) == 0) {
                if (isValidContainer()) {
                    getPlayerInventory().player.sendMessage(PlayerMessages.DeviceNotPowered.get(), Util.DUMMY_UUID);
                    getPlayerInventory().player.closeScreen();
                }
                setValidContainer(false);
            }
        }

        if (fluidMode != getPatternTerminal().isFluidMode()) {
            setFluidMode(getPatternTerminal().isFluidMode());
        }

        if (isCraftingMode() != getPatternTerminal().isCraftingRecipe()) {
            setCraftingMode(getPatternTerminal().isCraftingRecipe());
        }

        if (substitute != getPatternTerminal().isSubstitution()) {
            substitute = getPatternTerminal().isSubstitution();
            AbstractWirelessTerminalItem.setBoolean(wptGUIObject.getItemStack(), substitute, "substitute");
        }
    }

    @Override
    public void onSlotChange(final Slot s) {
        if (s == encodedPatternSlot && isServer()) {
            for (final IContainerListener listener : ((ContainerAccess) this).getListeners()) {
                for (int i = 0; i < inventorySlots.size(); i++) {
                    Slot slot = inventorySlots.get(i);
                    if (slot instanceof OptionalFakeSlot || slot instanceof FakeCraftingMatrixSlot)
                        listener.sendSlotContents(this, i, slot.getStack());
                }
                if (listener instanceof ServerPlayerEntity)
                    ((ServerPlayerEntity) listener).isChangingQuantityOnly = false;
            }
            detectAndSendChanges();
        }

        if (s == craftSlot && isClient()) getAndUpdateOutput();

        if (isClient() && isCraftingMode()) {
            for (Slot slot : craftingSlots) if (s == slot) getAndUpdateOutput();
            for (Slot slot : processingOutputSlots) if (s == slot) getAndUpdateOutput();
        }

    }

    /**
     * Callback for when the crafting matrix is changed.
     */

    @Override
    public boolean canInteractWith(PlayerEntity player) {
        ItemStack terminal = wptGUIObject.getItemStack();
        if (terminal.isEmpty()) return false;


        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (player.inventory.getStackInSlot(i) == terminal) return true;
        }


        if (CuriosHelper.CURIOS_PRESENT && CuriosHelper.isTerminalInCurios(player, terminal)) {
            return true;
        }

        return false;
    }

    public void encode() {
        if (isFluidMode()) {
            encodeFluidPattern();
        } else {
            encodeItemPattern();
        }
    }

    public void encodeItemPattern() {
        ItemStack output = encodedPatternSlot.getStack();

        final ItemStack[] in = getInputs();
        final ItemStack[] out = getOutputs();

        // if there is no input, this would be silly.
        if (in == null || out == null || isCraftingMode() && currentRecipe == null) return;

        ItemStack existingOutput = encodedPatternSlot.getStack();
        boolean hasExistingPattern = !existingOutput.isEmpty() && craftingHelper.isEncodedPattern(existingOutput);

        if (!hasExistingPattern) {
            ItemStack blank = blankPatternSlot.getStack();
            if (blank.isEmpty() || !isPattern(blank)) return;

            blank.setCount(blank.getCount() - 1);
            if (blank.getCount() == 0) {
                blankPatternSlot.putStack(ItemStack.EMPTY);
            }
            else {
                blankPatternSlot.putStack(blank);
            }

            existingOutput = null;
        } else {
        }

        ItemStack newPattern = isCraftingMode() ?
                craftingHelper.encodeCraftingPattern(existingOutput, currentRecipe, in, out[0], isSubstitute()) :
                craftingHelper.encodeProcessingPattern(existingOutput, in, out);

        encodedPatternSlot.putStack(newPattern);

    }

    private void encodeFluidPattern() {
        if (!FluidCraftHelper.PRESENT) return;
        ItemStack[] rawInputs = getInputs();
        ItemStack[] rawOutputs = getOutputs();
        if (rawInputs == null || rawOutputs == null) return;
        if (isCraftingMode()) return;


        ItemStack[] convertedInputs = new ItemStack[rawInputs.length];
        for (int i = 0; i < rawInputs.length; i++) {
            convertedInputs[i] = wptGUIObject.convertToFluidPacket(rawInputs[i]);

            craftingSlots[i].putStack(convertedInputs[i]);
        }

        ItemStack[] convertedOutputs = new ItemStack[rawOutputs.length];
        for (int i = 0; i < rawOutputs.length; i++) {
            convertedOutputs[i] = wptGUIObject.convertToFluidPacket(rawOutputs[i]);
            processingOutputSlots[i].putStack(convertedOutputs[i]);
        }


        boolean hasInput = false, hasOutput = false;
        for (ItemStack stack : convertedInputs) {
            if (!stack.isEmpty()) { hasInput = true; break; }
        }
        for (ItemStack stack : convertedOutputs) {
            if (!stack.isEmpty()) { hasOutput = true; break; }
        }
        if (!hasInput || !hasOutput) return;

        ItemStack existingOutput = encodedPatternSlot.getStack();
        boolean hasExistingPattern = !existingOutput.isEmpty() && FluidCraftHelper.isFluidEncodedPattern(existingOutput);

        if (!hasExistingPattern) {
            ItemStack blank = blankPatternSlot.getStack();
            if (blank.isEmpty() || !isPattern(blank)) return;
            blank.setCount(blank.getCount() - 1);
            if (blank.getCount() == 0) {
                blankPatternSlot.putStack(ItemStack.EMPTY);
            } else {
                blankPatternSlot.putStack(blank);
            }
            existingOutput = null;
        }

        ItemStack fluidPattern = FluidCraftHelper.encodeFluidPattern(convertedInputs, convertedOutputs);
        encodedPatternSlot.putStack(fluidPattern);
    }

    private ItemStack[] getInputs() {
        final ItemStack[] input = new ItemStack[9];
        boolean hasValue = false;

        for (int x = 0; x < craftingSlots.length; x++) {
            input[x] = craftingSlots[x].getStack();
            if (!input[x].isEmpty()) hasValue = true;
        }

        if (hasValue) return input;
        return null;
    }

    private ItemStack[] getOutputs() {
        if (isCraftingMode()) {
            final ItemStack out = getAndUpdateOutput();
            if (!out.isEmpty() && out.getCount() > 0) return new ItemStack[]{out};
        } else {
            boolean hasValue = false;
            final ItemStack[] list = new ItemStack[3];

            for (int i = 0; i < processingOutputSlots.length; i++) {
                final ItemStack out = processingOutputSlots[i].getStack();
                list[i] = out;
                if (!out.isEmpty()) hasValue = true;
            }
            if (hasValue) return list;
        }

        return null;
    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        if (name.equals("player")) return new InvWrapper(getPlayerInventory());
        return getPatternTerminal().getInventoryByName(name);
    }

    private boolean isPattern(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (FluidCraftHelper.PRESENT && FluidCraftHelper.isFluidEncodedPattern(stack)) return true;
        return craftingHelper.isEncodedPattern(stack) || Api.instance().definitions().materials().blankPattern().isSameAs(stack);
    }

    @Override
    public boolean isSlotEnabled(final int idx) {
        if (idx == 1) return isServer() ? !getPatternTerminal().isCraftingRecipe() : !isCraftingMode();
        else if (idx == 2) return isServer() ? getPatternTerminal().isCraftingRecipe() : isCraftingMode();
        else return false;
    }

    public void doAction(ServerPlayerEntity player, InventoryAction action, int slotId, long id) {
        if (isFluidMode() && fluidConversionEnabled && !isCraftingMode()) {
            if (slotId < 0 || slotId >= inventorySlots.size()) {
                super.doAction(player, action, slotId, id);
                return;
            }
            Slot slot = getSlot(slotId);
            ItemStack stack = player.inventory.getItemStack();
            if ((slot instanceof FakeCraftingMatrixSlot || slot instanceof PatternOutputsSlot) && !stack.isEmpty()) {
                FluidStack fluid = FluidCraftHelper.getFluidFromItem(stack);
                if (!fluid.isEmpty()) {
                    switch (action) {
                        case PICKUP_OR_SET_DOWN:
                            slot.putStack(FluidCraftHelper.newFluidPacket(fluid));
                            break;
                        case SPLIT_OR_PLACE_SINGLE:
                            FluidStack origin = FluidCraftHelper.getFluidFromPacket(slot.getStack());
                            if (!fluid.isEmpty() && fluid.equals(origin)) {
                                fluid.grow(origin.getAmount());
                                if (fluid.getAmount() <= 0) fluid = FluidStack.EMPTY;
                            }
                            slot.putStack(FluidCraftHelper.newFluidPacket(fluid));
                            break;
                    }
                    if (fluid.isEmpty()) {
                        super.doAction(player, action, slotId, id);
                        return;
                    }
                    return;
                }
                if (action == InventoryAction.SPLIT_OR_PLACE_SINGLE) {
                    if (stack.isEmpty() && !slot.getStack().isEmpty()) {
                        fluid = FluidCraftHelper.getFluidFromPacket(slot.getStack());
                        if (!fluid.isEmpty() && fluid.getAmount() - 1000 >= 1) {
                            fluid.shrink(1000);
                            slot.putStack(FluidCraftHelper.newFluidPacket(fluid));
                        }
                    }
                }
            }
        }
        super.doAction(player, action, slotId, id);
    }

    public void craftOrGetItem(final IAEItemStack slotItem, final boolean shift, final IAEItemStack[] pattern) {
        if (slotItem != null && this.monitor != null) {
            final IAEItemStack out = slotItem.copy();
            InventoryAdaptor inv = new AdaptorItemHandler(
                    new WrapperCursorItemHandler(this.getPlayerInventory().player.inventory));
            final InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(this.getPlayerInventory().player);

            if (shift) {
                inv = playerInv;
            }

            if (!inv.simulateAdd(out.createItemStack()).isEmpty()) {
                return;
            }

            final IAEItemStack extracted = Platform.poweredExtraction(this.powerSource, this.monitor,
                    out, this.getActionSource());
            final PlayerEntity p = this.getPlayerInventory().player;

            if (extracted != null) {
                inv.addItems(extracted.createItemStack());
                if (p instanceof ServerPlayerEntity) {
                    this.updateHeld((ServerPlayerEntity) p);
                }
                this.detectAndSendChanges();
                return;
            }

            final CraftingInventory ic = new CraftingInventory(new ContainerNull(), 3, 3);
            final CraftingInventory real = new CraftingInventory(new ContainerNull(), 3, 3);

            for (int x = 0; x < 9; x++) {
                ic.setInventorySlotContents(x, pattern[x] == null ? ItemStack.EMPTY
                        : pattern[x].createItemStack());
            }

            final IRecipe<CraftingInventory> r = p.world.getRecipeManager().getRecipe(IRecipeType.CRAFTING, ic, p.world)
                    .orElse(null);

            if (r == null) {
                return;
            }

            final IMEMonitor<IAEItemStack> storage = this.getPatternTerminal()
                    .getInventory(Api.instance().storage().getStorageChannel(IItemStorageChannel.class));
            final IItemList<IAEItemStack> all = storage.getStorageList();

            final ItemStack is = r.getCraftingResult(ic);

            for (int x = 0; x < ic.getSizeInventory(); x++) {
                if (!ic.getStackInSlot(x).isEmpty()) {
                    final ItemStack pulled = Platform.extractItemsByRecipe(this.powerSource,
                            this.getActionSource(), storage, p.world, r, is, ic, ic.getStackInSlot(x), x, all,
                            Actionable.MODULATE, ViewCellItem.createFilter(this.getViewCells()));
                    real.setInventorySlotContents(x, pulled);
                }
            }

            final IRecipe<CraftingInventory> rr = p.world.getRecipeManager()
                    .getRecipe(IRecipeType.CRAFTING, real, p.world).orElse(null);

            if (rr == r && Platform.itemComparisons().isSameItem(rr.getCraftingResult(real), is)) {
                final CraftResultInventory craftingResult = new CraftResultInventory();
                craftingResult.setRecipeUsed(rr);

                final CraftingResultSlot sc = new CraftingResultSlot(p, real, craftingResult, 0, 0, 0);
                sc.onTake(p, is);

                for (int x = 0; x < real.getSizeInventory(); x++) {
                    final ItemStack failed = playerInv.addItems(real.getStackInSlot(x));

                    if (!failed.isEmpty()) {
                        p.dropItem(failed, false);
                    }
                }

                inv.addItems(is);
                if (p instanceof ServerPlayerEntity) {
                    this.updateHeld((ServerPlayerEntity) p);
                }
                this.detectAndSendChanges();
            } else {
                for (int x = 0; x < real.getSizeInventory(); x++) {
                    final ItemStack failed = real.getStackInSlot(x);
                    if (!failed.isEmpty()) {
                        this.monitor.injectItems(AEItemStack.fromItemStack(failed), Actionable.MODULATE,
                                new MachineSource(this.getPatternTerminal()));
                    }
                }
            }
        }
    }

    public ItemStack getAndUpdateOutput() {
        final World world = getPlayerInventory().player.world;
        final CraftingInventory ic = new CraftingInventory(this, 3, 3);

        for (int x = 0; x < ic.getSizeInventory(); x++)
            ic.setInventorySlotContents(x, craftingGridInv.getStackInSlot(x));

        if (currentRecipe == null || !currentRecipe.matches(ic, world))
            currentRecipe = world.getRecipeManager().getRecipe(IRecipeType.CRAFTING, ic, world).orElse(null);

        final ItemStack is;

        if (currentRecipe == null) is = ItemStack.EMPTY;
        else is = currentRecipe.getCraftingResult(ic);

        cOut.setStackInSlot(0, is);
        craftSlot.setDisplayedCraftingOutput(is);
        return is;
    }

    @Override
    public boolean useRealItems() {
        return false;
    }

    public WPTGuiObject getPatternTerminal() {
        return wptGUIObject;
    }

    private boolean isSubstitute() {
        return substitute;
    }

    public boolean isCraftingMode() {
        return craftingMode;
    }

    public void setCraftingMode(final boolean craftingMode) {
        if (craftingMode != this.craftingMode) {
            this.craftingMode = craftingMode;
            AbstractWirelessTerminalItem.setBoolean(wptGUIObject.getItemStack(), craftingMode, "craftingMode");

        }
    }

    public void setFluidMode(boolean mode) {
        if (FLUID_CRAFT_PRESENT && mode != fluidMode) {
            this.fluidMode = mode;
            if (mode) {
                setCraftingMode(false);
            }
            AbstractWirelessTerminalItem.setBoolean(wptGUIObject.getItemStack(), mode, "fluidMode");
            getPatternTerminal().setFluidMode(mode);
            detectAndSendChanges();
        }
    }

    public boolean isFluidMode() {
        return FLUID_CRAFT_PRESENT && fluidMode;
    }

    public void clearPattern() {
        for (final Slot s : craftingSlots) s.putStack(ItemStack.EMPTY);
        for (final Slot s : processingOutputSlots) s.putStack(ItemStack.EMPTY);

        detectAndSendChanges();
        getAndUpdateOutput();
    }

    @Override
    public IGridNode getNetworkNode() {
        return wptGUIObject.getActionableNode();
    }

    public boolean isWUT() {
        return wptGUIObject.getItemStack().getItem() instanceof WUTItem;
    }

    public FakeCraftingMatrixSlot getCraftingGridSlot(int index) {
        return craftingSlots[index];
    }

    public OptionalFakeSlot getOutputSlot(int index) {
        return processingOutputSlots[index];
    }

    public PatternTermSlot getCraftSlot() {
        return craftSlot;
    }

    public void setFluidConversion(boolean enabled) {
        if (enabled != this.fluidConversionEnabled) {
            this.fluidConversionEnabled = enabled;
            AbstractWirelessTerminalItem.setBoolean(wptGUIObject.getItemStack(), enabled, "fluidConversion");
            wptGUIObject.setFluidConversionEnabled(enabled);
            detectAndSendChanges();
        }
    }
}