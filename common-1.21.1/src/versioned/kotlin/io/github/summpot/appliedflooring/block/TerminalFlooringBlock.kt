package io.github.summpot.appliedflooring.block

import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.blockentity.TerminalFlooringBlockEntity
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class TerminalFlooringBlock(
    properties: BlockBehaviour.Properties,
    color: AEColor = AEColor.TRANSPARENT
) : MEFlooringBlock(properties, color, isDense = false) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return TerminalFlooringBlockEntity(ModBlockEntities.TERMINAL_FLOORING_BE.get(), pos, state, color)
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val be = level.getBlockEntity(pos)
            if (be is TerminalFlooringBlockEntity) {
                if (be.openTerminalGui(player)) {
                    return InteractionResult.CONSUME
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }
}
