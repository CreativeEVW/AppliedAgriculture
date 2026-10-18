package com.example.appliedagriculture.block;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.example.appliedagriculture.blockentity.BiomaterialSynthesisChamberBlockEntity;
import com.example.appliedagriculture.menu.BiomaterialSynthesisChamberMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class BiomaterialSynthesisChamberBlock extends AEBaseEntityBlock<BiomaterialSynthesisChamberBlockEntity> {

    public static final BooleanProperty ONLINE = BooleanProperty.create("online");

    public static final double MAX_POWER = 2_000_000;

    public BiomaterialSynthesisChamberBlock(Properties props) {
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
            MenuOpener.open(BiomaterialSynthesisChamberMenu.TYPE, player, MenuLocators.forBlockEntity(be));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static void setOnline(Level level, BlockPos pos, BlockState state, boolean on) {
        if (!level.isClientSide() && state.getValue(ONLINE) != on)
            level.setBlock(pos, state.setValue(ONLINE, on), Block.UPDATE_ALL);
    }
}
