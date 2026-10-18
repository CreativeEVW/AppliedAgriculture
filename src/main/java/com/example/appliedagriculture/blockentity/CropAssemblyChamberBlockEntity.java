package com.example.appliedagriculture.blockentity;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.config.Setting;
import appeng.api.config.YesNo;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.blockentity.powersink.IExternalPowerSink;
import appeng.core.definitions.AEItems;
import appeng.helpers.ForgeEnergyAdapter;
import appeng.me.energy.StoredEnergyAmount;
import appeng.me.helpers.MachineSource;
import appeng.util.ConfigManager;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.example.appliedagriculture.api.AASettings;
import com.example.appliedagriculture.block.CropAssemblyChamberBlock;
import com.example.appliedagriculture.menu.CropAssemblyChamberMenu;
import com.example.appliedagriculture.registration.ModBlocks;
import com.example.appliedagriculture.registration.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CropAssemblyChamberBlockEntity extends AENetworkedBlockEntity
        implements IAEPowerStorage, IExternalPowerSink, IGridTickable, MenuProvider, IUpgradeableObject,
        IConfigurableObject, IOutputSideConfigurable, InternalInventoryHost, IFluidHandler {

    private static final int BASE_INTERVAL = 200;                 // 10s per operation
    private static final long POWER_COST = 200;                   // AE per operation
    private static final long WATER_COST = AEFluidKey.AMOUNT_BUCKET; // 1 bucket per operation
    public static final int MAX_UPGRADE_SLOTS = 8;

    /** Internal water tank: 200 buckets, water input only, consumed before network water. */
    private static final int WATER_CAPACITY = 200 * AEFluidKey.AMOUNT_BUCKET;
    private int storedWater = 0;

    private final StoredEnergyAmount stored;
    public final AppEngInternalInventory seedSlot = new AppEngInternalInventory(this, 1);
    private final IUpgradeInventory upgrades =
            UpgradeInventories.forMachine(ModBlocks.CROP_ASSEMBLY_CHAMBER, MAX_UPGRADE_SLOTS, this::onUpgradesChanged);
    private boolean isPowered = false;

    /** Storage mode: ME_EXPORT = YES inserts products into the ME network, NO pushes them to adjacent containers. */
    private final ConfigManager configManager = new ConfigManager(this::onConfigChanged);

    /** Sides products are pushed to when in adjacent-container mode. */
    private final Set<Direction> outputSides = EnumSet.noneOf(Direction.class);

    /** AE2-style FE adapter: external cables can feed the internal AE buffer (1 AE = 2 FE). */
    private final IEnergyStorage forgeEnergyAdapter = new ForgeEnergyAdapter(this);

    public CropAssemblyChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IAEPowerStorage.class, this)
                .addService(IGridTickable.class, this);
        var b = (CropAssemblyChamberBlock) state.getBlock();
        stored = new StoredEnergyAmount(0, b.getMaxPower(), t -> setChanged());
        seedSlot.setMaxStackSize(0, 1);
        this.configManager.registerSetting(AASettings.ME_EXPORT, YesNo.YES);
        this.outputSides.addAll(List.of(Direction.values()));
    }

    @Override
    public IConfigManager getConfigManager() {
        return configManager;
    }

    private void onConfigChanged(IConfigManager manager, Setting<?> setting) {
        saveChanges();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    @Override
    public Set<Direction> getOutputSides() {
        return outputSides;
    }

    @Override
    public void setOutputSide(Direction side, boolean value) {
        if (value) {
            this.outputSides.add(side);
        } else {
            this.outputSides.remove(side);
        }
        onConfigChanged(null, null);
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    private void onUpgradesChanged() {
        setChanged();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        setChanged();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        setChanged();
        // Sync the seed to clients so the renderer can show it floating in the block
        if (inv == seedSlot && !isClientSide()) {
            markForUpdate();
        }
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    @Override
    protected IManagedGridNode createMainNode() {
        return GridHelper.createManagedNode(this, new IGridNodeListener<>() {
            @Override public void onSaveChanges(CropAssemblyChamberBlockEntity o, IGridNode n) { o.setChanged(); }
            @Override public void onStateChanged(CropAssemblyChamberBlockEntity o, IGridNode n, IGridNodeListener.State s) {
                // Guard with isLoaded: AE2 destroys the node on ChunkEvent.Unload — calling
                // setBlock then would touch the chunk mid-unload, bump its generation refcount
                // and stall server shutdown forever (infinite unload rescheduling).
                if (o.level != null && !o.level.isClientSide() && o.level.isLoaded(o.worldPosition))
                    CropAssemblyChamberBlock.setOnline(o.level, o.worldPosition,
                            o.getBlockState(), n.isActive() || o.hasBufferPower());

                // Track whether the grid can actually deliver power (same check as the molecular assembler)
                if (s != IGridNodeListener.State.GRID_BOOT) {
                    boolean newState = false;
                    var grid = n.getGrid();
                    if (grid != null) {
                        newState = n.isPowered() && grid.getEnergyService().extractAEPower(1,
                                Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.0001;
                    }
                    if (newState != o.isPowered) {
                        o.isPowered = newState;
                        o.markForUpdate();
                    }
                }
            }
        }).setExposedOnSides(EnumSet.allOf(Direction.class));
    }

    @Override public AECableType getCableConnectionType(Direction d) { return AECableType.SMART; }

    // ===== IExternalPowerSink (copied from AE2's AEBasePoweredBlockEntity) =====
    @Override
    public double injectExternalPower(PowerUnit input, double amount, Actionable mode) {
        return PowerUnit.AE.convertTo(input, injectAEPower(input.convertTo(PowerUnit.AE, amount), mode));
    }

    @Override
    public double getExternalPowerDemand(PowerUnit externalUnit, double maxPowerRequired) {
        return PowerUnit.AE.convertTo(externalUnit,
                Math.max(0.0, getAEMaxPower() - getAECurrentPower()));
    }

    /**
     * FE capability endpoint. Copied from AE2's AEBasePoweredBlockEntity: null-side queries
     * (e.g. Jade's generic lookup) get null, so no FE slot is displayed; only side-specific
     * access from FE cables/pipes receives the adapter.
     */
    public IEnergyStorage getEnergyStorage(Direction side) {
        if (side == null) return null;
        return forgeEnergyAdapter;
    }

    // ===== IFluidHandler (internal water tank, input-only) =====
    public int getStoredWater() {
        return storedWater;
    }
    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) {
        return storedWater > 0 ? new FluidStack(Fluids.WATER, storedWater) : FluidStack.EMPTY;
    }
    @Override public int getTankCapacity(int tank) { return WATER_CAPACITY; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return stack.getFluid() == Fluids.WATER; }
    @Override public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        if (resource.getFluid() != Fluids.WATER) return 0;
        int filled = Math.min(resource.getAmount(), WATER_CAPACITY - storedWater);
        if (action.execute()) {
            storedWater += filled;
            setChanged();
        }
        return filled;
    }
    @Override public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) { return FluidStack.EMPTY; }
    @Override public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) { return FluidStack.EMPTY; }

    /** Whether the grid can actually deliver power — drives the client-side light strip. */
    public boolean isPowered() {
        return isPowered;
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        final boolean c = super.readFromStream(data);
        final boolean oldPower = this.isPowered;
        this.isPowered = data.readBoolean();
        this.seedSlot.setItemDirect(0, ItemStack.OPTIONAL_STREAM_CODEC.decode(data));
        return this.isPowered != oldPower || c;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.isPowered);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(data, this.seedSlot.getStackInSlot(0));
    }

    // ===== IAEPowerStorage =====
    @Override public double injectAEPower(double a, Actionable m) {
        double i = stored.insert(a, m == Actionable.MODULATE); return a - i;
    }
    @Override public double extractAEPower(double a, Actionable m, PowerMultiplier pm) {
        double r = pm.multiply(a); double e = stored.extract(r, m == Actionable.MODULATE); return pm.divide(e);
    }
    @Override public double getAEMaxPower() { return stored.getMaximum(); }
    @Override public double getAECurrentPower() { return stored.getAmount(); }
    @Override public boolean isAEPublicPowerStorage() { return false; } // private buffer, fed by FE input
    @Override public AccessRestriction getPowerFlow() { return AccessRestriction.READ_WRITE; }

    // ===== IGridTickable =====
    @Override public TickingRequest getTickingRequest(IGridNode n) { return new TickingRequest(1, 20, false); }
    @Override public TickRateModulation tickingRequest(IGridNode n, int t) {
        if (getLevel() instanceof ServerLevel sl) process(sl, t);
        return TickRateModulation.IDLE;
    }

    private int tickCounter;

    private void process(ServerLevel lv, int ticksSinceLastCall) {
        tickCounter += ticksSinceLastCall;
        int interval = getEffectiveInterval();
        if (tickCounter < interval) return;
        tickCounter = 0;

        var seed = seedSlot.getStackInSlot(0);
        if (!isSeedLike(seed)) return;

        var grid = getMainNode().getGrid();
        boolean gridActive = grid != null && getMainNode().isActive();
        // Offline (no active network): products can only go to adjacent containers
        if (!gridActive && outputSides.isEmpty()) return;

        var src = new MachineSource(this);
        appeng.api.storage.MEStorage inv = gridActive ? grid.getStorageService().getInventory() : null;

        // Draw power: internal buffer first (FE input), the grid covers the deficit
        double extracted = extractAEPower(POWER_COST, Actionable.MODULATE, PowerMultiplier.CONFIG);
        double deficit = POWER_COST - extracted;
        if (deficit > 1) {
            double fromGrid = 0;
            if (gridActive) {
                fromGrid = grid.getEnergyService().extractAEPower(deficit, Actionable.MODULATE,
                        PowerMultiplier.CONFIG);
            }
            if (extracted + fromGrid < POWER_COST - 1) return; // insufficient power anywhere
        }

        // Water: internal tank first, the network covers the deficit
        int fromTank = (int) Math.min(storedWater, WATER_COST);
        long waterDeficit = WATER_COST - fromTank;
        if (waterDeficit > 0) {
            if (inv == null) return; // tank insufficient and no network water
            long availWater = inv.extract(AEFluidKey.of(Fluids.WATER), waterDeficit, Actionable.SIMULATE, src);
            if (availWater < waterDeficit) return; // insufficient water anywhere
            inv.extract(AEFluidKey.of(Fluids.WATER), waterDeficit, Actionable.MODULATE, src);
        }
        storedWater -= fromTank;

        // Each amplification card triples this operation's output (max 6 cards -> x729)
        long multiplier = 1;
        for (int i = 0; i < upgrades.getInstalledUpgrades(ModItems.AMPLIFICATION_CARD.get()); i++) multiplier *= 3;

        if (seed.is(Items.CHORUS_FLOWER)) {
            // Chorus flower: 1x chorus flower + 1x chorus fruit per operation
            insertOrExport(inv, AEItemKey.of(Items.CHORUS_FLOWER), 1L * multiplier, src);
            insertOrExport(inv, AEItemKey.of(Items.CHORUS_FRUIT), 1L * multiplier, src);
        } else if (isFlower(seed)) {
            // Flowers: 3x of the same flower per operation
            var key = AEItemKey.of(seed);
            if (key != null) {
                insertOrExport(inv, key, 3L * multiplier, src);
            }
        } else {
            for (var drop : simulateHarvest(lv, seed)) {
                var key = AEItemKey.of(drop);
                if (key == null) continue;
                long amount = (long) drop.getCount() * multiplier;
                if (amount < 1) amount = 1;
                insertOrExport(inv, key, amount, src);
            }
        }

        // Crafting particles — the same fx the molecular assembler uses while crafting
        var craftingParticle = (net.minecraft.core.particles.ParticleOptions) BuiltInRegistries.PARTICLE_TYPE.get(
                ResourceLocation.fromNamespaceAndPath("ae2", "crafting_fx"));
        if (craftingParticle != null) {
            lv.sendParticles(craftingParticle,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    8, 0.4, 0.4, 0.4, 0.05);
        }

        // Texture: operational = connected to an active network OR the buffer can run the machine
        CropAssemblyChamberBlock.setOnline(lv, worldPosition, getBlockState(),
                gridActive || hasBufferPower());

        setChanged();
    }

    /** Whether the internal buffer alone can power one operation (FE-fed state). */
    private boolean hasBufferPower() {
        return getAECurrentPower() >= POWER_COST;
    }

    /** 0=idle (no seed), 1=ready, 2=missing power/water. */
    public int getStatus() {
        if (!isSeedLike(seedSlot.getStackInSlot(0))) return 0;
        var grid = getMainNode().getGrid();
        boolean gridActive = grid != null && getMainNode().isActive();
        var src = new MachineSource(this);
        appeng.api.storage.MEStorage inv = gridActive ? grid.getStorageService().getInventory() : null;

        // Energy: internal buffer first, then the network
        double available = Math.min(POWER_COST, getAECurrentPower());
        double deficit = POWER_COST - available;
        if (deficit > 1 && gridActive) {
            available += grid.getEnergyService().extractAEPower(deficit, Actionable.SIMULATE,
                    PowerMultiplier.CONFIG);
        }
        if (available < POWER_COST - 1) return 2; // both buffer and network lack energy

        // Water: internal tank first, then the network
        int fromTank = (int) Math.min(storedWater, WATER_COST);
        long waterDeficit = WATER_COST - fromTank;
        if (waterDeficit > 0) {
            if (inv == null) return 2; // tank insufficient and no network water
            long availWater = inv.extract(AEFluidKey.of(Fluids.WATER), waterDeficit, Actionable.SIMULATE, src);
            if (availWater < waterDeficit) return 2;
        }
        return 1;
    }

    /**
     * Route products per the storage-mode setting: ME network (default) or adjacent containers
     * on the configured output sides. Adjacent mode is strict: products ONLY go to the
     * containers, whatever does not fit is dropped (never falls back to the ME network).
     */
    private void insertOrExport(@Nullable appeng.api.storage.MEStorage inv, AEItemKey key, long amount, MachineSource src) {
        // Offline (no network) or adjacent mode: products only go to the containers
        if (inv == null || configManager.getSetting(AASettings.ME_EXPORT) == YesNo.NO) {
            if (!outputSides.isEmpty() && level != null) {
                exportToAdjacent(key, amount);
            }
            return;
        }
        inv.insert(key, amount, Actionable.MODULATE, src);
    }

    /** Push as much of the given key into adjacent containers on the output sides; returns the leftover amount. */
    private long exportToAdjacent(AEItemKey key, long amount) {
        long remaining = amount;
        int maxStack = key.getItem().getDefaultMaxStackSize();
        for (Direction dir : outputSides) {
            if (remaining <= 0) break;
            var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (handler == null) continue;
            while (remaining > 0) {
                int batch = (int) Math.min(remaining, maxStack);
                var leftover = key.toStack(batch);
                for (int i = 0; i < handler.getSlots() && !leftover.isEmpty(); i++) {
                    leftover = handler.insertItem(i, leftover, false);
                }
                int inserted = batch - leftover.getCount();
                if (inserted <= 0) break; // container full (or won't accept this item)
                remaining -= inserted;
            }
        }
        return remaining;
    }

    private int getEffectiveInterval() {
        int cards = upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        // 0 cards: 200 ticks (10s); each card multiplies by sqrt(0.1) -> 2 cards = 20 ticks (1s)
        double interval = BASE_INTERVAL * Math.pow(0.1, cards / 2.0);
        return Math.max(1, (int) Math.round(interval));
    }

    /** Accepts crop-like seed items: blocks with a maxable age (CropBlock or an "age" int property), plus flowers and chorus flowers. */
    public static boolean isSeedLike(ItemStack stack) {
        if (isFlower(stack) || stack.is(Items.CHORUS_FLOWER)) return true;
        if (stack.getItem() instanceof BlockItem blockItem) {
            var block = blockItem.getBlock();
            if (block instanceof CropBlock) return true;
            return block.defaultBlockState().getProperties().stream()
                    .anyMatch(p -> p.getName().equals("age") && p instanceof IntegerProperty);
        }
        return false;
    }

    /**
     * Small and tall flowers: by vanilla tags, plus untagged modded flowers
     * whose block extends the vanilla FlowerBlock/TallFlowerBlock classes.
     */
    private static boolean isFlower(ItemStack stack) {
        if (stack.is(ItemTags.FLOWERS) || stack.is(ItemTags.TALL_FLOWERS)) return true;
        if (stack.getItem() instanceof BlockItem bi) {
            var block = bi.getBlock();
            return block instanceof FlowerBlock || block instanceof TallFlowerBlock;
        }
        return false;
    }

    /**
     * Simulates planting the seed, force-growing the crop (age set to max — not bonemeal)
     * and breaking it. Purely internal: no blocks are placed in the world, the actual
     * output is whatever breaking the fully grown crop would drop.
     */
    public static List<ItemStack> simulateHarvest(ServerLevel lv, ItemStack seed) {
        var blockItem = (BlockItem) seed.getItem();
        var crop = blockItem.getBlock();
        BlockState grown = crop.defaultBlockState();
        // Set the age property to its maximum (CropBlock's age is also just an "age" int property)
        for (var prop : grown.getProperties()) {
            if (prop.getName().equals("age") && prop instanceof IntegerProperty ageProp) {
                grown = grown.setValue(ageProp, java.util.Collections.max(ageProp.getPossibleValues()));
                break;
            }
        }
        // The level is only a context for loot resolution — nothing is actually broken.
        return Block.getDrops(grown, lv, BlockPos.ZERO, null, null, ItemStack.EMPTY);
    }

    // ===== Drops =====
    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);

        // Drop the seed and installed upgrades when the machine is broken or wrenched
        var seed = seedSlot.getStackInSlot(0);
        if (!seed.isEmpty()) {
            drops.add(seed);
        }
        for (var upgrade : upgrades) {
            drops.add(upgrade);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        seedSlot.setItemDirect(0, ItemStack.EMPTY);
        upgrades.clear();
    }

    // ===== NBT =====
    @Override public void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r); t.putDouble("pwr", stored.getAmount());
        t.putInt("water", storedWater);
        seedSlot.writeToNBT(t, "seed", r);
        upgrades.writeToNBT(t, "upgrades", r);
        configManager.writeToNBT(t, r);
        var sides = new ListTag();
        for (var side : this.outputSides) {
            sides.add(StringTag.valueOf(side.getName()));
        }
        t.put("output_side", sides);
    }
    @Override public void loadTag(CompoundTag t, HolderLookup.Provider r) {
        super.loadTag(t, r); stored.setStored(t.getDouble("pwr"));
        storedWater = t.getInt("water");
        seedSlot.readFromNBT(t, "seed", r);
        upgrades.readFromNBT(t, "upgrades", r);
        configManager.readFromNBT(t, r);
        this.outputSides.clear();
        if (t.contains("output_side")) {
            for (var name : t.getList("output_side", CompoundTag.TAG_STRING)) {
                this.outputSides.add(Direction.byName(name.getAsString()));
            }
        } else {
            this.outputSides.addAll(List.of(Direction.values()));
        }
    }

    // ===== Menu =====
    @Override public Component getDisplayName() { return Component.translatable("block.appliedagriculture.crop_assembly_chamber"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new CropAssemblyChamberMenu(id, inv, this); }
}
