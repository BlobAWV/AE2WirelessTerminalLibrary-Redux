package tfar.ae2wt.terminal;

import appeng.api.config.Actionable;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.IConfigManager;
import appeng.container.ContainerLocator;
import appeng.core.Api;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import appeng.util.ConfigManager;
import appeng.util.Platform;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tags.Tag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.ModList;


import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

public abstract class AbstractWirelessTerminalItem extends AEBasePoweredItem implements IWirelessTermHandler {

    private static final String MAGNET_INSTALLED_KEY = "magnetInstalled";
    private static final String MAGNET_ENABLED_KEY = "magnetEnabled";
    public static final String MAGNET_PICKUP_MODE_KEY = "magnetPickupMode";
    private static final String MAGNET_FILTER_MODE_KEY = "magnetFilterMode";
    private static final String MAGNET_NBT_MATCH_KEY = "magnetNbtMatch";
    private static final String MAGNET_FILTERS_KEY = "magnetFilters";

    public static boolean hasMagnetUpgrade(ItemStack stack) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return false;
        return stack.getOrCreateTag().getBoolean(MAGNET_INSTALLED_KEY);
    }

    public static void enableMagnetUpgrade(ItemStack stack) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return;
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putBoolean(MAGNET_INSTALLED_KEY, true);
        tag.putBoolean(MAGNET_ENABLED_KEY, true);
        tag.putString(MAGNET_PICKUP_MODE_KEY, "INVENTORY");
        tag.putString(MAGNET_FILTER_MODE_KEY, "WHITELIST");
        tag.putBoolean(MAGNET_NBT_MATCH_KEY, false);
        tag.put(MAGNET_FILTERS_KEY, new ListNBT());
    }

    public static List<ItemStack> getMagnetFiltersAsList(ItemStack stack) {
        List<ItemStack> list = new ArrayList<>();
        ListNBT filters = getMagnetFilters(stack);
        for (int i = 0; i < filters.size(); i++) {
            CompoundNBT tag = filters.getCompound(i);
            ItemStack item = ItemStack.read(tag);
            if (!item.isEmpty()) list.add(item);
        }
        return list;
    }

    public static boolean isMagnetEnabled(ItemStack stack) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return false;
        return stack.getOrCreateTag().getBoolean(MAGNET_ENABLED_KEY);
    }

    public static void setMagnetEnabled(ItemStack stack, boolean enabled) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return;
        stack.getOrCreateTag().putBoolean(MAGNET_ENABLED_KEY, enabled);
    }

    public static void setMagnetPickupMode(ItemStack stack, String mode) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return;
        stack.getOrCreateTag().putString(MAGNET_PICKUP_MODE_KEY, mode);
    }
    public static void setMagnetFilterMode(ItemStack stack, String mode) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return;
        stack.getOrCreateTag().putString(MAGNET_FILTER_MODE_KEY, mode);
    }

    public static String getMagnetFilterMode(ItemStack stack) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return "WHITELIST";
        String mode = stack.getOrCreateTag().getString(MAGNET_FILTER_MODE_KEY);
        return mode.isEmpty() ? "WHITELIST" : mode;
    }

    public static void setMagnetNbtMatch(ItemStack stack, boolean match) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return;
        stack.getOrCreateTag().putBoolean(MAGNET_NBT_MATCH_KEY, match);
    }

    public static boolean getMagnetNbtMatch(ItemStack stack) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return false;
        return stack.getOrCreateTag().getBoolean(MAGNET_NBT_MATCH_KEY);
    }

    public static ListNBT getMagnetFilters(ItemStack stack) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return new ListNBT();
        return stack.getOrCreateTag().getList(MAGNET_FILTERS_KEY, Constants.NBT.TAG_COMPOUND);
    }

    public static void setMagnetFilters(ItemStack stack, ListNBT filters) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return;
        stack.getOrCreateTag().put(MAGNET_FILTERS_KEY, filters);
    }

    public static String getMagnetPickupMode(ItemStack stack) {
        if (!(stack.getItem() instanceof AbstractWirelessTerminalItem)) return "INVENTORY";
        String mode = stack.getOrCreateTag().getString(MAGNET_PICKUP_MODE_KEY);
        return mode.isEmpty() ? "INVENTORY" : mode;
    }

    private static Tag<Item> getInfiniteRangeTag() {
        net.minecraft.tags.ITagCollection<Item> tagCollection = ItemTags.getCollection();
        if (tagCollection == null) return null; // presumably this is always false
        return (Tag<Item>) tagCollection.getTagByID(new ResourceLocation("ae2wtlib", "infinite_range"));
    }

    private static Tag<Item> getCrossDimensionTag() {
        net.minecraft.tags.ITagCollection<Item> tagCollection = ItemTags.getCollection();
        if (tagCollection == null) return null; // presumably this is always false
        return (Tag<Item>) tagCollection.getTagByID(new ResourceLocation("ae2wtlib", "cross_dimensional"));
    }

    public static boolean isInfiniteRangeBooster(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Tag<Item> tag = getInfiniteRangeTag();
        return tag != null && tag.contains(stack.getItem());
    }

    public static boolean isCrossDimensionBooster(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Tag<Item> tag = getCrossDimensionTag();
        return tag != null && tag.contains(stack.getItem());
    }

    public static boolean isFluidCraftPresent() {
        return ModList.get().isLoaded("fluidcraft");
    }

    public AbstractWirelessTerminalItem(DoubleSupplier powerCapacity, Properties props) {
        super(powerCapacity, props);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final World w, final PlayerEntity player, final Hand hand) {
        openWirelessTerminalGui(player.getHeldItem(hand), player, hand);
        return new ActionResult<>(ActionResultType.SUCCESS, player.getHeldItem(hand));
    }

    private void openWirelessTerminalGui(ItemStack item, PlayerEntity player, Hand hand) {
        if (Platform.isClient()) {
            return;
        }

        final String unparsedKey = getEncryptionKey(item);
        if (unparsedKey.isEmpty()) {
            player.sendMessage(PlayerMessages.DeviceNotLinked.get(), Util.DUMMY_UUID);
            return;
        }

        final long parsedKey = Long.parseLong(unparsedKey);
        final ILocatable securityStation = Api.instance().registries().locatable().getLocatableBy(parsedKey);
        if (securityStation == null) {
            player.sendMessage(PlayerMessages.StationCanNotBeLocated.get(), Util.DUMMY_UUID);
            return;
        }

        if (hasPower(player, 0.5, item)) {
            open(player, ContainerLocator.forHand(player, hand));
        } else {
            player.sendMessage(PlayerMessages.DeviceNotPowered.get(), Util.DUMMY_UUID);
        }
    }

    public abstract void open(final PlayerEntity player, final ContainerLocator locator);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(final ItemStack stack, final World world, final List<ITextComponent> lines, final ITooltipFlag advancedTooltips) {
        super.addInformation(stack, world, lines, advancedTooltips);

        if (stack.hasTag()) {
            final CompoundNBT tag = stack.getTag();
            if (tag != null) {
                final String encKey = tag.getString("encryptionKey");

                if (encKey.isEmpty()) {
                    lines.add(GuiText.Unlinked.text());
                } else {
                    lines.add(GuiText.Linked.text());
                }
            }
        } else {
            lines.add(new TranslationTextComponent("AppEng.GuiITooltip.Unlinked"));
        }
    }

    @Override
    public boolean canHandle(ItemStack is) {
        return is.getItem() instanceof AbstractWirelessTerminalItem;
    }

    @Override
    public boolean usePower(PlayerEntity player, double amount, ItemStack is) {
        if (player.abilities.isCreativeMode) {
            return false;
        }
        return extractAEPower(is, amount, Actionable.MODULATE) >= amount - 0.5;
    }

    @Override
    public boolean hasPower(PlayerEntity player, double amount, ItemStack is) {
        return getAECurrentPower(is) >= amount;
    }

    @Override
    public IConfigManager getConfigManager(ItemStack is) {
        final ConfigManager out = new ConfigManager((manager, settingName, newValue) -> {
            final CompoundNBT data = is.getOrCreateTag();
            manager.writeToNBT(data);
        });

        out.registerSetting(appeng.api.config.Settings.SORT_BY, SortOrder.NAME);
        out.registerSetting(appeng.api.config.Settings.VIEW_MODE, ViewItems.ALL);
        out.registerSetting(appeng.api.config.Settings.SORT_DIRECTION, SortDir.ASCENDING);

        out.readFromNBT(is.getOrCreateTag().copy());
        return out;
    }

    @Override
    public String getEncryptionKey(ItemStack item) {
        final CompoundNBT tag = item.getOrCreateTag();
        return tag.getString("encryptionKey");
    }

    @Override
    public void setEncryptionKey(ItemStack item, String encKey, String name) {
        final CompoundNBT tag = item.getOrCreateTag();
        tag.putString("encryptionKey", encKey);
        tag.putString("name", name);
    }

    /**
     * get a previously stored {@link ItemStack} from a WirelessTerminal
     *
     * @param hostItem the Terminal to load from
     * @param slot     the location where the item is stored
     * @return the stored Item or {@link ItemStack}.EMPTY if it wasn't found
     */
    public static ItemStack getSavedSlot(ItemStack hostItem, SlotType slot) {
        return getSavedSlot(hostItem, slot, 0);
    }

    /**
     * get a previously stored {@link ItemStack} from a WirelessTerminal
     *
     * @param hostItem the Terminal to load from
     * @param slot     the location where the item is stored
     * @return the stored Item or {@link ItemStack}.EMPTY if it wasn't found
     */
    public static ItemStack getSavedSlot(ItemStack hostItem, SlotType slot, int index) {
        return ItemStack.read(hostItem.getOrCreateTag().getCompound(slot.toString() + index));
    }

    /**
     * store an {@link ItemStack} in a WirelessTerminal
     * this will overwrite any previously existing tags in slot
     *
     * @param hostItem  the Terminal to store in
     * @param savedItem the item to store
     * @param slot      the location where the stored item will be
     */
    public static void setSavedSlot(ItemStack hostItem, ItemStack savedItem, SlotType slot) {
        setSavedSlot(hostItem, savedItem, slot, 0);
    }

    /**
     * store an {@link ItemStack} in a WirelessTerminal
     * this will overwrite any previously existing tags in slot
     *
     * @param hostItem  the Terminal to store in
     * @param savedItem the item to store
     * @param slot      the location where the stored item will be
     */
    public static void setSavedSlot(ItemStack hostItem, ItemStack savedItem, SlotType slot, int index) {
        if (!(hostItem.getItem() instanceof AbstractWirelessTerminalItem)) return;
        CompoundNBT wctTag = hostItem.getOrCreateTag();
        if (savedItem.isEmpty()) {
            wctTag.remove(slot.toString() + index);
        } else {
            wctTag.put(slot.toString() + index, savedItem.write(new CompoundNBT()));
        }
    }

    /**
     * get a previously stored boolean from a WirelessTerminal
     *
     * @param hostItem the Terminal to load from
     * @return the boolean or false if it wasn't found
     */
    public static boolean getBoolean(ItemStack hostItem, String key) {
        if (!(hostItem.getItem() instanceof AbstractWirelessTerminalItem)) return false;
        return hostItem.getOrCreateTag().getBoolean(key);
    }

    /**
     * store a boolean in a WirelessTerminal
     * this will overwrite any previously existing tags in slot
     *
     * @param hostItem the Terminal to store in
     * @param b        the boolean to store
     * @param key      the location where the stored item will be
     */
    public static void setBoolean(ItemStack hostItem, boolean b, String key) {
        if (!(hostItem.getItem() instanceof AbstractWirelessTerminalItem)) return;
        CompoundNBT wctTag = hostItem.getOrCreateTag();
        wctTag.putBoolean(key, b);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

}