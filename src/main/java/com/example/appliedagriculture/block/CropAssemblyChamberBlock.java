package com.example.appliedagriculture.block;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.example.appliedagriculture.blockentity.CropAssemblyChamberBlockEntity;
import com.example.appliedagriculture.menu.CropAssemblyChamberMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CropAssemblyChamberBlock extends AEBaseEntityBlock<CropAssemblyChamberBlockEntity> {

    public static final BooleanProperty ONLINE = BooleanProperty.create("online");

    public static final double MAX_POWER = 2_000_000;

    public CropAssemblyChamberBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ONLINE, false));
    }

    public double getMaxPower() { return MAX_POWER; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ONLINE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        var be = getBlockEntity(level, pos);
        if (be == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            MenuOpener.open(CropAssemblyChamberMenu.TYPE, player, MenuLocators.forBlockEntity(be));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static void setOnline(Level level, BlockPos pos, BlockState state, boolean on) {
        if (!level.isClientSide() && state.getValue(ONLINE) != on)
            level.setBlock(pos, state.setValue(ONLINE, on), Block.UPDATE_ALL);
    }
}
