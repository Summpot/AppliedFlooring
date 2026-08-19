package io.github.summpot.appliedflooring.block

import appeng.api.parts.IPartHost
import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.blockentity.MEFlooringBlockEntity
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

open class MEFlooringBlock(
    properties: BlockBehaviour.Properties,
    val color: AEColor = AEColor.TRANSPARENT,
    val isDense: Boolean = false
) : Block(properties), EntityBlock, SimpleWaterloggedBlock {

    companion object {
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED
        private val FULL_SHAPE: VoxelShape = Shapes.block()
    }

    init {
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(WATERLOGGED)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return MEFlooringBlockEntity(ModBlockEntities.ME_FLOORING_BE.get(), pos, state, isDense, color)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return FULL_SHAPE
    }

    override fun getCollisionShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return FULL_SHAPE
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        return InteractionResult.PASS
    }

    override fun stepOn(level: Level, pos: BlockPos, state: BlockState, entity: Entity) {
        super.stepOn(level, pos, state, entity)
        val be = level.getBlockEntity(pos)
        if (be is MEFlooringBlockEntity) {
            be.onEntitySteppedOn(entity)
        }
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val fluidState = context.level.getFluidState(context.clickedPos)
        return defaultBlockState().setValue(WATERLOGGED, fluidState.type == Fluids.WATER)
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)
    }

    fun recolourBlock(level: BlockGetter, pos: BlockPos, side: Direction, color: DyeColor, who: Player): Boolean {
        val be = level.getBlockEntity(pos)
        if (be is MEFlooringBlockEntity) {
            return be.recolourBlock(side, AEColor.fromDye(color), who)
        }
        return false
    }
}
