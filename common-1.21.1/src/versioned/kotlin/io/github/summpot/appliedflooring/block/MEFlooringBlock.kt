package io.github.summpot.appliedflooring.block

import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.blockentity.MEFlooringBlockEntity
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
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
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
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
    val color: AEColor = AEColor.TRANSPARENT
) : Block(properties), EntityBlock, SimpleWaterloggedBlock {

    companion object {
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED
        val POWERED: BooleanProperty = BlockStateProperties.POWERED
        val NORTH: BooleanProperty = BlockStateProperties.NORTH
        val SOUTH: BooleanProperty = BlockStateProperties.SOUTH
        val EAST: BooleanProperty = BlockStateProperties.EAST
        val WEST: BooleanProperty = BlockStateProperties.WEST
        private val FULL_SHAPE: VoxelShape = Shapes.block()
    }

    init {
        registerDefaultState(
            defaultBlockState()
                .setValue(WATERLOGGED, false)
                .setValue(POWERED, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(WATERLOGGED)
        builder.add(POWERED)
        builder.add(NORTH)
        builder.add(SOUTH)
        builder.add(EAST)
        builder.add(WEST)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return MEFlooringBlockEntity(ModBlockEntities.ME_FLOORING_BE.get(), pos, state, isDenseCable = true, currentColor = color)
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return BlockEntityTicker { lvl, pos, st, be ->
            if (be is MEFlooringBlockEntity) {
                be.serverTick(lvl, pos, st)
            }
        }
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



    override fun getDrops(state: BlockState, builder: net.minecraft.world.level.storage.loot.LootParams.Builder): MutableList<ItemStack> {
        val drops = super.getDrops(state, builder)
        if (drops.isEmpty()) {
            return mutableListOf(ItemStack(this))
        }
        return drops
    }

    override fun isSignalSource(state: BlockState): Boolean {
        return true
    }

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, side: Direction): Int {
        val be = level.getBlockEntity(pos)
        if (be is MEFlooringBlockEntity) {
            return be.isProvidingWeakPower(side.opposite)
        }
        return 0
    }

    override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, side: Direction): Int {
        val be = level.getBlockEntity(pos)
        if (be is MEFlooringBlockEntity) {
            return be.isProvidingStrongPower(side.opposite)
        }
        return 0
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        neighborBlock: Block,
        neighborPos: BlockPos,
        movedByPiston: Boolean
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
        val be = level.getBlockEntity(pos)
        if (be is MEFlooringBlockEntity) {
            be.onNeighborChanged(level, pos, neighborPos)
        }
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (state.block != newState.block) {
            val be = level.getBlockEntity(pos)
            if (be is MEFlooringBlockEntity) {
                val drops = mutableListOf<ItemStack>()
                be.addAdditionalDrops(drops, false)
                for (drop in drops) {
                    Block.popResource(level, pos, drop)
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        val be = level.getBlockEntity(pos)
        if (be is MEFlooringBlockEntity) {
            val selectedPart = be.selectPartWorld(hit.location)
            if (selectedPart.part != null) {
                val activated = selectedPart.part.onUseWithoutItem(player, hit.location)
                if (activated) {
                    return InteractionResult.sidedSuccess(level.isClientSide)
                }
            }
        }
        return InteractionResult.PASS
    }

    override fun useItemOn(
        heldItem: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): ItemInteractionResult {
        val be = level.getBlockEntity(pos)
        if (be is MEFlooringBlockEntity) {
            // 0. Wrench interaction (dismantle part or dismantle floor block)
            if (io.github.summpot.appliedflooring.util.FlooringWrenchHelper.isWrench(heldItem) && player.isShiftKeyDown) {
                val res = be.disassembleWithWrench(player, level, hit.location, heldItem)
                if (res.consumesAction()) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide)
                }
            }

            // 1. If player is holding a Part Item (e.g. ME Terminal, Monitor, Pattern Provider, Storage Bus)
            if (heldItem.item is appeng.api.parts.IPartItem<*>) {
                val context = net.minecraft.world.item.context.UseOnContext(player, hand, hit)
                val res = appeng.api.parts.PartHelper.usePartItem(context)
                if (res.consumesAction()) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide)
                }
            }

            // 2. If clicking on an attached part
            val selectedPart = be.selectPartWorld(hit.location)
            if (selectedPart.part != null) {
                val activated = selectedPart.part.onUseItemOn(heldItem, player, hand, hit.location)
                if (activated) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide)
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun stepOn(level: Level, pos: BlockPos, state: BlockState, entity: Entity) {
        super.stepOn(level, pos, state, entity)
        val be = level.getBlockEntity(pos)
        if (be is MEFlooringBlockEntity) {
            be.onEntitySteppedOn(entity)
        }
    }

    fun canConnectTo(level: BlockGetter, pos: BlockPos, neighborPos: BlockPos): Boolean {
        val neighborState = level.getBlockState(neighborPos)
        val neighborBlock = neighborState.block
        if (neighborBlock is MEFlooringBlock) {
            val myColor = (level.getBlockEntity(pos) as? MEFlooringBlockEntity)?.currentColor ?: this.color
            val neighborColor = (level.getBlockEntity(neighborPos) as? MEFlooringBlockEntity)?.currentColor ?: neighborBlock.color
            return myColor == neighborColor
        }
        return false
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val level = context.level
        val pos = context.clickedPos
        val fluidState = level.getFluidState(pos)
        return defaultBlockState()
            .setValue(WATERLOGGED, fluidState.type == Fluids.WATER)
            .setValue(POWERED, false)
            .setValue(NORTH, canConnectTo(level, pos, pos.north()))
            .setValue(SOUTH, canConnectTo(level, pos, pos.south()))
            .setValue(EAST, canConnectTo(level, pos, pos.east()))
            .setValue(WEST, canConnectTo(level, pos, pos.west()))
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: net.minecraft.world.level.LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        }
        return when (direction) {
            Direction.NORTH -> state.setValue(NORTH, canConnectTo(level, currentPos, neighborPos))
            Direction.SOUTH -> state.setValue(SOUTH, canConnectTo(level, currentPos, neighborPos))
            Direction.EAST -> state.setValue(EAST, canConnectTo(level, currentPos, neighborPos))
            Direction.WEST -> state.setValue(WEST, canConnectTo(level, currentPos, neighborPos))
            else -> state
        }
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)
    }

    fun recolourBlock(level: BlockGetter, pos: BlockPos, side: Direction, color: DyeColor, who: Player): Boolean {
        val be = level.getBlockEntity(pos)
        if (be is MEFlooringBlockEntity) {
            val res = be.recolourBlock(side, AEColor.fromDye(color), who)
            if (res && level is Level && !level.isClientSide) {
                val currentState = level.getBlockState(pos)
                val newState = currentState
                    .setValue(NORTH, canConnectTo(level, pos, pos.north()))
                    .setValue(SOUTH, canConnectTo(level, pos, pos.south()))
                    .setValue(EAST, canConnectTo(level, pos, pos.east()))
                    .setValue(WEST, canConnectTo(level, pos, pos.west()))
                if (newState != currentState) {
                    level.setBlock(pos, newState, Block.UPDATE_ALL)
                }
                level.updateNeighborsAt(pos, this)
            }
            return res
        }
        return false
    }
}
