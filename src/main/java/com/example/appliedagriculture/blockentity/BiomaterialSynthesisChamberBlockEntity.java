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
import com.example.appliedagriculture.block.BiomaterialSynthesisChamberBlock;
import com.example.appliedagriculture.compat.IndustrialForegoingCompat;
import com.example.appliedagriculture.compat.MESpawnerCompat;
import com.example.appliedagriculture.menu.BiomaterialSynthesisChamberMenu;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class BiomaterialSynthesisChamberBlockEntity extends AENetworkedBlockEntity
        implements IAEPowerStorage, IExternalPowerSink, IGridTickable, MenuProvider, IUpgradeableObject,
        IConfigurableObject, IOutputSideConfigurable, InternalInventoryHost {

    private static final int BASE_INTERVAL = 200;                 // 10s per operation
    private static final long POWER_COST = 200;                   // AE per operation
    public static final int MAX_UPGRADE_SLOTS = 8;

    private final StoredEnergyAmount stored;
    public final AppEngInternalInventory eggSlot = new AppEngInternalInventory(this, 1);
    private final IUpgradeInventory upgrades =
            UpgradeInventories.forMachine(ModBlocks.BIOMATERIAL_SYNTHESIS_CHAMBER, MAX_UPGRADE_SLOTS, this::onUpgradesChanged);
    private boolean isPowered = false;

    /** Storage mode: ME_EXPORT = YES inserts products into the ME network, NO pushes them to adjacent containers. */
    private final ConfigManager configManager = new ConfigManager(this::onConfigChanged);

    /** Sides products are pushed to when in adjacent-container mode. */
    private final Set<Direction> outputSides = EnumSet.noneOf(Direction.class);

    /** AE2-style FE adapter: external cables can feed the internal AE buffer (1 AE = 2 FE). */
    private final IEnergyStorage forgeEnergyAdapter = new ForgeEnergyAdapter(this);

    public BiomaterialSynthesisChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IAEPowerStorage.class, this)
                .addService(IGridTickable.class, this);
        var b = (BiomaterialSynthesisChamberBlock) state.getBlock();
        stored = new StoredEnergyAmount(0, b.getMaxPower(), t -> setChanged());
        eggSlot.setMaxStackSize(0, 1);
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
        // Sync the egg to clients so the renderer can show it floating in the block
        if (inv == eggSlot && !isClientSide()) {
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
            @Override public void onSaveChanges(BiomaterialSynthesisChamberBlockEntity o, IGridNode n) { o.setChanged(); }
            @Override public void onStateChanged(BiomaterialSynthesisChamberBlockEntity o, IGridNode n, IGridNodeListener.State s) {
                // Guard with isLoaded: AE2 destroys the node on ChunkEvent.Unload — calling
                // setBlock then would touch the chunk mid-unload, bump its generation refcount
                // and stall server shutdown forever (infinite unload rescheduling).
                if (o.level != null && !o.level.isClientSide() && o.level.isLoaded(o.worldPosition))
                    BiomaterialSynthesisChamberBlock.setOnline(o.level, o.worldPosition,
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

    /** Whether the grid can actually deliver power — drives the client-side light strip. */
    public boolean isPowered() {
        return isPowered;
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        final boolean c = super.readFromStream(data);
        final boolean oldPower = this.isPowered;
        this.isPowered = data.readBoolean();
        this.eggSlot.setItemDirect(0, ItemStack.OPTIONAL_STREAM_CODEC.decode(data));
        return this.isPowered != oldPower || c;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.isPowered);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(data, this.eggSlot.getStackInSlot(0));
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

        var plan = computePlan(lv);
        if (plan == null) return;

        var grid = getMainNode().getGrid();
        boolean gridActive = grid != null && getMainNode().isActive();
        // Offline (no active network): products can only go to adjacent containers
        if (!gridActive && outputSides.isEmpty()) return;

        var src = new MachineSource(this);
        appeng.api.storage.MEStorage inv = gridActive ? grid.getStorageService().getInventory() : null;

        // Draw power: internal buffer first (FE input, works offline), the grid covers the deficit
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

        // Produce outputs — items follow the storage-mode route, fluids go to the network only
        for (var stack : plan.items()) {
            var key = AEItemKey.of(stack);
            if (key == null) continue;
            insertOrExport(inv, key, stack.getCount(), src);
        }
        // Fluids follow the storage mode too: ME network, or adjacent containers (strict gate)
        if (inv != null && configManager.getSetting(AASettings.ME_EXPORT) == YesNo.YES) {
            for (var fluidOutput : plan.fluids()) {
                inv.insert(AEFluidKey.of(fluidOutput.fluid()), fluidOutput.amount(), Actionable.MODULATE, src);
            }
        } else {
            exportFluidsToAdjacent(plan.fluids());
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
        BiomaterialSynthesisChamberBlock.setOnline(lv, worldPosition, getBlockState(),
                gridActive || hasBufferPower());

        setChanged();
    }

    /** Whether the internal buffer alone can power one operation (FE-fed state). */
    private boolean hasBufferPower() {
        return getAECurrentPower() >= POWER_COST;
    }

    /** 0=idle (no producible egg), 1=ready, 2=missing power. */
    public int getStatus() {
        if (!(level instanceof ServerLevel lv)) return 0;
        if (computePlan(lv) == null) return 0;

        // Energy check: internal buffer first (FE input, works offline), then the AE network.
        double available = Math.min(POWER_COST, getAECurrentPower());
        double deficit = POWER_COST - available;
        if (deficit > 1) {
            var grid = getMainNode().getGrid();
            if (grid != null && getMainNode().isActive()) {
                available += grid.getEnergyService().extractAEPower(deficit, Actionable.SIMULATE,
                        PowerMultiplier.CONFIG);
            }
        }
        if (available < POWER_COST - 1) return 2; // both buffer and network lack energy

        return 1;
    }

    /** The four vanilla mushroom-like items (brown/red mushroom, crimson/warped fungus). */
    private static final java.util.Set<Item> VANILLA_MUSHROOMS = java.util.Set.of(
            Items.BROWN_MUSHROOM, Items.RED_MUSHROOM,
            Items.CRIMSON_FUNGUS, Items.WARPED_FUNGUS
    );

    /** Accepts spawn eggs, (optionally) MESpawner monster disks, saplings and vanilla mushrooms. */
    public static boolean isValidInput(ItemStack stack) {
        if (stack.getItem() instanceof SpawnEggItem) return true;
        if (MESpawnerCompat.isLoaded() && MESpawnerCompat.isMonsterDisk(stack)) return true;
        if (stack.is(ItemTags.SAPLINGS)) return true;
        return VANILLA_MUSHROOMS.contains(stack.getItem());
    }

    /** The entity types the egg/disk represents (empty when nothing is loaded). */
    private static List<EntityType<?>> resolveEntityTypes(ServerLevel lv, ItemStack seed) {
        if (seed.getItem() instanceof SpawnEggItem egg) {
            var t = egg.getType(seed);
            if (t == null) return List.of();
            return List.of(t);
        }
        if (MESpawnerCompat.isLoaded() && MESpawnerCompat.isMonsterDisk(seed)) {
            return MESpawnerCompat.getFormedEntityTypes(seed, lv);
        }
        return List.of();
    }

    /**
     * Computes what this operation would produce, or null when nothing is producible.
     * Vanilla mushrooms -> 3x of the same mushroom.
     * Saplings -> 1b latex (Industrial Foregoing) + 5x the matching log + 1x sapling back
     * (oak additionally yields apples).
     * Chicken -> eggs; sheep -> wool; cow -> milk protein; mooshroom -> milk protein + fungus mixture.
     * A monster disk may contain several entity types — all of them are produced per operation.
     * Entity types matching IF's fluid laser drill recipes additionally output their fluid
     * (e.g. wither -> ether gas) into the network.
     * The yield card multiplies everything.
     */
    @Nullable
    private Plan computePlan(ServerLevel lv) {
        var seed = eggSlot.getStackInSlot(0);
        long amp = getAmplification();

        // Vanilla mushrooms -> 3x of the same mushroom
        if (VANILLA_MUSHROOMS.contains(seed.getItem())) {
            return new Plan(
                    new java.util.ArrayList<>(List.of(new ItemStack(seed.getItem(), (int) (3 * amp)))),
                    List.of());
        }

        // Sapling -> latex + logs + sapling back
        if (seed.is(ItemTags.SAPLINGS)) {
            var items = new java.util.ArrayList<ItemStack>();
            var log = getLogForSapling(seed);
            if (log != null) {
                items.add(new ItemStack(log, (int) (5 * amp)));
            }
            items.add(new ItemStack(seed.getItem(), (int) amp)); // sapling back
            if (seed.is(Items.OAK_SAPLING)) {
                items.add(new ItemStack(Items.APPLE, (int) amp));
            }
            var fluids = new java.util.ArrayList<FluidOutput>();
            if (IndustrialForegoingCompat.isLoaded()) {
                var latex = IndustrialForegoingCompat.getLatex();
                if (latex != null) {
                    fluids.add(new FluidOutput(latex, AEFluidKey.AMOUNT_BUCKET * amp));
                }
            }
            return new Plan(items, fluids);
        }

        var types = resolveEntityTypes(lv, seed);
        if (types.isEmpty()) return null;

        var items = new java.util.ArrayList<ItemStack>();
        var fluids = new java.util.ArrayList<FluidOutput>();
        for (var type : types) {
            var entity = type.create(lv);
            if (entity == null) continue;

            boolean handled = false;
            if (entity instanceof Chicken) {
                items.add(new ItemStack(Items.EGG, (int) amp));
                handled = true;
            } else if (entity instanceof Sheep) {
                items.add(new ItemStack(Items.WHITE_WOOL, (int) amp));
                handled = true;
            } else if (entity instanceof MushroomCow) { // must be checked before Cow (subclass)
                items.add(new ItemStack(ModItems.MILK_PROTEIN.get(), (int) amp));
                items.add(new ItemStack(ModItems.FUNGUS_MIXTURE.get(), (int) amp));
                handled = true;
            } else if (entity instanceof Cow) {
                items.add(new ItemStack(ModItems.MILK_PROTEIN.get(), (int) amp));
                handled = true;
            }
            if (IndustrialForegoingCompat.isLoaded()) {
                // Sync IF's fluid laser drill recipes: entity A -> fluid B
                var fluid = IndustrialForegoingCompat.getLaserDrillFluid(lv, type);
                if (fluid != null) {
                    fluids.add(new FluidOutput(fluid, AEFluidKey.AMOUNT_BUCKET * amp));
                    handled = true;
                }
                // Mobs without any specific product: pink slime + liquid meat scaled by max health
                // (20 HP -> 1b of each per operation)
                if (!handled && entity instanceof LivingEntity living) {
                    long amount = Math.max(1, (long) (living.getMaxHealth() * 50) * amp);
                    var pinkSlime = IndustrialForegoingCompat.getPinkSlime();
                    var liquidMeat = IndustrialForegoingCompat.getLiquidMeat();
                    if (pinkSlime != null) fluids.add(new FluidOutput(pinkSlime, amount));
                    if (liquidMeat != null) fluids.add(new FluidOutput(liquidMeat, amount));
                }
            }
        }

        if (items.isEmpty() && fluids.isEmpty()) return null;
        return new Plan(items, fluids);
    }

    private static final java.util.Map<Item, Item> SAPLING_LOGS = java.util.Map.ofEntries(
            java.util.Map.entry(Items.OAK_SAPLING, Items.OAK_LOG),
            java.util.Map.entry(Items.SPRUCE_SAPLING, Items.SPRUCE_LOG),
            java.util.Map.entry(Items.BIRCH_SAPLING, Items.BIRCH_LOG),
            java.util.Map.entry(Items.JUNGLE_SAPLING, Items.JUNGLE_LOG),
            java.util.Map.entry(Items.ACACIA_SAPLING, Items.ACACIA_LOG),
            java.util.Map.entry(Items.DARK_OAK_SAPLING, Items.DARK_OAK_LOG),
            java.util.Map.entry(Items.MANGROVE_PROPAGULE, Items.MANGROVE_LOG),
            java.util.Map.entry(Items.CHERRY_SAPLING, Items.CHERRY_LOG)
    );

    /** The log a sapling grows into: vanilla map first, then <ns>:<name>_sapling -> <ns>:<name>_log. */
    @Nullable
    private static Item getLogForSapling(ItemStack sapling) {
        var known = SAPLING_LOGS.get(sapling.getItem());
        if (known != null) return known;
        var id = BuiltInRegistries.ITEM.getKey(sapling.getItem());
        if (id.getPath().endsWith("_sapling")) {
            var stem = id.getPath().substring(0, id.getPath().length() - "_sapling".length());
            var logId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), stem + "_log");
            return BuiltInRegistries.ITEM.getOptional(logId).orElse(null);
        }
        return null;
    }

    private record Plan(List<ItemStack> items, List<FluidOutput> fluids) {
    }

    private record FluidOutput(Fluid fluid, long amount) {
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

    /**
     * Output fluids into adjacent containers on the enabled sides. All-or-nothing gate:
     * with n fluid types, if the total fluid slots of all configured containers is below n,
     * or every container is already full, nothing is output this cycle.
     */
    private boolean exportFluidsToAdjacent(List<FluidOutput> outputs) {
        int n = outputs.size();
        if (n == 0) return true;

        var handlers = new java.util.ArrayList<net.neoforged.neoforge.fluids.capability.IFluidHandler>();
        for (Direction dir : outputSides) {
            var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (handler != null) handlers.add(handler);
        }
        if (handlers.isEmpty()) return false;

        // Count usable fluid slots and whether anything has room at all
        int totalSlots = 0;
        boolean anySpace = false;
        for (var handler : handlers) {
            int tanks = handler.getTanks();
            totalSlots += tanks;
            for (int i = 0; i < tanks; i++) {
                if (handler.getFluidInTank(i).getAmount() < handler.getTankCapacity(i)) anySpace = true;
            }
        }
        if (totalSlots < n) return false; // fewer slots than output types
        if (!anySpace) return false;      // every container is full

        // Output directly — fill what fits
        for (var output : outputs) {
            var toFill = new net.neoforged.neoforge.fluids.FluidStack(output.fluid(), (int) output.amount());
            for (var handler : handlers) {
                if (toFill.isEmpty()) break;
                int filled = handler.fill(toFill, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                toFill.shrink(filled);
            }
        }
        return true;
    }

    private long getAmplification() {
        long multiplier = 1;
        for (int i = 0; i < upgrades.getInstalledUpgrades(ModItems.YIELD_SYNTHESIS_CARD.get()); i++) multiplier *= 3;
        return multiplier;
    }

    private int getEffectiveInterval() {
        int cards = upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        // 0 cards: 200 ticks (10s); each card multiplies by sqrt(0.1) -> 2 cards = 20 ticks (1s)
        double interval = BASE_INTERVAL * Math.pow(0.1, cards / 2.0);
        return Math.max(1, (int) Math.round(interval));
    }

    // ===== Drops =====
    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);

        // Drop the egg and installed upgrades when the machine is broken or wrenched
        var egg = eggSlot.getStackInSlot(0);
        if (!egg.isEmpty()) {
            drops.add(egg);
        }
        for (var upgrade : upgrades) {
            drops.add(upgrade);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        eggSlot.setItemDirect(0, ItemStack.EMPTY);
        upgrades.clear();
    }

    // ===== NBT =====
    @Override public void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r); t.putDouble("pwr", stored.getAmount());
        eggSlot.writeToNBT(t, "egg", r);
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
        eggSlot.readFromNBT(t, "egg", r);
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
    @Override public Component getDisplayName() { return Component.translatable("block.appliedagriculture.biomaterial_synthesis_chamber"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new BiomaterialSynthesisChamberMenu(id, inv, this); }
}
