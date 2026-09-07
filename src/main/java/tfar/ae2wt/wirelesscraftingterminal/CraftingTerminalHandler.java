package tfar.ae2wt.wirelesscraftingterminal;

import appeng.api.config.Actionable;
import appeng.api.features.ILocatable;
import appeng.api.implementations.tiles.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.DimensionalCoord;
import appeng.core.Api;
import appeng.me.helpers.PlayerSource;
import appeng.tile.networking.WirelessTileEntity;
import appeng.util.item.AEItemStack;
import net.minecraftforge.items.IItemHandler;
import tfar.ae2wt.terminal.AbstractWirelessTerminalItem;
import tfar.ae2wt.util.CuriosHelper;
import tfar.ae2wt.wut.WUTItem;
import tfar.ae2wt.wut.WUTHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class CraftingTerminalHandler {

    private static final HashMap<UUID, CraftingTerminalHandler> players = new HashMap<>();
    private final PlayerEntity player;
    private ItemStack craftingTerminal = ItemStack.EMPTY;
    private ILocatable securityStation;
    private IGrid targetGrid;

    private CraftingTerminalHandler(PlayerEntity player) {
        this.player = player;
    }

    public static CraftingTerminalHandler getCraftingTerminalHandler(PlayerEntity player) {
        if(players.containsKey(player.getUniqueID())) return players.get(player.getUniqueID());
        CraftingTerminalHandler handler = new CraftingTerminalHandler(player);
        players.put(player.getUniqueID(), handler);
        return handler;
    }

    public ItemStack getCraftingTerminal() {
        PlayerInventory inv = player.inventory;


        if (!craftingTerminal.isEmpty()) {
            boolean valid = false;

            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.getStackInSlot(i) == craftingTerminal) {
                    valid = true;
                    break;
                }
            }



            ItemStack offhand = player.getHeldItemOffhand();
            if (matchesCraftingTerminal(offhand)) {
                return craftingTerminal = offhand;
            }

            if (!valid && CuriosHelper.CURIOS_PRESENT) {
                valid = CuriosHelper.isTerminalInCurios(player, craftingTerminal);
            }
            if (valid) {
                return craftingTerminal;
            }

            craftingTerminal = ItemStack.EMPTY;
        }

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack terminal = inv.getStackInSlot(i);
            if (matchesCraftingTerminal(terminal)) {
                return craftingTerminal = terminal;
            }
        }


        ItemStack offhand = player.getHeldItemOffhand();
        if (matchesCraftingTerminal(offhand)) {
            return craftingTerminal = offhand;
        }


        if (CuriosHelper.CURIOS_PRESENT) {
            ItemStack terminal = CuriosHelper.findTerminalOfType(player, "crafting");
            if (!terminal.isEmpty()) {
                return craftingTerminal = terminal;
            }
        }

        return ItemStack.EMPTY;
    }

    private boolean matchesCraftingTerminal(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof WCTItem ||
                (stack.getItem() instanceof WUTItem && WUTHandler.hasTerminal(stack, "crafting"));
    }


    public ILocatable getSecurityStation() {
        if(securityStation != null) return securityStation;
        final String unParsedKey = ((AbstractWirelessTerminalItem) craftingTerminal.getItem()).getEncryptionKey(craftingTerminal);
        if(unParsedKey.isEmpty()) return null;
        final long parsedKey = Long.parseLong(unParsedKey);
        return securityStation = Api.instance().registries().locatable().getLocatableBy(parsedKey);
    }

    public IGrid getTargetGrid() {
        if(getSecurityStation() == null) return targetGrid = null;
        final IGridNode n = ((IActionHost) securityStation).getActionableNode();

        if(n != null) {
            return targetGrid = n.getGrid();
        }
        return targetGrid = null;
    }

    private IWirelessAccessPoint myWap;
    private double sqRange = Double.MAX_VALUE;

    public boolean inRange() {
        sqRange = Double.MAX_VALUE;
        ItemStack terminal = getCraftingTerminal();
        if (terminal.isEmpty()) return false;

        IGrid grid = getTargetGrid();
        if (grid == null) return false;


        boolean hasInfiniteBooster = false;
        boolean hasCrossBooster = false;

        IMachineSet waps = grid.getMachines(WirelessTileEntity.class);
        for (IGridNode node : waps) {
            if (!node.isActive()) continue;
            IWirelessAccessPoint wap = (IWirelessAccessPoint) node.getMachine();
            if (!(wap instanceof WirelessTileEntity)) continue;

            WirelessTileEntity wte = (WirelessTileEntity) wap;
            IItemHandler inv = wte.getInternalInventory();
            if (inv == null) continue;

            ItemStack booster = inv.getStackInSlot(0);
            if (booster.isEmpty()) continue;

            if (AbstractWirelessTerminalItem.isCrossDimensionBooster(booster)) {
                hasCrossBooster = true;
                break;
            }
            if (AbstractWirelessTerminalItem.isInfiniteRangeBooster(booster)) {
                if (wap.getLocation().getWorld() == player.world) {
                    hasInfiniteBooster = true;
                }
            }
        }

        if (hasCrossBooster) return true;
        if (hasInfiniteBooster) return true;


        if (myWap != null && myWap.getGrid() != grid) {
            myWap = null;
        }
        if (myWap != null) {
            if (testWap(myWap)) return true;
            myWap = null;
        }
        for (IGridNode node : waps) {
            if (!node.isActive()) continue;
            IWirelessAccessPoint wap = (IWirelessAccessPoint) node.getMachine();
            if (testWap(wap)) {
                myWap = wap;
                return true;
            }
        }
        return false;
    }

    private boolean testWap(final IWirelessAccessPoint wap) {
        double rangeLimit = wap.getRange();
        rangeLimit *= rangeLimit;

        final DimensionalCoord dc = wap.getLocation();

        if(dc.getWorld() == player.world) {
            final double offX = dc.x - player.getPosX();
            final double offY = dc.y - player.getPosY();
            final double offZ = dc.z - player.getPosZ();

            final double r = offX * offX + offY * offY + offZ * offZ;
            if(r < rangeLimit && sqRange > r) {
                if(wap.isActive()) {
                    sqRange = r;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean insertItemIntoME(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack terminal = getCraftingTerminal();
        if (terminal.isEmpty()) return false;
        if (!inRange()) return false;

        final ILocatable securityStation = getSecurityStation();
        if (securityStation == null) return false;
        IGrid targetGrid = getTargetGrid();
        if (targetGrid == null) return false;

        IStorageGrid sg = targetGrid.getCache(IStorageGrid.class);
        IMEMonitor<IAEItemStack> itemStorage = sg.getInventory(Api.instance().storage().getStorageChannel(IItemStorageChannel.class));
        IAEItemStack leftover = itemStorage.injectItems(AEItemStack.fromItemStack(stack), Actionable.MODULATE, new PlayerSource(player, (IActionHost) securityStation));

        if (leftover == null || leftover.createItemStack().isEmpty()) {
            stack.setCount(0);
            return true;
        } else {
            stack.setCount(leftover.createItemStack().getCount());
            return false;
        }
    }
}