package com.invasion.block;

import com.invasion.ModRegistry;
import com.invasion.world.PortalTeleporter;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/** Vertical, nether-portal style gateway between the Overworld and the Overgrowth. */
public class InvasionPortalBlock extends Block implements EntityBlock, net.minecraft.world.level.block.LiquidBlockContainer {
    public static final MapCodec<InvasionPortalBlock> CODEC = simpleCodec(InvasionPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** Exactly one block per portal is the core: it ticks moss spreading and the zombie horde. */
    public static final BooleanProperty CORE = BooleanProperty.create("core");

    private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(new Vector3f(0.2F, 0.75F, 0.25F), 1.0F);

    public InvasionPortalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(CORE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, CORE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Block.box(6, 0, 0, 10, 16, 16) : Block.box(0, 0, 6, 16, 16, 10);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player
                && !player.isPassenger() && !player.isVehicle()) {
            if (player.isOnPortalCooldown()) {
                player.setPortalCooldown();
                return;
            }
            PortalTeleporter.travel(player, serverLevel);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.4F, random.nextFloat() * 0.3F + 0.5F, false);
        }
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(i == 0 ? GREEN_DUST : ParticleTypes.PORTAL, x, y, z,
                    (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Every portal block has a block entity so the starfield renderer can draw it; only the core one ticks.
        return new PortalCoreBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !state.getValue(CORE) || type != ModRegistry.PORTAL_CORE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<PortalCoreBlockEntity>) PortalCoreBlockEntity::serverTick;
    }

    /** Water and other fluids can never flow into (and wash away) the portal. */
    @Override
    public boolean canPlaceLiquid(@Nullable net.minecraft.world.entity.player.Player player, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state, net.minecraft.world.level.material.FluidState fluid) {
        return false;
    }
}
