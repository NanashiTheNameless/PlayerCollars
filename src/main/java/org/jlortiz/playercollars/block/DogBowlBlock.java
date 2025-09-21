package org.jlortiz.playercollars.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.jlortiz.playercollars.PlayerCollarsMod;

public class DogBowlBlock extends Block implements BlockEntityProvider {
    protected static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 1.0, 15.0);
    public static final IntProperty LEVEL = Properties.AGE_3;
    public final DyeColor color;

    public DogBowlBlock(DyeColor c, Settings settings) {
        super(settings);
        color = c;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DogBowlBlockEntity(pos, state);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        return hasTopRim(world, blockPos) || sideCoversSmallSquare(world, blockPos, Direction.UP);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.isEmpty() || stack.get(DataComponentTypes.FOOD) == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(world.getBlockEntity(pos) instanceof DogBowlBlockEntity be)) return ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        int decr = be.insert(stack);
        if (decr > 0) {
            stack.decrement(decr);
            state = state.with(LEVEL, Math.min((be.getCount() + 15) / 16, 3));
            world.setBlockState(pos, state, 0);
            return ItemActionResult.SUCCESS;
        }
        return ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof DogBowlBlockEntity be)) return ActionResult.PASS;

        ItemStack is = be.take();
        if (is.isEmpty()) return ActionResult.PASS;
        state = state.with(LEVEL, Math.min((be.getCount() + 15) / 16, 3));
        world.setBlockState(pos, state, 0);

        FoodComponent food = is.get(DataComponentTypes.FOOD);
        if (food != null && player.canConsume(food.canAlwaysEat())) {
            player.eatFood(world, is, food);
            return ActionResult.SUCCESS;
        } else if (!player.giveItemStack(is)) {
            player.dropItem(is, true);
        }
        return ActionResult.CONSUME;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    public static class DogBowlBlockEntity extends BlockEntity {
        private ItemStack inBowl = ItemStack.EMPTY;

        public DogBowlBlockEntity(BlockPos pos, BlockState state) {
            super(PlayerCollarsMod.DOG_BOWL_BLOCK_ENTITY, pos, state);
        }

        protected int getCount() {
            return inBowl.getCount();
        }

        protected int insert(ItemStack is) {
            if (inBowl.isEmpty()) {
                inBowl = is.copy();
                return is.getCount();
            }
            if (is.isOf(inBowl.getItem())) {
                int count = Math.min(is.getCount(), inBowl.getMaxCount() - inBowl.getCount());
                inBowl.increment(count);
                return count;
            }
            return 0;
        }

        protected ItemStack take() {
            if (inBowl.isEmpty()) return ItemStack.EMPTY;
            ItemStack is = inBowl.copyWithCount(1);
            inBowl.decrement(1);
            return is;
        }
    }
}
